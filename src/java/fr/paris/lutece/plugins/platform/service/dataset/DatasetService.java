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
package fr.paris.lutece.plugins.platform.service.dataset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import jakarta.enterprise.inject.spi.CDI;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.httpaccess.MemoryFileItem;

import fr.paris.lutece.plugins.platform.business.dataset.DatasetDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetUploadRequestDTO;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJobHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJob;
import fr.paris.lutece.plugins.platform.service.bot.tools.FolderPathResolver;
import fr.paris.lutece.plugins.platform.service.dataset.dto.BreadcrumbEntry;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentJobView;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DatasetViewData;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentDownload;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentSegmentsView;
import fr.paris.lutece.plugins.platform.service.dataset.dto.FolderOption;
import fr.paris.lutece.plugins.platform.service.dataset.dto.IngestResult;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.rag.DocumentService;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;

/**
 * Service class for managing datasets, folders and documents. Encapsulates all business rules (ownership checks, folder integrity, file validation, upload
 * rollback) so REST endpoints and XPages only deal with HTTP/UI concerns.
 */
public final class DatasetService
{
    /**
     * Storage step of the upload flow, package-private so tests can substitute the I/O-bound document store (file store + Elasticsearch) with an HSQL-only
     * stub.
     */
    static BiFunction<DatasetDocument, MultipartItem, DatasetDocument> _documentStore = DocumentService::storeDocument;

    /**
     * Deletion step used by the upload rollback, package-private for the same testability reason as {@link #_documentStore}.
     */
    static Consumer<DatasetDocument> _documentDelete = DocumentService::deleteDocument;

    private static final String DATASET_NOT_FOUND_MESSAGE = "Dataset not found or access denied.";
    private static final String DOCUMENT_NOT_FOUND_MESSAGE = "Document not found or access denied.";
    private static final String NO_FILE_PROVIDED_MESSAGE = "No file provided.";
    private static final String FILE_TOO_LARGE_MESSAGE = "File is too large.";
    private static final String INVALID_FILE_TYPE_MESSAGE = "File type not allowed: ";
    private static final String INVALID_BASE64_MESSAGE = "Invalid base64 encoding for file: ";
    private static final String FOLDER_NOT_IN_DATASET_MESSAGE = "Target folder does not belong to this dataset.";
    private static final String FOLDER_NAME_REQUIRED_MESSAGE = "Folder name is required.";
    private static final String FOLDER_NAME_DUPLICATE_MESSAGE = "A folder with this name already exists at the same level.";
    private static final String INVALID_PARENT_FOLDER_MESSAGE = "Parent folder is invalid or does not belong to this dataset.";
    private static final String STORE_DOCUMENT_ERROR_MESSAGE = "Error storing document: ";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of( "application/pdf", "text/plain", "text/markdown", "text/csv", "application/json",
            "application/xml", "text/xml", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" );

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;
    private static final String DOWNLOAD_DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String INVALID_PATHS_JSON_MESSAGE = "Invalid document_paths JSON: ";
    private static final String DOCUMENT_METADATA_ERROR_MESSAGE = "Could not get file metadata for document ";
    private static final String DOWNLOAD_ERROR_MESSAGE = "Error downloading document ";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );
    private static final ElasticsearchService ELASTICSEARCH_SERVICE = CDI.current( ).select( ElasticsearchService.class ).get( );
    private static final EmbeddingService EMBEDDING_SERVICE = CDI.current( ).select( EmbeddingService.class ).get( );
    private static IFileStoreServiceProvider _fileStoreService;

    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries( Map.entry( ".pdf", "application/pdf" ),
            Map.entry( ".txt", "text/plain" ), Map.entry( ".md", "text/markdown" ), Map.entry( ".markdown", "text/markdown" ), Map.entry( ".csv", "text/csv" ),
            Map.entry( ".json", "application/json" ), Map.entry( ".xml", "application/xml" ), Map.entry( ".doc", "application/msword" ),
            Map.entry( ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ), Map.entry( ".xls", "application/vnd.ms-excel" ),
            Map.entry( ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ) );

    /**
     * Private constructor for utility class.
     */
    private DatasetService( )
    {
    }

    /**
     * Lists all datasets owned by a client.
     *
     * @param clientId
     *            The client identifier
     * @return The list of datasets as DTOs
     */
    public static List<DatasetDTO> listDatasetsForClient( int clientId )
    {
        List<DatasetDTO> dtos = new ArrayList<>( );
        for ( Dataset dataset : DatasetHome.getDatasetsByClientId( clientId ) )
        {
            dtos.add( toDTO( dataset ) );
        }
        return dtos;
    }

