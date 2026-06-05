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
package fr.paris.lutece.plugins.platform.service.cache;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Cache for subscription lookups. Client authentication checks an active subscription ({@code findByClientAndResource}) on every resource-scoped request;
 * subscriptions are stable configuration, cached per (client, resourceType, resourceId) and invalidated on mutation.
 */
@ApplicationScoped
public class SubscriptionCacheService extends AbstractPlatformCacheService
{
    private static final String CACHE_NAME = "platform.subscriptionCacheService";
    private static final String KEY_PREFIX = "platform.subscription.";

    /**
     * Registers the cache at bean initialization.
     */
    @PostConstruct
    public void init( )
    {
        initCache( CACHE_NAME, String.class, Object.class );
    }

    /**
     * Returns the cache name.
     *
     * @return the cache name
     */
    @Override
    public String getName( )
    {
        return CACHE_NAME;
    }

    /**
     * Returns the cache key for a subscription by client and resource.
     *
     * @param nClientId
     *            the client id
     * @param strResourceType
     *            the resource type
     * @param strResourceId
     *            the resource id
     * @return the cache key
     */
    public static String getKey( int nClientId, String strResourceType, String strResourceId )
    {
        return KEY_PREFIX + nClientId + "." + strResourceType + "." + strResourceId;
    }

    /**
     * Invalidates the subscription cache on any subscription mutation. The composite key cannot be reconstructed from the event alone, so a coarse
     * {@link #clear()} is used (subscription mutations are rare). Observes every {@link EntityChangedEvent} and acts only on
     * {@link PlatformResource#SUBSCRIPTION}.
     *
     * @param event
     *            the entity-changed event
     */
    public void onEntityChanged( @Observes EntityChangedEvent event )
    {
        if ( event.resource( ) == PlatformResource.SUBSCRIPTION )
        {
            clear( );
        }
    }
}
