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
package fr.paris.lutece.plugins.platform.rs.decisiontree;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationStep;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeHome;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeImageService;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeService;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;

/**
 * Admin REST API for navigating decision trees (preview mode, requires LuteceUser auth)
 */
@ApplicationScoped
@Path( "platform/agent/admin" )
public class DecisionTreeAdminRest
{
    private static final String DECISIONTREE_IMAGE_UPLOAD_PATH = "decisiontrees/{treeId}/images";

    private static final String IMAGE_CONTENT_KEY = "content";
    private static final String IMAGE_MIME_TYPE_KEY = "mimeType";
    private static final String IMAGE_INVALID_PAYLOAD_MESSAGE = "Contenu d'image invalide.";
    private static final String IMAGE_UPLOAD_FAILED_MESSAGE = "Impossible d'enregistrer l'image.";
    private static final int IMAGE_MAX_BYTES = 5 * 1024 * 1024;

    private static final String TREE_NOT_FOUND_MESSAGE = "L'arbre de d\u00e9cision demand\u00e9 n'existe pas.";
    private static final String NODE_NOT_FOUND_MESSAGE = "Le noeud demand\u00e9 n'existe pas ou n'appartient pas \u00e0 cet arbre.";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Une erreur technique est survenue.";
    private static final String USER_NOT_AUTHENTICATED_MESSAGE = "User not authenticated";
    private static final String UNEXPECTED_ERROR_LOG = "Unexpected error in DecisionTreeAdminRest: ";
    private static final String TRACKING_START_ERROR_LOG = "Error tracking conversation start: ";
    private static final String TRACKING_STEP_ERROR_LOG = "Error tracking conversation step: ";

    @Context
    private HttpServletRequest _request;

