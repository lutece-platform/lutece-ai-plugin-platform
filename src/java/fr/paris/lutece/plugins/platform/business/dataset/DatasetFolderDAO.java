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
 * Implementation of the IDatasetFolderDAO interface
 */
@ApplicationScoped
@Named( "platform.datasetFolderDAO" )
public class DatasetFolderDAO implements IDatasetFolderDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_dataset_folder (dataset_id, parent_folder_id, folder_name, folder_description) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_dataset_folder WHERE folder_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_dataset_folder SET dataset_id = ?, parent_folder_id = ?, folder_name = ?, folder_description = ? WHERE folder_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT folder_id, dataset_id, parent_folder_id, folder_name, folder_description, created_at, updated_at FROM platform_dataset_folder";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE folder_id = ?";
    private static final String SQL_QUERY_SELECT_BY_DATASET = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? ORDER BY folder_name";
    private static final String SQL_QUERY_SELECT_ROOT_CHILDREN = SQL_QUERY_SELECTALL
            + " WHERE dataset_id = ? AND parent_folder_id IS NULL ORDER BY folder_name";
    private static final String SQL_QUERY_SELECT_CHILDREN = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? AND parent_folder_id = ? ORDER BY folder_name";
    private static final String SQL_QUERY_SELECT_BY_ROOT_NAME = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? AND parent_folder_id IS NULL AND folder_name = ?";
    private static final String SQL_QUERY_SELECT_BY_PARENT_NAME = SQL_QUERY_SELECTALL + " WHERE dataset_id = ? AND parent_folder_id = ? AND folder_name = ?";
    private static final String SQL_QUERY_REPARENT_TO_FOLDER = "UPDATE platform_dataset_folder SET parent_folder_id = ? WHERE dataset_id = ? AND parent_folder_id = ?";
    private static final String SQL_QUERY_REPARENT_TO_ROOT = "UPDATE platform_dataset_folder SET parent_folder_id = NULL WHERE dataset_id = ? AND parent_folder_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DatasetFolder folder, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, folder.getDatasetId( ) );
            if ( folder.getParentFolderId( ) == null )
            {
                daoUtil.setIntNull( nIndex++ );
            }
            else
            {
                daoUtil.setInt( nIndex++, folder.getParentFolderId( ) );
            }
            daoUtil.setString( nIndex++, folder.getName( ) );
            daoUtil.setString( nIndex, folder.getDescription( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                folder.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( DatasetFolder folder, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, folder.getDatasetId( ) );
            if ( folder.getParentFolderId( ) == null )
            {
                daoUtil.setIntNull( nIndex++ );
            }
            else
            {
                daoUtil.setInt( nIndex++, folder.getParentFolderId( ) );
            }
            daoUtil.setString( nIndex++, folder.getName( ) );
            daoUtil.setString( nIndex++, folder.getDescription( ) );
            daoUtil.setInt( nIndex, folder.getId( ) );
            daoUtil.executeUpdate( );
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
    public Optional<DatasetFolder> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetFolder> selectByDatasetId( int nDatasetId, Plugin plugin )
    {
        List<DatasetFolder> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_DATASET, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetFolder> selectChildren( int nDatasetId, Integer nParentFolderId, Plugin plugin )
    {
        List<DatasetFolder> list = new ArrayList<>( );
        String sql = nParentFolderId == null ? SQL_QUERY_SELECT_ROOT_CHILDREN : SQL_QUERY_SELECT_CHILDREN;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            if ( nParentFolderId != null )
            {
                daoUtil.setInt( 2, nParentFolderId );
            }
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatasetFolder> findByParentAndName( int nDatasetId, Integer nParentFolderId, String strName, Plugin plugin )
    {
        String sql = nParentFolderId == null ? SQL_QUERY_SELECT_BY_ROOT_NAME : SQL_QUERY_SELECT_BY_PARENT_NAME;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            if ( nParentFolderId == null )
            {
                daoUtil.setString( 2, strName );
            }
            else
            {
                daoUtil.setInt( 2, nParentFolderId );
                daoUtil.setString( 3, strName );
            }
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reparentFolders( int nDatasetId, int nOldParentFolderId, Integer nNewParentFolderId, Plugin plugin )
    {
        String sql = nNewParentFolderId == null ? SQL_QUERY_REPARENT_TO_ROOT : SQL_QUERY_REPARENT_TO_FOLDER;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            int nIndex = 1;
            if ( nNewParentFolderId != null )
            {
                daoUtil.setInt( nIndex++, nNewParentFolderId );
            }
            daoUtil.setInt( nIndex++, nDatasetId );
            daoUtil.setInt( nIndex, nOldParentFolderId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Builds a DatasetFolder object from the current DAOUtil row
     *
     * @param daoUtil
     *            The DAOUtil with the current row pointing to the folder data
     * @return The DatasetFolder object
     */
    private DatasetFolder loadFromDaoUtil( DAOUtil daoUtil )
    {
        DatasetFolder folder = new DatasetFolder( );
        int nIndex = 1;
        folder.setId( daoUtil.getInt( nIndex++ ) );
        folder.setDatasetId( daoUtil.getInt( nIndex++ ) );
        folder.setParentFolderId( daoUtil.getObject( nIndex++, Integer.class ) );
        folder.setName( daoUtil.getString( nIndex++ ) );
        folder.setDescription( daoUtil.getString( nIndex++ ) );
        folder.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        folder.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return folder;
    }
}
