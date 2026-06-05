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
 * Test the CRUD cycle of the DatasetHome facade on HSQL.
 */
public class DatasetHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a Dataset: create, findByPrimaryKey, update, remove. Creates the parent Client and Provider entities first to satisfy
     * the foreign keys and removes them at the end.
     */
    @Test
    public void testBusinessDataset( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );

        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Dataset Name" );
        dataset.setDatasetDescription( "DatasetDescription 1" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        dataset.setDatasetRoutingRules( "RoutingRules 1" );

        DatasetHome.create( dataset );

        Optional<Dataset> optStored = DatasetHome.findByPrimaryKey( dataset.getId( ) );
        assertNotNull( optStored );
        assertTrue( optStored.isPresent( ) );
        Dataset datasetStored = optStored.get( );
        assertEquals( datasetStored.getDatasetName( ), dataset.getDatasetName( ) );
        assertEquals( datasetStored.getDatasetDescription( ), dataset.getDatasetDescription( ) );
        assertEquals( datasetStored.getClientId( ), dataset.getClientId( ) );
        assertEquals( datasetStored.getEmbedProviderId( ), dataset.getEmbedProviderId( ) );

        dataset.setDatasetDescription( "DatasetDescription 2" );
        DatasetHome.update( dataset );
        datasetStored = DatasetHome.findByPrimaryKey( dataset.getId( ) ).get( );
        assertEquals( datasetStored.getDatasetDescription( ), dataset.getDatasetDescription( ) );

        DatasetHome.remove( dataset.getId( ) );
        optStored = DatasetHome.findByPrimaryKey( dataset.getId( ) );
        assertFalse( optStored.isPresent( ) );

        DatasetHome.getDatasetsList( );
    }

    /**
     * Creates and persists a Client used as the parent of the dataset.
     *
     * @return The persisted Client with its generated ID
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Dataset Client" );
        client.setCode( "dataset-client-code" );
        client.setDescription( "Client for dataset test" );
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
        provider.setProviderName( "Dataset Provider" );
        provider.setProviderDescription( "Provider for dataset test" );
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
}
