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

import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineResponseDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineRequestDTO;
import fr.paris.lutece.plugins.platform.rs.util.SseRestUtils;
import fr.paris.lutece.plugins.platform.service.sse.PlatformSseStreamManager;
import fr.paris.lutece.plugins.platform.service.pipeline.IPipelineService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineVersionService;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableConverter;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Admin REST API for pipelines. Thin adapter that delegates to IPipelineService (runtime) and PipelineVersionService (CRUD) and maps service exceptions to HTTP
 * responses.
 */
@ApplicationScoped
@Named( "platform.pipelineAdminRest" )
@Path( "platform/agent/admin/pipelines" )
public class PipelineAdminRest
{
    private static final String EXECUTION_NOT_FOUND_MESSAGE = "Execution not found";
    private static final String EXECUTION_CANCEL_FAILED_MESSAGE = "Unable to cancel the execution";
    private static final String USER_NOT_AUTHENTICATED_MESSAGE = "User not authenticated";
    private static final String CANCELLED_STATUS = "cancelled";
    private static final String EXECUTION_CANCELLED_MESSAGE = "Execution cancelled successfully";

    private static final String AUTH_FAILED_LOG = "Authentication failed for executionId: ";
    private static final String SSE_SINK_NULL_LOG = "SseEventSink is null for executionId: ";
    private static final String SSE_SETUP_ERROR_LOG = "Error setting up SSE stream for executionId: ";

    private static final String PIPELINE_ID_PATH = "/{id}";
    private static final String CANCEL_EXECUTION_PATH = "/{id}/executions/{executionId}/cancel";
    private static final String NODE_TYPES_PATH = "/nodes/types";
    private static final String VERSION_PATH = "/versions/{id}";
    private static final String VERSIONS_BY_PIPELINE_PATH = "/{pipelineId}/versions";
    private static final String UPDATE_VERSION_PATH = "/versions/{id}";
    private static final String EXECUTION_EVENTS_PATH = "executions/{executionId}/events";

    @Context
    private HttpServletRequest _request;

    @Inject
    private IPipelineService _pipelineService;

    @Inject
    private PlatformSseStreamManager _sseStreamManager;

    /**
     * Default constructor required by CDI for proxy creation.
     */
    public PipelineAdminRest( )
    {
    }

