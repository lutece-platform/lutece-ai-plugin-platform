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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNodeHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransition;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransitionHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationStep;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeHome;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationDetail;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationsView;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeOverview;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for the database-backed logic of {@link DecisionTreeService}: the next-sort-order computation and its empty-list edge, the up/down
 * reordering of sibling transitions including the boundary no-ops, the JSON export/import round trip with identifier remapping (transitions and back-targets
 * must be rewired to the freshly generated node ids), and the published-tree filter that keeps only trees with an active subscription. Expectations follow the
 * intended contract rather than the implementation.
 */
public class DecisionTreeServiceTest extends AbstractPlatformDbTest
{
    private Client _client;
    private DecisionTree _tree;

    /**
     * Creates a client and a base decision tree shared by every test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client DecisionTree Test" );
        _client.setCode( "CLIENT_DECISIONTREE_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _tree = createTree( "Base Tree" );
    }

    /**
     * The next sort order is one for a node with no transitions, and one above the current maximum otherwise.
     */
    @Test
    public void testGetNextTransitionSortOrder( )
    {
        DecisionNode source = createNode( "Source", "content" );
        DecisionNode target = createNode( "Target", "content" );

        assertEquals( 1, DecisionTreeService.getNextTransitionSortOrder( source.getId( ) ) );

        createTransition( source.getId( ), target.getId( ), "a", 1 );
        createTransition( source.getId( ), target.getId( ), "b", 2 );
        createTransition( source.getId( ), target.getId( ), "c", 5 );

        assertEquals( 6, DecisionTreeService.getNextTransitionSortOrder( source.getId( ) ) );
    }

    /**
     * Moving a transition down swaps its sort order with the following sibling.
     */
    @Test
    public void testMoveTransitionDown( )
    {
        DecisionNode source = createNode( "Source", "content" );
        DecisionNode target = createNode( "Target", "content" );
        DecisionTransition t1 = createTransition( source.getId( ), target.getId( ), "first", 1 );
        DecisionTransition t2 = createTransition( source.getId( ), target.getId( ), "second", 2 );

        DecisionTreeService.moveTransitionDown( t1.getId( ) );

        assertEquals( 2, reloadOrder( t1.getId( ) ) );
        assertEquals( 1, reloadOrder( t2.getId( ) ) );
    }

    /**
     * Moving a transition up swaps its sort order with the preceding sibling.
     */
    @Test
    public void testMoveTransitionUp( )
    {
        DecisionNode source = createNode( "Source", "content" );
        DecisionNode target = createNode( "Target", "content" );
        DecisionTransition t1 = createTransition( source.getId( ), target.getId( ), "first", 1 );
        DecisionTransition t2 = createTransition( source.getId( ), target.getId( ), "second", 2 );

        DecisionTreeService.moveTransitionUp( t2.getId( ) );

        assertEquals( 2, reloadOrder( t1.getId( ) ) );
        assertEquals( 1, reloadOrder( t2.getId( ) ) );
    }

    /**
     * Moving the first sibling up, the last sibling down, or an unknown transition leaves every sort order untouched.
     */
    @Test
    public void testMoveTransitionBoundaryNoOps( )
    {
        DecisionNode source = createNode( "Source", "content" );
        DecisionNode target = createNode( "Target", "content" );
        DecisionTransition t1 = createTransition( source.getId( ), target.getId( ), "first", 1 );
        DecisionTransition t2 = createTransition( source.getId( ), target.getId( ), "second", 2 );

        DecisionTreeService.moveTransitionUp( t1.getId( ) );
        DecisionTreeService.moveTransitionDown( t2.getId( ) );
        DecisionTreeService.moveTransitionUp( 999999 );
        DecisionTreeService.moveTransitionDown( 999999 );

        assertEquals( 1, reloadOrder( t1.getId( ) ) );
        assertEquals( 2, reloadOrder( t2.getId( ) ) );
    }

