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
package fr.paris.lutece.plugins.platform.business.model;

import java.io.Serializable;
import java.sql.Timestamp;
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;
import fr.paris.lutece.plugins.platform.business.provider.Provider;

/**
 * Model class representing a model resource mapping to a provider
 */
public class Model implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "MODEL";

    private int _nId;
    private int _nClientId;
    private int _nProviderId;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    private transient Provider _provider;

    /**
     * Returns the model ID
     *
     * @return The model ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the model ID
     *
     * @param nId
     *            The model ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the client ID
     *
     * @return The client ID
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Sets the client ID
     *
     * @param nClientId
     *            The client ID
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Returns the provider ID
     *
     * @return The provider ID
     */
    public int getProviderId( )
    {
        return _nProviderId;
    }

    /**
     * Sets the provider ID
     *
     * @param nProviderId
     *            The provider ID
     */
    public void setProviderId( int nProviderId )
    {
        _nProviderId = nProviderId;
    }

    /**
     * Returns the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Checks if the current user can view this model
     *
     * @return true if user can view this model
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this model
     *
     * @param userCanView
     *            true if user can view this model
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this model
     *
     * @return true if user can modify this model
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this model
     *
     * @param userCanModify
     *            true if user can modify this model
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this model
     *
     * @return true if user can delete this model
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this model
     *
     * @param userCanDelete
     *            true if user can delete this model
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * Returns the associated provider (transient, not persisted)
     *
     * @return The provider
     */
    public Provider getProvider( )
    {
        return _provider;
    }

    /**
     * Sets the associated provider (transient, not persisted)
     *
     * @param provider
     *            The provider
     */
    public void setProvider( Provider provider )
    {
        _provider = provider;
    }

    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    @Override
    public String getResourceId( )
    {
        return String.valueOf( _nId );
    }
}
