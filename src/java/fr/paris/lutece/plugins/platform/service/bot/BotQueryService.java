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
package fr.paris.lutece.plugins.platform.service.bot;

import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import fr.paris.lutece.plugins.platform.business.conversation.FeedbackDTO;
import fr.paris.lutece.plugins.platform.business.conversation.MessageDTO;
import fr.paris.lutece.plugins.platform.business.bot.QueryRequestDTO;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;
import fr.paris.lutece.plugins.platform.business.bot.BotDatasetHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.IBotAssistant;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.event.domain.BotSourcesRetrievedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotStreamCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotStreamErrorEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotStreamStartedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotTokenEvent;
import fr.paris.lutece.plugins.platform.service.conversation.FeedbackService;
import fr.paris.lutece.plugins.platform.service.model.CostTrackingListener;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.rag.RAGService;
import fr.paris.lutece.plugins.platform.service.rag.citation.CitationTracker;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.plugins.platform.service.conversation.dto.ConversationView;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppLogService;

import fr.paris.lutece.plugins.platform.service.concurrent.BlockingIO;

import java.util.concurrent.Executor;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named( "platform.botQueryService" )
public class BotQueryService
{

    private static final String ERROR_LLM_PROVIDER_NOT_FOUND = "LLM provider not found for bot ";
    private static final String ERROR_STREAMING_QUERY = "Error processing streaming query for bot ";
    private static final String ERROR_TOKEN_STREAM = "Failed to obtain token stream for bot ";
    private static final String ERROR_DURING_STREAMING = "Error during streaming for bot ";
    private static final String ERROR_ASYNC_PROCESSING_FAILED = "Asynchronous processing failed: ";
    private static final String ERROR_HALLUCINATED_TOOL = "Error: there is no tool called ";
    private static final String ERROR_CONVERSATION_UUID_NULL = "Conversation UUID is null or empty";

    private static final String NODE_STREAMING_QUERY_SERVICE = "streaming-query-service-operation-";
    private static final String OP_STREAMING_QUERY_PROCESSING = "Requête en streaming";

    private static final String DESC_PROCESSING_STREAMING = "Traitement de la requête en streaming pour le bot ";
    private static final String MSG_STREAMING_COMPLETED = "Opération de traitement de requête en streaming terminée.";
    private static final String SOURCE_TEXT_KEY = "text";

    @Inject
    private FeedbackService feedbackService;

    @Inject
    private BotMemoryService _botMemoryService;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private ModelService _modelService;

    @Inject
    private RAGService _ragService;

    @Inject
    @BlockingIO
    private ManagedExecutorService _blockingExecutor;

    @Resource( lookup = "java:comp/DefaultContextService" )
    private ContextService _contextService;

    @Inject
    private Event<BotStreamStartedEvent> _streamStartedEvent;

    @Inject
    private Event<BotTokenEvent> _tokenEvent;

    @Inject
    private Event<BotStreamCompletedEvent> _streamCompletedEvent;

    @Inject
    private Event<BotStreamErrorEvent> _streamErrorEvent;

    @Inject
    private Event<BotSourcesRetrievedEvent> _sourcesRetrievedEvent;

    BotQueryService( )
    {
    }

