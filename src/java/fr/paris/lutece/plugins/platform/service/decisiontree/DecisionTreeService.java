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
package fr.paris.lutece.plugins.platform.service.decisiontree;

import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.util.sql.TransactionManager;
import fr.paris.lutece.plugins.platform.business.decisiontree.*;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationDetail;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationsView;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeExport;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeOverview;
import fr.paris.lutece.plugins.platform.service.exception.DecisionTreeImportException;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.sql.Timestamp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;

import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static service for DecisionTree CRUD and navigation logic
 */
public final class DecisionTreeService
{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Private constructor
     */
    private DecisionTreeService( )
    {
    }

    /**
     * Creates a new decision tree.
     *
     * @param tree
     *            the tree to create
     * @return the created tree with its generated ID
     */
    public static DecisionTree createTree( DecisionTree tree )
    {
        return DecisionTreeHome.create( tree );
    }

    /**
     * Updates an existing decision tree. Cleans up images that were referenced in the previous welcome/end messages but are no longer present in the new
     * values.
     *
     * @param tree
     *            the tree to update
     * @return the updated tree
     */
    public static DecisionTree updateTree( DecisionTree tree )
    {
        DecisionTree before = DecisionTreeHome.findByPrimaryKey( tree.getId( ) ).orElse( null );
        DecisionTree updated = DecisionTreeHome.update( tree );
        if ( before != null )
        {
            DecisionTreeImageService.cleanupDiff( before.getWelcomeMessage( ), tree.getWelcomeMessage( ), tree.getId( ) );
            DecisionTreeImageService.cleanupDiff( before.getEndMessage( ), tree.getEndMessage( ), tree.getId( ) );
        }
        return updated;
    }

    /**
     * Removes a decision tree and all its nodes and transitions. Physical image files are deleted first so the tree-level FK cascade is left with only the
     * mapping rows to wipe.
     *
     * @param nTreeId
     *            the tree identifier
     */
    public static void removeTree( int nTreeId )
    {
        DecisionTreeImageService.deleteByTreeId( nTreeId );
        DecisionTreeHome.remove( nTreeId );
    }

    /**
     * Finds a decision tree by its primary key.
     *
     * @param nTreeId
     *            the tree identifier
     * @return an Optional containing the tree, or empty if not found
     */
    public static Optional<DecisionTree> findTree( int nTreeId )
    {
        return DecisionTreeHome.findByPrimaryKey( nTreeId );
    }

    /**
     * Finds the decision tree owning a given node.
     *
     * @param nNodeId
     *            the node identifier
     * @return an Optional containing the owning tree, or empty if the node or its tree does not exist
     */
    public static Optional<DecisionTree> findTreeByNodeId( int nNodeId )
    {
        return DecisionNodeHome.findByPrimaryKey( nNodeId ).flatMap( node -> findTree( node.getTreeId( ) ) );
    }

    /**
     * Finds the decision tree owning a given transition (through its source node).
     *
     * @param nTransitionId
     *            the transition identifier
     * @return an Optional containing the owning tree, or empty if the transition, its source node or its tree does not exist
     */
    public static Optional<DecisionTree> findTreeByTransitionId( int nTransitionId )
    {
        return findTransitionById( nTransitionId ).flatMap( transition -> findTreeByNodeId( transition.getSourceNodeId( ) ) );
    }

    /**
     * Returns all decision trees for a given client.
     *
     * @param nClientId
     *            the client identifier
     * @return list of decision trees
     */
    public static List<DecisionTree> getTreesByClientId( int nClientId )
    {
        return DecisionTreeHome.getDecisionTreesListByClientId( nClientId );
    }

    /**
     * Returns only published decision trees for a given client.
     *
     * @param nClientId
     *            the client identifier
     * @return list of published decision trees
     */
    public static List<DecisionTree> getPublishedTreesByClientId( int nClientId )
    {
        Set<String> publishedIds = SubscriptionHome.getSubscriptionsByClientId( nClientId ).stream( )
                .filter( s -> DecisionTree.RESOURCE_TYPE.equals( s.getResourceType( ) ) && SubscriptionStatus.ACTIVE == s.getStatus( ) )
                .map( Subscription::getResourceId ).collect( Collectors.toSet( ) );

        return DecisionTreeHome.getDecisionTreesListByClientId( nClientId ).stream( ).filter( t -> publishedIds.contains( String.valueOf( t.getId( ) ) ) )
                .toList( );
    }

