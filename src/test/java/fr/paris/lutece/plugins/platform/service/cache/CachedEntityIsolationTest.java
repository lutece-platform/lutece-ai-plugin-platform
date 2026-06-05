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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Proves that entities served from the configuration caches are isolated between readers: a mutation performed by one consumer (typically per-user RBAC
 * enrichment such as {@code setUserCanModify}) must never be visible to the next consumer reading the same entity from the cache. A failure here means per-user
 * permission flags leak across users/requests.
 */
public class CachedEntityIsolationTest extends AbstractPlatformDbTest
{
    private ClientCacheService _clientCache;
    private BotCacheService _botCache;
    private boolean _previousClientStatus;
    private boolean _previousBotStatus;

    /**
     * Enables the client and bot caches before each test, remembering their previous status.
     */
    @BeforeEach
    public void enableCaches( )
    {
        _clientCache = CDI.current( ).select( ClientCacheService.class ).get( );
        _botCache = CDI.current( ).select( BotCacheService.class ).get( );
        _previousClientStatus = _clientCache.isCacheEnable( );
        _previousBotStatus = _botCache.isCacheEnable( );
        _clientCache.enableCache( true );
        _botCache.enableCache( true );
    }

    /**
     * Clears and restores the previous cache status after each test.
     */
    @AfterEach
    public void restoreCaches( )
    {
        _clientCache.clear( );
        _clientCache.enableCache( _previousClientStatus );
        _botCache.clear( );
        _botCache.enableCache( _previousBotStatus );
    }

    /**
     * RBAC flags set by one reader on a client from the cached list must not be visible to the next reader (cross-user permission leak).
     */
    @Test
    public void testClientListMutationDoesNotLeakToNextReader( )
    {
        Client client = createClient( "CLIENT_ISOLATION" );

        ClientHome.getClientsList( );
        assertNotNull( _clientCache.get( ClientCacheService.getAllListKey( ) ), "precondition: the client list must actually be served from the cache" );

        List<Client> firstRead = ClientHome.getClientsList( );
        Client firstInstance = findById( firstRead, client.getId( ) );
        firstInstance.setUserCanModify( true );
        firstInstance.setUserCanDelete( true );

        List<Client> secondRead = ClientHome.getClientsList( );
        Client secondInstance = findById( secondRead, client.getId( ) );

        assertFalse( secondInstance.isUserCanModify( ), "userCanModify set by a previous reader leaked through the cache" );
        assertFalse( secondInstance.isUserCanDelete( ), "userCanDelete set by a previous reader leaked through the cache" );
    }

    /**
     * The instance returned on the cache-miss read (the one that was just put) must also be isolated from the cache: mutating it must not corrupt later reads.
     */
    @Test
    public void testClientListMissInstanceDoesNotCorruptCache( )
    {
        Client client = createClient( "CLIENT_MISS_ISOLATION" );

        List<Client> missRead = ClientHome.getClientsList( );
        findById( missRead, client.getId( ) ).setUserCanModify( true );

        List<Client> hitRead = ClientHome.getClientsList( );

        assertFalse( findById( hitRead, client.getId( ) ).isUserCanModify( ), "mutation of the miss-path instance corrupted the cached list" );
    }

    /**
     * RBAC flags or config mutations on a bot loaded via the cached findByPrimaryKey must not be visible to the next reader.
     */
    @Test
    public void testBotFindByPrimaryKeyMutationDoesNotLeakToNextReader( )
    {
        Bot bot = createBot( );

        BotHome.findByPrimaryKey( bot.getId( ) );
        assertNotNull( _botCache.get( BotCacheService.getKey( bot.getId( ) ) ), "precondition: the bot must actually be served from the cache" );

        Optional<Bot> firstRead = BotHome.findByPrimaryKey( bot.getId( ) );
        assertTrue( firstRead.isPresent( ) );
        firstRead.get( ).setUserCanModify( true );
        firstRead.get( ).setBotSystemPrompt( "corrupted by previous reader" );

        Optional<Bot> secondRead = BotHome.findByPrimaryKey( bot.getId( ) );
        assertTrue( secondRead.isPresent( ) );

        assertFalse( secondRead.get( ).isUserCanModify( ), "userCanModify set by a previous reader leaked through the bot cache" );
        assertEquals( "System prompt", secondRead.get( ).getBotSystemPrompt( ), "config mutation by a previous reader leaked through the bot cache" );
    }

    /**
     * Creates and persists a client.
     *
     * @param strCode
     *            the client code
     * @return the persisted client
     */
    private Client createClient( String strCode )
    {
        Client client = new Client( );
        client.setName( "Client " + strCode );
        client.setCode( strCode );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates and persists a provider.
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
     * Creates and persists a bot with its required foreign keys.
     *
     * @return the persisted bot
     */
    private Bot createBot( )
    {
        Client client = createClient( "CLIENT_BOT_ISOLATION" );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );

        Bot bot = new Bot( );
        bot.setBotName( "Isolation Bot" );
        bot.setBotDescription( "Isolation test bot" );
        bot.setBotSystemPrompt( "System prompt" );
        bot.setClientId( client.getId( ) );
        bot.setLlmProviderId( llmProvider.getId( ) );
        bot.setEmbedProviderId( embedProvider.getId( ) );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        BotHome.create( bot );
        return bot;
    }

    /**
     * Finds a client by id in a list.
     *
     * @param clients
     *            the list to search
     * @param nId
     *            the client id
     * @return the matching client
     */
    private Client findById( List<Client> clients, int nId )
    {
        return clients.stream( ).filter( c -> c.getId( ) == nId ).findFirst( ).orElseThrow( );
    }
}
