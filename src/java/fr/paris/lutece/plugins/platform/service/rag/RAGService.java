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
package fr.paris.lutece.plugins.platform.service.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.IBotAssistant;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.bot.BotMemoryService;
import fr.paris.lutece.plugins.platform.service.model.CostTrackingListener;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.rag.aggregator.LimitedContentAggregator;
import fr.paris.lutece.plugins.platform.service.rag.citation.CitationAwareContentInjector;
import fr.paris.lutece.plugins.platform.service.rag.router.DatasetQueryRouter;
import fr.paris.lutece.plugins.platform.service.rag.transformer.CompositeQueryTransformer;
import fr.paris.lutece.plugins.platform.service.rag.transformer.HydeQueryTransformer;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for handling Retrieval-Augmented Generation (RAG) operations. This service manages query processing, content retrieval, and streaming responses for
 * AI assistants using dataset-based knowledge augmentation.
 */
@ApplicationScoped
@Named( "platform.ragService" )
public class RAGService
{

    private static final int DEFAULT_MAX_RESULTS = 15;
    private static final double DEFAULT_MIN_SCORE = 0.7;

    private static final String ERROR_STREAMING_BOT_NOT_FOUND = "RAGService (Streaming): Bot not found with id ";
    private static final String ERROR_STREAMING_LLM_PROVIDER_NOT_FOUND = "RAGService (Streaming): LLM provider not found for bot ";
    private static final String ERROR_EMBEDDING_PROVIDER_NOT_FOUND = "Embedding provider not found for dataset ";
    private static final String ERROR_RAG_STREAMING_PROCESS = "Error setting up RAG streaming process for executionId ";
    private static final String ERROR_CONTENT_RETRIEVER_SETUP = "Failed to setup content retriever for ";
    private static final String ERROR_NO_CONTENT_RETRIEVERS = "RAGService: No content retrievers were successfully set up for bot, despite having datasets. ExecutionId: ";
    private static final String ERROR_HALLUCINATED_TOOL = "Error: there is no tool called ";

    private static final String DATASET_LOG_FORMAT = "Dataset: %s (ID: %d)";

    @Inject
    private BotMemoryService _botMemoryService;

    @Inject
    private ModelService _modelService;

    @Inject
    private ElasticsearchService _elasticsearchService;

    RAGService( )
    {
    }

    /**
     * Processes a RAG streaming query using the provided streaming chat model and bot configuration.
     *
     * @param streamingChatModel
     *            the streaming chat language model to use
     * @param query
     *            the user query to process
     * @param bot
     *            the bot configuration
     * @param conversationUuid
     *            the unique conversation identifier
     * @param botDatasets
     *            the list of datasets associated with the bot
     * @param userId
     *            the user identifier
     * @param executionId
     *            the execution identifier for tracking
     * @param operationId
     *            the operation identifier
     * @param startOrder
     *            the starting order for cost tracking
     * @param toolProvider
     *            the tool provider exposing pipeline/builtin/MCP tools
     * @param ctxExecutor
     *            executor carrying the application context (TCCL/JNDI) on which langchain4j runs tool bodies
     * @return a TokenStream for streaming responses
     * @throws Exception
     *             if an error occurs during query processing
     */
    public TokenStream processRagStreamingQuery( StreamingChatModel streamingChatModel, String query, Bot bot, String conversationUuid,
            List<Dataset> botDatasets, String userId, String executionId, String operationId, int startOrder,
            dev.langchain4j.service.tool.ToolProvider toolProvider, java.util.concurrent.Executor ctxExecutor ) throws Exception
    {
        try
        {
            RetrievalAugmentor retrievalAugmentor = setupRetrievalAugmentor( bot.getId( ), executionId, operationId, botDatasets,
                    new AtomicInteger( startOrder ), userId );
            ChatMemory chatMemory = _botMemoryService.createPersistentChatMemory( bot, conversationUuid, userId );
            IBotAssistant assistant = buildBotAssistant( streamingChatModel, chatMemory, retrievalAugmentor, toolProvider, ctxExecutor );
            return assistant.chatStream( query );
        }
        catch( Exception e )
        {
            String errorMsg = ERROR_RAG_STREAMING_PROCESS + executionId + ": " + e.getMessage( );
            AppLogService.error( errorMsg, e );
            throw e;
        }
    }

