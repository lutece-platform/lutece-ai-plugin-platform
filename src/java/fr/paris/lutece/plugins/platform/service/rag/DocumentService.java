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
package fr.paris.lutece.plugins.platform.service.rag;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.upload.MultipartItem;
import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.platform.business.bot.BotDatasetHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Service class for managing dataset documents
 */
public class DocumentService
{
    private static final String ERROR_CANNOT_STORE_DOCUMENT = "Cannot store document: file item is null or empty";
    private static final String ERROR_STORING_DOCUMENT = "Error storing document: ";
    private static final String ERROR_UNEXPECTED_STORING_DOCUMENT = "Unexpected error storing document: ";
    private static final String ERROR_EMBEDDING_JOB = "Error creating embedding job for document ";
    private static final String ERROR_RETRIEVING_DOCUMENT = "Error retrieving document: ";
    private static final String ERROR_DELETING_DOCUMENT = "Error deleting document: ";
    private static final String ERROR_DELETING_DATASET_DOCUMENTS = "Error deleting dataset documents: ";
    private static final String ERROR_CREATING_EMBEDDING_JOB = "Error creating embedding job: ";
    private static final String ERROR_DATASET_NOT_ATTACHED = "Dataset %d is not attached to bot %d";
    private static final String ERROR_DOCUMENT_NOT_FOUND = "Document %d not found or not attached to dataset %d";

    private static final String DEFAULT_DOCUMENT_NAME = "document.txt";

    /**
     * File storage access, package-private and lazily resolved so tests can substitute a recording fake (same testability pattern as DatasetService seams).
     */
    static IFileStoreServiceProvider _fileService;

    private static final ElasticsearchService _elasticsearch = CDI.current( ).select( ElasticsearchService.class ).get( );
    private static final EmbeddingService _embedding = CDI.current( ).select( EmbeddingService.class ).get( );

    /**
     * Embeddings deletion step used when replacing a document, package-private for the same testability reason as {@link #_fileService}.
     */
    static java.util.function.BiConsumer<Integer, Integer> _embeddingsDelete = ( datasetId, documentId ) -> _elasticsearch.deleteDocumentEmbeddings( datasetId,
            documentId );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private DocumentService( )
    {
    }

    /**
     * Gets the file service instance
     *
     * @return The file service
     */
    private static IFileStoreServiceProvider getFileService( )
    {
        if ( _fileService == null )
        {
            _fileService = CDI.current( ).select( IFileStoreServiceProvider.class ).get( );
        }
        return _fileService;
    }

