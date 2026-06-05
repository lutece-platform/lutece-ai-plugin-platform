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

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeDTO;
import fr.paris.lutece.plugins.platform.business.decisiontree.*;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeImage;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeImageService;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeService;
import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;

/**
 * REST API for navigating decision trees
 */
@ApplicationScoped
@Path( "platform/agent/api" )
public class DecisionTreeAPIRest
{
    private static final String DECISIONTREES_PATH = "decisiontrees";
    private static final String DECISIONTREE_IMAGE_PATH = "decisiontrees/images/{contentHash}";
    private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000, immutable";
    private static final String IMAGE_NOT_FOUND_MESSAGE = "Image not found.";

    private static final String RESOURCE_TYPE_DECISION_TREE = DecisionTree.RESOURCE_TYPE;
    private static final String RESOURCE_TYPE_DECISION_TREE_NODE = DecisionTree.RESOURCE_TYPE + "_node";

    private static final String OBS_KEY_CONVERSATION_ID = "conversationId";
    private static final String OBS_KEY_TREE_ID = "treeId";

    private static final String TREE_NOT_FOUND_MESSAGE = "The requested decision tree does not exist or is no longer available.";
    private static final String NODE_NOT_FOUND_MESSAGE = "The requested node does not exist or does not belong to this tree.";

    @Context
    private HttpServletRequest _request;

    @Inject
    private AuthenticationService _authenticationService;
    @Inject
    private ObservabilityService _observabilityService;

    /**
     * List published decision trees for the authenticated client
     *
     * @return JSON response with the list of published trees
     */
    @GET
    @Path( DECISIONTREES_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getDecisionTrees( )
    {
        Client client = _authenticationService.authenticateClient( _request, null, null );

        List<DecisionTree> listTrees = DecisionTreeService.getPublishedTreesByClientId( client.getId( ) );
        List<DecisionTreeDTO> listTreeDTOs = listTrees.stream( ).map( DecisionNodeMapper::toTreeDTO ).toList( );
        return createSuccessResponse( listTreeDTOs );
    }

    /**
     * Get the start node of a decision tree
     *
     * @param nTreeId
     *            The tree identifier
     * @return JSON response with the start node details and tree metadata
     */
    @GET
    @Path( DecisionTreeRestConstants.DECISIONTREE_START_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getStartNode( @PathParam( "treeId" ) int nTreeId, @QueryParam( "conversationId" ) String strConversationId )
    {
        Client client = _authenticationService.authenticateClient( _request, null, null );

        DecisionTree tree = getPublishedTreeForClient( nTreeId, client );
        if ( tree == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
        }

        Optional<DecisionNode> optNode = DecisionTreeService.getStartNode( nTreeId );
        if ( optNode.isEmpty( ) )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, NODE_NOT_FOUND_MESSAGE );
        }
        DecisionNode node = optNode.get( );

        trackDecisionTree( RESOURCE_TYPE_DECISION_TREE, String.valueOf( nTreeId ), client.getId( ), strConversationId, null );

        return createSuccessResponse( DecisionNodeMapper.toResponseDTO( node, tree ) );
    }

