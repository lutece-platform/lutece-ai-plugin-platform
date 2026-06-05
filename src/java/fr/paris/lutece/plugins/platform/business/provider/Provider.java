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
package fr.paris.lutece.plugins.platform.business.provider;

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.validation.constraints.NotEmpty;

/**
 * This class represents a provider in the platform
 */
public class Provider implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    @NotEmpty( message = "#i18n{platform.agent.validation.provider.ProviderName.notEmpty}" )
    private String _strProviderName;
    private String _strProviderDescription;
    private String _strProviderType;
    private String _strProviderVendor;
    private String _strDeploymentName;
    private String _strDeploymentModelName;
    private String _strDeploymentEndpoint;
    private String _strDeploymentApiVersion;
    private String _strDeploymentApiKey;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;
    private double _dTokenInputPrice1M;
    private double _dTokenOutputPrice1M;
    private Double _dDocumentAnalysisPrice1000Pages;

    /**
     * Returns the provider ID
     *
     * @return The provider ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the provider ID
     *
     * @param nId
     *            The provider ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the provider name
     *
     * @return The provider name
     */
    public String getProviderName( )
    {
        return _strProviderName;
    }

    /**
     * Sets the provider name
     *
     * @param strProviderName
     *            The provider name
     */
    public void setProviderName( String strProviderName )
    {
        _strProviderName = strProviderName;
    }

    /**
     * Returns the provider description
     *
     * @return The provider description
     */
    public String getProviderDescription( )
    {
        return _strProviderDescription;
    }

    /**
     * Sets the provider description
     *
     * @param strProviderDescription
     *            The provider description
     */
    public void setProviderDescription( String strProviderDescription )
    {
        _strProviderDescription = strProviderDescription;
    }

    /**
     * Returns the provider type
     *
     * @return The provider type
     */
    public String getProviderType( )
    {
        return _strProviderType;
    }

    /**
     * Sets the provider type
     *
     * @param strProviderType
     *            The provider type
     */
    public void setProviderType( String strProviderType )
    {
        _strProviderType = strProviderType;
    }

    /**
     * Returns the provider vendor
     *
     * @return The provider vendor
     */
    public String getProviderVendor( )
    {
        return _strProviderVendor;
    }

    /**
     * Sets the provider vendor
     *
     * @param strProviderVendor
     *            The provider vendor
     */
    public void setProviderVendor( String strProviderVendor )
    {
        _strProviderVendor = strProviderVendor;
    }

    /**
     * Returns the deployment name
     *
     * @return The deployment name
     */
    public String getDeploymentName( )
    {
        return _strDeploymentName;
    }

    /**
     * Sets the deployment name
     *
     * @param strDeploymentName
     *            The deployment name
     */
    public void setDeploymentName( String strDeploymentName )
    {
        _strDeploymentName = strDeploymentName;
    }

    /**
     * Returns the deployment model name
     *
     * @return The deployment model name
     */
    public String getDeploymentModelName( )
    {
        return _strDeploymentModelName;
    }

    /**
     * Sets the deployment model name
     *
     * @param strDeploymentModelName
     *            The deployment model name
     */
    public void setDeploymentModelName( String strDeploymentModelName )
    {
        _strDeploymentModelName = strDeploymentModelName;
    }

    /**
     * Returns the deployment endpoint
     *
     * @return The deployment endpoint
     */
    public String getDeploymentEndpoint( )
    {
        return _strDeploymentEndpoint;
    }

    /**
     * Sets the deployment endpoint
     *
     * @param strDeploymentEndpoint
     *            The deployment endpoint
     */
    public void setDeploymentEndpoint( String strDeploymentEndpoint )
    {
        _strDeploymentEndpoint = strDeploymentEndpoint;
    }

    /**
     * Returns the deployment API version
     *
     * @return The deployment API version
     */
    public String getDeploymentApiVersion( )
    {
        return _strDeploymentApiVersion;
    }

    /**
     * Sets the deployment API version
     *
     * @param strDeploymentApiVersion
     *            The deployment API version
     */
    public void setDeploymentApiVersion( String strDeploymentApiVersion )
    {
        _strDeploymentApiVersion = strDeploymentApiVersion;
    }

    /**
     * Returns the deployment API key
     *
     * @return The deployment API key
     */
    public String getDeploymentApiKey( )
    {
        return _strDeploymentApiKey;
    }

    /**
     * Sets the deployment API key
     *
     * @param strDeploymentApiKey
     *            The deployment API key
     */
    public void setDeploymentApiKey( String strDeploymentApiKey )
    {
        _strDeploymentApiKey = strDeploymentApiKey;
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
     * Returns the token input price per 1M tokens
     *
     * @return The token input price per 1M tokens
     */
    public double getTokenInputPrice1M( )
    {
        return _dTokenInputPrice1M;
    }

    /**
     * Sets the token input price per 1M tokens
     *
     * @param dTokenInputPrice1M
     *            The token input price per 1M tokens
     */
    public void setTokenInputPrice1M( double dTokenInputPrice1M )
    {
        _dTokenInputPrice1M = dTokenInputPrice1M;
    }

    /**
     * Returns the token output price per 1M tokens
     *
     * @return The token output price per 1M tokens
     */
    public double getTokenOutputPrice1M( )
    {
        return _dTokenOutputPrice1M;
    }

    /**
     * Sets the token output price per 1M tokens
     *
     * @param dTokenOutputPrice1M
     *            The token output price per 1M tokens
     */
    public void setTokenOutputPrice1M( double dTokenOutputPrice1M )
    {
        _dTokenOutputPrice1M = dTokenOutputPrice1M;
    }

    /**
     * Returns the document analysis price per 1000 pages
     *
     * @return The document analysis price per 1000 pages
     */
    public Double getDocumentAnalysisPrice1000Pages( )
    {
        return _dDocumentAnalysisPrice1000Pages;
    }

    /**
     * Sets the document analysis price per 1000 pages
     *
     * @param dDocumentAnalysisPrice1000Pages
     *            The document analysis price per 1000 pages
     */
    public void setDocumentAnalysisPrice1000Pages( Double dDocumentAnalysisPrice1000Pages )
    {
        _dDocumentAnalysisPrice1000Pages = dDocumentAnalysisPrice1000Pages;
    }
}
