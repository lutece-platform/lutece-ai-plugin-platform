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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tool that performs an exact term search across the documents of a dataset (grep-like). When a folder path is supplied, search recurses into all sub-folders
 * below it.
 */
public class GrepTool extends AbstractDatasetTool
{
    private static final int MAX_RESULTS = 20;
    private static final int CONTEXT_RADIUS = 120;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public GrepTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Searches for an exact term in a dataset, optionally scoped to a folder subtree and/or a single document.
     *
     * @param datasetName
     *            The dataset name
     * @param term
     *            The term to search for
     * @param folderPath
     *            Optional folder path to recursively scope the search. Ignored if documentName is provided
     * @param documentName
     *            Optional exact document name to restrict the search to one document
     * @return Markdown-formatted matches with context snippets
     */
    @Tool( "Search for an exact word or phrase across documents of a dataset (grep-like). Returns matching snippets with the document name and folder. Scope is optional: a folder subtree (recursive) or a single document. If documentName is given, folderPath is ignored." )
    public String grep( @P( "Name of the dataset" ) String datasetName, @P( "Exact term or phrase to search" ) String term,
            @P( "Optional folder path to restrict search recursively (e.g. '/contracts/2024'). Empty or '/' to search the whole dataset. Ignored when documentName is provided." ) String folderPath,
            @P( "Optional exact document name to restrict the search to a single document (e.g. 'contrat-acme.md'). Empty for no document restriction." ) String documentName )
    {
        if ( term == null || term.trim( ).isEmpty( ) )
        {
            return "Error: search term is required.";
        }
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );
        FolderPathResolver resolver = getResolver( dataset.getId( ) );

        Integer documentId = null;
        String effectiveScope = "";
        List<Integer> folderIds = Collections.emptyList( );

        if ( documentName != null && !documentName.trim( ).isEmpty( ) )
        {
            Optional<DatasetDocument> docOpt = DatasetDocumentHome.findByNameAndDatasetId( documentName.trim( ), dataset.getId( ) );
            if ( docOpt.isEmpty( ) )
            {
                return "Document '" + documentName + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
            }
            documentId = docOpt.get( ).getId( );
            effectiveScope = "document: " + documentName.trim( );
        }
        else
        {
            List<Integer> scope = resolveFolderScope( resolver, folderPath );
            if ( scope == null )
            {
                return "Folder '" + folderPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
            }
            if ( !scope.isEmpty( ) )
            {
                folderIds = scope;
                effectiveScope = "folder: " + folderPath.trim( );
            }
        }

        List<Map<String, Object>> hits = CDI.current( ).select( ElasticsearchService.class ).get( ).searchByTerm( dataset.getId( ), term, folderIds, documentId,
                MAX_RESULTS );
        if ( hits.isEmpty( ) )
        {
            return "No match for '" + term + "' in dataset '" + dataset.getDatasetName( ) + "'"
                    + ( effectiveScope.isEmpty( ) ? "." : " (" + effectiveScope + ")." );
        }

        StringBuilder sb = new StringBuilder( );
        sb.append( hits.size( ) ).append( " match(es) for '" ).append( term ).append( "':\n\n" );
        int count = 0;
        for ( Map<String, Object> hit : hits )
        {
            count++;
            @SuppressWarnings( "unchecked" )
            Map<String, Object> meta = (Map<String, Object>) hit.getOrDefault( "metadata", Collections.emptyMap( ) );
            String segIdx = String.valueOf( meta.getOrDefault( "segment_index", "?" ) );
            String text = hit.getOrDefault( "text", "" ).toString( );
            String snippet = extractSnippet( text, term );
            sb.append( count ).append( ". " ).append( formatDocumentReference( meta, resolver ) );
            sb.append( " — segment " ).append( segIdx ).append( "\n" );
            sb.append( "   …" ).append( snippet ).append( "…\n\n" );
        }
        return sb.toString( );
    }

    /**
     * Extracts a context snippet around the first occurrence of the term in the given text
     *
     * @param text
     *            Source text
     * @param term
     *            Term to locate
     * @return A snippet with context around the term
     */
    private String extractSnippet( String text, String term )
    {
        int idx = text.toLowerCase( ).indexOf( term.toLowerCase( ) );
        if ( idx < 0 )
        {
            return text.length( ) > CONTEXT_RADIUS * 2 ? text.substring( 0, CONTEXT_RADIUS * 2 ) : text;
        }
        int start = Math.max( 0, idx - CONTEXT_RADIUS );
        int end = Math.min( text.length( ), idx + term.length( ) + CONTEXT_RADIUS );
        return text.substring( start, end ).replaceAll( "\\s+", " " );
    }
}
