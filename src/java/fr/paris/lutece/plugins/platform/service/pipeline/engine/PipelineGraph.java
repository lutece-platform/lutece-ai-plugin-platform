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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineEdge;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineDefinition;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl.RetryManagerNode;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;

public class PipelineGraph
{

    private static final String INVALID_PIPELINE_DEFINITION_MISSING_FIELDS = "Invalid pipeline definition: Missing rootNodeId, nodes, or edges.";
    private static final String INVALID_PIPELINE_DEFINITION_ROOT_NODE_NOT_FOUND = "Invalid pipeline definition: Root node ID '{}' not found in nodes map.";
    private static final String INVALID_PIPELINE_DEFINITION_UNKNOWN_EDGE_NODE = "Invalid pipeline definition: Edge references unknown node ID '{}'.";
    private static final String INVALID_PIPELINE_DEFINITION_CYCLE = "Invalid pipeline definition: Cycle detected involving nodes {}.";

    private final Map<String, PipelineNodeConfig> nodes;
    private final Map<String, List<PipelineEdge>> incomingEdges;
    private final Map<String, List<PipelineEdge>> outgoingEdges;
    private final String rootNodeId;

    /**
     * Constructs a PipelineGraph from a pipeline definition.
     *
     * @param definition
     *            the pipeline definition containing nodes, edges and root node ID
     * @throws PipelineConfigurationException
     *             if the definition is invalid
     */
    public PipelineGraph( PipelineDefinition definition )
    {
        if ( definition.getRootNodeId( ) == null || definition.getNodes( ) == null || definition.getEdges( ) == null )
        {
            throw new PipelineConfigurationException( INVALID_PIPELINE_DEFINITION_MISSING_FIELDS );
        }
        if ( !definition.getNodes( ).containsKey( definition.getRootNodeId( ) ) )
        {
            throw new PipelineConfigurationException( INVALID_PIPELINE_DEFINITION_ROOT_NODE_NOT_FOUND.replace( "{}", definition.getRootNodeId( ) ) );
        }
        this.nodes = definition.getNodes( );
        this.rootNodeId = definition.getRootNodeId( );
        validateEdgeEndpoints( definition.getEdges( ) );
        validateAcyclic( definition.getEdges( ) );
        this.incomingEdges = buildIncomingEdgesMap( definition.getEdges( ) );
        this.outgoingEdges = buildOutgoingEdgesMap( definition.getEdges( ) );
    }

    /**
     * Validates that every edge references nodes declared in the node map.
     *
     * @param allEdges
     *            list of all edges in the pipeline
     * @throws PipelineConfigurationException
     *             if an edge source or target is unknown
     */
    private void validateEdgeEndpoints( List<PipelineEdge> allEdges )
    {
        for ( PipelineEdge edge : allEdges )
        {
            if ( !nodes.containsKey( edge.getSource( ) ) )
            {
                throw new PipelineConfigurationException( INVALID_PIPELINE_DEFINITION_UNKNOWN_EDGE_NODE.replace( "{}", String.valueOf( edge.getSource( ) ) ) );
            }
            if ( !nodes.containsKey( edge.getTarget( ) ) )
            {
                throw new PipelineConfigurationException( INVALID_PIPELINE_DEFINITION_UNKNOWN_EDGE_NODE.replace( "{}", String.valueOf( edge.getTarget( ) ) ) );
            }
        }
    }

    /**
     * Validates that the graph contains no cycle, using Kahn's topological sort on scheduling edges. Retry-manager back-edges are excluded, mirroring the
     * exemption applied by {@link #areAllDependenciesMet(String, Set)}: they are intentional loops that never block the scheduler.
     *
     * @param allEdges
     *            list of all edges in the pipeline
     * @throws PipelineConfigurationException
     *             if a cycle is detected among non-exempt edges
     */
    private void validateAcyclic( List<PipelineEdge> allEdges )
    {
        List<PipelineEdge> schedulingEdges = allEdges.stream( ).filter( edge -> !isRetryManagerBackEdge( edge ) ).toList( );
        Map<String, Integer> inDegree = new HashMap<>( );
        nodes.keySet( ).forEach( nodeId -> inDegree.put( nodeId, 0 ) );
        schedulingEdges.forEach( edge -> inDegree.merge( edge.getTarget( ), 1, Integer::sum ) );

        Deque<String> ready = new ArrayDeque<>( );
        inDegree.forEach( ( nodeId, degree ) -> {
            if ( degree == 0 )
            {
                ready.add( nodeId );
            }
        } );

        int processed = 0;
        Map<String, List<PipelineEdge>> outgoing = schedulingEdges.stream( ).collect( Collectors.groupingBy( PipelineEdge::getSource ) );
        while ( !ready.isEmpty( ) )
        {
            String nodeId = ready.poll( );
            processed++;
            for ( PipelineEdge edge : outgoing.getOrDefault( nodeId, Collections.emptyList( ) ) )
            {
                int remaining = inDegree.merge( edge.getTarget( ), -1, Integer::sum );
                if ( remaining == 0 )
                {
                    ready.add( edge.getTarget( ) );
                }
            }
        }

        if ( processed < nodes.size( ) )
        {
            String cyclicNodes = inDegree.entrySet( ).stream( ).filter( entry -> entry.getValue( ) > 0 ).map( Map.Entry::getKey ).sorted( )
                    .collect( Collectors.joining( ", " ) );
            throw new PipelineConfigurationException( INVALID_PIPELINE_DEFINITION_CYCLE.replace( "{}", cyclicNodes ) );
        }
    }

