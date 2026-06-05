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

import fr.paris.lutece.plugins.platform.service.security.RateLimitResult;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.BotUserRateLimit;
import fr.paris.lutece.plugins.platform.business.bot.BotUserRateLimitHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link BotRateLimitService} against HSQL. These exercise the daily per-user rate-limit state machine: an unlimited bot always passes, a
 * missing bot is rejected, the first message creates a fresh counter, subsequent messages increment it, the limit denies further messages with a reset hint,
 * and a counter older than 24 hours is rolled over. The expired-entry cleanup is also covered.
 */
public class BotRateLimitServiceTest extends AbstractPlatformDbTest
{
    private static final String USER_ID = "rate-limit-user";

    private Client _client;
    private Provider _llmProvider;
    private Provider _embedProvider;

    /**
     * Resolves the CDI-managed rate-limit service.
     *
     * @return the rate-limit service instance
     */
    private BotRateLimitService service( )
    {
        return CDI.current( ).select( BotRateLimitService.class ).get( );
    }

    /**
     * Builds the foreign-key parent chain (client and two providers) shared by every test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client RateLimitSvc Test" );
        _client.setCode( "CLIENT_RATELIMITSVC_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _llmProvider = createProvider( "LLM" );
        _embedProvider = createProvider( "EMBEDDING" );
    }

    /**
     * A bot with a non-positive daily limit is unlimited: the check always allows and never persists a counter.
     */
    @Test
    public void testUnlimitedBotAlwaysAllowed( )
    {
        Bot bot = createBot( 0 );

        RateLimitResult result = service( ).checkRateLimit( USER_ID, bot.getId( ) );

        assertTrue( result.isAllowed( ) );
        assertNull( result.getErrorMessage( ) );
        assertTrue( BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) ).isEmpty( ) );
    }

    /**
     * Checking a non-existent bot is denied with the dedicated message rather than throwing.
     */
    @Test
    public void testUnknownBotDenied( )
    {
        RateLimitResult result = service( ).checkRateLimit( USER_ID, 999999 );

        assertFalse( result.isAllowed( ) );
        assertEquals( "Bot non trouvé", result.getErrorMessage( ) );
    }

    /**
     * The first message of a limited bot creates a counter starting at one and is allowed.
     */
    @Test
    public void testFirstMessageCreatesCounter( )
    {
        Bot bot = createBot( 3 );

        RateLimitResult result = service( ).checkRateLimit( USER_ID, bot.getId( ) );

        assertTrue( result.isAllowed( ) );
        Optional<BotUserRateLimit> stored = BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) );
        assertTrue( stored.isPresent( ) );
        assertEquals( 1, stored.get( ).getMessageCount( ) );
    }

    /**
     * A second message under the limit increments the counter and is allowed.
     */
    @Test
    public void testSecondMessageIncrementsCounter( )
    {
        Bot bot = createBot( 3 );

        service( ).checkRateLimit( USER_ID, bot.getId( ) );
        RateLimitResult result = service( ).checkRateLimit( USER_ID, bot.getId( ) );

        assertTrue( result.isAllowed( ) );
        assertEquals( 2, BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) ).get( ).getMessageCount( ) );
    }

    /**
     * Once the counter has reached the limit within the window, the next message is denied with a reset hint.
     */
    @Test
    public void testLimitReachedDenied( )
    {
        Bot bot = createBot( 2 );
        seedCounter( bot.getId( ), 2, LocalDateTime.now( ) );

        RateLimitResult result = service( ).checkRateLimit( USER_ID, bot.getId( ) );

        assertFalse( result.isAllowed( ) );
        assertNotNull( result.getErrorMessage( ) );
        assertTrue( result.getErrorMessage( ).contains( "2" ) );
        assertNotNull( result.getResetTime( ) );
        assertEquals( 2, BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) ).get( ).getMessageCount( ) );
    }

    /**
     * A counter whose first message is older than 24 hours rolls over: the request is allowed and the counter is reset to one.
     */
    @Test
    public void testCounterRolledOverAfterWindow( )
    {
        Bot bot = createBot( 2 );
        seedCounter( bot.getId( ), 2, LocalDateTime.now( ).minusHours( 25 ) );

        RateLimitResult result = service( ).checkRateLimit( USER_ID, bot.getId( ) );

        assertTrue( result.isAllowed( ) );
        assertEquals( 1, BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) ).get( ).getMessageCount( ) );
    }

    /**
     * The cleanup removes counters whose first message predates the 24-hour cutoff and keeps recent ones.
     */
    @Test
    public void testCleanExpiredEntries( )
    {
        Bot bot = createBot( 5 );
        seedCounter( bot.getId( ), 1, LocalDateTime.now( ).minusHours( 30 ) );

        service( ).cleanExpiredEntries( );

        assertTrue( BotUserRateLimitHome.findByUserIdAndBotId( USER_ID, bot.getId( ) ).isEmpty( ) );
    }

    /**
     * Persists a rate-limit counter for the shared user and the given bot with an explicit first-message time.
     *
     * @param nBotId
     *            the bot identifier
     * @param nCount
     *            the message count to store
     * @param firstMessage
     *            the first-message timestamp
     */
    private void seedCounter( int nBotId, int nCount, LocalDateTime firstMessage )
    {
        BotUserRateLimit rateLimit = new BotUserRateLimit( );
        rateLimit.setUserId( USER_ID );
        rateLimit.setBotId( nBotId );
        rateLimit.setMessageCount( nCount );
        rateLimit.setDateFirstMessage( Timestamp.valueOf( firstMessage ) );
        BotUserRateLimitHome.create( rateLimit );
    }

    /**
     * Creates a persisted provider of the given type.
     *
     * @param strType
     *            the provider type
     * @return the persisted provider
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
     * Creates a persisted bot wired to the shared parent chain with the given daily rate limit.
     *
     * @param nDailyLimit
     *            the daily per-user message limit (0 means unlimited)
     * @return the persisted bot
     */
    private Bot createBot( int nDailyLimit )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot RateLimitSvc Test" );
        bot.setClientId( _client.getId( ) );
        bot.setLlmProviderId( _llmProvider.getId( ) );
        bot.setEmbedProviderId( _embedProvider.getId( ) );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        bot.setRateLimitByUserByDay( nDailyLimit );
        return BotHome.create( bot );
    }
}
