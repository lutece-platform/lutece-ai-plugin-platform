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

import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderTypeConstants;
import fr.paris.lutece.plugins.platform.business.provider.ProviderVendorConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.rag.parser.AzureParser;
import fr.paris.lutece.plugins.platform.service.rag.parser.MistralOcrParser;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Pipeline node that parses documents using Azure Document Intelligence.
 */
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_DOCUMENT_PARSER, name = "Document Parser", description = "Extrait le texte de documents (PDF, DOCX, XLSX, images) via un fournisseur Document Intelligence. Retourne le contenu textuel structuré." )
public class DocumentParserNode extends AbstractFileProcessingNode
{

    private static final String [ ] ACCEPTED_TYPES = {
            "application/pdf", "image/jpeg", "image/png", "image/bmp", "image/tiff", "image/heif",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", "text/html"
    };

    /**
     * Default constructor.
     */
    public DocumentParserNode( )
    {
        super( PipelineConstants.NODE_TYPE_DOCUMENT_PARSER );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String processFile( byte [ ] bytes, String fileName, Provider provider, String executionId, String nodeId )
    {
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            MistralOcrParser parser = CDI.current( ).select( MistralOcrParser.class ).get( );
            return parser.analyzeDocument( bytes, MistralOcrParser.detectContentType( fileName ), provider, executionId ).text( );
        }

        AzureParser parser = CDI.current( ).select( AzureParser.class ).get( );
        return parser.analyzeDocument( bytes, provider, null, executionId ).text( );
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
        return ProviderTypeConstants.PROVIDER_TYPE_DOCUMENT_INTELLIGENCE;
    }
}