    /**
     * Processes a streaming query for a bot. The caller provides the {@code operationId} the SSE subscription was registered with, so the subscription is in
     * place before the first event fires (no registration race).
     *
     * @param bot
     *            the bot to process the query for
     * @param request
     *            the query request (query text, conversation UUID, user ID)
     * @param operationId
     *            the operation identifier the events are emitted on, owned by the caller
     * @return the streaming query response
     */
    public StreamingQueryResponse processStreamingQuery( Bot bot, QueryRequestDTO request, String operationId )
    {
        String streamingQueryServiceNodeId = NODE_STREAMING_QUERY_SERVICE + UUID.randomUUID( );
        int botId = bot.getId( );

        String uuid = _botMemoryService.getOrCreateConversation( botId, request.getConversationUuid( ), request.getUserId( ) );
        List<Map<String, String>> history = extractConversationHistory( uuid, request.getUserId( ) );

        String executionId = _observabilityService.startResourceExecution( Bot.RESOURCE_TYPE, String.valueOf( botId ), bot.getClientId( ), ObservabilityData.of(
                "BOT_QUERY", "botId", botId, "query", request.getQuery( ), "userId", request.getUserId( ), "conversationUuid", uuid, "history", history ) );

        Provider llmProvider = ProviderHome.findByPrimaryKey( bot.getLlmProviderId( ) ).orElse( null );
        if ( llmProvider == null )
        {
            String errorMsg = ERROR_LLM_PROVIDER_NOT_FOUND + bot.getBotName( );
            AppLogService.error( errorMsg );
            dispatchErrorEvent( operationId, botId, request.getUserId( ), errorMsg );
            _observabilityService.completeResourceExecutionError( executionId, errorMsg, null );
            return new StreamingQueryResponse( operationId, uuid );
        }

        StreamingQueryResponse response = new StreamingQueryResponse( operationId, uuid );

        dispatchRequestReceivedEvent( operationId, botId, request.getUserId( ), request.getQuery( ), uuid );
        processStreamingAsync( executionId, bot, llmProvider, operationId, streamingQueryServiceNodeId, request.getQuery( ), uuid, request.getUserId( ) );

        return response;
    }

