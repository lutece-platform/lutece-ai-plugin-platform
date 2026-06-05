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

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

public class CostTrackingListener implements ChatModelListener
{
    private static final String LOG_ON_ERROR = "LLM onError: executionId={0}, nodeId={1}, error={2}";
    private static final String NODE_COMPLETION_NO_TOKEN_MSG = "Requête LLM complétée sans données de tokens";
    private static final String NODE_EXECUTION_TITLE = "Requête LLM ({0})";
    private static final String UNKNOWN_MODEL = "Unknown model";
    private static final double TOKEN_PRICE_DIVISOR = 1_000_000.0;

    public static final String NODE_TRANSFORMER = "llm-for-transform";
    public static final String NODE_ROUTER = "llm-for-router";
    public static final String NODE_STREAMING_TRANSFORMER = "llm-stream-for-transform";
    public static final String NODE_STREAMING_ROUTER = "llm-stream-for-router-";
    public static final String NODE_STREAMING_MAIN_QUERY = "stream-query-llm-";
    public static final String NODE_RAG_RETRIEVAL = "rag-streaming-retriever-";
    public static final String NODE_OCR = "llm-for-ocr-";

    private static final String PURPOSE_TRANSFORMER = "Transformateur";
    private static final String PURPOSE_VISION = "Vision/OCR";
    private static final String PURPOSE_ROUTER = "Routeur";
    private static final String PURPOSE_STREAMING_TRANSFORMER = "Transformateur";
    private static final String PURPOSE_STREAMING_ROUTER = "Routeur";
    private static final String PURPOSE_STREAMING_MAIN_QUERY = "Requête principale";
    private static final String PURPOSE_GENERIC = "Générique";
    private static final String PURPOSE_RAG_RETRIEVAL = "Récupération RAG";

    private final String executionId;
    private final String nodeId;
    private final double tokenInputPrice;
    private final double tokenOutputPrice;
    private final String modelName;
    private final int nodeOrder;
    private final ObservabilityService _observability = CDI.current( ).select( ObservabilityService.class ).get( );
    private final AtomicBoolean started = new AtomicBoolean( false );
    private final AtomicBoolean completed = new AtomicBoolean( false );
    private final Executor _ctxExecutor;

    /**
     * Constructs a cost-tracking chat model listener for a single LLM node execution, converting the per-million token prices to per-token prices.
     *
     * @param executionId
     *            the resource execution identifier
     * @param nodeId
     *            the node identifier
     * @param tokenInputPrice1M
     *            the input token price per one million tokens
     * @param tokenOutputPrice1M
     *            the output token price per one million tokens
     * @param modelName
     *            the model name, or null for an unknown model
     * @param nodeOrder
     *            the ordering position of the node within the execution
     * @param ctxExecutor
     *            the executor carrying the CDI/TCCL context, or null to run inline
     */
    public CostTrackingListener( String executionId, String nodeId, double tokenInputPrice1M, double tokenOutputPrice1M, String modelName, int nodeOrder,
            Executor ctxExecutor )
    {
        this.executionId = executionId;
        this.nodeId = nodeId;
        this.tokenInputPrice = tokenInputPrice1M / TOKEN_PRICE_DIVISOR;
        this.tokenOutputPrice = tokenOutputPrice1M / TOKEN_PRICE_DIVISOR;
        this.modelName = modelName != null ? modelName : UNKNOWN_MODEL;
        this.nodeOrder = nodeOrder;
        this._ctxExecutor = ctxExecutor;
    }

    /**
     * Starts the observability node execution exactly once across concurrent callbacks.
     */
    private void ensureNodeStarted( )
    {
        if ( started.compareAndSet( false, true ) )
        {
            _observability.startNodeExecution( this.executionId, this.nodeId, MessageFormat.format( NODE_EXECUTION_TITLE, extractNodePurpose( this.nodeId ) ),
                    this.nodeOrder, ObservabilityData.input( "LLM Request", this.modelName ) );
        }
    }

    /**
     * Runs the given task on the captured CDI/TCCL context, or inline if no executor was provided.
     *
     * @param task
     *            the task to run
     */
    private void runWithContext( Runnable task )
    {
        if ( _ctxExecutor != null )
        {
            _ctxExecutor.execute( task );
        }
        else
        {
            task.run( );
        }
    }

