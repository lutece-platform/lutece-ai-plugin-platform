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
package fr.paris.lutece.plugins.platform.service.bot;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.BotUserRateLimit;
import fr.paris.lutece.plugins.platform.business.bot.BotUserRateLimitHome;
import fr.paris.lutece.plugins.platform.service.security.AbstractUserRateLimitService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Per-user daily rate limiting of bot messages. The check/reset/atomic-increment algorithm lives in {@link AbstractUserRateLimitService}; this class binds it
 * to the bot storage.
 */
@ApplicationScoped
@Named( "platform.botRateLimitService" )
public class BotRateLimitService extends AbstractUserRateLimitService<BotUserRateLimit>
{
    private static final String BOT_NOT_FOUND_MESSAGE = "Bot non trouvé";
    private static final String RATE_LIMIT_ERROR_MESSAGE = "Vous avez atteint la limite de %d messages par jour pour ce bot. Votre quota sera réinitialisé le %s";
    private static final int INITIAL_MESSAGE_COUNT = 1;

    @Inject
    private SubscriptionService _subscriptionService;

    /**
     * Default constructor for CDI.
     */
    BotRateLimitService( )
    {
    }

    /**
     * Retrieves the list of user rate limits for a specific user, restricted to the bots the client is subscribed to.
     *
     * @param userId
     *            the user identifier
     * @param client
     *            the client context
     * @return List of BotUserRateLimit for the specified user
     */
    public List<BotUserRateLimit> getUserRateLimits( String userId, Client client )
    {
        try
        {
            List<Bot> bots = BotHome.getBotsListByClientId( client.getId( ) );
            if ( bots.isEmpty( ) )
            {
                return new ArrayList<>( );
            }
            List<Integer> authorizedBotIds = bots.stream( )
                    .filter( bot -> _subscriptionService.hasActiveSubscription( client.getId( ), Bot.RESOURCE_TYPE, String.valueOf( bot.getId( ) ) ) )
                    .map( Bot::getId ).toList( );

            if ( authorizedBotIds.isEmpty( ) )
            {
                return new ArrayList<>( );
            }

            return BotUserRateLimitHome.findByUserIdAndBotIds( userId, authorizedBotIds );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error retrieving user rate limits", e );
            return new ArrayList<>( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OptionalInt findDailyLimit( int resourceId )
    {
        return BotHome.findByPrimaryKey( resourceId ).map( bot -> OptionalInt.of( bot.getRateLimitByUserByDay( ) ) ).orElse( OptionalInt.empty( ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<BotUserRateLimit> findEntry( String userId, int resourceId )
    {
        return BotUserRateLimitHome.findByUserIdAndBotId( userId, resourceId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEntry( String userId, int resourceId, Timestamp now )
    {
        BotUserRateLimit rateLimit = new BotUserRateLimit( );
        rateLimit.setUserId( userId );
        rateLimit.setBotId( resourceId );
        rateLimit.setMessageCount( INITIAL_MESSAGE_COUNT );
        rateLimit.setDateFirstMessage( now );
        BotUserRateLimitHome.create( rateLimit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Timestamp windowStart( BotUserRateLimit entry )
    {
        return entry.getDateFirstMessage( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetEntry( BotUserRateLimit entry, Timestamp now )
    {
        entry.setMessageCount( INITIAL_MESSAGE_COUNT );
        entry.setDateFirstMessage( now );
        BotUserRateLimitHome.update( entry );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean incrementIfBelow( BotUserRateLimit entry, int dailyLimit )
    {
        return BotUserRateLimitHome.incrementIfBelow( entry.getId( ), dailyLimit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeEntriesBefore( Timestamp cutoff )
    {
        BotUserRateLimitHome.removeExpiredEntries( cutoff );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String resourceNotFoundMessage( )
    {
        return BOT_NOT_FOUND_MESSAGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String exceededMessageTemplate( )
    {
        return RATE_LIMIT_ERROR_MESSAGE;
    }
}
