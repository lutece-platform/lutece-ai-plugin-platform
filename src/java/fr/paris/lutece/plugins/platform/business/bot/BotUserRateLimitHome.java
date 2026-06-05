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

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Home class for BotUserRateLimit entity providing CRUD operations
 */
public final class BotUserRateLimitHome
{
    private static final String PLUGIN_NAME = "platform";

    private static Plugin _plugin = PluginService.getPlugin( PLUGIN_NAME );
    private static IBotUserRateLimitDAO _dao = CDI.current( ).select( IBotUserRateLimitDAO.class ).get( );

    /**
     * Private constructor to prevent instantiation
     */
    private BotUserRateLimitHome( )
    {
    }

    /**
     * Creates a new BotUserRateLimit record in the database
     *
     * @param rateLimit
     *            the BotUserRateLimit object to create
     * @return the generated primary key
     */
    public static int create( BotUserRateLimit rateLimit )
    {
        return _dao.insert( rateLimit, _plugin );
    }

    /**
     * Updates an existing BotUserRateLimit record in the database
     *
     * @param rateLimit
     *            the BotUserRateLimit object to update
     */
    public static void update( BotUserRateLimit rateLimit )
    {
        _dao.store( rateLimit, _plugin );
    }

    /**
     * Atomically increments the message count of an entry while it is below the given limit.
     *
     * @param nId
     *            the rate limit entry id
     * @param nLimit
     *            the daily limit the count must stay below
     * @return true if the count was incremented, false if the limit was already reached
     */
    public static boolean incrementIfBelow( int nId, int nLimit )
    {
        return _dao.incrementIfBelow( nId, nLimit, _plugin );
    }

    /**
     * Finds a BotUserRateLimit by its primary key
     *
     * @param nId
     *            the primary key
     * @return an Optional containing the BotUserRateLimit if found
     */
    public static Optional<BotUserRateLimit> findByPrimaryKey( int nId )
    {
        return _dao.load( nId, _plugin );
    }

    /**
     * Finds a BotUserRateLimit by user ID and bot ID
     *
     * @param userId
     *            the user identifier
     * @param botId
     *            the bot identifier
     * @return an Optional containing the BotUserRateLimit if found
     */
    public static Optional<BotUserRateLimit> findByUserIdAndBotId( String userId, int botId )
    {
        return _dao.findByUserIdAndBotId( userId, botId, _plugin );
    }

    /**
     * Finds BotUserRateLimit records by user ID and a list of bot IDs
     *
     * @param userId
     *            the user identifier
     * @param botIds
     *            the list of bot identifiers
     * @return a list of BotUserRateLimit records matching the criteria
     */
    public static List<BotUserRateLimit> findByUserIdAndBotIds( String userId, List<Integer> botIds )
    {
        return _dao.findByUserIdAndBotIds( userId, botIds, _plugin );
    }

    /**
     * Removes a BotUserRateLimit by its primary key
     *
     * @param nId
     *            the primary key of the record to remove
     */
    public static void remove( int nId )
    {
        _dao.delete( nId, _plugin );
    }

    /**
     * Removes all expired BotUserRateLimit entries before the specified date
     *
     * @param date
     *            the timestamp before which entries should be removed
     */
    public static void removeExpiredEntries( Timestamp date )
    {
        _dao.deleteByDateBefore( date, _plugin );
    }

    /**
     * Retrieves all expired BotUserRateLimit entries before the specified date
     *
     * @param date
     *            the timestamp before which entries are considered expired
     * @return a list of expired BotUserRateLimit entries
     */
    public static List<BotUserRateLimit> getExpiredEntries( Timestamp date )
    {
        return _dao.selectExpiredEntries( date, _plugin );
    }
}