    /**
     * Processes streaming asynchronously
     *
     * @param executionId
     *            the execution ID
     * @param bot
     *            the bot
     * @param llmProvider
     *            the LLM provider
     * @param operationId
     *            the operation ID
     * @param streamingQueryServiceNodeId
     *            the streaming query service node ID
     * @param query
     *            the query text
     * @param uuid
     *            the conversation UUID
     * @param userId
     *            the user ID
     */
    private void processStreamingAsync( String executionId, Bot bot, Provider llmProvider, String operationId, String streamingQueryServiceNodeId, String query,
            String uuid, String userId )
    {
        int botId = bot.getId( );
        CompletableFuture.runAsync( ( ) -> {
            final AtomicBoolean overallOperationCompleted = new AtomicBoolean( false );
            final Executor _ctxExecutor = _contextService.currentContextExecutor( );
            try
            {
                int currentOrder = 1;
                _observabilityService.startNodeExecution( executionId, streamingQueryServiceNodeId, OP_STREAMING_QUERY_PROCESSING, currentOrder++,
                        ObservabilityData.input( OP_STREAMING_QUERY_PROCESSING, DESC_PROCESSING_STREAMING + bot.getBotName( ) ) );

                String mainStreamingLlmNodeId = CostTrackingListener.NODE_STREAMING_MAIN_QUERY + UUID.randomUUID( );
                StreamingChatModel streamingChatModel = _modelService.createTrackedStreamingChatModel( llmProvider, executionId, mainStreamingLlmNodeId,
                        currentOrder++, bot.getTemperature( ) );

                List<Integer> botDatasetsId = BotDatasetHome.getDatasetIdsByBotId( botId );
                List<Dataset> botDatasets = new ArrayList<>( );
                for ( Integer datasetId : botDatasetsId )
                {
                    DatasetHome.findByPrimaryKey( datasetId ).ifPresent( botDatasets::add );
                }

                TokenStream tokenStream;

                CompositeToolProvider compositeToolProvider = new CompositeToolProvider( bot, botDatasets, operationId );

                if ( botDatasets.isEmpty( ) )
                {
                    tokenStream = processSimpleStreamingQuery( streamingChatModel, query, bot, uuid, userId, operationId, compositeToolProvider, _ctxExecutor );
                }
                else
                {
                    tokenStream = _ragService.processRagStreamingQuery( streamingChatModel, query, bot, uuid, botDatasets, userId, executionId, operationId,
                            currentOrder, compositeToolProvider, _ctxExecutor );
                }

                if ( tokenStream == null )
                {
                    String errorMsg = ERROR_TOKEN_STREAM + bot.getBotName( );
                    AppLogService.error( errorMsg );
                    handleStreamingError( executionId, streamingQueryServiceNodeId, operationId, botId, userId, errorMsg, overallOperationCompleted );
                    return;
                }

                final List<Content> ragProvidedContents = new ArrayList<>( );
                final StringBuilder accumulatedResponse = new StringBuilder( );

                tokenStream.onPartialResponse( token -> {
                    accumulatedResponse.append( token );
                    dispatchTokenEvent( operationId, botId, userId, token );
                } ).onRetrieved( sources -> {
                    if ( sources != null && !sources.isEmpty( ) )
                    {
                        ragProvidedContents.clear( );
                        ragProvidedContents.addAll( sources );
                    }
                } ).onCompleteResponse( response -> _ctxExecutor.execute( ( ) -> {
                    if ( overallOperationCompleted.compareAndSet( false, true ) )
                    {
                        String completeResponse = accumulatedResponse.toString( );

                        if ( !ragProvidedContents.isEmpty( ) )
                        {
                            extractAndDispatchCitedSources( completeResponse, ragProvidedContents, operationId, botId, userId );
                        }

                        _observabilityService.completeNodeExecutionSuccess( executionId, streamingQueryServiceNodeId,
                                ObservabilityData.output( MSG_STREAMING_COMPLETED, "" ) );
                        _observabilityService.completeResourceExecutionSuccess( executionId,
                                ObservabilityData.of( "BOT_RESPONSE", "success", true, "response", completeResponse ) );
                        Integer assistantMessageId = getLatestAssistantMessageId( uuid );
                        dispatchCompletedEvent( operationId, botId, userId, uuid, assistantMessageId );
                    }
                } ) ).onError( error -> _ctxExecutor.execute( ( ) -> {
                    String errorMsg = ERROR_DURING_STREAMING + bot.getBotName( ) + ": " + error.getMessage( );
                    AppLogService.error( errorMsg, error );
                    handleStreamingError( executionId, streamingQueryServiceNodeId, operationId, botId, userId, errorMsg, overallOperationCompleted );
                } ) ).start( );
            }
            catch( Exception e )
            {
                String errorMsg = ERROR_STREAMING_QUERY + bot.getBotName( ) + ": " + e.getMessage( );
                AppLogService.error( errorMsg, e );
                handleStreamingError( executionId, streamingQueryServiceNodeId, operationId, botId, userId, errorMsg, overallOperationCompleted );
            }
        }, _blockingExecutor ).exceptionally( throwable -> {
            String errorMsg = ERROR_ASYNC_PROCESSING_FAILED + throwable.getMessage( );
            AppLogService.error( errorMsg, throwable );
            dispatchErrorEvent( operationId, botId, userId, errorMsg );
            return null;
        } );
    }

    /**
     * Handles streaming errors
     *
     * @param executionId
     *            the execution ID
     * @param streamingQueryServiceNodeId
     *            the streaming query service node ID
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     * @param errorMsg
     *            the error message
     * @param overallOperationCompleted
     *            the overall operation completed flag
     */
    private void handleStreamingError( String executionId, String streamingQueryServiceNodeId, String operationId, int botId, String userId, String errorMsg,
            AtomicBoolean overallOperationCompleted )
    {
        if ( overallOperationCompleted.compareAndSet( false, true ) )
        {
            if ( executionId != null )
            {
                _observabilityService.completeNodeExecutionError( executionId, streamingQueryServiceNodeId, errorMsg, null );
                _observabilityService.completeResourceExecutionError( executionId, errorMsg,
                        ObservabilityData.of( "BOT_RESPONSE", "success", false, "response", errorMsg ) );
            }
        }
        dispatchErrorEvent( operationId, botId, userId, errorMsg );
    }

