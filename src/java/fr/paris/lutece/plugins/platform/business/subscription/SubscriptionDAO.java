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
package fr.paris.lutece.plugins.platform.business.subscription;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ISubscriptionDAO interface for database operations.
 */
@ApplicationScoped
@Named( "platform.subscriptionDAO" )
public class SubscriptionDAO implements ISubscriptionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_subscription (id_client, resource_type, resource_id, status) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_subscription SET id_client = ?, resource_type = ?, resource_id = ?, status = ? WHERE id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_subscription WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, id_client, resource_type, resource_id, subscription_date, status FROM platform_subscription WHERE id = ?";
    private static final String SQL_QUERY_SELECT_ALL = "SELECT id, id_client, resource_type, resource_id, subscription_date, status FROM platform_subscription";
    private static final String SQL_QUERY_SELECT_BY_CLIENT = SQL_QUERY_SELECT_ALL + " WHERE id_client = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_AND_RESOURCE = SQL_QUERY_SELECT_ALL
            + " WHERE id_client = ? AND resource_type = ? AND resource_id = ?";
    private static final String SQL_QUERY_SELECT_BY_RESOURCE = SQL_QUERY_SELECT_ALL + " WHERE resource_type = ? AND resource_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( Subscription subscription, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, subscription.getClientId( ) );
            daoUtil.setString( nIndex++, subscription.getResourceType( ) );
            daoUtil.setString( nIndex++, subscription.getResourceId( ) );
            daoUtil.setString( nIndex, subscription.getStatus( ).getValue( ) );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                subscription.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( Subscription subscription, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, subscription.getClientId( ) );
            daoUtil.setString( nIndex++, subscription.getResourceType( ) );
            daoUtil.setString( nIndex++, subscription.getResourceId( ) );
            daoUtil.setString( nIndex++, subscription.getStatus( ).getValue( ) );
            daoUtil.setInt( nIndex, subscription.getId( ) );
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
    public Optional<Subscription> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );

            Subscription subscription = null;
            if ( daoUtil.next( ) )
            {
                subscription = loadSubscription( daoUtil );
            }

            return Optional.ofNullable( subscription );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> selectSubscriptionsList( Plugin plugin )
    {
        List<Subscription> subscriptionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                subscriptionList.add( loadSubscription( daoUtil ) );
            }
            return subscriptionList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> selectSubscriptionsByClientId( int nClientId, Plugin plugin )
    {
        List<Subscription> subscriptionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT, plugin ) )
        {
            daoUtil.setInt( 1, nClientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                subscriptionList.add( loadSubscription( daoUtil ) );
            }
            return subscriptionList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Subscription> findByClientAndResource( int nClientId, String strResourceType, String strResourceId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_AND_RESOURCE, plugin ) )
        {
            daoUtil.setInt( 1, nClientId );
            daoUtil.setString( 2, strResourceType );
            daoUtil.setString( 3, strResourceId );
            daoUtil.executeQuery( );

            Subscription subscription = null;
            if ( daoUtil.next( ) )
            {
                subscription = loadSubscription( daoUtil );
            }

            return Optional.ofNullable( subscription );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> selectSubscriptionsByResource( String resourceType, String resourceId, Plugin plugin )
    {
        List<Subscription> subscriptionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_RESOURCE, plugin ) )
        {
            daoUtil.setString( 1, resourceType );
            daoUtil.setString( 2, resourceId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                subscriptionList.add( loadSubscription( daoUtil ) );
            }
            return subscriptionList;
        }
    }

    /**
     * Helper method to load a Subscription object from database results.
     *
     * @param daoUtil
     *            The DAOUtil object containing query results
     * @return A populated Subscription object
     */
    private Subscription loadSubscription( DAOUtil daoUtil )
    {
        int nIndex = 1;
        Subscription subscription = new Subscription( );
        subscription.setId( daoUtil.getInt( nIndex++ ) );
        subscription.setClientId( daoUtil.getInt( nIndex++ ) );
        subscription.setResourceType( daoUtil.getString( nIndex++ ) );
        subscription.setResourceId( daoUtil.getString( nIndex++ ) );
        subscription.setSubscriptionDate( daoUtil.getTimestamp( nIndex++ ) );
        subscription.setStatus( SubscriptionStatus.fromValue( daoUtil.getString( nIndex ) ) );
        return subscription;
    }
}
