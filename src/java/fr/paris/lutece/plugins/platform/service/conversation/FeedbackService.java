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
import java.util.Optional;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedbackHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageHome;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.api.user.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service class for managing conversation message feedback. Provides methods for creating, updating, and retrieving feedback with proper authorization.
 */
@ApplicationScoped
public class FeedbackService
{
    private static final String ERROR_MESSAGE_NOT_FOUND = "Message not found";
    private static final String ERROR_BOT_NOT_FOUND = "Bot not found";
    private static final String ERROR_NO_SUBSCRIPTION = "No active subscription to this bot";
    private static final String ERROR_INVALID_MESSAGE_ACCESS = "Access denied to this message";
    private static final String ERROR_FEEDBACK_NOT_FOUND = "Feedback not found";
    private static final String ERROR_USER_ID_REQUIRED = "User ID is required";
    private static final String ERROR_MESSAGE_ID_REQUIRED = "Message ID is required";
    private static final String ERROR_FEEDBACK_ALREADY_EXISTS = "Feedback already exists for this message";
    private static final String ERROR_CONVERSATION_NOT_FOUND = "Conversation not found";
    private static final String ERROR_MODIFY_DENIED = "Not allowed to modify this bot's feedback";

    private static final String BOT_RESOURCE_TYPE = Bot.RESOURCE_TYPE;
    @Inject
    private SubscriptionService subscriptionService;

    /**
     * Default constructor for CDI.
     */
    FeedbackService( )
    {
    }

    /**
     * Creates a new feedback for a message from a client
     *
     * @param messageId
     *            The message ID
     * @param userId
     *            The user ID
     * @param isPositive
     *            Whether the feedback is positive
     * @param comment
     *            Optional comment
     * @param client
     *            The client making the request
     * @return The created feedback
     * @throws InvalidRequestException
     *             if the request is invalid
     * @throws AccessDeniedException
     *             if the client doesn't have access
     */
    public ConversationMessageFeedback createFeedbackForClient( int messageId, String userId, boolean isPositive, String comment, Client client )
    {
        validateBasicRequest( messageId, userId );

        ConversationMessage message = getAndValidateMessage( messageId );
        BotConversation conversation = getAndValidateConversation( message.getConversationId( ) );
        Bot bot = getAndValidateBot( conversation.getBotId( ) );

        validateClientAccess( client, bot.getId( ) );
        validateConversationAccess( conversation, userId );

        Optional<ConversationMessageFeedback> existingFeedback = ConversationMessageFeedbackHome.findByMessageId( messageId );
        if ( existingFeedback.isPresent( ) )
        {
            throw new InvalidRequestException( ERROR_FEEDBACK_ALREADY_EXISTS );
        }

        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( messageId );
        feedback.setUserId( userId );
        feedback.setBotId( bot.getId( ) );
        feedback.setClientId( client.getId( ) );
        feedback.setIsPositive( isPositive );
        feedback.setComment( comment );
        feedback.setStatus( ConversationMessageFeedback.Status.PENDING );

        return ConversationMessageFeedbackHome.create( feedback );
    }

    /**
     * Creates a new feedback for a message from an admin user
     *
     * @param messageId
     *            The message ID
     * @param userId
     *            The user ID
     * @param isPositive
     *            Whether the feedback is positive
     * @param comment
     *            Optional comment
     * @param user
     *            The admin user making the request
     * @return The created feedback
     * @throws InvalidRequestException
     *             if the request is invalid
     */
    public ConversationMessageFeedback createFeedbackForAdmin( int messageId, boolean isPositive, String comment, User user )
    {
        if ( user == null )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }
        validateBasicRequest( messageId, user.getAccessCode( ) );

        ConversationMessage message = getAndValidateMessage( messageId );

        BotConversation conversation = getAndValidateConversation( message.getConversationId( ) );
        validateConversationAccess( conversation, user.getAccessCode( ) );

        Bot bot = getAndValidateBot( conversation.getBotId( ) );
        Optional<ConversationMessageFeedback> existingFeedback = ConversationMessageFeedbackHome.findByMessageId( messageId );
        if ( existingFeedback.isPresent( ) )
        {
            throw new InvalidRequestException( ERROR_FEEDBACK_ALREADY_EXISTS );
        }

        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( messageId );
        feedback.setUserId( user.getAccessCode( ) );
        feedback.setBotId( bot.getId( ) );
        feedback.setIsPositive( isPositive );
        feedback.setComment( comment );
        feedback.setClientId( bot.getClientId( ) );
        feedback.setStatus( ConversationMessageFeedback.Status.PENDING );

