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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of the IProviderDAO interface for JDBC access
 */
@ApplicationScoped
@Named( "platform.providerDAO" )
public class ProviderDAO implements IProviderDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_provider (provider_name, provider_description, provider_type, provider_vendor, deployment_name, deployment_model_name, deployment_endpoint, deployment_api_version, deployment_api_key, token_input_price_1m, token_output_price_1m, document_analysis_price_1000_pages) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_provider WHERE provider_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_provider SET provider_name = ?, provider_description = ?, provider_type = ?, provider_vendor = ?, deployment_name = ?, deployment_model_name = ?, deployment_endpoint = ?, deployment_api_version = ?, deployment_api_key = ?, token_input_price_1m = ?, token_output_price_1m = ?, document_analysis_price_1000_pages = ?, updated_at = CURRENT_TIMESTAMP WHERE provider_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT provider_id, provider_name, provider_description, provider_type, provider_vendor, deployment_name, deployment_model_name, deployment_endpoint, deployment_api_version, deployment_api_key, token_input_price_1m, token_output_price_1m, document_analysis_price_1000_pages, created_at, updated_at FROM platform_provider";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE provider_id = ?";
    private static final String SQL_QUERY_SELECT_BY_TYPE = SQL_QUERY_SELECTALL + " WHERE provider_type = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( Provider provider, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, provider.getProviderName( ) );
            daoUtil.setString( nIndex++, provider.getProviderDescription( ) );
            daoUtil.setString( nIndex++, provider.getProviderType( ) );
            daoUtil.setString( nIndex++, provider.getProviderVendor( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentName( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentModelName( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentEndpoint( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentApiVersion( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentApiKey( ) );
            daoUtil.setDouble( nIndex++, provider.getTokenInputPrice1M( ) );
            daoUtil.setDouble( nIndex++, provider.getTokenOutputPrice1M( ) );
            if ( provider.getDocumentAnalysisPrice1000Pages( ) != null )
            {
                daoUtil.setDouble( nIndex++, provider.getDocumentAnalysisPrice1000Pages( ) );
            }
            else
            {
                daoUtil.setDoubleNull( nIndex++ );
            }
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                provider.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Provider> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Provider provider = null;
            if ( daoUtil.next( ) )
            {
                provider = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( provider );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( Provider provider, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, provider.getProviderName( ) );
            daoUtil.setString( nIndex++, provider.getProviderDescription( ) );
            daoUtil.setString( nIndex++, provider.getProviderType( ) );
            daoUtil.setString( nIndex++, provider.getProviderVendor( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentName( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentModelName( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentEndpoint( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentApiVersion( ) );
            daoUtil.setString( nIndex++, provider.getDeploymentApiKey( ) );
            daoUtil.setDouble( nIndex++, provider.getTokenInputPrice1M( ) );
            daoUtil.setDouble( nIndex++, provider.getTokenOutputPrice1M( ) );
            if ( provider.getDocumentAnalysisPrice1000Pages( ) != null )
            {
                daoUtil.setDouble( nIndex++, provider.getDocumentAnalysisPrice1000Pages( ) );
            }
            else
            {
                daoUtil.setDoubleNull( nIndex++ );
            }
            daoUtil.setInt( nIndex, provider.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Provider> selectProvidersList( Plugin plugin )
    {
        List<Provider> providerList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                providerList.add( loadFromDaoUtil( daoUtil ) );
            }
            return providerList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Provider> selectProvidersByType( String strType, Plugin plugin )
    {
        List<Provider> providerList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_TYPE, plugin ) )
        {
            daoUtil.setString( 1, strType );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                providerList.add( loadFromDaoUtil( daoUtil ) );
            }
            return providerList;
        }
    }

    /**
     * Creates a Provider object from the current position of the DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object
     * @return The Provider object
     */
    private Provider loadFromDaoUtil( DAOUtil daoUtil )
    {
        Provider provider = new Provider( );
        int nIndex = 1;
        provider.setId( daoUtil.getInt( nIndex++ ) );
        provider.setProviderName( daoUtil.getString( nIndex++ ) );
        provider.setProviderDescription( daoUtil.getString( nIndex++ ) );
        provider.setProviderType( daoUtil.getString( nIndex++ ) );
        provider.setProviderVendor( daoUtil.getString( nIndex++ ) );
        provider.setDeploymentName( daoUtil.getString( nIndex++ ) );
        provider.setDeploymentModelName( daoUtil.getString( nIndex++ ) );
        provider.setDeploymentEndpoint( daoUtil.getString( nIndex++ ) );
        provider.setDeploymentApiVersion( daoUtil.getString( nIndex++ ) );
        provider.setDeploymentApiKey( daoUtil.getString( nIndex++ ) );
        provider.setTokenInputPrice1M( daoUtil.getDouble( nIndex++ ) );
        provider.setTokenOutputPrice1M( daoUtil.getDouble( nIndex++ ) );
        Double documentAnalysisPrice = daoUtil.getObject( nIndex++, Double.class );
        provider.setDocumentAnalysisPrice1000Pages( documentAnalysisPrice );
        provider.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        provider.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return provider;
    }
}
