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
package fr.paris.lutece.plugins.platform.rs.bot;

import fr.paris.lutece.plugins.platform.business.conversation.FeedbackDTO;
import fr.paris.lutece.plugins.platform.service.bot.BotService;
import fr.paris.lutece.plugins.platform.service.sse.PlatformSseStreamManager;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.DocumentFileDTO;
import fr.paris.lutece.plugins.platform.business.bot.QueryRequestDTO;
import fr.paris.lutece.plugins.platform.business.bot.UserDTO;
import fr.paris.lutece.plugins.platform.service.conversation.FeedbackService;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.sse.*;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Path( "platform/agent/admin" )
public class BotAdminRest
{
    private static final String DELETE_CONVERSATION_PATH = "conversation/{conversation_uuid}";
    private static final String DELETE_ALL_CONVERSATIONS_PATH = "user/conversations";
    private static final String USER = "user";
    private static final String CONVERSATION_DELETED_MESSAGE = "Conversation deleted successfully";
    private static final String ALL_CONVERSATIONS_DELETED_MESSAGE = "All conversations deleted successfully";
    private static final String USER_NOT_AUTHENTICATED_MESSAGE = "User not authenticated";
    private static final String IS_POSITIVE_KEY = "isPositive";
    private static final String COMMENT_KEY = "comment";
    private static final String FEEDBACK_REQUIRED_MESSAGE = "Feedback type (positive/negative) is required";
    private static final String HIGH_TRAFFIC_MESSAGE = "Nous expérimentons un fort trafic actuellement. Veuillez réessayer dans quelques instants.";

    @Context
    private HttpServletRequest _request;

    @Inject
    private BotService botService;
    @Inject
    private PlatformSseStreamManager sseStreamManager;
    @Inject
    private FeedbackService feedbackService;