    /**
     * Exporting a tree to JSON and importing it back creates an independent tree whose nodes carry fresh identifiers, while the transition wiring and the
     * back-target reference are remapped to those new identifiers.
     */
    @Test
    public void testExportImportRoundTrip( )
    {
        DecisionNode start = createNode( "Start", "Welcome" );
        DecisionNode end = createNode( "End", "Goodbye" );
        end.setBackTargetNodeId( start.getId( ) );
        DecisionNodeHome.update( end );
        createTransition( start.getId( ), end.getId( ), "yes", 1 );

        String json = DecisionTreeService.exportTreeToJson( _tree.getId( ) );
        assertNotNull( json );
        assertFalse( json.isEmpty( ) );

        DecisionTree imported = assertDoesNotThrow( ( ) -> DecisionTreeService.importTreeFromJson( json, _client.getId( ) ) );
        assertNotEquals( _tree.getId( ), imported.getId( ) );

        Optional<DecisionTree> full = DecisionTreeService.getFullTree( imported.getId( ) );
        assertTrue( full.isPresent( ) );
        List<DecisionNode> nodes = full.get( ).getListNodes( );
        assertEquals( 2, nodes.size( ) );

        DecisionNode newStart = nodeByTitle( nodes, "Start" );
        DecisionNode newEnd = nodeByTitle( nodes, "End" );
        assertNotEquals( start.getId( ), newStart.getId( ) );
        assertEquals( "Welcome", newStart.getContent( ) );

        assertEquals( 1, newStart.getTransitions( ).size( ) );
        DecisionTransition newTransition = newStart.getTransitions( ).get( 0 );
        assertEquals( "yes", newTransition.getLabel( ) );
        assertEquals( newEnd.getId( ), newTransition.getTargetNodeId( ) );

        assertEquals( Integer.valueOf( newStart.getId( ) ), newEnd.getBackTargetNodeId( ) );
    }

    /**
     * The published filter keeps only trees with an active subscription, excluding unsubscribed and suspended ones.
     */
    @Test
    public void testGetPublishedTreesByClientId( )
    {
        DecisionTree suspended = createTree( "Suspended Tree" );
        createTree( "Unsubscribed Tree" );

        createSubscription( _tree.getId( ), SubscriptionStatus.ACTIVE );
        createSubscription( suspended.getId( ), SubscriptionStatus.SUSPENDED );

        List<DecisionTree> published = DecisionTreeService.getPublishedTreesByClientId( _client.getId( ) );

        assertEquals( 1, published.size( ) );
        assertEquals( _tree.getId( ), published.get( 0 ).getId( ) );
    }

    /**
     * Creating a node with a parent persists the node and adds a transition from the parent to it, whose sort order is appended after the parent's existing
     * transitions; creating one without a parent adds no transition.
     */
    @Test
    public void testCreateNodeWithParentLink( )
    {
        DecisionNode parent = createNode( "Parent", "content" );
        DecisionNode sibling = createNode( "Sibling", "content" );
        createTransition( parent.getId( ), sibling.getId( ), "existing", 1 );

        DecisionNode child = new DecisionNode( );
        child.setTreeId( _tree.getId( ) );
        child.setNodeTitle( "Child" );
        child.setContent( "content" );
        child.setShowBackButton( false );

        DecisionNode created = DecisionTreeService.createNodeWithParent( child, parent.getId( ), "go child" );

        assertTrue( created.getId( ) > 0, "created node must have a generated id" );
        assertTrue( DecisionNodeHome.findByPrimaryKey( created.getId( ) ).isPresent( ), "node must be persisted" );

        List<DecisionTransition> transitions = DecisionTransitionHome.getTransitionsBySourceNodeId( parent.getId( ) );
        DecisionTransition linking = transitions.stream( ).filter( t -> t.getTargetNodeId( ) == created.getId( ) ).findFirst( ).get( );
        assertEquals( "go child", linking.getLabel( ), "linking transition must carry the supplied label" );
        assertEquals( 2, linking.getSortOrder( ), "linking transition must be appended after existing siblings" );

        DecisionNode orphan = new DecisionNode( );
        orphan.setTreeId( _tree.getId( ) );
        orphan.setNodeTitle( "Orphan" );
        orphan.setContent( "content" );
        orphan.setShowBackButton( false );

        DecisionNode createdOrphan = DecisionTreeService.createNodeWithParent( orphan, null, "ignored" );
        assertTrue( DecisionTransitionHome.getTransitionsBySourceNodeId( createdOrphan.getId( ) ).isEmpty( ),
                "node created without a parent must have no incoming wiring" );
    }

