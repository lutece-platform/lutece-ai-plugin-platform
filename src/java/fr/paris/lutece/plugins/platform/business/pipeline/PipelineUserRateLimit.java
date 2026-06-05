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
 * Pipeline User Rate Limit entity for tracking user execution limits per pipeline
 */
public class PipelineUserRateLimit implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private String _strUserId;
    private int _nPipelineId;
    private int _nExecutionCount;
    private Timestamp _timestampDateFirstExecution;

    /**
     * Gets the ID
     *
     * @return the ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the ID
     *
     * @param nId
     *            the ID to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the user ID
     *
     * @return the user ID
     */
    public String getUserId( )
    {
        return _strUserId;
    }

    /**
     * Sets the user ID
     *
     * @param strUserId
     *            the user ID to set
     */
    public void setUserId( String strUserId )
    {
        _strUserId = strUserId;
    }

    /**
     * Gets the pipeline ID
     *
     * @return the pipeline ID
     */
    public int getPipelineId( )
    {
        return _nPipelineId;
    }

    /**
     * Sets the pipeline ID
     *
     * @param nPipelineId
     *            the pipeline ID to set
     */
    public void setPipelineId( int nPipelineId )
    {
        _nPipelineId = nPipelineId;
    }

    /**
     * Gets the execution count
     *
     * @return the execution count
     */
    public int getExecutionCount( )
    {
        return _nExecutionCount;
    }

    /**
     * Sets the execution count
     *
     * @param nExecutionCount
     *            the execution count to set
     */
    public void setExecutionCount( int nExecutionCount )
    {
        _nExecutionCount = nExecutionCount;
    }

    /**
     * Gets the date of first execution
     *
     * @return the date of first execution
     */
    public Timestamp getDateFirstExecution( )
    {
        return _timestampDateFirstExecution;
    }

    /**
     * Sets the date of first execution
     *
     * @param timestampDateFirstExecution
     *            the date of first execution to set
     */
    public void setDateFirstExecution( Timestamp timestampDateFirstExecution )
    {
        _timestampDateFirstExecution = timestampDateFirstExecution;
    }
}
