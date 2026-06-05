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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJob;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJobHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.model.CostTrackingListener;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.business.provider.ProviderVendorConstants;
import fr.paris.lutece.plugins.platform.service.rag.parser.AzureParser;
import fr.paris.lutece.plugins.platform.service.rag.parser.MistralOcrParser;
import fr.paris.lutece.plugins.platform.service.rag.parser.PdfParserWithOCR;
import fr.paris.lutece.plugins.platform.service.event.domain.EmbeddingJobSubmittedEvent;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.plugins.platform.service.concurrent.BlockingIO;
import fr.paris.lutece.plugins.platform.service.concurrent.Scheduler;
import jakarta.annotation.PreDestroy;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.servlet.ServletContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for managing document embeddings. Handles the process of embedding documents into vector stores for retrieval.
 */
@ApplicationScoped
@Named( "platform.embeddingService" )
public class EmbeddingService
{
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private static final String LOG_SERVICE_STARTED = "[EmbeddingService] started with ";
    private static final String LOG_JOB_NOT_FOUND = "[EmbeddingService] Job not found: ";
    private static final String LOG_DOCUMENT_NOT_FOUND = "[EmbeddingService] Document not found: ";
    private static final String LOG_DATASET_NOT_FOUND = "[EmbeddingService] Dataset not found: ";
    private static final String LOG_PROVIDER_NOT_FOUND = "[EmbeddingService] Embedding provider not found: ";
    private static final String LOG_FILE_LOAD_ERROR = "[EmbeddingService] Unable to load file: ";
    private static final String LOG_SEGMENT_ERROR = "[EmbeddingService] Error embedding segment ";
    private static final String LOG_CRITICAL_ERROR = "[EmbeddingService] Critical error while processing job ";
    private static final String LOG_SHUTTING_DOWN = "[EmbeddingService] Shutting down...";
    private static final String LOG_SHUTDOWN_COMPLETE = "[EmbeddingService] Shutdown complete.";
    private static final String LOG_OCR_PROVIDER_ERROR = "[EmbeddingService] Unable to retrieve LLM provider for OCR: ";
    private static final String LOG_DOCUMENT_JOB_CONTEXT = " for job ";
    private static final String LOG_DATASET_JOB_CONTEXT = " for dataset ";

    public static final String META_DOCUMENT_ID = "document_id";
    public static final String META_DOCUMENT_NAME = "document_name";
    public static final String META_DATASET_ID = "dataset_id";
    public static final String META_DATASET_NAME = "dataset_name";
    public static final String META_SEGMENT_INDEX = "segment_index";
    public static final String META_FOLDER_ID = "folder_id";

    /**
     * Metadata key carrying the analyzed page count of a parsed document.
     */
    public static final String META_PAGE_COUNT = "page_count";

    /**
     * Metadata key carrying the model used for document analysis.
     */
    public static final String META_ANALYSIS_MODEL = "analysis_model";

    /**
     * Metadata key carrying the cost of the document analysis.
     */
    public static final String META_ANALYSIS_COST = "analysis_cost";

    private static final String OBS_DOC_EMBEDDING_OPERATION = "Document Embedding Operation (";
    private static final String OBS_EMBEDDING_SEGMENTS = "Embedding ";
    private static final String OBS_SEGMENT_ERROR = "Segment Embedding Error";
    private static final String OBS_FAILED_EMBED_SEGMENT = "Failed to embed segment ";
    private static final String OBS_TOKEN_USAGE = "Token Usage & Cost";
    private static final String OBS_TOKEN_FORMAT = "Input Tokens: %d, Estimated Cost: %s €";
    private static final String OBS_ALL_SEGMENTS_SUCCESS = "All ";
    private static final String OBS_SEGMENTS_SUCCESS_SUFFIX = " segments embedded successfully. Total input tokens: ";
    private static final String OBS_PARTIAL_EMBED_FORMAT = "Partially embedded: %d/%d segments. Total input tokens for successful: %d";
    private static final String OBS_EMBED_FAIL = "Failed to embed any segments for document ";
    private static final String OBS_CRITICAL_ERROR = "Critical error: ";
    private static final String OBS_SEGMENTS_LABEL = " segments for document: ";
    private static final String OBS_SEGMENT_DOC_PREFIX = " for doc ";
    private static final String OBS_DOC_ERROR_SEPARATOR = ": ";

