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
package fr.paris.lutece.plugins.platform.business.dataset;

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * This class provides instances management methods for DatasetFolder objects
 */
public final class DatasetFolderHome
{
    private static IDatasetFolderDAO _dao = CDI.current( ).select( IDatasetFolderDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private DatasetFolderHome( )
    {
    }

    /**
     * Creates a new DatasetFolder in the database
     *
     * @param folder
     *            The DatasetFolder to create
     * @return The created DatasetFolder populated with the generated ID
     */
    public static DatasetFolder create( DatasetFolder folder )
    {
        _dao.insert( folder, _plugin );
        return folder;
    }

    /**
     * Updates a DatasetFolder in the database
     *
     * @param folder
     *            The DatasetFolder to update
     * @return The updated DatasetFolder
     */
    public static DatasetFolder update( DatasetFolder folder )
    {
        _dao.store( folder, _plugin );
        return folder;
    }

    /**
     * Removes a DatasetFolder from the database
     *
     * @param nKey
     *            The DatasetFolder ID to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Returns a DatasetFolder from the database
     *
     * @param nKey
     *            The DatasetFolder ID
     * @return The DatasetFolder matching the ID, or empty if not found
     */
    public static Optional<DatasetFolder> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns all folders of a dataset (used to build the tree in memory)
     *
     * @param nDatasetId
     *            The dataset ID
     * @return A list of DatasetFolder objects
     */
    public static List<DatasetFolder> getFoldersByDatasetId( int nDatasetId )
    {
        return _dao.selectByDatasetId( nDatasetId, _plugin );
    }

    /**
     * Returns the direct children folders of a parent (null parent = root folders of the dataset)
     *
     * @param nDatasetId
     *            The dataset ID
     * @param nParentFolderId
     *            The parent folder ID, or null for root folders
     * @return A list of immediate child folders sorted by name
     */
    public static List<DatasetFolder> getChildren( int nDatasetId, Integer nParentFolderId )
    {
        return _dao.selectChildren( nDatasetId, nParentFolderId, _plugin );
    }

    /**
     * Finds a folder by its dataset, parent and name
     *
     * @param nDatasetId
     *            The dataset ID
     * @param nParentFolderId
     *            The parent folder ID, or null for root folders
     * @param strName
     *            The folder name
     * @return The matching DatasetFolder, or empty if not found
     */
    public static Optional<DatasetFolder> findByParentAndName( int nDatasetId, Integer nParentFolderId, String strName )
    {
        return _dao.findByParentAndName( nDatasetId, nParentFolderId, strName, _plugin );
    }

    /**
     * Moves all child folders of one parent to another parent (or to the dataset root) in a single UPDATE.
     *
     * @param nDatasetId
     *            The dataset ID
     * @param nOldParentFolderId
     *            The source parent folder ID
     * @param nNewParentFolderId
     *            The destination parent folder ID, or null for the dataset root
     */
    public static void reparentFolders( int nDatasetId, int nOldParentFolderId, Integer nNewParentFolderId )
    {
        _dao.reparentFolders( nDatasetId, nOldParentFolderId, nNewParentFolderId, _plugin );
    }
}
