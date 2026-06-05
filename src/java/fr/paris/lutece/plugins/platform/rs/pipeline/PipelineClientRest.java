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
package fr.paris.lutece.plugins.platform.rs.pipeline;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineResponseDTO;
import fr.paris.lutece.plugins.platform.service.security.PlatformSecurityException;
import fr.paris.lutece.plugins.platform.service.security.RateLimitResult;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVariableDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.rs.util.SseRestUtils;
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.service.sse.PlatformSseStreamManager;
import fr.paris.lutece.plugins.platform.service.pipeline.IPipelineService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineRateLimitService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineVersionService;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineOutputAnalyzer;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineOutputAnalyzer.PipelineOutputAnalysisResult;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableConverter;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineRequestDTO;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Client REST API for pipelines. Thin adapter that delegates to IPipelineService (runtime) and PipelineVersionService (CRUD) and maps service exceptions to
 * HTTP responses.
 */
@ApplicationScoped
@Named( "platform.pipelineClientRest" )
@Path( "platform/agent/api/pipelines" )
public class PipelineClientRest
{
    private static final String EXECUTION_NOT_FOUND_MESSAGE = "Execution not found or access denied.";
    private static final String INVALID_PIPELINE_ID_MESSAGE = "Invalid pipeline identifier.";
    private static final String AUTHENTICATION_FAILED_MESSAGE = "Votre session a expiré. Veuillez vous reconnecter.";
    private static final String STREAM_SETUP_FAILED_MESSAGE = "Impossible d'établir la connexion. Veuillez actualiser la page.";
    private static final String ACCESS_DENIED_EXECUTION_MESSAGE = "Access denied to this execution";
    private static final String USER_ID_BODY_KEY = "user_id";

    private static final String AUTH_FAILED_LOG = "Authentication failed for executionId: ";
    private static final String SSE_SINK_NULL_LOG = "SseEventSink is null for executionId: ";
    private static final String SSE_SETUP_ERROR_LOG = "Error setting up SSE stream for executionId: ";
    private static final String EXECUTION_EVENTS_PATH = "{id}/execution/{executionId}/events";
    private static final String REQUIRED_INPUTS_VERSION_PATH = "/{id}/required-inputs/version/{versionName}";
    private static final String OUTPUTS_PATH = "/{id}/outputs";
    private static final String OUTPUTS_VERSION_PATH = "/{id}/outputs/version/{versionName}";
    private static final String USER_EXECUTIONS_PATH = "/{id}/executions";

    private static final PipelineOutputAnalyzer OUTPUT_ANALYZER = new PipelineOutputAnalyzer( );

    @Context
    private HttpServletRequest _request;

    @Inject
    private IPipelineService _pipelineService;

    @Inject
    private PlatformSseStreamManager _sseStreamManager;

    @Inject
    private AuthenticationService _authenticationService;

    @Inject
    private PipelineRateLimitService _rateLimitService;

    /**
     * Default constructor required by CDI for proxy creation.
     */
    public PipelineClientRest( )
    {
    }