    /**
     * Tells whether an edge is a retry-manager back-edge, exempt from dependency and cycle checks.
     *
     * @param edge
     *            the edge to inspect
     * @return true if the edge leaves a retry-manager node through its retry output port
     */
    private boolean isRetryManagerBackEdge( PipelineEdge edge )
    {
        PipelineNodeConfig sourceConfig = nodes.get( edge.getSource( ) );
        return sourceConfig != null && PipelineConstants.NODE_TYPE_RETRY_MANAGER.equals( sourceConfig.getType( ) )
                && RetryManagerNode.RETRY_OUTPUT_PORT.equals( edge.getSourcePort( ) );
    }

    /**
     * Retrieves the node configuration for the specified node ID.
     *
     * @param nodeId
     *            the ID of the node
     * @return the node configuration or null if not found
     */
    public PipelineNodeConfig getNodeConfig( String nodeId )
    {
        return nodes.get( nodeId );
    }

    /**
     * Gets the root node ID of the pipeline.
     *
     * @return the root node ID
     */
    public String getRootNodeId( )
    {
        return rootNodeId;
    }

    /**
     * Gets an unmodifiable view of all nodes in the pipeline.
     *
     * @return unmodifiable map of node ID to node configuration
     */
    public Map<String, PipelineNodeConfig> getNodes( )
    {
        return Collections.unmodifiableMap( nodes );
    }

    /**
     * Gets all outgoing edges from the specified node.
     *
     * @param nodeId
     *            the ID of the source node
     * @return list of outgoing edges or empty list if none
     */
    public List<PipelineEdge> getOutgoingEdges( String nodeId )
    {
        return outgoingEdges.getOrDefault( nodeId, Collections.emptyList( ) );
    }

    /**
     * Gets outgoing edges from the specified node and output port.
     *
     * @param nodeId
     *            the ID of the source node
     * @param outputPort
     *            the output port name
     * @return list of matching outgoing edges
     */
    public List<PipelineEdge> getOutgoingEdges( String nodeId, String outputPort )
    {
        return getOutgoingEdges( nodeId ).stream( ).filter( edge -> edge.getSourcePort( ) == null || edge.getSourcePort( ).equals( outputPort ) ).toList( );
    }

    /**
     * Gets all incoming edges to the specified node.
     *
     * @param nodeId
     *            the ID of the target node
     * @return list of incoming edges or empty list if none
     */
    public List<PipelineEdge> getIncomingEdges( String nodeId )
    {
        return incomingEdges.getOrDefault( nodeId, Collections.emptyList( ) );
    }

    /**
     * Checks if all dependencies for a node are met based on completed nodes.
     *
     * @param nodeId
     *            the ID of the node to check
     * @param completedNodes
     *            set of completed node IDs
     * @return true if all dependencies are met, false otherwise
     */
    public boolean areAllDependenciesMet( String nodeId, Set<String> completedNodes )
    {
        if ( nodeId.equals( rootNodeId ) )
        {
            return true;
        }

        List<PipelineEdge> dependencies = getIncomingEdges( nodeId );
        if ( dependencies.isEmpty( ) )
        {
            return true;
        }

        return dependencies.stream( ).allMatch( dependency -> {
            String sourceNodeId = dependency.getSource( );
            String sourcePort = dependency.getSourcePort( );
            PipelineNodeConfig sourceConfig = getNodeConfig( sourceNodeId );

            if ( sourceConfig != null && sourceConfig.getType( ).equals( PipelineConstants.NODE_TYPE_RETRY_MANAGER )
                    && RetryManagerNode.RETRY_OUTPUT_PORT.equals( sourcePort ) )
            {
                return true;
            }

            return completedNodes.contains( sourceNodeId );
        } );
    }

    /**
     * Builds a map of incoming edges grouped by target node.
     *
     * @param allEdges
     *            list of all edges in the pipeline
     * @return map of target node ID to list of incoming edges
     */
    private Map<String, List<PipelineEdge>> buildIncomingEdgesMap( List<PipelineEdge> allEdges )
    {
        return allEdges.stream( ).collect( Collectors.groupingBy( PipelineEdge::getTarget ) );
    }

    /**
     * Builds a map of outgoing edges grouped by source node.
     *
     * @param allEdges
     *            list of all edges in the pipeline
     * @return map of source node ID to list of outgoing edges
     */
    private Map<String, List<PipelineEdge>> buildOutgoingEdgesMap( List<PipelineEdge> allEdges )
    {
        return allEdges.stream( ).collect( Collectors.groupingBy( PipelineEdge::getSource ) );
    }
}
