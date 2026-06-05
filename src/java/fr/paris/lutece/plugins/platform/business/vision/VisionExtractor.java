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
package fr.paris.lutece.plugins.platform.business.vision;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import jakarta.validation.constraints.NotEmpty;

public class VisionExtractor implements Serializable
{

    private static final long serialVersionUID = 1L;
    private static final String VALIDATION_EXTRACTOR_NAME_NOT_EMPTY = "#i18n{platform.agent.validation.visionextractor.ExtractorName.notEmpty}";

    private int _nId;
    private int _nVisionId;

    @NotEmpty( message = VALIDATION_EXTRACTOR_NAME_NOT_EMPTY )
    private String _strExtractorName;
    private String _strExtractorDescription;
    private List<VisionExtractorField> _fields = new ArrayList<>( );

    /**
     * Gets the extractor ID
     *
     * @return the extractor ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the extractor ID
     *
     * @param nId
     *            the extractor ID to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the vision ID
     *
     * @return the vision ID
     */
    public int getVisionId( )
    {
        return _nVisionId;
    }

    /**
     * Sets the vision ID
     *
     * @param nVisionId
     *            the vision ID to set
     */
    public void setVisionId( int nVisionId )
    {
        _nVisionId = nVisionId;
    }

    /**
     * Gets the extractor name
     *
     * @return the extractor name
     */
    public String getExtractorName( )
    {
        return _strExtractorName;
    }

    /**
     * Sets the extractor name
     *
     * @param strExtractorName
     *            the extractor name to set
     */
    public void setExtractorName( String strExtractorName )
    {
        _strExtractorName = strExtractorName;
    }

    /**
     * Gets the extractor description
     *
     * @return the extractor description
     */
    public String getExtractorDescription( )
    {
        return _strExtractorDescription;
    }

    /**
     * Sets the extractor description
     *
     * @param strExtractorDescription
     *            the extractor description to set
     */
    public void setExtractorDescription( String strExtractorDescription )
    {
        _strExtractorDescription = strExtractorDescription;
    }

    /**
     * Gets the list of fields
     *
     * @return the list of fields
     */
    public List<VisionExtractorField> getFields( )
    {
        return _fields;
    }

    /**
     * Sets the list of fields
     *
     * @param fields
     *            the list of fields to set
     */
    public void setFields( List<VisionExtractorField> fields )
    {
        _fields = fields;
    }
}