    /**
     * Creates a new decision node.
     *
     * @param node
     *            the node to create
     * @return the created node with its generated ID
     */
    private static DecisionNode createNode( DecisionNode node )
    {
        return DecisionNodeHome.create( node );
    }

    /**
     * Updates an existing decision node. Cleans up images that were referenced in the previous content but are no longer present in the new content.
     *
     * @param node
     *            the node to update
     * @return the updated node
     */
    public static DecisionNode updateNode( DecisionNode node )
    {
        Optional<DecisionNode> before = DecisionNodeHome.findByPrimaryKey( node.getId( ) );
        DecisionNode updated = DecisionNodeHome.update( node );
        before.ifPresent( b -> DecisionTreeImageService.cleanupDiff( b.getContent( ), node.getContent( ), node.getTreeId( ) ) );
        return updated;
    }

    /**
     * Removes a node along with every transition referencing it (incoming and outgoing, wiped by the database FK cascade). Image files referenced from the node
     * content are purged first.
     *
     * @param nNodeId
     *            the node identifier
     */
    public static void removeNode( int nNodeId )
    {
        Optional<DecisionNode> before = DecisionNodeHome.findByPrimaryKey( nNodeId );
        before.ifPresent( n -> DecisionTreeImageService.deleteFromMarkdown( n.getContent( ), n.getTreeId( ) ) );
        DecisionNodeHome.remove( nNodeId );
    }

    /**
     * Returns all nodes for a given tree.
     *
     * @param nTreeId
     *            the tree identifier
     * @return list of nodes
     */
    public static List<DecisionNode> getNodesByTreeId( int nTreeId )
    {
        return DecisionNodeHome.getNodesByTreeId( nTreeId );
    }

    /**
     * Returns all nodes for a given tree with their transitions populated.
     *
     * @param nTreeId
     *            the tree identifier
     * @return list of nodes with transitions
     */
    public static List<DecisionNode> getNodesWithTransitions( int nTreeId )
    {
        List<DecisionNode> nodes = DecisionNodeHome.getNodesByTreeId( nTreeId );
        nodes.forEach( node -> node.setTransitions( DecisionTransitionHome.getTransitionsBySourceNodeId( node.getId( ) ) ) );
        return nodes;
    }

    /**
     * Returns the next sort order value for a new transition on a given source node.
     *
     * @param nSourceNodeId
     *            the source node identifier
     * @return the next sort order value
     */
    public static int getNextTransitionSortOrder( int nSourceNodeId )
    {
        List<DecisionTransition> transitions = DecisionTransitionHome.getTransitionsBySourceNodeId( nSourceNodeId );
        return transitions.stream( ).mapToInt( DecisionTransition::getSortOrder ).max( ).orElse( 0 ) + 1;
    }

    /**
     * Moves a transition up in the sort order relative to its siblings.
     *
     * @param nTransitionId
     *            the transition identifier
     */
    public static void moveTransitionUp( int nTransitionId )
    {
        DecisionTransition transition = DecisionTransitionHome.findByPrimaryKey( nTransitionId ).orElse( null );
        if ( transition == null )
        {
            return;
        }
        List<DecisionTransition> transitions = DecisionTransitionHome.getTransitionsBySourceNodeId( transition.getSourceNodeId( ) );
        swapWithPrevious( transitions, transition.getId( ), DecisionTransition::getId, DecisionTransition::getSortOrder, DecisionTransition::setSortOrder,
                DecisionTransitionHome::update );
    }

    /**
     * Moves a transition down in the sort order relative to its siblings.
     *
     * @param nTransitionId
     *            the transition identifier
     */
    public static void moveTransitionDown( int nTransitionId )
    {
        DecisionTransition transition = DecisionTransitionHome.findByPrimaryKey( nTransitionId ).orElse( null );
        if ( transition == null )
        {
            return;
        }
        List<DecisionTransition> transitions = DecisionTransitionHome.getTransitionsBySourceNodeId( transition.getSourceNodeId( ) );
        swapWithNext( transitions, transition.getId( ), DecisionTransition::getId, DecisionTransition::getSortOrder, DecisionTransition::setSortOrder,
                DecisionTransitionHome::update );
    }

