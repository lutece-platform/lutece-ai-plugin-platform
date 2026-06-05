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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test the create and read operations of the DecisionTreeConversationHome facade on HSQL. The conversation table is transactional and the facade exposes no
 * remove operation, so this test covers conversation insertion, step insertion and their read-back only.
 */
public class DecisionTreeConversationHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises createConversation, findConversationById, addStep and findStepsByConversationId on a freshly generated conversation identifier.
     */
    @Test
    public void testBusinessDecisionTreeConversation( )
    {
        String strConversationId = UUID.randomUUID( ).toString( );

        DecisionTreeConversation conversation = new DecisionTreeConversation( );
        conversation.setConversationId( strConversationId );
        conversation.setTreeId( 1 );
        conversation.setClientId( 1 );

        DecisionTreeConversationHome.createConversation( conversation );

        Optional<DecisionTreeConversation> optStored = DecisionTreeConversationHome.findConversationById( strConversationId );
        assertTrue( optStored.isPresent( ) );
        DecisionTreeConversation conversationStored = optStored.get( );
        assertEquals( conversationStored.getConversationId( ), strConversationId );
        assertEquals( conversationStored.getTreeId( ), conversation.getTreeId( ) );
        assertEquals( conversationStored.getClientId( ), conversation.getClientId( ) );

        DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
        step.setConversationId( strConversationId );
        step.setNodeId( 1 );
        step.setNodeTitle( "Step Node Title" );
        step.setChosenTransitionId( 0 );

        DecisionTreeConversationHome.addStep( step );

        List<DecisionTreeConversationStep> steps = DecisionTreeConversationHome.findStepsByConversationId( strConversationId );
        assertEquals( 1, steps.size( ) );
        assertEquals( steps.get( 0 ).getNodeTitle( ), step.getNodeTitle( ) );

        DecisionTreeConversationHome.findConversationsByTreeId( conversation.getTreeId( ) );
        DecisionTreeConversationHome.getAverageStepsPerConversation( conversation.getTreeId( ), null, null );
    }

    /**
     * The date range is applied to the step/duration aggregates: a window containing the conversations yields their average, a window entirely in the past
     * yields zero.
     */
    @Test
    public void testAggregatesHonorDateRange( )
    {
        int nTreeId = 4242;
        String strConversationId = UUID.randomUUID( ).toString( );

        DecisionTreeConversation conversation = new DecisionTreeConversation( );
        conversation.setConversationId( strConversationId );
        conversation.setTreeId( nTreeId );
        conversation.setClientId( 1 );
        DecisionTreeConversationHome.createConversation( conversation );

        for ( int i = 0; i < 3; i++ )
        {
            DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
            step.setConversationId( strConversationId );
            step.setNodeId( i + 1 );
            step.setNodeTitle( "Step " + i );
            step.setChosenTransitionId( 0 );
            DecisionTreeConversationHome.addStep( step );
        }

        java.sql.Timestamp past = java.sql.Timestamp.valueOf( "2000-01-01 00:00:00" );
        java.sql.Timestamp pastEnd = java.sql.Timestamp.valueOf( "2000-12-31 23:59:59" );
        java.sql.Timestamp future = java.sql.Timestamp.valueOf( "2999-01-01 00:00:00" );

        assertEquals( 3.0, DecisionTreeConversationHome.getAverageStepsPerConversation( nTreeId, past, future ), 0.001,
                "a window containing the conversation must count its steps" );
        assertEquals( 0.0, DecisionTreeConversationHome.getAverageStepsPerConversation( nTreeId, past, pastEnd ), 0.001,
                "a window entirely in the past must yield zero" );
        assertEquals( 3, DecisionTreeConversationHome.getStepCountsByTreeId( nTreeId, past, future ).get( strConversationId ),
                "step counts must be served within the window" );
        assertTrue( DecisionTreeConversationHome.getStepCountsByTreeId( nTreeId, past, pastEnd ).isEmpty( ), "no step counts outside the window" );
    }
}
