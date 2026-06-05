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

import fr.paris.lutece.plugins.platform.business.bot.RateLimitDTO;
import fr.paris.lutece.plugins.platform.service.security.RateLimitResult;
import fr.paris.lutece.plugins.platform.business.dataset.DocumentFileDTO;
import fr.paris.lutece.plugins.platform.business.bot.QueryRequestDTO;
import fr.paris.lutece.plugins.platform.business.bot.BotUserRateLimit;
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.service.bot.BotRateLimitService;
import fr.paris.lutece.plugins.platform.service.bot.BotService;
import fr.paris.lutece.plugins.platform.service.sse.PlatformSseStreamManager;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.service.conversation.FeedbackService;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import java.util.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.sse.*;

/**
 * Public bot API. Business exceptions raised by the services bubble up to the JAX-RS {@code ExceptionMapper} providers (see {@code rs.mapper}); only SSE setup
 * keeps a local handler because it must close the event sink rather than return a response.
 */
@ApplicationScoped
@Path( "platform/agent/api" )
public class BotAPIRest
{
    private static final String DELETE_CONVERSATION_PATH = "conversation/{conversation_uuid}/{user_id}";
    private static final String DELETE_ALL_CONVERSATIONS_PATH = "user/{user_id}/conversations";
    private static final String USER_RATE_LIMITS_PATH = "user/rate";

    private static final String USER_ID_REQUIRED_MESSAGE = "Votre identifiant utilisateur est requis pour cette opération.";
    private static final String CONVERSATION_DELETED_MESSAGE = "La conversation a été supprimée avec succès.";
    private static final String ALL_CONVERSATIONS_DELETED_MESSAGE = "Toutes vos conversations ont été supprimées avec succès.";
    private static final String HIGH_TRAFFIC_MESSAGE = "Nous expérimentons un fort trafic actuellement. Veuillez réessayer dans quelques instants.";
    private static final String TOO_MANY_CONCURRENT_CONVERSATIONS_MESSAGE = "Vous avez trop de conversations actives. Vous ne pouvez pas lancer plusieurs conversations simultanément.";
    private static final String FEEDBACK_CREATED_MESSAGE = "Votre feedback a été enregistré avec succès.";

    @Context
    private HttpServletRequest _request;

    @Inject
    private BotService botService;
    @Inject
    private PlatformSseStreamManager sseStreamManager;
    @Inject
    private AuthenticationService authenticationService;
    @Inject
    private BotRateLimitService rateLimitService;
    @Inject
    private FeedbackService feedbackService;

    /**
     * Lists the bots the authenticated client is authorized to use.
     *
     * @return a JSON response with the authorized bots
     */
    @GET
    @Path( BotRestConstants.BOTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getBots( )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        return createSuccessResponse( botService.getAuthorizedBotsForClient( client ) );
    }

