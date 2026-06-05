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
package fr.paris.lutece.plugins.platform.business.decisiontree;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test the CRUD cycle of the DecisionNodeHome facade on HSQL.
 */
public class DecisionNodeHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a DecisionNode: create, findByPrimaryKey, update, remove. A parent DecisionTree is created first to satisfy the
     * {@code tree_id} foreign key and removed at the end of the test.
     */
    @Test
    public void testBusinessDecisionNode( )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( "Tree for nodes" );
        tree.setClientId( 1 );
        DecisionTreeHome.create( tree );

        DecisionNode node = new DecisionNode( );
        node.setTreeId( tree.getId( ) );
        node.setNodeTitle( "Node Title 1" );
        node.setShowBackButton( false );
        node.setBackTargetNodeId( null );
        node.setContent( "Content 1" );

        DecisionNodeHome.create( node );

        Optional<DecisionNode> optStored = DecisionNodeHome.findByPrimaryKey( node.getId( ) );
        assertTrue( optStored.isPresent( ) );
        DecisionNode nodeStored = optStored.get( );
        assertEquals( nodeStored.getTreeId( ), node.getTreeId( ) );
        assertEquals( nodeStored.getNodeTitle( ), node.getNodeTitle( ) );
        assertEquals( nodeStored.getContent( ), node.getContent( ) );

        node.setNodeTitle( "Node Title 2" );
        node.setContent( "Content 2" );
        DecisionNodeHome.update( node );

        nodeStored = DecisionNodeHome.findByPrimaryKey( node.getId( ) ).get( );
        assertEquals( nodeStored.getNodeTitle( ), node.getNodeTitle( ) );
        assertEquals( nodeStored.getContent( ), node.getContent( ) );

        DecisionNodeHome.getNodesByTreeId( tree.getId( ) );

        DecisionNodeHome.remove( node.getId( ) );
        optStored = DecisionNodeHome.findByPrimaryKey( node.getId( ) );
        assertFalse( optStored.isPresent( ) );
    }
}
