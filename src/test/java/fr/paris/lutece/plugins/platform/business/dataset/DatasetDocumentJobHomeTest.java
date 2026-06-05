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
 * Test the CRUD cycle of the DatasetDocumentJobHome facade on HSQL.
 */
public class DatasetDocumentJobHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a DatasetDocumentJob: create, findByPrimaryKey, update, remove. Creates the parent Client, Provider, Dataset and
     * DatasetDocument first to satisfy the foreign keys and removes them at the end.
     */
    @Test
    public void testBusinessDatasetDocumentJob( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Dataset dataset = createDataset( client, provider );
        DatasetDocument document = createDocument( dataset );

        DatasetDocumentJob job = new DatasetDocumentJob( );
        job.setDocumentId( document.getId( ) );
        job.setDatasetId( dataset.getId( ) );
        job.setStatus( DatasetDocumentJob.STATUS_PENDING );
        job.setErrorMessage( null );

        DatasetDocumentJobHome.create( job );

        Optional<DatasetDocumentJob> optStored = DatasetDocumentJobHome.findByPrimaryKey( job.getId( ) );
        assertNotNull( optStored );
        assertTrue( optStored.isPresent( ) );
        DatasetDocumentJob jobStored = optStored.get( );
        assertEquals( jobStored.getDocumentId( ), job.getDocumentId( ) );
        assertEquals( jobStored.getDatasetId( ), job.getDatasetId( ) );
        assertEquals( jobStored.getStatus( ), job.getStatus( ) );

        job.setStatus( DatasetDocumentJob.STATUS_COMPLETED );
        job.setErrorMessage( "ErrorMessage 1" );
        DatasetDocumentJobHome.update( job );
        jobStored = DatasetDocumentJobHome.findByPrimaryKey( job.getId( ) ).get( );
        assertEquals( jobStored.getStatus( ), job.getStatus( ) );
        assertEquals( jobStored.getErrorMessage( ), job.getErrorMessage( ) );

        DatasetDocumentJobHome.remove( job.getId( ) );
        optStored = DatasetDocumentJobHome.findByPrimaryKey( job.getId( ) );
        assertFalse( optStored.isPresent( ) );

        DatasetDocumentJobHome.getJobsList( );
    }

    /**
     * Creates and persists a Client used as the parent of the dataset.
     *
     * @return The persisted Client with its generated ID
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Job Client" );
        client.setCode( "job-client-code" );
        client.setDescription( "Client for job test" );
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
        provider.setProviderName( "Job Provider" );
        provider.setProviderDescription( "Provider for job test" );
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
     * Creates and persists a Dataset used as the parent of the document and job.
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
        dataset.setDatasetName( "Job Dataset" );
        dataset.setDatasetDescription( "Dataset for job test" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Creates and persists a DatasetDocument used as the parent of the job.
     *
     * @param dataset
     *            The parent dataset
     * @return The persisted DatasetDocument with its generated ID
     */
    private DatasetDocument createDocument( Dataset dataset )
    {
        DatasetDocument document = new DatasetDocument( );
        document.setName( "Job Document" );
        document.setDescription( "Document for job test" );
        document.setFileKey( "file-key-job-test" );
        document.setDatasetId( dataset.getId( ) );
        document.setFolderId( null );
        document.setUseDocumentIntelligence( false );
        document.setDocumentIntelligenceProviderId( null );
        document.setChunkSize( 1000 );
        document.setChunkOverlap( 200 );
        DatasetDocumentHome.create( document );
        return document;
    }
}
