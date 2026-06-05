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

/**
 * Interface for BotUserRateLimit DAO operations
 */
public interface IBotUserRateLimitDAO
{

    /**
     * Insert a new BotUserRateLimit record
     *
     * @param rateLimit
     *            the BotUserRateLimit object to insert
     * @param plugin
     *            the plugin context
     * @return the generated primary key
     */
    int insert( BotUserRateLimit rateLimit, Plugin plugin );

    /**
     * Update an existing BotUserRateLimit record
     *
     * @param rateLimit
     *            the BotUserRateLimit object to update
     * @param plugin
     *            the plugin context
     */
    void store( BotUserRateLimit rateLimit, Plugin plugin );

    /**
     * Atomically increments the message count of an entry, only when the current count is still below the given limit. The conditional UPDATE makes the
     * check-and-increment race-free across concurrent requests.
     *
     * @param nId
     *            the rate limit entry id
     * @param nLimit
     *            the daily limit the count must stay below
     * @param plugin
     *            the Plugin
     * @return true if the count was incremented, false if the limit was already reached
     */
    boolean incrementIfBelow( int nId, int nLimit, Plugin plugin );

    /**
     * Load a BotUserRateLimit by its primary key
     *
     * @param nId
     *            the primary key
     * @param plugin
     *            the plugin context
     * @return an Optional containing the BotUserRateLimit if found
     */
    Optional<BotUserRateLimit> load( int nId, Plugin plugin );

    /**
     * Find a BotUserRateLimit by user ID and bot ID
     *
     * @param userId
     *            the user identifier
     * @param botId
     *            the bot identifier
     * @param plugin
     *            the plugin context
     * @return an Optional containing the BotUserRateLimit if found
     */
    Optional<BotUserRateLimit> findByUserIdAndBotId( String userId, int botId, Plugin plugin );

    /**
     * Delete a BotUserRateLimit by its primary key
     *
     * @param nId
     *            the primary key
     * @param plugin
     *            the plugin context
     */
    void delete( int nId, Plugin plugin );

    /**
     * Delete all BotUserRateLimit records with expiration date before the specified date
     *
     * @param date
     *            the cutoff date
     * @param plugin
     *            the plugin context
     */
    void deleteByDateBefore( Timestamp date, Plugin plugin );

    /**
     * Select all expired BotUserRateLimit entries before the specified date
     *
     * @param date
     *            the cutoff date
     * @param plugin
     *            the plugin context
     * @return a list of expired BotUserRateLimit entries
     */
    List<BotUserRateLimit> selectExpiredEntries( Timestamp date, Plugin plugin );

    /**
     * Find BotUserRateLimit records by user ID and a list of bot IDs
     *
     * @param userId
     *            the user identifier
     * @param botIds
     *            the list of bot identifiers
     * @param plugin
     *            the plugin context
     * @return a list of BotUserRateLimit records matching the criteria
     */
    List<BotUserRateLimit> findByUserIdAndBotIds( String userId, List<Integer> botIds, Plugin plugin );
}
