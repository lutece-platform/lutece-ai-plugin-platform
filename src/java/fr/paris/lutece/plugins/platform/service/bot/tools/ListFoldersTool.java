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
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;

/**
 * Tool that lists the immediate sub-folders of a given folder inside a dataset. Each entry returns the folder path and its description when available so the AI
 * can navigate the tree one level at a time.
 */
public class ListFoldersTool extends AbstractDatasetTool
{
    private static final int MAX_CHILDREN_SHOWN = 50;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public ListFoldersTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Lists the direct children folders of a parent folder. Pass empty parentPath to list the dataset root.
     *
     * @param datasetName
     *            The name of the dataset to inspect
     * @param parentPath
     *            Optional parent folder path. Empty for root-level folders.
     * @return Markdown-formatted folder list with descriptions
     */
    @Tool( "List direct sub-folders of a given folder in a dataset (non-recursive). Returns up to 50 folders alphabetically; when truncated, includes the total count and suggests findFolders for fuzzy keyword filtering. Use findFolders instead of this tool when you know the dataset has many folders (hundreds or thousands)." )
    public String listFolders( @P( "Name of the dataset to inspect" ) String datasetName,
            @P( "Parent folder path (e.g. '/contracts/2024'). Empty or '/' for dataset root." ) String parentPath )
    {
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );
        FolderPathResolver resolver = getResolver( dataset.getId( ) );

        Integer parentId;
        if ( parentPath == null || parentPath.trim( ).isEmpty( ) || "/".equals( parentPath.trim( ) ) )
        {
            parentId = null;
        }
        else
        {
            DatasetFolder parent = resolver.resolvePath( parentPath.trim( ) );
            if ( parent == null )
            {
                return "Folder '" + parentPath + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
            }
            parentId = parent.getId( );
        }

        List<DatasetFolder> children = resolver.getChildren( parentId );
        if ( children.isEmpty( ) )
        {
            return "No sub-folder in dataset '" + dataset.getDatasetName( ) + "'"
                    + ( parentId == null ? "." : " under '" + resolver.pathOf( parentId ) + "'." );
        }

        int total = children.size( );
        int shown = Math.min( total, MAX_CHILDREN_SHOWN );

        StringBuilder sb = new StringBuilder( );
        sb.append( "Folders in dataset '" ).append( dataset.getDatasetName( ) ).append( "'" );
        if ( parentId != null )
        {
            sb.append( " under '" ).append( resolver.pathOf( parentId ) ).append( "'" );
        }
        sb.append( " (" ).append( total ).append( " total" );
        if ( total > MAX_CHILDREN_SHOWN )
        {
            sb.append( ", showing first " ).append( MAX_CHILDREN_SHOWN ).append( " alphabetically" );
        }
        sb.append( "):\n\n" );

        for ( int i = 0; i < shown; i++ )
        {
            DatasetFolder child = children.get( i );
            sb.append( "- " ).append( resolver.pathOf( child.getId( ) ) );
            String description = child.getDescription( );
            if ( description != null && !description.isEmpty( ) )
            {
                sb.append( " — " ).append( description );
            }
            sb.append( "\n" );
        }

        if ( total > MAX_CHILDREN_SHOWN )
        {
            int remaining = total - MAX_CHILDREN_SHOWN;
            sb.append( "\n… and " ).append( remaining ).append( " more folder(s) not shown. " )
                    .append( "Use findFolders(datasetName, pattern) to narrow down by keyword (supports fuzzy matching).\n" );
        }
        return sb.toString( );
    }
}
