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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.util.httpaccess.MemoryFileItem;

/**
 * Proves the replace ordering contract of DocumentService.storeDocument : when storing the replacement file fails, the previous file of the document must still
 * exist (no delete-before-store window losing the only copy).
 */
public class DocumentReplaceOrderingTest extends AbstractPlatformDbTest
{
    /**
     * Restores the static seams of DocumentService after each test.
     */
    @AfterEach
    public void restoreSeams( )
    {
        DocumentService._fileService = null;
        DocumentService._embeddingsDelete = ( datasetId, documentId ) -> {
        };
    }

    /**
     * Replaces an existing document with a file whose storage fails : the old file key must NOT have been deleted, and the document row must still reference
     * it.
     */
    @Test
    public void testFailedReplacementKeepsPreviousFile( )
    {
        Client client = new Client( );
        client.setName( "Client DocOrder Test" );
        client.setCode( "CLIENT_DOCORDER_TEST" );
        client.setActive( true );
        client = ClientHome.create( client );

        Provider provider = new Provider( );
        provider.setProviderName( "Provider DocOrder" );
        provider.setProviderType( "EMBEDDING" );
        provider.setDeploymentModelName( "model-embed" );
        provider.setDeploymentApiKey( "test-key" );
        provider = ProviderHome.create( provider );

        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Dataset DocOrder" );
        dataset.setClientId( client.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset = DatasetHome.create( dataset );

        DatasetDocument existing = new DatasetDocument( );
        existing.setName( "doc-order.txt" );
        existing.setDatasetId( dataset.getId( ) );
        existing.setFileKey( "previous-file-key" );
        existing = DatasetDocumentHome.create( existing );

        List<String> deletedKeys = new ArrayList<>( );
        DocumentService._fileService = failingStoreFileService( deletedKeys );
        DocumentService._embeddingsDelete = ( datasetId, documentId ) -> {
        };

        byte [ ] content = "new content".getBytes( );
        MultipartItem fileItem = new MemoryFileItem( content, "doc-order.txt", content.length, "text/plain" );

        DatasetDocument replacement = new DatasetDocument( );
        replacement.setName( "doc-order.txt" );
        replacement.setDatasetId( dataset.getId( ) );

        DatasetDocument result = DocumentService.storeDocument( replacement, fileItem );

        assertNull( result, "storeDocument must report the failure" );
        assertFalse( deletedKeys.contains( "previous-file-key" ), "the previous file must not be deleted when storing its replacement fails" );
        DatasetDocument reloaded = DatasetDocumentHome.findByPrimaryKey( existing.getId( ) ).orElseThrow( );
        assertEquals( "previous-file-key", reloaded.getFileKey( ), "the document row must still reference the previous file" );
    }

    /**
     * Builds a dynamic-proxy file store fake : delete() records the key, storeFileItem() fails, every other method returns null.
     *
     * @param deletedKeys
     *            the list collecting deleted keys
     * @return the fake provider
     */
    private static IFileStoreServiceProvider failingStoreFileService( List<String> deletedKeys )
    {
        return (IFileStoreServiceProvider) Proxy.newProxyInstance( IFileStoreServiceProvider.class.getClassLoader( ), new Class<?> [ ] {
                IFileStoreServiceProvider.class
        }, ( proxy, method, args ) -> switch( method.getName( ) )
        {
            case "storeFileItem" -> throw new FileServiceException( "simulated storage failure", 507, null );
            case "delete" -> {
                deletedKeys.add( (String) args [0] );
                yield null;
            }
            case "getName" -> "failingStoreFake";
            case "isDefault" -> false;
            default -> null;
        } );
    }
}