    /**
     * Swaps the sort order of the target item with the one preceding it in the list and persists both.
     *
     * @param <T>
     *            the item type
     * @param items
     *            the ordered list of items
     * @param nTargetId
     *            the identifier of the item to move up
     * @param getId
     *            function returning an item identifier
     * @param getOrder
     *            function returning an item sort order
     * @param setOrder
     *            consumer setting an item sort order
     * @param persist
     *            consumer persisting an item
     */
    private static <T> void swapWithPrevious( List<T> items, int nTargetId, java.util.function.ToIntFunction<T> getId,
            java.util.function.ToIntFunction<T> getOrder, java.util.function.ObjIntConsumer<T> setOrder, java.util.function.Consumer<T> persist )
    {
        for ( int i = 1; i < items.size( ); i++ )
        {
            if ( getId.applyAsInt( items.get( i ) ) == nTargetId )
            {
                T current = items.get( i );
                T previous = items.get( i - 1 );
                int tmp = getOrder.applyAsInt( current );
                setOrder.accept( current, getOrder.applyAsInt( previous ) );
                setOrder.accept( previous, tmp );
                persist.accept( current );
                persist.accept( previous );
                return;
            }
        }
    }

    /**
     * Swaps the sort order of the target item with the one following it in the list and persists both.
     *
     * @param <T>
     *            the item type
     * @param items
     *            the ordered list of items
     * @param nTargetId
     *            the identifier of the item to move down
     * @param getId
     *            function returning an item identifier
     * @param getOrder
     *            function returning an item sort order
     * @param setOrder
     *            consumer setting an item sort order
     * @param persist
     *            consumer persisting an item
     */
    private static <T> void swapWithNext( List<T> items, int nTargetId, java.util.function.ToIntFunction<T> getId, java.util.function.ToIntFunction<T> getOrder,
            java.util.function.ObjIntConsumer<T> setOrder, java.util.function.Consumer<T> persist )
    {
        for ( int i = 0; i < items.size( ) - 1; i++ )
        {
            if ( getId.applyAsInt( items.get( i ) ) == nTargetId )
            {
                T current = items.get( i );
                T next = items.get( i + 1 );
                int tmp = getOrder.applyAsInt( current );
                setOrder.accept( current, getOrder.applyAsInt( next ) );
                setOrder.accept( next, tmp );
                persist.accept( current );
                persist.accept( next );
                return;
            }
        }
    }

    /**
     * Creates a new transition between two nodes.
     *
     * @param transition
     *            the transition to create
     * @return the created transition with its generated ID
     */
    public static DecisionTransition createTransition( DecisionTransition transition )
    {
        return DecisionTransitionHome.create( transition );
    }

    /**
     * Finds a transition by its primary key.
     *
     * @param nTransitionId
     *            the transition identifier
     * @return an Optional containing the transition, or empty if not found
     */
    public static Optional<DecisionTransition> findTransitionById( int nTransitionId )
    {
        return DecisionTransitionHome.findByPrimaryKey( nTransitionId );
    }

    /**
     * Updates an existing transition.
     *
     * @param transition
     *            the transition to update
     * @return the updated transition
     */
    public static DecisionTransition updateTransition( DecisionTransition transition )
    {
        return DecisionTransitionHome.update( transition );
    }

    /**
     * Removes a transition by its identifier.
     *
     * @param nTransitionId
     *            the transition identifier
     */
    public static void removeTransition( int nTransitionId )
    {
        DecisionTransitionHome.remove( nTransitionId );
    }

    /**
     * Returns a node with its transitions populated.
     *
     * @param nNodeId
     *            the node identifier
     * @return an Optional containing the node with details, or empty if not found
     */
    public static Optional<DecisionNode> getNodeWithDetails( int nNodeId )
    {
        Optional<DecisionNode> optNode = DecisionNodeHome.findByPrimaryKey( nNodeId );
        optNode.ifPresent( DecisionTreeService::populateNodeDetails );
        return optNode;
    }

