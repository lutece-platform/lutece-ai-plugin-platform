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
package fr.paris.lutece.plugins.platform.service.model;

import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.business.model.ChatCompletionRequestDTO;
import fr.paris.lutece.plugins.platform.business.model.ChatCompletionResponseDTO;
import fr.paris.lutece.plugins.platform.business.model.ChatMessageDTO;
import fr.paris.lutece.plugins.platform.business.model.EmbeddingRequestDTO;
import fr.paris.lutece.plugins.platform.business.model.EmbeddingResponseDTO;
import fr.paris.lutece.plugins.platform.business.model.FunctionCallDTO;
import fr.paris.lutece.plugins.platform.business.model.TokenUsageDTO;
import fr.paris.lutece.plugins.platform.business.model.ToolCallDTO;
import fr.paris.lutece.plugins.platform.business.model.ToolDTO;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelStreamCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelStreamErrorEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelToolCallsEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelTokenEvent;
import fr.paris.lutece.plugins.platform.langchain4j.util.LuteceAiMessageConverter;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.portal.service.util.AppLogService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import fr.paris.lutece.plugins.platform.service.concurrent.BlockingIO;

import java.util.concurrent.Executor;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for executing model queries including chat completion, streaming chat, and embedding operations
 */
@ApplicationScoped
@Named( "platform.modelQueryService" )
public class ModelQueryService
{

    private static final String ERROR_CHAT_COMPLETION = "Error during chat completion for model: ";
    private static final String ERROR_STREAMING_CHAT = "Error during streaming chat for model: ";
    private static final String ERROR_EMBEDDING = "Error during embedding for model: ";
    private static final String SUCCESS_STREAMING_COMPLETION = "Streaming chat completion successful";
    private static final String SUCCESS_EMBEDDING = "Embedding generation successful";
    private static final String NODE_ID_PREFIX_EMBEDDING = "model-api-embedding-";
    private static final String NODE_DESC_EMBEDDING = "Embedding generation (";
    private static final String NODE_INPUT_DESC_EMBEDDING = "Embedding ";
    private static final String NODE_COMPLETE_FORMAT_TOKENS = "Embedding completed: %d tokens";
    private static final String NODE_COMPLETE_NO_USAGE = "Embedding completed (no token usage data)";
    private static final String ROLE_SYSTEM = "system";
    private static final String CONTENT_TYPE_TEXT = "text";
    private static final String CONTENT_TYPE_IMAGE_URL = "image_url";
    private static final String KEY_CONTENT_TYPE = "type";
    private static final String KEY_IMAGE_URL = "image_url";
    private static final String KEY_URL = "url";
    private static final String DATA_URL_PREFIX = "data:";
    private static final String BASE64_SEPARATOR = ";base64,";
    private static final String RESPONSE_FORMAT_JSON = "json";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int TOKEN_PRICE_DIVISOR = 1_000_000;
    @Inject
    @BlockingIO
    private ManagedExecutorService _blockingExecutor;

    /**
     * Used to restore the CDI/TCCL context when langchain4j fires StreamingChatResponseHandler callbacks from its internal HTTP client pool — required for DAO
     * access in the callbacks.
     */
    @Resource( lookup = "java:comp/DefaultContextService" )
    private ContextService _contextService;

    @Inject
    private Event<ModelTokenEvent> _tokenEvent;

    @Inject
    private Event<ModelStreamCompletedEvent> _streamCompletedEvent;

    @Inject
    private Event<ModelStreamErrorEvent> _streamErrorEvent;

    @Inject
    private Event<ModelToolCallsEvent> _toolCallsEvent;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private ModelService _modelService;

    /**
     * Default constructor for CDI.
     */
    ModelQueryService( )
    {
    }