    /**
     * Stores a document. If a document with the same name already exists in the dataset, it will be replaced
     *
     * @param document
     *            The dataset document to store
     * @param fileItem
     *            The file item
     * @return The created or updated dataset document or null if an error occurred
     */
    public static DatasetDocument storeDocument( DatasetDocument document, MultipartItem fileItem )
    {
        try
        {
            if ( fileItem == null || StringUtils.isEmpty( fileItem.getName( ) ) )
            {
                AppLogService.error( ERROR_CANNOT_STORE_DOCUMENT );
                return null;
            }

            Optional<DatasetDocument> existingDocument = DatasetDocumentHome.findByNameFolderAndDatasetId( document.getName( ), document.getFolderId( ),
                    document.getDatasetId( ) );

            if ( existingDocument.isPresent( ) )
            {
                DatasetDocument existing = existingDocument.get( );
                String strOldFileKey = existing.getFileKey( );

                String strFileKey = getFileService( ).storeFileItem( fileItem );

                try
                {
                    _embeddingsDelete.accept( existing.getDatasetId( ), existing.getId( ) );
                    getFileService( ).delete( strOldFileKey );
                }
                catch( FileServiceException e )
                {
                    AppLogService.error( "Error deleting old document content: {}", e.getMessage( ), e );
                }

                existing.setFileKey( strFileKey );
                existing.setChunkSize( document.getChunkSize( ) );
                existing.setChunkOverlap( document.getChunkOverlap( ) );
                existing.setUseDocumentIntelligence( document.getUseDocumentIntelligence( ) );
                existing.setDocumentIntelligenceProviderId( document.getDocumentIntelligenceProviderId( ) );

                DatasetDocument updatedDocument = DatasetDocumentHome.update( existing );

                try
                {
                    createEmbeddingJob( updatedDocument );
                }
                catch( Exception e )
                {
                    AppLogService.error( "{}{}: {}", ERROR_EMBEDDING_JOB, updatedDocument.getId( ), e.getMessage( ), e );
                }

                return updatedDocument;
            }
            else
            {
                String strFileKey = getFileService( ).storeFileItem( fileItem );
                document.setFileKey( strFileKey );
                DatasetDocument createdDocument = DatasetDocumentHome.create( document );

                try
                {
                    createEmbeddingJob( createdDocument );
                }
                catch( Exception e )
                {
                    AppLogService.error( "{}{}: {}", ERROR_EMBEDDING_JOB, document.getId( ), e.getMessage( ), e );
                }

                return createdDocument;
            }
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_STORING_DOCUMENT, e.getMessage( ), e );
            return null;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_UNEXPECTED_STORING_DOCUMENT, e.getMessage( ), e );
            return null;
        }
    }

    /**
     * Gets a document file as input stream by its file key
     *
     * @param strFileKey
     *            The file key
     * @return The document input stream or null if an error occurred
     */
    public static InputStream getDocumentFile( String strFileKey )
    {
        try
        {
            return getFileService( ).getInputStream( strFileKey );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_RETRIEVING_DOCUMENT, e.getMessage( ), e );
            return null;
        }
    }

    /**
     * Deletes a document
     *
     * @param document
     *            The document to delete
     */
    public static void deleteDocument( DatasetDocument document )
    {
        try
        {
            int datasetId = document.getDatasetId( );
            int documentId = document.getId( );

            _elasticsearch.deleteDocumentEmbeddings( datasetId, documentId );

            DatasetDocumentHome.remove( document.getId( ) );

            getFileService( ).delete( document.getFileKey( ) );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_DELETING_DOCUMENT, e.getMessage( ), e );
        }
    }

    /**
     * Deletes all documents associated with a dataset
     *
     * @param nDatasetId
     *            The dataset ID
     */
    public static void deleteDocumentsByDatasetId( int nDatasetId )
    {
        try
        {
            _elasticsearch.deleteDatasetEmbeddings( nDatasetId );

            for ( DatasetDocument document : DatasetDocumentHome.getDatasetDocumentsListByDatasetId( nDatasetId ) )
            {
                getFileService( ).delete( document.getFileKey( ) );
            }

            DatasetDocumentHome.removeByDatasetId( nDatasetId );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_DELETING_DATASET_DOCUMENTS, e.getMessage( ), e );
        }
    }

    /**
     * Creates an embedding job for a document
     *
     * @param document
     *            The document
     */
    private static void createEmbeddingJob( DatasetDocument document )
    {
        try
        {
            _embedding.createEmbeddingJob( document );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_CREATING_EMBEDDING_JOB, e.getMessage( ), e );
        }
    }

    /**
     * Gets a document file with validation of bot, dataset and document
     *
     * @param botId
     *            The bot ID
     * @param datasetId
     *            The dataset ID
     * @param documentId
     *            The document ID
     * @return The document input stream or null if validation failed
     */
    public static InputStream getDocumentFileWithValidation( int botId, int datasetId, int documentId )
    {
        List<Integer> botDatasetIds = BotDatasetHome.getDatasetIdsByBotId( botId );

        if ( !botDatasetIds.contains( datasetId ) )
        {
            AppLogService.error( String.format( ERROR_DATASET_NOT_ATTACHED, datasetId, botId ) );
            return null;
        }

        Optional<DatasetDocument> optDocument = DatasetDocumentHome.findByPrimaryKey( documentId );
        if ( optDocument.isEmpty( ) || optDocument.get( ).getDatasetId( ) != datasetId )
        {
            AppLogService.error( String.format( ERROR_DOCUMENT_NOT_FOUND, documentId, datasetId ) );
            return null;
        }

        DatasetDocument document = optDocument.get( );
        return getDocumentFile( document.getFileKey( ) );
    }

    /**
     * Gets a document name by its ID
     *
     * @param documentId
     *            The document ID
     * @return The document name or a default name if not found
     */
    public static String getDocumentName( int documentId )
    {
        return DatasetDocumentHome.findByPrimaryKey( documentId ).map( DatasetDocument::getName ).orElse( DEFAULT_DOCUMENT_NAME );
    }
}