    /**
     * Returns the root (start) node of a tree with its transitions populated.
     *
     * @param nTreeId
     *            the tree identifier
     * @return an Optional containing the start node, or empty if the tree has no nodes
     */
    public static Optional<DecisionNode> getStartNode( int nTreeId )
    {
        List<DecisionNode> nodes = getNodesWithTransitions( nTreeId );
        List<DecisionNode> roots = findRootNodes( nodes );
        if ( roots.size( ) == 1 )
        {
            return getNodeWithDetails( roots.get( 0 ).getId( ) );
        }
        if ( !nodes.isEmpty( ) )
        {
            return getNodeWithDetails( nodes.get( 0 ).getId( ) );
        }
        return Optional.empty( );
    }

    /**
     * Returns a full tree with all nodes and their transitions populated.
     *
     * @param nTreeId
     *            the tree identifier
     * @return an Optional containing the full tree, or empty if not found
     */
    public static Optional<DecisionTree> getFullTree( int nTreeId )
    {
        Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( nTreeId );
        optTree.ifPresent( tree -> {
            List<DecisionNode> listNodes = DecisionNodeHome.getNodesByTreeId( nTreeId );
            listNodes.forEach( DecisionTreeService::populateNodeDetails );
            tree.setListNodes( listNodes );
        } );
        return optTree;
    }

    /**
     * Builds a map of nodes keyed by their string ID for fast lookup.
     *
     * @param nodes
     *            the list of nodes
     * @return map of node ID (as string) to node
     */
    static Map<String, DecisionNode> buildNodeMap( List<DecisionNode> nodes )
    {
        Map<String, DecisionNode> map = new HashMap<>( );
        for ( DecisionNode node : nodes )
        {
            map.put( String.valueOf( node.getId( ) ), node );
        }
        return map;
    }

    /**
     * Identifies root nodes (nodes that are not the target of any transition).
     *
     * @param nodes
     *            the list of nodes with transitions populated
     * @return list of root nodes
     */
    static List<DecisionNode> findRootNodes( List<DecisionNode> nodes )
    {
        Set<Integer> targeted = new HashSet<>( );
        for ( DecisionNode node : nodes )
        {
            if ( node.getTransitions( ) != null )
            {
                for ( DecisionTransition t : node.getTransitions( ) )
                {
                    targeted.add( t.getTargetNodeId( ) );
                }
            }
        }
        List<DecisionNode> roots = nodes.stream( ).filter( n -> !targeted.contains( n.getId( ) ) ).toList( );
        if ( roots.size( ) != 1 && !nodes.isEmpty( ) )
        {
            return Collections.singletonList( nodes.get( 0 ) );
        }
        return roots;
    }

    /**
     * Returns nodes that are not reachable from the root (orphan nodes).
     *
     * @param nodes
     *            the list of nodes with transitions populated
     * @return list of orphan nodes, or empty list if tree has no root
     */
    public static List<DecisionNode> getOrphanNodes( List<DecisionNode> nodes )
    {
        if ( nodes.isEmpty( ) )
        {
            return new ArrayList<>( );
        }

        List<DecisionNode> roots = findRootNodes( nodes );
        if ( roots.isEmpty( ) )
        {
            return new ArrayList<>( );
        }

        Set<Integer> connected = new HashSet<>( );
        Queue<Integer> queue = new LinkedList<>( );
        Map<Integer, DecisionNode> byId = new HashMap<>( );
        for ( DecisionNode node : nodes )
        {
            byId.put( node.getId( ), node );
        }

        queue.add( roots.get( 0 ).getId( ) );
        while ( !queue.isEmpty( ) )
        {
            int nId = queue.poll( );
            if ( !connected.add( nId ) )
            {
                continue;
            }
            DecisionNode node = byId.get( nId );
            if ( node != null && node.getTransitions( ) != null )
            {
                for ( DecisionTransition t : node.getTransitions( ) )
                {
                    if ( !connected.contains( t.getTargetNodeId( ) ) )
                    {
                        queue.add( t.getTargetNodeId( ) );
                    }
                }
            }
        }

        return nodes.stream( ).filter( n -> !connected.contains( n.getId( ) ) ).toList( );
    }

    /**
     * Populates a node with its outgoing transitions.
     *
     * @param node
     *            the node to populate
     */
    private static void populateNodeDetails( DecisionNode node )
    {
        node.setTransitions( DecisionTransitionHome.getTransitionsBySourceNodeId( node.getId( ) ) );
    }

