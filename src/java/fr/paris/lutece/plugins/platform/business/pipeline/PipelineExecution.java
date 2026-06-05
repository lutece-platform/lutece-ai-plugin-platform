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
 * PipelineExecution business class
 */
public class PipelineExecution implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nIdExecution;
    private int _nIdPipeline;
    private int _nIdClient;
    private String _strExecutionId;
    private Timestamp _dateCreation;
    private Timestamp _dateCompletion;
    private String _strStatus;
    private String _strInputs;
    private String _strOutputs;
    private String _strError;
    private String _strUserId;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    /**
     * Get execution ID
     *
     * @return The execution ID
     */
    public int getId( )
    {
        return _nIdExecution;
    }

    /**
     * Set execution ID
     *
     * @param nIdExecution
     *            The execution ID
     */
    public void setId( int nIdExecution )
    {
        _nIdExecution = nIdExecution;
    }

    /**
     * Get pipeline ID
     *
     * @return The pipeline ID
     */
    public int getIdPipeline( )
    {
        return _nIdPipeline;
    }

    /**
     * Set pipeline ID
     *
     * @param nIdPipeline
     *            The pipeline ID
     */
    public void setIdPipeline( int nIdPipeline )
    {
        _nIdPipeline = nIdPipeline;
    }

    /**
     * Get client ID
     *
     * @return The client ID
     */
    public int getIdClient( )
    {
        return _nIdClient;
    }

    /**
     * Set client ID
     *
     * @param nIdClient
     *            The client ID
     */
    public void setIdClient( int nIdClient )
    {
        _nIdClient = nIdClient;
    }

    /**
     * Get execution ID string
     *
     * @return The execution ID string
     */
    public String getExecutionId( )
    {
        return _strExecutionId;
    }

    /**
     * Set execution ID string
     *
     * @param strExecutionId
     *            The execution ID string
     */
    public void setExecutionId( String strExecutionId )
    {
        _strExecutionId = strExecutionId;
    }

    /**
     * Get creation date
     *
     * @return The creation date
     */
    public Timestamp getCreationDate( )
    {
        return _dateCreation;
    }

    /**
     * Set creation date
     *
     * @param dateCreation
     *            The creation date
     */
    public void setCreationDate( Timestamp dateCreation )
    {
        _dateCreation = dateCreation;
    }

    /**
     * Get completion date
     *
     * @return The completion date
     */
    public Timestamp getCompletionDate( )
    {
        return _dateCompletion;
    }

    /**
     * Set completion date
     *
     * @param dateCompletion
     *            The completion date
     */
    public void setCompletionDate( Timestamp dateCompletion )
    {
        _dateCompletion = dateCompletion;
    }

    /**
     * Get status
     *
     * @return The status
     */
    public String getStatus( )
    {
        return _strStatus;
    }

    /**
     * Set status
     *
     * @param strStatus
     *            The status
     */
    public void setStatus( String strStatus )
    {
        _strStatus = strStatus;
    }

    /**
     * Get inputs
     *
     * @return The inputs
     */
    public String getInputs( )
    {
        return _strInputs;
    }

    /**
     * Set inputs
     *
     * @param strInputs
     *            The inputs
     */
    public void setInputs( String strInputs )
    {
        _strInputs = strInputs;
    }

    /**
     * Get outputs
     *
     * @return The outputs
     */
    public String getOutputs( )
    {
        return _strOutputs;
    }

    /**
     * Set outputs
     *
     * @param strOutputs
     *            The outputs
     */
    public void setOutputs( String strOutputs )
    {
        _strOutputs = strOutputs;
    }

    /**
     * Get error
     *
     * @return The error
     */
    public String getError( )
    {
        return _strError;
    }

    /**
     * Set error
     *
     * @param strError
     *            The error
     */
    public void setError( String strError )
    {
        _strError = strError;
    }

    /**
     * Returns the user identifier.
     *
     * @return The user ID
     */
    public String getUserId( )
    {
        return _strUserId;
    }

    /**
     * Sets the user identifier.
     *
     * @param strUserId
     *            The user ID
     */
    public void setUserId( String strUserId )
    {
        _strUserId = strUserId;
    }
}
