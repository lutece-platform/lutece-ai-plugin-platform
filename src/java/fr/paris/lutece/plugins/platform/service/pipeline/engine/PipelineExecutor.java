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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import fr.paris.lutece.plugins.platform.service.concurrent.Orchestration;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineStateManager.NodeState;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineEdge;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineDefinition;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl.RetryManagerNode;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Pipeline executor responsible for managing and executing pipeline workflows. Handles pipeline preparation, execution, and cleanup operations.
 */
@ApplicationScoped
@Named( "platform.pipelineExecutor" )
public class PipelineExecutor
{

    private static final String LOG_PIPELINE_CONFIG_ERROR = "Pipeline configuration error for job {}: {}";
    private static final String LOG_UNEXPECTED_ERROR_PREPARATION = "Unexpected error during pipeline preparation for job {}";
    private static final String LOG_CANNOT_START_PIPELINE = "Cannot start pipeline for job {}. No pending pipeline found.";
    private static final String LOG_ERROR_FINAL_RESULT = "Error determining final result for job {}";
    private static final String LOG_UNEXPECTED_ERROR_PROCESSING = "Unexpected error in node processing loop for job {}";
    private static final String LOG_ERROR_STARTING_PIPELINE = "Error starting pipeline for job {}";
    private static final String LOG_SUCCESSOR_NODE_NOT_FOUND = "Error: Successor node config for {} not found. Skipping edge from {}.";

    private static final String ERROR_UNEXPECTED_PREPARATION = "Unexpected error during pipeline preparation";
    private static final String ERROR_NODE_CONFIG_MISSING = "Node configuration missing for ID: ";
    private static final String ERROR_DEPENDENCY_FAILURE = "Cannot execute due to failed dependencies";
    private static final String ERROR_PIPELINE_CANCELLED = "Pipeline execution cancelled";
    private static final String ERROR_FINAL_RESULT = "Error determining final result: ";
    private static final String ERROR_PIPELINE_FAILED_NO_ERRORS = "Pipeline execution failed without specific node errors";
    private static final String ERROR_PIPELINE_FAILED_WITH_ERRORS = "Pipeline execution failed with the following node errors:";
    private static final String ERROR_UNKNOWN_PREFIX = "Unknown error: ";
    private static final String ERROR_PIPELINE_EXECUTION_FAILED = "Pipeline execution failed: ";

    @Inject
    @Orchestration
    private ManagedExecutorService _orchestrator;

    @Inject
    private PipelineNodeExecutionManager _nodeExecutionManager;

    private final Map<String, Map<String, String>> jobNodeErrors = new ConcurrentHashMap<>( );
    private final Map<String, PendingPipeline> pendingPipelines = new ConcurrentHashMap<>( );
    private final Map<String, ActiveExecution> activeExecutions = new ConcurrentHashMap<>( );

    /**
     * Per-job execution state used by the event-driven processing loop.
     */
    private static class ActiveExecution
    {
        final PipelineComponents components;
        final CompletableFuture<Void> processingFuture;

        /**
         * Constructor.
         *
         * @param components
         *            the pipeline components for the job
         * @param processingFuture
         *            the future signalling end of node processing
         */
        ActiveExecution( PipelineComponents components, CompletableFuture<Void> processingFuture )
        {
            this.components = components;
            this.processingFuture = processingFuture;
        }
    }

    /**
     * Internal class representing a pending pipeline execution.
     */
    private static class PendingPipeline
    {
        final PipelineDefinition definition;
        final Map<String, Object> initialInputs;
        final CompletableFuture<Map<String, Object>> resultFuture;
        final int pipelineId;
        final String observabilityId;

        PendingPipeline( int pipelineId, String observabilityId, PipelineDefinition definition, Map<String, Object> initialInputs,
                CompletableFuture<Map<String, Object>> resultFuture )
        {
            this.definition = definition;
            this.initialInputs = initialInputs != null ? new ConcurrentHashMap<>( initialInputs ) : new ConcurrentHashMap<>( );
            this.resultFuture = resultFuture;
            this.pipelineId = pipelineId;
            this.observabilityId = observabilityId;
        }
    }