    @Override
    public void onRequest( ChatModelRequestContext requestContext )
    {
        runWithContext( this::ensureNodeStarted );
    }

    @Override
    public void onResponse( ChatModelResponseContext responseContext )
    {
        runWithContext( ( ) -> {
            ensureNodeStarted( );
            if ( !completed.compareAndSet( false, true ) )
            {
                return;
            }

            TokenUsage tokenUsage = responseContext.chatResponse( ).tokenUsage( );
            if ( tokenUsage != null )
            {
                int inputTokens = tokenUsage.inputTokenCount( ) != null ? tokenUsage.inputTokenCount( ) : 0;
                int outputTokens = tokenUsage.outputTokenCount( ) != null ? tokenUsage.outputTokenCount( ) : 0;
                BigDecimal inputCost = BigDecimal.valueOf( this.tokenInputPrice * inputTokens );
                BigDecimal outputCost = BigDecimal.valueOf( this.tokenOutputPrice * outputTokens );

                _observability.addNodeTrace( this.executionId, this.nodeId,
                        ObservabilityData.of( "TOKEN_COST", "type", "input", "tokens", inputTokens, "cost", inputCost ),
                        PlatformResourceNodeTraceStatus.INFO.getValue( ), inputCost );

                _observability.addNodeTrace( this.executionId, this.nodeId,
                        ObservabilityData.of( "TOKEN_COST", "type", "output", "tokens", outputTokens, "cost", outputCost ),
                        PlatformResourceNodeTraceStatus.INFO.getValue( ), outputCost );

                String responseText = responseContext.chatResponse( ).aiMessage( ) != null ? responseContext.chatResponse( ).aiMessage( ).text( ) : "";
                _observability.completeNodeExecutionSuccess( this.executionId, this.nodeId, ObservabilityData.of( "MODEL_RESPONSE", "success", true,
                        "inputTokens", inputTokens, "outputTokens", outputTokens, "response", responseText ) );
            }
            else
            {
                _observability.completeNodeExecutionSuccess( this.executionId, this.nodeId, ObservabilityData.output( NODE_COMPLETION_NO_TOKEN_MSG, null ) );
            }
        } );
    }

    @Override
    public void onError( ChatModelErrorContext errorContext )
    {
        runWithContext( ( ) -> {
            ensureNodeStarted( );
            if ( !completed.compareAndSet( false, true ) )
            {
                return;
            }

            String errorMessage = errorContext.error( ) != null ? errorContext.error( ).getMessage( ) : "Unknown LLM error";

            _observability.addNodeTrace( this.executionId, this.nodeId, ObservabilityData.trace( "LLM_ERROR", errorMessage, null ),
                    PlatformResourceNodeTraceStatus.ERROR.getValue( ), BigDecimal.ZERO );

            _observability.completeNodeExecutionError( this.executionId, this.nodeId, errorMessage, null );

            AppLogService.error( MessageFormat.format( LOG_ON_ERROR, executionId, nodeId, errorMessage ), errorContext.error( ) );
        } );
    }

    /**
     * Derives a human-readable purpose label from the node identifier by matching its known prefix.
     *
     * @param fullNodeId
     *            the full node identifier
     * @return the purpose label associated with the node identifier prefix, or the generic label when none matches
     */
    private String extractNodePurpose( String fullNodeId )
    {
        if ( fullNodeId.startsWith( NODE_STREAMING_TRANSFORMER ) )
            return PURPOSE_STREAMING_TRANSFORMER;
        if ( fullNodeId.startsWith( NODE_STREAMING_ROUTER ) )
            return PURPOSE_STREAMING_ROUTER;
        if ( fullNodeId.startsWith( NODE_STREAMING_MAIN_QUERY ) )
            return PURPOSE_STREAMING_MAIN_QUERY;
        if ( fullNodeId.startsWith( NODE_RAG_RETRIEVAL ) )
            return PURPOSE_RAG_RETRIEVAL;
        if ( fullNodeId.startsWith( NODE_OCR ) )
            return PURPOSE_VISION;
        if ( fullNodeId.startsWith( NODE_TRANSFORMER ) )
            return PURPOSE_TRANSFORMER;
        if ( fullNodeId.startsWith( NODE_ROUTER ) )
            return PURPOSE_ROUTER;
        return PURPOSE_GENERIC;
    }
}
