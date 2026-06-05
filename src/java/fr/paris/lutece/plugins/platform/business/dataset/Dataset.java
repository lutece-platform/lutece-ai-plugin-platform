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
package fr.paris.lutece.plugins.platform.business.dataset;

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.validation.constraints.NotEmpty;
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;

/**
 * Dataset class representing a dataset entity in the platform agent module
 */
public class Dataset implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "DATASET";

    private int _nId;
    @NotEmpty( message = "#i18n{platform.agent.validation.dataset.DatasetName.notEmpty}" )
    private String _strDatasetName;
    private String _strDatasetDescription;
    private int _nEmbedProviderId;
    private int _nLlmProviderId;
    private int _nClientId;
    private String _strDatasetRoutingRules;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    /**
     * Get the dataset ID
     *
     * @return The dataset ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Set the dataset ID
     *
     * @param nId
     *            The dataset ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Get the dataset name
     *
     * @return The dataset name
     */
    public String getDatasetName( )
    {
        return _strDatasetName;
    }

    /**
     * Set the dataset name
     *
     * @param strDatasetName
     *            The dataset name
     */
    public void setDatasetName( String strDatasetName )
    {
        _strDatasetName = strDatasetName;
    }

    /**
     * Get the dataset description
     *
     * @return The dataset description
     */
    public String getDatasetDescription( )
    {
        return _strDatasetDescription;
    }

    /**
     * Set the dataset description
     *
     * @param strDatasetDescription
     *            The dataset description
     */
    public void setDatasetDescription( String strDatasetDescription )
    {
        _strDatasetDescription = strDatasetDescription;
    }

    /**
     * Get the embed provider ID
     *
     * @return The embed provider ID
     */
    public int getEmbedProviderId( )
    {
        return _nEmbedProviderId;
    }

    /**
     * Set the embed provider ID
     *
     * @param nEmbedProviderId
     *            The embed provider ID
     */
    public void setEmbedProviderId( int nEmbedProviderId )
    {
        _nEmbedProviderId = nEmbedProviderId;
    }

    /**
     * Get the LLM provider ID
     *
     * @return The LLM provider ID
     */
    public int getLlmProviderId( )
    {
        return _nLlmProviderId;
    }

    /**
     * Set the LLM provider ID
     *
     * @param nLlmProviderId
     *            The LLM provider ID
     */
    public void setLlmProviderId( int nLlmProviderId )
    {
        _nLlmProviderId = nLlmProviderId;
    }

    /**
     * Get the client ID
     *
     * @return The client ID
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Set the client ID
     *
     * @param nClientId
     *            The client ID
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Get the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Set the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Get the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Set the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Get the dataset routing rules
     *
     * @return The dataset routing rules
     */
    public String getDatasetRoutingRules( )
    {
        return _strDatasetRoutingRules;
    }

    /**
     * Set the dataset routing rules
     *
     * @param strDatasetRoutingRules
     *            The dataset routing rules
     */
    public void setDatasetRoutingRules( String strDatasetRoutingRules )
    {
        _strDatasetRoutingRules = strDatasetRoutingRules;
    }

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    private String _strClientName;

    /**
     * Checks if the current user can view this dataset
     *
     * @return true if user can view this dataset
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this dataset
     *
     * @param userCanView
     *            true if user can view this dataset
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this dataset
     *
     * @return true if user can modify this dataset
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this dataset
     *
     * @param userCanModify
     *            true if user can modify this dataset
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this dataset
     *
     * @return true if user can delete this dataset
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this dataset
     *
     * @param userCanDelete
     *            true if user can delete this dataset
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * Gets the client name for display purposes
     *
     * @return The client name
     */
    public String getClientName( )
    {
        return _strClientName;
    }

    /**
     * Sets the client name for display purposes
     *
     * @param strClientName
     *            The client name
     */
    public void setClientName( String strClientName )
    {
        _strClientName = strClientName;
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
        return String.valueOf( _nId );
    }
}
