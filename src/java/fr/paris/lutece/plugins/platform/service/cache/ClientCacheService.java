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
 * Cache for client lookups. Client authentication resolves every request against the active client list ({@code getActiveClientsList}), so without a cache each
 * REST call triggers a full table scan — the dominant DB-pool consumer under load. Clients are configuration data: read on every request, mutated rarely, hence
 * a near-static cache invalidated on mutation.
 */
@ApplicationScoped
public class ClientCacheService extends AbstractPlatformCacheService
{
    private static final String CACHE_NAME = "platform.clientCacheService";
    private static final String ACTIVE_LIST_KEY = "platform.client.active.list";
    private static final String ALL_LIST_KEY = "platform.client.all.list";

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
     * Returns the cache key for the active client list.
     *
     * @return the active list cache key
     */
    public static String getActiveListKey( )
    {
        return ACTIVE_LIST_KEY;
    }

    /**
     * Returns the cache key for the full client list.
     *
     * @return the full list cache key
     */
    public static String getAllListKey( )
    {
        return ALL_LIST_KEY;
    }

    /**
     * Invalidates the cached client lists on any client mutation. Observes every {@link EntityChangedEvent} and acts only on {@link PlatformResource#CLIENT}.
     *
     * @param event
     *            the entity-changed event
     */
    public void onEntityChanged( @Observes EntityChangedEvent event )
    {
        if ( event.resource( ) == PlatformResource.CLIENT )
        {
            remove( getActiveListKey( ) );
            remove( getAllListKey( ) );
        }
    }
}
