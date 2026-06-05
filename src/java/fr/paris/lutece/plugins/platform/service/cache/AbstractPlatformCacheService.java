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

import javax.cache.CacheException;

import fr.paris.lutece.portal.service.cache.AbstractCacheableService;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Base for the platform's configuration caches. Factors the defensive guards that the core {@link AbstractCacheableService} omits:
 * {@code put}/{@code get}/{@code remove} delegate to a backing cache that is {@code null} when the cache is disabled in the datastore, so each call must check
 * {@link #isCacheEnable()} and that the cache is allocated and open before touching it.
 *
 * <p>
 * Subclasses only declare their cache name, registration ({@code @PostConstruct initCache}) and key builders.
 * </p>
 */
public abstract class AbstractPlatformCacheService extends AbstractCacheableService<String, Object>
{
    /**
     * Stores a value, guarded against a disabled or closed cache.
     *
     * @param key
     *            the cache key
     * @param value
     *            the value to cache
     */
    @Override
    public void put( String key, Object value )
    {
        if ( isCacheEnable( ) )
        {
            try
            {
                super.put( key, value );
            }
            catch( CacheException | IllegalStateException e )
            {
                AppLogService.error( "{} : error putting key {} in cache", getName( ), key, e );
            }
        }
    }

    /**
     * Retrieves a value, guarded against a disabled or closed cache.
     *
     * @param key
     *            the cache key
     * @return the cached value, or null on miss or when the cache is unavailable
     */
    @Override
    public Object get( String key )
    {
        if ( isCacheEnable( ) )
        {
            try
            {
                return super.get( key );
            }
            catch( CacheException | IllegalStateException e )
            {
                AppLogService.error( "{} : error getting key {} from cache", getName( ), key, e );
            }
        }
        return null;
    }

    /**
     * Removes a value, guarded against a disabled or closed cache.
     *
     * @param key
     *            the cache key
     * @return true if removed, false otherwise
     */
    @Override
    public boolean remove( String key )
    {
        if ( isCacheEnable( ) )
        {
            try
            {
                return super.remove( key );
            }
            catch( CacheException | IllegalStateException e )
            {
                AppLogService.error( "{} : error removing key {} from cache", getName( ), key, e );
            }
        }
        return false;
    }

    /**
     * Clears all entries, guarded against a disabled or closed cache. Used for coarse invalidation on rare mutations where computing the affected composite
     * keys is not worth the indirection.
     */
    public void clear( )
    {
        if ( isCacheEnable( ) )
        {
            try
            {
                resetCache( );
            }
            catch( CacheException | IllegalStateException e )
            {
                AppLogService.error( "{} : error clearing cache", getName( ), e );
            }
        }
    }

}
