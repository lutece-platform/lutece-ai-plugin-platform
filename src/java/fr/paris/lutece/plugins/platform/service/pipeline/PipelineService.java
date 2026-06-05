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
package fr.paris.lutece.plugins.platform.service.pipeline;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineOutputVariableDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineRequestDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecutionHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersionHome;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineExecutionCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineExecutionFailedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineExecutionStartedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PlatformDomainEvent;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineExecutor;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineNodeRegistry;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineDefinition;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineOutputConverter;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named( "platform.pipelineService" )
public class PipelineService implements IPipelineService
{

    private static final String LOG_INIT_MESSAGE = "Initializing Pipeline service";
    private static final String ERROR_PIPELINE_NOT_FOUND = "Pipeline not found: ";
    private static final String ERROR_VERSION_NOT_FOUND = "Specified version not found: ";
    private static final String ERROR_CURRENT_VERSION_NOT_FOUND = "Current version not found for pipeline: ";
    private static final String ERROR_QUEUE_FULL = "Pipeline job queue full (maxPendingJobs reached) — please retry later";
    private static final String ERROR_EXECUTION_NOT_FOUND = "Unable to find the execution with ID: ";
    private static final String ERROR_PIPELINE_EXECUTION = "Pipeline execution error ";
    private static final String ERROR_PIPELINE_PREPARATION = "Error while preparing the pipeline ";
    private static final String ERROR_SERIALIZATION = "Error while serializing the pipeline results";
    private static final String ERROR_STATUS_UPDATE = "Error while updating the execution status";
    private static final String ERROR_PIPELINE_START = "Error while starting the pipeline for execution ";
    private static final String ERROR_DEFINITION_NOT_FOUND = "Unable to start the pipeline: definition not found for execution ";
    private static final String ERROR_FLOW_VALIDATION = "Error while validating the flow";
    private static final String ERROR_PIPELINE_EXECUTION_FULL = "Error while executing the pipeline: ";
    private static final String ERROR_CANCELLED_BY_USER = "Execution cancelled by the user";
    private static final String CLIENT_LABEL = " (Client: ";
    private static final String CLOSING_PARENTHESIS = ")";
    private static final String COLON_SEPARATOR = ": ";
    private static final String OBS_PIPELINE_EXECUTION_FINISHED = "Pipeline execution completed successfully.";
    private static final String OBS_PIPELINE_EXECUTION_FAILED = "Pipeline execution failed: ";
    private static final String OBS_PIPELINE_CANCELLED = "Pipeline execution cancelled.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    private final Map<String, CompletableFuture<Map<String, Object>>> executions = new ConcurrentHashMap<>( );
    private final Map<String, PipelineDefinition> pendingPipelines = new ConcurrentHashMap<>( );

    @Inject
    private PipelineExecutor pipelineExecutor;

    @Inject
    private PipelineFileService _pipelineFileService;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private PipelineNodeRegistry _nodeRegistry;

    @Inject
    private PipelineJobManager _jobManager;

    @Inject
    private Event<PlatformDomainEvent> _domainEvent;

