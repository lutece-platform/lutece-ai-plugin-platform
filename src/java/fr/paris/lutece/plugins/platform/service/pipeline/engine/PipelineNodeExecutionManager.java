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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.service.concurrent.Orchestration;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineNodeCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineNodeFailedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineNodePortChosenEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineNodeStartedEvent;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.IPipelineNode;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * CDI-managed manager that drives the execution of a single pipeline node. Stateless across calls: per-execution observability identifiers are stored in a
 * concurrent map keyed by {@code jobId + "-" + nodeId} and removed on completion / failure of the node.
 */
@ApplicationScoped
@Named( "platform.pipelineNodeExecutionManager" )
public class PipelineNodeExecutionManager
{
    private static final String NODE_TYPE_NOT_FOUND_MESSAGE = "Node type not found: ";
    private static final String OBS_NODE_EXECUTION = "Exécution du nœud: ";
    private static final String OBS_NODE_SUCCESS = "Nœud exécuté avec succès";
    private static final String OBS_NODE_FAILED = "Échec de l'exécution du nœud: ";
    private static final String OBS_NODE_TYPE_INFO = "Type de nœud: ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    @Inject
    @Orchestration
    private ManagedExecutorService _orchestrator;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private PipelineNodeRegistry _pipelineNodeRegistry;

    @Inject
    private Event<PipelineNodeStartedEvent> _nodeStartedEvent;

    @Inject
    private Event<PipelineNodeCompletedEvent> _nodeCompletedEvent;

    @Inject
    private Event<PipelineNodePortChosenEvent> _nodePortChosenEvent;

    @Inject
    private Event<PipelineNodeFailedEvent> _nodeFailedEvent;

    private final Map<String, String> nodeObservabilityIds = new ConcurrentHashMap<>( );

    /**
     * Executes a pipeline node asynchronously.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param config
     *            the configuration of the pipeline node
     * @param context
     *            the pipeline execution context
     * @return a CompletableFuture containing the output port name
     */
    public CompletableFuture<String> executeNode( String nodeId, PipelineNodeConfig config, PipelineContext context )
    {
        String nodeType = config.getType( );

        return getNodeInstance( nodeType ).thenComposeAsync( nodeInstance -> executeNodeWithInstance( nodeInstance, nodeId, config, context ), _orchestrator );
    }

    /**
     * Executes a node with its instance and handles events.
     *
     * @param nodeInstance
     *            the pipeline node instance to execute
     * @param nodeId
     *            the unique identifier of the node
     * @param config
     *            the configuration of the pipeline node
     * @param context
     *            the pipeline execution context
     * @return a CompletableFuture containing the output port name
     */
    private CompletableFuture<String> executeNodeWithInstance( IPipelineNode nodeInstance, String nodeId, PipelineNodeConfig config, PipelineContext context )
    {
        String nodeType = config.getType( );
        String jobId = context.getJobId( );
        int pipelineId = context.getPipelineId( );

        String observabilityNodeId = startNodeObservability( jobId, nodeId, nodeType, context );
        dispatchNodeStartEvent( nodeId, nodeType, jobId, pipelineId );

        return nodeInstance.execute( context, config ).thenComposeAsync( updatedContext -> nodeInstance.determineOutputPort( context, config ), _orchestrator )
                .thenApplyAsync( outputPort -> handleNodeSuccess( nodeId, nodeType, jobId, pipelineId, context, outputPort, observabilityNodeId ),
                        _orchestrator )
                .exceptionally( error -> handleNodeError( nodeId, nodeType, jobId, pipelineId, context, error, observabilityNodeId ) );
    }

    /**
     * Starts observability tracking for a node execution.
     *
     * @param jobId
     *            the job identifier
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param context
     *            the pipeline execution context
     * @return the observability node identifier
     */
    private String startNodeObservability( String jobId, String nodeId, String nodeType, PipelineContext context )
    {
        try
        {
            String observabilityNodeId = "pipeline-node-" + nodeId + "-" + UUID.randomUUID( ).toString( ).substring( 0, 8 );
            int nodeOrder = calculateNodeOrder( context, nodeId );
            String contextObservabilityId = context.getObservabilityId( );

            _observabilityService.startNodeExecution( contextObservabilityId, observabilityNodeId, OBS_NODE_EXECUTION + nodeId, nodeOrder,
                    ObservabilityData.input( OBS_NODE_EXECUTION + nodeId, OBS_NODE_TYPE_INFO + nodeType ) );

            String nodeKey = jobId + "-" + nodeId;
            nodeObservabilityIds.put( nodeKey, observabilityNodeId );
            return observabilityNodeId;
        }
        catch( Exception e )
        {
            AppLogService.error( "Error while starting observability for node {}", nodeId, e );
            return null;
        }
    }

    /**
     * Calculates the execution order of a node within the pipeline.
     *
     * @param context
     *            the pipeline execution context
     * @param nodeId
     *            the unique identifier of the node
     * @return the node execution order
     */
    private int calculateNodeOrder( PipelineContext context, String nodeId )
    {
        return context.getAllOutputs( ).size( ) + 1;
    }

    /**
     * Dispatches the node start event.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     */
    private void dispatchNodeStartEvent( String nodeId, String nodeType, String jobId, int pipelineId )
    {
        _nodeStartedEvent.fire( PipelineNodeStartedEvent.now( jobId, pipelineId, nodeId, nodeType ) );
    }

