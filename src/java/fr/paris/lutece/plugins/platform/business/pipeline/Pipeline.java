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
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;

/**
 * This is the business class for the object Pipeline
 */
public class Pipeline implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "PIPELINE";

    private int _nIdPipeline;
    private String _strName;
    private String _strDescription;
    private int _nMaxConcurrentWorkers;
    private int _nIdClient;
    private int _nRateLimitByUserByDay = 0;

    /**
     * Returns the ID
     *
     * @return The ID
     */
    public int getId( )
    {
        return _nIdPipeline;
    }

    /**
     * Sets the ID
     *
     * @param nIdPipeline
     *            The ID
     */
    public void setId( int nIdPipeline )
    {
        _nIdPipeline = nIdPipeline;
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
     * Returns the Description
     *
     * @return The Description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the Description
     *
     * @param strDescription
     *            The Description
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the MaxConcurrentWorkers
     *
     * @return The MaxConcurrentWorkers
     */
    public int getMaxConcurrentWorkers( )
    {
        return _nMaxConcurrentWorkers;
    }

    /**
     * Sets the MaxConcurrentWorkers
     *
     * @param nMaxConcurrentWorkers
     *            The MaxConcurrentWorkers
     */
    public void setMaxConcurrentWorkers( int nMaxConcurrentWorkers )
    {
        _nMaxConcurrentWorkers = nMaxConcurrentWorkers;
    }

    /**
     * Returns the IdClient
     *
     * @return The IdClient
     */
    public int getIdClient( )
    {
        return _nIdClient;
    }

    /**
     * Sets the IdClient
     *
     * @param nIdClient
     *            The IdClient
     */
    public void setIdClient( int nIdClient )
    {
        _nIdClient = nIdClient;
    }

    /**
     * Returns the rate limit by user by day
     *
     * @return The rate limit by user by day
     */
    public int getRateLimitByUserByDay( )
    {
        return _nRateLimitByUserByDay;
    }

    /**
     * Sets the rate limit by user by day
     *
     * @param nRateLimitByUserByDay
     *            The rate limit by user by day
     */
    public void setRateLimitByUserByDay( int nRateLimitByUserByDay )
    {
        _nRateLimitByUserByDay = nRateLimitByUserByDay;
    }

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    /**
     * Checks if the current user can view this pipeline
     *
     * @return true if user can view this pipeline
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this pipeline
     *
     * @param userCanView
     *            true if user can view this pipeline
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this pipeline
     *
     * @return true if user can modify this pipeline
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this pipeline
     *
     * @param userCanModify
     *            true if user can modify this pipeline
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this pipeline
     *
     * @return true if user can delete this pipeline
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this pipeline
     *
     * @param userCanDelete
     *            true if user can delete this pipeline
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceId( )
    {
        return String.valueOf( _nIdPipeline );
    }
}
