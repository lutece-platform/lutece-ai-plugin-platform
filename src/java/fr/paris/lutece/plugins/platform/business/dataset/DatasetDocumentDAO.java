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

/**
 * Implementation of the IDatasetDocumentDAO interface
 */
@ApplicationScoped
@Named( "platform.datasetDocumentDAO" )
public class DatasetDocumentDAO implements IDatasetDocumentDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_dataset_document (document_name, document_description, document_file_key, dataset_id, folder_id, full_content, use_document_intelligence, document_intelligence_provider_id, chunk_size, chunk_overlap) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_dataset_document WHERE document_id = ?";
    private static final String SQL_QUERY_DELETE_BY_DATASET = "DELETE FROM platform_dataset_document WHERE dataset_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_dataset_document SET document_name = ?, document_description = ?, document_file_key = ?, dataset_id = ?, folder_id = ?, full_content = ?, use_document_intelligence = ?, document_intelligence_provider_id = ?, chunk_size = ?, chunk_overlap = ? WHERE document_id = ?";
    private static final String SQL_COLUMNS_LIGHT = "document_id, document_name, document_description, document_file_key, dataset_id, folder_id, use_document_intelligence, document_intelligence_provider_id, chunk_size, chunk_overlap";
    private static final String SQL_COLUMNS_FULL = "document_id, document_name, document_description, document_file_key, dataset_id, folder_id, full_content, use_document_intelligence, document_intelligence_provider_id, chunk_size, chunk_overlap";
    private static final String SQL_QUERY_SELECTALL = "SELECT " + SQL_COLUMNS_LIGHT + " FROM platform_dataset_document";
    private static final String SQL_QUERY_SELECT_BY_ID = "SELECT " + SQL_COLUMNS_FULL + " FROM platform_dataset_document WHERE document_id = ?";
    private static final String SQL_QUERY_SELECT_BY_DATASET_ID = SQL_QUERY_SELECTALL + " WHERE dataset_id = ?";
    private static final String SQL_QUERY_SELECT_BY_NAME_AND_DATASET_ID = "SELECT " + SQL_COLUMNS_LIGHT
            + " FROM platform_dataset_document WHERE document_name = ? AND dataset_id = ?";
    private static final String SQL_QUERY_SELECT_BY_NAME_FOLDER_DATASET = "SELECT " + SQL_COLUMNS_LIGHT
            + " FROM platform_dataset_document WHERE document_name = ? AND dataset_id = ? AND folder_id = ?";
    private static final String SQL_QUERY_SELECT_BY_NAME_ROOT_DATASET = "SELECT " + SQL_COLUMNS_LIGHT
            + " FROM platform_dataset_document WHERE document_name = ? AND dataset_id = ? AND folder_id IS NULL";
    private static final String SQL_QUERY_SELECT_BY_FOLDER_ID = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? AND folder_id = ?";
    private static final String SQL_QUERY_SELECT_BY_ROOT = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? AND folder_id IS NULL";
    private static final String SQL_QUERY_REPARENT_TO_FOLDER = "UPDATE platform_dataset_document SET folder_id = ? WHERE dataset_id = ? AND folder_id = ?";
    private static final String SQL_QUERY_REPARENT_TO_ROOT = "UPDATE platform_dataset_document SET folder_id = NULL WHERE dataset_id = ? AND folder_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DatasetDocument datasetDocument, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, datasetDocument.getName( ) );
            daoUtil.setString( nIndex++, datasetDocument.getDescription( ) );
            daoUtil.setString( nIndex++, datasetDocument.getFileKey( ) );
            daoUtil.setInt( nIndex++, datasetDocument.getDatasetId( ) );
            if ( datasetDocument.getFolderId( ) == null )
            {
                daoUtil.setIntNull( nIndex++ );
            }
            else
            {
                daoUtil.setInt( nIndex++, datasetDocument.getFolderId( ) );
            }
            daoUtil.setString( nIndex++, datasetDocument.getFullContent( ) );
            daoUtil.setBoolean( nIndex++, datasetDocument.getUseDocumentIntelligence( ) );
            if ( datasetDocument.getDocumentIntelligenceProviderId( ) != null )
            {
                daoUtil.setInt( nIndex++, datasetDocument.getDocumentIntelligenceProviderId( ) );
            }
            else
            {
                daoUtil.setIntNull( nIndex++ );
            }
            daoUtil.setInt( nIndex++, datasetDocument.getChunkSize( ) );
            daoUtil.setInt( nIndex, datasetDocument.getChunkOverlap( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                datasetDocument.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatasetDocument> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            DatasetDocument datasetDocument = null;
            if ( daoUtil.next( ) )
            {
                datasetDocument = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( datasetDocument );
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
    public void deleteByDatasetId( int nDatasetId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_DATASET, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( DatasetDocument datasetDocument, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, datasetDocument.getName( ) );
            daoUtil.setString( nIndex++, datasetDocument.getDescription( ) );
            daoUtil.setString( nIndex++, datasetDocument.getFileKey( ) );
            daoUtil.setInt( nIndex++, datasetDocument.getDatasetId( ) );
            if ( datasetDocument.getFolderId( ) == null )
            {
                daoUtil.setIntNull( nIndex++ );
            }
            else
            {
                daoUtil.setInt( nIndex++, datasetDocument.getFolderId( ) );
            }
            daoUtil.setString( nIndex++, datasetDocument.getFullContent( ) );
            daoUtil.setBoolean( nIndex++, datasetDocument.getUseDocumentIntelligence( ) );
            if ( datasetDocument.getDocumentIntelligenceProviderId( ) != null )
            {
                daoUtil.setInt( nIndex++, datasetDocument.getDocumentIntelligenceProviderId( ) );
            }
            else
            {
                daoUtil.setIntNull( nIndex++ );
            }
            daoUtil.setInt( nIndex++, datasetDocument.getChunkSize( ) );
            daoUtil.setInt( nIndex++, datasetDocument.getChunkOverlap( ) );
            daoUtil.setInt( nIndex, datasetDocument.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocument> selectByDatasetIdAndFolderId( int nDatasetId, Integer nFolderId, Plugin plugin )
    {
        List<DatasetDocument> list = new ArrayList<>( );
        String sql = nFolderId == null ? SQL_QUERY_SELECT_BY_ROOT : SQL_QUERY_SELECT_BY_FOLDER_ID;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            if ( nFolderId != null )
            {
                daoUtil.setInt( 2, nFolderId );
            }
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil, false ) );
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reparentDocuments( int nDatasetId, int nOldFolderId, Integer nNewFolderId, Plugin plugin )
    {
        String sql = nNewFolderId == null ? SQL_QUERY_REPARENT_TO_ROOT : SQL_QUERY_REPARENT_TO_FOLDER;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            int nIndex = 1;
            if ( nNewFolderId != null )
            {
                daoUtil.setInt( nIndex++, nNewFolderId );
            }
            daoUtil.setInt( nIndex++, nDatasetId );
            daoUtil.setInt( nIndex, nOldFolderId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocument> selectDatasetDocumentsList( Plugin plugin )
    {
        List<DatasetDocument> datasetDocumentList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                datasetDocumentList.add( loadFromDaoUtil( daoUtil, false ) );
            }
            return datasetDocumentList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocument> selectDatasetDocumentsListByDatasetId( int nDatasetId, Plugin plugin )
    {
        List<DatasetDocument> datasetDocumentList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_DATASET_ID, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                datasetDocumentList.add( loadFromDaoUtil( daoUtil, false ) );
            }
            return datasetDocumentList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatasetDocument> findByNameAndDatasetId( String strName, int nDatasetId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_NAME_AND_DATASET_ID, plugin ) )
        {
            daoUtil.setString( 1, strName );
            daoUtil.setInt( 2, nDatasetId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil, false ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatasetDocument> findByNameFolderAndDatasetId( String strName, Integer nFolderId, int nDatasetId, Plugin plugin )
    {
        String sql = nFolderId == null ? SQL_QUERY_SELECT_BY_NAME_ROOT_DATASET : SQL_QUERY_SELECT_BY_NAME_FOLDER_DATASET;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            daoUtil.setString( 1, strName );
            daoUtil.setInt( 2, nDatasetId );
            if ( nFolderId != null )
            {
                daoUtil.setInt( 3, nFolderId );
            }
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil, false ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * Creates a DatasetDocument object from the current row of the DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil with the current row pointing to the DatasetDocument data
     * @return The DatasetDocument object filled with data from the database
     */
    private DatasetDocument loadFromDaoUtil( DAOUtil daoUtil )
    {
        return loadFromDaoUtil( daoUtil, true );
    }

    /**
     * Builds a DatasetDocument from the current DAOUtil row. Skips the full_content column when loading a list (LONGTEXT can be Mo-large and is not needed for
     * listings).
     *
     * @param daoUtil
     *            The DAOUtil pointing at the row
     * @param bWithFullContent
     *            True if the query includes full_content column, false for light listing queries
     * @return The DatasetDocument object filled with data from the database
     */
    private DatasetDocument loadFromDaoUtil( DAOUtil daoUtil, boolean bWithFullContent )
    {
        DatasetDocument datasetDocument = new DatasetDocument( );
        int nIndex = 1;
        datasetDocument.setId( daoUtil.getInt( nIndex++ ) );
        datasetDocument.setName( daoUtil.getString( nIndex++ ) );
        datasetDocument.setDescription( daoUtil.getString( nIndex++ ) );
        datasetDocument.setFileKey( daoUtil.getString( nIndex++ ) );
        datasetDocument.setDatasetId( daoUtil.getInt( nIndex++ ) );
        datasetDocument.setFolderId( daoUtil.getObject( nIndex++, Integer.class ) );
        if ( bWithFullContent )
        {
            datasetDocument.setFullContent( daoUtil.getString( nIndex++ ) );
        }
        datasetDocument.setUseDocumentIntelligence( daoUtil.getBoolean( nIndex++ ) );
        Integer providerId = daoUtil.getObject( nIndex++, Integer.class );
        datasetDocument.setDocumentIntelligenceProviderId( providerId );
        datasetDocument.setChunkSize( daoUtil.getInt( nIndex++ ) );
        datasetDocument.setChunkOverlap( daoUtil.getInt( nIndex ) );
        return datasetDocument;
    }
}
