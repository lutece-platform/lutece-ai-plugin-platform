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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Verifies the {@code ON DELETE CASCADE} foreign keys hanging off the DecisionNode root entity on HSQL.
 */
public class DecisionNodeCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Proves that removing a DecisionNode cascades to its child transitions.
     * <p>
     * A parent DecisionTree and two DecisionNodes (source and target) are created, then a single DecisionTransition that references the source node via
     * {@code source_node_id} and the target node via {@code target_node_id}. Removing the source node must delete the transition through the
     * {@code source_node_id} CASCADE foreign key; removing the target node must likewise delete a second transition through the {@code target_node_id} CASCADE
     * foreign key. Both FK columns are therefore exercised.
     */
    @Test
    public void testCascadeOnDeleteDecisionNode( )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( "Tree for cascade" );
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

        DecisionTransition sourceFkTransition = new DecisionTransition( );
        sourceFkTransition.setSourceNodeId( sourceNode.getId( ) );
        sourceFkTransition.setTargetNodeId( targetNode.getId( ) );
        sourceFkTransition.setLabel( "Source FK transition" );
        sourceFkTransition.setSortOrder( 1 );
        DecisionTransitionHome.create( sourceFkTransition );

        DecisionTransition targetFkTransition = new DecisionTransition( );
        targetFkTransition.setSourceNodeId( targetNode.getId( ) );
        targetFkTransition.setTargetNodeId( targetNode.getId( ) );
        targetFkTransition.setLabel( "Target FK transition" );
        targetFkTransition.setSortOrder( 2 );
        DecisionTransitionHome.create( targetFkTransition );

        assertTrue( DecisionTransitionHome.findByPrimaryKey( sourceFkTransition.getId( ) ).isPresent( ) );
        assertTrue( DecisionTransitionHome.findByPrimaryKey( targetFkTransition.getId( ) ).isPresent( ) );

        DecisionNodeHome.remove( sourceNode.getId( ) );
        assertFalse( DecisionTransitionHome.findByPrimaryKey( sourceFkTransition.getId( ) ).isPresent( ) );

        DecisionNodeHome.remove( targetNode.getId( ) );
        assertFalse( DecisionTransitionHome.findByPrimaryKey( targetFkTransition.getId( ) ).isPresent( ) );
    }
}