    /**
     * Retrieves a dataset as DTO.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The dataset DTO
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     */
    public static DatasetDTO getDataset( int datasetId )
    {
        return toDTO( requireDataset( datasetId ) );
    }

    /**
     * Lists all documents of a dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The list of documents as DTOs
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     */
    public static List<DatasetDocumentDTO> listDocuments( int datasetId )
    {
        requireDataset( datasetId );
        List<DatasetDocumentDTO> dtos = new ArrayList<>( );
        for ( DatasetDocument doc : DatasetDocumentHome.getDatasetDocumentsListByDatasetId( datasetId ) )
        {
            dtos.add( toDTO( doc ) );
        }
        return dtos;
    }

    /**
     * Lists the ingestion jobs of a dataset enriched with their document names, for a user whose view permission on the dataset is enforced here. Document
     * names are resolved in a single pass over the dataset's documents.
     *
     * @param datasetId
     *            The dataset identifier
     * @param user
     *            The requesting user
     * @return The job views, most recent ordering preserved from the Home
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws AccessDeniedException
     *             if the user has no view permission on the dataset
     */
    public static List<DocumentJobView> getDatasetJobs( int datasetId, User user )
    {
        Dataset dataset = requireDataset( datasetId );
        if ( !AgentRBACService.canViewDataset( dataset, user ) )
        {
            throw new AccessDeniedException( DATASET_NOT_FOUND_MESSAGE );
        }

        Map<Integer, DatasetDocument> documentsById = new HashMap<>( );
        for ( DatasetDocument document : DatasetDocumentHome.getDatasetDocumentsListByDatasetId( datasetId ) )
        {
            documentsById.put( document.getId( ), document );
        }

        List<DocumentJobView> views = new ArrayList<>( );
        for ( DatasetDocumentJob job : DatasetDocumentJobHome.getJobsByDatasetId( datasetId ) )
        {
            DatasetDocument document = documentsById.get( job.getDocumentId( ) );
            views.add( new DocumentJobView( job.getId( ), job.getDocumentId( ), job.getStatus( ), job.getErrorMessage( ), job.getCreated( ), job.getUpdated( ),
                    document != null ? document.getName( ) : null, document != null ? document.getDescription( ) : null ) );
        }
        return views;
    }

    /**
     * Lists all folders of a dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The list of folders as DTOs
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     */
    public static List<DatasetFolderDTO> listFolders( int datasetId )
    {
        requireDataset( datasetId );
        List<DatasetFolderDTO> dtos = new ArrayList<>( );
        for ( DatasetFolder folder : DatasetFolderHome.getFoldersByDatasetId( datasetId ) )
        {
            dtos.add( toDTO( folder ) );
        }
        return dtos;
    }

    /**
     * Creates a folder inside a dataset. If a folder with the same name already exists under the same parent, the existing folder is returned so the operation
     * is idempotent for batch scripts that replay creation.
     *
     * @param datasetId
     *            The dataset identifier
     * @param payload
     *            The folder payload (name required, parent_folder_id + description optional)
     * @return The created or pre-existing folder DTO
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws InvalidRequestException
     *             if the payload is invalid or parent folder is invalid
     */
    public static DatasetFolderDTO createFolder( int datasetId, DatasetFolderDTO payload )
    {
        if ( payload == null || payload.getName( ) == null || payload.getName( ).trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( FOLDER_NAME_REQUIRED_MESSAGE );
        }

        requireDataset( datasetId );

        Integer parentFolderId = payload.getParentFolderId( );
        if ( parentFolderId != null )
        {
            DatasetFolder parent = DatasetFolderHome.findByPrimaryKey( parentFolderId ).orElse( null );
            if ( parent == null || parent.getDatasetId( ) != datasetId )
            {
                throw new InvalidRequestException( INVALID_PARENT_FOLDER_MESSAGE );
            }
        }

        String folderName = payload.getName( ).trim( );
        DatasetFolder existing = DatasetFolderHome.findByParentAndName( datasetId, parentFolderId, folderName ).orElse( null );
        if ( existing != null )
        {
            return toDTO( existing );
        }

        DatasetFolder folder = new DatasetFolder( );
        folder.setDatasetId( datasetId );
        folder.setParentFolderId( parentFolderId );
        folder.setName( folderName );
        folder.setDescription( payload.getDescription( ) );
        DatasetFolderHome.create( folder );

        return toDTO( folder );
    }

