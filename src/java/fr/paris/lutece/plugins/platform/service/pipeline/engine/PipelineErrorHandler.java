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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import fr.paris.lutece.portal.service.util.AppLogService;

public class PipelineErrorHandler
{
    private static final String NODE_FAILED_MESSAGE = "Node {} ({}) failed: {}";

    /**
     * Handles a node failure by unwrapping the root cause, marking the node as failed, cancelling its future and logging the error.
     *
     * @param nodeId
     *            the failed node identifier
     * @param nodeType
     *            the failed node type
     * @param throwable
     *            the error raised during execution
     * @param stateManager
     *            the pipeline state manager
     */
    public static void handleNodeError( String nodeId, String nodeType, Throwable throwable, PipelineStateManager stateManager )
    {

        Throwable rootCause = throwable.getCause( );
        Throwable cause = ( throwable instanceof CompletionException && rootCause != null ) ? rootCause : throwable;

        stateManager.markNodeAsFailed( nodeId );
        stateManager.cancelNodeFuture( nodeId );

        AppLogService.error( NODE_FAILED_MESSAGE, nodeId, nodeType, cause.getMessage( ), cause );

    }

    /**
     * Creates a CompletableFuture already completed exceptionally with the given cause.
     *
     * @param <T>
     *            the future result type
     * @param cause
     *            the failure cause
     * @return a future completed exceptionally with the cause
     */
    public static <T> CompletableFuture<T> completeExceptionally( Throwable cause )
    {
        CompletableFuture<T> future = new CompletableFuture<>( );
        future.completeExceptionally( cause );
        return future;
    }
}
