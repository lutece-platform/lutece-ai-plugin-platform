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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransition;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Adversarial tests for the pure in-memory graph helpers of {@link DecisionTreeService}: {@code buildNodeMap}, {@code findRootNodes} and
 * {@code getOrphanNodes}. Expectations are derived from the intended contract, including the deliberate fallbacks: when the graph has no single unambiguous
 * root (zero roots in a cycle, or several disconnected roots) the service falls back to treating the first node as root, and orphan detection then runs from
 * that node. These helpers take a node list directly, so they are exercised without any database.
 */
public class DecisionTreeGraphTest extends AbstractPlatformDbTest
{
    /**
     * buildNodeMap keys every node by its identifier rendered as a string.
     */
    @Test
    public void testBuildNodeMap( )
    {
        DecisionNode n5 = node( 5 );
        DecisionNode n7 = node( 7 );

        Map<String, DecisionNode> map = DecisionTreeService.buildNodeMap( Arrays.asList( n5, n7 ) );

        assertEquals( 2, map.size( ) );
        assertSame( n5, map.get( "5" ) );
        assertSame( n7, map.get( "7" ) );
        assertNull( map.get( "0" ) );
    }

    /**
     * A linear chain a → b → c has exactly one root: the only node no transition points to.
     */
    @Test
    public void testFindRootNodesSingleRoot( )
    {
        DecisionNode a = node( 1, 2 );
        DecisionNode b = node( 2, 3 );
        DecisionNode c = node( 3 );

        List<DecisionNode> roots = DecisionTreeService.findRootNodes( Arrays.asList( a, b, c ) );

        assertEquals( 1, roots.size( ) );
        assertEquals( 1, roots.get( 0 ).getId( ) );
    }

    /**
     * With several untargeted nodes there is no unambiguous root, so the contract falls back to the first node of the list.
     */
    @Test
    public void testFindRootNodesMultipleRootsFallsBackToFirst( )
    {
        DecisionNode a = node( 1, 3 );
        DecisionNode b = node( 2, 3 );
        DecisionNode c = node( 3 );

        List<DecisionNode> roots = DecisionTreeService.findRootNodes( Arrays.asList( a, b, c ) );

        assertEquals( 1, roots.size( ) );
        assertEquals( 1, roots.get( 0 ).getId( ) );
    }

    /**
     * In a pure cycle every node is targeted, so there is no root and the contract falls back to the first node.
     */
    @Test
    public void testFindRootNodesCycleFallsBackToFirst( )
    {
        DecisionNode a = node( 1, 2 );
        DecisionNode b = node( 2, 1 );

        List<DecisionNode> roots = DecisionTreeService.findRootNodes( Arrays.asList( a, b ) );

        assertEquals( 1, roots.size( ) );
        assertEquals( 1, roots.get( 0 ).getId( ) );
    }

    /**
     * An empty node list has no roots.
     */
    @Test
    public void testFindRootNodesEmpty( )
    {
        assertTrue( DecisionTreeService.findRootNodes( new ArrayList<>( ) ).isEmpty( ) );
    }

    /**
     * A node unreachable from the root is reported as an orphan; connected nodes are not.
     */
    @Test
    public void testGetOrphanNodesDetectsDisconnected( )
    {
        DecisionNode a = node( 1, 2 );
        DecisionNode b = node( 2, 3 );
        DecisionNode c = node( 3 );
        DecisionNode orphan = node( 4 );

        List<DecisionNode> orphans = DecisionTreeService.getOrphanNodes( Arrays.asList( a, b, c, orphan ) );

        assertEquals( 1, orphans.size( ) );
        assertEquals( 4, orphans.get( 0 ).getId( ) );
    }

    /**
     * A fully connected chain has no orphans.
     */
    @Test
    public void testGetOrphanNodesNoneWhenAllConnected( )
    {
        DecisionNode a = node( 1, 2 );
        DecisionNode b = node( 2, 3 );
        DecisionNode c = node( 3 );

        assertTrue( DecisionTreeService.getOrphanNodes( Arrays.asList( a, b, c ) ).isEmpty( ) );
    }

    /**
     * A cycle among reachable nodes does not loop forever and yields no orphans, while a disconnected node is still flagged.
     */
    @Test
    public void testGetOrphanNodesHandlesCycle( )
    {
        DecisionNode a = node( 1, 2 );
        DecisionNode b = node( 2, 3 );
        DecisionNode c = node( 3, 2 );
        DecisionNode orphan = node( 9 );

        List<DecisionNode> orphans = DecisionTreeService.getOrphanNodes( Arrays.asList( a, b, c, orphan ) );

        assertEquals( 1, orphans.size( ) );
        assertEquals( 9, orphans.get( 0 ).getId( ) );
    }

    /**
     * An empty node list has no orphans.
     */
    @Test
    public void testGetOrphanNodesEmpty( )
    {
        assertTrue( DecisionTreeService.getOrphanNodes( new ArrayList<>( ) ).isEmpty( ) );
    }

    /**
     * Builds an in-memory node with the given id and outgoing transitions to the given target ids.
     *
     * @param nId
     *            the node identifier
     * @param targetIds
     *            the identifiers this node transitions to
     * @return the node with its transitions populated
     */
    private DecisionNode node( int nId, int... targetIds )
    {
        DecisionNode node = new DecisionNode( );
        node.setId( nId );
        node.setNodeTitle( "Node " + nId );
        if ( targetIds.length == 0 )
        {
            node.setTransitions( Collections.emptyList( ) );
            return node;
        }
        List<DecisionTransition> transitions = new ArrayList<>( );
        for ( int target : targetIds )
        {
            DecisionTransition transition = new DecisionTransition( );
            transition.setSourceNodeId( nId );
            transition.setTargetNodeId( target );
            transitions.add( transition );
        }
        node.setTransitions( transitions );
        return node;
    }
}