    /**
     * Removing orphan nodes deletes only the nodes unreachable from the root, returns their count and leaves the connected nodes in place.
     */
    @Test
    public void testRemoveOrphanNodes( )
    {
        DecisionNode root = createNode( "Root", "content" );
        DecisionNode reachable = createNode( "Reachable", "content" );
        DecisionNode orphanA = createNode( "OrphanA", "content" );
        DecisionNode orphanB = createNode( "OrphanB", "content" );
        createTransition( root.getId( ), reachable.getId( ), "next", 1 );
        createTransition( orphanA.getId( ), orphanB.getId( ), "link", 1 );

        int nRemoved = DecisionTreeService.removeOrphanNodes( _tree.getId( ) );

        assertEquals( 2, nRemoved, "both unreachable nodes must be counted as removed" );
        assertTrue( DecisionNodeHome.findByPrimaryKey( orphanA.getId( ) ).isEmpty( ), "orphan A must be deleted" );
        assertTrue( DecisionNodeHome.findByPrimaryKey( orphanB.getId( ) ).isEmpty( ), "orphan B must be deleted" );
        assertTrue( DecisionNodeHome.findByPrimaryKey( root.getId( ) ).isPresent( ), "root must remain" );
        assertTrue( DecisionNodeHome.findByPrimaryKey( reachable.getId( ) ).isPresent( ), "reachable node must remain" );
    }

    /**
     * The overview exposes the tree structure (nodes, node map, root nodes), the conversation count, a non-null last activity once conversations exist and the
     * average step and average duration aggregates.
     */
    @Test
    public void testGetTreeOverview( )
    {
        DecisionNode root = createNode( "Root", "content" );
        DecisionNode child = createNode( "Child", "content" );
        createTransition( root.getId( ), child.getId( ), "next", 1 );

        seedConversation( root.getId( ), child.getId( ) );
        seedConversation( root.getId( ), child.getId( ) );

        DecisionTreeOverview overview = DecisionTreeService.getTreeOverview( _tree.getId( ) );

        assertEquals( 2, overview.getNodes( ).size( ), "overview must expose both nodes" );
        assertEquals( 2, overview.getNodeMap( ).size( ), "node map must be keyed for every node" );
        assertTrue( overview.getNodeMap( ).containsKey( String.valueOf( root.getId( ) ) ), "node map must be keyed by string id" );
        assertEquals( 1, overview.getRootNodes( ).size( ), "the single unreferenced node is the root" );
        assertEquals( root.getId( ), overview.getRootNodes( ).get( 0 ).getId( ), "root must be the node with no incoming transition" );
        assertEquals( 2, overview.getConversationCount( ), "both seeded conversations must be counted" );
        assertNotNull( overview.getLastActivity( ), "last activity must be set once conversations exist" );
        assertEquals( 2.0, overview.getAverageSteps( ), 0.001, "each conversation has two steps" );
        assertTrue( overview.getAverageDurationSeconds( ) >= 0, "average duration must be non-negative" );
    }

