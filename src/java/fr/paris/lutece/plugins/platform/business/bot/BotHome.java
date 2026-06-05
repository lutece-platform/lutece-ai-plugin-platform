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
package fr.paris.lutece.plugins.platform.business.bot;

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.plugins.platform.service.cache.BotCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;

/**
 * This class provides instances management methods for Bot objects
 */
public final class BotHome
{
    private static IBotDAO _dao = CDI.current( ).select( IBotDAO.class ).get( );
    private static final BotCacheService _cache = CDI.current( ).select( BotCacheService.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private BotHome( )
    {
    }

    /**
     * Creates a new Bot
     *
     * @param bot
     *            The Bot object to create
     * @return The created Bot
     */
    public static Bot create( Bot bot )
    {
        _dao.insert( bot, _plugin );
        PlatformCacheEvents.fire( PlatformResource.BOT, bot.getId( ), Action.CREATED );
        return bot;
    }

    /**
     * Updates a Bot
     *
     * @param bot
     *            The Bot object to update
     * @return The updated Bot
     */
    public static Bot update( Bot bot )
    {
        _dao.store( bot, _plugin );
        PlatformCacheEvents.fire( PlatformResource.BOT, bot.getId( ), Action.UPDATED );
        return bot;
    }

    /**
     * Removes a Bot
     *
     * @param nKey
     *            The Bot identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
        PlatformCacheEvents.fire( PlatformResource.BOT, nKey, Action.REMOVED );
    }

    /**
     * Finds a Bot by its primary key
     *
     * @param nKey
     *            The Bot identifier
     * @return An Optional containing the Bot if found, empty otherwise
     */
    public static Optional<Bot> findByPrimaryKey( int nKey )
    {
        Object cached = _cache.get( BotCacheService.getKey( nKey ) );

        if ( cached != null )
        {
            return Optional.of( (Bot) cached );
        }

        Optional<Bot> result = _dao.load( nKey, _plugin );
        result.ifPresent( bot -> _cache.put( BotCacheService.getKey( nKey ), bot ) );
        return result;
    }

    /**
     * Returns the list of all Bots
     *
     * @return The list of Bots
     */
    public static List<Bot> getBotsList( )
    {
        return _dao.selectBotsList( _plugin );
    }

    /**
     * Returns the list of Bots for a specific client
     *
     * @param clientId
     *            The client identifier
     * @return The list of Bots for the specified client
     */
    public static List<Bot> getBotsListByClientId( int clientId )
    {
        return _dao.selectBotsListByClientId( clientId, _plugin );
    }

    /**
     * Returns a reference list of all Bots (ID and name only) for RBAC purposes
     *
     * @return A ReferenceList containing bot IDs and names
     */
    public static ReferenceList getBotsReferenceList( )
    {
        return _dao.selectBotsReferenceList( _plugin );
    }
}