    /**
     * Get a specific node of a decision tree
     *
     * @param nTreeId
     *            The tree identifier
     * @param nNodeId
     *            The node identifier
     * @return JSON response with the node details and tree metadata
     */
    @GET
    @Path( DecisionTreeRestConstants.DECISIONTREE_NODE_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getNode( @PathParam( "treeId" ) int nTreeId, @PathParam( "nodeId" ) int nNodeId, @QueryParam( "conversationId" ) String strConversationId )
    {
        Client client = _authenticationService.authenticateClient( _request, null, null );

        DecisionTree tree = getPublishedTreeForClient( nTreeId, client );
        if ( tree == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, TREE_NOT_FOUND_MESSAGE );
        }

        Optional<DecisionNode> optNode = DecisionTreeService.getNodeWithDetails( nNodeId );
        if ( optNode.isEmpty( ) || optNode.get( ).getTreeId( ) != nTreeId )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, NODE_NOT_FOUND_MESSAGE );
        }
        DecisionNode node = optNode.get( );

        trackDecisionTree( RESOURCE_TYPE_DECISION_TREE_NODE, String.valueOf( nNodeId ), client.getId( ), strConversationId, String.valueOf( nTreeId ) );

        return createSuccessResponse( DecisionNodeMapper.toResponseDTO( node, tree ) );
    }

    /**
     * Streams an image referenced from a decision tree markdown. Requires API-key auth. The content hash is looked up against every tree that references the
     * image — only a tree that belongs to the authenticated client AND is published will yield the bytes.
     *
     * @param strContentHash
     *            the sha256 content hash
     * @return the image bytes with MIME type and cache headers, 404 otherwise
     */
    @GET
    @Path( DECISIONTREE_IMAGE_PATH )
    public Response getImage( @PathParam( "contentHash" ) String strContentHash )
    {
        Client client = _authenticationService.authenticateClient( _request, null, null );

        DecisionTreeImage mapping = findPublishedMapping( strContentHash, client );
        if ( mapping == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, IMAGE_NOT_FOUND_MESSAGE );
        }

        File file = DecisionTreeImageService.getFile( mapping );
        PhysicalFile physical = file == null ? null : file.getPhysicalFile( );
        if ( physical == null || physical.getValue( ) == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, IMAGE_NOT_FOUND_MESSAGE );
        }

        return Response.ok( physical.getValue( ) ).type( mapping.getMimeType( ) ).header( HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE ).build( );
    }

    /**
     * Scans every mapping for the given hash and returns the first one whose owning tree belongs to the client and is published.
     *
     * @param strContentHash
     *            the content hash
     * @param client
     *            the authenticated client
     * @return the matching mapping, or null if none qualifies
     */
    private DecisionTreeImage findPublishedMapping( String strContentHash, Client client )
    {
        for ( DecisionTreeImage image : DecisionTreeImageService.findAnyMapping( strContentHash ) )
        {
            if ( getPublishedTreeForClient( image.getTreeId( ), client ) != null )
            {
                return image;
            }
        }
        return null;
    }

    /**
     * Load a tree and verify it belongs to the client and is PUBLISHED
     *
     * @param nTreeId
     *            The tree identifier
     * @param client
     *            The authenticated client
     * @return The tree if valid, null otherwise
     */
    private DecisionTree getPublishedTreeForClient( int nTreeId, Client client )
    {
        Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( nTreeId );
        if ( optTree.isEmpty( ) )
        {
            return null;
        }

        DecisionTree tree = optTree.get( );
        if ( tree.getClientId( ) != client.getId( ) )
        {
            return null;
        }

        boolean bIsPublished = SubscriptionHome.findByClientAndResource( client.getId( ), DecisionTree.RESOURCE_TYPE, String.valueOf( nTreeId ) )
                .map( s -> SubscriptionStatus.ACTIVE == s.getStatus( ) ).orElse( false );
        if ( !bIsPublished )
        {
            return null;
        }

        return tree;
    }

    /**
     * Track decision tree usage via observability service
     *
     * @param strResourceType
     *            The resource type for observability
     * @param strResourceId
     *            The resource identifier
     * @param nClientId
     *            The client identifier
     * @param strConversationId
     *            The conversation identifier
     * @param strTreeId
     *            The tree identifier, or null for tree-level tracking
     */
    private void trackDecisionTree( String strResourceType, String strResourceId, int nClientId, String strConversationId, String strTreeId )
    {
        ObservabilityData inputData = strTreeId != null
                ? ObservabilityData.of( "INPUT", OBS_KEY_CONVERSATION_ID, strConversationId, OBS_KEY_TREE_ID, strTreeId )
                : ObservabilityData.of( "INPUT", OBS_KEY_CONVERSATION_ID, strConversationId );
        String strExecId = _observabilityService.startResourceExecution( strResourceType, strResourceId, nClientId, inputData );
        _observabilityService.completeResourceExecutionSuccess( strExecId, null );
    }

    /**
     * Creates a success JSON response
     *
     * @param data
     *            The response data
     * @return The success response
     */
    private Response createSuccessResponse( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Creates an error JSON response
     *
     * @param status
     *            The HTTP status
     * @param strMessage
     *            The error message
     * @return The error response
     */
    private Response createErrorResponse( Response.Status status, String strMessage )
    {
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), strMessage ) ) ).build( );
    }
}