    /**
     * Uploads documents to a dataset with atomic rollback semantics. If any file fails validation or storage, all documents created in the same call are
     * deleted before the exception propagates. Documents that already existed before the call (replaced by the batch) are never rolled back: deleting them
     * would destroy pre-existing data.
     *
     * @param datasetId
     *            The dataset identifier
     * @param request
     *            The upload request (non-null, must contain at least one file)
     * @return The list of created document identifiers
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws InvalidRequestException
     *             if the request, folder, file type or file size is invalid
     * @throws AgentServiceException
     *             if document storage fails
     */
    public static List<Integer> uploadDocuments( int datasetId, DatasetUploadRequestDTO request )
    {
        requireDataset( datasetId );

        if ( request == null || request.getFiles( ) == null || request.getFiles( ).isEmpty( ) )
        {
            throw new InvalidRequestException( NO_FILE_PROVIDED_MESSAGE );
        }

        Integer targetFolderId = request.getFolderId( );
        if ( targetFolderId != null )
        {
            DatasetFolder folder = DatasetFolderHome.findByPrimaryKey( targetFolderId ).orElse( null );
            if ( folder == null || folder.getDatasetId( ) != datasetId )
            {
                throw new InvalidRequestException( FOLDER_NOT_IN_DATASET_MESSAGE );
            }
        }

        List<DatasetDocument> created = new ArrayList<>( );
        long totalSize = 0;
        try
        {
            for ( DatasetUploadRequestDTO.FileUploadDTO fileUpload : request.getFiles( ) )
            {
                byte [ ] content = decodeBase64( fileUpload );
                totalSize += content.length;
                if ( totalSize > MAX_FILE_SIZE )
                {
                    throw new InvalidRequestException( FILE_TOO_LARGE_MESSAGE );
                }

                String fileName = fileUpload.getFileName( );
                String contentType = validateAndResolveContentType( fileName );

                DatasetDocument document = buildDocument( datasetId, targetFolderId, fileName, request );
                boolean existedBefore = DatasetDocumentHome.findByNameFolderAndDatasetId( fileName, targetFolderId, datasetId ).isPresent( );
                DatasetDocument stored = _documentStore.apply( document, createFileItem( fileName, contentType, content ) );
                if ( stored == null )
                {
                    throw new AgentServiceException( STORE_DOCUMENT_ERROR_MESSAGE + fileName );
                }
                if ( !existedBefore )
                {
                    created.add( stored );
                }
            }
            return created.stream( ).map( DatasetDocument::getId ).toList( );
        }
        catch( RuntimeException e )
        {
            rollback( created );
            throw e;
        }
    }

    /**
     * Deletes a document from a dataset after verifying it belongs to the dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @param documentId
     *            The document identifier
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws ResourceNotFoundException
     *             if the document does not exist or does not belong to the dataset
     */
    public static void deleteDocument( int datasetId, int documentId )
    {
        requireDataset( datasetId );
        DatasetDocument document = DatasetDocumentHome.findByPrimaryKey( documentId ).orElse( null );
        if ( document == null || document.getDatasetId( ) != datasetId )
        {
            throw new ResourceNotFoundException( DOCUMENT_NOT_FOUND_MESSAGE );
        }
        DocumentService.deleteDocument( document );
    }

    /**
     * Aggregates the dataset detail view for a given folder: the documents and subfolders under the folder, the resolved current folder, the breadcrumb trail,
     * and the per-document chunk counts and file sizes. Performs all folder resolution, Elasticsearch chunk counting and file-store metadata lookups in one
     * pass so the controller only fills the model.
     *
     * @param datasetId
     *            The dataset identifier
     * @param folderId
     *            The folder being browsed, or null for the dataset root (an unknown id resolves to the root)
     * @return The aggregated view data
     */
    public static DatasetViewData getDatasetViewData( int datasetId, Integer folderId )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( datasetId );
        Integer currentFolderId = folderId;
        DatasetFolder currentFolder = null;
        if ( currentFolderId != null )
        {
            currentFolder = resolver.findById( currentFolderId );
            if ( currentFolder == null )
            {
                currentFolderId = null;
            }
        }

        List<DatasetDocument> documents = DatasetDocumentHome.getDocumentsByFolderId( datasetId, currentFolderId );
        List<DatasetFolder> subfolders = resolver.getChildren( currentFolderId );
        List<BreadcrumbEntry> breadcrumb = buildBreadcrumb( resolver, currentFolderId );