    /**
     * Creates a decision node and, when a parent node is supplied, links it to that parent with a new transition whose sort order is appended after the
     * parent's existing transitions.
     *
     * @param node
     *            the node to create (its tree id must already be set)
     * @param nParentNodeId
     *            the parent node identifier, or null when the node has no incoming transition
     * @param strTransitionLabel
     *            the label of the linking transition (only used when a parent is supplied)
     * @return the created node with its generated id
     */
    public static DecisionNode createNodeWithParent( DecisionNode node, Integer nParentNodeId, String strTransitionLabel )
    {
        DecisionNode created = createNode( node );
        if ( nParentNodeId != null )
        {
            DecisionTransition transition = new DecisionTransition( );
            transition.setSourceNodeId( nParentNodeId );
            transition.setTargetNodeId( created.getId( ) );
            transition.setLabel( strTransitionLabel );
            transition.setSortOrder( getNextTransitionSortOrder( nParentNodeId ) );
            createTransition( transition );
        }
        return created;
    }

    /**
     * Removes every orphan node of a tree (nodes not reachable from the root).
     *
     * @param nTreeId
     *            the tree identifier
     * @return the number of removed orphan nodes
     */
    public static int removeOrphanNodes( int nTreeId )
    {
        List<DecisionNode> orphans = getOrphanNodes( getNodesWithTransitions( nTreeId ) );
        orphans.forEach( orphan -> removeNode( orphan.getId( ) ) );
        return orphans.size( );
    }

    /**
     * Builds the read-only overview of a tree: nodes with transitions, node map, root nodes and the conversation statistics (count, last activity, average
     * steps, average duration).
     *
     * @param nTreeId
     *            the tree identifier
     * @return the overview
     */
    public static DecisionTreeOverview getTreeOverview( int nTreeId )
    {
        return buildOverview( nTreeId, DecisionTreeConversationHome.findConversationsByTreeId( nTreeId ), null, null );
    }

    /**
     * Builds the shared tree structure and conversation statistics (nodes with transitions, node map, root nodes, count, last activity, average steps, average
     * duration) for a given set of conversations. Shared by the overview and the conversations analytics view so the aggregation is computed in a single place.
     *
     * @param nTreeId
     *            the tree identifier
     * @param conversations
     *            the conversations to derive count and last activity from (all conversations or a date-filtered subset)
     * @param tsFrom
     *            the start bound applied to the aggregates (inclusive), or null for no filter
     * @param tsTo
     *            the end bound applied to the aggregates (exclusive), or null for no filter
     * @return the overview
     */
    private static DecisionTreeOverview buildOverview( int nTreeId, List<DecisionTreeConversation> conversations, Timestamp tsFrom, Timestamp tsTo )
    {
        List<DecisionNode> nodes = getNodesWithTransitions( nTreeId );
        Timestamp lastActivity = conversations.stream( ).map( DecisionTreeConversation::getLastStepTime ).filter( java.util.Objects::nonNull )
                .max( Timestamp::compareTo ).orElse( null );
        return new DecisionTreeOverview( nodes, buildNodeMap( nodes ), findRootNodes( nodes ), conversations.size( ), lastActivity,
                DecisionTreeConversationHome.getAverageStepsPerConversation( nTreeId, tsFrom, tsTo ),
                DecisionTreeConversationHome.getAverageDurationSeconds( nTreeId, tsFrom, tsTo ) );
    }

