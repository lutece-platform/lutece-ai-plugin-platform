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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineDefinition;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineEdge;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl.RetryManagerNode;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineGraph}: definition validation at construction, edge routing by output port, incoming/outgoing adjacency, and the
 * dependency-readiness logic that drives the scheduler — including the retry-manager back-edge exemption.
 */
public class PipelineGraphTest extends AbstractPlatformDbTest
{
    private static final String ROOT = "root";

    /**
     * Builds a node configuration with the given id and type.
     *
     * @param id
     *            the node id
     * @param type
     *            the node type
     * @return the node configuration
     */
    private PipelineNodeConfig node( String id, String type )
    {
        PipelineNodeConfig config = new PipelineNodeConfig( );
        config.setId( id );
        config.setType( type );
        return config;
    }

    /**
     * Builds an edge from a source node and port to a target node.
     *
     * @param source
     *            the source node id
     * @param sourcePort
     *            the source output port, or null for any port
     * @param target
     *            the target node id
     * @return the edge
     */
    private PipelineEdge edge( String source, String sourcePort, String target )
    {
        PipelineEdge e = new PipelineEdge( );
        e.setSource( source );
        e.setSourcePort( sourcePort );
        e.setTarget( target );
        return e;
    }

    /**
     * Assembles a pipeline definition from a node map and an edge list, rooted at {@link #ROOT}.
     *
     * @param nodes
     *            the node map
     * @param edges
     *            the edge list
     * @return the definition
     */
    private PipelineDefinition definition( Map<String, PipelineNodeConfig> nodes, List<PipelineEdge> edges )
    {
        PipelineDefinition def = new PipelineDefinition( );
        def.setNodes( nodes );
        def.setEdges( edges );
        def.setRootNodeId( ROOT );
        return def;
    }

    /**
     * A definition missing its node map (or edges, or root id) is rejected at construction.
     */
    @Test
    public void testConstructionRejectsIncompleteDefinition( )
    {
        PipelineDefinition def = new PipelineDefinition( );
        def.setRootNodeId( ROOT );
        def.setEdges( List.of( ) );

        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( def ) );
    }

    /**
     * A root id that is not present in the node map is rejected at construction.
     */
    @Test
    public void testConstructionRejectsUnknownRoot( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( "other", node( "other", "START" ) );

        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( definition( nodes, List.of( ) ) ) );
    }

    /**
     * Port-filtered outgoing lookup returns edges whose source port matches the requested port, plus port-agnostic edges (null source port), and excludes edges
     * bound to a different port.
     */
    @Test
    public void testOutgoingEdgesFilteredByPort( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "CONDITION" ) );
        nodes.put( "yes", node( "yes", "END" ) );
        nodes.put( "no", node( "no", "END" ) );
        nodes.put( "any", node( "any", "END" ) );

        PipelineEdge toYes = edge( ROOT, "true", "yes" );
        PipelineEdge toNo = edge( ROOT, "false", "no" );
        PipelineEdge toAny = edge( ROOT, null, "any" );

        PipelineGraph graph = new PipelineGraph( definition( nodes, List.of( toYes, toNo, toAny ) ) );

        assertEquals( 3, graph.getOutgoingEdges( ROOT ).size( ) );
        List<PipelineEdge> onTruePort = graph.getOutgoingEdges( ROOT, "true" );
        assertTrue( onTruePort.contains( toYes ) );
        assertTrue( onTruePort.contains( toAny ) );
        assertFalse( onTruePort.contains( toNo ) );
    }

    /**
     * The root node and any node without incoming edges are always ready; a node with a dependency becomes ready only once that dependency has completed.
     */
    @Test
    public void testDependencyReadiness( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "a", node( "a", "MODEL" ) );
        nodes.put( "b", node( "b", "END" ) );

        PipelineGraph graph = new PipelineGraph( definition( nodes, List.of( edge( ROOT, null, "a" ), edge( "a", null, "b" ) ) ) );

        assertTrue( graph.areAllDependenciesMet( ROOT, Set.of( ) ) );
        assertTrue( graph.areAllDependenciesMet( "a", Set.of( ROOT ) ) );
        assertFalse( graph.areAllDependenciesMet( "b", Set.of( ROOT ) ) );
        assertTrue( graph.areAllDependenciesMet( "b", Set.of( ROOT, "a" ) ) );
    }

    /**
     * An edge whose source or target references a node absent from the node map is rejected at construction.
     */
    @Test
    public void testConstructionRejectsEdgeWithUnknownNode( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );

        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( definition( nodes, List.of( edge( ROOT, null, "ghost" ) ) ) ) );
        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( definition( nodes, List.of( edge( "ghost", null, ROOT ) ) ) ) );
    }

    /**
     * A cycle between regular nodes is rejected at construction: without validation such a pipeline would hang forever, no node of the cycle ever becoming
     * ready.
     */
    @Test
    public void testConstructionRejectsCycle( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "a", node( "a", "MODEL" ) );
        nodes.put( "b", node( "b", "MODEL" ) );

        List<PipelineEdge> edges = List.of( edge( ROOT, null, "a" ), edge( "a", null, "b" ), edge( "b", null, "a" ) );

        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( definition( nodes, edges ) ) );
    }

    /**
     * A retry loop (body feeding a retry-manager whose retry port loops back to the body) is a legitimate cycle and must NOT be rejected: the back-edge is
     * exempt from cycle detection, mirroring the scheduler exemption.
     */
    @Test
    public void testRetryLoopIsNotRejectedAsCycle( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "body", node( "body", "MODEL" ) );
        nodes.put( "rm", node( "rm", PipelineConstants.NODE_TYPE_RETRY_MANAGER ) );

        List<PipelineEdge> edges = List.of( edge( ROOT, null, "body" ), edge( "body", null, "rm" ), edge( "rm", RetryManagerNode.RETRY_OUTPUT_PORT, "body" ) );

        PipelineGraph graph = new PipelineGraph( definition( nodes, edges ) );

        assertEquals( ROOT, graph.getRootNodeId( ) );
    }

    /**
     * A cycle closed through a retry-manager's NON-retry port is still a real cycle and is rejected: only the retry output port is exempt.
     */
    @Test
    public void testCycleThroughRetryManagerRegularPortIsRejected( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "body", node( "body", "MODEL" ) );
        nodes.put( "rm", node( "rm", PipelineConstants.NODE_TYPE_RETRY_MANAGER ) );

        List<PipelineEdge> edges = List.of( edge( ROOT, null, "body" ), edge( "body", null, "rm" ), edge( "rm", "success", "body" ) );

        assertThrows( PipelineConfigurationException.class, ( ) -> new PipelineGraph( definition( nodes, edges ) ) );
    }

    /**
     * A dependency coming from a retry-manager node's retry output port is treated as satisfied even when that node has not completed, so the back-edge does
     * not deadlock the loop body.
     */
    @Test
    public void testRetryManagerBackEdgeIsExempt( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "rm", node( "rm", PipelineConstants.NODE_TYPE_RETRY_MANAGER ) );
        nodes.put( "body", node( "body", "MODEL" ) );

        PipelineEdge backEdge = edge( "rm", RetryManagerNode.RETRY_OUTPUT_PORT, "body" );
        PipelineGraph graph = new PipelineGraph( definition( nodes, List.of( edge( ROOT, null, "rm" ), backEdge ) ) );

        assertTrue( graph.areAllDependenciesMet( "body", Set.of( ) ) );
    }
}
