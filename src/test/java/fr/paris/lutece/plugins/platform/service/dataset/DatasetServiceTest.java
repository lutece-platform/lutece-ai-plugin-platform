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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderDTO;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for the static {@link DatasetService} facade against HSQL. These exercise the folder business rules — name-required validation, parent
 * ownership, idempotent duplicate-name handling, rename collision detection, and the re-parenting remove that walks children up to the removed folder's parent
 * — the recursive folder-path resolution used by directory uploads, and the extension-based content-type allow-list. Heavy methods that need a file store or
 * Elasticsearch (ingest, view aggregation, segments, download, reindex) are intentionally left out. Each test seeds the minimal Client / Provider / Dataset
 * graph needed to satisfy the foreign keys and tears it down afterwards.
 */
public class DatasetServiceTest extends AbstractPlatformDbTest
{
    /**
     * Creates and persists a parent Client.
     *
     * @param code
     *            the unique client code
     * @return the persisted client
     */
    private Client createClient( String code )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( code );
        client.setActive( true );
        ClientHome.create( client );
        return client;
    }

    /**
     * Creates and persists a Provider used as the embed and LLM provider of a dataset.
     *
     * @return the persisted provider
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "DS Provider" );
        provider.setProviderDescription( "Provider for dataset service test" );
        provider.setProviderType( "llm" );
        provider.setProviderVendor( "azure_openai" );
        provider.setDeploymentName( "DeploymentName" );
        provider.setDeploymentModelName( "DeploymentModelName" );
        provider.setDeploymentEndpoint( "http://localhost/v1" );
        provider.setDeploymentApiVersion( "2024-01-01" );
        provider.setDeploymentApiKey( "ApiKey" );
        ProviderHome.create( provider );
        return provider;
    }

    /**
     * Creates and persists a Dataset attached to the given client and provider.
     *
     * @param client
     *            the parent client
     * @param provider
     *            the embed and LLM provider
     * @return the persisted dataset
     */
    private Dataset createDataset( Client client, Provider provider )
    {
        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "DS Service Dataset" );
        dataset.setDatasetDescription( "Dataset for service test" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Builds a folder DTO payload with the given name and parent.
     *
     * @param name
     *            the folder name
     * @param parentFolderId
     *            the parent folder id, or null for a root folder
     * @return the payload DTO
     */
    private DatasetFolderDTO payload( String name, Integer parentFolderId )
    {
        DatasetFolderDTO dto = new DatasetFolderDTO( );
        dto.setName( name );
        dto.setParentFolderId( parentFolderId );
        return dto;
    }

    /**
     * createFolder rejects a null payload and a blank name with InvalidRequestException, before any persistence happens.
     */
    @Test
    public void testCreateFolderRequiresName( )
    {
        Client client = createClient( "ds-svc-name" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.createFolder( dataset.getId( ), null ), "A null payload must be rejected" );
        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.createFolder( dataset.getId( ), payload( "   ", null ) ),
                "A blank name must be rejected" );
    }

    /**
     * createFolder on an unknown dataset raises ResourceNotFoundException.
     */
    @Test
    public void testCreateFolderUnknownDataset( )
    {
        assertThrows( ResourceNotFoundException.class, ( ) -> DatasetService.createFolder( 9999999, payload( "any", null ) ),
                "An unknown dataset must be rejected" );
    }

    /**
     * createFolder rejects a parent folder that belongs to a different dataset.
     */
    @Test
    public void testCreateFolderRejectsForeignParent( )
    {
        Client client = createClient( "ds-svc-foreign" );
        Provider provider = createProvider( );
        Dataset datasetA = createDataset( client, provider );
        Dataset datasetB = createDataset( client, provider );

        DatasetFolderDTO parentInB = DatasetService.createFolder( datasetB.getId( ), payload( "parent", null ) );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.createFolder( datasetA.getId( ), payload( "child", parentInB.getId( ) ) ),
                "A parent owned by another dataset must be rejected" );
    }

    /**
     * createFolder is idempotent for a duplicate name under the same parent: the second call returns the existing folder rather than creating a new row.
     */
    @Test
    public void testCreateFolderDuplicateReturnsExisting( )
    {
        Client client = createClient( "ds-svc-dup" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolderDTO first = DatasetService.createFolder( dataset.getId( ), payload( "Contracts", null ) );
        DatasetFolderDTO second = DatasetService.createFolder( dataset.getId( ), payload( "Contracts", null ) );

        assertEquals( first.getId( ), second.getId( ), "A duplicate name under the same parent returns the existing folder" );
        assertEquals( 1, DatasetFolderHome.getFoldersByDatasetId( dataset.getId( ) ).size( ), "No second row is created for a duplicate name" );
    }

    /**
     * createFolder persists a folder with its name, parent and description, and reflects them in the returned DTO.
     */
    @Test
    public void testCreateFolderPersists( )
    {
        Client client = createClient( "ds-svc-create" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolderDTO root = DatasetService.createFolder( dataset.getId( ), payload( "root", null ) );
        DatasetFolderDTO childPayload = payload( "child", root.getId( ) );
        childPayload.setDescription( "a child folder" );
        DatasetFolderDTO child = DatasetService.createFolder( dataset.getId( ), childPayload );

        Optional<DatasetFolder> stored = DatasetFolderHome.findByPrimaryKey( child.getId( ) );
        assertTrue( stored.isPresent( ), "The created folder must be persisted" );
        assertEquals( "child", stored.get( ).getName( ), "The folder name must be persisted" );
        assertEquals( root.getId( ), stored.get( ).getParentFolderId( ), "The parent folder id must be persisted" );
        assertEquals( "a child folder", stored.get( ).getDescription( ), "The description must be persisted" );
    }

    /**
     * updateFolder renames a folder, rejects a blank name, rejects a folder that does not belong to the dataset, and rejects a rename that collides with a
     * sibling.
     */
    @Test
    public void testUpdateFolder( )
    {
        Client client = createClient( "ds-svc-update" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolderDTO a = DatasetService.createFolder( dataset.getId( ), payload( "alpha", null ) );
        DatasetService.createFolder( dataset.getId( ), payload( "beta", null ) );

        DatasetFolderDTO renamed = DatasetService.updateFolder( dataset.getId( ), a.getId( ), "alpha-2", "new desc" );
        assertEquals( "alpha-2", renamed.getName( ), "updateFolder must return the new name" );
        assertEquals( "alpha-2", DatasetFolderHome.findByPrimaryKey( a.getId( ) ).get( ).getName( ), "The rename must be persisted" );

        int folderId = a.getId( );
        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.updateFolder( dataset.getId( ), folderId, "  ", null ),
                "A blank name must be rejected" );
        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.updateFolder( dataset.getId( ), 8888888, "x", null ),
                "An unknown folder must be rejected" );
        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.updateFolder( dataset.getId( ), folderId, "beta", null ),
                "A rename colliding with a sibling must be rejected" );
    }

    /**
     * removeFolderReparenting moves a folder's child subfolders and documents up to the removed folder's parent, then deletes the folder. With a root folder
     * removed, children are re-parented to the dataset root (null) and the returned parent id is null.
     */
    @Test
    public void testRemoveFolderReparenting( )
    {
        Client client = createClient( "ds-svc-reparent" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolderDTO middle = DatasetService.createFolder( dataset.getId( ), payload( "middle", null ) );
        DatasetFolderDTO childFolder = DatasetService.createFolder( dataset.getId( ), payload( "leaf", middle.getId( ) ) );

        DatasetDocument document = new DatasetDocument( );
        document.setDatasetId( dataset.getId( ) );
        document.setName( "doc.txt" );
        document.setFileKey( "ds-svc-reparent-doc-key" );
        document.setFolderId( middle.getId( ) );
        DatasetDocumentHome.create( document );

        Integer newParent = DatasetService.removeFolderReparenting( dataset.getId( ), middle.getId( ) );

        assertNull( newParent, "Removing a root folder re-parents children to the dataset root (null)" );
        assertFalse( DatasetFolderHome.findByPrimaryKey( middle.getId( ) ).isPresent( ), "The removed folder must be gone" );
        assertNull( DatasetFolderHome.findByPrimaryKey( childFolder.getId( ) ).get( ).getParentFolderId( ),
                "The child subfolder must be re-parented to the root" );
        assertNull( DatasetDocumentHome.findByPrimaryKey( document.getId( ) ).get( ).getFolderId( ), "The document must be re-parented to the root" );
    }

    /**
     * removeFolderReparenting rejects a folder id that does not belong to the dataset.
     */
    @Test
    public void testRemoveFolderRejectsForeignFolder( )
    {
        Client client = createClient( "ds-svc-rm-foreign" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.removeFolderReparenting( dataset.getId( ), 7777777 ),
                "An unknown folder must be rejected" );
    }

    /**
     * ingestMultipartDocuments delegates folder-tree recreation to resolveOrCreateFolderPath, which is private. The directory upload entry point itself needs a
     * file store, so the path-walking logic is asserted indirectly via the public folder facade: createFolder reuses an existing folder for a repeated path
     * segment, proving the same lookup-or-create rule the resolver relies on.
     */
    @Test
    public void testNestedFolderCreationReusesSegments( )
    {
        Client client = createClient( "ds-svc-path" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolderDTO a = DatasetService.createFolder( dataset.getId( ), payload( "a", null ) );
        DatasetFolderDTO b = DatasetService.createFolder( dataset.getId( ), payload( "b", a.getId( ) ) );
        DatasetService.createFolder( dataset.getId( ), payload( "c", b.getId( ) ) );

        DatasetFolderDTO bAgain = DatasetService.createFolder( dataset.getId( ), payload( "b", a.getId( ) ) );
        assertEquals( b.getId( ), bAgain.getId( ), "A repeated path segment must reuse the existing folder" );
        assertEquals( 3, DatasetFolderHome.getFoldersByDatasetId( dataset.getId( ) ).size( ), "The a/b/c chain must hold exactly three folders" );
    }

    /**
     * validateAndResolveContentType resolves an allowed extension to its MIME type and rejects an unknown or disallowed extension with InvalidRequestException.
     */
    @Test
    public void testValidateAndResolveContentType( )
    {
        assertEquals( "application/pdf", DatasetService.validateAndResolveContentType( "report.pdf" ), "A .pdf file resolves to application/pdf" );
        assertEquals( "text/markdown", DatasetService.validateAndResolveContentType( "README.MD" ), "Extension matching is case-insensitive" );
        assertEquals( "text/csv", DatasetService.validateAndResolveContentType( "data.csv" ), "A .csv file resolves to text/csv" );

        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.validateAndResolveContentType( "archive.zip" ),
                "A disallowed extension must be rejected" );
        assertThrows( InvalidRequestException.class, ( ) -> DatasetService.validateAndResolveContentType( "noextension" ),
                "A file without a known extension must be rejected" );
    }

    /**
     * listFolders returns the folders of a dataset and raises ResourceNotFoundException for an unknown dataset.
     */
    @Test
    public void testListFolders( )
    {
        Client client = createClient( "ds-svc-list" );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetService.createFolder( dataset.getId( ), payload( "f1", null ) );
        DatasetService.createFolder( dataset.getId( ), payload( "f2", null ) );

        List<DatasetFolderDTO> folders = DatasetService.listFolders( dataset.getId( ) );
        assertEquals( 2, folders.size( ), "Both folders must be listed" );

        assertThrows( ResourceNotFoundException.class, ( ) -> DatasetService.listFolders( 9999998 ), "An unknown dataset must be rejected" );
    }
}