    /**
     * Initializes a streaming query and returns the stream identifier to connect to.
     *
     * @param request
     *            the query request
     * @return a JSON response with the stream id and conversation uuid
     */
    @POST
    @Path( BotRestConstants.QUERY_STREAM_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void streamQuery( @NotNull @Valid QueryRequestDTO request, @Context SseEventSink eventSink, @Context Sse sse )
    {
        Client client = authenticationService.authenticateClient( _request, Bot.RESOURCE_TYPE, String.valueOf( request.getBotId( ) ) );
        Bot bot = botService.validateStreamingQueryForClient( request, client );
        String userId = request.getUserId( );

        if ( !sseStreamManager.canCreateNewStream( ) )
        {
            throw streamError( Response.Status.TOO_MANY_REQUESTS, "TOO_MANY_CONCURRENT_STREAMS", HIGH_TRAFFIC_MESSAGE );
        }
        if ( !sseStreamManager.canCreateNewStreamForClientUser( client.getId( ), userId ) )
        {
            throw streamError( Response.Status.TOO_MANY_REQUESTS, "TOO_MANY_CONCURRENT_STREAMS", TOO_MANY_CONCURRENT_CONVERSATIONS_MESSAGE );
        }

        RateLimitResult rateLimitResult = rateLimitService.checkRateLimit( userId, request.getBotId( ) );
        if ( !rateLimitResult.isAllowed( ) )
        {
            Response.Status status = rateLimitResult.isInternalError( ) ? Response.Status.INTERNAL_SERVER_ERROR : Response.Status.TOO_MANY_REQUESTS;
            String code = rateLimitResult.isInternalError( ) ? "INTERNAL_ERROR" : "RATE_LIMIT_EXCEEDED";
            throw streamError( status, code, rateLimitResult.getErrorMessage( ) );
        }

        String operationId = UUID.randomUUID( ).toString( );
        sseStreamManager.registerSseStream( operationId, Bot.RESOURCE_TYPE, eventSink, sse, client.getId( ), userId );
        botService.startStreamingQuery( bot, request, operationId );
    }

    /**
     * Builds a {@link WebApplicationException} carrying a JSON error body, thrown before the SSE response is committed so the client receives a clean HTTP
     * status rather than a streamed error.
     *
     * @param status
     *            the HTTP status
     * @param code
     *            the stable error code
     * @param message
     *            the human-readable message
     * @return the exception to throw
     */
    private WebApplicationException streamError( Response.Status status, String code, String message )
    {
        return new WebApplicationException(
                Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( code, message ) ) ).type( MediaType.APPLICATION_JSON ).build( ) );
    }

    /**
     * Returns the message history of a conversation for the authenticated client.
     *
     * @param conversationUuid
     *            the conversation uuid
     * @param requestBody
     *            the request body carrying the user id
     * @return a JSON response with the conversation messages
     */
    @POST
    @Path( BotRestConstants.CONVERSATION_HISTORY_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getConversationHistory( @PathParam( "conversation_uuid" ) String conversationUuid, Map<String, String> requestBody )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        String userId = requestBody.get( "userId" );
        return createSuccessResponse( botService.getConversationHistoryForClient( conversationUuid, userId, client ) );
    }

    /**
     * Returns the conversations of a user for the authenticated client.
     *
     * @param requestBody
     *            the request body carrying the user id
     * @return a JSON response with the user conversations
     */
    @POST
    @Path( BotRestConstants.USER_CONVERSATIONS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getUserConversations( Map<String, String> requestBody )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        String userId = requestBody.get( "userId" );
        return createSuccessResponse( botService.getUserConversationsForClient( userId, client ) );
    }

    /**
     * Deletes a conversation for the authenticated client.
     *
     * @param conversationUuid
     *            the conversation uuid
     * @param userId
     *            the user id
     * @return a JSON success response
     */
    @DELETE
    @Path( DELETE_CONVERSATION_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response deleteConversation( @PathParam( "conversation_uuid" ) String conversationUuid, @PathParam( "user_id" ) String userId )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        botService.deleteConversationForClient( conversationUuid, userId, client );
        return createSuccessResponse( CONVERSATION_DELETED_MESSAGE );
    }

    /**
     * Deletes all conversations of a user for the authenticated client.
     *
     * @param userId
     *            the user id
     * @return a JSON success response
     */
    @DELETE
    @Path( DELETE_ALL_CONVERSATIONS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response deleteAllUserConversations( @PathParam( "user_id" ) String userId )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        botService.deleteAllUserConversationsForClient( userId, client );
        return createSuccessResponse( ALL_CONVERSATIONS_DELETED_MESSAGE );
    }

    /**
     * Returns the rate limits of a user for the authenticated client.
     *
     * @param requestBody
     *            the request body carrying the user id
     * @return a JSON response with the user rate limits
     */
    @POST
    @Path( USER_RATE_LIMITS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getUserRateLimits( Map<String, String> requestBody )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        String userId = requestBody.get( "userId" );

        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "BAD_REQUEST", USER_ID_REQUIRED_MESSAGE ) ) ).build( );
        }

        List<RateLimitDTO> userRateLimits = rateLimitService.getUserRateLimits( userId, client ).stream( ).map( BotAPIRest::toRateLimitDTO ).toList( );
        return createSuccessResponse( userRateLimits );
    }

    /**
     * Streams a document file the authenticated client is authorized to access.
     *
     * @param botId
     *            the bot id
     * @param datasetId
     *            the dataset id
     * @param documentId
     *            the document id
     * @return a binary response carrying the document file
     */
    @GET
    @Path( BotRestConstants.DOCUMENT_FILE_PATH )
    @Produces( MediaType.WILDCARD )
    public Response getDocumentFile( @PathParam( "bot_id" ) int botId, @PathParam( "dataset_id" ) int datasetId, @PathParam( "document_id" ) int documentId )
    {
        Client client = authenticationService.authenticateClient( _request, Bot.RESOURCE_TYPE, String.valueOf( botId ) );

        DocumentFileDTO documentFile = botService.getDocumentFileForClient( botId, datasetId, documentId, client );

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
     * Records a feedback on a bot message for the authenticated client.
     *
     * @param messageId
     *            the message id
     * @param requestBody
     *            the request body carrying user id, positivity and optional comment
     * @return a JSON success response
     */
    @POST
    @Path( BotRestConstants.MESSAGE_FEEDBACK_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createMessageFeedback( @PathParam( "message_id" ) int messageId, Map<String, Object> requestBody )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        String userId = (String) requestBody.get( "userId" );
        Boolean isPositive = (Boolean) requestBody.get( "isPositive" );
        String comment = (String) requestBody.get( "comment" );

        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "BAD_REQUEST", USER_ID_REQUIRED_MESSAGE ) ) ).build( );
        }

        if ( isPositive == null )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "BAD_REQUEST", "Le type de feedback (positif/négatif) est requis." ) ) )
                    .build( );
        }

        feedbackService.createFeedbackForClient( messageId, userId, isPositive, comment, client );
        return createSuccessResponse( FEEDBACK_CREATED_MESSAGE );
    }

    /**
     * Wraps a payload into a standard JSON success response.
     *
     * @param data
     *            the payload to serialize
     * @return a 200 JSON response
     */
    private Response createSuccessResponse( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Maps a rate limit entity to its API DTO.
     *
     * @param limit
     *            the rate limit entity
     * @return the DTO
     */
    private static RateLimitDTO toRateLimitDTO( BotUserRateLimit limit )
    {
        RateLimitDTO dto = new RateLimitDTO( );
        dto.setUserId( limit.getUserId( ) );
        dto.setBotId( limit.getBotId( ) );
        dto.setMessageCount( limit.getMessageCount( ) );
        dto.setDateFirstMessage( limit.getDateFirstMessage( ) );
        return dto;
    }
}
