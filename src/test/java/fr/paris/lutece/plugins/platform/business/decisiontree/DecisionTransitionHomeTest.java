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
 * Test the CRUD cycle of the DecisionTransitionHome facade on HSQL.
 */
public class DecisionTransitionHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a DecisionTransition: create, findByPrimaryKey, update, remove. A parent DecisionTree and two DecisionNodes (source and
     * target) are created first to satisfy the {@code source_node_id} and {@code target_node_id} foreign keys, then removed at the end of the test.
     */
    @Test
    public void testBusinessDecisionTransition( )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( "Tree for transitions" );
        tree.setClientId( 1 );
        DecisionTreeHome.create( tree );

        DecisionNode sourceNode = new DecisionNode( );
        sourceNode.setTreeId( tree.getId( ) );
        sourceNode.setNodeTitle( "Source Node" );
        sourceNode.setShowBackButton( false );
        DecisionNodeHome.create( sourceNode );

        DecisionNode targetNode = new DecisionNode( );
        targetNode.setTreeId( tree.getId( ) );
        targetNode.setNodeTitle( "Target Node" );
        targetNode.setShowBackButton( false );
        DecisionNodeHome.create( targetNode );

        DecisionTransition transition = new DecisionTransition( );
        transition.setSourceNodeId( sourceNode.getId( ) );
        transition.setTargetNodeId( targetNode.getId( ) );
        transition.setLabel( "Label 1" );
        transition.setSortOrder( 1 );

        DecisionTransitionHome.create( transition );

        Optional<DecisionTransition> optStored = DecisionTransitionHome.findByPrimaryKey( transition.getId( ) );
        assertTrue( optStored.isPresent( ) );
        DecisionTransition transitionStored = optStored.get( );
        assertEquals( transitionStored.getSourceNodeId( ), transition.getSourceNodeId( ) );
        assertEquals( transitionStored.getTargetNodeId( ), transition.getTargetNodeId( ) );
        assertEquals( transitionStored.getLabel( ), transition.getLabel( ) );
        assertEquals( transitionStored.getSortOrder( ), transition.getSortOrder( ) );

        transition.setLabel( "Label 2" );
        transition.setSortOrder( 2 );
        DecisionTransitionHome.update( transition );

        transitionStored = DecisionTransitionHome.findByPrimaryKey( transition.getId( ) ).get( );
        assertEquals( transitionStored.getLabel( ), transition.getLabel( ) );
        assertEquals( transitionStored.getSortOrder( ), transition.getSortOrder( ) );

        DecisionTransitionHome.getTransitionsBySourceNodeId( sourceNode.getId( ) );

        DecisionTransitionHome.remove( transition.getId( ) );
        optStored = DecisionTransitionHome.findByPrimaryKey( transition.getId( ) );
        assertFalse( optStored.isPresent( ) );
    }
}
