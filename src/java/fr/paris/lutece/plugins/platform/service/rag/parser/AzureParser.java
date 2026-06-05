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
package fr.paris.lutece.plugins.platform.service.rag.parser;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentLine;
import com.azure.ai.documentintelligence.models.DocumentPage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for processing documents with Azure Document Intelligence Supports PDF, JPEG, PNG, BMP, TIFF, DOCX, XLSX, PPTX, HTML and more
 */
@ApplicationScoped
public class AzureParser
{
    @Inject
    private ObservabilityService _observability;

    @Inject
    private fr.paris.lutece.plugins.platform.service.concurrent.SharedHttpClientFactory _sharedHttpClientFactory;

    private static final String OBS_NODE_PREFIX = "DocumentIntelligence_";
    private static final String DEFAULT_MODEL = "prebuilt-read";

    /**
     * Extracts text from a document using Azure Document Intelligence
     *
     * @param documentBytes
     *            The document content as bytes
     * @param provider
     *            The Document Intelligence provider configuration
     * @param modelId
     *            The model to use (defaults to "prebuilt-read" for simple text extraction)
     * @param executionId
     *            The execution ID for observability
     * @return The parsed Document with extracted text and metadata
     */
    public Document analyzeDocument( byte [ ] documentBytes, Provider provider, String modelId, String executionId )
    {
        String nodeId = OBS_NODE_PREFIX + UUID.randomUUID( ).toString( ).substring( 0, 8 );
        String effectiveModelId = modelId != null ? modelId : DEFAULT_MODEL;

        _observability.startNodeExecution( executionId, nodeId, "Document Intelligence Text Extraction (" + effectiveModelId + ")", 1,
                ObservabilityData.input( "Document Intelligence Text Extraction", "Extracting text from document with Azure Document Intelligence" ) );

        try
        {
            DocumentIntelligenceClient client = createClient( provider );

            SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller = client.beginAnalyzeDocument( effectiveModelId,
                    new AnalyzeDocumentOptions( documentBytes ) );

            AnalyzeResult result = poller.getFinalResult( );

            int pageCount = result.getPages( ) != null ? result.getPages( ).size( ) : 0;
            BigDecimal totalCost = calculateCost( pageCount, provider );

            _observability.addNodeTrace( executionId, nodeId,
                    ObservabilityData.trace( "COST_ANALYSIS", "Cost Analysis",
                            "Extracted text from %d pages. Estimated cost: %.4f €".formatted( pageCount, totalCost ) ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), totalCost );

            String extractedText = extractTextContent( result );

            Metadata metadata = new Metadata( );
            metadata.put( EmbeddingService.META_PAGE_COUNT, String.valueOf( pageCount ) );
            metadata.put( EmbeddingService.META_ANALYSIS_MODEL, effectiveModelId );
            metadata.put( EmbeddingService.META_ANALYSIS_COST, totalCost.toString( ) );

            Document document = Document.from( extractedText, metadata );

            _observability.completeNodeExecutionSuccess( executionId, nodeId,
                    ObservabilityData.output( "Success", "Successfully extracted text from %d pages".formatted( pageCount ) ) );

            return document;
        }
        catch( Exception e )
        {
            String errorMsg = "Failed to extract text from document: " + e.getMessage( );
            AppLogService.error( "[AzureDocumentIntelligenceService] {}", errorMsg, e );

            _observability.completeNodeExecutionError( executionId, nodeId, errorMsg, null );

            throw new RuntimeException( "Document Intelligence text extraction failed", e );
        }
    }

    /**
     * Creates a Document Intelligence client
     */
    private DocumentIntelligenceClient createClient( Provider provider )
    {
        return new DocumentIntelligenceClientBuilder( ).endpoint( provider.getDeploymentEndpoint( ) )
                .credential( new AzureKeyCredential( provider.getDeploymentApiKey( ) ) ).httpClient( _sharedHttpClientFactory.azureHttpClient( ) )
                .buildClient( );
    }

    /**
     * Calculates the cost based on pages analyzed
     */
    private BigDecimal calculateCost( int pageCount, Provider provider )
    {
        if ( provider.getDocumentAnalysisPrice1000Pages( ) == null || pageCount == 0 )
        {
            return BigDecimal.ZERO;
        }

        double pricePerPage = provider.getDocumentAnalysisPrice1000Pages( ) / 1000.0;
        return BigDecimal.valueOf( pageCount * pricePerPage );
    }

    /**
     * Extracts all text content from the analysis result This method simply concatenates all the text found in the document
     */
    private String extractTextContent( AnalyzeResult result )
    {
        StringBuilder content = new StringBuilder( );

        String directContent = result.getContent( );
        if ( directContent != null && !directContent.isEmpty( ) )
        {
            content.append( directContent );
        }
        else
        {
            List<DocumentPage> pages = result.getPages( );
            if ( pages != null )
            {
                for ( DocumentPage page : pages )
                {
                    List<DocumentLine> lines = page.getLines( );
                    if ( lines != null )
                    {
                        for ( DocumentLine line : lines )
                        {
                            content.append( line.getContent( ) ).append( "\n" );
                        }
                    }
                    if ( pages.size( ) > 1 )
                    {
                        content.append( "\n" );
                    }
                }
            }
        }

        return content.toString( );
    }
}
