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
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineStateManager.NodeState;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineErrorHandler}. These verify that a node failure flips the node to FAILED and tears down its in-flight future, and that
 * the helper that builds an already-failed future carries the original cause.
 */
public class PipelineErrorHandlerTest extends AbstractPlatformDbTest
{
    /**
     * Handling a node error marks the node FAILED, cancels its registered future and leaves no active work behind.
     */
    @Test
    public void testHandleNodeErrorMarksFailedAndCancelsFuture( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        CompletableFuture<String> future = new CompletableFuture<>( );
        mgr.registerNodeFuture( "a", future );

        PipelineErrorHandler.handleNodeError( "a", "MODEL", new RuntimeException( "boom" ), mgr );

        assertEquals( NodeState.FAILED, mgr.getNodeState( "a" ) );
        assertTrue( future.isCancelled( ) );
        assertFalse( mgr.hasActiveFutures( ) );
    }

    /**
     * A node error wrapped in a {@link CompletionException} is still handled and flips the node to FAILED.
     */
    @Test
    public void testHandleNodeErrorUnwrapsCompletionException( )
    {
        PipelineStateManager mgr = new PipelineStateManager( Set.of( "a" ) );
        CompletionException wrapped = new CompletionException( new IllegalStateException( "root" ) );

        PipelineErrorHandler.handleNodeError( "a", "MODEL", wrapped, mgr );

        assertEquals( NodeState.FAILED, mgr.getNodeState( "a" ) );
    }

    /**
     * The exceptional-future helper returns a future that is already failed and re-throws the original cause when joined.
     */
    @Test
    public void testCompleteExceptionallyCarriesCause( )
    {
        IllegalArgumentException cause = new IllegalArgumentException( "bad" );

        CompletableFuture<String> future = PipelineErrorHandler.completeExceptionally( cause );

        assertTrue( future.isCompletedExceptionally( ) );
        CompletionException thrown = assertThrows( CompletionException.class, future::join );
        assertSame( cause, thrown.getCause( ) );
    }
}