    @Inject
    private ClientService _clientService;
    @Inject
    private ObservabilityService _observabilityService;

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
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "UNAUTHORIZED", USER_NOT_AUTHENTICATED_MESSAGE ) ) ).build( ) );
        }
        return user;
    }

    /**
     * Returns the start node of the specified decision tree and tracks the conversation start.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param strConversationId
     *            the optional conversation identifier for tracking
     * @return a JSON response containing the start node data, or an error response
     */
    @GET
    @Path( DecisionTreeRestConstants.DECISIONTREE_START_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getStartNode( @PathParam( "treeId" ) int nTreeId, @QueryParam( "conversationId" ) String strConversationId )
    {
        try
        {
            LuteceUser user = getAuthenticatedUser( );

            Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( nTreeId );
            if ( optTree.isEmpty( ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }
            DecisionTree tree = optTree.get( );

            if ( !_clientService.getAuthorizedClientIds( user ).contains( tree.getClientId( ) ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }

            Optional<DecisionNode> optNode = DecisionTreeService.getStartNode( nTreeId );
            if ( optNode.isEmpty( ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, NODE_NOT_FOUND_MESSAGE );
            }
            DecisionNode node = optNode.get( );

            trackConversationStart( strConversationId, nTreeId, tree.getClientId( ), node );

            return createSuccessResponse( DecisionNodeMapper.toResponseDTO( node, tree ) );
        }
        catch( WebApplicationException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", UNEXPECTED_ERROR_LOG, e.getMessage( ), e );
            return createErrorResponse( Response.Status.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE );
        }
    }

    /**
     * Returns a specific node of the given tree, records an observability execution, and tracks the conversation step.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param nNodeId
     *            the node identifier to fetch
     * @param strConversationId
     *            the optional conversation identifier for tracking
     * @param nTransitionId
     *            the optional transition identifier that led to this node
     * @return a JSON response containing the node data, or an error response
     */
    @GET
    @Path( DecisionTreeRestConstants.DECISIONTREE_NODE_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getNode( @PathParam( "treeId" ) int nTreeId, @PathParam( "nodeId" ) int nNodeId, @QueryParam( "conversationId" ) String strConversationId,
            @QueryParam( "transitionId" ) Integer nTransitionId )
    {
        try
        {
            LuteceUser user = getAuthenticatedUser( );

            Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( nTreeId );
            if ( optTree.isEmpty( ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }
            DecisionTree tree = optTree.get( );

            if ( !_clientService.getAuthorizedClientIds( user ).contains( tree.getClientId( ) ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }

            Optional<DecisionNode> optNode = DecisionTreeService.getNodeWithDetails( nNodeId );
            if ( optNode.isEmpty( ) || optNode.get( ).getTreeId( ) != nTreeId )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, NODE_NOT_FOUND_MESSAGE );
            }
            DecisionNode node = optNode.get( );

            String executionId = _observabilityService.startResourceExecution( DecisionTree.RESOURCE_TYPE, String.valueOf( nTreeId ), tree.getClientId( ),
                    null );

            trackConversationStep( strConversationId, node, nTransitionId );

            _observabilityService.completeResourceExecutionSuccess( executionId, null );

            return createSuccessResponse( DecisionNodeMapper.toResponseDTO( node, tree ) );
        }
        catch( WebApplicationException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", UNEXPECTED_ERROR_LOG, e.getMessage( ), e );
            return createErrorResponse( Response.Status.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE );
        }
    }

    /**
     * Uploads an inline markdown image for a decision tree. The content is provided as a base64 string (optionally in data-URI form). Returns the generated
     * file key and the public URL to embed in markdown.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param payload
     *            the JSON payload carrying {@code content} (base64) and {@code mimeType}
     * @return a JSON response with {@code fileKey} and {@code url} on success
     */
    @POST
    @Path( DECISIONTREE_IMAGE_UPLOAD_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response uploadTreeImage( @PathParam( "treeId" ) int nTreeId, Map<String, String> payload )
    {
        try
        {
            LuteceUser user = getAuthenticatedUser( );

            Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( nTreeId );
            if ( optTree.isEmpty( ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }
            DecisionTree tree = optTree.get( );

            if ( !_clientService.getAuthorizedClientIds( user ).contains( tree.getClientId( ) ) )
            {
                return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
            }

            byte [ ] bytes = decodeBase64Content( payload == null ? null : payload.get( IMAGE_CONTENT_KEY ) );
            String strMimeType = payload == null ? null : payload.get( IMAGE_MIME_TYPE_KEY );
            if ( bytes == null || bytes.length == 0 || bytes.length > IMAGE_MAX_BYTES || strMimeType == null || strMimeType.isEmpty( ) )
            {
                return createErrorResponse( Response.Status.BAD_REQUEST, IMAGE_INVALID_PAYLOAD_MESSAGE );
            }

            String strContentHash = DecisionTreeImageService.upload( nTreeId, bytes, strMimeType );
            if ( strContentHash == null )
            {
                return createErrorResponse( Response.Status.INTERNAL_SERVER_ERROR, IMAGE_UPLOAD_FAILED_MESSAGE );
            }

            return createSuccessResponse( new ImageUploadResultDTO( strContentHash, DecisionTreeImageService.buildMarker( strContentHash ) ) );
        }
        catch( WebApplicationException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", UNEXPECTED_ERROR_LOG, e.getMessage( ), e );
            return createErrorResponse( Response.Status.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE );
        }
    }

    /**
     * Decodes a base64 image payload. Accepts raw base64 or data-URI form {@code data:MIME;base64,XXXX}.
     *
     * @param strContent
     *            the payload string
     * @return the decoded bytes, or null if invalid
     */
    private byte [ ] decodeBase64Content( String strContent )
    {
        if ( strContent == null || strContent.isEmpty( ) )
        {
            return null;
        }
        String strBase64 = strContent;
        int nComma = strContent.indexOf( ',' );
        if ( strContent.startsWith( "data:" ) && nComma > 0 )
        {
            strBase64 = strContent.substring( nComma + 1 );
        }
        try
        {
            return Base64.getDecoder( ).decode( strBase64 );
        }
        catch( IllegalArgumentException e )
        {
            return null;
        }
    }

    /**
     * Records the start of a tracked conversation, creating the conversation and its first step. Does nothing when no conversation identifier is provided.
     *
     * @param strConversationId
     *            the conversation identifier, or null/empty to skip tracking
     * @param nTreeId
     *            the decision tree identifier
     * @param nClientId
     *            the client identifier owning the tree
     * @param startNode
     *            the start node reached
     */
    private void trackConversationStart( String strConversationId, int nTreeId, int nClientId, DecisionNode startNode )
    {
        if ( strConversationId == null || strConversationId.isEmpty( ) )
        {
            return;
        }
        try
        {
            DecisionTreeConversation conversation = new DecisionTreeConversation( );
            conversation.setConversationId( strConversationId );
            conversation.setTreeId( nTreeId );
            conversation.setClientId( nClientId );
            DecisionTreeConversationHome.createConversation( conversation );

            DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
            step.setConversationId( strConversationId );
            step.setNodeId( startNode.getId( ) );
            step.setNodeTitle( startNode.getNodeTitle( ) );
            DecisionTreeConversationHome.addStep( step );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", TRACKING_START_ERROR_LOG, e.getMessage( ), e );
        }
    }

    /**
     * Records a step in a tracked conversation, including the optionally chosen transition. Does nothing when no conversation identifier is provided.
     *
     * @param strConversationId
     *            the conversation identifier, or null/empty to skip tracking
     * @param node
     *            the node reached at this step
     * @param nTransitionId
     *            the identifier of the chosen transition, or null if none
     */
    private void trackConversationStep( String strConversationId, DecisionNode node, Integer nTransitionId )
    {
        if ( strConversationId == null || strConversationId.isEmpty( ) )
        {
            return;
        }
        try
        {
            DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
            step.setConversationId( strConversationId );
            step.setNodeId( node.getId( ) );
            step.setNodeTitle( node.getNodeTitle( ) );
            if ( nTransitionId != null )
            {
                step.setChosenTransitionId( nTransitionId );
            }
            DecisionTreeConversationHome.addStep( step );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", TRACKING_STEP_ERROR_LOG, e.getMessage( ), e );
        }
    }

    /**
     * Builds an HTTP 200 JSON response wrapping the given data.
     *
     * @param data
     *            the payload to include in the response
     * @return the success response
     */
    private Response createSuccessResponse( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Builds an error JSON response with the given HTTP status and message.
     *
     * @param status
     *            the HTTP status to return
     * @param strMessage
     *            the error message
     * @return the error response
     */
    private Response createErrorResponse( Response.Status status, String strMessage )
    {
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), strMessage ) ) ).build( );
    }
}