    /**
     * Builds a bot assistant with the provided models and augmentors.
     *
     * @param streamingChatModel
     *            the streaming chat language model
     * @param chatMemory
     *            the chat memory instance
     * @param retrievalAugmentor
     *            the retrieval augmentor
     * @param toolProvider
     *            the tool provider exposing pipeline/builtin/MCP tools, or null
     * @param ctxExecutor
     *            executor carrying the application context (TCCL/JNDI) on which langchain4j runs tool bodies, or null
     * @return the configured bot assistant
     */
    private IBotAssistant buildBotAssistant( StreamingChatModel streamingChatModel, ChatMemory chatMemory, RetrievalAugmentor retrievalAugmentor,
            dev.langchain4j.service.tool.ToolProvider toolProvider, java.util.concurrent.Executor ctxExecutor )
    {
        AiServices<IBotAssistant> builder = AiServices.builder( IBotAssistant.class ).streamingChatModel( streamingChatModel ).chatMemory( chatMemory )
                .retrievalAugmentor( retrievalAugmentor )
                .hallucinatedToolNameStrategy( request -> ToolExecutionResultMessage.from( request, ERROR_HALLUCINATED_TOOL + request.name( ) ) );
        if ( toolProvider != null )
        {
            builder.toolProvider( toolProvider );
        }
        if ( ctxExecutor != null )
        {
            builder.executeToolsConcurrently( ctxExecutor );
        }
        return builder.build( );
    }

    /**
     * Sets up the retrieval augmentor with query transformation, routing, and content injection.
     *
     * @param botId
     *            the bot identifier
     * @param executionId
     *            the execution identifier
     * @param operationId
     *            the operation identifier
     * @param botDatasets
     *            the list of bot datasets
     * @param currentOrder
     *            the current order counter
     * @param userId
     *            the user identifier
     * @return the configured retrieval augmentor
     * @throws Exception
     *             if setup fails
     */
    private RetrievalAugmentor setupRetrievalAugmentor( int botId, String executionId, String operationId, List<Dataset> botDatasets,
            AtomicInteger currentOrder, String userId ) throws Exception
    {

        String llmRouterNodeId = CostTrackingListener.NODE_STREAMING_ROUTER + UUID.randomUUID( ).toString( );

        Bot bot = findBotById( botId );
        Provider llmProvider = findLlmProvider( bot, botId );

        QueryTransformer queryTransformer = createQueryTransformerWithEnhancement( bot, llmProvider, executionId, currentOrder );
        DatasetRetrieverMapping mapping = setupContentRetrieversWithMapping( botDatasets, executionId, bot );
        QueryRouter queryRouter = createQueryRouter( llmProvider, mapping, botDatasets, botId, userId, executionId, operationId, llmRouterNodeId, currentOrder,
                bot.getTemperature( ) );
        ContentInjector customContentInjector = createContentInjector( );

        DefaultRetrievalAugmentor.DefaultRetrievalAugmentorBuilder builder = DefaultRetrievalAugmentor.builder( ).queryTransformer( queryTransformer )
                .queryRouter( queryRouter ).contentInjector( customContentInjector );

        if ( bot.isEnableContentAggregation( ) )
        {
            ContentAggregator contentAggregator = new LimitedContentAggregator( bot.getAggregationMaxResults( ) );
            builder.contentAggregator( contentAggregator );
        }

        return builder.build( );
    }

    /**
     * Finds a bot by its identifier.
     *
     * @param botId
     *            the bot identifier
     * @return the bot instance
     * @throws Exception
     *             if bot is not found
     */
    private Bot findBotById( int botId ) throws Exception
    {
        return BotHome.findByPrimaryKey( botId ).orElseThrow( ( ) -> new Exception( ERROR_STREAMING_BOT_NOT_FOUND + botId ) );
    }

    /**
     * Finds the LLM provider for a given bot.
     *
     * @param bot
     *            the bot instance
     * @param botId
     *            the bot identifier for error messages
     * @return the LLM provider
     * @throws Exception
     *             if provider is not found
     */
    private Provider findLlmProvider( Bot bot, int botId ) throws Exception
    {
        return ProviderHome.findByPrimaryKey( bot.getLlmProviderId( ) ).orElseThrow( ( ) -> new Exception( ERROR_STREAMING_LLM_PROVIDER_NOT_FOUND + botId ) );
    }

