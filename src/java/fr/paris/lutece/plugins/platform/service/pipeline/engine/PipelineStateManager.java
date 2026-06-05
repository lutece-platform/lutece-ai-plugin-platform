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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.paris.lutece.portal.service.util.AppLogService;

public class PipelineStateManager
{

    private static final String NODE_FAILED_LOG = "Node {} failed";

    public enum NodeState
    {
        NOT_STARTED,
        READY,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final ConcurrentHashMap<String, NodeState> nodeStates;
    private final ConcurrentLinkedQueue<String> readyQueue;
    private final ConcurrentHashMap<String, CompletableFuture<?>> nodeFutures;
    private final Set<CompletableFuture<?>> activeFutures;

    /**
     * Constructs a new PipelineStateManager with the specified node IDs.
     *
     * @param nodeIds
     *            set of node IDs to manage
     */
    public PipelineStateManager( Set<String> nodeIds )
    {
        this.nodeStates = new ConcurrentHashMap<>( );
        this.readyQueue = new ConcurrentLinkedQueue<>( );
        this.nodeFutures = new ConcurrentHashMap<>( );
        this.activeFutures = ConcurrentHashMap.newKeySet( );
        nodeIds.forEach( nodeId -> nodeStates.put( nodeId, NodeState.NOT_STARTED ) );
    }

    /**
     * Marks a node as ready for execution and adds it to the ready queue.
     *
     * @param nodeId
     *            the ID of the node to mark as ready
     */
    public void markNodeAsReady( String nodeId )
    {
        NodeState previousState = nodeStates.put( nodeId, NodeState.READY );
        if ( previousState != NodeState.READY )
        {
            readyQueue.add( nodeId );
        }
    }

    /**
     * Attempts to mark a node as running if it's currently ready.
     *
     * @param nodeId
     *            the ID of the node to mark as running
     * @return true if the state was successfully changed from READY to RUNNING
     */
    public boolean markNodeAsRunning( String nodeId )
    {
        return nodeStates.replace( nodeId, NodeState.READY, NodeState.RUNNING );
    }

    /**
     * Marks a node as completed.
     *
     * @param nodeId
     *            the ID of the node to mark as completed
     */
    public void markNodeAsCompleted( String nodeId )
    {
        nodeStates.put( nodeId, NodeState.COMPLETED );
    }

    /**
     * Marks a node as failed.
     *
     * @param nodeId
     *            the ID of the node to mark as failed
     */
    public void markNodeAsFailed( String nodeId )
    {
        if ( nodeStates.put( nodeId, NodeState.FAILED ) != NodeState.FAILED )
        {
            AppLogService.error( NODE_FAILED_LOG, nodeId );
        }
    }

    /**
     * Polls and returns the next ready node from the queue.
     *
     * @return the ID of the next ready node or null if queue is empty
     */
    public String pollReadyNode( )
    {
        return readyQueue.poll( );
    }

    /**
     * Gets the current state of a node.
     *
     * @param nodeId
     *            the ID of the node
     * @return the current state of the node
     */
    public NodeState getNodeState( String nodeId )
    {
        return nodeStates.get( nodeId );
    }

    /**
     * Checks if a node is completed.
     *
     * @param nodeId
     *            the ID of the node to check
     * @return true if the node is completed
     */
    public boolean isNodeCompleted( String nodeId )
    {
        return NodeState.COMPLETED.equals( getNodeState( nodeId ) );
    }

    /**
     * Gets a set of all completed node IDs.
     *
     * @return set of completed node IDs
     */
    public Set<String> getCompletedNodes( )
    {
        Set<String> completedNodes = ConcurrentHashMap.newKeySet( );
        nodeStates.forEach( ( nodeId, state ) -> {
            if ( state == NodeState.COMPLETED )
            {
                completedNodes.add( nodeId );
            }
        } );
        return completedNodes;
    }

    /**
     * Gets an unmodifiable view of all node states.
     *
     * @return unmodifiable map of node ID to state
     */
    public Map<String, NodeState> getNodeStates( )
    {
        return Collections.unmodifiableMap( nodeStates );
    }

    /**
     * Registers a CompletableFuture for a node's execution.
     *
     * @param nodeId
     *            the ID of the node
     * @param future
     *            the CompletableFuture to register
     */
    public void registerNodeFuture( String nodeId, CompletableFuture<?> future )
    {
        nodeFutures.put( nodeId, future );
        activeFutures.add( future );
        future.whenComplete( ( result, error ) -> activeFutures.remove( future ) );
    }

    /**
     * Cancels the future associated with a node.
     *
     * @param nodeId
     *            the ID of the node whose future to cancel
     */
    public void cancelNodeFuture( String nodeId )
    {
        CompletableFuture<?> future = nodeFutures.remove( nodeId );
        if ( future != null && !future.isDone( ) )
        {
            future.cancel( true );
            activeFutures.remove( future );
        }
    }

    /**
     * Checks if there are any active futures running.
     *
     * @return true if there are active futures
     */
    public boolean hasActiveFutures( )
    {
        return !activeFutures.isEmpty( );
    }

    /**
     * Checks if there are any ready nodes in the queue.
     *
     * @return true if there are ready nodes
     */
    public boolean hasReadyNodes( )
    {
        return !readyQueue.isEmpty( );
    }

    /**
     * Resets a node's state to NOT_STARTED and cancels its future.
     *
     * @param nodeId
     *            the ID of the node to reset
     */
    public void resetNodeState( String nodeId )
    {
        nodeStates.put( nodeId, NodeState.NOT_STARTED );
        cancelNodeFuture( nodeId );
    }
}