    /**
     * Builds the conversations analytics view of a tree, optionally restricted to a date range. The date bounds are inclusive day boundaries derived from
     * {@code yyyy-MM-dd} strings; both must be present and non-empty for the filter to apply.
     *
     * @param nTreeId
     *            the tree identifier
     * @param strDateFrom
     *            the start date as {@code yyyy-MM-dd}, or null/empty for no filter
     * @param strDateTo
     *            the end date as {@code yyyy-MM-dd}, or null/empty for no filter
     * @return the conversations view
     */
    public static DecisionTreeConversationsView getConversationsView( int nTreeId, String strDateFrom, String strDateTo )
    {
        boolean bHasDateFilter = strDateFrom != null && !strDateFrom.isEmpty( ) && strDateTo != null && !strDateTo.isEmpty( );
        Timestamp tsFrom = bHasDateFilter ? Timestamp.valueOf( strDateFrom + " 00:00:00" ) : null;
        Timestamp tsTo = bHasDateFilter ? Timestamp.valueOf( strDateTo + " 23:59:59" ) : null;

        List<DecisionTreeConversation> conversations = bHasDateFilter
                ? DecisionTreeConversationHome.findConversationsByTreeIdAndDateRange( nTreeId, tsFrom, tsTo )
                : DecisionTreeConversationHome.findConversationsByTreeId( nTreeId );

        Map<String, Integer> nodeVisitCounts = new HashMap<>( );
        DecisionTreeConversationHome.getNodeVisitCounts( nTreeId, tsFrom, tsTo )
                .forEach( ( key, value ) -> nodeVisitCounts.put( String.valueOf( key ), value ) );

        return new DecisionTreeConversationsView( buildOverview( nTreeId, conversations, tsFrom, tsTo ), conversations,
                DecisionTreeConversationHome.getStepCountsByTreeId( nTreeId, tsFrom, tsTo ), nodeVisitCounts );
    }

    /**
     * Loads a conversation detail (conversation, ordered steps and the map of the visited nodes keyed by string id), resolving each distinct visited node once.
     *
     * @param strConversationId
     *            the conversation identifier
     * @return an Optional carrying the detail, or empty when the conversation does not exist
     */
    public static Optional<DecisionTreeConversationDetail> getConversationDetail( String strConversationId )
    {
        DecisionTreeConversation conversation = DecisionTreeConversationHome.findConversationById( strConversationId ).orElse( null );
        if ( conversation == null )
        {
            return Optional.empty( );
        }

        List<DecisionTreeConversationStep> steps = DecisionTreeConversationHome.findStepsByConversationId( strConversationId );
        Map<String, DecisionNode> nodeMap = new HashMap<>( );
        for ( DecisionTreeConversationStep step : steps )
        {
            String strNodeId = String.valueOf( step.getNodeId( ) );
            if ( !nodeMap.containsKey( strNodeId ) )
            {
                DecisionNodeHome.findByPrimaryKey( step.getNodeId( ) ).ifPresent( node -> nodeMap.put( strNodeId, node ) );
            }
        }
        return Optional.of( new DecisionTreeConversationDetail( conversation, steps, nodeMap ) );
    }

    /**
     * Exports a tree as a downloadable JSON payload together with a file name sanitized from the tree name.
     *
     * @param tree
     *            the tree to export
     * @return the export carrying the JSON and the file name
     */
    public static DecisionTreeExport exportTree( DecisionTree tree )
    {
        String strFileName = tree.getTreeName( ).replaceAll( "[^a-zA-Z0-9_-]", "_" ) + ".json";
        return new DecisionTreeExport( exportTreeToJson( tree.getId( ) ), strFileName );
    }

    /**
     * Exports a full decision tree (metadata, nodes, transitions) as a JSON string.
     *
     * @param nTreeId
     *            the tree identifier
     * @return the JSON representation, or empty string if tree not found
     */
    static String exportTreeToJson( int nTreeId )
    {
        DecisionTree tree = getFullTree( nTreeId ).orElse( null );
        if ( tree == null )
        {
            return "";
        }

        ObjectNode root = OBJECT_MAPPER.createObjectNode( );
        root.put( "treeName", tree.getTreeName( ) );
        root.put( "treeDescription", tree.getTreeDescription( ) );
        root.put( "welcomeMessage", tree.getWelcomeMessage( ) );
        root.put( "endMessage", tree.getEndMessage( ) );
        root.put( "logoBase64", tree.getLogoBase64( ) );

        ArrayNode nodesArray = root.putArray( "nodes" );
        if ( tree.getListNodes( ) != null )
        {
            for ( DecisionNode node : tree.getListNodes( ) )
            {
                ObjectNode nodeObj = OBJECT_MAPPER.createObjectNode( );
                nodeObj.put( "refId", node.getId( ) );
                nodeObj.put( "nodeTitle", node.getNodeTitle( ) );
                nodeObj.put( "content", node.getContent( ) );
                nodeObj.put( "showBackButton", node.getShowBackButton( ) );
                nodeObj.put( "backTargetRef", node.getBackTargetNodeId( ) != null ? node.getBackTargetNodeId( ) : 0 );

                ArrayNode transArray = nodeObj.putArray( "transitions" );
                if ( node.getTransitions( ) != null )
                {
                    for ( DecisionTransition t : node.getTransitions( ) )
                    {
                        ObjectNode tObj = OBJECT_MAPPER.createObjectNode( );
                        tObj.put( "label", t.getLabel( ) );
                        tObj.put( "targetRef", t.getTargetNodeId( ) );
                        tObj.put( "sortOrder", t.getSortOrder( ) );
                        transArray.add( tObj );
                    }
                }
                nodesArray.add( nodeObj );
            }
        }

        appendImagesToExport( tree, root );

        try
        {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter( ).writeValueAsString( root );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error exporting decision tree {}", nTreeId, e );
            return "";
        }
    }

