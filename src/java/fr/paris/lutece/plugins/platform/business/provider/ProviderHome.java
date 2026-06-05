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
package fr.paris.lutece.plugins.platform.business.provider;

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.plugins.platform.service.cache.ProviderCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;

/**
 * This class provides methods for managing Provider objects in the data layer
 */
public final class ProviderHome
{
    private static IProviderDAO _dao = CDI.current( ).select( IProviderDAO.class ).get( );
    private static final ProviderCacheService _cache = CDI.current( ).select( ProviderCacheService.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private ProviderHome( )
    {
    }

    /**
     * Creates a new provider in the database
     *
     * @param provider
     *            The provider to create
     * @return The created provider
     */
    public static Provider create( Provider provider )
    {
        _dao.insert( provider, _plugin );
        PlatformCacheEvents.fire( PlatformResource.PROVIDER, provider.getId( ), Action.CREATED );
        return provider;
    }

    /**
     * Updates a provider in the database
     *
     * @param provider
     *            The provider to update
     * @return The updated provider
     */
    public static Provider update( Provider provider )
    {
        _dao.store( provider, _plugin );
        PlatformCacheEvents.fire( PlatformResource.PROVIDER, provider.getId( ), Action.UPDATED );
        return provider;
    }

    /**
     * Removes a provider from the database
     *
     * @param nKey
     *            The provider identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
        PlatformCacheEvents.fire( PlatformResource.PROVIDER, nKey, Action.REMOVED );
    }

    /**
     * Loads a provider from the database
     *
     * @param nKey
     *            The provider identifier
     * @return An Optional containing the provider, or empty if not found
     */
    public static Optional<Provider> findByPrimaryKey( int nKey )
    {
        Object cached = _cache.get( ProviderCacheService.getKey( nKey ) );

        if ( cached != null )
        {
            return Optional.of( (Provider) cached );
        }

        Optional<Provider> result = _dao.load( nKey, _plugin );
        result.ifPresent( provider -> _cache.put( ProviderCacheService.getKey( nKey ), provider ) );
        return result;
    }

    /**
     * Returns the list of all providers
     *
     * @return The list of providers
     */
    public static List<Provider> getProvidersList( )
    {
        return _dao.selectProvidersList( _plugin );
    }

    /**
     * Returns the list of providers of a specific type
     *
     * @param strType
     *            The provider type
     * @return The list of providers
     */
    public static List<Provider> getProvidersByType( String strType )
    {
        return _dao.selectProvidersByType( strType, _plugin );
    }
}
