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
 * CRUD test for ConversationMessageFeedbackHome on HSQL
 */
public class ConversationMessageFeedbackHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full create / findByPrimaryKey / update / remove cycle of ConversationMessageFeedbackHome. The foreign key parent chain (Client, two
     * Providers, Bot, BotConversation, ConversationMessage) is created first to satisfy the bot_id and message_id foreign keys, then removed at the end.
     */
    @Test
    public void testBusinessConversationMessageFeedback( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client, llmProvider, embedProvider );
        BotConversation conversation = createConversation( bot );
        ConversationMessage message = createMessage( conversation );

        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( message.getId( ) );
        feedback.setUserId( "user-feedback-test" );
        feedback.setBotId( bot.getId( ) );
        feedback.setClientId( client.getId( ) );
        feedback.setIsPositive( true );
        feedback.setComment( "Comment 1" );
        feedback.setStatus( ConversationMessageFeedback.Status.PENDING );

        ConversationMessageFeedbackHome.create( feedback );
        Optional<ConversationMessageFeedback> feedbackStored = ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) );
        assertTrue( feedbackStored.isPresent( ) );
        assertEquals( feedback.getComment( ), feedbackStored.get( ).getComment( ) );
        assertEquals( feedback.getMessageId( ), feedbackStored.get( ).getMessageId( ) );
        assertTrue( feedbackStored.get( ).isPositive( ) );
        assertEquals( ConversationMessageFeedback.Status.PENDING, feedbackStored.get( ).getStatus( ) );

        feedback.setComment( "Comment 2" );
        feedback.setIsPositive( false );
        feedback.setStatus( ConversationMessageFeedback.Status.PROCESSED );
        ConversationMessageFeedbackHome.update( feedback );
        feedbackStored = ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) );
        assertTrue( feedbackStored.isPresent( ) );
        assertEquals( feedback.getComment( ), feedbackStored.get( ).getComment( ) );
        assertFalse( feedbackStored.get( ).isPositive( ) );
        assertEquals( ConversationMessageFeedback.Status.PROCESSED, feedbackStored.get( ).getStatus( ) );

        ConversationMessageFeedbackHome.getConversationMessageFeedbacksList( bot.getId( ) );

        ConversationMessageFeedbackHome.remove( feedback.getId( ) );
        feedbackStored = ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) );
        assertTrue( feedbackStored.isEmpty( ) );
    }

    /**
     * Creates a persisted Client to be used as a foreign key parent of a Bot.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client Feedback Test" );
        client.setCode( "CLIENT_FEEDBACK_TEST" );
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
        bot.setBotName( "BotName Feedback Test" );
        bot.setBotDescription( "BotDescription Feedback Test" );
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
     * Creates a persisted BotConversation to be used as a foreign key parent of a ConversationMessage.
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
        conversation.setUserId( "user-feedback-test" );
        BotConversationHome.create( conversation );
        return conversation;
    }

    /**
     * Creates a persisted ConversationMessage to be used as the foreign key parent of a feedback.
     *
     * @param conversation
     *            The parent BotConversation
     * @return The created ConversationMessage with its generated primary key
     */
    private ConversationMessage createMessage( BotConversation conversation )
    {
        ConversationMessage message = new ConversationMessage( );
        message.setConversationId( conversation.getId( ) );
        message.setMessage( "Message content for feedback" );
        message.setRole( "assistant" );
        ConversationMessageHome.create( message );
        return message;
    }
}
