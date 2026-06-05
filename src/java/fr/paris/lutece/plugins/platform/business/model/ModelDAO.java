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
 * Implementation of the IModelDAO interface
 */
@ApplicationScoped
@Named( "platform.modelDAO" )
public class ModelDAO implements IModelDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_model ( client_id, provider_id, created_at, updated_at ) "
            + "VALUES ( ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_model WHERE model_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_model SET client_id = ?, provider_id = ?, updated_at = CURRENT_TIMESTAMP WHERE model_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT model_id, client_id, provider_id, created_at, updated_at FROM platform_model";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE model_id = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE client_id = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_AND_PROVIDER = SQL_QUERY_SELECTALL + " WHERE client_id = ? AND provider_id = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT m.model_id, p.provider_name FROM platform_model m "
            + "INNER JOIN platform_provider p ON m.provider_id = p.provider_id ORDER BY p.provider_name";

    @Override
    public void insert( Model model, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, model.getClientId( ) );
            daoUtil.setInt( nIndex++, model.getProviderId( ) );

            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                model.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public Optional<Model> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Model model = null;
            if ( daoUtil.next( ) )
            {
                model = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( model );
        }
    }

    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void store( Model model, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, model.getClientId( ) );
            daoUtil.setInt( nIndex++, model.getProviderId( ) );
            daoUtil.setInt( nIndex, model.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<Model> selectModelsList( Plugin plugin )
    {
        List<Model> modelList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                modelList.add( loadFromDaoUtil( daoUtil ) );
            }
            return modelList;
        }
    }

    @Override
    public List<Model> selectModelsListByClientId( int clientId, Plugin plugin )
    {
        List<Model> modelList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                modelList.add( loadFromDaoUtil( daoUtil ) );
            }
            return modelList;
        }
    }

    @Override
    public Optional<Model> selectByClientIdAndProviderId( int clientId, int providerId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_AND_PROVIDER, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.setInt( 2, providerId );
            daoUtil.executeQuery( );
            Model model = null;
            if ( daoUtil.next( ) )
            {
                model = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( model );
        }
    }

    @Override
    public ReferenceList selectModelsReferenceList( Plugin plugin )
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
     * Builds a Model instance from the current row of the given DAOUtil
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to read
     * @return the loaded model
     */
    private Model loadFromDaoUtil( DAOUtil daoUtil )
    {
        Model model = new Model( );
        int nIndex = 1;

        model.setId( daoUtil.getInt( nIndex++ ) );
        model.setClientId( daoUtil.getInt( nIndex++ ) );
        model.setProviderId( daoUtil.getInt( nIndex++ ) );
        model.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        model.setUpdatedAt( daoUtil.getTimestamp( nIndex++ ) );

        return model;
    }
}
