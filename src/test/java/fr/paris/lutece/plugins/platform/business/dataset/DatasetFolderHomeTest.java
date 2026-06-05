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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test the CRUD cycle of the DatasetFolderHome facade on HSQL.
 */
public class DatasetFolderHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a DatasetFolder: create, findByPrimaryKey, update, remove. Creates the parent Client, Provider and Dataset first to
     * satisfy the foreign keys and removes them at the end.
     */
    @Test
    public void testBusinessDatasetFolder( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );

        DatasetFolder folder = new DatasetFolder( );
        folder.setDatasetId( dataset.getId( ) );
        folder.setParentFolderId( null );
        folder.setName( "Folder Name" );
        folder.setDescription( "FolderDescription 1" );

        DatasetFolderHome.create( folder );

        Optional<DatasetFolder> optStored = DatasetFolderHome.findByPrimaryKey( folder.getId( ) );
        assertNotNull( optStored );
        assertTrue( optStored.isPresent( ) );
        DatasetFolder folderStored = optStored.get( );
        assertEquals( folderStored.getName( ), folder.getName( ) );
        assertEquals( folderStored.getDescription( ), folder.getDescription( ) );
        assertEquals( folderStored.getDatasetId( ), folder.getDatasetId( ) );

        folder.setDescription( "FolderDescription 2" );
        DatasetFolderHome.update( folder );
        folderStored = DatasetFolderHome.findByPrimaryKey( folder.getId( ) ).get( );
        assertEquals( folderStored.getDescription( ), folder.getDescription( ) );

        DatasetFolderHome.remove( folder.getId( ) );
        optStored = DatasetFolderHome.findByPrimaryKey( folder.getId( ) );
        assertFalse( optStored.isPresent( ) );

        DatasetFolderHome.getFoldersByDatasetId( dataset.getId( ) );
    }

    /**
     * Creates and persists a Client used as the parent of the dataset.
     *
     * @return The persisted Client with its generated ID
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Folder Client" );
        client.setCode( "folder-client-code" );
        client.setDescription( "Client for folder test" );
        client.setActive( true );
        ClientHome.create( client );
        return client;
    }

    /**
     * Creates and persists a Provider used as the embed and LLM provider of the dataset.
     *
     * @return The persisted Provider with its generated ID
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Folder Provider" );
        provider.setProviderDescription( "Provider for folder test" );
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
     * Creates and persists a Dataset used as the parent of the folder.
     *
     * @param client
     *            The parent client
     * @param provider
     *            The provider used for embed and LLM
     * @return The persisted Dataset with its generated ID
     */
    private Dataset createDataset( Client client, Provider provider )
    {
        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Folder Dataset" );
        dataset.setDatasetDescription( "Dataset for folder test" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }
}