        return ConversationMessageFeedbackHome.create( feedback );
    }

    /**
     * Updates the status of a feedback. No authorization here: the public entry point (markFeedbackProcessed) enforces the RBAC gate.
     *
     * @param feedbackId
     *            The feedback ID
     * @param status
     *            The new status
     * @param adminUser
     *            The admin user making the request
     * @return The updated feedback
     * @throws InvalidRequestException
     *             if the request is invalid
     */
    private ConversationMessageFeedback updateFeedbackStatus( int feedbackId, ConversationMessageFeedback.Status status )
    {
        ConversationMessageFeedback feedback = ConversationMessageFeedbackHome.findByPrimaryKey( feedbackId )
                .orElseThrow( ( ) -> new InvalidRequestException( ERROR_FEEDBACK_NOT_FOUND ) );
        feedback.setStatus( status );

        return ConversationMessageFeedbackHome.update( feedback );
    }

    /**
     * Gets the feedbacks for a bot, optionally filtered by status, each enriched with the UUID of its owning conversation. The conversation join resolves the
     * message then its conversation for every feedback, so the controller does not have to orchestrate that lookup.
     *
     * @param botId
     *            the bot ID
     * @param status
     *            the status to filter by, or {@code null} to return every status
     * @return the enriched feedbacks
     * @throws InvalidRequestException
     *             if the bot is not found
     */
    public List<ConversationMessageFeedback> getEnrichedFeedbacksForBot( int botId, ConversationMessageFeedback.Status status )
    {
        getAndValidateBot( botId );
        List<ConversationMessageFeedback> feedbacks = status != null ? ConversationMessageFeedbackHome.getConversationMessageFeedbacksList( botId, status )
                : ConversationMessageFeedbackHome.getConversationMessageFeedbacksList( botId );
        for ( ConversationMessageFeedback feedback : feedbacks )
        {
            enrichWithConversationUuid( feedback );
        }
        return feedbacks;
    }

    /**
     * Marks a feedback as processed after checking that the user is allowed to modify the given bot. Combines the authorization rule and the status update so
     * the controller only has to redirect.
     *
     * @param feedbackId
     *            the feedback ID
     * @param botId
     *            the bot ID
     * @param user
     *            the authenticated user
     * @throws InvalidRequestException
     *             if the bot is not found
     * @throws AccessDeniedException
     *             if the user is not allowed to modify the bot
     */
    public void markFeedbackProcessed( int feedbackId, int botId, User user )
    {
        Bot bot = getAndValidateBot( botId );
        AgentRBACService.enrichWithPermissions( bot, user );
        if ( !bot.isUserCanModify( ) )
        {
            throw new AccessDeniedException( ERROR_MODIFY_DENIED );
        }
        updateFeedbackStatus( feedbackId, ConversationMessageFeedback.Status.PROCESSED );
    }

    /**
     * Sets the conversation UUID on the given feedback by resolving its message then the owning conversation.
     *
     * @param feedback
     *            the feedback to enrich
     */
    private void enrichWithConversationUuid( ConversationMessageFeedback feedback )
    {
        ConversationMessageHome.findByPrimaryKey( feedback.getMessageId( ) )
                .flatMap( message -> BotConversationHome.findByPrimaryKey( message.getConversationId( ) ) )
                .ifPresent( conv -> feedback.setConversationUuid( conv.getConversationUuid( ) ) );
    }

    /**
     * Gets feedback by message ID
     *
     * @param messageId
     *            The message ID
     * @return The feedback if found
     */
    public Optional<ConversationMessageFeedback> getFeedbackByMessageId( int messageId )
    {
        return ConversationMessageFeedbackHome.findByMessageId( messageId );
    }

    /**
     * Validates the basic request parameters, ensuring the message ID is positive and the user ID is not blank
     *
     * @param messageId
     *            The message ID
     * @param userId
     *            The user ID
     * @throws InvalidRequestException
     *             if the message ID or user ID is invalid
     */
    private void validateBasicRequest( int messageId, String userId )
    {
        if ( messageId <= 0 )
        {
            throw new InvalidRequestException( ERROR_MESSAGE_ID_REQUIRED );
        }
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }
    }

    /**
     * Retrieves a message by its ID and validates that it exists
     *
     * @param messageId
     *            The message ID
     * @return The found message
     * @throws InvalidRequestException
     *             if the message is not found
     */
    private ConversationMessage getAndValidateMessage( int messageId )
    {
        return ConversationMessageHome.findByPrimaryKey( messageId ).orElseThrow( ( ) -> new InvalidRequestException( ERROR_MESSAGE_NOT_FOUND ) );
    }

    /**
     * Retrieves a conversation by its ID and validates that it exists
     *
     * @param conversationId
     *            The conversation ID
     * @return The found conversation
     * @throws InvalidRequestException
     *             if the conversation is not found
     */
    private BotConversation getAndValidateConversation( int conversationId )
    {
        return BotConversationHome.findByPrimaryKey( conversationId ).orElseThrow( ( ) -> new InvalidRequestException( ERROR_CONVERSATION_NOT_FOUND ) );
    }

    /**
     * Retrieves a bot by its ID and validates that it exists
     *
     * @param botId
     *            The bot ID
     * @return The found bot
     * @throws InvalidRequestException
     *             if the bot is not found
     */
    private Bot getAndValidateBot( int botId )
    {
        return BotHome.findByPrimaryKey( botId ).orElseThrow( ( ) -> new InvalidRequestException( ERROR_BOT_NOT_FOUND ) );
    }

    /**
     * Validates that the client has an active subscription to the given bot
     *
     * @param client
     *            The client
     * @param botId
     *            The bot ID
     * @throws AccessDeniedException
     *             if the client has no active subscription to the bot
     */
    private void validateClientAccess( Client client, int botId )
    {
        if ( !subscriptionService.hasActiveSubscription( client.getId( ), BOT_RESOURCE_TYPE, String.valueOf( botId ) ) )
        {
            throw new AccessDeniedException( ERROR_NO_SUBSCRIPTION );
        }
    }

    /**
     * Validates that the given user owns the conversation
     *
     * @param conversation
     *            The conversation
     * @param userId
     *            The user ID
     * @throws AccessDeniedException
     *             if the user does not own the conversation
     */
    private void validateConversationAccess( BotConversation conversation, String userId )
    {
        if ( !conversation.getUserId( ).equals( userId ) )
        {
            throw new AccessDeniedException( ERROR_INVALID_MESSAGE_ACCESS );
        }
    }
}
