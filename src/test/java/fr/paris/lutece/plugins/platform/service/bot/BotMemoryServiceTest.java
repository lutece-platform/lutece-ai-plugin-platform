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

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link BotMemoryService} against HSQL. These cover the conversation lifecycle the service is responsible for: resolving an existing
 * conversation by UUID, creating a fresh one when the UUID is null or unknown, listing a user's conversations, and the ownership guard that prevents deleting
 * another user's conversation.
 */
public class BotMemoryServiceTest extends AbstractPlatformDbTest
{
    private Client _client;
    private Provider _llmProvider;
    private Provider _embedProvider;
    private Bot _bot;

    /**
     * Resolves the CDI-managed memory service.
     *
     * @return the memory service instance
     */
    private BotMemoryService service( )
    {
        return CDI.current( ).select( BotMemoryService.class ).get( );
    }

    /**
     * Builds the foreign-key parent chain (client, two providers, bot) shared by every test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client MemorySvc Test" );
        _client.setCode( "CLIENT_MEMORYSVC_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _llmProvider = createProvider( "LLM" );
        _embedProvider = createProvider( "EMBEDDING" );
        _bot = createBot( );
    }

    /**
     * Removes the shared parent chain; conversations cascade on bot removal.
     */
    @AfterEach
    public void tearDown( )
    {
        BotHome.remove( _bot.getId( ) );
        ProviderHome.remove( _llmProvider.getId( ) );
        ProviderHome.remove( _embedProvider.getId( ) );
        ClientHome.remove( _client.getId( ) );
    }

    /**
     * A null UUID creates a new conversation and returns its generated UUID, persisted against the bot and user.
     */
    @Test
    public void testGetOrCreateWithNullUuidCreates( )
    {
        String userId = "user-" + UUID.randomUUID( );

        String uuid = service( ).getOrCreateConversation( _bot.getId( ), null, userId );

        assertNotNull( uuid );
        assertTrue( BotConversationHome.findByUuid( uuid ).isPresent( ) );
        assertEquals( userId, BotConversationHome.findByUuid( uuid ).get( ).getUserId( ) );
    }

    /**
     * Passing an existing UUID returns that same conversation rather than creating a new one.
     */
    @Test
    public void testGetOrCreateWithExistingUuidReturnsSame( )
    {
        String userId = "user-" + UUID.randomUUID( );
        String first = service( ).getOrCreateConversation( _bot.getId( ), null, userId );

        String second = service( ).getOrCreateConversation( _bot.getId( ), first, userId );

        assertEquals( first, second );
        assertEquals( 1, BotConversationHome.getConversationsByUserId( userId ).size( ) );
    }

    /**
     * An unknown UUID is treated as a new conversation and yields a different UUID.
     */
    @Test
    public void testGetOrCreateWithUnknownUuidCreatesNew( )
    {
        String userId = "user-" + UUID.randomUUID( );

        String uuid = service( ).getOrCreateConversation( _bot.getId( ), "does-not-exist", userId );

        assertNotNull( uuid );
        assertNotEquals( "does-not-exist", uuid );
        assertTrue( BotConversationHome.findByUuid( uuid ).isPresent( ) );
    }

    /**
     * The listing returns exactly the conversations belonging to the given user.
     */
    @Test
    public void testGetConversationsByUserId( )
    {
        String userId = "user-" + UUID.randomUUID( );
        service( ).getOrCreateConversation( _bot.getId( ), null, userId );
        service( ).getOrCreateConversation( _bot.getId( ), null, userId );

        List<BotConversation> conversations = service( ).getConversationsByUserId( userId );

        assertEquals( 2, conversations.size( ) );
    }

    /**
     * The owner can delete their conversation.
     */
    @Test
    public void testDeleteConversationByOwner( )
    {
        String userId = "user-" + UUID.randomUUID( );
        String uuid = service( ).getOrCreateConversation( _bot.getId( ), null, userId );

        service( ).deleteConversation( uuid, userId );

        assertTrue( BotConversationHome.findByUuid( uuid ).isEmpty( ) );
    }

    /**
     * A non-owner cannot delete someone else's conversation; the ownership guard leaves it untouched.
     */
    @Test
    public void testDeleteConversationByNonOwnerIsNoOp( )
    {
        String owner = "owner-" + UUID.randomUUID( );
        String intruder = "intruder-" + UUID.randomUUID( );
        String uuid = service( ).getOrCreateConversation( _bot.getId( ), null, owner );

        service( ).deleteConversation( uuid, intruder );

        assertTrue( BotConversationHome.findByUuid( uuid ).isPresent( ) );
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
     * Creates a persisted bot wired to the shared parent chain.
     *
     * @return the persisted bot
     */
    private Bot createBot( )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot MemorySvc Test" );
        bot.setClientId( _client.getId( ) );
        bot.setLlmProviderId( _llmProvider.getId( ) );
        bot.setEmbedProviderId( _embedProvider.getId( ) );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }
}
