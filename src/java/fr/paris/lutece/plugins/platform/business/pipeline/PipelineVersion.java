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
package fr.paris.lutece.plugins.platform.business.pipeline;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Pipeline Version entity
 */
public class PipelineVersion implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nIdVersion;
    private int _nIdPipeline;
    private String _strFlow;
    private String _strVersionName;
    private String _strDescription;
    private Timestamp _dateCreation;
    private boolean _bCurrent;
    private String _strInputSchema;

    /**
     * Returns the ID of the version
     *
     * @return The ID of the version
     */
    public int getId( )
    {
        return _nIdVersion;
    }

    /**
     * Sets the ID of the version
     *
     * @param nIdVersion
     *            The ID of the version
     */
    public void setId( int nIdVersion )
    {
        _nIdVersion = nIdVersion;
    }

    /**
     * Returns the ID of the pipeline
     *
     * @return The ID of the pipeline
     */
    public int getIdPipeline( )
    {
        return _nIdPipeline;
    }

    /**
     * Sets the ID of the pipeline
     *
     * @param nIdPipeline
     *            The ID of the pipeline
     */
    public void setIdPipeline( int nIdPipeline )
    {
        _nIdPipeline = nIdPipeline;
    }

    /**
     * Returns the flow definition
     *
     * @return The flow definition
     */
    public String getFlow( )
    {
        return _strFlow;
    }

    /**
     * Sets the flow definition
     *
     * @param strFlow
     *            The flow definition
     */
    public void setFlow( String strFlow )
    {
        _strFlow = strFlow;
    }

    /**
     * Returns the version name
     *
     * @return The version name
     */
    public String getVersionName( )
    {
        return _strVersionName;
    }

    /**
     * Sets the version name
     *
     * @param strVersionName
     *            The version name
     */
    public void setVersionName( String strVersionName )
    {
        _strVersionName = strVersionName;
    }

    /**
     * Returns the description
     *
     * @return The description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the description
     *
     * @param strDescription
     *            The description
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the creation date
     *
     * @return The creation date
     */
    public Timestamp getCreationDate( )
    {
        return _dateCreation;
    }

    /**
     * Sets the creation date
     *
     * @param dateCreation
     *            The creation date
     */
    public void setCreationDate( Timestamp dateCreation )
    {
        _dateCreation = dateCreation;
    }

    /**
     * Returns whether this version is the current one
     *
     * @return True if it's the current version, false otherwise
     */
    public boolean isCurrent( )
    {
        return _bCurrent;
    }

    /**
     * Sets whether this version is the current one
     *
     * @param bCurrent
     *            True if it's the current version, false otherwise
     */
    public void setCurrent( boolean bCurrent )
    {
        _bCurrent = bCurrent;
    }

    /**
     * Returns the input schema JSON definition.
     *
     * @return The input schema JSON
     */
    public String getInputSchema( )
    {
        return _strInputSchema;
    }

    /**
     * Sets the input schema JSON definition.
     *
     * @param strInputSchema
     *            The input schema JSON
     */
    public void setInputSchema( String strInputSchema )
    {
        _strInputSchema = strInputSchema;
    }
}
