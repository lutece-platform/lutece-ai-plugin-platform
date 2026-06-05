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
 * This is the business class for the object PipelineTrigger
 */
public class PipelineTrigger implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nIdTrigger;
    private int _nIdPipeline;
    private int _nIdClient;
    private String _strName;
    private String _strTriggerType;
    private String _strConfiguration;
    private String _strInputData;
    private boolean _bActive;
    private Timestamp _tsLastTriggeredAt;
    private Timestamp _tsCreatedAt;

    /**
     * Returns the ID
     *
     * @return The ID
     */
    public int getId( )
    {
        return _nIdTrigger;
    }

    /**
     * Sets the ID
     *
     * @param nIdTrigger
     *            The ID
     */
    public void setId( int nIdTrigger )
    {
        _nIdTrigger = nIdTrigger;
    }

    /**
     * Returns the pipeline ID
     *
     * @return The pipeline ID
     */
    public int getIdPipeline( )
    {
        return _nIdPipeline;
    }

    /**
     * Sets the pipeline ID
     *
     * @param nIdPipeline
     *            The pipeline ID
     */
    public void setIdPipeline( int nIdPipeline )
    {
        _nIdPipeline = nIdPipeline;
    }

    /**
     * Returns the client ID
     *
     * @return The client ID
     */
    public int getIdClient( )
    {
        return _nIdClient;
    }

    /**
     * Sets the client ID
     *
     * @param nIdClient
     *            The client ID
     */
    public void setIdClient( int nIdClient )
    {
        _nIdClient = nIdClient;
    }

    /**
     * Returns the Name
     *
     * @return The Name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the Name
     *
     * @param strName
     *            The Name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the trigger type
     *
     * @return The trigger type
     */
    public String getTriggerType( )
    {
        return _strTriggerType;
    }

    /**
     * Sets the trigger type
     *
     * @param strTriggerType
     *            The trigger type
     */
    public void setTriggerType( String strTriggerType )
    {
        _strTriggerType = strTriggerType;
    }

    /**
     * Returns the configuration JSON
     *
     * @return The configuration JSON
     */
    public String getConfiguration( )
    {
        return _strConfiguration;
    }

    /**
     * Sets the configuration JSON
     *
     * @param strConfiguration
     *            The configuration JSON
     */
    public void setConfiguration( String strConfiguration )
    {
        _strConfiguration = strConfiguration;
    }

    /**
     * Returns the input data JSON
     *
     * @return The input data JSON
     */
    public String getInputData( )
    {
        return _strInputData;
    }

    /**
     * Sets the input data JSON
     *
     * @param strInputData
     *            The input data JSON
     */
    public void setInputData( String strInputData )
    {
        _strInputData = strInputData;
    }

    /**
     * Returns whether the trigger is active
     *
     * @return true if active
     */
    public boolean isActive( )
    {
        return _bActive;
    }

    /**
     * Sets whether the trigger is active
     *
     * @param bActive
     *            true if active
     */
    public void setActive( boolean bActive )
    {
        _bActive = bActive;
    }

    /**
     * Returns the last triggered timestamp
     *
     * @return The last triggered timestamp
     */
    public Timestamp getLastTriggeredAt( )
    {
        return _tsLastTriggeredAt;
    }

    /**
     * Sets the last triggered timestamp
     *
     * @param tsLastTriggeredAt
     *            The last triggered timestamp
     */
    public void setLastTriggeredAt( Timestamp tsLastTriggeredAt )
    {
        _tsLastTriggeredAt = tsLastTriggeredAt;
    }

    /**
     * Returns the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _tsCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param tsCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp tsCreatedAt )
    {
        _tsCreatedAt = tsCreatedAt;
    }
}
