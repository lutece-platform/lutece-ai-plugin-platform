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
 * Interface for data access operations on DatasetDocument objects
 */
public interface IDatasetDocumentDAO
{
    /**
     * Inserts a new DatasetDocument object in the database
     *
     * @param datasetDocument
     *            The DatasetDocument object to insert
     * @param plugin
     *            The Plugin using this data access service
     */
    void insert( DatasetDocument datasetDocument, Plugin plugin );

    /**
     * Updates a DatasetDocument in the database
     *
     * @param datasetDocument
     *            The DatasetDocument object to store
     * @param plugin
     *            The Plugin using this data access service
     */
    void store( DatasetDocument datasetDocument, Plugin plugin );

    /**
     * Deletes a DatasetDocument from the database
     *
     * @param nKey
     *            The identifier of the DatasetDocument to delete
     * @param plugin
     *            The Plugin using this data access service
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Deletes all DatasetDocuments associated with a dataset
     *
     * @param nDatasetId
     *            The identifier of the dataset
     * @param plugin
     *            The Plugin using this data access service
     */
    void deleteByDatasetId( int nDatasetId, Plugin plugin );

    /**
     * Loads a DatasetDocument from the database
     *
     * @param nKey
     *            The identifier of the DatasetDocument to load
     * @param plugin
     *            The Plugin using this data access service
     * @return The DatasetDocument object matching the specified identifier, or empty if not found
     */
    Optional<DatasetDocument> load( int nKey, Plugin plugin );

    /**
     * Returns all DatasetDocuments
     *
     * @param plugin
     *            The Plugin using this data access service
     * @return A list of all DatasetDocuments
     */
    List<DatasetDocument> selectDatasetDocumentsList( Plugin plugin );

    /**
     * Returns all DatasetDocuments associated with a dataset
     *
     * @param nDatasetId
     *            The identifier of the dataset
     * @param plugin
     *            The Plugin using this data access service
     * @return A list of DatasetDocuments associated with the specified dataset
     */
    List<DatasetDocument> selectDatasetDocumentsListByDatasetId( int nDatasetId, Plugin plugin );

    /**
     * Find a DatasetDocument by name and dataset ID
     *
     * @param strName
     *            The document name
     * @param nDatasetId
     *            The dataset identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return The DatasetDocument object matching the name and dataset ID, or empty if not found
     */
    Optional<DatasetDocument> findByNameAndDatasetId( String strName, int nDatasetId, Plugin plugin );

    /**
     * Finds a document by its name, folder and dataset — used for deduplication scoped to a folder.
     *
     * @param strName
     *            The document name
     * @param nFolderId
     *            The folder identifier, or null for the dataset root
     * @param nDatasetId
     *            The dataset identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return The matching DatasetDocument, or empty if not found
     */
    Optional<DatasetDocument> findByNameFolderAndDatasetId( String strName, Integer nFolderId, int nDatasetId, Plugin plugin );

    /**
     * Returns the documents sitting directly inside a given folder (non-recursive). Pass null folderId for documents at the dataset root.
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param nFolderId
     *            The folder identifier, or null for dataset root
     * @param plugin
     *            The Plugin using this data access service
     * @return A list of DatasetDocuments in the specified folder
     */
    List<DatasetDocument> selectByDatasetIdAndFolderId( int nDatasetId, Integer nFolderId, Plugin plugin );

    /**
     * Moves all documents currently in one folder to another (or to the dataset root). Single UPDATE, no row reload.
     *
     * @param nDatasetId
     *            The dataset identifier
     * @param nOldFolderId
     *            The source folder identifier
     * @param nNewFolderId
     *            The destination folder identifier, or null for the dataset root
     * @param plugin
     *            The Plugin using this data access service
     */
    void reparentDocuments( int nDatasetId, int nOldFolderId, Integer nNewFolderId, Plugin plugin );
}