    /**
     * Without a date filter the conversations view returns every conversation; with a date filter it returns only the conversations within the range. In both
     * cases it exposes the step-count distribution and the per-node visit counts keyed by node string id, sharing the overview aggregates.
     */
    @Test
    public void testGetConversationsView( )
    {
        DecisionNode root = createNode( "Root", "content" );
        DecisionNode child = createNode( "Child", "content" );
        createTransition( root.getId( ), child.getId( ), "next", 1 );

        seedConversation( root.getId( ), child.getId( ) );
        seedConversation( root.getId( ), child.getId( ) );

        DecisionTreeConversationsView all = DecisionTreeService.getConversationsView( _tree.getId( ), null, null );
        assertEquals( 2, all.getConversations( ).size( ), "no filter must return every conversation" );
        assertEquals( 2, all.getStepCounts( ).size( ), "one step-count entry per conversation" );

        Map<String, Integer> visits = all.getNodeVisitCounts( );
        assertEquals( Integer.valueOf( 2 ), visits.get( String.valueOf( root.getId( ) ) ), "root visited once per conversation" );
        assertEquals( Integer.valueOf( 2 ), visits.get( String.valueOf( child.getId( ) ) ), "child visited once per conversation" );
        assertTrue( visits.keySet( ).stream( ).allMatch( k -> k.matches( "\\d+" ) ), "node visit counts must be keyed by node string id" );

        String strToday = java.time.LocalDate.now( ).toString( );
        DecisionTreeConversationsView filtered = DecisionTreeService.getConversationsView( _tree.getId( ), strToday, strToday );
        assertEquals( 2, filtered.getConversations( ).size( ), "today's conversations must fall within an inclusive same-day filter" );

        String strPast = java.time.LocalDate.now( ).minusDays( 10 ).toString( );
        DecisionTreeConversationsView empty = DecisionTreeService.getConversationsView( _tree.getId( ), strPast, strPast );
        assertEquals( 0, empty.getConversations( ).size( ), "a past-only range must exclude today's conversations" );
    }

    /**
     * The conversation detail carries the conversation, its ordered steps and a node map that resolves each distinct visited node exactly once; an unknown
     * conversation yields an empty optional.
     */
    @Test
    public void testGetConversationDetail( )
    {
        DecisionNode root = createNode( "Root", "content" );
        DecisionNode child = createNode( "Child", "content" );

        String strConversationId = UUID.randomUUID( ).toString( );
        DecisionTreeConversation conversation = new DecisionTreeConversation( );
        conversation.setConversationId( strConversationId );
        conversation.setTreeId( _tree.getId( ) );
        conversation.setClientId( _client.getId( ) );
        DecisionTreeConversationHome.createConversation( conversation );
        addStep( strConversationId, root.getId( ), "Root" );
        addStep( strConversationId, child.getId( ), "Child" );
        addStep( strConversationId, root.getId( ), "Root" );

        Optional<DecisionTreeConversationDetail> optDetail = DecisionTreeService.getConversationDetail( strConversationId );
        assertTrue( optDetail.isPresent( ), "detail must be present for an existing conversation" );

        DecisionTreeConversationDetail detail = optDetail.get( );
        assertEquals( strConversationId, detail.getConversation( ).getConversationId( ), "detail must carry the conversation" );
        assertEquals( 3, detail.getSteps( ).size( ), "every step must be returned in order" );
        assertEquals( 2, detail.getNodeMap( ).size( ), "the node map must deduplicate the revisited node" );
        assertTrue( detail.getNodeMap( ).containsKey( String.valueOf( root.getId( ) ) ), "node map must be keyed by node string id" );

        assertTrue( DecisionTreeService.getConversationDetail( "UNKNOWN_" + UUID.randomUUID( ) ).isEmpty( ),
                "an unknown conversation must yield an empty optional" );
    }

