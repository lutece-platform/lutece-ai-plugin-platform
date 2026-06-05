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
package fr.paris.lutece.plugins.platform.business.conversation;

import java.util.List;
import java.util.Optional;

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

/**
 * CRUD test for ConversationMessageHome on HSQL
 */
public class ConversationMessageHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the create / findByPrimaryKey / getMessagesByConversationId / remove cycle of ConversationMessageHome. The foreign key parent chain (Client,
     * two Providers, Bot, BotConversation) is created first to satisfy the conversation_id foreign key, then removed at the end. ConversationMessageHome
     * exposes no update method, so the update step is intentionally omitted.
     */
    @Test
    public void testBusinessConversationMessage( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client, llmProvider, embedProvider );
        BotConversation conversation = createConversation( bot );

        ConversationMessage message = new ConversationMessage( );
        message.setConversationId( conversation.getId( ) );
        message.setMessage( "Message content 1" );
        message.setRole( "user" );

        ConversationMessageHome.create( message );
        Optional<ConversationMessage> messageStored = ConversationMessageHome.findByPrimaryKey( message.getId( ) );
        assertTrue( messageStored.isPresent( ) );
        assertEquals( message.getMessage( ), messageStored.get( ).getMessage( ) );
        assertEquals( message.getRole( ), messageStored.get( ).getRole( ) );
        assertEquals( conversation.getId( ), messageStored.get( ).getConversationId( ) );

        List<ConversationMessage> messageList = ConversationMessageHome.getMessagesByConversationId( conversation.getId( ) );
        assertNotNull( messageList );
        assertFalse( messageList.isEmpty( ) );

        ConversationMessageHome.remove( message.getId( ) );
        messageStored = ConversationMessageHome.findByPrimaryKey( message.getId( ) );
        assertTrue( messageStored.isEmpty( ) );
    }

    /**
     * Creates a persisted Client to be used as a foreign key parent of a Bot.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client Message Test" );
        client.setCode( "CLIENT_MESSAGE_TEST" );
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
     * Creates a persisted Bot to be used as a foreign key parent of a BotConversation.
     *
     * @param client
     *            The parent Client
     * @param llmProvider
     *            The parent LLM Provider
     * @param embedProvider
     *            The parent embedding Provider
     * @return The created Bot with its generated primary key
     */
    private Bot createBot( Client client, Provider llmProvider, Provider embedProvider )
    {
        Bot bot = new Bot( );
        bot.setBotName( "BotName Message Test" );
        bot.setBotDescription( "BotDescription Message Test" );
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
     * Creates a persisted BotConversation to be used as the foreign key parent of a ConversationMessage.
     *
     * @param bot
     *            The parent Bot
     * @return The created BotConversation with its generated primary key
     */
    private BotConversation createConversation( Bot bot )
    {
        BotConversation conversation = new BotConversation( );
        conversation.setBotId( bot.getId( ) );
        conversation.setConversationUuid( java.util.UUID.randomUUID( ).toString( ) );
        conversation.setUserId( "user-message-test" );
        BotConversationHome.create( conversation );
        return conversation;
    }
}
