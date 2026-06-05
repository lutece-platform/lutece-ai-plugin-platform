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
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
 * Service for processing documents with Mistral OCR API. Supports PDF, JPEG, PNG, BMP, TIFF via base64 encoding.
 */
@ApplicationScoped
public class MistralOcrParser
{
    @Inject
    private ObservabilityService _observability;

    @Inject
    private fr.paris.lutece.plugins.platform.service.concurrent.SharedHttpClientFactory _sharedHttpClientFactory;

    private static final String OBS_NODE_PREFIX = "MistralOCR_";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final int HTTP_TIMEOUT_SECONDS = 120;
    private static final int HTTP_OK = 200;

    private static final ObjectMapper _objectMapper = new ObjectMapper( );

    /**
     * Extracts text from a document using Mistral OCR API.
     *
     * @param documentBytes
     *            the document content as bytes
     * @param contentType
     *            the MIME type of the document (e.g. application/pdf, image/jpeg)
     * @param provider
     *            the Mistral OCR provider configuration
     * @param executionId
     *            the execution ID for observability
     * @return the parsed Document with extracted markdown and metadata
     */
    public Document analyzeDocument( byte [ ] documentBytes, String contentType, Provider provider, String executionId )
    {
        String nodeId = OBS_NODE_PREFIX + UUID.randomUUID( ).toString( ).substring( 0, 8 );

        _observability.startNodeExecution( executionId, nodeId, "Mistral OCR Text Extraction (" + provider.getDeploymentModelName( ) + ")", 1,
                ObservabilityData.input( "Mistral OCR Text Extraction", "Extracting text from document with Mistral OCR" ) );

        try
        {
            String base64Content = Base64.getEncoder( ).encodeToString( documentBytes );
            String requestBody = buildRequestBody( base64Content, contentType, provider.getDeploymentModelName( ) );

            HttpRequest request = HttpRequest.newBuilder( ).uri( URI.create( provider.getDeploymentEndpoint( ) ) ).header( "Content-Type", CONTENT_TYPE_JSON )
                    .header( "Authorization", AUTH_HEADER_PREFIX + provider.getDeploymentApiKey( ) ).timeout( Duration.ofSeconds( HTTP_TIMEOUT_SECONDS ) )
                    .POST( HttpRequest.BodyPublishers.ofString( requestBody ) ).build( );

            HttpResponse<String> response = _sharedHttpClientFactory.javaHttpClient( ).send( request, HttpResponse.BodyHandlers.ofString( ) );

            if ( response.statusCode( ) != HTTP_OK )
            {
                throw new RuntimeException( "Mistral OCR API returned HTTP " + response.statusCode( ) + ": " + response.body( ) );
            }

            JsonNode root = _objectMapper.readTree( response.body( ) );
            JsonNode pages = root.get( "pages" );
            int pageCount = pages != null ? pages.size( ) : 0;

            String extractedText = extractMarkdownContent( pages );
            BigDecimal totalCost = calculateCost( pageCount, provider );

            _observability.addNodeTrace( executionId, nodeId,
                    ObservabilityData.trace( "COST_ANALYSIS", "Cost Analysis",
                            "Extracted text from %d pages. Estimated cost: %.4f €".formatted( pageCount, totalCost ) ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), totalCost );

            Metadata metadata = new Metadata( );
            metadata.put( EmbeddingService.META_PAGE_COUNT, String.valueOf( pageCount ) );
            metadata.put( EmbeddingService.META_ANALYSIS_MODEL, provider.getDeploymentModelName( ) );
            metadata.put( EmbeddingService.META_ANALYSIS_COST, totalCost.toString( ) );

            Document document = Document.from( extractedText, metadata );

            _observability.completeNodeExecutionSuccess( executionId, nodeId,
                    ObservabilityData.output( "Success", "Successfully extracted text from %d pages".formatted( pageCount ) ) );

            return document;
        }
        catch( Exception e )
        {
            String errorMsg = "Failed to extract text from document: " + e.getMessage( );
            AppLogService.error( "[MistralOcrParser] {}", errorMsg, e );
            _observability.completeNodeExecutionError( executionId, nodeId, errorMsg, null );
            throw new RuntimeException( "Mistral OCR text extraction failed", e );
        }
    }

    /**
     * Builds the JSON request body for the Mistral OCR API.
     */
    private String buildRequestBody( String base64Content, String contentType, String modelName )
    {
        String documentType = contentType != null && contentType.startsWith( "image/" ) ? "image_url" : "document_url";
        String dataUri = "data:" + ( contentType != null ? contentType : "application/pdf" ) + ";base64," + base64Content;

        ObjectNode body = _objectMapper.createObjectNode( );
        body.put( "model", modelName );
        ObjectNode document = body.putObject( "document" );
        document.put( "type", documentType );
        document.put( documentType, dataUri );

        return body.toString( );
    }

    /**
     * Extracts and concatenates markdown content from all pages.
     */
    private String extractMarkdownContent( JsonNode pages )
    {
        if ( pages == null || !pages.isArray( ) )
        {
            return "";
        }

        StringBuilder content = new StringBuilder( );
        for ( JsonNode page : pages )
        {
            JsonNode markdown = page.get( "markdown" );
            if ( markdown != null && !markdown.asText( ).isEmpty( ) )
            {
                if ( content.length( ) > 0 )
                {
                    content.append( "\n\n" );
                }
                content.append( markdown.asText( ) );
            }
        }

        return content.toString( );
    }

    /**
     * Calculates the cost based on pages analyzed.
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
     * Detects the MIME content type from a file name extension.
     *
     * @param fileName
     *            the file name
     * @return the MIME type, defaults to application/pdf
     */
    public static String detectContentType( String fileName )
    {
        if ( fileName == null )
        {
            return "application/pdf";
        }

        String lower = fileName.toLowerCase( );

        if ( lower.endsWith( ".pdf" ) )
            return "application/pdf";
        if ( lower.endsWith( ".jpg" ) || lower.endsWith( ".jpeg" ) )
            return "image/jpeg";
        if ( lower.endsWith( ".png" ) )
            return "image/png";
        if ( lower.endsWith( ".bmp" ) )
            return "image/bmp";
        if ( lower.endsWith( ".tiff" ) || lower.endsWith( ".tif" ) )
            return "image/tiff";
        if ( lower.endsWith( ".docx" ) )
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if ( lower.endsWith( ".xlsx" ) )
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if ( lower.endsWith( ".pptx" ) )
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if ( lower.endsWith( ".html" ) || lower.endsWith( ".htm" ) )
            return "text/html";

        return "application/pdf";
    }
}