    /**
     * Retrieves a pipeline by its identifier.
     *
     * @param id
     *            the pipeline identifier
     * @return Response containing pipeline data or error
     */
    @GET
    @Path( PIPELINE_ID_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipeline( @PathParam( "id" ) int id )
    {
        requireAuthenticatedUser( );
        return success( PipelineVersionService.findPipeline( id ) );
    }

    /**
     * Executes a pipeline with provided inputs.
     *
     * @param id
     *            the pipeline identifier
     * @param inputs
     *            the input parameters for the pipeline execution
     * @return Response containing execution details or error
     */
    @POST
    @Path( PipelineRestConstants.EXECUTE_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response executePipeline( @PathParam( "id" ) int id, Map<String, Object> inputs )
    {
        LuteceUser user = requireAuthenticatedUser( );
        Pipeline pipeline = PipelineVersionService.findPipeline( id );

        PipelineRequestDTO pipelineRequest = new PipelineRequestDTO( );
        pipelineRequest.setPipelineId( id );
        pipelineRequest.setUserId( user.getName( ) );
        if ( inputs != null )
        {
            for ( Map.Entry<String, Object> entry : inputs.entrySet( ) )
            {
                pipelineRequest.addInput( entry.getKey( ), entry.getValue( ) );
            }
        }
        String executionId = _pipelineService.executePipeline( pipelineRequest, pipeline.getIdClient( ) );
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
        requireAuthenticatedUser( );
        Optional<PipelineExecution> optExecution = _pipelineService.getExecution( executionId );
        if ( optExecution.isEmpty( ) || optExecution.get( ).getIdPipeline( ) != pipelineId )
        {
            return error( Response.Status.NOT_FOUND, EXECUTION_NOT_FOUND_MESSAGE );
        }
        return success( PipelineExecutionMapper.toDTO( optExecution.get( ) ) );
    }

    /**
     * Cancels a pipeline execution.
     *
     * @param pipelineId
     *            the pipeline identifier
     * @param executionId
     *            the execution identifier
     * @return Response containing cancellation status or error
     */
    @POST
    @Path( CANCEL_EXECUTION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response cancelExecution( @PathParam( "id" ) int pipelineId, @PathParam( "executionId" ) String executionId )
    {
        requireAuthenticatedUser( );
        Optional<PipelineExecution> optExecution = _pipelineService.getExecution( executionId );
        if ( optExecution.isEmpty( ) || optExecution.get( ).getIdPipeline( ) != pipelineId )
        {
            return error( Response.Status.NOT_FOUND, EXECUTION_NOT_FOUND_MESSAGE );
        }
        if ( !_pipelineService.cancelExecution( executionId ) )
        {
            return error( Response.Status.BAD_REQUEST, EXECUTION_CANCEL_FAILED_MESSAGE );
        }
        return success( new PipelineResponseDTO( executionId, CANCELLED_STATUS, EXECUTION_CANCELLED_MESSAGE ) );
    }

    /**
     * Retrieves available node types for pipeline building.
     *
     * @return Response containing node types or error
     */
    @GET
    @Path( NODE_TYPES_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getNodeTypes( )
    {
        requireAuthenticatedUser( );
        return success( _pipelineService.getNodeTypes( ) );
    }

    /**
     * Retrieves required inputs for a specific pipeline version.
     *
     * @param id
     *            the pipeline identifier
     * @param version
     *            the version name
     * @return Response containing required inputs or error
     */
    @GET
    @Path( PipelineRestConstants.REQUIRED_INPUTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getPipelineRequiredInputs( @PathParam( "id" ) int id, @PathParam( "version" ) String version )
    {
        requireAuthenticatedUser( );
        PipelineVersion pipelineVersion = PipelineVersionService.findVersionByName( id, version );
        return success( PipelineVariableConverter.resolveInputVariables( pipelineVersion ) );
    }

    /**
     * Retrieves a pipeline version by its identifier.
     *
     * @param id
     *            the version identifier
     * @return Response containing version data or error
     */
    @GET
    @Path( VERSION_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVersion( @PathParam( "id" ) int id )
    {
        requireAuthenticatedUser( );
        return success( PipelineVersionService.findVersion( id ) );
    }

    /**
     * Retrieves all versions for a specific pipeline.
     *
     * @param pipelineId
     *            the pipeline identifier
     * @return Response containing list of versions or error
     */
    @GET
    @Path( VERSIONS_BY_PIPELINE_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getVersionsByPipelineId( @PathParam( "pipelineId" ) int pipelineId )
    {
        requireAuthenticatedUser( );
        return success( PipelineVersionService.listVersionsOfPipeline( pipelineId ) );
    }

    /**
     * Updates a pipeline version.
     *
     * @param id
     *            the version identifier
     * @param versionData
     *            the version data to update
     * @return Response containing updated version or error
     */
    @PUT
    @Path( UPDATE_VERSION_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response updateVersion( @PathParam( "id" ) int id, Map<String, Object> versionData )
    {
        requireAuthenticatedUser( );
        return success( PipelineVersionService.updateVersion( id, versionData ) );
    }

    /**
     * Streams execution events via SSE.
     *
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
    public void getExecutionEvents( @PathParam( "executionId" ) String executionId, @Context SseEventSink eventSink, @Context Sse sse )
    {
        try
        {
            requireAuthenticatedUser( );
            if ( eventSink == null )
            {
                AppLogService.error( "{}{}", SSE_SINK_NULL_LOG, executionId );
                return;
            }
            _sseStreamManager.registerSseStream( executionId, Pipeline.RESOURCE_TYPE, eventSink, sse );
        }
        catch( WebApplicationException e )
        {
            AppLogService.error( "{}{}", AUTH_FAILED_LOG, executionId );
            SseRestUtils.closeEventSinkWithError( eventSink, sse, USER_NOT_AUTHENTICATED_MESSAGE );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", SSE_SETUP_ERROR_LOG, executionId, e );
            SseRestUtils.closeEventSinkWithError( eventSink, sse, e.getMessage( ) );
        }
    }

    /**
     * Resolves the current user or throws a WebApplicationException that maps to a 401 response.
     *
     * @return the authenticated LuteceUser
     * @throws WebApplicationException
     *             if no user is registered in the session
     */
    private LuteceUser requireAuthenticatedUser( )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            throw new WebApplicationException( Response.status( Response.Status.UNAUTHORIZED )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "UNAUTHORIZED", USER_NOT_AUTHENTICATED_MESSAGE ) ) ).build( ) );
        }
        return user;
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
        return Response.ok( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
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