    /**
     * Re-uploads every image blob carried by an export JSON under the {@code images} array, binding each to the freshly created tree. The content hash is
     * recomputed on the destination from the base64 content — it matches the hash already embedded in the imported markdown (since hashes are deterministic
     * from bytes), so no rewrite is needed. If an image already exists for the pair (hash, tree), the upload is idempotent.
     *
     * @param nTreeId
     *            the identifier of the tree just created on the destination instance
     * @param imagesNode
     *            the {@code images} array of the export JSON (may be missing or empty)
     */
    private static void reuploadImagesFromExport( int nTreeId, JsonNode imagesNode )
    {
        if ( imagesNode == null || !imagesNode.isArray( ) || imagesNode.isEmpty( ) )
        {
            return;
        }
        for ( JsonNode imageJson : imagesNode )
        {
            String strMimeType = imageJson.path( "mimeType" ).asText( null );
            String strContent = imageJson.path( "content" ).asText( null );
            if ( strMimeType == null || strContent == null )
            {
                continue;
            }
            try
            {
                byte [ ] bytes = Base64.getDecoder( ).decode( strContent );
                DecisionTreeImageService.upload( nTreeId, bytes, strMimeType );
            }
            catch( IllegalArgumentException e )
            {
                AppLogService.error( "Invalid base64 payload for an image during decision tree import", e );
            }
        }
    }

    /**
     * Collects every image content hash referenced anywhere in the tree (welcome, end, all node contents), loads each blob from the FileStoreService via
     * {@link DecisionTreeImageService} and appends a self-contained {@code images} array to the export JSON. Each entry carries only the MIME type and the
     * base64-encoded content — the hash is recomputable at import time, no correlation field is needed.
     *
     * @param tree
     *            the fully loaded tree
     * @param root
     *            the JSON root being built
     */
    private static void appendImagesToExport( DecisionTree tree, ObjectNode root )
    {
        Set<String> hashes = new HashSet<>( );
        hashes.addAll( DecisionTreeImageService.extractContentHashes( tree.getWelcomeMessage( ) ) );
        hashes.addAll( DecisionTreeImageService.extractContentHashes( tree.getEndMessage( ) ) );
        if ( tree.getListNodes( ) != null )
        {
            for ( DecisionNode node : tree.getListNodes( ) )
            {
                hashes.addAll( DecisionTreeImageService.extractContentHashes( node.getContent( ) ) );
            }
        }
        if ( hashes.isEmpty( ) )
        {
            return;
        }

        ArrayNode imagesArray = root.putArray( "images" );
        for ( String strHash : hashes )
        {
            DecisionTreeImage mapping = DecisionTreeImageService.findMapping( strHash, tree.getId( ) ).orElse( null );
            if ( mapping == null )
            {
                continue;
            }
            File file = DecisionTreeImageService.getFile( mapping );
            PhysicalFile physical = file == null ? null : file.getPhysicalFile( );
            if ( physical == null || physical.getValue( ) == null )
            {
                continue;
            }

            ObjectNode imageObj = OBJECT_MAPPER.createObjectNode( );
            imageObj.put( "mimeType", mapping.getMimeType( ) );
            imageObj.put( "content", Base64.getEncoder( ).encodeToString( physical.getValue( ) ) );
            imagesArray.add( imageObj );
        }
    }

