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
package fr.paris.lutece.plugins.platform.business.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of IClientDAO interface for database operations on Client entities.
 */
@ApplicationScoped
@Named( "platform.clientDAO" )
public class ClientDAO implements IClientDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_client (name, code, description, active) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_client SET name = ?, code = ?, description = ?, active = ? WHERE id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_client WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, name, code, description, active, created_at FROM platform_client WHERE id = ?";
    private static final String SQL_QUERY_SELECT_ALL = "SELECT id, name, code, description, active, created_at FROM platform_client";
    private static final String SQL_QUERY_SELECT_ACTIVE = SQL_QUERY_SELECT_ALL + " WHERE active = 1";

    /**
     * {@inheritDoc} Uses auto-generated keys to set the client's ID after insertion.
     */
    @Override
    public void insert( Client client, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, client.getName( ) );
            daoUtil.setString( nIndex++, client.getCode( ) );
            daoUtil.setString( nIndex++, client.getDescription( ) );
            daoUtil.setBoolean( nIndex, client.isActive( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                client.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( Client client, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, client.getName( ) );
            daoUtil.setString( nIndex++, client.getCode( ) );
            daoUtil.setString( nIndex++, client.getDescription( ) );
            daoUtil.setBoolean( nIndex++, client.isActive( ) );
            daoUtil.setInt( nIndex, client.getId( ) );
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
    public Optional<Client> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Client client = null;
            if ( daoUtil.next( ) )
            {
                client = loadClient( daoUtil );
            }
            return Optional.ofNullable( client );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Client> selectClientsList( Plugin plugin )
    {
        List<Client> clientList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                clientList.add( loadClient( daoUtil ) );
            }
            return clientList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Client> selectActiveClientsList( Plugin plugin )
    {
        List<Client> clientList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ACTIVE, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                clientList.add( loadClient( daoUtil ) );
            }
            return clientList;
        }
    }

    /**
     * Helper method to create a Client object from database results.
     *
     * @param daoUtil
     *            Data Access Object Utility
     * @return populated Client object
     */
    private Client loadClient( DAOUtil daoUtil )
    {
        Client client = new Client( );
        int nIndex = 1;
        client.setId( daoUtil.getInt( nIndex++ ) );
        client.setName( daoUtil.getString( nIndex++ ) );
        client.setCode( daoUtil.getString( nIndex++ ) );
        client.setDescription( daoUtil.getString( nIndex++ ) );
        client.setActive( daoUtil.getBoolean( nIndex++ ) );
        client.setCreatedAt( daoUtil.getTimestamp( nIndex ) );
        return client;
    }
}