    /**
     * Creates a query transformer with enhancement based on bot configuration.
     *
     * @param bot
     *            the bot configuration
     * @param llmProvider
     *            the LLM provider
     * @param executionId
     *            the execution identifier
     * @param currentOrder
     *            the current order counter
     * @return the configured query transformer
     */
    private QueryTransformer createQueryTransformerWithEnhancement( Bot bot, Provider llmProvider, String executionId, AtomicInteger currentOrder )
    {

        String compressorNodeId = CostTrackingListener.NODE_STREAMING_TRANSFORMER + UUID.randomUUID( ).toString( );
        ChatModel compressorModel = _modelService.createTrackedChatModel( llmProvider, executionId, compressorNodeId, currentOrder.getAndIncrement( ),
                bot.getTemperature( ) );
        QueryTransformer compressor = CompressingQueryTransformer.builder( ).chatModel( compressorModel ).build( );

        QueryEnhancementMode mode = QueryEnhancementMode.fromCode( bot.getQueryEnhancementMode( ) );

        if ( mode == QueryEnhancementMode.NONE )
        {
            return compressor;
        }

        QueryTransformer expander = null;
        if ( mode.includesExpansion( ) )
        {
            String expanderNodeId = "query-expander-" + UUID.randomUUID( ).toString( );
            ChatModel expanderModel = _modelService.createTrackedChatModel( llmProvider, executionId, expanderNodeId, currentOrder.getAndIncrement( ),
                    bot.getTemperature( ) );
            expander = ExpandingQueryTransformer.builder( ).chatModel( expanderModel ).n( bot.getQueryExpansionVariants( ) ).build( );
        }

        QueryTransformer hydeTransformer = null;
        if ( mode.includesHyde( ) )
        {
            String hydeNodeId = "query-hyde-" + UUID.randomUUID( ).toString( );
            ChatModel hydeModel = _modelService.createTrackedChatModel( llmProvider, executionId, hydeNodeId, currentOrder.getAndIncrement( ),
                    bot.getTemperature( ) );

            hydeTransformer = HydeQueryTransformer.builder( ).chatModel( hydeModel ).maxTokens( bot.getHydeMaxTokens( ) )
                    .promptTemplate( bot.getHydePromptTemplate( ) ).build( );
        }

        return new CompositeQueryTransformer( compressor, expander, hydeTransformer, mode, executionId, currentOrder.getAndIncrement( ) );
    }

    /**
     * Creates a query router for dataset-based routing.
     *
     * @param llmProvider
     *            the LLM provider
     * @param mapping
     *            the dataset retriever mapping
     * @param botDatasets
     *            the list of bot datasets
     * @param botId
     *            the bot identifier
     * @param userId
     *            the user identifier
     * @param executionId
     *            the execution identifier
     * @param operationId
     *            the operation identifier
     * @param nodeId
     *            the node identifier for tracking
     * @param currentOrder
     *            the current order counter
     * @param temperature
     *            the temperature parameter for the model
     * @return the configured query router
     */
    private QueryRouter createQueryRouter( Provider llmProvider, DatasetRetrieverMapping mapping, List<Dataset> botDatasets, int botId, String userId,
            String executionId, String operationId, String nodeId, AtomicInteger currentOrder, double temperature )
    {
        ChatModel routerModel = _modelService.createTrackedChatModel( llmProvider, executionId, nodeId, currentOrder.getAndIncrement( ), temperature );
        return new DatasetQueryRouter( routerModel, mapping.getRetrieverToDescription( ), botDatasets, botId, userId, executionId, operationId,
                mapping.getRetrieverToDataset( ) );
    }

    /**
     * Creates a custom content injector with metadata handling.
     *
     * @return the configured content injector
     */
    private ContentInjector createContentInjector( )
    {
        return new CitationAwareContentInjector( );
    }

    /**
     * Sets up content retrievers with mapping for the provided datasets.
     *
     * @param botDatasets
     *            the list of bot datasets
     * @param executionId
     *            the execution identifier
     * @param bot
     *            the bot configuration
     * @return the dataset retriever mapping
     * @throws Exception
     *             if setup fails
     */
    private DatasetRetrieverMapping setupContentRetrieversWithMapping( List<Dataset> botDatasets, String executionId, Bot bot ) throws Exception
    {
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>( );
        Map<ContentRetriever, Dataset> retrieverToDataset = new HashMap<>( );

        if ( botDatasets.isEmpty( ) )
        {
            return new DatasetRetrieverMapping( retrieverToDescription, retrieverToDataset );
        }

        processDatasets( botDatasets, retrieverToDescription, retrieverToDataset, bot );

        if ( retrieverToDescription.isEmpty( ) && !botDatasets.isEmpty( ) )
        {
            AppLogService.error( "{}{}", ERROR_NO_CONTENT_RETRIEVERS, executionId );
        }

        return new DatasetRetrieverMapping( retrieverToDescription, retrieverToDataset );
    }