    /**
     * Returns the authenticated Lutece user or throws 401.
     *
     * @return the authenticated LuteceUser
     */
    private LuteceUser getAuthenticatedUser( )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            throw new WebApplicationException( Response.status( Response.Status.UNAUTHORIZED )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "UNAUTHORIZED", USER_NOT_AUTHENTICATED_MESSAGE ) ) )
                    .type( MediaType.APPLICATION_JSON ).build( ) );
        }
        return user;
    }

    /**
     * Returns the bots authorized for the authenticated user.
     *
     * @return a JSON response containing the list of authorized bots
     */
    @GET
    @Path( BotRestConstants.BOTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getBots( )
    {
        LuteceUser user = getAuthenticatedUser( );
        return createSuccessResponse( botService.getAuthorizedBots( user ) );
    }

    /**
     * Streams a bot query as Server-Sent Events over a single POST : the response body is the event stream (no init/streamId/events split). Validation, access
     * and the global concurrency cap are checked synchronously and surface as clean JSON errors before the SSE response is committed ; once validated, the SSE
     * sink is registered and generation starts on the operation id it will emit on, then the method returns while events keep flowing.
     *
     * @param request
     *            the query request payload
     * @param eventSink
     *            the SSE event sink (the live connection)
     * @param sse
     *            the SSE context
     */
    @POST
    @Path( BotRestConstants.QUERY_STREAM_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void streamQuery( QueryRequestDTO request, @Context SseEventSink eventSink, @Context Sse sse )
    {
        LuteceUser user = getAuthenticatedUser( );
        Bot bot = botService.validateStreamingQuery( request, user );
        if ( !sseStreamManager.canCreateNewStream( ) )
        {
            throw new WebApplicationException( Response.status( Response.Status.TOO_MANY_REQUESTS )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "TOO_MANY_CONCURRENT_STREAMS", HIGH_TRAFFIC_MESSAGE ) ) ).build( ) );
        }
        String operationId = UUID.randomUUID( ).toString( );
        sseStreamManager.registerSseStream( operationId, Bot.RESOURCE_TYPE, eventSink, sse );
        botService.startStreamingQuery( bot, request, operationId );
    }

    /**
     * Returns the message history of a conversation for the authenticated user.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @return a JSON response containing the conversation UUID and its messages
     */
    @GET
    @Path( BotRestConstants.CONVERSATION_HISTORY_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getConversationHistory( @PathParam( "conversation_uuid" ) String conversationUuid )
    {
        LuteceUser user = getAuthenticatedUser( );
        return createSuccessResponse( botService.getConversationHistory( conversationUuid, user ) );
    }

    /**
     * Returns the list of conversations belonging to the authenticated user.
     *
     * @return a JSON response containing the user's conversations
     */
    @GET
    @Path( BotRestConstants.USER_CONVERSATIONS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getUserConversations( )
    {
        LuteceUser user = getAuthenticatedUser( );
        return createSuccessResponse( botService.getUserConversations( user ) );
    }

    /**
     * Deletes a conversation belonging to the authenticated user.
     *
     * @param conversationUuid
     *            the conversation UUID to delete
     * @return a JSON response confirming the deletion
     */
    @DELETE
    @Path( DELETE_CONVERSATION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response deleteConversation( @PathParam( "conversation_uuid" ) String conversationUuid )
    {
        LuteceUser user = getAuthenticatedUser( );
        botService.deleteConversation( conversationUuid, user );
        return createSuccessResponse( CONVERSATION_DELETED_MESSAGE );
    }

    /**
     * Deletes all conversations belonging to the authenticated user.
     *
     * @return a JSON response confirming the deletion
     */
    @DELETE
    @Path( DELETE_ALL_CONVERSATIONS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response deleteAllUserConversations( )
    {
        LuteceUser user = getAuthenticatedUser( );
        botService.deleteAllUserConversations( user );
        return createSuccessResponse( ALL_CONVERSATIONS_DELETED_MESSAGE );
    }

    /**
     * Returns a dataset document file as a downloadable or inline stream, choosing the content type and disposition from the file extension.
     *
     * @param botId
     *            the bot identifier
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     * @return a response streaming the document file with the appropriate content type and disposition
     */
    @GET
    @Path( BotRestConstants.DOCUMENT_FILE_PATH )
    @Produces( MediaType.WILDCARD )
    public Response getDocumentFile( @PathParam( "bot_id" ) int botId, @PathParam( "dataset_id" ) int datasetId, @PathParam( "document_id" ) int documentId )
    {
        LuteceUser user = getAuthenticatedUser( );
        DocumentFileDTO documentFile = botService.getDocumentFile( botId, datasetId, documentId, user );
        String fileName = documentFile.getFileName( );
        String lowerCaseFileName = fileName.toLowerCase( );
        String contentType = MediaType.APPLICATION_OCTET_STREAM;
        String contentDisposition = String.format( BotRestConstants.ATTACHMENT_FILENAME_FORMAT, fileName );

        if ( lowerCaseFileName.endsWith( ".pdf" ) )
        {
            contentType = "application/pdf";
            contentDisposition = "inline; filename=\"%s\"".formatted( fileName );
        }
        else if ( lowerCaseFileName.endsWith( ".txt" ) || lowerCaseFileName.endsWith( ".md" ) )
        {
            contentType = "text/plain; charset=UTF-8";
            contentDisposition = "inline; filename=\"%s\"".formatted( fileName );
        }
        else if ( lowerCaseFileName.matches( ".*\\.(jpg|jpeg|png|gif|bmp|svg)$" ) )
        {
            if ( lowerCaseFileName.endsWith( ".svg" ) )
            {
                contentType = "image/svg+xml";
            }
            else if ( lowerCaseFileName.endsWith( ".png" ) )
            {
                contentType = "image/png";
            }
            else if ( lowerCaseFileName.matches( ".*\\.(jpg|jpeg)$" ) )
            {
                contentType = "image/jpeg";
            }
            else if ( lowerCaseFileName.endsWith( ".gif" ) )
            {
                contentType = "image/gif";
            }
            else if ( lowerCaseFileName.endsWith( ".bmp" ) )
            {
                contentType = "image/bmp";
            }
            contentDisposition = "inline; filename=\"%s\"".formatted( fileName );
        }

        return Response.ok( documentFile.getInputStream( ), contentType ).header( HttpHeaders.CONTENT_DISPOSITION, contentDisposition ).build( );
    }

    /**
     * Returns the profile information of the authenticated user.
     *
     * @return a JSON response containing the authenticated user's information
     */
    @GET
    @Path( USER )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAuthenticatedUserInfo( )
    {
        LuteceUser user = getAuthenticatedUser( );
        return createSuccessResponse( UserDTO.fromUser( user ) );
    }

    /**
     * Creates a feedback entry on a conversation message for the authenticated admin user.
     *
     * @param messageId
     *            the message identifier
     * @param requestBody
     *            the request body holding the feedback flag and optional comment
     * @return a JSON response containing the created feedback, or a bad request response if the feedback type is missing
     */
    @POST
    @Path( BotRestConstants.MESSAGE_FEEDBACK_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createMessageFeedback( @PathParam( "message_id" ) int messageId, Map<String, Object> requestBody )
    {
        LuteceUser user = getAuthenticatedUser( );
        Boolean isPositive = (Boolean) requestBody.get( IS_POSITIVE_KEY );
        String comment = (String) requestBody.get( COMMENT_KEY );

        if ( isPositive == null )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "BAD_REQUEST", FEEDBACK_REQUIRED_MESSAGE ) ) ).build( );
        }

        ConversationMessageFeedback feedback = feedbackService.createFeedbackForAdmin( messageId, isPositive, comment, user );
        FeedbackDTO feedbackDTO = new FeedbackDTO( feedback.getId( ), feedback.getMessageId( ), feedback.getUserId( ), feedback.getBotId( ),
                feedback.isPositive( ), feedback.getComment( ), feedback.getStatus( ).getValue( ), feedback.getCreatedAt( ), feedback.getUpdatedAt( ) );
        return createSuccessResponse( feedbackDTO );
    }

    /**
     * Builds an HTTP 200 JSON success response wrapping the given data.
     *
     * @param data
     *            the payload to wrap in the JSON response
     * @return an OK response with the JSON-serialized data
     */
    private Response createSuccessResponse( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }
}
