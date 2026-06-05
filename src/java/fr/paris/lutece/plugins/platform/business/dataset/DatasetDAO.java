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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import fr.paris.lutece.util.ReferenceList;

/**
 * Data Access Object implementation for Dataset objects
 */
@ApplicationScoped
@Named( "platform.datasetDAO" )
public class DatasetDAO implements IDatasetDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_dataset (dataset_name, dataset_description, embed_provider_id, llm_provider_id, client_id, dataset_routing_rules) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_dataset WHERE dataset_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_dataset SET dataset_name = ?, dataset_description = ?, embed_provider_id = ?, llm_provider_id = ?, client_id = ?, dataset_routing_rules = ?, updated_at = CURRENT_TIMESTAMP WHERE dataset_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT dataset_id, dataset_name, dataset_description, embed_provider_id, llm_provider_id, client_id, dataset_routing_rules, created_at, updated_at FROM platform_dataset";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE dataset_id = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE client_id = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT dataset_id, dataset_name FROM platform_dataset ORDER BY dataset_name";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( Dataset dataset, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, dataset.getDatasetName( ) );
            daoUtil.setString( nIndex++, dataset.getDatasetDescription( ) );
            daoUtil.setInt( nIndex++, dataset.getEmbedProviderId( ) );
            daoUtil.setInt( nIndex++, dataset.getLlmProviderId( ) );
            daoUtil.setInt( nIndex++, dataset.getClientId( ) );
            daoUtil.setString( nIndex++, dataset.getDatasetRoutingRules( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                dataset.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Dataset> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Dataset dataset = null;
            if ( daoUtil.next( ) )
            {
                dataset = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( dataset );
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
    public void store( Dataset dataset, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, dataset.getDatasetName( ) );
            daoUtil.setString( nIndex++, dataset.getDatasetDescription( ) );
            daoUtil.setInt( nIndex++, dataset.getEmbedProviderId( ) );
            daoUtil.setInt( nIndex++, dataset.getLlmProviderId( ) );
            daoUtil.setInt( nIndex++, dataset.getClientId( ) );
            daoUtil.setString( nIndex++, dataset.getDatasetRoutingRules( ) );
            daoUtil.setInt( nIndex, dataset.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Dataset> selectDatasetsList( Plugin plugin )
    {
        List<Dataset> datasetList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                datasetList.add( loadFromDaoUtil( daoUtil ) );
            }
            return datasetList;
        }
    }

    /**
     * Selects datasets by client ID
     *
     * @param nClientId
     *            The client ID
     * @param plugin
     *            The plugin
     * @return The list of datasets for the specified client
     */
    @Override
    public List<Dataset> selectDatasetsByClientId( int nClientId, Plugin plugin )
    {
        List<Dataset> datasetList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nClientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                datasetList.add( loadFromDaoUtil( daoUtil ) );
            }
            return datasetList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList selectDatasetsReferenceList( Plugin plugin )
    {
        ReferenceList referenceList = new ReferenceList( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_REFERENCE_LIST, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                referenceList.addItem( daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
        }
        return referenceList;
    }

    /**
     * Loads a dataset object from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object with the dataset data
     * @return The Dataset object
     */
    private Dataset loadFromDaoUtil( DAOUtil daoUtil )
    {
        Dataset dataset = new Dataset( );
        int nIndex = 1;
        dataset.setId( daoUtil.getInt( nIndex++ ) );
        dataset.setDatasetName( daoUtil.getString( nIndex++ ) );
        dataset.setDatasetDescription( daoUtil.getString( nIndex++ ) );
        dataset.setEmbedProviderId( daoUtil.getInt( nIndex++ ) );
        dataset.setLlmProviderId( daoUtil.getInt( nIndex++ ) );
        dataset.setClientId( daoUtil.getInt( nIndex++ ) );
        dataset.setDatasetRoutingRules( daoUtil.getString( nIndex++ ) );
        dataset.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        dataset.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return dataset;
    }
}
