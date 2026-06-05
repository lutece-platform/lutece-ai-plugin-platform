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
 * Test the CRUD cycle of the DecisionTreeHome facade on HSQL.
 */
public class DecisionTreeHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a DecisionTree: create, findByPrimaryKey, update, remove.
     */
    @Test
    public void testBusinessDecisionTree( )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( "Tree Name" );
        tree.setTreeDescription( "Description 1" );
        tree.setClientId( 1 );
        tree.setWelcomeMessage( "Welcome 1" );
        tree.setEndMessage( "End 1" );
        tree.setLogoBase64( null );

        DecisionTreeHome.create( tree );

        Optional<DecisionTree> optStored = DecisionTreeHome.findByPrimaryKey( tree.getId( ) );
        assertTrue( optStored.isPresent( ) );
        DecisionTree treeStored = optStored.get( );
        assertEquals( treeStored.getTreeName( ), tree.getTreeName( ) );
        assertEquals( treeStored.getTreeDescription( ), tree.getTreeDescription( ) );
        assertEquals( treeStored.getClientId( ), tree.getClientId( ) );
        assertEquals( treeStored.getWelcomeMessage( ), tree.getWelcomeMessage( ) );
        assertEquals( treeStored.getEndMessage( ), tree.getEndMessage( ) );

        tree.setTreeDescription( "Description 2" );
        tree.setWelcomeMessage( "Welcome 2" );
        DecisionTreeHome.update( tree );

        treeStored = DecisionTreeHome.findByPrimaryKey( tree.getId( ) ).get( );
        assertEquals( treeStored.getTreeDescription( ), tree.getTreeDescription( ) );
        assertEquals( treeStored.getWelcomeMessage( ), tree.getWelcomeMessage( ) );

        DecisionTreeHome.remove( tree.getId( ) );
        optStored = DecisionTreeHome.findByPrimaryKey( tree.getId( ) );
        assertFalse( optStored.isPresent( ) );

        DecisionTreeHome.getDecisionTreesList( );
        DecisionTreeHome.getDecisionTreesReferenceList( );
        DecisionTreeHome.getDecisionTreesListByClientId( 1 );
    }
}
