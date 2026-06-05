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
package fr.paris.lutece.plugins.platform.service.pipeline.engine;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineStateManager.NodeState;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineStateManager}, the per-execution node state machine that drives the scheduler. These exercise the state transitions, the
 * single-dequeue invariant, the atomic READY→RUNNING claim that guarantees a node runs at most once, completion tracking and future lifecycle.
 */
public class PipelineStateManagerTest extends AbstractPlatformDbTest
{
    /**
     * Every declared node starts in NOT_STARTED and the ready queue is initially empty.
     */
    @Test
    public void testInitialStateIsNotStarted( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a", "b" ) );

        assertEquals( NodeState.NOT_STARTED, mgr.getNodeState( "a" ) );
        assertEquals( NodeState.NOT_STARTED, mgr.getNodeState( "b" ) );
        assertFalse( mgr.hasReadyNodes( ) );
        assertNull( mgr.pollReadyNode( ) );
    }

    /**
     * Marking a node ready enqueues it once; marking it ready again does not enqueue a duplicate, so it is polled exactly once.
     */
    @Test
    public void testMarkReadyEnqueuesOnce( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );

        mgr.markNodeAsReady( "a" );
        mgr.markNodeAsReady( "a" );

        assertEquals( NodeState.READY, mgr.getNodeState( "a" ) );
        assertEquals( "a", mgr.pollReadyNode( ) );
        assertNull( mgr.pollReadyNode( ) );
    }

    /**
     * The READY→RUNNING transition is an atomic claim: it succeeds once and fails on any further attempt, which is what stops a node from being executed twice.
     */
    @Test
    public void testRunningClaimIsAtomicAndSingle( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        mgr.markNodeAsReady( "a" );

        assertTrue( mgr.markNodeAsRunning( "a" ) );
        assertEquals( NodeState.RUNNING, mgr.getNodeState( "a" ) );
        assertFalse( mgr.markNodeAsRunning( "a" ) );
    }

    /**
     * A node that was never made ready cannot be claimed for running.
     */
    @Test
    public void testRunningClaimFailsWhenNotReady( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );

        assertFalse( mgr.markNodeAsRunning( "a" ) );
        assertEquals( NodeState.NOT_STARTED, mgr.getNodeState( "a" ) );
    }

    /**
     * Completion is reflected by the state, the completion predicate and the completed-node set.
     */
    @Test
    public void testCompletionTracking( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a", "b" ) );

        mgr.markNodeAsCompleted( "a" );

        assertTrue( mgr.isNodeCompleted( "a" ) );
        assertFalse( mgr.isNodeCompleted( "b" ) );
        assertEquals( Set.of( "a" ), mgr.getCompletedNodes( ) );
    }

    /**
     * The exposed node-state map is a read-only view and cannot be mutated by callers.
     */
    @Test
    public void testNodeStatesViewIsUnmodifiable( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );

        assertThrows( UnsupportedOperationException.class, ( ) -> mgr.getNodeStates( ).put( "a", NodeState.COMPLETED ) );
    }

    /**
     * A registered future counts as active until it completes, after which the manager no longer reports active work.
     */
    @Test
    public void testActiveFutureClearedOnCompletion( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        CompletableFuture<String> future = new CompletableFuture<>( );

        mgr.registerNodeFuture( "a", future );
        assertTrue( mgr.hasActiveFutures( ) );

        future.complete( "done" );
        assertFalse( mgr.hasActiveFutures( ) );
    }

    /**
     * Cancelling a node's future cancels the underlying task and clears it from the active set.
     */
    @Test
    public void testCancelNodeFuture( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        CompletableFuture<String> future = new CompletableFuture<>( );
        mgr.registerNodeFuture( "a", future );

        mgr.cancelNodeFuture( "a" );

        assertTrue( future.isCancelled( ) );
        assertFalse( mgr.hasActiveFutures( ) );
    }

    /**
     * Resetting a node returns it to NOT_STARTED so it can be scheduled again, as used by retry loops.
     */
    @Test
    public void testResetNodeState( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        mgr.markNodeAsCompleted( "a" );

        mgr.resetNodeState( "a" );

        assertEquals( NodeState.NOT_STARTED, mgr.getNodeState( "a" ) );
    }
}
