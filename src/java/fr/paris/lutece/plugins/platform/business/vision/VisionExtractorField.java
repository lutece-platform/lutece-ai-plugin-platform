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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class VisionExtractorField implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nExtractorId;

    @NotEmpty( message = "#i18n{platform.agent.validation.visionextractorfield.FieldName.notEmpty}" )
    private String _strFieldName;
    private String _strFieldDescription;

    @NotNull( message = "#i18n{platform.agent.validation.visionextractorfield.FieldType.notNull}" )
    private VisionFieldType _fieldType;

    /**
     * Gets the id
     *
     * @return the id
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the id
     *
     * @param nId
     *            the id to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the extractor id
     *
     * @return the extractor id
     */
    public int getExtractorId( )
    {
        return _nExtractorId;
    }

    /**
     * Sets the extractor id
     *
     * @param nExtractorId
     *            the extractor id to set
     */
    public void setExtractorId( int nExtractorId )
    {
        _nExtractorId = nExtractorId;
    }

    /**
     * Gets the field name
     *
     * @return the field name
     */
    public String getFieldName( )
    {
        return _strFieldName;
    }

    /**
     * Sets the field name
     *
     * @param strFieldName
     *            the field name to set
     */
    public void setFieldName( String strFieldName )
    {
        _strFieldName = strFieldName;
    }

    /**
     * Gets the field description
     *
     * @return the field description
     */
    public String getFieldDescription( )
    {
        return _strFieldDescription;
    }

    /**
     * Sets the field description
     *
     * @param strFieldDescription
     *            the field description to set
     */
    public void setFieldDescription( String strFieldDescription )
    {
        _strFieldDescription = strFieldDescription;
    }

    /**
     * Gets the field type
     *
     * @return the field type
     */
    public VisionFieldType getFieldType( )
    {
        return _fieldType;
    }

    /**
     * Sets the field type
     *
     * @param fieldType
     *            the field type to set
     */
    public void setFieldType( VisionFieldType fieldType )
    {
        _fieldType = fieldType;
    }
}
