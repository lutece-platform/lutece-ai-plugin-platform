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
 * Cascade test for {@link ConversationMessageHome} on HSQL.
 *
 * <p>
 * Proves that the {@code ON DELETE CASCADE} foreign key from {@code platform_conversation_message_feedback.message_id} to {@code platform_conversation_message}
 * is enforced: removing the root message must wipe its feedback children. HSQL honours foreign keys, so the cascade is exercised for real.
 * </p>
 */
public class ConversationMessageCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full graph (Client, two Providers, Bot, BotConversation, root ConversationMessage, one ConversationMessageFeedback child), asserts the child
     * exists, removes the root message once, then asserts the feedback child was cascaded away. The remaining FK parents are torn down at the end.
     */
    @Test
    public void testCascadeOnDeleteConversationMessage( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client, llmProvider, embedProvider );
        BotConversation conversation = createConversation( bot );

        ConversationMessage message = new ConversationMessage( );
        message.setConversationId( conversation.getId( ) );
        message.setMessage( "Cascade message content" );
        message.setRole( "assistant" );
        ConversationMessageHome.create( message );

        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( message.getId( ) );
        feedback.setUserId( "user-cascade-test" );
        feedback.setBotId( bot.getId( ) );
        feedback.setClientId( client.getId( ) );
        feedback.setIsPositive( true );
        feedback.setComment( "Cascade feedback comment" );
        feedback.setStatus( ConversationMessageFeedback.Status.PENDING );
        ConversationMessageFeedbackHome.create( feedback );

        assertTrue( ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) ).isPresent( ) );
        assertFalse( ConversationMessageFeedbackHome.findByMessageId( message.getId( ) ).isEmpty( ) );

        ConversationMessageHome.remove( message.getId( ) );

        assertTrue( ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) ).isEmpty( ) );
        assertTrue( ConversationMessageFeedbackHome.findByMessageId( message.getId( ) ).isEmpty( ) );
    }

    /**
     * Creates a persisted Client to be used as a foreign key parent of a Bot.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client Message Cascade Test" );
        client.setCode( "CLIENT_MESSAGE_CASCADE_TEST" );
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
     * Creates a persisted Bot to be used as a foreign key parent of a BotConversation and a feedback.
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
        bot.setBotName( "BotName Message Cascade Test" );
        bot.setBotDescription( "BotDescription Message Cascade Test" );
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
        conversation.setUserId( "user-message-cascade-test" );
        BotConversationHome.create( conversation );
        return conversation;
    }
}
