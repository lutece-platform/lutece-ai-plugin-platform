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

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.plugins.platform.service.cache.SubscriptionCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;

/**
 * Provides business layer access to Subscription operations.
 */
public final class SubscriptionHome
{
    private static final ISubscriptionDAO _dao = CDI.current( ).select( ISubscriptionDAO.class ).get( );
    private static final SubscriptionCacheService _cache = CDI.current( ).select( SubscriptionCacheService.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class need not be instantiated
     */
    private SubscriptionHome( )
    {
    }

    /**
     * Creates a new Subscription in the database.
     *
     * @param subscription
     *            The Subscription to create
     * @return The created Subscription with its ID set
     */
    public static Subscription create( Subscription subscription )
    {
        _dao.insert( subscription, _plugin );
        PlatformCacheEvents.fire( PlatformResource.SUBSCRIPTION, subscription.getId( ), Action.CREATED );
        return subscription;
    }

    /**
     * Updates a Subscription in the database.
     *
     * @param subscription
     *            The Subscription to update
     * @return The updated Subscription
     */
    public static Subscription update( Subscription subscription )
    {
        _dao.store( subscription, _plugin );
        PlatformCacheEvents.fire( PlatformResource.SUBSCRIPTION, subscription.getId( ), Action.UPDATED );
        return subscription;
    }

    /**
     * Removes a Subscription from the database.
     *
     * @param nKey
     *            The identifier of the Subscription to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
        PlatformCacheEvents.fire( PlatformResource.SUBSCRIPTION, nKey, Action.REMOVED );
    }

    /**
     * Loads a Subscription by its primary key.
     *
     * @param nKey
     *            The identifier of the Subscription
     * @return An Optional containing the Subscription if found
     */
    public static Optional<Subscription> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns all Subscriptions from the database.
     *
     * @return List of all Subscriptions
     */
    public static List<Subscription> getSubscriptionsList( )
    {
        return _dao.selectSubscriptionsList( _plugin );
    }

    /**
     * Returns all Subscriptions for a specific client.
     *
     * @param nClientId
     *            The client identifier
     * @return List of Subscriptions for the client
     */
    public static List<Subscription> getSubscriptionsByClientId( int nClientId )
    {
        return _dao.selectSubscriptionsByClientId( nClientId, _plugin );
    }

    /**
     * Returns all Subscriptions for a specific resource.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The resource identifier
     * @return List of Subscriptions for the resource
     */
    public static List<Subscription> getSubscriptionsByResource( String resourceType, String resourceId )
    {
        return _dao.selectSubscriptionsByResource( resourceType, resourceId, _plugin );
    }

    /**
     * Finds a Subscription by client and resource information.
     *
     * @param nClientId
     *            The client identifier
     * @param strResourceType
     *            The type of resource
     * @param strResourceId
     *            The resource identifier
     * @return An Optional containing the Subscription if found
     */
    public static Optional<Subscription> findByClientAndResource( int nClientId, String strResourceType, String strResourceId )
    {
        String strKey = SubscriptionCacheService.getKey( nClientId, strResourceType, strResourceId );
        Object cached = _cache.get( strKey );

        if ( cached != null )
        {
            return Optional.of( (Subscription) cached );
        }

        Optional<Subscription> result = _dao.findByClientAndResource( nClientId, strResourceType, strResourceId, _plugin );
        result.ifPresent( subscription -> _cache.put( strKey, subscription ) );
        return result;
    }
}
