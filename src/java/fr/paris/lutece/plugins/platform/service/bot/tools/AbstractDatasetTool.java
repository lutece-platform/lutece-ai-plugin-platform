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

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;

/**
 * Base class for built-in dataset tools. Holds the list of datasets the bot can access and provides shared lookup, scoring and rendering helpers. Caches one
 * FolderPathResolver per dataset so the LLM chaining multiple tool calls within the same conversation turn does not reload the folder tree on every call.
 */
public abstract class AbstractDatasetTool
{
    protected static final String CITATION_HINT = "\nCite the source document name in your final answer.";

    protected static final int DEFAULT_MAX_RESULTS = 20;
    protected static final int HARD_MAX_RESULTS = 50;
    protected static final double MIN_SCORE = 0.72;
    protected static final double SUBSTRING_MATCH_SCORE = 1.0;
    protected static final JaroWinklerSimilarity SIMILARITY = new JaroWinklerSimilarity( );

    protected final List<Dataset> _accessibleDatasets;
    private final Map<Integer, FolderPathResolver> _resolverCache = new ConcurrentHashMap<>( );

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the current bot has access to
     */
    protected AbstractDatasetTool( List<Dataset> accessibleDatasets )
    {
        _accessibleDatasets = accessibleDatasets;
    }

    /**
     * Returns the folder path resolver for the given dataset, loading it once per tool instance (cached across calls).
     *
     * @param datasetId
     *            The dataset identifier
     * @return A cached FolderPathResolver ready for path/id lookups
     */
    protected FolderPathResolver getResolver( int datasetId )
    {
        return _resolverCache.computeIfAbsent( datasetId, FolderPathResolver::forDataset );
    }

    /**
     * Resolves a dataset by its name (case insensitive)
     *
     * @param datasetName
     *            The dataset name to look up
     * @return Optional containing the dataset if found
     */
    protected Optional<Dataset> findDatasetByName( String datasetName )
    {
        if ( datasetName == null || _accessibleDatasets == null )
        {
            return Optional.empty( );
        }
        return _accessibleDatasets.stream( ).filter( d -> datasetName.trim( ).equalsIgnoreCase( d.getDatasetName( ) ) ).findFirst( );
    }

    /**
     * Returns a comma-separated list of accessible dataset names for error messages
     *
     * @return The dataset names joined with commas
     */
    protected String listAccessibleDatasetNames( )
    {
        if ( _accessibleDatasets == null || _accessibleDatasets.isEmpty( ) )
        {
            return "(none)";
        }
        return _accessibleDatasets.stream( ).map( Dataset::getDatasetName ).collect( Collectors.joining( ", " ) );
    }

    /**
     * Builds the standard "dataset not found" error message
     *
     * @param datasetName
     *            The name the user tried to access
     * @return Formatted error message including the available datasets
     */
    protected String datasetNotFoundMessage( String datasetName )
    {
        return "Dataset '" + datasetName + "' not found. Available datasets: " + listAccessibleDatasetNames( );
    }

    /**
     * Resolves a folder path to the list of folder IDs covered by it (the folder itself and all its descendants). Returns an empty list for a blank path or "/"
     * (meaning: unrestricted), and returns null when the path does not exist in the dataset (callers should render a "not found" error).
     *
     * @param resolver
     *            The resolver for the target dataset
     * @param folderPath
     *            The user-supplied folder path
     * @return A list of folder IDs (empty = unrestricted), or null if the path is invalid
     */
    protected List<Integer> resolveFolderScope( FolderPathResolver resolver, String folderPath )
    {
        if ( folderPath == null || folderPath.trim( ).isEmpty( ) || "/".equals( folderPath.trim( ) ) )
        {
            return Collections.emptyList( );
        }
        DatasetFolder folder = resolver.resolvePath( folderPath.trim( ) );
        if ( folder == null )
        {
            return null;
        }
        return resolver.getDescendantIds( folder.getId( ) );
    }

    /**
     * Clamps the requested maxResults to sane bounds using the shared default and hard cap.
     *
     * @param requested
     *            The user-requested max, possibly null or out of range
     * @return A valid max results value in [1, HARD_MAX_RESULTS]
     */
    protected int resolveLimit( Integer requested )
    {
        if ( requested == null || requested <= 0 )
        {
            return DEFAULT_MAX_RESULTS;
        }
        return Math.min( requested, HARD_MAX_RESULTS );
    }

    /**
     * Normalizes a string for fuzzy comparison: lowercase + stripped diacritics.
     *
     * @param input
     *            The raw string
     * @return The normalized string (empty string if input is null)
     */
    protected static String normalize( String input )
    {
        if ( input == null )
        {
            return "";
        }
        String decomposed = Normalizer.normalize( input, Normalizer.Form.NFD );
        return decomposed.replaceAll( "\\p{InCombiningDiacriticalMarks}+", "" ).toLowerCase( );
    }

    /**
     * Computes a fuzzy similarity score against a name and an optional description. Uses an accent/case insensitive substring match as a fast path (score 1.0)
     * and falls back to JaroWinkler on both fields.
     *
     * @param normalizedPattern
     *            The pre-normalized search pattern
     * @param name
     *            The primary field (e.g. document or folder name), may be null
     * @param description
     *            The secondary field, may be null or empty
     * @return A similarity score in [0, 1]
     */
    protected static double fuzzyScore( String normalizedPattern, String name, String description )
    {
        String normalizedName = normalize( name );
        if ( normalizedName.contains( normalizedPattern ) )
        {
            return SUBSTRING_MATCH_SCORE;
        }
        String normalizedDesc = description == null ? "" : normalize( description );
        if ( !normalizedDesc.isEmpty( ) && normalizedDesc.contains( normalizedPattern ) )
        {
            return SUBSTRING_MATCH_SCORE;
        }
        double nameScore = SIMILARITY.apply( normalizedPattern, normalizedName );
        double descScore = normalizedDesc.isEmpty( ) ? 0.0 : SIMILARITY.apply( normalizedPattern, normalizedDesc );
        return Math.max( nameScore, descScore );
    }

    /**
     * Renders a segment metadata map to a "**docName** (folderPath)" markdown reference using the given resolver to translate the META_FOLDER_ID into a human
     * readable path. Silently skips the folder path if the metadata is missing or malformed.
     *
     * @param metadata
     *            The segment metadata (document_name, META_FOLDER_ID...)
     * @param resolver
     *            The folder path resolver for the dataset
     * @return A markdown snippet such as "**report.pdf** (/contracts/2024)"
     */
    protected static String formatDocumentReference( Map<String, Object> metadata, FolderPathResolver resolver )
    {
        String docName = String.valueOf( metadata.getOrDefault( EmbeddingService.META_DOCUMENT_NAME, "?" ) );
        StringBuilder sb = new StringBuilder( "**" ).append( docName ).append( "**" );
        Object folderIdMeta = metadata.get( EmbeddingService.META_FOLDER_ID );
        if ( folderIdMeta != null )
        {
            try
            {
                String path = resolver.pathOf( Integer.valueOf( folderIdMeta.toString( ) ) );
                if ( !path.isEmpty( ) && !"/".equals( path ) )
                {
                    sb.append( " (" ).append( path ).append( ")" );
                }
            }
            catch( NumberFormatException ignored )
            {
            }
        }
        return sb.toString( );
    }
}
