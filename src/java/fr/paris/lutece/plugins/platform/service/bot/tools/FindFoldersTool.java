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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;

/**
 * Tool that searches folders by fuzzy matching on their name and description. Designed for datasets with large folder trees (hundreds to thousands of folders,
 * e.g. addresses, product codes) where {@code listFolders} would overflow the LLM context. Tolerates typos, accent differences, and case.
 */
public class FindFoldersTool extends AbstractDatasetTool
{
    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public FindFoldersTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Searches folders matching a pattern using fuzzy similarity. Use when the folder tree is too large to list.
     *
     * @param datasetName
     *            The dataset name
     * @param pattern
     *            The search pattern. Accent-insensitive, case-insensitive, tolerates typos
     * @param parentPath
     *            Optional parent folder path; search recurses into all descendants. Empty or "/" for whole dataset
     * @param maxResults
     *            Max results to return (default 20, capped at 50)
     * @return Markdown-formatted list of matching folders with their full path and description
     */
    @Tool( "Search folders by fuzzy name or description match within a dataset. Use this when a dataset has many folders (e.g. addresses, product codes) and listFolders would return too many results. Tolerates typos, accent differences and case." )
    public String findFolders( @P( "Name of the dataset" ) String datasetName,
            @P( "Search pattern — can be partial, misspelled or accent-insensitive. Matches against folder name and description." ) String pattern,
            @P( "Optional parent folder path to scope the search (recursive into all descendants). Empty or '/' for the whole dataset." ) String parentPath,
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

        Collection<DatasetFolder> scope = resolveScope( resolver, parentPath );
        if ( scope == null )
        {
            return "Folder '" + parentPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
        }

        String normalizedPattern = normalize( pattern.trim( ) );
        int limit = resolveLimit( maxResults );

        List<Scored<DatasetFolder>> scored = new ArrayList<>( );
        for ( DatasetFolder folder : scope )
        {
            double score = fuzzyScore( normalizedPattern, folder.getName( ), folder.getDescription( ) );
            if ( score >= MIN_SCORE )
            {
                scored.add( new Scored<>( folder, score ) );
            }
        }

        if ( scored.isEmpty( ) )
        {
            return "No folder matching '" + pattern + "' in dataset '" + dataset.getDatasetName( ) + "'.";
        }
        scored.sort( Comparator.comparingDouble( Scored<DatasetFolder>::getScore ).reversed( ) );

        StringBuilder sb = new StringBuilder( );
        int shown = Math.min( scored.size( ), limit );
        sb.append( shown ).append( " folder(s) matching '" ).append( pattern ).append( "'" );
        if ( scored.size( ) > limit )
        {
            sb.append( " (top " ).append( limit ).append( " of " ).append( scored.size( ) ).append( " matches)" );
        }
        sb.append( ":\n\n" );
        for ( int i = 0; i < shown; i++ )
        {
            DatasetFolder folder = scored.get( i ).getValue( );
            sb.append( "- " ).append( resolver.pathOf( folder.getId( ) ) );
            String description = folder.getDescription( );
            if ( description != null && !description.isEmpty( ) )
            {
                sb.append( " — " ).append( description );
            }
            sb.append( "\n" );
        }
        return sb.toString( );
    }

    /**
     * Resolves the search scope: all folders of the dataset, or the descendants of a given parent path.
     *
     * @param resolver
     *            The folder resolver for this dataset
     * @param parentPath
     *            The parent folder path, or null/empty/"/" for whole dataset
     * @return A collection of folders to search in, or null if parentPath is invalid
     */
    private Collection<DatasetFolder> resolveScope( FolderPathResolver resolver, String parentPath )
    {
        List<Integer> scopedIds = resolveFolderScope( resolver, parentPath );
        if ( scopedIds == null )
        {
            return null;
        }
        if ( scopedIds.isEmpty( ) )
        {
            return resolver.getAllFolders( );
        }
        Set<Integer> idSet = new HashSet<>( scopedIds );
        List<DatasetFolder> scoped = new ArrayList<>( );
        for ( DatasetFolder folder : resolver.getAllFolders( ) )
        {
            if ( idSet.contains( folder.getId( ) ) )
            {
                scoped.add( folder );
            }
        }
        return scoped;
    }
}
