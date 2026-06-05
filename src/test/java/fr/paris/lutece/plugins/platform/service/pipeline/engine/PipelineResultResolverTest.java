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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineStateManager.NodeState;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineDefinition;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineEdge;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineResultResolver}, which selects the pipeline's final output from the completed END nodes. These cover the single-END,
 * no-END (error) and multiple-END (first-wins) cases.
 */
public class PipelineResultResolverTest extends AbstractPlatformDbTest
{
    private static final String ROOT = "start";

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
     * Builds a port-agnostic edge between two nodes.
     *
     * @param source
     *            the source node id
     * @param target
     *            the target node id
     * @return the edge
     */
    private PipelineEdge edge( String source, String target )
    {
        PipelineEdge e = new PipelineEdge( );
        e.setSource( source );
        e.setTarget( target );
        return e;
    }

    /**
     * Builds a graph with a START root and two END nodes, end1 declared before end2.
     *
     * @return the graph
     */
    private PipelineGraph twoEndGraph( )
    {
        Map<String, PipelineNodeConfig> nodes = new LinkedHashMap<>( );
        nodes.put( ROOT, node( ROOT, "START" ) );
        nodes.put( "end1", node( "end1", "END" ) );
        nodes.put( "end2", node( "end2", "END" ) );

        PipelineDefinition def = new PipelineDefinition( );
        def.setNodes( nodes );
        def.setEdges( List.of( edge( ROOT, "end1" ), edge( ROOT, "end2" ) ) );
        def.setRootNodeId( ROOT );
        return new PipelineGraph( def );
    }

    /**
     * Builds a context holding distinct outputs for both END nodes.
     *
     * @return the context
     */
    private PipelineContext contextWithEndOutputs( )
    {
        PipelineContext context = new PipelineContext( "job", 1, "obs", new HashMap<>( ) );
        context.addNodeOutput( "end1", Map.of( "result", "A" ) );
        context.addNodeOutput( "end2", Map.of( "result", "B" ) );
        return context;
    }

    /**
     * When exactly one END node completed, its output is returned as the final result.
     */
    @Test
    public void testSingleCompletedEndNodeWins( )
    {
        Map<String, NodeState> states = new HashMap<>( );
        states.put( ROOT, NodeState.COMPLETED );
        states.put( "end1", NodeState.COMPLETED );
        states.put( "end2", NodeState.NOT_STARTED );

        Map<String, Object> result = new PipelineResultResolver( ).determineFinalResult( twoEndGraph( ), contextWithEndOutputs( ), states );

        assertEquals( "A", result.get( "result" ) );
    }

    /**
     * When no END node reached completion, resolution fails with a pipeline execution exception rather than returning a partial result.
     */
    @Test
    public void testNoCompletedEndNodeThrows( )
    {
        Map<String, NodeState> states = new HashMap<>( );
        states.put( ROOT, NodeState.COMPLETED );
        states.put( "end1", NodeState.NOT_STARTED );
        states.put( "end2", NodeState.FAILED );

        PipelineGraph graph = twoEndGraph( );
        PipelineContext context = contextWithEndOutputs( );
        PipelineResultResolver resolver = new PipelineResultResolver( );

        assertThrows( PipelineExecutionException.class, ( ) -> resolver.determineFinalResult( graph, context, states ) );
    }

    /**
     * When multiple END nodes completed, the first one declared is chosen deterministically.
     */
    @Test
    public void testMultipleCompletedEndNodesUseFirst( )
    {
        Map<String, NodeState> states = new HashMap<>( );
        states.put( ROOT, NodeState.COMPLETED );
        states.put( "end1", NodeState.COMPLETED );
        states.put( "end2", NodeState.COMPLETED );

        Map<String, Object> result = new PipelineResultResolver( ).determineFinalResult( twoEndGraph( ), contextWithEndOutputs( ), states );

        assertEquals( "A", result.get( "result" ) );
    }
}
