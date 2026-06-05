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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotDataset;
import fr.paris.lutece.plugins.platform.business.bot.BotDatasetHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Proves the ON DELETE CASCADE foreign keys hanging off the Dataset root on HSQL. Builds a Dataset with one child in each cascading table
 * (platform_dataset_document, platform_document_job, platform_dataset_folder, platform_bot_dataset), removes the Dataset once and asserts every child has been
 * swept away.
 */
public class DatasetCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full child graph under a single Dataset, asserts each child exists, removes the Dataset once and asserts the FK ON DELETE CASCADE has removed
     * every child (document, document job, folder, bot/dataset association). The remaining parents (Client, Provider, Bot) are removed at the end.
     */
    @Test
    public void testDatasetDeleteCascadesToChildren( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Bot bot = createBot( client, provider );
        Dataset dataset = createDataset( client, provider );

        DatasetFolder folder = createFolder( dataset );
        DatasetDocument document = createDocument( dataset );
        DatasetDocumentJob job = createJob( dataset, document );
        BotDataset botDataset = new BotDataset( bot.getId( ), dataset.getId( ) );
        BotDatasetHome.associate( botDataset );

        assertTrue( DatasetFolderHome.findByPrimaryKey( folder.getId( ) ).isPresent( ) );
        assertTrue( DatasetDocumentHome.findByPrimaryKey( document.getId( ) ).isPresent( ) );
        assertTrue( DatasetDocumentJobHome.findByPrimaryKey( job.getId( ) ).isPresent( ) );
        assertTrue( BotDatasetHome.getBotIdsByDatasetId( dataset.getId( ) ).contains( bot.getId( ) ) );

        DatasetHome.remove( dataset.getId( ) );

        assertTrue( DatasetFolderHome.findByPrimaryKey( folder.getId( ) ).isEmpty( ) );
        assertTrue( DatasetDocumentHome.findByPrimaryKey( document.getId( ) ).isEmpty( ) );
        assertTrue( DatasetDocumentJobHome.findByPrimaryKey( job.getId( ) ).isEmpty( ) );
        assertTrue( BotDatasetHome.getBotIdsByDatasetId( dataset.getId( ) ).isEmpty( ) );
    }

    /**
     * Creates and persists a Client used as the parent of the bot and dataset.
     *
     * @return The persisted Client with its generated ID
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Cascade Client" );
        client.setCode( "cascade-client-code" );
        client.setDescription( "Client for cascade test" );
        client.setActive( true );
        ClientHome.create( client );
        return client;
    }

    /**
     * Creates and persists a Provider used as the embed and LLM provider of the bot and dataset.
     *
     * @return The persisted Provider with its generated ID
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Cascade Provider" );
        provider.setProviderDescription( "Provider for cascade test" );
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
     * Creates and persists a Bot used as the parent of the bot/dataset association.
     *
     * @param client
     *            The parent client
     * @param provider
     *            The provider used for embed and LLM
     * @return The persisted Bot with its generated ID
     */
    private Bot createBot( Client client, Provider provider )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Cascade Bot" );
        bot.setClientId( client.getId( ) );
        bot.setLlmProviderId( provider.getId( ) );
        bot.setEmbedProviderId( provider.getId( ) );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }

    /**
     * Creates and persists the root Dataset whose deletion is exercised.
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
        dataset.setDatasetName( "Cascade Dataset" );
        dataset.setDatasetDescription( "Dataset for cascade test" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Creates and persists a DatasetFolder child of the dataset.
     *
     * @param dataset
     *            The parent dataset
     * @return The persisted DatasetFolder with its generated ID
     */
    private DatasetFolder createFolder( Dataset dataset )
    {
        DatasetFolder folder = new DatasetFolder( );
        folder.setDatasetId( dataset.getId( ) );
        folder.setParentFolderId( null );
        folder.setName( "Cascade Folder" );
        folder.setDescription( "Folder for cascade test" );
        DatasetFolderHome.create( folder );
        return folder;
    }

    /**
     * Creates and persists a DatasetDocument child of the dataset.
     *
     * @param dataset
     *            The parent dataset
     * @return The persisted DatasetDocument with its generated ID
     */
    private DatasetDocument createDocument( Dataset dataset )
    {
        DatasetDocument document = new DatasetDocument( );
        document.setName( "Cascade Document" );
        document.setDescription( "Document for cascade test" );
        document.setFileKey( "file-key-cascade-test" );
        document.setDatasetId( dataset.getId( ) );
        document.setFolderId( null );
        document.setUseDocumentIntelligence( false );
        document.setDocumentIntelligenceProviderId( null );
        document.setChunkSize( 1000 );
        document.setChunkOverlap( 200 );
        DatasetDocumentHome.create( document );
        return document;
    }

    /**
     * Creates and persists a DatasetDocumentJob child of the dataset and document.
     *
     * @param dataset
     *            The parent dataset
     * @param document
     *            The parent document
     * @return The persisted DatasetDocumentJob with its generated ID
     */
    private DatasetDocumentJob createJob( Dataset dataset, DatasetDocument document )
    {
        DatasetDocumentJob job = new DatasetDocumentJob( );
        job.setDocumentId( document.getId( ) );
        job.setDatasetId( dataset.getId( ) );
        job.setStatus( DatasetDocumentJob.STATUS_PENDING );
        job.setErrorMessage( null );
        DatasetDocumentJobHome.create( job );
        return job;
    }
}