    /**
     * Processes datasets to create content retrievers and mappings.
     *
     * @param botDatasets
     *            the list of bot datasets
     * @param retrieverToDescription
     *            the mapping of retrievers to descriptions
     * @param retrieverToDataset
     *            the mapping of retrievers to datasets
     * @param bot
     *            the bot configuration
     */
    private void processDatasets( List<Dataset> botDatasets, Map<ContentRetriever, String> retrieverToDescription,
            Map<ContentRetriever, Dataset> retrieverToDataset, Bot bot )
    {
        for ( Dataset botDataset : botDatasets )
        {
            String datasetInfoForTrace = String.format( DATASET_LOG_FORMAT, botDataset.getDatasetName( ), botDataset.getId( ) );
            try
            {
                ContentRetriever contentRetriever = createContentRetriever( botDataset, bot );
                String description = createDatasetDescription( botDataset );
                retrieverToDescription.put( contentRetriever, description );
                retrieverToDataset.put( contentRetriever, botDataset );
            }
            catch( Exception e )
            {
                String errorMsg = ERROR_CONTENT_RETRIEVER_SETUP + datasetInfoForTrace + ": " + e.getMessage( );
                AppLogService.error( errorMsg, e );
            }
        }
    }

    /**
     * Creates a content retriever for the specified dataset.
     *
     * @param botDataset
     *            the dataset to create retriever for
     * @param bot
     *            the bot configuration
     * @return the configured content retriever
     * @throws Exception
     *             if retriever creation fails
     */
    private ContentRetriever createContentRetriever( Dataset botDataset, Bot bot ) throws Exception
    {
        Provider embedProvider = ProviderHome.findByPrimaryKey( botDataset.getEmbedProviderId( ) )
                .orElseThrow( ( ) -> new Exception( ERROR_EMBEDDING_PROVIDER_NOT_FOUND + botDataset.getDatasetName( ) ) );

        EmbeddingModel embeddingModel = _modelService.createEmbeddingModel( embedProvider );
        EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( botDataset.getId( ) );

        return EmbeddingStoreContentRetriever.builder( ).embeddingStore( embeddingStore ).embeddingModel( embeddingModel )
                .maxResults( bot.getSemanticSearchMaxResults( ) > 0 ? bot.getSemanticSearchMaxResults( ) : DEFAULT_MAX_RESULTS )
                .minScore( bot.getSemanticSearchMinScore( ) > 0 ? bot.getSemanticSearchMinScore( ) : DEFAULT_MIN_SCORE ).build( );
    }

    /**
     * Creates a description for the dataset combining name, description, and routing rules.
     *
     * @param botDataset
     *            the dataset to create description for
     * @return the dataset description
     */
    private String createDatasetDescription( Dataset botDataset )
    {
        String description = botDataset.getDatasetName( );
        if ( botDataset.getDatasetDescription( ) != null && !botDataset.getDatasetDescription( ).isEmpty( ) )
        {
            description += ": " + botDataset.getDatasetDescription( );
        }
        if ( botDataset.getDatasetRoutingRules( ) != null && !botDataset.getDatasetRoutingRules( ).isEmpty( ) )
        {
            description += " [Règles de routage: " + botDataset.getDatasetRoutingRules( ) + "]";
        }
        return description;
    }

    /**
     * Inner class that holds mappings between content retrievers and their associated data.
     */
    private static class DatasetRetrieverMapping
    {
        private final Map<ContentRetriever, String> retrieverToDescription;
        private final Map<ContentRetriever, Dataset> retrieverToDataset;

        /**
         * Creates a new DatasetRetrieverMapping with the specified mappings.
         *
         * @param retrieverToDescription
         *            mapping of retrievers to descriptions
         * @param retrieverToDataset
         *            mapping of retrievers to datasets
         */
        public DatasetRetrieverMapping( Map<ContentRetriever, String> retrieverToDescription, Map<ContentRetriever, Dataset> retrieverToDataset )
        {
            this.retrieverToDescription = retrieverToDescription;
            this.retrieverToDataset = retrieverToDataset;
        }

        /**
         * Gets the mapping of content retrievers to their descriptions.
         *
         * @return the retriever to description mapping
         */
        public Map<ContentRetriever, String> getRetrieverToDescription( )
        {
            return retrieverToDescription;
        }

        /**
         * Gets the mapping of content retrievers to their datasets.
         *
         * @return the retriever to dataset mapping
         */
        public Map<ContentRetriever, Dataset> getRetrieverToDataset( )
        {
            return retrieverToDataset;
        }
    }
}
