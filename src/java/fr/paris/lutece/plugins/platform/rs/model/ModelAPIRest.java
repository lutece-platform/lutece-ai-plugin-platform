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
package fr.paris.lutece.plugins.platform.rs.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import fr.paris.lutece.plugins.platform.business.model.ModelInfoDTO;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.model.ChatCompletionRequestDTO;
import fr.paris.lutece.plugins.platform.business.model.ChatCompletionResponseDTO;
import fr.paris.lutece.plugins.platform.business.model.EmbeddingRequestDTO;
import fr.paris.lutece.plugins.platform.business.model.EmbeddingResponseDTO;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.provider.ProviderTypeConstants;
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.service.sse.PlatformSseStreamManager;
import fr.paris.lutece.plugins.platform.service.model.ModelQueryService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

/**
 * REST API for model operations
 */
@ApplicationScoped
@Path( "platform/agent/api/models" )
public class ModelAPIRest
{

    private static final String CHAT_COMPLETIONS_PATH = "chat/completions";
    private static final String EMBEDDINGS_PATH = "embeddings";
    private static final String MODELS_PATH = "models";
    private static final String CHAT_STREAM_PATH = "chat/completions/stream";

    private static final String MODEL_NOT_FOUND_MESSAGE = "The requested model does not exist or is no longer available.";
    private static final String HIGH_TRAFFIC_MESSAGE = "Nous expérimentons un fort trafic actuellement. Veuillez réessayer dans quelques instants.";

    @Context
    private HttpServletRequest _request;

    @Inject
    private AuthenticationService authenticationService;
    @Inject
    private PlatformSseStreamManager sseStreamManager;
    @Inject
    private ModelQueryService modelQueryService;

    /**
     * Handle chat completion requests
     *
     * @param request
     *            the chat completion request containing model and messages
     * @return the response containing the chat completion result or error
     */
    @POST
    @Path( CHAT_COMPLETIONS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response chatCompletions( @NotNull @Valid ChatCompletionRequestDTO request )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        Model model = findModelByProviderName( request.getModel( ), ProviderTypeConstants.PROVIDER_TYPE_LLM, client.getId( ) );
        if ( model == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, MODEL_NOT_FOUND_MESSAGE );
        }

        authenticationService.authenticateClient( _request, Model.RESOURCE_TYPE, String.valueOf( model.getId( ) ) );

        ChatCompletionResponseDTO response = modelQueryService.executeChatCompletion( request, model );
        return createSuccessResponse( response );
    }

    /**
     * Handle embedding generation requests
     *
     * @param request
     *            the embedding request containing model and texts
     * @return the response containing the embedding vectors or error
     */
    @POST
    @Path( EMBEDDINGS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response embeddings( @NotNull @Valid EmbeddingRequestDTO request )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        Model model = findModelByProviderName( request.getModel( ), ProviderTypeConstants.PROVIDER_TYPE_EMBEDDING, client.getId( ) );
        if ( model == null )
        {
            return createErrorResponse( Response.Status.NOT_FOUND, MODEL_NOT_FOUND_MESSAGE );
        }

        authenticationService.authenticateClient( _request, Model.RESOURCE_TYPE, String.valueOf( model.getId( ) ) );

        EmbeddingResponseDTO response = modelQueryService.executeEmbedding( request, model );
        return createSuccessResponse( response );
    }

    /**
     * Get list of available models for the authenticated client
     *
     * @return the response containing list of models or error
     */
    @GET
    @Path( MODELS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getModels( )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        List<Model> clientModels = ModelHome.getModelsListByClientId( client.getId( ) );
        List<ModelInfoDTO> modelInfoList = new ArrayList<>( );

        for ( Model model : clientModels )
        {
            Provider provider = ProviderHome.findByPrimaryKey( model.getProviderId( ) ).orElse( null );
            if ( provider != null )
            {
                modelInfoList.add( providerToModelInfo( provider ) );
            }
        }

        return createSuccessResponse( modelInfoList );
    }

    /**
     * Streams a chat completion as Server-Sent Events over a single POST : the response body is the event stream. Authentication, model access and the global
     * concurrency cap are checked synchronously and surface as clean JSON errors before the SSE response is committed ; once validated, the SSE sink is
     * registered on the operation id the generation emits on, then generation starts and the method returns while events flow.
     *
     * @param request
     *            the chat completion request for streaming
     * @param eventSink
     *            the SSE event sink (the live connection)
     * @param sse
     *            the SSE context
     */
    @POST
    @Path( CHAT_STREAM_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.SERVER_SENT_EVENTS )
    public void streamChatCompletion( @NotNull @Valid ChatCompletionRequestDTO request, @Context SseEventSink eventSink, @Context Sse sse )
    {
        Client client = authenticationService.authenticateClient( _request, null, null );

        Model model = findModelByProviderName( request.getModel( ), ProviderTypeConstants.PROVIDER_TYPE_LLM, client.getId( ) );
        if ( model == null )
        {
            throw new WebApplicationException( createErrorResponse( Response.Status.NOT_FOUND, MODEL_NOT_FOUND_MESSAGE ) );
        }

        authenticationService.authenticateClient( _request, Model.RESOURCE_TYPE, String.valueOf( model.getId( ) ) );

        if ( !sseStreamManager.canCreateNewStream( ) )
        {
            throw new WebApplicationException( Response.status( Response.Status.TOO_MANY_REQUESTS )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "TOO_MANY_CONCURRENT_STREAMS", HIGH_TRAFFIC_MESSAGE ) ) )
                    .type( MediaType.APPLICATION_JSON ).build( ) );
        }

        String operationId = UUID.randomUUID( ).toString( );
        sseStreamManager.registerSseStream( operationId, Model.RESOURCE_TYPE, eventSink, sse, client.getId( ), null );
        modelQueryService.executeStreamingChatCompletion( request, operationId, model );
    }

    /**
     * Find model by provider name, type and client
     *
     * @param modelName
     *            the name of the model to find
     * @param providerType
     *            the type of provider
     * @param clientId
     *            the client identifier
     * @return Model with provider set if found, null otherwise
     */
    private Model findModelByProviderName( String modelName, String providerType, int clientId )
    {
        List<Model> clientModels = ModelHome.getModelsListByClientId( clientId );
        for ( Model model : clientModels )
        {
            Provider provider = ProviderHome.findByPrimaryKey( model.getProviderId( ) ).orElse( null );
            if ( provider != null && providerType.equals( provider.getProviderType( ) ) )
            {
                if ( modelName.equals( provider.getDeploymentModelName( ) ) || modelName.equals( provider.getProviderName( ) ) )
                {
                    model.setProvider( provider );
                    return model;
                }
            }
        }
        return null;
    }

    /**
     * Maps a provider to the model catalog DTO.
     *
     * @param provider
     *            the provider to convert
     * @return the model info DTO
     */
    private ModelInfoDTO providerToModelInfo( Provider provider )
    {
        ModelInfoDTO info = new ModelInfoDTO( );
        info.setId( provider.getId( ) );
        info.setName( provider.getDeploymentModelName( ) );
        info.setProviderName( provider.getProviderName( ) );
        info.setType( provider.getProviderType( ) );
        return info;
    }

    /**
     * Create success response with data
     *
     * @param data
     *            the data to include in response
     * @return success response
     */
    private Response createSuccessResponse( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Create error response with status and message
     *
     * @param status
     *            the HTTP status
     * @param message
     *            the error message
     * @return error response
     */
    private Response createErrorResponse( Response.Status status, String message )
    {
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), message ) ) ).build( );
    }
}
