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
package fr.paris.lutece.plugins.platform.service.conversation;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedbackHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link FeedbackService} against HSQL. They exercise the enriched-feedback query — which joins each feedback to its owning conversation
 * via the message lookup and exposes the conversation UUID, with and without the status filter — and the {@code markFeedbackProcessed} flow combining the RBAC
 * modify check with the status transition. An {@link AdminUser} with no roles is used as the canonical unauthorized caller, since RBAC denies a user holding
 * none of the required roles. The full foreign key parent chain (Client, two Providers, Bot, BotConversation, ConversationMessage) is seeded for every test.
 */
public class FeedbackServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed feedback service.
     *
     * @return the feedback service instance
     */
    private FeedbackService service( )
    {
        return CDI.current( ).select( FeedbackService.class ).get( );
    }

    /**
     * Creates and persists an active client with the given code.
     *
     * @param code
     *            the unique client code
     * @return the persisted client
     */
    private Client createClient( String code )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( code );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates and persists a provider of the given type.
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
     * Creates and persists a bot bound to the given client and providers.
     *
     * @param client
     *            the parent client
     * @param llmProvider
     *            the parent LLM provider
     * @param embedProvider
     *            the parent embedding provider
     * @return the persisted bot
     */
    private Bot createBot( Client client, Provider llmProvider, Provider embedProvider )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot Feedback Service Test" );
        bot.setClientId( client.getId( ) );
        bot.setLlmProviderId( llmProvider.getId( ) );
        bot.setEmbedProviderId( embedProvider.getId( ) );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }

    /**
     * Creates and persists a conversation owning the given bot, with a freshly generated UUID.
     *
     * @param bot
     *            the parent bot
     * @return the persisted conversation
     */
    private BotConversation createConversation( Bot bot )
    {
        BotConversation conversation = new BotConversation( );
        conversation.setBotId( bot.getId( ) );
        conversation.setConversationUuid( UUID.randomUUID( ).toString( ) );
        conversation.setUserId( "user-feedback-service" );
        BotConversationHome.create( conversation );
        return conversation;
    }

    /**
     * Creates and persists a message belonging to the given conversation.
     *
     * @param conversation
     *            the parent conversation
     * @return the persisted message
     */
    private ConversationMessage createMessage( BotConversation conversation )
    {
        ConversationMessage message = new ConversationMessage( );
        message.setConversationId( conversation.getId( ) );
        message.setMessage( "Message content for feedback service test" );
        message.setRole( "assistant" );
        ConversationMessageHome.create( message );
        return message;
    }

    /**
     * Creates and persists a feedback for the given message, bot and client with the given status.
     *
     * @param messageId
     *            the parent message id
     * @param botId
     *            the bot id
     * @param clientId
     *            the client id
     * @param status
     *            the feedback status
     * @return the persisted feedback
     */
    private ConversationMessageFeedback createFeedback( int messageId, int botId, int clientId, ConversationMessageFeedback.Status status )
    {
        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( messageId );
        feedback.setUserId( "user-feedback-service" );
        feedback.setBotId( botId );
        feedback.setClientId( clientId );
        feedback.setIsPositive( true );
        feedback.setComment( "Comment" );
        feedback.setStatus( status );
        return ConversationMessageFeedbackHome.create( feedback );
    }

    /**
     * The enriched query returns every feedback of the bot when no status filter is given, and each returned feedback carries the UUID of its owning
     * conversation, resolved through the message-to-conversation join.
     */
    @Test
    public void testGetEnrichedFeedbacksForBotResolvesConversationUuid( )
    {
        Client client = createClient( "fb-svc-enrich" );
        Provider llm = createProvider( "LLM-enrich" );
        Provider embed = createProvider( "EMBEDDING-enrich" );
        Bot bot = createBot( client, llm, embed );
        BotConversation conversation = createConversation( bot );
        ConversationMessage message = createMessage( conversation );
        createFeedback( message.getId( ), bot.getId( ), client.getId( ), ConversationMessageFeedback.Status.PENDING );

        List<ConversationMessageFeedback> enriched = service( ).getEnrichedFeedbacksForBot( bot.getId( ), null );
        assertEquals( 1, enriched.size( ), "the bot has exactly one feedback" );
        assertEquals( conversation.getConversationUuid( ), enriched.get( 0 ).getConversationUuid( ),
                "the feedback must be enriched with its conversation UUID" );
    }

    /**
     * The status filter restricts the enriched query to feedbacks in the given status: a PROCESSED filter returns only the processed feedback, while a null
     * filter returns both.
     */
    @Test
    public void testGetEnrichedFeedbacksForBotFiltersByStatus( )
    {
        Client client = createClient( "fb-svc-filter" );
        Provider llm = createProvider( "LLM-filter" );
        Provider embed = createProvider( "EMBEDDING-filter" );
        Bot bot = createBot( client, llm, embed );
        BotConversation conversation = createConversation( bot );
        ConversationMessage pendingMsg = createMessage( conversation );
        ConversationMessage processedMsg = createMessage( conversation );
        createFeedback( pendingMsg.getId( ), bot.getId( ), client.getId( ), ConversationMessageFeedback.Status.PENDING );
        createFeedback( processedMsg.getId( ), bot.getId( ), client.getId( ), ConversationMessageFeedback.Status.PROCESSED );

        List<ConversationMessageFeedback> all = service( ).getEnrichedFeedbacksForBot( bot.getId( ), null );
        assertEquals( 2, all.size( ), "without filter both feedbacks are returned" );

        List<ConversationMessageFeedback> onlyProcessed = service( ).getEnrichedFeedbacksForBot( bot.getId( ), ConversationMessageFeedback.Status.PROCESSED );
        assertEquals( 1, onlyProcessed.size( ), "the PROCESSED filter returns a single feedback" );
        assertEquals( ConversationMessageFeedback.Status.PROCESSED, onlyProcessed.get( 0 ).getStatus( ), "the returned feedback is the processed one" );
        assertEquals( conversation.getConversationUuid( ), onlyProcessed.get( 0 ).getConversationUuid( ), "filtered feedbacks are also enriched" );
    }

    /**
     * The enriched query rejects an unknown bot with the typed {@link InvalidRequestException}, since the bot existence is validated before any feedback is
     * loaded.
     */
    @Test
    public void testGetEnrichedFeedbacksForUnknownBotIsRejected( )
    {
        FeedbackService service = service( );
        assertThrows( InvalidRequestException.class, ( ) -> service.getEnrichedFeedbacksForBot( 999999, null ),
                "an unknown bot must raise InvalidRequestException" );
    }

    /**
     * A caller with no roles is denied by the RBAC modify gate: {@code markFeedbackProcessed} throws {@link AccessDeniedException} and the feedback status is
     * left untouched.
     */
    @Test
    public void testMarkFeedbackProcessedDeniesUnauthorizedUser( )
    {
        Client client = createClient( "fb-svc-rbac" );
        Provider llm = createProvider( "LLM-rbac" );
        Provider embed = createProvider( "EMBEDDING-rbac" );
        Bot bot = createBot( client, llm, embed );
        BotConversation conversation = createConversation( bot );
        ConversationMessage message = createMessage( conversation );
        ConversationMessageFeedback feedback = createFeedback( message.getId( ), bot.getId( ), client.getId( ), ConversationMessageFeedback.Status.PENDING );
        AdminUser user = new AdminUser( );
        FeedbackService service = service( );

        assertThrows( AccessDeniedException.class, ( ) -> service.markFeedbackProcessed( feedback.getId( ), bot.getId( ), user ),
                "a user with no roles cannot modify the bot's feedback" );
        assertEquals( ConversationMessageFeedback.Status.PENDING, ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) ).get( ).getStatus( ),
                "the denied call must not change the feedback status" );
    }

    /**
     * The bot is validated before the RBAC gate, so marking a feedback for an unknown bot raises the typed {@link InvalidRequestException}.
     */
    @Test
    public void testMarkFeedbackProcessedForUnknownBotIsRejected( )
    {
        AdminUser user = new AdminUser( );
        FeedbackService service = service( );
        assertThrows( InvalidRequestException.class, ( ) -> service.markFeedbackProcessed( 1, 999999, user ),
                "an unknown bot must raise InvalidRequestException before any feedback update" );
    }

    /**
     * createFeedbackForAdmin must reject a null user with the typed InvalidRequestException instead of crashing with a NullPointerException.
     */
    @Test
    public void testCreateFeedbackForAdminRejectsNullUser( )
    {
        FeedbackService service = service( );
        assertThrows( InvalidRequestException.class, ( ) -> service.createFeedbackForAdmin( 1, true, "comment", null ),
                "a null user must raise the typed InvalidRequestException, not a NullPointerException" );
    }
}
