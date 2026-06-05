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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for BotUserRateLimitHome on HSQL
 */
public class BotUserRateLimitHomeTest extends AbstractPlatformDbTest
{
    /**
     * incrementIfBelow only increments while strictly below the limit, atomically: at the limit it returns false and the stored count never exceeds the limit.
     */
    @Test
    public void testIncrementIfBelowStopsAtLimit( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );

        BotUserRateLimit rateLimit = new BotUserRateLimit( );
        rateLimit.setUserId( "user-incr" );
        rateLimit.setBotId( bot.getId( ) );
        rateLimit.setMessageCount( 4 );
        rateLimit.setDateFirstMessage( new Timestamp( System.currentTimeMillis( ) ) );
        BotUserRateLimitHome.create( rateLimit );

        assertTrue( BotUserRateLimitHome.incrementIfBelow( rateLimit.getId( ), 5 ), "count 4 < limit 5: increment must succeed" );
        assertFalse( BotUserRateLimitHome.incrementIfBelow( rateLimit.getId( ), 5 ), "count 5 = limit 5: increment must be refused" );
        assertEquals( 5, BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) ).orElseThrow( ).getMessageCount( ),
                "the stored count must never exceed the limit" );
    }

    /**
     * Full CRUD round-trip of the BotUserRateLimit Home.
     */
    @Test
    public void testBusinessBotUserRateLimit( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );

        BotUserRateLimit rateLimit = new BotUserRateLimit( );
        rateLimit.setUserId( "user-1" );
        rateLimit.setBotId( bot.getId( ) );
        rateLimit.setMessageCount( 1 );
        rateLimit.setDateFirstMessage( new Timestamp( System.currentTimeMillis( ) ) );

        BotUserRateLimitHome.create( rateLimit );
        Optional<BotUserRateLimit> stored = BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) );
        assertTrue( stored.isPresent( ) );
        assertEquals( rateLimit.getMessageCount( ), stored.get( ).getMessageCount( ) );
        assertEquals( rateLimit.getUserId( ), stored.get( ).getUserId( ) );

        rateLimit.setMessageCount( 5 );
        BotUserRateLimitHome.update( rateLimit );
        stored = BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) );
        assertTrue( stored.isPresent( ) );
        assertEquals( rateLimit.getMessageCount( ), stored.get( ).getMessageCount( ) );

        Optional<BotUserRateLimit> byUserAndBot = BotUserRateLimitHome.findByUserIdAndBotId( "user-1", bot.getId( ) );
        assertTrue( byUserAndBot.isPresent( ) );

        BotUserRateLimitHome.remove( rateLimit.getId( ) );
        stored = BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) );
        assertTrue( stored.isEmpty( ) );
    }

    /**
     * Creates a persisted Client to be used as the foreign key parent of a Bot.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client RateLimit Test" );
        client.setCode( "CLIENT_RATELIMIT_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a persisted Provider to be used as a foreign key parent of a Bot.
     *
     * @param strType
     *            The provider type
     * @return The created Provider with its generated primary key
     */
    private Provider createProvider( String strType )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Provider " + strType );
        provider.setProviderType( strType );
        provider.setDeploymentModelName( "model-" + strType );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates a persisted Bot to be used as the foreign key parent of a rate limit entry.
     *
     * @param nClientId
     *            The parent client identifier
     * @param nLlmProviderId
     *            The LLM provider identifier
     * @param nEmbedProviderId
     *            The embedding provider identifier
     * @return The created Bot with its generated primary key
     */
    private Bot createBot( int nClientId, int nLlmProviderId, int nEmbedProviderId )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot RateLimit Test" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nLlmProviderId );
        bot.setEmbedProviderId( nEmbedProviderId );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }
}
