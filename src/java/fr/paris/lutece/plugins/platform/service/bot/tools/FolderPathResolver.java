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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderHome;

/**
 * Navigates the folder tree of a single dataset entirely in memory.
 * <p>
 * Load once per request/tool-call with {@link #forDataset(int)} ; every subsequent lookup is a plain map access. The resolver is the only place that knows how
 * to translate between the user-facing path notation ("/foo/bar") the LLM manipulates, and the internal folder_id adjacency list used by the database.
 * </p>
 */
public final class FolderPathResolver
{
    private static final String PATH_SEPARATOR = "/";

    private final int _nDatasetId;
    private final Map<Integer, DatasetFolder> _byId = new HashMap<>( );
    private final Map<Integer, List<DatasetFolder>> _childrenByParent = new HashMap<>( );
    private final List<DatasetFolder> _roots = new ArrayList<>( );

    /**
     * Private constructor — build through {@link #forDataset(int)}.
     *
     * @param nDatasetId
     *            The dataset identifier this resolver operates on
     */
    private FolderPathResolver( int nDatasetId )
    {
        _nDatasetId = nDatasetId;
    }

    /**
     * Loads all folders of a dataset into a fresh resolver.
     *
     * @param nDatasetId
     *            The dataset identifier
     * @return A resolver ready for path/id queries
     */
    public static FolderPathResolver forDataset( int nDatasetId )
    {
        FolderPathResolver resolver = new FolderPathResolver( nDatasetId );
        List<DatasetFolder> folders = DatasetFolderHome.getFoldersByDatasetId( nDatasetId );
        for ( DatasetFolder folder : folders )
        {
            resolver._byId.put( folder.getId( ), folder );
            Integer parentId = folder.getParentFolderId( );
            if ( parentId == null )
            {
                resolver._roots.add( folder );
            }
            else
            {
                resolver._childrenByParent.computeIfAbsent( parentId, k -> new ArrayList<>( ) ).add( folder );
            }
        }
        return resolver;
    }

    /**
     * Returns the dataset identifier this resolver is bound to.
     *
     * @return The dataset identifier
     */
    public int getDatasetId( )
    {
        return _nDatasetId;
    }

    /**
     * Returns the root folders (direct children of the dataset).
     *
     * @return A list of root folders, or an empty list
     */
    public List<DatasetFolder> getRoots( )
    {
        return Collections.unmodifiableList( _roots );
    }

    /**
     * Returns every folder of the dataset already loaded in memory. No extra DB query.
     *
     * @return A collection of all folders in this dataset
     */
    public java.util.Collection<DatasetFolder> getAllFolders( )
    {
        return Collections.unmodifiableCollection( _byId.values( ) );
    }

    /**
     * Returns the immediate children of a folder, or the root folders if nFolderId is null.
     *
     * @param nFolderId
     *            The parent folder identifier, or null to list dataset roots
     * @return A list of children folders, or an empty list if none
     */
    public List<DatasetFolder> getChildren( Integer nFolderId )
    {
        if ( nFolderId == null )
        {
            return Collections.unmodifiableList( _roots );
        }
        return Collections.unmodifiableList( _childrenByParent.getOrDefault( nFolderId, Collections.emptyList( ) ) );
    }

    /**
     * Returns a folder by its ID.
     *
     * @param nFolderId
     *            The folder identifier
     * @return The folder, or null if unknown in this dataset
     */
    public DatasetFolder findById( Integer nFolderId )
    {
        if ( nFolderId == null )
        {
            return null;
        }
        return _byId.get( nFolderId );
    }

    /**
     * Walks up the parent chain to build the full path of a folder (e.g. "/contracts/2024/q1").
     *
     * @param nFolderId
     *            The folder identifier, or null for the dataset root
     * @return The absolute path (always starts with "/", "/" itself for root)
     */
    public String pathOf( Integer nFolderId )
    {
        if ( nFolderId == null )
        {
            return PATH_SEPARATOR;
        }
        DatasetFolder folder = _byId.get( nFolderId );
        if ( folder == null )
        {
            return PATH_SEPARATOR;
        }
        List<String> segments = new ArrayList<>( );
        DatasetFolder cursor = folder;
        while ( cursor != null )
        {
            segments.add( 0, cursor.getName( ) );
            Integer parentId = cursor.getParentFolderId( );
            cursor = parentId == null ? null : _byId.get( parentId );
        }
        return PATH_SEPARATOR + String.join( PATH_SEPARATOR, segments );
    }

    /**
     * Resolves an absolute path ("/a/b/c") to its folder, matching names case-sensitively along the tree.
     *
     * @param strPath
     *            The absolute path. null, empty or "/" means dataset root
     * @return The matching folder, or null if the path does not exist (root also returns null = dataset root)
     */
    public DatasetFolder resolvePath( String strPath )
    {
        if ( strPath == null || strPath.isEmpty( ) || PATH_SEPARATOR.equals( strPath ) )
        {
            return null;
        }
        String normalized = strPath.startsWith( PATH_SEPARATOR ) ? strPath.substring( 1 ) : strPath;
        if ( normalized.endsWith( PATH_SEPARATOR ) )
        {
            normalized = normalized.substring( 0, normalized.length( ) - 1 );
        }
        if ( normalized.isEmpty( ) )
        {
            return null;
        }
        String [ ] segments = normalized.split( PATH_SEPARATOR );
        Integer currentParentId = null;
        DatasetFolder current = null;
        for ( String segment : segments )
        {
            current = findChildByName( currentParentId, segment );
            if ( current == null )
            {
                return null;
            }
            currentParentId = current.getId( );
        }
        return current;
    }

    /**
     * Returns the folder IDs of the entire subtree rooted at the given folder (inclusive). Used for recursive scoping (e.g. "search everything under
     * /contracts").
     *
     * @param nFolderId
     *            The root folder identifier, or null for the whole dataset
     * @return A list of folder IDs covering the subtree. Empty list when nFolderId is null (caller interprets as "no folder filter")
     */
    public List<Integer> getDescendantIds( Integer nFolderId )
    {
        List<Integer> result = new ArrayList<>( );
        if ( nFolderId == null )
        {
            return result;
        }
        DatasetFolder root = _byId.get( nFolderId );
        if ( root == null )
        {
            return result;
        }
        collectDescendants( root.getId( ), result );
        return result;
    }

    /**
     * Recursively adds a folder and its descendants to the accumulator list.
     *
     * @param nFolderId
     *            The folder identifier to collect
     * @param accumulator
     *            The list to append IDs to
     */
    private void collectDescendants( int nFolderId, List<Integer> accumulator )
    {
        accumulator.add( nFolderId );
        List<DatasetFolder> children = _childrenByParent.get( nFolderId );
        if ( children != null )
        {
            for ( DatasetFolder child : children )
            {
                collectDescendants( child.getId( ), accumulator );
            }
        }
    }

    /**
     * Finds a folder by parent and name (null parent = root level).
     *
     * @param nParentFolderId
     *            The parent folder identifier, or null for root level
     * @param strName
     *            The folder name to match
     * @return The matching folder, or null if not found
     */
    private DatasetFolder findChildByName( Integer nParentFolderId, String strName )
    {
        List<DatasetFolder> candidates = nParentFolderId == null ? _roots : _childrenByParent.get( nParentFolderId );
        if ( candidates == null )
        {
            return null;
        }
        for ( DatasetFolder candidate : candidates )
        {
            if ( strName.equals( candidate.getName( ) ) )
            {
                return candidate;
            }
        }
        return null;
    }
}
