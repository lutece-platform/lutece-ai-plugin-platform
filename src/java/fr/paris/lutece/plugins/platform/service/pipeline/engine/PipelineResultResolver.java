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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineStateManager.NodeState;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.portal.service.util.AppLogService;

public class PipelineResultResolver
{

    private static final String END_NODE_TYPE = "END";
    private static final String NO_END_NODE_COMPLETED_ERROR = "Pipeline finished, but no END node reached COMPLETED state.";
    private static final String PIPELINE_COMPLETED_WITHOUT_END_NODE_ERROR = "Pipeline completed without reaching a successful END node.";
    private static final String MULTIPLE_END_NODES_COMPLETED_ERROR = "Pipeline finished with multiple END nodes completed: {}. Using first one: {}";

    /**
     * Determines the final result of a pipeline execution based on completed END nodes.
     *
     * @param graph
     *            the pipeline graph containing node configurations
     * @param context
     *            the pipeline execution context with node outputs
     * @param nodeStates
     *            map of node IDs to their current states
     * @return map containing the final pipeline result
     * @throws PipelineExecutionException
     *             if no END node completed successfully
     */
    public Map<String, Object> determineFinalResult( PipelineGraph graph, PipelineContext context, Map<String, NodeState> nodeStates )
    {

        List<PipelineNodeConfig> completedEndNodes = getCompletedEndNodes( graph, nodeStates );

        if ( completedEndNodes.size( ) == 1 )
        {
            return handleSingleEndNode( completedEndNodes.get( 0 ), context );
        }
        else if ( completedEndNodes.isEmpty( ) )
        {
            return handleNoEndNode( );
        }
        else
        {
            return handleMultipleEndNodes( completedEndNodes, context );
        }
    }

    /**
     * Retrieves all END nodes that have completed successfully.
     *
     * @param graph
     *            the pipeline graph
     * @param nodeStates
     *            map of node states
     * @return list of completed END nodes
     */
    private List<PipelineNodeConfig> getCompletedEndNodes( PipelineGraph graph, Map<String, NodeState> nodeStates )
    {
        return graph.getNodes( ).values( ).stream( ).filter( nc -> END_NODE_TYPE.equalsIgnoreCase( nc.getType( ) ) )
                .filter( nc -> nodeStates.get( nc.getId( ) ) == NodeState.COMPLETED ).toList( );
    }

    /**
     * Handles the case where exactly one END node completed.
     *
     * @param endNode
     *            the completed END node
     * @param context
     *            the pipeline context
     * @return the END node's output as the final result
     */
    private Map<String, Object> handleSingleEndNode( PipelineNodeConfig endNode, PipelineContext context )
    {
        Map<String, Object> endNodeOutput = context.getNodeOutput( endNode.getId( ) );
        return endNodeOutput != null ? new ConcurrentHashMap<>( endNodeOutput ) : new ConcurrentHashMap<>( );
    }

    /**
     * Handles the case where no END node completed.
     *
     * @return never returns, always throws exception
     * @throws PipelineExecutionException
     *             always thrown
     */
    private Map<String, Object> handleNoEndNode( )
    {
        AppLogService.error( NO_END_NODE_COMPLETED_ERROR );
        throw new PipelineExecutionException( PIPELINE_COMPLETED_WITHOUT_END_NODE_ERROR );
    }

    /**
     * Handles the case where multiple END nodes completed.
     *
     * @param completedEndNodes
     *            list of completed END nodes
     * @param context
     *            the pipeline context
     * @return the first END node's output as the final result
     */
    private Map<String, Object> handleMultipleEndNodes( List<PipelineNodeConfig> completedEndNodes, PipelineContext context )
    {
        List<String> endNodeIds = completedEndNodes.stream( ).map( PipelineNodeConfig::getId ).toList( );

        PipelineNodeConfig firstEndNode = completedEndNodes.get( 0 );
        AppLogService.error( MULTIPLE_END_NODES_COMPLETED_ERROR, endNodeIds, firstEndNode.getId( ) );

        Map<String, Object> endNodeOutput = context.getNodeOutput( firstEndNode.getId( ) );
        return endNodeOutput != null ? new ConcurrentHashMap<>( endNodeOutput ) : new ConcurrentHashMap<>( );
    }
}
