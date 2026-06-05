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
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;

/**
 * Tool that lists documents sitting directly inside a specific folder of a dataset (non-recursive).
 */
public class ListDocumentsTool extends AbstractDatasetTool
{
    private static final int MAX_DOCS_SHOWN = 50;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public ListDocumentsTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Lists documents within a given folder of a dataset (non-recursive).
     *
     * @param datasetName
     *            The dataset name
     * @param folderPath
     *            The folder path (empty or "/" for dataset root)
     * @return Markdown-formatted list of documents with their description
     */
    @Tool( "List documents stored directly in a folder of a dataset (non-recursive). Use empty path or '/' to list documents at the dataset root." )
    public String listDocuments( @P( "Name of the dataset" ) String datasetName,
            @P( "Folder path (e.g. '/contracts/2024'). Empty or '/' for dataset root." ) String folderPath )
    {
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );
        FolderPathResolver resolver = getResolver( dataset.getId( ) );

        Integer folderId;
        String effectivePath;
        if ( folderPath == null || folderPath.trim( ).isEmpty( ) || "/".equals( folderPath.trim( ) ) )
        {
            folderId = null;
            effectivePath = "/";
        }
        else
        {
            DatasetFolder folder = resolver.resolvePath( folderPath.trim( ) );
            if ( folder == null )
            {
                return "Folder '" + folderPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
            }
            folderId = folder.getId( );
            effectivePath = resolver.pathOf( folderId );
        }

        List<DatasetDocument> docs = DatasetDocumentHome.getDocumentsByFolderId( dataset.getId( ), folderId );
        if ( docs.isEmpty( ) )
        {
            return "No document found in dataset '" + dataset.getDatasetName( ) + "' folder '" + effectivePath + "'.";
        }

        int total = docs.size( );
        int shown = Math.min( total, MAX_DOCS_SHOWN );

        StringBuilder sb = new StringBuilder( );
        sb.append( "Documents in '" ).append( dataset.getDatasetName( ) ).append( "' at '" ).append( effectivePath ).append( "'" );
        sb.append( " (" ).append( total ).append( " total" );
        if ( total > MAX_DOCS_SHOWN )
        {
            sb.append( ", showing first " ).append( MAX_DOCS_SHOWN );
        }
        sb.append( "):\n\n" );

        for ( int i = 0; i < shown; i++ )
        {
            DatasetDocument doc = docs.get( i );
            sb.append( "- " ).append( doc.getName( ) );
            if ( doc.getDescription( ) != null && !doc.getDescription( ).isEmpty( ) )
            {
                sb.append( " — " ).append( doc.getDescription( ) );
            }
            sb.append( "\n" );
        }

        if ( total > MAX_DOCS_SHOWN )
        {
            int remaining = total - MAX_DOCS_SHOWN;
            sb.append( "\n… and " ).append( remaining ).append( " more document(s) not shown. " )
                    .append( "Use findDocuments(datasetName, pattern, folderPath) for fuzzy name matching, " )
                    .append( "or searchInDataset(datasetName, query, folderPath) for semantic content search.\n" );
        }
        return sb.toString( );
    }
}
