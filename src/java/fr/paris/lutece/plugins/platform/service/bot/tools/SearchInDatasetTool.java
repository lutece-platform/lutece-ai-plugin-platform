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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tool that performs a semantic (RAG) search inside a single dataset. When a folder path is supplied, the search is recursively scoped to the whole subtree
 * below it. Independent from the bot's main RAG routing.
 */
public class SearchInDatasetTool extends AbstractDatasetTool
{
    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public SearchInDatasetTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Performs a semantic search restricted to a single dataset and an optional folder subtree.
     *
     * @param datasetName
     *            The dataset name
     * @param query
     *            The natural language query
     * @param folderPath
     *            Optional folder path to recursively scope the search
     * @return Markdown-formatted search results or an explanatory message
     */
    @Tool( "Perform a semantic (vector) search across the documents of a single dataset, optionally restricted to a folder subtree (recursive). Use when looking for relevant content based on meaning rather than exact words." )
    public String searchInDataset( @P( "Name of the dataset" ) String datasetName, @P( "Natural language query describing what to find" ) String query,
            @P( "Optional folder path to restrict search recursively (e.g. '/contracts/2024'). Empty or '/' for the whole dataset." ) String folderPath )
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
        FolderPathResolver resolver = getResolver( dataset.getId( ) );

        Optional<Provider> embedProviderOpt = ProviderHome.findByPrimaryKey( dataset.getEmbedProviderId( ) );
        if ( embedProviderOpt.isEmpty( ) )
        {
            return "No embedding provider configured for dataset '" + dataset.getDatasetName( ) + "'.";
        }

        try
        {
            EmbeddingModel embeddingModel = CDI.current( ).select( ModelService.class ).get( ).createEmbeddingModel( embedProviderOpt.get( ) );
            EmbeddingStore<TextSegment> embeddingStore = CDI.current( ).select( ElasticsearchService.class ).get( ).createEmbeddingStore( dataset.getId( ) );

            EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder builder = EmbeddingStoreContentRetriever.builder( )
                    .embeddingStore( embeddingStore ).embeddingModel( embeddingModel ).maxResults( MAX_RESULTS ).minScore( MIN_SCORE );

            List<Integer> scopedIds = resolveFolderScope( resolver, folderPath );
            if ( scopedIds == null )
            {
                return "Folder '" + folderPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
            }
            if ( !scopedIds.isEmpty( ) )
            {
                Set<String> idValues = new HashSet<>( );
                for ( Integer id : scopedIds )
                {
                    idValues.add( String.valueOf( id ) );
                }
                builder.filter( new IsIn( EmbeddingService.META_FOLDER_ID, new ArrayList<>( idValues ) ) );
            }

            EmbeddingStoreContentRetriever retriever = builder.build( );
            List<Content> contents = retriever.retrieve( Query.from( query ) );
            if ( contents.isEmpty( ) )
            {
                return "No semantic match for '" + query + "' in dataset '" + dataset.getDatasetName( ) + "'.";
            }

            StringBuilder sb = new StringBuilder( );
            sb.append( contents.size( ) ).append( " result(s) for '" ).append( query ).append( "':\n\n" );
            int count = 0;
            for ( Content content : contents )
            {
                count++;
                TextSegment segment = content.textSegment( );
                sb.append( count ).append( ". " ).append( formatDocumentReference( segment.metadata( ).toMap( ), resolver ) ).append( "\n" );
                sb.append( segment.text( ) ).append( "\n\n" );
            }
            sb.append( CITATION_HINT );
            return sb.toString( );
        }
        catch( Exception e )
        {
            AppLogService.error( "SearchInDatasetTool error: {}", e.getMessage( ), e );
            return "Error during semantic search: " + e.getMessage( );
        }
    }
}