    /**
     * Seeds a conversation that visits the two given nodes in order, returning a fresh conversation identifier.
     *
     * @param nFirstNodeId
     *            the first visited node identifier
     * @param nSecondNodeId
     *            the second visited node identifier
     * @return the generated conversation identifier
     */
    private String seedConversation( int nFirstNodeId, int nSecondNodeId )
    {
        String strConversationId = UUID.randomUUID( ).toString( );
        DecisionTreeConversation conversation = new DecisionTreeConversation( );
        conversation.setConversationId( strConversationId );
        conversation.setTreeId( _tree.getId( ) );
        conversation.setClientId( _client.getId( ) );
        DecisionTreeConversationHome.createConversation( conversation );
        addStep( strConversationId, nFirstNodeId, "first" );
        addStep( strConversationId, nSecondNodeId, "second" );
        return strConversationId;
    }

    /**
     * Adds a step visiting the given node to a conversation.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param nNodeId
     *            the visited node identifier
     * @param strNodeTitle
     *            the node title recorded on the step
     */
    private void addStep( String strConversationId, int nNodeId, String strNodeTitle )
    {
        DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
        step.setConversationId( strConversationId );
        step.setNodeId( nNodeId );
        step.setNodeTitle( strNodeTitle );
        DecisionTreeConversationHome.addStep( step );
    }

    /**
     * Reloads the persisted sort order of a transition.
     *
     * @param nTransitionId
     *            the transition identifier
     * @return the stored sort order
     */
    private int reloadOrder( int nTransitionId )
    {
        return DecisionTransitionHome.findByPrimaryKey( nTransitionId ).get( ).getSortOrder( );
    }

    /**
     * Finds a node by its title within a list.
     *
     * @param nodes
     *            the node list
     * @param strTitle
     *            the title to match
     * @return the matching node
     */
    private DecisionNode nodeByTitle( List<DecisionNode> nodes, String strTitle )
    {
        return nodes.stream( ).filter( n -> strTitle.equals( n.getNodeTitle( ) ) ).findFirst( ).get( );
    }

    /**
     * Creates and persists a decision tree under the shared client.
     *
     * @param strName
     *            the tree name
     * @return the persisted tree
     */
    private DecisionTree createTree( String strName )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( strName );
        tree.setClientId( _client.getId( ) );
        return DecisionTreeHome.create( tree );
    }

    /**
     * Creates and persists a node in the base tree.
     *
     * @param strTitle
     *            the node title
     * @param strContent
     *            the node content
     * @return the persisted node
     */
    private DecisionNode createNode( String strTitle, String strContent )
    {
        DecisionNode node = new DecisionNode( );
        node.setTreeId( _tree.getId( ) );
        node.setNodeTitle( strTitle );
        node.setContent( strContent );
        node.setShowBackButton( false );
        return DecisionNodeHome.create( node );
    }

    /**
     * Creates and persists a transition between two nodes.
     *
     * @param nSourceId
     *            the source node identifier
     * @param nTargetId
     *            the target node identifier
     * @param strLabel
     *            the transition label
     * @param nSortOrder
     *            the sort order
     * @return the persisted transition
     */
    private DecisionTransition createTransition( int nSourceId, int nTargetId, String strLabel, int nSortOrder )
    {
        DecisionTransition transition = new DecisionTransition( );
        transition.setSourceNodeId( nSourceId );
        transition.setTargetNodeId( nTargetId );
        transition.setLabel( strLabel );
        transition.setSortOrder( nSortOrder );
        return DecisionTransitionHome.create( transition );
    }

    /**
     * Creates and persists a subscription of the shared client to a decision tree resource.
     *
     * @param nTreeId
     *            the tree identifier used as resource id
     * @param status
     *            the subscription status
     * @return the persisted subscription
     */
    private Subscription createSubscription( int nTreeId, SubscriptionStatus status )
    {
        Subscription subscription = new Subscription( );
        subscription.setClientId( _client.getId( ) );
        subscription.setResourceType( DecisionTree.RESOURCE_TYPE );
        subscription.setResourceId( String.valueOf( nTreeId ) );
        subscription.setStatus( status );
        return SubscriptionHome.create( subscription );
    }
}