    /**
     * Imports a decision tree from a JSON string, creating a new tree with all nodes and transitions in a single transaction: a failure mid-import leaves no
     * partial tree behind. Node IDs are remapped to newly generated IDs. Images already pushed to the file store before a failure are not rolled back (orphan
     * files, no DB inconsistency).
     *
     * @param strJson
     *            the JSON representation of the tree
     * @param nClientId
     *            the client identifier for the new tree
     * @return the created decision tree
     * @throws DecisionTreeImportException
     *             if the payload is missing, malformed or otherwise cannot be imported
     */
    public static DecisionTree importTreeFromJson( String strJson, int nClientId )
    {
        if ( strJson == null || strJson.trim( ).isEmpty( ) )
        {
            throw new DecisionTreeImportException( "Empty decision tree import payload" );
        }
        TransactionManager.beginTransaction( PluginService.getPlugin( "platform" ) );
        try
        {
            DecisionTree tree = doImportTreeFromJson( strJson, nClientId );
            TransactionManager.commitTransaction( PluginService.getPlugin( "platform" ) );
            return tree;
        }
        catch( DecisionTreeImportException e )
        {
            TransactionManager.rollBack( PluginService.getPlugin( "platform" ) );
            throw e;
        }
        catch( Exception e )
        {
            TransactionManager.rollBack( PluginService.getPlugin( "platform" ) );
            throw new DecisionTreeImportException( "Failed to import decision tree from JSON", e );
        }
    }

    /**
     * Performs the actual decision tree import, parsing the JSON and creating the tree, its nodes and transitions.
     *
     * @param strJson
     *            the JSON representation of the tree
     * @param nClientId
     *            the client identifier for the new tree
     * @return the created decision tree
     * @throws Exception
     *             if JSON parsing or creation fails
     */
    private static DecisionTree doImportTreeFromJson( String strJson, int nClientId ) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree( strJson );

        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( root.path( "treeName" ).asText( "" ) );
        tree.setTreeDescription( root.path( "treeDescription" ).asText( null ) );
        tree.setLogoBase64( root.path( "logoBase64" ).asText( null ) );
        tree.setClientId( nClientId );
        createTree( tree );

        reuploadImagesFromExport( tree.getId( ), root.path( "images" ) );

        tree.setWelcomeMessage( root.path( "welcomeMessage" ).asText( null ) );
        tree.setEndMessage( root.path( "endMessage" ).asText( null ) );
        DecisionTreeHome.update( tree );

        Map<Integer, Integer> idMapping = new HashMap<>( );
        JsonNode nodesArray = root.path( "nodes" );

        for ( JsonNode nodeJson : nodesArray )
        {
            int nOldId = nodeJson.path( "refId" ).asInt( );
            DecisionNode node = new DecisionNode( );
            node.setTreeId( tree.getId( ) );
            node.setNodeTitle( nodeJson.path( "nodeTitle" ).asText( "" ) );
            node.setContent( nodeJson.path( "content" ).asText( null ) );
            node.setShowBackButton( nodeJson.path( "showBackButton" ).asBoolean( false ) );
            createNode( node );
            idMapping.put( nOldId, node.getId( ) );
        }

        for ( JsonNode nodeJson : nodesArray )
        {
            int nOldId = nodeJson.path( "refId" ).asInt( );
            int nBackRef = nodeJson.path( "backTargetRef" ).asInt( 0 );
            if ( nBackRef > 0 && idMapping.containsKey( nBackRef ) )
            {
                DecisionNode node = DecisionNodeHome.findByPrimaryKey( idMapping.get( nOldId ) ).orElse( null );
                if ( node != null )
                {
                    node.setBackTargetNodeId( idMapping.get( nBackRef ) );
                    updateNode( node );
                }
            }

            for ( JsonNode tJson : nodeJson.path( "transitions" ) )
            {
                int nTargetRef = tJson.path( "targetRef" ).asInt( );
                if ( idMapping.containsKey( nTargetRef ) )
                {
                    DecisionTransition transition = new DecisionTransition( );
                    transition.setSourceNodeId( idMapping.get( nOldId ) );
                    transition.setTargetNodeId( idMapping.get( nTargetRef ) );
                    transition.setLabel( tJson.path( "label" ).asText( null ) );
                    transition.setSortOrder( tJson.path( "sortOrder" ).asInt( 0 ) );
                    createTransition( transition );
                }
            }
        }

        return tree;
    }
}