    /**
     * Retrieves all pipelines accessible (by subscription) to the authenticated client.
     *
     * @return Response containing list of pipelines or error
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelines( )
    {
        Client authClient = _authenticationService.authenticateClient( _request, Pipeline.RESOURCE_TYPE, null );
        return success( PipelineVersionService.listAccessiblePipelines( authClient.getId( ) ) );
    }

    /**
     * Executes a pipeline with provided inputs.
     *
     * @param id
     *            the pipeline identifier
     * @param request
     *            the request payload carrying user id and inputs
     * @return Response containing execution details or error
     */
    @POST
    @Path( PipelineRestConstants.EXECUTE_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response executePipeline( @Positive( message = "pipeline id must be positive" ) @PathParam( "id" ) int id,
            @NotNull @Valid PipelineRequestDTO request )
    {
        Client authClient = authenticateClient( id );
        String userId = request.getUserId( );
        RateLimitResult rateLimitResult = _rateLimitService.checkRateLimit( userId, id );
        if ( !rateLimitResult.isAllowed( ) )
        {
            if ( rateLimitResult.isInternalError( ) )
            {
                return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                        .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "INTERNAL_ERROR", rateLimitResult.getErrorMessage( ) ) ) ).build( );
            }
            return Response.status( Response.Status.TOO_MANY_REQUESTS )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "RATE_LIMIT_EXCEEDED", rateLimitResult.getErrorMessage( ) ) ) ).build( );
        }
        request.setPipelineId( id );
        String executionId = _pipelineService.executePipeline( request, authClient.getId( ) );
        return success( new PipelineResponseDTO( executionId, PipelineExecution.STATUS_RUNNING, PipelineRestConstants.EXECUTION_STARTED_MESSAGE ) );
    }

    /**
     * Retrieves execution details for a specific pipeline execution.
     *
     * @param pipelineId
     *            the pipeline identifier
     * @param executionId
     *            the execution identifier
     * @return Response containing execution details or error
     */
    @GET
    @Path( PipelineRestConstants.EXECUTION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getExecution( @PathParam( "id" ) int pipelineId, @PathParam( "executionId" ) String executionId )
    {
        if ( pipelineId <= 0 )
        {
            return error( Response.Status.BAD_REQUEST, INVALID_PIPELINE_ID_MESSAGE );
        }
        Client authClient = authenticateClient( pipelineId );
        PipelineExecution execution = _pipelineService.getExecution( executionId ).orElse( null );
        if ( execution == null || !isExecutionAccessible( execution, pipelineId, authClient ) )
        {
            return error( Response.Status.NOT_FOUND, EXECUTION_NOT_FOUND_MESSAGE );
        }
        return success( PipelineExecutionMapper.toDTO( execution ) );
    }

    /**
     * Retrieves required inputs for the current version of a pipeline.
     *
     * @param id
     *            the pipeline identifier
     * @return Response containing required inputs or error
     */
    @GET
    @Path( PipelineRestConstants.REQUIRED_INPUTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelineRequiredInputs( @PathParam( "id" ) int id )
    {
        authenticateClient( id );
        PipelineVersion current = PipelineVersionService.findCurrentVersion( id );
        return success( PipelineVariableConverter.resolveInputVariables( current ) );
    }

    /**
     * Retrieves required inputs for a specific version of a pipeline.
     *
     * @param id
     *            the pipeline identifier
     * @param versionName
     *            the version name
     * @return Response containing required inputs or error
     */
    @GET
    @Path( REQUIRED_INPUTS_VERSION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelineRequiredInputsByVersion( @PathParam( "id" ) int id, @PathParam( "versionName" ) String versionName )
    {
        authenticateClient( id );
        PipelineVersion version = PipelineVersionService.findVersionByName( id, versionName );
        return success( PipelineVariableConverter.resolveInputVariables( version ) );
    }

    /**
     * Retrieves declared outputs for the current version of a pipeline.
     *
     * @param id
     *            the pipeline identifier
     * @return Response containing declared outputs or error
     */
    @GET
    @Path( OUTPUTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelineOutputs( @PathParam( "id" ) int id )
    {
        authenticateClient( id );
        PipelineVersion current = PipelineVersionService.findCurrentVersion( id );
        return success( convertOutputs( current.getFlow( ) ) );
    }

    /**
     * Retrieves declared outputs for a specific version of a pipeline.
     *
     * @param id
     *            the pipeline identifier
     * @param versionName
     *            the version name
     * @return Response containing declared outputs or error
     */
    @GET
    @Path( OUTPUTS_VERSION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelineOutputsByVersion( @PathParam( "id" ) int id, @PathParam( "versionName" ) String versionName )
    {
        authenticateClient( id );
        PipelineVersion version = PipelineVersionService.findVersionByName( id, versionName );
        return success( convertOutputs( version.getFlow( ) ) );
    }

    /**
     * Retrieves executions for a pipeline filtered by user id.
     *
     * @param id
     *            the pipeline identifier
     * @param requestBody
     *            the request body containing user_id
     * @return Response containing list of executions or error
     */
    @POST
    @Path( USER_EXECUTIONS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getUserExecutions( @PathParam( "id" ) int id, Map<String, String> requestBody )
    {
        authenticateClient( id );
        String userId = requestBody != null ? requestBody.get( USER_ID_BODY_KEY ) : null;
        List<PipelineExecution> executions = PipelineVersionService.listExecutionsByUserAndPipeline( id, userId );
        return success( executions.stream( ).map( PipelineExecutionMapper::toDTO ).toList( ) );
    }

    /**
     * Establishes an SSE connection for pipeline execution events.
     *
     * @param pipelineId
     *            the pipeline identifier
     * @param executionId
     *            the execution identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE context
     */
    @GET
    @Path( EXECUTION_EVENTS_PATH )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void getExecutionEvents( @PathParam( "id" ) int pipelineId, @PathParam( "executionId" ) String executionId, @Context SseEventSink eventSink,
            @Context Sse sse )
    {
        if ( pipelineId <= 0 )
        {
            SseRestUtils.closeEventSinkWithError( eventSink, sse, INVALID_PIPELINE_ID_MESSAGE );
            return;
        }
        Client authClient;
        try
        {
            authClient = authenticateClient( pipelineId );
        }
        catch( PlatformSecurityException e )
        {
            AppLogService.error( "{}{}", AUTH_FAILED_LOG, executionId );
            SseRestUtils.closeEventSinkWithError( eventSink, sse, AUTHENTICATION_FAILED_MESSAGE );
            return;
        }
        if ( eventSink == null )
        {
            AppLogService.error( "{}{}", SSE_SINK_NULL_LOG, executionId );
            return;
        }
        try
        {
            if ( !validateExecutionAccess( executionId, authClient ) )
            {
                SseRestUtils.closeEventSinkWithError( eventSink, sse, ACCESS_DENIED_EXECUTION_MESSAGE );
                return;
            }
            _sseStreamManager.registerSseStream( executionId, Pipeline.RESOURCE_TYPE, eventSink, sse );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", SSE_SETUP_ERROR_LOG, executionId, e );
            SseRestUtils.closeEventSinkWithError( eventSink, sse, STREAM_SETUP_FAILED_MESSAGE );
        }
    }

    /**
     * Authenticates the client for pipeline access.
     *
     * @param pipelineId
     *            the pipeline identifier
     * @return the authentication result
     */
    private Client authenticateClient( int pipelineId )
    {
        return _authenticationService.authenticateClient( _request, Pipeline.RESOURCE_TYPE, String.valueOf( pipelineId ) );
    }

    /**
     * Checks that an execution belongs to the requested pipeline and client.
     *
     * @param execution
     *            the execution
     * @param pipelineId
     *            the pipeline identifier in the URL
     * @param client
     *            the authenticated client
     * @return true when the execution is visible to the client
     */
    private boolean isExecutionAccessible( PipelineExecution execution, int pipelineId, Client client )
    {
        return execution.getIdPipeline( ) == pipelineId && execution.getIdClient( ) == client.getId( );
    }

    /**
     * Analyzes a pipeline flow and converts the declared outputs to their DTO form.
     *
     * @param flow
     *            the pipeline flow JSON
     * @return the declared output variables
     */
    private List<PipelineVariableDTO> convertOutputs( String flow )
    {
        PipelineOutputAnalysisResult analysisResult = OUTPUT_ANALYZER.analyzeRequiredOutputs( flow );
        return PipelineVariableConverter.convertToDTO( analysisResult.getVariables( ) );
    }

    /**
     * Validates that an execution is accessible to the authenticated client.
     *
     * @param executionId
     *            the execution identifier
     * @param client
     *            the authenticated client
     * @return true when the execution is visible to the client, or when it cannot be resolved (handled downstream)
     */
    private boolean validateExecutionAccess( String executionId, Client client )
    {
        return _pipelineService.getExecution( executionId ).map( execution -> execution.getIdClient( ) == client.getId( ) ).orElse( true );
    }

    /**
     * Builds a 200 OK JSON response.
     *
     * @param data
     *            the payload
     * @return the HTTP response
     */
    private Response success( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Builds a JSON error response.
     *
     * @param status
     *            the HTTP status
     * @param message
     *            the message shown to the caller
     * @return the HTTP response
     */
    private Response error( Response.Status status, String message )
    {
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), message ) ) ).build( );
    }
}
