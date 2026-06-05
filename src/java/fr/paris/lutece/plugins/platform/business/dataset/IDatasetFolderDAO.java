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

/**
 * Interface for data access operations on DatasetFolder objects
 */
public interface IDatasetFolderDAO
{
    /**
     * Inserts a new DatasetFolder object in the database
     *
     * @param folder
     *            The DatasetFolder object to insert
     * @param plugin
     *            The Plugin using this data access service
     */
    void insert( DatasetFolder folder, Plugin plugin );

    /**
     * Updates a DatasetFolder in the database
     *
     * @param folder
     *            The DatasetFolder object to store
     * @param plugin
     *            The Plugin using this data access service
     */
    void store( DatasetFolder folder, Plugin plugin );

    /**
     * Deletes a DatasetFolder from the database
     *
     * @param nKey
     *            The identifier of the DatasetFolder to delete
     * @param plugin
     *            The Plugin using this data access service
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Loads a DatasetFolder from the database
     *
     * @param nKey
     *            The identifier of the DatasetFolder to load
     * @param plugin
     *            The Plugin using this data access service
     * @return The DatasetFolder object matching the specified identifier, or empty if not found
     */
    Optional<DatasetFolder> load( int nKey, Plugin plugin );

    /**
     * Returns all folders for a given dataset (used to build the tree in memory)
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return A list of DatasetFolder objects, in no particular order
     */
    List<DatasetFolder> selectByDatasetId( int nDatasetId, Plugin plugin );

    /**
     * Returns the direct children folders of a given parent. Pass null parentFolderId to list root folders of the dataset.
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param nParentFolderId
     *            The parent folder identifier, or null for root folders
     * @param plugin
     *            The Plugin using this data access service
     * @return A list of immediate child folders, sorted by name
     */
    List<DatasetFolder> selectChildren( int nDatasetId, Integer nParentFolderId, Plugin plugin );

    /**
     * Find a folder by its dataset ID, parent folder ID and name (unique triple).
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param nParentFolderId
     *            The parent folder identifier, or null for root folders
     * @param strName
     *            The folder name
     * @param plugin
     *            The Plugin using this data access service
     * @return The matching DatasetFolder, or empty if not found
     */
    Optional<DatasetFolder> findByParentAndName( int nDatasetId, Integer nParentFolderId, String strName, Plugin plugin );

    /**
     * Moves all folders currently under one parent to another (or to the dataset root). Single UPDATE.
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param nOldParentFolderId
     *            The source parent folder identifier
     * @param nNewParentFolderId
     *            The destination parent folder identifier, or null for the dataset root
     * @param plugin
     *            The Plugin using this data access service
     */
    void reparentFolders( int nDatasetId, int nOldParentFolderId, Integer nNewParentFolderId, Plugin plugin );
}