    /**
     * Default constructor for CDI.
     */
    PipelineService( )
    {
        AppLogService.info( LOG_INIT_MESSAGE );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String executePipeline( int pipelineId, String versionName, int clientId, Map<String, Object> inputs )
    {
        PipelineRequestDTO request = new PipelineRequestDTO( );
        request.setPipelineId( pipelineId );
        request.setVersionName( versionName );
        request.setUserId( "system-" + clientId );

        if ( inputs != null )
        {
            for ( Map.Entry<String, Object> entry : inputs.entrySet( ) )
            {
                request.addInput( entry.getKey( ), entry.getValue( ) );
            }
        }

        return executePipeline( request, clientId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String executePipeline( int pipelineId, int clientId, Map<String, Object> inputs )
    {
        PipelineRequestDTO request = new PipelineRequestDTO( );
        request.setPipelineId( pipelineId );
        request.setUserId( "system-" + clientId );

        if ( inputs != null )
        {
            for ( Map.Entry<String, Object> entry : inputs.entrySet( ) )
            {
                request.addInput( entry.getKey( ), entry.getValue( ) );
            }
        }

        return executePipeline( request, clientId );
    }

    /**
     * Executes a pipeline with a PipelineRequestDTO
     *
     * @param request
     *            the pipeline request containing all execution parameters
     * @param clientId
     *            the client identifier requesting the execution
     * @return the execution identifier
     */
    @Override
    public String executePipeline( PipelineRequestDTO request, int clientId )
    {
        int pipelineId = request.getPipelineId( );
        Pipeline pipeline = PipelineHome.findByPrimaryKey( pipelineId ).orElseThrow( ( ) -> new AppException( ERROR_PIPELINE_NOT_FOUND + pipelineId ) );
        String executionId = UUID.randomUUID( ).toString( );

        Map<String, Object> inputs = new HashMap<>( );
        if ( request.getInputs( ) != null )
        {
            request.getInputs( ).forEach( input -> inputs.put( input.getName( ), input.getValue( ) ) );
        }

        _pipelineFileService.storeFileInputs( inputs );

        try
        {
            if ( PipelineExecutionHome.countByStatus( PipelineExecution.STATUS_PENDING ) >= PipelineJobManager.MAX_PENDING_JOBS )
            {
                throw new RejectedExecutionException( ERROR_QUEUE_FULL );
            }

            PipelineExecution execution = createPipelineExecution( pipelineId, clientId, executionId, inputs, request.getUserId( ) );
            PipelineExecutionHome.create( execution );

            PipelineVersion version;
            if ( request.getVersionName( ) != null && !request.getVersionName( ).trim( ).isEmpty( ) )
            {
                version = getVersionByName( pipelineId, request.getVersionName( ) );
            }
            else
            {
                version = PipelineVersionHome.findCurrentVersion( pipelineId )
                        .orElseThrow( ( ) -> new AppException( ERROR_CURRENT_VERSION_NOT_FOUND + pipelineId ) );
            }

            PipelineDefinition definition = OBJECT_MAPPER.readValue( version.getFlow( ), PipelineDefinition.class );

            return executeWithDefinitionAndDTO( executionId, pipelineId, clientId, definition, inputs, pipeline.getMaxConcurrentWorkers( ), request );
        }
        catch( JsonProcessingException | AppException e )
        {
            handleExecutionError( executionId, pipelineId, clientId, e );
            throw new AppException( ERROR_PIPELINE_EXECUTION_FULL + e.getMessage( ), e );
        }
    }

    /**
     * Starts the pipeline execution for the specified execution ID.
     *
     * @param executionId
     *            The execution identifier
     */
    public void startPipeline( String executionId )
    {
        PipelineDefinition definition = pendingPipelines.get( executionId );
        if ( definition == null )
        {
            AppLogService.error( "{}{}", ERROR_DEFINITION_NOT_FOUND, executionId );
            return;
        }

        try
        {
            PipelineExecutionHome.findByExecutionId( executionId )
                    .ifPresent( execution -> fireDomainEvent( PipelineExecutionStartedEvent.now( executionId, execution.getIdPipeline( ) ) ) );

            pipelineExecutor.startPipeline( executionId );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_PIPELINE_START, executionId, e );
            CompletableFuture<Map<String, Object>> future = executions.get( executionId );
            if ( future != null && !future.isDone( ) )
            {
                future.completeExceptionally( e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PipelineExecution> getExecution( String executionId )
    {
        return PipelineExecutionHome.findByExecutionId( executionId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancelExecution( String executionId )
    {
        CompletableFuture<Map<String, Object>> future = executions.get( executionId );
        if ( future != null && !future.isDone( ) )
        {
            pipelineExecutor.cancelPipeline( executionId );

            String observabilityId = pipelineExecutor.getObservabilityId( executionId );
            if ( observabilityId != null )
            {
                _observabilityService.completeResourceExecutionError( observabilityId, OBS_PIPELINE_CANCELLED,
                        ObservabilityData.of( "PIPELINE_RESULT", "success", false, "message", OBS_PIPELINE_CANCELLED ) );
            }

            updateExecutionStatus( executionId, PipelineExecution.STATUS_FAILED, ERROR_CANCELLED_BY_USER );

            PipelineExecutionHome.findByExecutionId( executionId ).ifPresent(
                    execution -> fireDomainEvent( PipelineExecutionFailedEvent.now( executionId, execution.getIdPipeline( ), ERROR_CANCELLED_BY_USER ) ) );
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getNodeTypes( )
    {
        return _nodeRegistry.getAvailableNodesInfo( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validatePipelineFlow( String flow )
    {
        if ( flow == null )
        {
            return false;
        }
        try
        {
            PipelineDefinition definition = OBJECT_MAPPER.readValue( flow, PipelineDefinition.class );
            return definition != null && definition.getNodes( ) != null && definition.getEdges( ) != null;
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( ERROR_FLOW_VALIDATION, e );
            return false;
        }
    }

    /**
     * Creates a new pipeline execution with the specified parameters.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @param clientId
     *            The client identifier
     * @param executionId
     *            The execution identifier
     * @param inputs
     *            The input parameters
     * @return The created pipeline execution
     * @throws JsonProcessingException
     *             If JSON serialization fails
     */
    private PipelineExecution createPipelineExecution( int pipelineId, int clientId, String executionId, Map<String, Object> inputs, String userId )
            throws JsonProcessingException
    {
        PipelineExecution execution = new PipelineExecution( );
        execution.setIdPipeline( pipelineId );
        execution.setIdClient( clientId );
        execution.setExecutionId( executionId );
        execution.setUserId( userId );
        execution.setCreationDate( new Timestamp( new Date( ).getTime( ) ) );
        execution.setStatus( PipelineExecution.STATUS_PENDING );
        execution.setInputs( OBJECT_MAPPER.writeValueAsString( inputs ) );
        return execution;
    }

    /**
     * Retrieves the pipeline version by name or returns the current version if no name is specified.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @param versionName
     *            The version name or null for current version
     * @return The pipeline version
     */
    private PipelineVersion getVersionByName( int pipelineId, String versionName )
    {
        if ( versionName != null && !versionName.isEmpty( ) )
        {
            return PipelineVersionHome.findByPipelineIdAndVersionName( pipelineId, versionName )
                    .orElseThrow( ( ) -> new AppException( ERROR_VERSION_NOT_FOUND + versionName ) );
        }
        return PipelineVersionHome.findCurrentVersion( pipelineId ).orElseThrow( ( ) -> new AppException( ERROR_CURRENT_VERSION_NOT_FOUND + pipelineId ) );
    }

    /**
     * Executes the pipeline with the specified definition and DTO.
     */
    private String executeWithDefinitionAndDTO( String executionId, int pipelineId, int clientId, PipelineDefinition definition, Map<String, Object> inputs,
            int maxConcurrentWorkers, PipelineRequestDTO requestDTO )
    {
        String observabilityId = startObservabilityExecution( executionId, pipelineId, clientId, inputs, requestDTO );
        pendingPipelines.put( executionId, definition );

        CompletableFuture<Map<String, Object>> future = pipelineExecutor.preparePipeline( executionId, pipelineId, observabilityId, definition,
                inputs != null ? inputs : new HashMap<>( ) );
        executions.put( executionId, future );

        PipelineJobManager.JobInfo jobInfo = _jobManager.createJob( executionId, pipelineId, maxConcurrentWorkers, future );
        String status = jobInfo.getStatus( ) == PipelineJobManager.JobStatus.QUEUED ? PipelineExecution.STATUS_PENDING : PipelineExecution.STATUS_RUNNING;

        updateExecutionStatus( executionId, status, null );

        if ( status.equals( PipelineExecution.STATUS_RUNNING ) )
        {
            startPipeline( executionId );
        }

        setupCompletionHandler( executionId, pipelineId, clientId, future );
        return executionId;
    }

    /**
     * Starts observability execution tracking for the pipeline with the full DTO.
     */
    private String startObservabilityExecution( String executionId, int pipelineId, int clientId, Map<String, Object> inputs, PipelineRequestDTO requestDTO )
    {
        try
        {
            List<Map<String, Object>> inputsList = new ArrayList<>( );
            if ( inputs != null )
            {
                inputs.forEach( ( name, value ) -> {
                    Map<String, Object> inputMap = new HashMap<>( );
                    inputMap.put( name, value );
                    inputsList.add( inputMap );
                } );
            }

            ObservabilityData inputData = ObservabilityData.of( "PIPELINE_EXECUTION", "pipelineId", pipelineId, "versionName", requestDTO.getVersionName( ),
                    "userId", requestDTO.getUserId( ), "inputs", inputsList );

            String observabilityId = _observabilityService.startResourceExecution( Pipeline.RESOURCE_TYPE, String.valueOf( pipelineId ), clientId, inputData );
            return observabilityId;
        }
        catch( Exception e )
        {
            AppLogService.error( "Error while starting observability for pipeline {}", pipelineId, e );
            return null;
        }
    }

    /**
     * Sets up the completion handler for the pipeline execution.
     *
     * @param executionId
     *            The execution identifier
     * @param pipelineId
     *            The pipeline identifier
     * @param clientId
     *            The client identifier
     * @param future
     *            The completion future
     */
    private void setupCompletionHandler( String executionId, int pipelineId, int clientId, CompletableFuture<Map<String, Object>> future )
    {
        future.whenComplete( ( result, error ) -> {
            try
            {
                handleCompletion( executionId, pipelineId, clientId, result, error );
            }
            finally
            {
                executions.remove( executionId );
                pendingPipelines.remove( executionId );
            }
        } );
    }

    /**
     * Handles the completion of a pipeline execution.
     *
     * @param executionId
     *            The execution identifier
     * @param pipelineId
     *            The pipeline identifier
     * @param clientId
     *            The client identifier
     * @param result
     *            The execution result
     * @param error
     *            The execution error or null if successful
     */
    private void handleCompletion( String executionId, int pipelineId, int clientId, Map<String, Object> result, Throwable error )
    {
        try
        {
            PipelineExecution updatedExecution = PipelineExecutionHome.findByExecutionId( executionId ).orElse( null );
            if ( updatedExecution == null )
            {
                AppLogService.error( "{}{}", ERROR_EXECUTION_NOT_FOUND, executionId );
                return;
            }

            updatedExecution.setCompletionDate( new Timestamp( new Date( ).getTime( ) ) );

            String observabilityId = pipelineExecutor.getObservabilityId( executionId );

            if ( error != null )
            {
                handleExecutionFailure( updatedExecution, executionId, pipelineId, clientId, error );
                if ( observabilityId != null )
                {
                    _observabilityService.completeResourceExecutionError( observabilityId, OBS_PIPELINE_EXECUTION_FAILED + error.getMessage( ),
                            ObservabilityData.of( "PIPELINE_RESULT", "success", false, "message", OBS_PIPELINE_EXECUTION_FAILED + error.getMessage( ) ) );
                }
            }
            else
            {
                handleExecutionSuccess( updatedExecution, executionId, pipelineId, result );
                if ( observabilityId != null )
                {
                    _observabilityService.completeResourceExecutionSuccess( observabilityId,
                            ObservabilityData.of( "PIPELINE_RESULT", "success", true, "message", OBS_PIPELINE_EXECUTION_FINISHED ) );
                }
            }

            PipelineExecutionHome.update( updatedExecution );
            _jobManager.activateNextQueued( );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( ERROR_SERIALIZATION, e );
        }
    }

    /**
     * Handles successful pipeline execution completion.
     *
     * @param execution
     *            The pipeline execution
     * @param executionId
     *            The execution identifier
     * @param pipelineId
     *            The pipeline identifier
     * @param result
     *            The execution result
     * @throws JsonProcessingException
     *             If JSON serialization fails
     */
    private void handleExecutionSuccess( PipelineExecution execution, String executionId, int pipelineId, Map<String, Object> result )
            throws JsonProcessingException
    {
        execution.setStatus( PipelineExecution.STATUS_COMPLETED );

        List<PipelineOutputVariableDTO> outputsList = PipelineOutputConverter.convertMapToDTO( result );
        execution.setOutputs( OBJECT_MAPPER.writeValueAsString( outputsList ) );

        fireDomainEvent( PipelineExecutionCompletedEvent.now( executionId, pipelineId, result ) );
    }

    /**
     * Handles failed pipeline execution completion.
     *
     * @param execution
     *            The pipeline execution
     * @param executionId
     *            The execution identifier
     * @param pipelineId
     *            The pipeline identifier
     * @param clientId
     *            The client identifier
     * @param error
     *            The execution error
     */
    private void handleExecutionFailure( PipelineExecution execution, String executionId, int pipelineId, int clientId, Throwable error )
    {
        execution.setStatus( PipelineExecution.STATUS_FAILED );
        execution.setError( error.getMessage( ) );

        AppLogService.error( "{}{}{}{}{}{}{}", ERROR_PIPELINE_EXECUTION, pipelineId, CLIENT_LABEL, clientId, CLOSING_PARENTHESIS, COLON_SEPARATOR,
                error.getMessage( ), error );

        fireDomainEvent( PipelineExecutionFailedEvent.now( executionId, pipelineId, error.getMessage( ) ) );
    }

    /**
     * Updates the execution status in the database.
     *
     * @param executionId
     *            The execution identifier
     * @param status
     *            The new status
     * @param errorMessage
     *            The error message or null
     */
    private void updateExecutionStatus( String executionId, String status, String errorMessage )
    {
        try
        {
            PipelineExecution execution = PipelineExecutionHome.findByExecutionId( executionId ).orElse( null );
            if ( execution != null )
            {
                execution.setStatus( status );
                if ( errorMessage != null )
                {
                    execution.setCompletionDate( new Timestamp( new Date( ).getTime( ) ) );
                    execution.setError( errorMessage );
                }
                PipelineExecutionHome.update( execution );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_STATUS_UPDATE, e );
        }
    }

    /**
     * Handles pipeline execution errors by logging and updating status.
     *
     * @param executionId
     *            The execution identifier
     * @param pipelineId
     *            The pipeline identifier
     * @param clientId
     *            The client identifier
     * @param error
     *            The execution error
     */
    private void handleExecutionError( String executionId, int pipelineId, int clientId, Exception error )
    {
        AppLogService.error( "{}{}{}{}{}", ERROR_PIPELINE_PREPARATION, pipelineId, CLIENT_LABEL, clientId, CLOSING_PARENTHESIS, error );

        String observabilityId = pipelineExecutor.getObservabilityId( executionId );
        if ( observabilityId != null )
        {
            _observabilityService.completeResourceExecutionError( observabilityId, ERROR_PIPELINE_PREPARATION + error.getMessage( ),
                    ObservabilityData.of( "PIPELINE_RESULT", "success", false, "message", ERROR_PIPELINE_PREPARATION + error.getMessage( ) ) );
        }

        updateExecutionStatus( executionId, PipelineExecution.STATUS_FAILED, error.getMessage( ) );

        fireDomainEvent( PipelineExecutionFailedEvent.now( executionId, pipelineId, error.getMessage( ) ) );
    }

    /**
     * Fires a typed domain event via the injected CDI Event dispatcher.
     *
     * @param event
     *            the typed event to fire
     */
    private void fireDomainEvent( PlatformDomainEvent event )
    {
        _domainEvent.fire( event );
    }
}