        Map<String, Long> documentChunks = ELASTICSEARCH_SERVICE.indexExists( datasetId ) ? ELASTICSEARCH_SERVICE.countSegmentsByDocument( datasetId )
                : Collections.emptyMap( );
        Map<String, Integer> documentFileSizes = new HashMap<>( );
        for ( DatasetDocument doc : documents )
        {
            File fileMeta = readFileMetaData( doc.getFileKey( ), doc.getId( ) );
            if ( fileMeta != null )
            {
                documentFileSizes.put( String.valueOf( doc.getId( ) ), fileMeta.getSize( ) );
            }
        }

        return new DatasetViewData( currentFolder, documents, subfolders, breadcrumb, documentChunks, documentFileSizes );
    }

    /**
     * Builds the list of folder select options for a dataset: each folder with its computed full path, sorted by path.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The folder options ordered by path
     */
    public static List<FolderOption> listFolderOptions( int datasetId )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( datasetId );
        List<FolderOption> options = new ArrayList<>( );
        for ( DatasetFolder folder : resolver.getAllFolders( ) )
        {
            options.add( new FolderOption( folder.getId( ), resolver.pathOf( folder.getId( ) ) ) );
        }
        options.sort( ( a, b ) -> a.getPath( ).compareTo( b.getPath( ) ) );
        return options;
    }

    /**
     * Resolves the path of a folder within a dataset (e.g. "/contracts/2024").
     *
     * @param datasetId
     *            The dataset identifier
     * @param folderId
     *            The folder identifier, or null for the dataset root
     * @return The absolute folder path
     */
    public static String folderPath( int datasetId, Integer folderId )
    {
        return FolderPathResolver.forDataset( datasetId ).pathOf( folderId );
    }

    /**
     * Ingests one or more uploaded files into a dataset, creating any missing intermediate folders implied by the browser relative paths. Each file is
     * validated against the content-type allow-list, stored via the document service, and counted as a success or failure. Files that fail validation or
     * storage do not abort the batch.
     *
     * @param datasetId
     *            The dataset identifier
     * @param targetFolderId
     *            The root folder selected in the form, or null for the dataset root (an id not belonging to the dataset is treated as the root)
     * @param files
     *            The uploaded multipart items (entries with a blank name are skipped)
     * @param relativePaths
     *            The browser relative paths aligned by index with {@code files}, used to recreate the folder tree
     * @param chunkSize
     *            The chunk size to apply to each document
     * @param chunkOverlap
     *            The chunk overlap to apply to each document
     * @param useDocumentIntelligence
     *            true to enable document intelligence extraction
     * @param diProviderId
     *            The document-intelligence provider id, or null when not used
     * @return The ingest result carrying success and failure counts
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     */
    public static IngestResult ingestMultipartDocuments( int datasetId, Integer targetFolderId, List<MultipartItem> files, List<String> relativePaths,
            int chunkSize, int chunkOverlap, boolean useDocumentIntelligence, Integer diProviderId )
    {
        requireDataset( datasetId );

        Integer rootFolderId = resolveTargetFolder( datasetId, targetFolderId );
        int successCount = 0;
        int failureCount = 0;

        for ( int i = 0; i < files.size( ); i++ )
        {
            MultipartItem fileItem = files.get( i );
            if ( fileItem == null || isBlank( fileItem.getName( ) ) )
            {
                continue;
            }

            String relativePath = i < relativePaths.size( ) ? relativePaths.get( i ) : null;
            String basename = extractBasename( fileItem.getName( ), relativePath );

            try
            {
                validateAndResolveContentType( basename );
            }
            catch( InvalidRequestException e )
            {
                AppLogService.info( e.getMessage( ) );
                failureCount++;
                continue;
            }

            Integer leafFolderId = resolveOrCreateFolderPath( datasetId, rootFolderId, relativePath );

            DatasetDocument document = new DatasetDocument( );
            document.setDatasetId( datasetId );
            document.setName( basename );
            document.setFolderId( leafFolderId );
            if ( useDocumentIntelligence && diProviderId != null )
            {
                document.setUseDocumentIntelligence( true );
                document.setDocumentIntelligenceProviderId( diProviderId );
            }
            document.setChunkSize( chunkSize );
            document.setChunkOverlap( chunkOverlap );

            DatasetDocument stored = DocumentService.storeDocument( document, fileItem );
            if ( stored != null )
            {
                successCount++;
            }
            else
            {
                failureCount++;
            }
        }

        return new IngestResult( successCount, failureCount );
    }

    /**
     * Parses the document_paths JSON array sent by the upload form (e.g. ["a/b/doc.pdf","doc2.pdf"]). Returns an empty list on null, blank or malformed input
     * so a flat upload still works.
     *
     * @param json
     *            The raw JSON array string, may be null
     * @return The list of relative paths in the same order as the uploaded files
     */
    public static List<String> parseRelativePaths( String json )
    {
        if ( json == null || json.trim( ).isEmpty( ) )
        {
            return new ArrayList<>( );
        }
        try
        {
            return new ArrayList<>( Arrays.asList( OBJECT_MAPPER.readValue( json, String [ ].class ) ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", INVALID_PATHS_JSON_MESSAGE, e.getMessage( ), e );
            return new ArrayList<>( );
        }
    }

    /**
     * Reindexes a whole dataset: deletes its Elasticsearch embeddings, clears pending embedding jobs and schedules a new embedding job for every document.
     *
     * @param datasetId
     *            The dataset identifier
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws AgentServiceException
     *             if the reindex orchestration fails
     */
    public static void reindexDataset( int datasetId )
    {
        requireDataset( datasetId );
        try
        {
            ELASTICSEARCH_SERVICE.deleteDatasetEmbeddings( datasetId );
            DatasetDocumentJobHome.removeByDatasetId( datasetId );
            for ( DatasetDocument document : DatasetDocumentHome.getDatasetDocumentsListByDatasetId( datasetId ) )
            {
                EMBEDDING_SERVICE.createEmbeddingJob( document );
            }
        }
        catch( RuntimeException e )
        {
            throw new AgentServiceException( "Dataset reindexing failed for dataset " + datasetId, e );
        }
    }

    /**
     * Retrieves the segments of a document for display: branches on whether the dataset index exists, fetching the segment list and total count from
     * Elasticsearch when it does.
     *
     * @param datasetId
     *            The dataset identifier
     * @param documentId
     *            The document identifier
     * @return The typed segments view
     */
    public static DocumentSegmentsView getDocumentSegments( int datasetId, int documentId )
    {
        if ( !ELASTICSEARCH_SERVICE.indexExists( datasetId ) )
        {
            return new DocumentSegmentsView( false, new ArrayList<>( ), 0L );
        }
        List<Map<String, Object>> segments = ELASTICSEARCH_SERVICE.getDocumentSegments( datasetId, documentId );
        long segmentCount = ELASTICSEARCH_SERVICE.countDocumentSegments( datasetId, documentId );
        return new DocumentSegmentsView( true, segments, segmentCount );
    }

    /**
     * Reads a document's binary content, file name and MIME type from the file store.
     *
     * @param documentId
     *            The document identifier
     * @return The download payload, or empty when the document or its bytes cannot be retrieved
     * @throws ResourceNotFoundException
     *             if the document does not exist
     */
    public static Optional<DocumentDownload> getDocumentDownload( int documentId )
    {
        DatasetDocument document = DatasetDocumentHome.findByPrimaryKey( documentId )
                .orElseThrow( ( ) -> new ResourceNotFoundException( DOCUMENT_NOT_FOUND_MESSAGE ) );
        try
        {
            File fileMeta = getFileStoreService( ).getFileMetaData( document.getFileKey( ) );
            try ( InputStream inputStream = DocumentService.getDocumentFile( document.getFileKey( ) ) )
            {
                if ( inputStream == null )
                {
                    return Optional.empty( );
                }
                byte [ ] content = inputStream.readAllBytes( );
                String fileName = ( fileMeta != null && fileMeta.getTitle( ) != null ) ? fileMeta.getTitle( ) : document.getName( );
                String contentType = ( fileMeta != null && fileMeta.getMimeType( ) != null ) ? fileMeta.getMimeType( ) : DOWNLOAD_DEFAULT_CONTENT_TYPE;
                return Optional.of( new DocumentDownload( content, fileName, contentType ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", DOWNLOAD_ERROR_MESSAGE, documentId, e );
            return Optional.empty( );
        }
    }

    /**
     * Updates a folder's name and description after verifying it belongs to the dataset and that no sibling already uses the new name.
     *
     * @param datasetId
     *            The dataset identifier
     * @param folderId
     *            The folder identifier
     * @param name
     *            The new folder name (required)
     * @param description
     *            The new folder description (may be null)
     * @return The updated folder DTO
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws InvalidRequestException
     *             if the name is blank, the folder is unknown, or the name duplicates a sibling
     */
    public static DatasetFolderDTO updateFolder( int datasetId, int folderId, String name, String description )
    {
        requireDataset( datasetId );
        String folderName = name == null ? null : name.trim( );
        if ( folderName == null || folderName.isEmpty( ) )
        {
            throw new InvalidRequestException( FOLDER_NAME_REQUIRED_MESSAGE );
        }
        DatasetFolder folder = DatasetFolderHome.findByPrimaryKey( folderId ).orElse( null );
        if ( folder == null || folder.getDatasetId( ) != datasetId )
        {
            throw new InvalidRequestException( FOLDER_NOT_IN_DATASET_MESSAGE );
        }
        if ( !folderName.equals( folder.getName( ) )
                && DatasetFolderHome.findByParentAndName( datasetId, folder.getParentFolderId( ), folderName ).isPresent( ) )
        {
            throw new InvalidRequestException( FOLDER_NAME_DUPLICATE_MESSAGE );
        }
        folder.setName( folderName );
        folder.setDescription( description );
        DatasetFolderHome.update( folder );
        return toDTO( folder );
    }

    /**
     * Removes a folder, re-parenting its direct child subfolders and documents to the removed folder's parent (no cascade delete).
     *
     * @param datasetId
     *            The dataset identifier
     * @param folderId
     *            The folder identifier
     * @return The parent folder id the children were re-parented to, or null when the removed folder was a root
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     * @throws InvalidRequestException
     *             if the folder is unknown or does not belong to the dataset
     */
    public static Integer removeFolderReparenting( int datasetId, int folderId )
    {
        requireDataset( datasetId );
        DatasetFolder folder = DatasetFolderHome.findByPrimaryKey( folderId ).orElse( null );
        if ( folder == null || folder.getDatasetId( ) != datasetId )
        {
            throw new InvalidRequestException( FOLDER_NOT_IN_DATASET_MESSAGE );
        }
        Integer parentId = folder.getParentFolderId( );
        DatasetFolderHome.reparentFolders( datasetId, folderId, parentId );
        DatasetDocumentHome.reparentDocuments( datasetId, folderId, parentId );
        DatasetFolderHome.remove( folderId );
        return parentId;
    }

    /**
     * Returns the default chunk size applied to uploaded documents.
     *
     * @return The default chunk size
     */
    public static int getDefaultChunkSize( )
    {
        return DEFAULT_CHUNK_SIZE;
    }

    /**
     * Returns the default chunk overlap applied to uploaded documents.
     *
     * @return The default chunk overlap
     */
    public static int getDefaultChunkOverlap( )
    {
        return DEFAULT_CHUNK_OVERLAP;
    }

    /**
     * Builds a breadcrumb trail for a folder: each ancestor from root down to the target folder.
     *
     * @param resolver
     *            The folder path resolver for the dataset
     * @param folderId
     *            The target folder id, or null for an empty (root) breadcrumb
     * @return The breadcrumb entries ordered from root to current folder
     */
    private static List<BreadcrumbEntry> buildBreadcrumb( FolderPathResolver resolver, Integer folderId )
    {
        List<BreadcrumbEntry> trail = new ArrayList<>( );
        if ( folderId == null )
        {
            return trail;
        }
        List<DatasetFolder> chain = new ArrayList<>( );
        DatasetFolder cursor = resolver.findById( folderId );
        while ( cursor != null )
        {
            chain.add( 0, cursor );
            cursor = cursor.getParentFolderId( ) == null ? null : resolver.findById( cursor.getParentFolderId( ) );
        }
        for ( DatasetFolder folder : chain )
        {
            trail.add( new BreadcrumbEntry( folder.getId( ), folder.getName( ) ) );
        }
        return trail;
    }

    /**
     * Normalizes a form-selected target folder: returns it only when it exists and belongs to the dataset, otherwise null (dataset root).
     *
     * @param datasetId
     *            The dataset identifier
     * @param targetFolderId
     *            The candidate folder id, may be null
     * @return The validated folder id, or null
     */
    private static Integer resolveTargetFolder( int datasetId, Integer targetFolderId )
    {
        if ( targetFolderId == null )
        {
            return null;
        }
        DatasetFolder folder = DatasetFolderHome.findByPrimaryKey( targetFolderId ).orElse( null );
        return ( folder != null && folder.getDatasetId( ) == datasetId ) ? targetFolderId : null;
    }

    /**
     * Resolves the destination folder for a document given its browser relative path, creating any missing intermediate folders under the root folder. Path
     * segments are looked up by name under the current parent, reusing existing folders when present.
     *
     * @param datasetId
     *            The dataset identifier
     * @param rootFolderId
     *            The base folder id, or null for the dataset root
     * @param relativePath
     *            The browser relative path (e.g. "a/b/doc.pdf"), may be null
     * @return The leaf folder id where the document should be stored, or rootFolderId when the path has no folder segments
     */
    private static Integer resolveOrCreateFolderPath( int datasetId, Integer rootFolderId, String relativePath )
    {
        if ( isBlank( relativePath ) || !relativePath.contains( "/" ) )
        {
            return rootFolderId;
        }
        String [ ] segments = relativePath.split( "/" );
        Integer currentParentId = rootFolderId;
        for ( int i = 0; i < segments.length - 1; i++ )
        {
            String segment = segments [i];
            if ( isBlank( segment ) )
            {
                continue;
            }
            DatasetFolder existing = DatasetFolderHome.findByParentAndName( datasetId, currentParentId, segment ).orElse( null );
            if ( existing != null )
            {
                currentParentId = existing.getId( );
            }
            else
            {
                DatasetFolder newFolder = new DatasetFolder( );
                newFolder.setDatasetId( datasetId );
                newFolder.setParentFolderId( currentParentId );
                newFolder.setName( segment );
                currentParentId = DatasetFolderHome.create( newFolder ).getId( );
            }
        }
        return currentParentId;
    }

    /**
     * Extracts the basename (last segment) of a relative path, falling back to the raw file name when no path is given.
     *
     * @param fileName
     *            The fallback file name from the multipart item
     * @param relativePath
     *            The browser relative path, may be null
     * @return The basename to store in the document
     */
    private static String extractBasename( String fileName, String relativePath )
    {
        if ( isBlank( relativePath ) )
        {
            return fileName;
        }
        int slash = relativePath.lastIndexOf( '/' );
        return slash < 0 ? relativePath : relativePath.substring( slash + 1 );
    }

    /**
     * Reads file metadata from the file store, logging and swallowing any retrieval error.
     *
     * @param fileKey
     *            The file store key
     * @param documentId
     *            The owning document id (for logging)
     * @return The file metadata, or null when unavailable
     */
    private static File readFileMetaData( String fileKey, int documentId )
    {
        try
        {
            return getFileStoreService( ).getFileMetaData( fileKey );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{} with fileKey={}", DOCUMENT_METADATA_ERROR_MESSAGE, documentId, fileKey, e );
            return null;
        }
    }

    /**
     * Lazily resolves the file store provider via CDI.
     *
     * @return The file store provider
     */
    private static IFileStoreServiceProvider getFileStoreService( )
    {
        if ( _fileStoreService == null )
        {
            _fileStoreService = CDI.current( ).select( IFileStoreServiceProvider.class ).get( );
        }
        return _fileStoreService;
    }

    /**
     * Returns true when the string is null or blank (whitespace only).
     *
     * @param value
     *            The string to test
     * @return true when null or blank
     */
    private static boolean isBlank( String value )
    {
        return value == null || value.trim( ).isEmpty( );
    }

    /**
     * Loads a dataset by primary key or throws.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The dataset entity
     * @throws ResourceNotFoundException
     *             if the dataset does not exist
     */
    private static Dataset requireDataset( int datasetId )
    {
        return DatasetHome.findByPrimaryKey( datasetId ).orElseThrow( ( ) -> new ResourceNotFoundException( DATASET_NOT_FOUND_MESSAGE ) );
    }

    /**
     * Builds a DatasetDocument entity from an upload request.
     *
     * @param datasetId
     *            The dataset identifier
     * @param folderId
     *            The target folder identifier (may be null)
     * @param fileName
     *            The file name
     * @param request
     *            The upload request carrying chunk settings
     * @return The populated entity
     */
    private static DatasetDocument buildDocument( int datasetId, Integer folderId, String fileName, DatasetUploadRequestDTO request )
    {
        DatasetDocument document = new DatasetDocument( );
        document.setDatasetId( datasetId );
        document.setName( fileName );
        document.setDescription( "" );
        document.setChunkSize( request.getChunkSize( ) );
        document.setChunkOverlap( request.getChunkOverlap( ) );
        document.setFolderId( folderId );
        document.setUseDocumentIntelligence( request.isUseDocumentIntelligence( ) && request.getDocumentIntelligenceProviderId( ) != null );
        document.setDocumentIntelligenceProviderId( request.getDocumentIntelligenceProviderId( ) );
        return document;
    }

    /**
     * Decodes the base64 content of an uploaded file.
     *
     * @param fileUpload
     *            The upload payload
     * @return The decoded bytes
     * @throws InvalidRequestException
     *             if the content is not valid base64
     */
    private static byte [ ] decodeBase64( DatasetUploadRequestDTO.FileUploadDTO fileUpload )
    {
        try
        {
            return Base64.getDecoder( ).decode( fileUpload.getContent( ) );
        }
        catch( IllegalArgumentException e )
        {
            throw new InvalidRequestException( INVALID_BASE64_MESSAGE + fileUpload.getFileName( ) );
        }
    }

    /**
     * Resolves the MIME content type of a file from its extension and validates it against the allow-list. Shared by the REST upload flow and by admin XPages
     * that bypass the DTO path.
     *
     * @param fileName
     *            The file name
     * @return The resolved MIME type
     * @throws InvalidRequestException
     *             if the extension is unknown or not in the allow-list
     */
    public static String validateAndResolveContentType( String fileName )
    {
        String contentType = detectContentType( fileName );
        if ( !ALLOWED_CONTENT_TYPES.contains( contentType ) )
        {
            throw new InvalidRequestException( INVALID_FILE_TYPE_MESSAGE + fileName );
        }
        return contentType;
    }

    /**
     * Detects the MIME content type from a file name extension.
     *
     * @param fileName
     *            The file name
     * @return The MIME type, or a safe default when the extension is unknown
     */
    private static String detectContentType( String fileName )
    {
        String lower = fileName.toLowerCase( );
        for ( Map.Entry<String, String> entry : EXTENSION_TO_CONTENT_TYPE.entrySet( ) )
        {
            if ( lower.endsWith( entry.getKey( ) ) )
            {
                return entry.getValue( );
            }
        }
        return DEFAULT_CONTENT_TYPE;
    }

    /**
     * Creates a FileItem from an in-memory byte array.
     *
     * @param fileName
     *            The file name
     * @param contentType
     *            The MIME type
     * @param content
     *            The file content
     * @return The FileItem ready for storage
     * @throws AgentServiceException
     *             if the content cannot be written to the file item
     */
    private static MultipartItem createFileItem( String fileName, String contentType, byte [ ] content )
    {
        return new MemoryFileItem( content, fileName, content.length, contentType );
    }

    /**
     * Deletes a batch of documents, used to unwind a partially successful upload.
     *
     * @param documents
     *            The entities to remove
     */
    private static void rollback( List<DatasetDocument> documents )
    {
        documents.forEach( _documentDelete );
    }

    /**
     * Converts a Dataset entity to its DTO representation.
     *
     * @param dataset
     *            The dataset entity
     * @return The DTO
     */
    private static DatasetDTO toDTO( Dataset dataset )
    {
        DatasetDTO dto = new DatasetDTO( );
        dto.setId( dataset.getId( ) );
        dto.setDatasetName( dataset.getDatasetName( ) );
        dto.setDatasetDescription( dataset.getDatasetDescription( ) );
        dto.setEmbedProviderId( dataset.getEmbedProviderId( ) );
        return dto;
    }

    /**
     * Converts a DatasetDocument entity to its DTO representation.
     *
     * @param doc
     *            The document entity
     * @return The DTO
     */
    private static DatasetDocumentDTO toDTO( DatasetDocument doc )
    {
        DatasetDocumentDTO dto = new DatasetDocumentDTO( );
        dto.setId( doc.getId( ) );
        dto.setDatasetId( doc.getDatasetId( ) );
        dto.setName( doc.getName( ) );
        dto.setDescription( doc.getDescription( ) );
        dto.setChunkSize( doc.getChunkSize( ) );
        dto.setChunkOverlap( doc.getChunkOverlap( ) );
        dto.setUseDocumentIntelligence( doc.getUseDocumentIntelligence( ) );
        dto.setDocumentIntelligenceProviderId( doc.getDocumentIntelligenceProviderId( ) );
        dto.setFolderId( doc.getFolderId( ) );
        return dto;
    }

    /**
     * Converts a DatasetFolder entity to its DTO representation.
     *
     * @param folder
     *            The folder entity
     * @return The DTO
     */
    private static DatasetFolderDTO toDTO( DatasetFolder folder )
    {
        DatasetFolderDTO dto = new DatasetFolderDTO( );
        dto.setId( folder.getId( ) );
        dto.setDatasetId( folder.getDatasetId( ) );
        dto.setParentFolderId( folder.getParentFolderId( ) );
        dto.setName( folder.getName( ) );
        dto.setDescription( folder.getDescription( ) );
        return dto;
    }
}