    /**
     * Processes a simple streaming query without RAG
     *
     * @param streamingChatModel
     *            the streaming chat model
     * @param query
     *            the query text
     * @param bot
     *            the bot
     * @param uuid
     *            the conversation UUID
     * @param userId
     *            the user ID
     * @param operationId
     *            the operation ID
     * @param toolProvider
     *            the tool provider exposing pipeline/builtin/MCP tools
     * @param ctxExecutor
     *            executor carrying the application context (TCCL/JNDI), used by langchain4j to run tool bodies
     * @return the token stream
     */
    private TokenStream processSimpleStreamingQuery( StreamingChatModel streamingChatModel, String query, Bot bot, String uuid, String userId,
            String operationId, dev.langchain4j.service.tool.ToolProvider toolProvider, Executor ctxExecutor )
    {
        ChatMemory chatMemory = _botMemoryService.createPersistentChatMemory( bot, uuid, userId );

        IBotAssistant assistant = AiServices.builder( IBotAssistant.class ).streamingChatModel( streamingChatModel ).chatMemory( chatMemory )
                .toolProvider( toolProvider ).executeToolsConcurrently( ctxExecutor )
                .hallucinatedToolNameStrategy( request -> ToolExecutionResultMessage.from( request, ERROR_HALLUCINATED_TOOL + request.name( ) ) ).build( );
        return assistant.chatStream( query );
    }

    /**
     * Dispatches a request received event
     *
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     * @param query
     *            the query text
     * @param conversationUuid
     *            the conversation UUID
     */
    private void dispatchRequestReceivedEvent( String operationId, int botId, String userId, String query, String conversationUuid )
    {
        _streamStartedEvent.fire( BotStreamStartedEvent.now( operationId, botId, query ) );
    }

    /**
     * Dispatches a token event
     *
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     * @param token
     *            the token
     */
    private void dispatchTokenEvent( String operationId, int botId, String userId, String token )
    {
        _tokenEvent.fire( BotTokenEvent.now( operationId, botId, token ) );
    }

    /**
     * Dispatches a completed event
     *
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     * @param conversationUuid
     *            the conversation UUID
     */
    private void dispatchCompletedEvent( String operationId, int botId, String userId, String conversationUuid, Integer assistantMessageId )
    {
        _streamCompletedEvent.fire( BotStreamCompletedEvent.now( operationId, botId, conversationUuid ) );
    }

    /**
     * Dispatches an error event
     *
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     * @param errorMessage
     *            the error message
     */
    private void dispatchErrorEvent( String operationId, int botId, String userId, String errorMessage )
    {
        _streamErrorEvent.fire( BotStreamErrorEvent.now( operationId, botId, errorMessage, null ) );
    }

    /**
     * Converts a source content to a map
     *
     * @param source
     *            the source content
     * @return the source as a map
     */
    private Map<String, Object> convertSourceToMap( Content source )
    {
        Map<String, Object> sourceMap = new HashMap<>( );
        if ( source != null && source.textSegment( ) != null )
        {
            sourceMap.put( SOURCE_TEXT_KEY, source.textSegment( ).text( ) );
            if ( source.textSegment( ).metadata( ) != null )
            {
                addStringMetadataIfPresent( sourceMap, source.textSegment( ).metadata( ), EmbeddingService.META_DOCUMENT_NAME );
                addStringMetadataIfPresent( sourceMap, source.textSegment( ).metadata( ), EmbeddingService.META_DATASET_NAME );
                addIntMetadataIfPresent( sourceMap, source.textSegment( ).metadata( ), EmbeddingService.META_DOCUMENT_ID );
                addIntMetadataIfPresent( sourceMap, source.textSegment( ).metadata( ), EmbeddingService.META_DATASET_ID );
            }
        }
        return sourceMap;
    }