    /**
     * Handles successful node execution by dispatching success and port events.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param context
     *            the pipeline execution context
     * @param outputPort
     *            the output port name
     * @param observabilityNodeId
     *            the observability node identifier
     * @return the output port name
     */
    private String handleNodeSuccess( String nodeId, String nodeType, String jobId, int pipelineId, PipelineContext context, String outputPort,
            String observabilityNodeId )
    {
        if ( observabilityNodeId != null )
        {
            try
            {
                Map<String, Object> nodeOutput = context.getNodeOutput( nodeId );
                String outputDetails;

                if ( nodeOutput != null && !nodeOutput.isEmpty( ) )
                {
                    try
                    {
                        outputDetails = OBJECT_MAPPER.writeValueAsString( nodeOutput );
                    }
                    catch( Exception jsonEx )
                    {
                        outputDetails = buildOutputInfo( nodeOutput );
                    }
                }
                else
                {
                    outputDetails = "";
                }

                _observabilityService.completeNodeExecutionSuccess( context.getObservabilityId( ), observabilityNodeId,
                        ObservabilityData.output( OBS_NODE_SUCCESS, outputDetails ) );
            }
            catch( Exception e )
            {
                AppLogService.error( "Error while finalizing observability for node {}", nodeId, e );
                _observabilityService.completeNodeExecutionSuccess( context.getObservabilityId( ), observabilityNodeId,
                        ObservabilityData.output( OBS_NODE_SUCCESS, "" ) );
            }
        }

        dispatchNodeSuccessEvent( nodeId, nodeType, jobId, pipelineId, context );
        dispatchPortChosenEvent( nodeId, nodeType, jobId, pipelineId, outputPort );

        String nodeKey = jobId + "-" + nodeId;
        nodeObservabilityIds.remove( nodeKey );

        return outputPort;
    }

    /**
     * Handles node execution errors by completing observability tracking and dispatching error events.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param context
     *            the pipeline execution context
     * @param error
     *            the execution error
     * @param observabilityNodeId
     *            the observability node identifier
     * @return null (required for exceptionally handler)
     */
    private String handleNodeError( String nodeId, String nodeType, String jobId, int pipelineId, PipelineContext context, Throwable error,
            String observabilityNodeId )
    {
        if ( observabilityNodeId != null )
        {
            String errorMessage = error.getMessage( ) != null ? error.getMessage( ) : error.getClass( ).getSimpleName( );
            _observabilityService.completeNodeExecutionError( context.getObservabilityId( ), observabilityNodeId, errorMessage,
                    ObservabilityData.output( OBS_NODE_FAILED + errorMessage, null ) );
        }

        dispatchNodeErrorEvent( nodeId, nodeType, jobId, pipelineId, error );

        String nodeKey = jobId + "-" + nodeId;
        nodeObservabilityIds.remove( nodeKey );

        if ( error instanceof RuntimeException re )
        {
            throw re;
        }
        throw new RuntimeException( error );
    }

    /**
     * Builds a formatted string representation of node output for observability.
     *
     * @param nodeOutput
     *            the node output data
     * @return a formatted string containing output information
     */
    private String buildOutputInfo( Map<String, Object> nodeOutput )
    {
        if ( nodeOutput == null || nodeOutput.isEmpty( ) )
        {
            return "";
        }

        StringBuilder info = new StringBuilder( );
        int count = 0;
        for ( Map.Entry<String, Object> entry : nodeOutput.entrySet( ) )
        {
            if ( count > 0 )
                info.append( ", " );
            if ( count >= 3 )
            {
                info.append( "..." );
                break;
            }

            String key = entry.getKey( );
            Object value = entry.getValue( );
            String valueStr = value != null ? value.toString( ) : "null";
            if ( valueStr.length( ) > 50 )
            {
                valueStr = valueStr.substring( 0, 47 ) + "...";
            }
            info.append( key ).append( "=" ).append( valueStr );
            count++;
        }
        return info.toString( );
    }

    /**
     * Dispatches the node success event.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param context
     *            the pipeline execution context
     */
    private void dispatchNodeSuccessEvent( String nodeId, String nodeType, String jobId, int pipelineId, PipelineContext context )
    {
        Map<String, Object> nodeOutput = context.getNodeOutput( nodeId );
        _nodeCompletedEvent.fire( PipelineNodeCompletedEvent.now( jobId, pipelineId, nodeId, nodeType, nodeOutput ) );
    }

    /**
     * Dispatches the port chosen event.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param outputPort
     *            the output port name
     */
    private void dispatchPortChosenEvent( String nodeId, String nodeType, String jobId, int pipelineId, String outputPort )
    {
        _nodePortChosenEvent.fire( PipelineNodePortChosenEvent.now( jobId, pipelineId, nodeId, nodeType, outputPort ) );
    }

    /**
     * Dispatches the node error event.
     *
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeType
     *            the type of the node
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param error
     *            the execution error
     */
    private void dispatchNodeErrorEvent( String nodeId, String nodeType, String jobId, int pipelineId, Throwable error )
    {
        _nodeFailedEvent.fire( PipelineNodeFailedEvent.now( jobId, pipelineId, nodeId, nodeType, error.getMessage( ) ) );
    }

    /**
     * Retrieves a node instance asynchronously by its type.
     *
     * @param nodeType
     *            the type of the node to retrieve
     * @return a CompletableFuture containing the pipeline node instance
     */
    private CompletableFuture<IPipelineNode> getNodeInstance( String nodeType )
    {
        return _pipelineNodeRegistry.getNodeInstance( nodeType ).map( CompletableFuture::<IPipelineNode> completedFuture )
                .orElseGet( ( ) -> CompletableFuture.failedFuture( new PipelineConfigurationException( NODE_TYPE_NOT_FOUND_MESSAGE + nodeType ) ) );
    }
}