    private static final String NODE_ID_PREFIX = "embedding-model-call-doc-";
    private static final String NODE_ID_JOB_SEPARATOR = "-job-";
    private static final String NODE_ID_UUID_SEPARATOR = "-";
    private static final int UUID_SUBSTRING_LENGTH = 8;
    private static final String EXT_PDF = ".pdf";
    private static final String EXT_DOC = ".doc";
    private static final String EXT_DOCX = ".docx";
    private static final String EXT_PPT = ".ppt";
    private static final String EXT_PPTX = ".pptx";
    private static final String EXT_XLS = ".xls";
    private static final String EXT_XLSX = ".xlsx";

    private static final String QUEUE_FAILURE_MESSAGE = "Embedding failure, check logs";
    private static final double MILLION_TOKENS = 1_000_000.0;

    private static final long STALE_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final int MAX_ATTEMPTS = 3;
    private static final int EMBEDDING_BATCH_SIZE = 96;
    private static final String LOG_BATCH_ERROR = "Batch embedding failed at offset {} for document {} — falling back to per-segment embedding";
    private static final long RECOVERY_INITIAL_DELAY_SECONDS = 120L;
    private static final long RECOVERY_PERIOD_SECONDS = 300L;

    @Inject
    @BlockingIO
    private ManagedExecutorService _blockingExecutor;

    @Inject
    @Scheduler
    private ManagedScheduledExecutorService _scheduler;

    private ScheduledFuture<?> _recoveryFuture;

    @Inject
    private Event<EmbeddingJobSubmittedEvent> _jobSubmittedEvent;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private ModelService _modelService;

    @Inject
    private ElasticsearchService _elasticsearchService;

    @Inject
    private AzureParser _azureParser;

    @Inject
    private MistralOcrParser _mistralOcrParser;

    private volatile boolean _stopped = false;

    /**
     * Default constructor for CDI.
     */
    EmbeddingService( )
    {
    }

    /**
     * Runs a first recovery/replay pass and schedules it periodically on the managed scheduler. The pass re-queues stale {@code processing} jobs and replays
     * {@code pending} jobs through the async observers.
     */
    @PostConstruct
    void init( )
    {
        recoverAndReplay( );
        _recoveryFuture = _scheduler.scheduleAtFixedRate( this::recoverAndReplay, RECOVERY_INITIAL_DELAY_SECONDS, RECOVERY_PERIOD_SECONDS, TimeUnit.SECONDS );
        AppLogService.info( "{}event-driven async observers (BlockingIO Virtual Threads)", LOG_SERVICE_STARTED );
    }

    /**
     * Eagerly triggers {@link #init()} when the web application context starts. Without this observer, {@code @ApplicationScoped} beans are lazily instantiated
     * on first lookup — pending jobs would only be replayed when something else first hits this service.
     *
     * <p>
     * The event payload is typed {@link ServletContext} (not {@code Object}) so the observer only fires inside a real web container — matching lutece-core's
     * {@code AppInitListener}. In a Java SE unit-test container there is no {@code ServletContext}, so the recovery work is skipped until the core is
     * initialized, avoiding a startup ordering crash against an uninitialized {@code PluginService}.
     * </p>
     *
     * @param context
     *            the servlet context of the starting web application (content unused)
     */
    void onApplicationStart( @Observes @Initialized( ApplicationScoped.class ) ServletContext context )
    {
    }