    /**
     * Adds a string metadata value to the source map if present
     *
     * @param sourceMap
     *            the source map
     * @param metadata
     *            the metadata
     * @param key
     *            the metadata key
     */
    private void addStringMetadataIfPresent( Map<String, Object> sourceMap, dev.langchain4j.data.document.Metadata metadata, String key )
    {
        String value = metadata.getString( key );
        if ( value != null )
        {
            sourceMap.put( key, value );
        }
    }

    /**
     * Adds a numeric metadata value to the source map if present, parsed as an int. Falls back to the raw string value when the metadata is not numeric.
     *
     * @param sourceMap
     *            the source map
     * @param metadata
     *            the metadata
     * @param key
     *            the metadata key
     */
    private void addIntMetadataIfPresent( Map<String, Object> sourceMap, dev.langchain4j.data.document.Metadata metadata, String key )
    {
        String value = metadata.getString( key );
        if ( value == null )
        {
            return;
        }
        try
        {
            sourceMap.put( key, Integer.parseInt( value.trim( ) ) );
        }
        catch( NumberFormatException e )
        {
            AppLogService.error( "Non-numeric value '{}' for metadata key '{}', keeping raw value", value, key );
            sourceMap.put( key, value );
        }
    }

    /**
     * Gets the conversation history
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user ID
     * @return the list of messages in the conversation
     */
    public List<MessageDTO> getConversationHistory( String conversationUuid, String userId )
    {
        if ( conversationUuid == null || conversationUuid.isEmpty( ) )
        {
            AppLogService.error( ERROR_CONVERSATION_UUID_NULL );
            return new ArrayList<>( );
        }

        List<MessageDTO> result = new ArrayList<>( );
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation == null )
        {
            AppLogService.error( "Conversation not found: {}", conversationUuid );
            return result;
        }

        List<ConversationMessage> conversationMessages = ConversationMessageHome.getMessagesByConversationId( conversation.getId( ) );

        for ( ConversationMessage msg : conversationMessages )
        {
            if ( "tool".equals( msg.getRole( ) ) )
            {
                continue;
            }
            if ( "assistant".equals( msg.getRole( ) ) && msg.getMessage( ) != null && msg.getMessage( ).startsWith( "[TOOL_REQUESTS]" ) )
            {
                continue;
            }

            FeedbackDTO feedbackDTO = null;
            ConversationMessageFeedback feedback = feedbackService.getFeedbackByMessageId( msg.getId( ) ).orElse( null );
            if ( feedback != null )
            {
                feedbackDTO = new FeedbackDTO( feedback.getId( ), feedback.getMessageId( ), feedback.getUserId( ), feedback.getBotId( ), feedback.isPositive( ),
                        feedback.getComment( ), feedback.getStatus( ).getValue( ), feedback.getCreatedAt( ), feedback.getUpdatedAt( ) );
            }

            result.add( new MessageDTO( msg.getId( ), msg.getMessage( ), msg.getRole( ), feedbackDTO, msg.getCreatedAt( ) ) );
        }

