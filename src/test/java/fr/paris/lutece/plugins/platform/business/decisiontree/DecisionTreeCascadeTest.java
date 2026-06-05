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
 * Verifies the {@code ON DELETE CASCADE} foreign keys rooted on a DecisionTree. Removing the root must cascade-delete the rows of every child table that
 * references it: {@code platform_decision_node} (tree_id) and {@code platform_decision_tree_image} (tree_id). Foreign keys are active on HSQL, so this test
 * proves the cascade end to end.
 */
public class DecisionTreeCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Creates a DecisionTree root with one DecisionNode and one DecisionTreeImage child, asserts both children exist, removes the root once, then asserts both
     * children have been cascade-deleted.
     */
    @Test
    public void testCascadeOnDeleteDecisionTree( )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( "Cascade Root Tree" );
        tree.setClientId( 1 );
        DecisionTreeHome.create( tree );

        DecisionNode node = new DecisionNode( );
        node.setTreeId( tree.getId( ) );
        node.setNodeTitle( "Cascade Node" );
        node.setShowBackButton( false );
        node.setBackTargetNodeId( null );
        node.setContent( "Cascade Content" );
        DecisionNodeHome.create( node );

        DecisionTreeImage image = new DecisionTreeImage( );
        image.setContentHash( "cascade-hash-0001" );
        image.setTreeId( tree.getId( ) );
        image.setFileStoreKey( "cascade-file-store-key" );
        image.setMimeType( "image/png" );
        DecisionTreeImageHome.create( image );

        assertTrue( DecisionNodeHome.findByPrimaryKey( node.getId( ) ).isPresent( ) );
        assertTrue( DecisionTreeImageHome.findByPrimaryKey( image.getContentHash( ), tree.getId( ) ).isPresent( ) );

        DecisionTreeHome.remove( tree.getId( ) );

        assertFalse( DecisionNodeHome.findByPrimaryKey( node.getId( ) ).isPresent( ) );
        assertTrue( DecisionNodeHome.getNodesByTreeId( tree.getId( ) ).isEmpty( ) );
        assertFalse( DecisionTreeImageHome.findByPrimaryKey( image.getContentHash( ), tree.getId( ) ).isPresent( ) );
        assertTrue( DecisionTreeImageHome.findByTreeId( tree.getId( ) ).isEmpty( ) );
    }
}
