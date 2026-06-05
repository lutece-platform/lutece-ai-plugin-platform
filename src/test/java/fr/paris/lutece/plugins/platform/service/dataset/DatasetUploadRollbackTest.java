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

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetUploadRequestDTO;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.rag.DocumentService;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Proves the rollback contract of {@link DatasetService#uploadDocuments}: when a batch fails mid-way, only the documents CREATED by that batch are deleted. A
 * document that already existed before the call (and was merely replaced by the batch) must survive the rollback — deleting it is data loss. The I/O-bound
 * storage step (file store + Elasticsearch) is substituted with an HSQL-only stub that reproduces the create-or-replace semantics of
 * {@link DocumentService#storeDocument}.
 */
public class DatasetUploadRollbackTest extends AbstractPlatformDbTest
{
    /**
     * Substitutes the storage and deletion steps with HSQL-only equivalents of the create-or-replace contract of DocumentService.
     */
    @BeforeEach
    public void stubDocumentStore( )
    {
        DatasetService._documentStore = ( document, fileItem ) -> {
            Optional<DatasetDocument> existing = DatasetDocumentHome.findByNameFolderAndDatasetId( document.getName( ), document.getFolderId( ),
                    document.getDatasetId( ) );
            if ( existing.isPresent( ) )
            {
                return DatasetDocumentHome.update( existing.get( ) );
            }
            document.setFileKey( "stub-file-key" );
            return DatasetDocumentHome.create( document );
        };
        DatasetService._documentDelete = document -> DatasetDocumentHome.remove( document.getId( ) );
    }

    /**
     * Restores the real storage and deletion steps.
     */
    @AfterEach
    public void restoreDocumentStore( )
    {
        DatasetService._documentStore = DocumentService::storeDocument;
        DatasetService._documentDelete = DocumentService::deleteDocument;
    }

    /**
     * A failing batch whose first file REPLACED a pre-existing document must not delete that document on rollback.
     */
    @Test
    public void testRollbackDoesNotDeletePreExistingDocument( )
    {
        Dataset dataset = createDataset( );
        DatasetDocument preExisting = createDocument( dataset, "existing-report.pdf" );

        DatasetUploadRequestDTO request = new DatasetUploadRequestDTO( );
        request.setFiles( List.of( file( "existing-report.pdf" ), file( "not-allowed.xyz" ) ) );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.uploadDocuments( dataset.getId( ), request ),
                "the second file has a disallowed extension, the batch must fail" );

        assertTrue( DatasetDocumentHome.findByPrimaryKey( preExisting.getId( ) ).isPresent( ),
                "rollback deleted a document that existed before the upload (data loss)" );
    }

    /**
     * The legitimate part of the rollback contract still holds: documents genuinely created by the failing batch are deleted.
     */
    @Test
    public void testRollbackDeletesDocumentsCreatedByTheBatch( )
    {
        Dataset dataset = createDataset( );

        DatasetUploadRequestDTO request = new DatasetUploadRequestDTO( );
        request.setFiles( List.of( file( "brand-new.pdf" ), file( "not-allowed.xyz" ) ) );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.uploadDocuments( dataset.getId( ), request ),
                "the second file has a disallowed extension, the batch must fail" );

        assertTrue( DatasetDocumentHome.findByNameFolderAndDatasetId( "brand-new.pdf", null, dataset.getId( ) ).isEmpty( ),
                "a document created by the failing batch must be rolled back" );
    }

    /**
     * Builds a base64 upload payload for the given file name.
     *
     * @param strFileName
     *            the file name
     * @return the upload payload
     */
    private DatasetUploadRequestDTO.FileUploadDTO file( String strFileName )
    {
        DatasetUploadRequestDTO.FileUploadDTO dto = new DatasetUploadRequestDTO.FileUploadDTO( );
        dto.setFileName( strFileName );
        dto.setContent( Base64.getEncoder( ).encodeToString( "content".getBytes( ) ) );
        return dto;
    }

    /**
     * Creates and persists the minimal Client / Provider / Dataset graph.
     *
     * @return the persisted dataset
     */
    private Dataset createDataset( )
    {
        Client client = new Client( );
        client.setName( "Client rollback" );
        client.setCode( "CLIENT_ROLLBACK" );
        client.setActive( true );
        ClientHome.create( client );

        Provider provider = new Provider( );
        provider.setProviderName( "Rollback Provider" );
        provider.setProviderType( "llm" );
        provider.setDeploymentModelName( "model" );
        provider.setDeploymentApiKey( "key" );
        ProviderHome.create( provider );

        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Rollback Dataset" );
        dataset.setDatasetDescription( "Rollback test dataset" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Creates and persists a document at the root of the given dataset.
     *
     * @param dataset
     *            the owning dataset
     * @param strName
     *            the document name
     * @return the persisted document
     */
    private DatasetDocument createDocument( Dataset dataset, String strName )
    {
        DatasetDocument document = new DatasetDocument( );
        document.setName( strName );
        document.setDescription( "pre-existing" );
        document.setFileKey( "file-key-rollback" );
        document.setDatasetId( dataset.getId( ) );
        document.setFolderId( null );
        document.setChunkSize( 1000 );
        document.setChunkOverlap( 200 );
        return DatasetDocumentHome.create( document );
    }
}