        return result;
    }

    /**
     * Resolves the full conversation timeline for the front-office: looks up the conversation by UUID, fetches its message history, resolves the owning bot,
     * runs the RBAC view check and enriches the bot with the user permissions. The result is a typed view the controller only has to place in the model.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param user
     *            the authenticated user
     * @return a {@link ConversationView}; its bot is {@code null} and access is denied when the conversation or bot cannot be resolved
     */
    public ConversationView loadConversationView( String conversationUuid, LuteceUser user )
    {
        if ( conversationUuid == null || conversationUuid.isEmpty( ) )
        {
            return new ConversationView( conversationUuid, null, new ArrayList<>( ), false );
        }

        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation == null )
        {
            return new ConversationView( conversationUuid, null, new ArrayList<>( ), false );
        }

        List<MessageDTO> messages = getConversationHistory( conversationUuid, conversation.getUserId( ) );

        Bot bot = BotHome.findByPrimaryKey( conversation.getBotId( ) ).orElse( null );
        if ( bot == null )
        {
            return new ConversationView( conversationUuid, null, messages, false );
        }

        if ( !AgentRBACService.canViewBot( bot, user ) )
        {
            return new ConversationView( conversationUuid, null, messages, false );
        }

        AgentRBACService.enrichWithPermissions( bot, user );
        return new ConversationView( conversationUuid, bot, messages, true );
    }

    /**
     * Resolves a bot by its identifier and enriches it with the RBAC permissions of the given user, so the controller can read {@code isUserCanModify} and
     * friends without orchestrating the lookup itself.
     *
     * @param botId
     *            the bot identifier
     * @param user
     *            the authenticated user
     * @return the RBAC-enriched bot, or {@code null} when no bot matches the identifier
     */
    public Bot loadEnrichedBot( int botId, LuteceUser user )
    {
        Bot bot = BotHome.findByPrimaryKey( botId ).orElse( null );
        if ( bot == null )
        {
            return null;
        }
        AgentRBACService.enrichWithPermissions( bot, user );
        return bot;
    }

    /**
     * Extracts conversation history as a simple list of maps for observability
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user ID
     * @return a list of maps containing role and content for each message
     */
    private List<Map<String, String>> extractConversationHistory( String conversationUuid, String userId )
    {
        List<Map<String, String>> history = new ArrayList<>( );
        List<MessageDTO> messages = getConversationHistory( conversationUuid, userId );
        for ( MessageDTO msg : messages )
        {
            Map<String, String> entry = new HashMap<>( );
            entry.put( "role", msg.getRole( ) );
            entry.put( "content", msg.getMessage( ) != null ? msg.getMessage( ) : "" );
            history.add( entry );
        }
        return history;
    }

    /**
     * Returns the identifier of the most recent assistant message in the given conversation.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @return the latest assistant message identifier, or {@code null} if none exists
     */
    private Integer getLatestAssistantMessageId( String conversationUuid )
    {
        try
        {
            BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
            if ( conversation != null )
            {
                List<ConversationMessage> messages = ConversationMessageHome.getMessagesByConversationId( conversation.getId( ) );
                OptionalInt maxId = messages.stream( ).filter( msg -> ConversationMessage.ROLE_ASSISTANT.equals( msg.getRole( ) ) )
                        .mapToInt( ConversationMessage::getId ).max( );
                return maxId.isPresent( ) ? maxId.getAsInt( ) : null;
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error retrieving the assistant message ID for conversation: {}", conversationUuid, e );
        }
        return null;
    }

    /**
     * Extracts cited sources and dispatches them as an event.
     *
     * @param response
     *            the complete response from the LLM
     * @param providedContents
     *            the contents that were provided to the LLM
     * @param operationId
     *            the operation ID
     * @param botId
     *            the bot ID
     * @param userId
     *            the user ID
     */
    private void extractAndDispatchCitedSources( String response, List<Content> providedContents, String operationId, int botId, String userId )
    {
        try
        {
            CitationTracker tracker = new CitationTracker( );
            Map<Integer, Content> citedSourcesMap = tracker.extractCitedContentsWithNumbers( response, providedContents );
            if ( citedSourcesMap.isEmpty( ) )
            {
                return;
            }

            List<Object> citedSourcesList = new ArrayList<>( );

            for ( Map.Entry<Integer, Content> entry : citedSourcesMap.entrySet( ) )
            {
                int sourceNumber = entry.getKey( );
                Content cited = entry.getValue( );
                Map<String, Object> sourceData = convertSourceToMap( cited );
                sourceData.put( "source_number", sourceNumber );
                citedSourcesList.add( sourceData );
            }

            _sourcesRetrievedEvent.fire( BotSourcesRetrievedEvent.now( operationId, botId, citedSourcesList ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error extracting citations", e );
        }
    }

}