    /**
     * Prepares a pipeline for execution by validating configuration and creating a pending pipeline.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param pipelineId
     *            The pipeline identifier
     * @param observabilityId
     *            The observability execution identifier
     * @param definition
     *            The pipeline definition containing node configurations
     * @param initialInputs
     *            The initial input data for the pipeline
     * @return A CompletableFuture that will contain the pipeline execution results
     */
    public CompletableFuture<Map<String, Object>> preparePipeline( String jobId, int pipelineId, String observabilityId, PipelineDefinition definition,
            Map<String, Object> initialInputs )
    {
        try
        {
            validatePipelineConfiguration( definition );
            CompletableFuture<Map<String, Object>> pipelineFuture = new CompletableFuture<>( );
            pendingPipelines.put( jobId, new PendingPipeline( pipelineId, observabilityId, definition, initialInputs, pipelineFuture ) );
            return pipelineFuture;
        }
        catch( PipelineConfigurationException e )
        {
            AppLogService.error( LOG_PIPELINE_CONFIG_ERROR, jobId, e.getMessage( ), e );
            return PipelineErrorHandler.completeExceptionally( e );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_UNEXPECTED_ERROR_PREPARATION, jobId, e );
            return PipelineErrorHandler.completeExceptionally( new PipelineExecutionException( ERROR_UNEXPECTED_PREPARATION, e ) );
        }
    }

    /**
     * Starts the execution of a prepared pipeline.
     *
     * @param jobId
     *            The unique identifier for the job
     */
    public void startPipeline( String jobId )
    {
        PendingPipeline pendingPipeline = pendingPipelines.get( jobId );
        if ( pendingPipeline == null )
        {
            AppLogService.error( LOG_CANNOT_START_PIPELINE, jobId );
            return;
        }

        try
        {
            initializePipelineExecution( jobId, pendingPipeline );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_STARTING_PIPELINE, jobId, e );
            pendingPipeline.resultFuture.completeExceptionally( e );
            cleanupPipeline( jobId );
        }
    }

    /**
     * Cancels a pipeline execution and cleans up resources.
     *
     * @param jobId
     *            The unique identifier for the job to cancel
     */
    public void cancelPipeline( String jobId )
    {
        PendingPipeline pendingPipeline = pendingPipelines.remove( jobId );
        if ( pendingPipeline != null && !pendingPipeline.resultFuture.isDone( ) )
        {
            pendingPipeline.resultFuture.completeExceptionally( new PipelineExecutionException( ERROR_PIPELINE_CANCELLED ) );
        }
        jobNodeErrors.remove( jobId );
    }

    /**
     * Validates the pipeline configuration by creating a pipeline graph.
     *
     * @param definition
     *            The pipeline definition to validate
     * @throws PipelineConfigurationException
     *             If the configuration is invalid
     */
    private void validatePipelineConfiguration( PipelineDefinition definition ) throws PipelineConfigurationException
    {
        new PipelineGraph( definition );
    }

    /**
     * Initializes the pipeline execution by setting up all necessary components.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param pendingPipeline
     *            The pending pipeline to initialize
     */
    private void initializePipelineExecution( String jobId, PendingPipeline pendingPipeline )
    {
        initializeJobErrorTracking( jobId );

        PipelineDefinition definition = pendingPipeline.definition;
        Map<String, Object> initialInputs = pendingPipeline.initialInputs;
        int pipelineId = pendingPipeline.pipelineId;
        String observabilityId = pendingPipeline.observabilityId;
        CompletableFuture<Map<String, Object>> pipelineFuture = pendingPipeline.resultFuture;

        PipelineComponents components = createPipelineComponents( jobId, pipelineId, observabilityId, definition, initialInputs );

        executeAsyncPipeline( jobId, components, pipelineFuture );
    }

    /**
     * Initializes error tracking for a job.
     *
     * @param jobId
     *            The unique identifier for the job
     */
    private void initializeJobErrorTracking( String jobId )
    {
        jobNodeErrors.put( jobId, new ConcurrentHashMap<>( ) );
    }

    /**
     * Creates all necessary pipeline components for execution.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param pipelineId
     *            The pipeline identifier
     * @param observabilityId
     *            The observability execution identifier
     * @param definition
     *            The pipeline definition
     * @param initialInputs
     *            The initial input data
     * @return A container with all pipeline components
     */
    private PipelineComponents createPipelineComponents( String jobId, int pipelineId, String observabilityId, PipelineDefinition definition,
            Map<String, Object> initialInputs )
    {
        PipelineGraph graph = new PipelineGraph( definition );
        PipelineContext context = new PipelineContext( jobId, pipelineId, observabilityId, initialInputs );
        PipelineStateManager stateManager = new PipelineStateManager( definition.getNodes( ).keySet( ) );
        PipelineNodeExecutionManager nodeExecutor = _nodeExecutionManager;
        PipelineResultResolver resultResolver = new PipelineResultResolver( );

        String rootNodeId = graph.getRootNodeId( );
        stateManager.markNodeAsReady( rootNodeId );

        return new PipelineComponents( graph, context, stateManager, nodeExecutor, resultResolver );
    }

    /**
     * Container class for pipeline components.
     */
    private static class PipelineComponents
    {
        final PipelineGraph graph;
        final PipelineContext context;
        final PipelineStateManager stateManager;
        final PipelineNodeExecutionManager nodeExecutor;
        final PipelineResultResolver resultResolver;

        PipelineComponents( PipelineGraph graph, PipelineContext context, PipelineStateManager stateManager, PipelineNodeExecutionManager nodeExecutor,
                PipelineResultResolver resultResolver )
        {
            this.graph = graph;
            this.context = context;
            this.stateManager = stateManager;
            this.nodeExecutor = nodeExecutor;
            this.resultResolver = resultResolver;
        }
    }

    /**
     * Executes the pipeline asynchronously.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param components
     *            The pipeline components
     * @param pipelineFuture
     *            The future to complete with results
     */
    private void executeAsyncPipeline( String jobId, PipelineComponents components, CompletableFuture<Map<String, Object>> pipelineFuture )
    {
        CompletableFuture.runAsync( ( ) -> {
            try
            {
                processNodes( jobId, components ).whenComplete( ( result, error ) -> handlePipelineCompletion( jobId, error, components, pipelineFuture ) );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_UNEXPECTED_ERROR_PROCESSING, jobId, e );
                pipelineFuture.completeExceptionally( e );
                cleanupPipeline( jobId );
            }
        }, _orchestrator );
    }

    /**
     * Handles the completion of pipeline execution.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param error
     *            Any error that occurred during execution
     * @param components
     *            The pipeline components
     * @param pipelineFuture
     *            The future to complete with results
     */
    private void handlePipelineCompletion( String jobId, Throwable error, PipelineComponents components, CompletableFuture<Map<String, Object>> pipelineFuture )
    {
        if ( error != null )
        {
            handlePipelineError( jobId, error, pipelineFuture );
        }
        else
        {
            handlePipelineSuccess( jobId, components, pipelineFuture );
        }
        cleanupPipeline( jobId );
    }

    /**
     * Handles pipeline execution error.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param error
     *            The error that occurred
     * @param pipelineFuture
     *            The future to complete exceptionally
     */
    private void handlePipelineError( String jobId, Throwable error, CompletableFuture<Map<String, Object>> pipelineFuture )
    {
        String detailedErrorMessage = getDetailedErrorMessage( jobId, error );
        PipelineExecutionException pipelineError = new PipelineExecutionException( detailedErrorMessage, error );
        pipelineFuture.completeExceptionally( pipelineError );
    }

    /**
     * Handles successful pipeline execution.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param components
     *            The pipeline components
     * @param pipelineFuture
     *            The future to complete with results
     */
    private void handlePipelineSuccess( String jobId, PipelineComponents components, CompletableFuture<Map<String, Object>> pipelineFuture )
    {
        try
        {
            Map<String, Object> finalResult = components.resultResolver.determineFinalResult( components.graph, components.context,
                    components.stateManager.getNodeStates( ) );
            pipelineFuture.complete( finalResult );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_FINAL_RESULT, jobId, e );
            String errorMsg = ERROR_FINAL_RESULT + e.getMessage( );
            pipelineFuture.completeExceptionally( new PipelineExecutionException( errorMsg, e ) );
        }
    }

    /**
     * Processes all nodes in the pipeline using an event-driven model — initial READY nodes are drained, then each node completion schedules the next ready
     * nodes via {@link #onNodeStateChanged(String)}. No polling.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param components
     *            The pipeline components
     * @return A CompletableFuture that completes when all nodes are processed
     */
    private CompletableFuture<Void> processNodes( String jobId, PipelineComponents components )
    {
        CompletableFuture<Void> processingFuture = new CompletableFuture<>( );
        activeExecutions.put( jobId, new ActiveExecution( components, processingFuture ) );
        drainReadyNodes( jobId );
        return processingFuture;
    }

    /**
     * Drains all currently READY nodes for the job and dispatches them for execution. Then, if neither active futures nor ready nodes remain, completes the
     * processing future.
     *
     * @param jobId
     *            the unique job identifier
     */
    private void drainReadyNodes( String jobId )
    {
        ActiveExecution execution = activeExecutions.get( jobId );
        if ( execution == null || execution.processingFuture.isDone( ) )
        {
            return;
        }

        PipelineComponents components = execution.components;
        String nodeId;
        while ( ( nodeId = components.stateManager.pollReadyNode( ) ) != null )
        {
            processReadyNode( jobId, nodeId, components, execution.processingFuture );
        }

        if ( !components.stateManager.hasActiveFutures( ) && !components.stateManager.hasReadyNodes( ) )
        {
            handleNoMoreNodes( jobId, execution.processingFuture );
        }
    }

    /**
     * Re-evaluates the per-job ready queue. Called after every node completion / failure to propagate the event-driven progression of the pipeline.
     *
     * @param jobId
     *            the unique job identifier
     */
    private void onNodeStateChanged( String jobId )
    {
        drainReadyNodes( jobId );
    }

    /**
     * Processes a ready node for execution.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the node to process
     * @param components
     *            The pipeline components
     * @param processingFuture
     *            The future to complete when processing is done
     */
    private void processReadyNode( String jobId, String nodeId, PipelineComponents components, CompletableFuture<Void> processingFuture )
    {
        if ( components.stateManager.markNodeAsRunning( nodeId ) )
        {
            PipelineNodeConfig nodeConfig = components.graph.getNodeConfig( nodeId );
            executeNodeAndHandleOutput( jobId, nodeId, nodeConfig, components ).exceptionally( error -> {
                if ( !processingFuture.isDone( ) )
                {
                    checkUnprocessableNodes( jobId, components.graph, components.stateManager );
                    processingFuture.completeExceptionally( error );
                }
                return null;
            } );
        }
    }

    /**
     * Handles the case when no more nodes are available for processing.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param processingFuture
     *            The future to complete
     */
    private void handleNoMoreNodes( String jobId, CompletableFuture<Void> processingFuture )
    {
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors != null && !errors.isEmpty( ) )
        {
            String errorMsg = getFailedNodesErrorMessage( jobId );
            processingFuture.completeExceptionally( new PipelineExecutionException( errorMsg ) );
        }
        else
        {
            processingFuture.complete( null );
        }
    }

    /**
     * Checks for nodes that cannot be processed due to failed dependencies.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param graph
     *            The pipeline graph
     * @param stateManager
     *            The state manager
     */
    private void checkUnprocessableNodes( String jobId, PipelineGraph graph, PipelineStateManager stateManager )
    {
        Map<String, NodeState> nodeStates = stateManager.getNodeStates( );
        Set<String> completedOrFailedNodes = getCompletedOrFailedNodes( nodeStates );

        nodeStates.entrySet( ).stream( ).filter( entry -> !isNodeCompletedOrFailed( entry.getValue( ) ) )
                .filter( entry -> !graph.areAllDependenciesMet( entry.getKey( ), completedOrFailedNodes ) )
                .forEach( entry -> markNodeAsUnprocessable( jobId, entry.getKey( ), stateManager ) );
    }

    /**
     * Gets the set of completed or failed nodes.
     *
     * @param nodeStates
     *            The current node states
     * @return A set of node IDs that are completed or failed
     */
    private Set<String> getCompletedOrFailedNodes( Map<String, NodeState> nodeStates )
    {
        return nodeStates.entrySet( ).stream( ).filter( e -> e.getValue( ) == NodeState.COMPLETED || e.getValue( ) == NodeState.FAILED )
                .map( Map.Entry::getKey ).collect( Collectors.toSet( ) );
    }

    /**
     * Checks if a node state is completed or failed.
     *
     * @param nodeState
     *            The node state to check
     * @return true if the node is completed or failed
     */
    private boolean isNodeCompletedOrFailed( NodeState nodeState )
    {
        return nodeState == NodeState.COMPLETED || nodeState == NodeState.FAILED;
    }

    /**
     * Marks a node as unprocessable due to dependency failures.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the node to mark
     * @param stateManager
     *            The state manager
     */
    private void markNodeAsUnprocessable( String jobId, String nodeId, PipelineStateManager stateManager )
    {
        stateManager.markNodeAsFailed( nodeId );
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors != null )
        {
            errors.put( nodeId, ERROR_DEPENDENCY_FAILURE );
        }
    }

    /**
     * Executes a node and handles its output.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the node to execute
     * @param nodeConfig
     *            The node configuration
     * @param components
     *            The pipeline components
     * @return A CompletableFuture that completes when the node execution is handled
     */
    private CompletableFuture<Void> executeNodeAndHandleOutput( String jobId, String nodeId, PipelineNodeConfig nodeConfig, PipelineComponents components )
    {
        if ( nodeConfig == null )
        {
            return handleMissingNodeConfig( jobId, nodeId, components.stateManager );
        }

        CompletableFuture<Void> nodeFuture = components.nodeExecutor.executeNode( nodeId, nodeConfig, components.context )
                .thenAcceptAsync( outputPort -> handleNodeSuccess( jobId, nodeId, nodeConfig, outputPort, components ), _orchestrator )
                .exceptionally( error -> handleNodeFailure( jobId, nodeId, nodeConfig, error, components.stateManager ) );

        components.stateManager.registerNodeFuture( nodeId, nodeFuture );
        nodeFuture.whenComplete( ( result, error ) -> onNodeStateChanged( jobId ) );
        return nodeFuture;
    }

    /**
     * Handles the case when a node configuration is missing.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the missing node
     * @param stateManager
     *            The state manager
     * @return A completed CompletableFuture
     */
    private CompletableFuture<Void> handleMissingNodeConfig( String jobId, String nodeId, PipelineStateManager stateManager )
    {
        String errorMsg = ERROR_NODE_CONFIG_MISSING + nodeId;
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors != null )
        {
            errors.put( nodeId, errorMsg );
        }
        PipelineErrorHandler.handleNodeError( nodeId, "UNKNOWN", new PipelineConfigurationException( errorMsg ), stateManager );
        return CompletableFuture.completedFuture( null );
    }

    /**
     * Handles successful node execution.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the successful node
     * @param nodeConfig
     *            The node configuration
     * @param outputPort
     *            The output port of the node
     * @param components
     *            The pipeline components
     */
    private void handleNodeSuccess( String jobId, String nodeId, PipelineNodeConfig nodeConfig, String outputPort, PipelineComponents components )
    {
        components.stateManager.markNodeAsCompleted( nodeId );
        activateSuccessorNodes( jobId, nodeId, nodeConfig, outputPort, components );
    }

    /**
     * Handles node execution failure.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the failed node
     * @param nodeConfig
     *            The node configuration
     * @param error
     *            The error that occurred
     * @param stateManager
     *            The state manager
     * @return null (required by exceptionally callback)
     */
    private Void handleNodeFailure( String jobId, String nodeId, PipelineNodeConfig nodeConfig, Throwable error, PipelineStateManager stateManager )
    {
        String errorMsg = extractErrorMessage( error );
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors != null )
        {
            errors.put( nodeId, errorMsg );
        }
        PipelineErrorHandler.handleNodeError( nodeId, nodeConfig.getType( ), error, stateManager );
        return null;
    }

    /**
     * Activates successor nodes based on the output of a completed node.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param nodeId
     *            The identifier of the completed node
     * @param nodeConfig
     *            The node configuration
     * @param outputPort
     *            The output port of the node
     * @param components
     *            The pipeline components
     */
    private void activateSuccessorNodes( String jobId, String nodeId, PipelineNodeConfig nodeConfig, String outputPort, PipelineComponents components )
    {
        List<PipelineEdge> relevantEdges = components.graph.getOutgoingEdges( nodeId, outputPort );
        for ( PipelineEdge edge : relevantEdges )
        {
            processSuccessorEdge( jobId, nodeId, nodeConfig, edge, components );
        }
    }

    /**
     * Processes a successor edge to potentially activate the target node.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param sourceNodeId
     *            The identifier of the source node
     * @param sourceNodeConfig
     *            The source node configuration
     * @param edge
     *            The edge to process
     * @param components
     *            The pipeline components
     */
    private void processSuccessorEdge( String jobId, String sourceNodeId, PipelineNodeConfig sourceNodeConfig, PipelineEdge edge,
            PipelineComponents components )
    {
        String targetNodeId = edge.getTarget( );
        PipelineNodeConfig targetNodeConfig = components.graph.getNodeConfig( targetNodeId );

        if ( targetNodeConfig == null )
        {
            AppLogService.error( LOG_SUCCESSOR_NODE_NOT_FOUND, targetNodeId, sourceNodeId );
            return;
        }

        evaluateNodeActivation( jobId, sourceNodeId, sourceNodeConfig, edge, targetNodeId, targetNodeConfig, components );
    }

    /**
     * Evaluates whether a target node should be activated.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param sourceNodeId
     *            The identifier of the source node
     * @param sourceNodeConfig
     *            The source node configuration
     * @param edge
     *            The edge connecting the nodes
     * @param targetNodeId
     *            The identifier of the target node
     * @param targetNodeConfig
     *            The target node configuration
     * @param components
     *            The pipeline components
     */
    private void evaluateNodeActivation( String jobId, String sourceNodeId, PipelineNodeConfig sourceNodeConfig, PipelineEdge edge, String targetNodeId,
            PipelineNodeConfig targetNodeConfig, PipelineComponents components )
    {
        PipelineStateManager.NodeState targetState = components.stateManager.getNodeState( targetNodeId );
        String targetNodeType = targetNodeConfig.getType( );

        boolean isRetryMgrLoopReentry = isRetryManagerLoopReentry( sourceNodeConfig, edge );
        boolean requiresReactivation = shouldReactivateNode( components.stateManager, targetNodeId, targetNodeType );

        if ( isRetryMgrLoopReentry )
        {
            handleLoopReentry( targetNodeId, components.graph, components.stateManager );
        }
        else if ( targetState == PipelineStateManager.NodeState.NOT_STARTED || requiresReactivation )
        {
            attemptNodeActivation( jobId, sourceNodeId, targetNodeId, components.graph, components.stateManager, requiresReactivation );
        }
    }

    /**
     * Checks if this is a retry manager loop reentry.
     *
     * @param sourceNodeConfig
     *            The source node configuration
     * @param edge
     *            The edge being processed
     * @return true if this is a retry manager loop reentry
     */
    private boolean isRetryManagerLoopReentry( PipelineNodeConfig sourceNodeConfig, PipelineEdge edge )
    {
        return sourceNodeConfig.getType( ).equals( PipelineConstants.NODE_TYPE_RETRY_MANAGER )
                && edge.getSourcePort( ).equals( RetryManagerNode.RETRY_OUTPUT_PORT );
    }

    /**
     * Determines if a node should be reactivated.
     *
     * @param stateManager
     *            The state manager
     * @param targetNodeId
     *            The target node ID
     * @param targetNodeType
     *            The target node type
     * @return true if the node should be reactivated
     */
    private boolean shouldReactivateNode( PipelineStateManager stateManager, String targetNodeId, String targetNodeType )
    {
        return stateManager.isNodeCompleted( targetNodeId )
                && ( targetNodeType.equals( PipelineConstants.NODE_TYPE_RETRY_MANAGER ) || targetNodeType.equals( PipelineConstants.NODE_TYPE_CONDITION ) );
    }

    /**
     * Attempts to activate a node if all dependencies are met.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param sourceNodeId
     *            The identifier of the source node
     * @param targetNodeId
     *            The identifier of the target node
     * @param graph
     *            The pipeline graph
     * @param stateManager
     *            The state manager
     * @param requiresReactivation
     *            Whether the node requires reactivation
     */
    private void attemptNodeActivation( String jobId, String sourceNodeId, String targetNodeId, PipelineGraph graph, PipelineStateManager stateManager,
            boolean requiresReactivation )
    {
        Set<String> completedNodes = stateManager.getCompletedNodes( );
        boolean allDependenciesMet = graph.areAllDependenciesMet( targetNodeId, completedNodes );

        if ( allDependenciesMet )
        {
            stateManager.markNodeAsReady( targetNodeId );
        }
    }

    /**
     * Handles loop reentry for retry scenarios.
     *
     * @param targetNodeId
     *            The identifier of the target node
     * @param graph
     *            The pipeline graph
     * @param stateManager
     *            The state manager
     */
    private void handleLoopReentry( String targetNodeId, PipelineGraph graph, PipelineStateManager stateManager )
    {
        stateManager.markNodeAsReady( targetNodeId );
        List<PipelineEdge> downstreamEdges = graph.getOutgoingEdges( targetNodeId );

        for ( PipelineEdge downstreamEdge : downstreamEdges )
        {
            String downstreamNodeId = downstreamEdge.getTarget( );
            if ( stateManager.getNodeState( downstreamNodeId ) != PipelineStateManager.NodeState.NOT_STARTED )
            {
                stateManager.resetNodeState( downstreamNodeId );
            }
        }
    }

    /**
     * Extracts a meaningful error message from a throwable.
     *
     * @param error
     *            The throwable to extract message from
     * @return The extracted error message
     */
    private String extractErrorMessage( Throwable error )
    {
        Throwable cause = ( error instanceof CompletionException && error.getCause( ) != null ) ? error.getCause( ) : error;
        String message = cause.getMessage( );
        if ( message == null || message.isEmpty( ) )
        {
            message = cause.getClass( ).getSimpleName( );
        }
        return message;
    }

    /**
     * Gets a detailed error message for failed nodes.
     *
     * @param jobId
     *            The unique identifier for the job
     * @return The detailed error message
     */
    private String getFailedNodesErrorMessage( String jobId )
    {
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors == null || errors.isEmpty( ) )
        {
            return ERROR_PIPELINE_FAILED_NO_ERRORS;
        }

        StringBuilder errorBuilder = new StringBuilder( ERROR_PIPELINE_FAILED_WITH_ERRORS );
        for ( Map.Entry<String, String> entry : errors.entrySet( ) )
        {
            errorBuilder.append( "\n- Node '" ).append( entry.getKey( ) ).append( "': " ).append( entry.getValue( ) );
        }
        return errorBuilder.toString( );
    }

    /**
     * Gets a detailed error message for pipeline execution failures.
     *
     * @param jobId
     *            The unique identifier for the job
     * @param error
     *            The error that occurred
     * @return The detailed error message
     */
    private String getDetailedErrorMessage( String jobId, Throwable error )
    {
        Map<String, String> errors = jobNodeErrors.get( jobId );
        if ( errors != null && !errors.isEmpty( ) )
        {
            return getFailedNodesErrorMessage( jobId );
        }

        Throwable cause = ( error instanceof CompletionException && error.getCause( ) != null ) ? error.getCause( ) : error;
        String message = cause.getMessage( );
        if ( message == null || message.isEmpty( ) )
        {
            message = ERROR_UNKNOWN_PREFIX + cause.getClass( ).getSimpleName( );
        }
        return ERROR_PIPELINE_EXECUTION_FAILED + message;
    }

    /**
     * Cleans up resources associated with a completed or cancelled pipeline.
     *
     * @param jobId
     *            The unique identifier for the job to clean up
     */
    private void cleanupPipeline( String jobId )
    {
        jobNodeErrors.remove( jobId );
        pendingPipelines.remove( jobId );
        activeExecutions.remove( jobId );
    }

    /**
     * Retrieves the observability ID for a given execution.
     *
     * @param executionId
     *            The execution identifier
     * @return The observability ID or null if not found
     */
    public String getObservabilityId( String executionId )
    {
        PendingPipeline pending = pendingPipelines.get( executionId );
        return pending != null ? pending.observabilityId : null;
    }
}
