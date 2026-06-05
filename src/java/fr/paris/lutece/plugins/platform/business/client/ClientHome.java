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

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.plugins.platform.service.cache.ClientCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * This class provides data access methods for Client objects
 */
public final class ClientHome
{
    private static final IClientDAO _dao = CDI.current( ).select( IClientDAO.class ).get( );
    private static final ClientCacheService _cache = CDI.current( ).select( ClientCacheService.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private ClientHome( )
    {
    }

    /**
     * Creates a new client in the database
     *
     * @param client
     *            The instance of the Client to create
     * @return The created client
     */
    public static Client create( Client client )
    {
        _dao.insert( client, _plugin );
        PlatformCacheEvents.fire( PlatformResource.CLIENT, client.getId( ), Action.CREATED );
        return client;
    }

    /**
     * Updates a client in the database
     *
     * @param client
     *            The instance of the Client to update
     * @return The updated client
     */
    public static Client update( Client client )
    {
        _dao.store( client, _plugin );
        PlatformCacheEvents.fire( PlatformResource.CLIENT, client.getId( ), Action.UPDATED );
        return client;
    }

    /**
     * Removes a client from the database
     *
     * @param nKey
     *            The identifier of the client to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
        PlatformCacheEvents.fire( PlatformResource.CLIENT, nKey, Action.REMOVED );
    }

    /**
     * Loads a client from the database
     *
     * @param nKey
     *            The identifier of the client to load
     * @return The loaded client as an Optional
     */
    public static Optional<Client> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of all clients
     *
     * @return The list of all clients
     */
    @SuppressWarnings( "unchecked" )
    public static List<Client> getClientsList( )
    {
        List<Client> list = (List<Client>) _cache.get( ClientCacheService.getAllListKey( ) );

        if ( list == null )
        {
            list = _dao.selectClientsList( _plugin );
            _cache.put( ClientCacheService.getAllListKey( ), list );
        }

        return list;
    }

    /**
     * Returns the list of active clients
     *
     * @return The list of active clients
     */
    @SuppressWarnings( "unchecked" )
    public static List<Client> getActiveClientsList( )
    {
        List<Client> list = (List<Client>) _cache.get( ClientCacheService.getActiveListKey( ) );

        if ( list == null )
        {
            list = _dao.selectActiveClientsList( _plugin );
            _cache.put( ClientCacheService.getActiveListKey( ), list );
        }

        return list;
    }
}
