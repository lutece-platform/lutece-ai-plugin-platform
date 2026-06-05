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
 * This class provides instances management methods for DatasetDocument objects
 */
public final class DatasetDocumentHome
{
    private static IDatasetDocumentDAO _dao = CDI.current( ).select( IDatasetDocumentDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private DatasetDocumentHome( )
    {
    }

    /**
     * Creates a new DatasetDocument in the database
     *
     * @param datasetDocument
     *            The DatasetDocument to create
     * @return The created DatasetDocument populated with the generated ID
     */
    public static DatasetDocument create( DatasetDocument datasetDocument )
    {
        _dao.insert( datasetDocument, _plugin );
        return datasetDocument;
    }

    /**
     * Updates a DatasetDocument in the database
     *
     * @param datasetDocument
     *            The DatasetDocument to update
     * @return The updated DatasetDocument
     */
    public static DatasetDocument update( DatasetDocument datasetDocument )
    {
        _dao.store( datasetDocument, _plugin );
        return datasetDocument;
    }

    /**
     * Removes a DatasetDocument from the database
     *
     * @param nKey
     *            The DatasetDocument ID to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Removes all DatasetDocuments associated with a specific dataset
     *
     * @param nDatasetId
     *            The dataset ID
     */
    public static void removeByDatasetId( int nDatasetId )
    {
        _dao.deleteByDatasetId( nDatasetId, _plugin );
    }

    /**
     * Returns a DatasetDocument from the database
     *
     * @param nKey
     *            The DatasetDocument ID
     * @return The DatasetDocument matching the ID, or empty if not found
     */
    public static Optional<DatasetDocument> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of all DatasetDocuments
     *
     * @return A list of all DatasetDocuments
     */
    public static List<DatasetDocument> getDatasetDocumentsList( )
    {
        return _dao.selectDatasetDocumentsList( _plugin );
    }

    /**
     * Returns the list of all DatasetDocuments associated with a specific dataset
     *
     * @param nDatasetId
     *            The dataset ID
     * @return A list of all DatasetDocuments for the specified dataset
     */
    public static List<DatasetDocument> getDatasetDocumentsListByDatasetId( int nDatasetId )
    {
        return _dao.selectDatasetDocumentsListByDatasetId( nDatasetId, _plugin );
    }

    /**
     * Find a DatasetDocument by name and dataset ID
     *
     * @param strName
     *            The document name
     * @param nDatasetId
     *            The dataset identifier
     * @return The DatasetDocument object matching the name and dataset ID, or empty if not found
     */
    public static Optional<DatasetDocument> findByNameAndDatasetId( String strName, int nDatasetId )
    {
        return _dao.findByNameAndDatasetId( strName, nDatasetId, _plugin );
    }

    /**
     * Finds a document by its name, folder and dataset — used for deduplication scoped to a folder.
     *
     * @param strName
     *            The document name
     * @param nFolderId
     *            The folder identifier, or null for the dataset root
     * @param nDatasetId
     *            The dataset identifier
     * @return The matching DatasetDocument, or empty if not found
     */
    public static Optional<DatasetDocument> findByNameFolderAndDatasetId( String strName, Integer nFolderId, int nDatasetId )
    {
        return _dao.findByNameFolderAndDatasetId( strName, nFolderId, nDatasetId, _plugin );
    }

    /**
     * Returns the documents sitting directly inside a given folder (non-recursive). Pass null folderId for documents at the dataset root.
     *
     * @param nDatasetId
     *            The dataset ID
     * @param nFolderId
     *            The folder ID, or null for dataset root
     * @return A list of DatasetDocuments in the specified folder
     */
    public static List<DatasetDocument> getDocumentsByFolderId( int nDatasetId, Integer nFolderId )
    {
        return _dao.selectByDatasetIdAndFolderId( nDatasetId, nFolderId, _plugin );
    }

    /**
     * Moves all documents from one folder to another (or to the dataset root) in a single UPDATE.
     *
     * @param nDatasetId
     *            The dataset ID
     * @param nOldFolderId
     *            The source folder ID
     * @param nNewFolderId
     *            The destination folder ID, or null for the dataset root
     */
    public static void reparentDocuments( int nDatasetId, int nOldFolderId, Integer nNewFolderId )
    {
        _dao.reparentDocuments( nDatasetId, nOldFolderId, nNewFolderId, _plugin );
    }
}
