/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderTypeConstants;
import fr.paris.lutece.plugins.platform.service.model.OcrService;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;

/**
 * Pipeline node that extracts text from images using LLM vision (OCR).
 */
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_VISION, name = "Vision (OCR)", description = "Envoie des images à un LLM vision pour en extraire le texte brut (OCR). Retourne uniquement le texte visible, chaîne vide si aucun texte." )
public class VisionNode extends AbstractFileProcessingNode
{

    private static final String [ ] ACCEPTED_TYPES = {
            "image/jpeg", "image/png", "image/bmp", "image/tiff", "image/webp"
    };

    private final OcrService _ocrService;

    /**
     * Default constructor.
     */
    public VisionNode( )
    {
        super( PipelineConstants.NODE_TYPE_VISION );
        _ocrService = new OcrService( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String processFile( byte [ ] bytes, String fileName, Provider provider, String executionId, String nodeId )
    {
        try
        {
            BufferedImage image = ImageIO.read( new ByteArrayInputStream( bytes ) );
            if ( image == null )
            {
                throw new RuntimeException( "Unable to read image: " + fileName );
            }
            return _ocrService.performOCR( image, provider, executionId, nodeId );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "OCR error for " + fileName + ": " + e.getMessage( ), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String [ ] getAcceptedContentTypes( )
    {
        return ACCEPTED_TYPES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getProviderType( )
    {
        return ProviderTypeConstants.PROVIDER_TYPE_LLM;
    }
}
