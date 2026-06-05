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
package fr.paris.lutece.plugins.platform.service.bot.tools;

import java.util.List;
import java.util.Optional;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tool that performs a semantic (RAG) search scoped to a single document of a dataset. Use when a specific document has been identified but is too large to
 * read entirely — this returns only the chunks semantically matching the query.
 */
public class SearchInDocumentTool extends AbstractDatasetTool
{
    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public SearchInDocumentTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Performs a semantic search restricted to a single document.
     *
     * @param datasetName
     *            The dataset name
     * @param documentName
     *            The exact document name
     * @param query
     *            The natural language query
     * @return Markdown-formatted semantically matching snippets, or an explanatory message
     */
    @Tool( "Perform a semantic (vector) search inside a single document of a dataset. Use when you have identified a relevant document but it is too large to read in full — returns only the most semantically relevant chunks instead of the whole content." )
    public String searchInDocument( @P( "Name of the dataset" ) String datasetName, @P( "Exact name of the document to search in" ) String documentName,
            @P( "Natural language query describing what to find inside the document" ) String query )
    {
        if ( query == null || query.trim( ).isEmpty( ) )
        {
            return "Error: query is required.";
        }
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );

        Optional<DatasetDocument> docOpt = DatasetDocumentHome.findByNameAndDatasetId( documentName, dataset.getId( ) );
        if ( docOpt.isEmpty( ) )
        {
            return "Document '" + documentName + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
        }
        DatasetDocument doc = docOpt.get( );

        Optional<Provider> embedProviderOpt = ProviderHome.findByPrimaryKey( dataset.getEmbedProviderId( ) );
        if ( embedProviderOpt.isEmpty( ) )
        {
            return "No embedding provider configured for dataset '" + dataset.getDatasetName( ) + "'.";
        }

        try
        {
            EmbeddingModel embeddingModel = CDI.current( ).select( ModelService.class ).get( ).createEmbeddingModel( embedProviderOpt.get( ) );
            EmbeddingStore<TextSegment> embeddingStore = CDI.current( ).select( ElasticsearchService.class ).get( ).createEmbeddingStore( dataset.getId( ) );

            EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder( ).embeddingStore( embeddingStore )
                    .embeddingModel( embeddingModel ).maxResults( MAX_RESULTS ).minScore( MIN_SCORE )
                    .filter( new IsEqualTo( EmbeddingService.META_DOCUMENT_ID, String.valueOf( doc.getId( ) ) ) ).build( );

            List<Content> contents = retriever.retrieve( Query.from( query ) );
            if ( contents.isEmpty( ) )
            {
                return "No semantic match for '" + query + "' inside document '" + documentName + "'.";
            }

            StringBuilder sb = new StringBuilder( );
            sb.append( contents.size( ) ).append( " relevant chunk(s) in '" ).append( documentName ).append( "' for '" ).append( query ).append( "':\n\n" );
            int count = 0;
            for ( Content content : contents )
            {
                count++;
                TextSegment segment = content.textSegment( );
                sb.append( count ).append( ". " );
                Object segIdx = segment.metadata( ).toMap( ).get( "segment_index" );
                if ( segIdx != null )
                {
                    sb.append( "(segment " ).append( segIdx ).append( ") " );
                }
                sb.append( "\n" ).append( segment.text( ) ).append( "\n\n" );
            }
            sb.append( CITATION_HINT );
            return sb.toString( );
        }
        catch( Exception e )
        {
            AppLogService.error( "SearchInDocumentTool error: {}", e.getMessage( ), e );
            return "Error during semantic search: " + e.getMessage( );
        }
    }
}
