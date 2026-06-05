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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;

/**
 * Tool that searches documents by fuzzy matching on their name and description. Complementary to searchInDataset (which searches content chunks) and grep
 * (exact substring in chunks) — findDocuments operates on metadata only, useful when the LLM knows a document's approximate name but not its exact filename.
 */
public class FindDocumentsTool extends AbstractDatasetTool
{
    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public FindDocumentsTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Searches documents matching a pattern using fuzzy similarity on name and description.
     *
     * @param datasetName
     *            The dataset name
     * @param pattern
     *            The search pattern. Accent-insensitive, case-insensitive, tolerates typos
     * @param folderPath
     *            Optional folder path; search recurses into all descendants. Empty or "/" for whole dataset
     * @param maxResults
     *            Max results to return (default 20, capped at 50)
     * @return Markdown-formatted list of matching documents with their folder path
     */
    @Tool( "Search documents by fuzzy name or description match within a dataset. Use when you know a document's approximate name but not its exact filename. Tolerates typos, accent differences and case. Scope is optionally a folder subtree." )
    public String findDocuments( @P( "Name of the dataset" ) String datasetName,
            @P( "Search pattern — can be partial, misspelled or accent-insensitive. Matches against document name and description." ) String pattern,
            @P( "Optional folder path to scope the search (recursive into all descendants). Empty or '/' for the whole dataset." ) String folderPath,
            @P( "Max results to return (default 20, max 50)" ) Integer maxResults )
    {
        if ( pattern == null || pattern.trim( ).isEmpty( ) )
        {
            return "Error: search pattern is required.";
        }
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );
        FolderPathResolver resolver = getResolver( dataset.getId( ) );

        List<Integer> scope = resolveFolderScope( resolver, folderPath );
        if ( scope == null )
        {
            return "Folder '" + folderPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
        }
        Set<Integer> allowedFolderIds = scope.isEmpty( ) ? null : new HashSet<>( scope );

        String normalizedPattern = normalize( pattern.trim( ) );
        int limit = resolveLimit( maxResults );

        List<DatasetDocument> allDocs = DatasetDocumentHome.getDatasetDocumentsListByDatasetId( dataset.getId( ) );
        List<Scored<DatasetDocument>> scored = new ArrayList<>( );
        for ( DatasetDocument doc : allDocs )
        {
            if ( allowedFolderIds != null && ( doc.getFolderId( ) == null || !allowedFolderIds.contains( doc.getFolderId( ) ) ) )
            {
                continue;
            }
            double score = fuzzyScore( normalizedPattern, doc.getName( ), doc.getDescription( ) );
            if ( score >= MIN_SCORE )
            {
                scored.add( new Scored<>( doc, score ) );
            }
        }

        if ( scored.isEmpty( ) )
        {
            return "No document matching '" + pattern + "' in dataset '" + dataset.getDatasetName( ) + "'.";
        }
        scored.sort( Comparator.comparingDouble( Scored<DatasetDocument>::getScore ).reversed( ) );

        StringBuilder sb = new StringBuilder( );
        int shown = Math.min( scored.size( ), limit );
        sb.append( shown ).append( " document(s) matching '" ).append( pattern ).append( "'" );
        if ( scored.size( ) > limit )
        {
            sb.append( " (top " ).append( limit ).append( " of " ).append( scored.size( ) ).append( " matches)" );
        }
        sb.append( ":\n\n" );
        for ( int i = 0; i < shown; i++ )
        {
            DatasetDocument doc = scored.get( i ).getValue( );
            sb.append( "- " ).append( doc.getName( ) );
            if ( doc.getFolderId( ) != null )
            {
                sb.append( " (" ).append( resolver.pathOf( doc.getFolderId( ) ) ).append( ")" );
            }
            String description = doc.getDescription( );
            if ( description != null && !description.isEmpty( ) )
            {
                sb.append( " — " ).append( description );
            }
            sb.append( "\n" );
        }
        return sb.toString( );
    }
}