    /**
     * Recovers jobs stuck in {@code processing} past the timeout (re-queued or failed after max attempts), then fires a submission event for every
     * {@code pending} job. The per-job claim in {@link #onEmbeddingJobSubmitted} guarantees each job is processed once even when several instances replay
     * concurrently.
     */
    private void recoverAndReplay( )
    {
        if ( _stopped )
        {
            return;
        }
        try
        {
            DatasetDocumentJobHome.requeueStaleJobs( STALE_TIMEOUT_MS, MAX_ATTEMPTS );
            for ( DatasetDocumentJob job : DatasetDocumentJobHome.getJobsByStatus( DatasetDocumentJob.STATUS_PENDING ) )
            {
                fireJobEvent( job.getId( ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "[EmbeddingService] Recovery/replay cycle failed", e );
        }
    }

    /**
     * Fires an async embedding submission event scheduled on the BlockingIO Virtual Thread executor.
     *
     * @param jobId
     *            the embedding job identifier
     */
    private void fireJobEvent( int jobId )
    {
        _jobSubmittedEvent.fireAsync( EmbeddingJobSubmittedEvent.now( jobId ), NotificationOptions.ofExecutor( _blockingExecutor ) );
    }

    /**
     * Async observer invoked by the CDI container on the BlockingIO Virtual Thread executor whenever an embedding job is submitted. Replaces the legacy
     * 5-worker polling pool — concurrency now scales with the executor.
     *
     * @param event
     *            the embedding submission event
     */
    public void onEmbeddingJobSubmitted( @ObservesAsync EmbeddingJobSubmittedEvent event )
    {
        if ( _stopped )
        {
            return;
        }
        int jobId = event.jobId( );
        if ( !DatasetDocumentJobHome.claim( jobId ) )
        {
            return;
        }
        boolean ok = false;
        try
        {
            ok = processJob( jobId );
        }
        catch( Exception e )
        {
            AppLogService.error( "[EmbeddingService] Unexpected error processing job {}", jobId, e );
        }
        DatasetDocumentJobHome.updateStatus( jobId, ok ? DatasetDocumentJob.STATUS_COMPLETED : DatasetDocumentJob.STATUS_ERROR,
                ok ? null : QUEUE_FAILURE_MESSAGE );
    }

    /**
     * Creates a new embedding job for a document and submits it to the queue.
     *
     * @param document
     *            The document to embed
     * @return The created job
     */
    public DatasetDocumentJob createEmbeddingJob( DatasetDocument document )
    {
        DatasetDocumentJob job = new DatasetDocumentJob( );
        job.setDocumentId( document.getId( ) );
        job.setDatasetId( document.getDatasetId( ) );
        DatasetDocumentJobHome.create( job );
        fireJobEvent( job.getId( ) );
        return job;
    }

    /**
     * Gets the appropriate document parser based on the file extension.
     *
     * @param fileName
     *            the name of the file to parse
     * @param dataset
     *            the dataset configuration
     * @param executionId
     *            the execution ID for observability tracking
     * @param baseNodeId
     *            the base node ID for cost tracking
     * @return the appropriate document parser instance
     */
    private DocumentParser getDocumentParser( String fileName, Dataset dataset, String executionId, String baseNodeId )
    {
        if ( fileName == null || fileName.isEmpty( ) )
        {
            return new TextDocumentParser( );
        }

        String lowerCaseName = fileName.toLowerCase( );

        if ( lowerCaseName.endsWith( EXT_PDF ) )
        {
            boolean canDoOCR = dataset.getLlmProviderId( ) > 0;
            if ( canDoOCR )
            {
                try
                {
                    Optional<Provider> optLlmProvider = ProviderHome.findByPrimaryKey( dataset.getLlmProviderId( ) );
                    if ( optLlmProvider.isPresent( ) )
                    {
                        Provider llmProvider = optLlmProvider.get( );
                        return new PdfParserWithOCR( llmProvider, true, executionId, baseNodeId );
                    }
                }
                catch( Exception e )
                {
                    AppLogService.error( "{}{}", LOG_OCR_PROVIDER_ERROR, e.getMessage( ), e );
                }
            }
            return new ApachePdfBoxDocumentParser( );
        }
        else if ( lowerCaseName.endsWith( EXT_DOC ) || lowerCaseName.endsWith( EXT_DOCX ) || lowerCaseName.endsWith( EXT_PPT )
                || lowerCaseName.endsWith( EXT_PPTX ) || lowerCaseName.endsWith( EXT_XLS ) || lowerCaseName.endsWith( EXT_XLSX ) )
        {
            return new ApachePoiDocumentParser( );
        }
        else
        {
            return new TextDocumentParser( );
        }
    }

    /**
     * Processes an embedding job by retrieving the document, splitting it into segments, and embedding each segment into the vector store.
     *
     * @param jobId
     *            The ID of the job to process
     * @return true if the job was processed successfully, false otherwise
     */
    private boolean processJob( int jobId )
    {
        String executionId = null;
        String embeddingNodeId = null;
        DatasetDocumentJob job;
        DatasetDocument datasetDocument;
        Dataset dataset;
        Provider embedProvider;

        try
        {
            Optional<DatasetDocumentJob> optJob = DatasetDocumentJobHome.findByPrimaryKey( jobId );
            if ( optJob.isEmpty( ) )
            {
                AppLogService.error( "{}{}", LOG_JOB_NOT_FOUND, jobId );
                return false;
            }
            job = optJob.get( );

            Optional<DatasetDocument> optDocument = DatasetDocumentHome.findByPrimaryKey( job.getDocumentId( ) );
            if ( optDocument.isEmpty( ) )
            {
                AppLogService.error( "{}{}{}{}", LOG_DOCUMENT_NOT_FOUND, job.getDocumentId( ), LOG_DOCUMENT_JOB_CONTEXT, jobId );
                return false;
            }
            datasetDocument = optDocument.get( );

            Optional<Dataset> optDataset = DatasetHome.findByPrimaryKey( job.getDatasetId( ) );
            if ( optDataset.isEmpty( ) )
            {
                AppLogService.error( "{}{}{}{}", LOG_DATASET_NOT_FOUND, job.getDatasetId( ), LOG_DOCUMENT_JOB_CONTEXT, jobId );
                return false;
            }
            dataset = optDataset.get( );

            Optional<Provider> optEmbedProvider = ProviderHome.findByPrimaryKey( dataset.getEmbedProviderId( ) );
            if ( optEmbedProvider.isEmpty( ) )
            {
                AppLogService.error( "{}{}{}{}", LOG_PROVIDER_NOT_FOUND, dataset.getEmbedProviderId( ), LOG_DATASET_JOB_CONTEXT, dataset.getId( ) );
                return false;
            }
            embedProvider = optEmbedProvider.get( );

            executionId = _observabilityService.startResourceExecution( Dataset.RESOURCE_TYPE, String.valueOf( dataset.getId( ) ), dataset.getClientId( ),
                    ObservabilityData.of( "EMBEDDING", "documentId", datasetDocument.getId( ), "documentName", datasetDocument.getName( ), "jobId", jobId,
                            "datasetId", dataset.getId( ) ) );

            byte [ ] documentBytes;
            try ( InputStream documentStream = DocumentService.getDocumentFile( datasetDocument.getFileKey( ) ) )
            {
                if ( documentStream == null )
                {
                    String errorMsg = LOG_FILE_LOAD_ERROR + datasetDocument.getFileKey( );
                    AppLogService.error( errorMsg );
                    _observabilityService.completeResourceExecutionError( executionId, errorMsg, null );
                    return false;
                }
                documentBytes = documentStream.readAllBytes( );
            }

            Document langchainDocument = parseDocument( documentBytes, datasetDocument, dataset, executionId );

            if ( langchainDocument != null && langchainDocument.text( ) != null )
            {
                datasetDocument.setFullContent( langchainDocument.text( ) );
                DatasetDocumentHome.update( datasetDocument );
            }

            EmbeddingModel embeddingModel = _modelService.createEmbeddingModel( embedProvider );
            EmbeddingStore<TextSegment> embeddingStore = _elasticsearchService.createEmbeddingStore( dataset.getId( ) );

            int chunkSize = datasetDocument.getChunkSize( ) > 0 ? datasetDocument.getChunkSize( ) : CHUNK_SIZE;
            int chunkOverlap = datasetDocument.getChunkOverlap( ) >= 0 ? datasetDocument.getChunkOverlap( ) : CHUNK_OVERLAP;

            DocumentSplitter splitter = DocumentSplitters.recursive( chunkSize, chunkOverlap );
            List<TextSegment> segments = splitter.split( langchainDocument );

            if ( segments.isEmpty( ) )
            {
                _observabilityService.completeResourceExecutionSuccess( executionId,
                        ObservabilityData.of( "EMBEDDING_RESULT", "success", true, "chunksCreated", 0 ) );
                return true;
            }

            embeddingNodeId = NODE_ID_PREFIX + datasetDocument.getId( ) + NODE_ID_JOB_SEPARATOR + jobId + NODE_ID_UUID_SEPARATOR
                    + UUID.randomUUID( ).toString( ).substring( 0, UUID_SUBSTRING_LENGTH );

            _observabilityService.startNodeExecution( executionId, embeddingNodeId, OBS_DOC_EMBEDDING_OPERATION + embedProvider.getDeploymentModelName( ) + ")",
                    1, ObservabilityData.input( OBS_DOC_EMBEDDING_OPERATION + embedProvider.getDeploymentModelName( ) + ")",
                            OBS_EMBEDDING_SEGMENTS + segments.size( ) + OBS_SEGMENTS_LABEL + datasetDocument.getName( ) ) );

            List<TextSegment> enrichedSegments = new ArrayList<>( );
            for ( int i = 0; i < segments.size( ); i++ )
            {
                TextSegment segment = segments.get( i );

                Metadata enrichedMetadata = Metadata.from( segment.metadata( ).toMap( ) );
                enrichedMetadata.put( META_DOCUMENT_ID, String.valueOf( datasetDocument.getId( ) ) );
                enrichedMetadata.put( META_DOCUMENT_NAME, datasetDocument.getName( ) );
                enrichedMetadata.put( META_DATASET_ID, String.valueOf( dataset.getId( ) ) );
                enrichedMetadata.put( META_DATASET_NAME, dataset.getDatasetName( ) );
                enrichedMetadata.put( META_SEGMENT_INDEX, String.valueOf( i ) );
                if ( datasetDocument.getFolderId( ) != null )
                {
                    enrichedMetadata.put( META_FOLDER_ID, String.valueOf( datasetDocument.getFolderId( ) ) );
                }

                enrichedSegments.add( TextSegment.from( segment.text( ), enrichedMetadata ) );
            }

            long totalInputTokens = 0;
            int successfullyEmbeddedSegments = 0;

            for ( int start = 0; start < enrichedSegments.size( ); start += EMBEDDING_BATCH_SIZE )
            {
                List<TextSegment> batch = enrichedSegments.subList( start, Math.min( start + EMBEDDING_BATCH_SIZE, enrichedSegments.size( ) ) );
                try
                {
                    Response<List<Embedding>> response = embeddingModel.embedAll( batch );
                    embeddingStore.addAll( response.content( ), batch );
                    successfullyEmbeddedSegments += batch.size( );

                    TokenUsage tokenUsage = response.tokenUsage( );
                    if ( tokenUsage != null && tokenUsage.inputTokenCount( ) != null )
                    {
                        totalInputTokens += tokenUsage.inputTokenCount( );
                    }
                }
                catch( Exception batchError )
                {
                    AppLogService.error( LOG_BATCH_ERROR, start, datasetDocument.getId( ), batchError );
                    for ( int j = 0; j < batch.size( ); j++ )
                    {
                        int segmentIndex = start + j;
                        TextSegment segWithMeta = batch.get( j );
                        try
                        {
                            Response<Embedding> response = embeddingModel.embed( segWithMeta );
                            embeddingStore.add( response.content( ), segWithMeta );
                            successfullyEmbeddedSegments++;

                            TokenUsage tokenUsage = response.tokenUsage( );
                            if ( tokenUsage != null && tokenUsage.inputTokenCount( ) != null )
                            {
                                totalInputTokens += tokenUsage.inputTokenCount( );
                            }
                        }
                        catch( Exception e )
                        {
                            String segErrorMsg = LOG_SEGMENT_ERROR + segmentIndex + OBS_SEGMENT_DOC_PREFIX + datasetDocument.getId( ) + OBS_DOC_ERROR_SEPARATOR
                                    + e.getMessage( );
                            AppLogService.error( segErrorMsg, e );
                            _observabilityService.addNodeTrace( executionId, embeddingNodeId,
                                    ObservabilityData.trace( OBS_SEGMENT_ERROR, OBS_FAILED_EMBED_SEGMENT + segmentIndex, e.getMessage( ) ),
                                    PlatformResourceNodeTraceStatus.ERROR.getValue( ), BigDecimal.ZERO );
                        }
                    }
                }
            }

            double tokenInputPricePerMillion = embedProvider.getTokenInputPrice1M( );
            double tokenInputPricePerToken = tokenInputPricePerMillion / MILLION_TOKENS;
            BigDecimal totalCost = BigDecimal.valueOf( tokenInputPricePerToken * totalInputTokens );

            _observabilityService.addNodeTrace( executionId, embeddingNodeId,
                    ObservabilityData.trace( OBS_TOKEN_USAGE, String.format( OBS_TOKEN_FORMAT, totalInputTokens, totalCost.toPlainString( ) ), null ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), totalCost );

            if ( successfullyEmbeddedSegments == segments.size( ) )
            {
                _observabilityService.completeNodeExecutionSuccess( executionId, embeddingNodeId,
                        ObservabilityData.output( OBS_ALL_SEGMENTS_SUCCESS + segments.size( ) + OBS_SEGMENTS_SUCCESS_SUFFIX + totalInputTokens, null ) );

                _observabilityService.completeResourceExecutionSuccess( executionId,
                        ObservabilityData.of( "EMBEDDING_RESULT", "success", true, "chunksCreated", successfullyEmbeddedSegments ) );
                return true;
            }
            else if ( successfullyEmbeddedSegments > 0 )
            {
                String partialMsgNode = String.format( OBS_PARTIAL_EMBED_FORMAT, successfullyEmbeddedSegments, segments.size( ), totalInputTokens );
                _observabilityService.completeNodeExecutionSuccess( executionId, embeddingNodeId, ObservabilityData.output( partialMsgNode, null ) );

                _observabilityService.completeResourceExecutionSuccess( executionId,
                        ObservabilityData.of( "EMBEDDING_RESULT", "success", true, "chunksCreated", successfullyEmbeddedSegments ) );
                return true;
            }
            else
            {
                String errorMsg = OBS_EMBED_FAIL + datasetDocument.getName( );
                _observabilityService.completeNodeExecutionError( executionId, embeddingNodeId, errorMsg, null );
                _observabilityService.completeResourceExecutionError( executionId, errorMsg,
                        ObservabilityData.of( "EMBEDDING_RESULT", "success", false, "chunksCreated", successfullyEmbeddedSegments ) );
                return false;
            }

        }
        catch( Exception e )
        {
            String errorMsg = LOG_CRITICAL_ERROR + jobId + OBS_DOC_ERROR_SEPARATOR + e.getMessage( );
            AppLogService.error( errorMsg, e );

            if ( executionId != null )
            {
                if ( embeddingNodeId != null )
                {
                    _observabilityService.completeNodeExecutionError( executionId, embeddingNodeId, OBS_CRITICAL_ERROR + e.getMessage( ), null );
                }
                _observabilityService.completeResourceExecutionError( executionId, OBS_CRITICAL_ERROR + e.getMessage( ),
                        ObservabilityData.of( "EMBEDDING_RESULT", "success", false, "chunksCreated", 0 ) );
            }
            return false;
        }
    }

    /**
     * Parses a document using the appropriate parser and enriches it with metadata.
     *
     * @param bytes
     *            the document content as byte array
     * @param doc
     *            the dataset document metadata
     * @param dataset
     *            the dataset configuration
     * @param executionId
     *            the execution ID for observability tracking
     * @return the parsed Document with enriched metadata
     * @throws Exception
     *             if parsing fails
     */
    private Document parseDocument( byte [ ] bytes, DatasetDocument doc, Dataset dataset, String executionId ) throws Exception
    {
        if ( doc.getUseDocumentIntelligence( ) && doc.getDocumentIntelligenceProviderId( ) != null )
        {
            return parseWithDocumentIntelligence( bytes, doc, executionId );
        }

        String baseNodeId = CostTrackingListener.NODE_OCR + doc.getId( );
        DocumentParser parser = getDocumentParser( doc.getName( ), dataset, executionId, baseNodeId );
        Document document;

        try ( InputStream inputStream = new ByteArrayInputStream( bytes ) )
        {
            document = parser.parse( inputStream );
        }

        Metadata md = document.metadata( );
        md.put( META_DOCUMENT_ID, String.valueOf( doc.getId( ) ) );
        md.put( META_DOCUMENT_NAME, doc.getName( ) );
        md.put( META_DATASET_ID, String.valueOf( doc.getDatasetId( ) ) );

        return new DefaultDocument( document.text( ), md );
    }

    /**
     * Flags the consumer loop to stop on CDI shutdown.
     */
    @PreDestroy
    void shutdown( )
    {
        AppLogService.info( LOG_SHUTTING_DOWN );
        _stopped = true;
        if ( _recoveryFuture != null )
        {
            _recoveryFuture.cancel( false );
        }
        AppLogService.info( LOG_SHUTDOWN_COMPLETE );
    }

    /**
     * Parse document using Azure Document Intelligence
     *
     * @param bytes
     *            the document bytes
     * @param doc
     *            the dataset document
     * @param executionId
     *            the execution ID for observability
     * @return the parsed Document
     * @throws Exception
     *             if parsing fails
     */
    private Document parseWithDocumentIntelligence( byte [ ] bytes, DatasetDocument doc, String executionId ) throws Exception
    {
        Provider diProvider = ProviderHome.findByPrimaryKey( doc.getDocumentIntelligenceProviderId( ) )
                .orElseThrow( ( ) -> new Exception( "Document Intelligence provider not found: " + doc.getDocumentIntelligenceProviderId( ) ) );

        Document document;
        String parsedWith;

        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( diProvider.getProviderVendor( ) ) )
        {
            document = _mistralOcrParser.analyzeDocument( bytes, MistralOcrParser.detectContentType( doc.getName( ) ), diProvider, executionId );
            parsedWith = "Mistral OCR";
        }
        else
        {
            document = _azureParser.analyzeDocument( bytes, diProvider, diProvider.getDeploymentModelName( ), executionId );
            parsedWith = "Azure Document Intelligence";
        }

        Metadata metadata = document.metadata( );
        metadata.put( "document_id", String.valueOf( doc.getId( ) ) );
        metadata.put( "document_name", doc.getName( ) );
        metadata.put( "parsed_with", parsedWith );

        return document;
    }
}