    /**
     * Executes a chat completion request
     *
     * @param requestDTO
     *            the chat completion request
     * @param provider
     *            the provider to use
     * @return the chat completion response
     */
    public ChatCompletionResponseDTO executeChatCompletion( ChatCompletionRequestDTO requestDTO, Model model )
    {
        Provider provider = model.getProvider( );
        int modelId = model.getId( );
        int clientId = model.getClientId( );

        String lastUserMessage = extractLastUserMessage( requestDTO.getMessages( ) );
        List<Map<String, String>> history = extractMessageHistory( requestDTO.getMessages( ) );
        String executionId = _observabilityService.startResourceExecution( Model.RESOURCE_TYPE, String.valueOf( modelId ), clientId,
                ObservabilityData.of( "MODEL_CALL", "model", provider.getDeploymentModelName( ), "providerId", provider.getId( ), "streaming", false, "prompt",
                        lastUserMessage, "history", history ) );

        try
        {
            Double temperatureValue = requestDTO.getTemperature( );
            double temperature = temperatureValue != null ? temperatureValue : DEFAULT_TEMPERATURE;

            ChatModel chatModel = _modelService.createApiChatModel( provider, executionId, temperature );

            List<ChatMessage> messages = requestDTO.getMessages( ).stream( ).map( this::convertToLangChainMessage ).toList( );

            DefaultChatRequestParameters.Builder<?> paramsBuilder = ChatRequestParameters.builder( ).modelName( provider.getDeploymentName( ) );

            if ( requestDTO.getMaxTokens( ) != null )
            {
                paramsBuilder.maxOutputTokens( requestDTO.getMaxTokens( ) );
            }
            if ( requestDTO.getTopP( ) != null )
            {
                paramsBuilder.topP( requestDTO.getTopP( ) );
            }
            if ( requestDTO.getFrequencyPenalty( ) != null )
            {
                paramsBuilder.frequencyPenalty( requestDTO.getFrequencyPenalty( ) );
            }
            if ( requestDTO.getPresencePenalty( ) != null )
            {
                paramsBuilder.presencePenalty( requestDTO.getPresencePenalty( ) );
            }
            if ( requestDTO.getStop( ) != null && !requestDTO.getStop( ).isEmpty( ) )
            {
                paramsBuilder.stopSequences( requestDTO.getStop( ) );
            }
            if ( requestDTO.getTools( ) != null && !requestDTO.getTools( ).isEmpty( ) )
            {
                paramsBuilder.toolSpecifications( convertToolsToSpecifications( requestDTO.getTools( ) ) );
            }
            if ( RESPONSE_FORMAT_JSON.equalsIgnoreCase( requestDTO.getResponseFormat( ) ) )
            {
                paramsBuilder.responseFormat( ResponseFormat.JSON );
            }

            ChatRequest chatRequest = ChatRequest.builder( ).messages( messages ).parameters( paramsBuilder.build( ) ).build( );

            ChatResponse chatResponse = chatModel.chat( chatRequest );
            String responseText = chatResponse.aiMessage( ) != null ? chatResponse.aiMessage( ).text( ) : "";

            int inputTokens = chatResponse.metadata( ) != null && chatResponse.metadata( ).tokenUsage( ) != null
                    ? chatResponse.metadata( ).tokenUsage( ).inputTokenCount( )
                    : 0;
            int outputTokens = chatResponse.metadata( ) != null && chatResponse.metadata( ).tokenUsage( ) != null
                    ? chatResponse.metadata( ).tokenUsage( ).outputTokenCount( )
                    : 0;

            _observabilityService.completeResourceExecutionSuccess( executionId, ObservabilityData.of( "MODEL_RESPONSE", "success", true, "inputTokens",
                    inputTokens, "outputTokens", outputTokens, "response", responseText ) );

            return convertToChatCompletionResponse( chatResponse, provider );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_CHAT_COMPLETION, provider.getDeploymentModelName( ), e );
            _observabilityService.completeResourceExecutionError( executionId, e.getMessage( ), null );
            throw e;
        }
    }

    /**
     * Executes a streaming chat completion request
     *
     * @param requestDTO
     *            the chat completion request
     * @param provider
     *            the provider to use
     * @param streamId
     *            the stream identifier
     */
    public void executeStreamingChatCompletion( ChatCompletionRequestDTO requestDTO, String streamId, Model model )
    {
        final Executor ctxExecutor = _contextService.currentContextExecutor( );
        CompletableFuture.runAsync( ( ) -> {
            Provider provider = model.getProvider( );
            int modelId = model.getId( );
            int clientId = model.getClientId( );

            String lastUserMessage = extractLastUserMessage( requestDTO.getMessages( ) );
            List<Map<String, String>> history = extractMessageHistory( requestDTO.getMessages( ) );
            String executionId = _observabilityService.startResourceExecution( Model.RESOURCE_TYPE, String.valueOf( modelId ), clientId,
                    ObservabilityData.of( "MODEL_CALL", "model", provider.getDeploymentModelName( ), "providerId", provider.getId( ), "streaming", true,
                            "prompt", lastUserMessage, "history", history ) );

            try
            {
                Double temperatureValue = requestDTO.getTemperature( );
                double temperature = temperatureValue != null ? temperatureValue : DEFAULT_TEMPERATURE;

                StreamingChatModel streamingChatModel = _modelService.createApiStreamingChatModel( provider, executionId, temperature );

                List<ChatMessage> messages = requestDTO.getMessages( ).stream( ).map( this::convertToLangChainMessage ).toList( );

                DefaultChatRequestParameters.Builder<?> paramsBuilder = ChatRequestParameters.builder( ).modelName( provider.getDeploymentName( ) );

                if ( requestDTO.getMaxTokens( ) != null )
                {
                    paramsBuilder.maxOutputTokens( requestDTO.getMaxTokens( ) );
                }
                if ( requestDTO.getTopP( ) != null )
                {
                    paramsBuilder.topP( requestDTO.getTopP( ) );
                }
                if ( requestDTO.getFrequencyPenalty( ) != null )
                {
                    paramsBuilder.frequencyPenalty( requestDTO.getFrequencyPenalty( ) );
                }
                if ( requestDTO.getPresencePenalty( ) != null )
                {
                    paramsBuilder.presencePenalty( requestDTO.getPresencePenalty( ) );
                }
                if ( requestDTO.getStop( ) != null && !requestDTO.getStop( ).isEmpty( ) )
                {
                    paramsBuilder.stopSequences( requestDTO.getStop( ) );
                }
                if ( requestDTO.getTools( ) != null && !requestDTO.getTools( ).isEmpty( ) )
                {
                    paramsBuilder.toolSpecifications( convertToolsToSpecifications( requestDTO.getTools( ) ) );
                }
                if ( RESPONSE_FORMAT_JSON.equalsIgnoreCase( requestDTO.getResponseFormat( ) ) )
                {
                    paramsBuilder.responseFormat( ResponseFormat.JSON );
                }

                ChatRequest chatRequest = ChatRequest.builder( ).messages( messages ).parameters( paramsBuilder.build( ) ).build( );

                streamingChatModel.chat( chatRequest, new StreamingChatResponseHandler( )
                {
                    @Override
                    public void onPartialResponse( String token )
                    {
                        _tokenEvent.fire( ModelTokenEvent.now( streamId, provider.getId( ), token ) );
                    }

                    @Override
                    public void onCompleteResponse( ChatResponse response )
                    {
                        ctxExecutor.execute( ( ) -> {
                            fireToolCallsIfAny( streamId, provider.getId( ), response );
                            _streamCompletedEvent.fire( ModelStreamCompletedEvent.now( streamId, provider.getId( ) ) );
                            int inputTokens = response.metadata( ) != null && response.metadata( ).tokenUsage( ) != null
                                    ? response.metadata( ).tokenUsage( ).inputTokenCount( )
                                    : 0;
                            int outputTokens = response.metadata( ) != null && response.metadata( ).tokenUsage( ) != null
                                    ? response.metadata( ).tokenUsage( ).outputTokenCount( )
                                    : 0;
                            _observabilityService.completeResourceExecutionSuccess( executionId, ObservabilityData.of( "MODEL_RESPONSE", "success", true,
                                    "inputTokens", inputTokens, "outputTokens", outputTokens, "response", SUCCESS_STREAMING_COMPLETION ) );
                        } );
                    }

                    @Override
                    public void onError( Throwable error )
                    {
                        ctxExecutor.execute( ( ) -> {
                            AppLogService.error( "{}{}", ERROR_STREAMING_CHAT, provider.getDeploymentModelName( ), error );
                            _streamErrorEvent.fire( ModelStreamErrorEvent.now( streamId, provider.getId( ), error.getMessage( ) ) );
                            _observabilityService.completeResourceExecutionError( executionId, error.getMessage( ), null );
                        } );
                    }
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( "{}{}", ERROR_STREAMING_CHAT, provider.getDeploymentModelName( ), e );
                _streamErrorEvent.fire( ModelStreamErrorEvent.now( streamId, provider.getId( ), e.getMessage( ) ) );
                _observabilityService.completeResourceExecutionError( executionId, e.getMessage( ), null );
            }
        }, _blockingExecutor ).exceptionally( throwable -> {
            AppLogService.error( "{}{}", ERROR_STREAMING_CHAT, model.getProvider( ).getDeploymentModelName( ), throwable );
            _streamErrorEvent.fire( ModelStreamErrorEvent.now( streamId, model.getProvider( ).getId( ), throwable.getMessage( ) ) );
            return null;
        } );
    }

    /**
     * Executes an embedding request
     *
     * @param requestDTO
     *            the embedding request
     * @param Model
     *            model the model to use
     * @return the embedding response
     */
    public EmbeddingResponseDTO executeEmbedding( EmbeddingRequestDTO requestDTO, Model model )
    {
        Provider provider = model.getProvider( );
        int modelId = model.getId( );
        int clientId = model.getClientId( );

        String executionId = _observabilityService.startResourceExecution( Model.RESOURCE_TYPE, String.valueOf( modelId ), clientId,
                ObservabilityData.of( "MODEL_CALL", "model", provider.getDeploymentModelName( ), "providerId", provider.getId( ), "streaming", false ) );

        String nodeId = NODE_ID_PREFIX_EMBEDDING + executionId;

        try
        {
            _observabilityService.startNodeExecution( executionId, nodeId, NODE_DESC_EMBEDDING + provider.getDeploymentModelName( ) + ")", 1,
                    ObservabilityData.input( NODE_INPUT_DESC_EMBEDDING + requestDTO.getTexts( ).size( ) + " text(s)", "Embedding initialization" ) );

            EmbeddingModel embeddingModel = _modelService.createEmbeddingModel( provider );

            List<TextSegment> textSegments = requestDTO.getTexts( ).stream( ).map( TextSegment::from ).toList( );

            Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll( textSegments );

            if ( embeddingResponse.tokenUsage( ) != null && embeddingResponse.tokenUsage( ).inputTokenCount( ) != null )
            {
                int totalInputTokens = embeddingResponse.tokenUsage( ).inputTokenCount( );
                double tokenInputPricePerMillion = provider.getTokenInputPrice1M( );
                double tokenInputPricePerToken = tokenInputPricePerMillion / TOKEN_PRICE_DIVISOR;
                BigDecimal totalCost = BigDecimal.valueOf( tokenInputPricePerToken * totalInputTokens );

                _observabilityService.addNodeTrace( executionId, nodeId,
                        ObservabilityData.of( "TOKEN_COST", "type", "input", "tokens", totalInputTokens, "cost", totalCost ),
                        PlatformResourceNodeTraceStatus.INFO.getValue( ), totalCost );

                _observabilityService.completeNodeExecutionSuccess( executionId, nodeId,
                        ObservabilityData.output( String.format( NODE_COMPLETE_FORMAT_TOKENS, totalInputTokens ), "Embedding completed successfully" ) );
            }
            else
            {
                _observabilityService.completeNodeExecutionSuccess( executionId, nodeId,
                        ObservabilityData.output( NODE_COMPLETE_NO_USAGE, "No token usage data available" ) );
            }

            _observabilityService.completeResourceExecutionSuccess( executionId,
                    ObservabilityData.of( "MODEL_RESPONSE", "success", true, "inputTokens",
                            embeddingResponse.tokenUsage( ) != null ? embeddingResponse.tokenUsage( ).inputTokenCount( ) : 0, "outputTokens", 0, "response",
                            SUCCESS_EMBEDDING ) );

            return convertToEmbeddingResponse( embeddingResponse );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_EMBEDDING, provider.getDeploymentModelName( ), e );
            _observabilityService.completeNodeExecutionError( executionId, nodeId, e.getMessage( ), null );
            _observabilityService.completeResourceExecutionError( executionId, e.getMessage( ), null );
            throw e;
        }
    }

    /**
     * Extracts the last user message from a list of messages
     *
     * @param messages
     *            the list of chat messages
     * @return the content of the last user message, or empty string if none found
     */
    private String extractLastUserMessage( List<ChatMessageDTO> messages )
    {
        if ( messages == null || messages.isEmpty( ) )
        {
            return "";
        }
        for ( int i = messages.size( ) - 1; i >= 0; i-- )
        {
            ChatMessageDTO msg = messages.get( i );
            if ( ConversationMessage.ROLE_USER.equalsIgnoreCase( msg.getRole( ) ) && msg.getContent( ) != null )
            {
                return msg.getContent( );
            }
        }
        return "";
    }

    /**
     * Extracts the message history as a list of simple maps
     *
     * @param messages
     *            the list of chat messages
     * @return a list of maps containing role and content for each message
     */
    private List<Map<String, String>> extractMessageHistory( List<ChatMessageDTO> messages )
    {
        if ( messages == null || messages.isEmpty( ) )
        {
            return new ArrayList<>( );
        }
        List<Map<String, String>> history = new ArrayList<>( );
        for ( ChatMessageDTO msg : messages )
        {
            Map<String, String> entry = new HashMap<>( );
            entry.put( "role", msg.getRole( ) );
            entry.put( "content", msg.getContent( ) != null ? msg.getContent( ) : "" );
            history.add( entry );
        }
        return history;
    }

    /**
     * Converts a ChatMessageDTO to a LangChain ChatMessage
     *
     * @param dto
     *            the chat message DTO
     * @return the LangChain ChatMessage
     */
    private ChatMessage convertToLangChainMessage( ChatMessageDTO dto )
    {
        String role = dto.getRole( );
        String content = dto.getContent( );

        if ( ConversationMessage.ROLE_USER.equalsIgnoreCase( role ) )
        {
            return buildUserMessage( dto, content );
        }
        else if ( ConversationMessage.ROLE_ASSISTANT.equalsIgnoreCase( role ) )
        {
            if ( dto.getToolCalls( ) != null && !dto.getToolCalls( ).isEmpty( ) )
            {
                List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests = new ArrayList<>( );
                for ( ToolCallDTO toolCall : dto.getToolCalls( ) )
                {
                    dev.langchain4j.agent.tool.ToolExecutionRequest request = dev.langchain4j.agent.tool.ToolExecutionRequest.builder( ).id( toolCall.getId( ) )
                            .name( toolCall.getFunction( ).getName( ) ).arguments( toolCall.getFunction( ).getArguments( ) ).build( );
                    requests.add( request );
                }
                return new AiMessage( content, requests );
            }
            return AiMessage.from( content );
        }
        else if ( ROLE_SYSTEM.equalsIgnoreCase( role ) )
        {
            return SystemMessage.from( content != null ? content : "" );
        }
        else if ( ConversationMessage.ROLE_TOOL.equalsIgnoreCase( role ) )
        {
            return new ToolExecutionResultMessage( dto.getToolCallId( ), dto.getName( ), content != null ? content : "" );
        }
        else
        {
            return buildUserMessage( dto, content );
        }
    }

    /**
     * Builds a UserMessage from a ChatMessageDTO, handling both text-only and multimodal content.
     *
     * @param dto
     *            the chat message DTO
     * @param textContent
     *            the text content (null if multimodal)
     * @return the UserMessage
     */
    @SuppressWarnings( "unchecked" )
    private UserMessage buildUserMessage( ChatMessageDTO dto, String textContent )
    {
        Object rawContent = dto.getRawContent( );

        if ( !( rawContent instanceof List ) )
        {
            return UserMessage.from( textContent != null ? textContent : "" );
        }

        List<Content> contents = new ArrayList<>( );

        for ( Map<String, Object> block : (List<Map<String, Object>>) rawContent )
        {
            String type = (String) block.get( KEY_CONTENT_TYPE );

            if ( CONTENT_TYPE_TEXT.equals( type ) )
            {
                contents.add( TextContent.from( (String) block.get( CONTENT_TYPE_TEXT ) ) );
            }
            else if ( CONTENT_TYPE_IMAGE_URL.equals( type ) )
            {
                Map<String, String> imageUrl = (Map<String, String>) block.get( KEY_IMAGE_URL );
                String url = imageUrl.get( KEY_URL );
                contents.add( parseImageContent( url ) );
            }
        }

        return contents.isEmpty( ) ? UserMessage.from( "" ) : UserMessage.from( contents );
    }

    /**
     * Parses an image URL (data URL or HTTP URL) into an ImageContent. Data URLs are split into mimeType + base64 for provider compatibility (Anthropic
     * requires separate fields).
     *
     * @param url
     *            the image URL or data URL
     * @return the ImageContent
     */
    private ImageContent parseImageContent( String url )
    {
        if ( url != null && url.startsWith( DATA_URL_PREFIX ) && url.contains( BASE64_SEPARATOR ) )
        {
            int separatorIndex = url.indexOf( BASE64_SEPARATOR );
            String mimeType = url.substring( DATA_URL_PREFIX.length( ), separatorIndex );
            String base64Data = url.substring( separatorIndex + BASE64_SEPARATOR.length( ) );
            return ImageContent.from( base64Data, mimeType );
        }
        return ImageContent.from( url );
    }

    /**
     * Converts a LangChain ChatResponse to a ChatCompletionResponseDTO
     *
     * @param chatResponse
     *            the chat response
     * @param provider
     *            the provider
     * @return the chat completion response DTO
     */
    private ChatCompletionResponseDTO convertToChatCompletionResponse( ChatResponse chatResponse, Provider provider )
    {
        ChatCompletionResponseDTO dto = new ChatCompletionResponseDTO( );
        dto.setId( chatResponse.metadata( ) != null ? chatResponse.metadata( ).id( ) : UUID.randomUUID( ).toString( ) );
        dto.setModel( provider.getDeploymentModelName( ) );

        ChatMessageDTO messageDTO = new ChatMessageDTO( );
        messageDTO.setRole( ConversationMessage.ROLE_ASSISTANT );
        messageDTO.setContent( chatResponse.aiMessage( ).text( ) != null ? chatResponse.aiMessage( ).text( ) : "" );

        if ( chatResponse.aiMessage( ).toolExecutionRequests( ) != null && !chatResponse.aiMessage( ).toolExecutionRequests( ).isEmpty( ) )
        {
            List<ToolCallDTO> toolCalls = new ArrayList<>( );
            for ( dev.langchain4j.agent.tool.ToolExecutionRequest request : chatResponse.aiMessage( ).toolExecutionRequests( ) )
            {
                ToolCallDTO toolCallDTO = new ToolCallDTO( );
                toolCallDTO.setId( request.id( ) );
                toolCallDTO.setType( "function" );

                FunctionCallDTO functionCallDTO = new FunctionCallDTO( );
                functionCallDTO.setName( request.name( ) );
                functionCallDTO.setArguments( request.arguments( ) );
                toolCallDTO.setFunction( functionCallDTO );

                toolCalls.add( toolCallDTO );
            }
            messageDTO.setToolCalls( toolCalls );
        }

        dto.setMessage( messageDTO );

        if ( chatResponse.metadata( ) != null && chatResponse.metadata( ).tokenUsage( ) != null )
        {
            TokenUsage usage = chatResponse.metadata( ).tokenUsage( );
            TokenUsageDTO tokenUsageDTO = new TokenUsageDTO( );
            tokenUsageDTO.setInputTokenCount( usage.inputTokenCount( ) );
            tokenUsageDTO.setOutputTokenCount( usage.outputTokenCount( ) );
            tokenUsageDTO.setTotalTokenCount( usage.totalTokenCount( ) );
            dto.setTokenUsage( tokenUsageDTO );
        }

        if ( chatResponse.metadata( ) != null && chatResponse.metadata( ).finishReason( ) != null )
        {
            dto.setFinishReason( chatResponse.metadata( ).finishReason( ).toString( ) );
        }

        return dto;
    }

    /**
     * Converts a LangChain embedding response to an EmbeddingResponseDTO
     *
     * @param embeddingResponse
     *            the embedding response
     * @return the embedding response DTO
     */
    private EmbeddingResponseDTO convertToEmbeddingResponse( Response<List<Embedding>> embeddingResponse )
    {
        EmbeddingResponseDTO dto = new EmbeddingResponseDTO( );

        List<List<Double>> embeddings = embeddingResponse.content( ).stream( ).map( this::convertEmbedding ).toList( );

        dto.setEmbeddings( embeddings );

        if ( embeddingResponse.tokenUsage( ) != null )
        {
            TokenUsageDTO tokenUsageDTO = new TokenUsageDTO( );
            tokenUsageDTO.setInputTokenCount( embeddingResponse.tokenUsage( ).inputTokenCount( ) );
            dto.setTokenUsage( tokenUsageDTO );
        }

        return dto;
    }

    /**
     * Converts a LangChain Embedding to a list of Double values
     *
     * @param embedding
     *            the embedding
     * @return the list of double values
     */
    private List<Double> convertEmbedding( Embedding embedding )
    {
        float [ ] vector = embedding.vector( );
        List<Double> result = new ArrayList<>( vector.length );

        for ( float value : vector )
        {
            result.add( (double) value );
        }

        return result;
    }

    /**
     * Converts ToolDTO list to LangChain4j ToolSpecification list
     *
     * @param tools
     *            the list of tool DTOs
     * @return the list of tool specifications
     */
    private List<ToolSpecification> convertToolsToSpecifications( List<ToolDTO> tools )
    {
        List<ToolSpecification> specifications = new ArrayList<>( );

        for ( ToolDTO tool : tools )
        {
            if ( tool.getFunction( ) != null )
            {
                JsonObjectSchema parameters = null;
                if ( tool.getFunction( ).getParameters( ) != null )
                {
                    parameters = LuteceAiMessageConverter.convertMapToJsonObjectSchema( tool.getFunction( ).getParameters( ) );
                }

                ToolSpecification spec = ToolSpecification.builder( ).name( tool.getFunction( ).getName( ) )
                        .description( tool.getFunction( ).getDescription( ) ).parameters( parameters ).build( );

                specifications.add( spec );
            }
        }

        return specifications;
    }

    /**
     * Fires a tool-calls event when a streaming response ends with tool execution requests, so SSE consumers receive them (the streaming path has no response
     * DTO to carry them).
     *
     * @param streamId
     *            the stream identifier
     * @param providerId
     *            the provider identifier
     * @param response
     *            the completed chat response
     */
    private void fireToolCallsIfAny( String streamId, int providerId, ChatResponse response )
    {
        if ( response.aiMessage( ) == null || response.aiMessage( ).toolExecutionRequests( ) == null
                || response.aiMessage( ).toolExecutionRequests( ).isEmpty( ) )
        {
            return;
        }
        List<ModelToolCallsEvent.ToolCall> toolCalls = response.aiMessage( ).toolExecutionRequests( ).stream( )
                .map( toolRequest -> new ModelToolCallsEvent.ToolCall( toolRequest.id( ), toolRequest.name( ), toolRequest.arguments( ) ) ).toList( );
        _toolCallsEvent.fire( ModelToolCallsEvent.now( streamId, providerId, toolCalls ) );
    }
}
