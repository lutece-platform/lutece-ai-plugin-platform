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
package fr.paris.lutece.plugins.platform.business.bot;

import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.spi.CDI;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.cache.DatasetCacheService;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for BotDatasetHome on HSQL. BotDataset is a bot/dataset association, so the cycle exercised is associate / read / removeByBotId.
 */
public class BotDatasetHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the associate / getDatasetIdsByBotId / removeByBotId cycle of BotDatasetHome. The parent Bot and Dataset (and their own Client and Provider
     * parents) are created first to satisfy the foreign keys, then removed at the end.
     */
    @Test
    public void testBusinessBotDataset( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );
        Dataset dataset = createDataset( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );

        BotDataset botDataset = new BotDataset( bot.getId( ), dataset.getId( ) );

        BotDatasetHome.associate( botDataset );
        List<Integer> datasetIds = BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) );
        assertTrue( datasetIds.contains( dataset.getId( ) ) );

        List<Integer> botIds = BotDatasetHome.getBotIdsByDatasetId( dataset.getId( ) );
        assertTrue( botIds.contains( bot.getId( ) ) );

        BotDatasetHome.removeByBotId( bot.getId( ) );
        datasetIds = BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) );
        assertTrue( datasetIds.isEmpty( ) );
    }

    /**
     * Proves that the list returned by getDatasetIdsByBotId is isolated from caller mutation when the dataset cache is enabled : mutating the returned list
     * must not corrupt the cached entry served to subsequent callers.
     */
    @Test
    public void testGetDatasetIdsByBotIdIsolatedFromCallerMutation( )
    {
        DatasetCacheService cache = CDI.current( ).select( DatasetCacheService.class ).get( );
        boolean bPreviousStatus = cache.isCacheEnable( );
        cache.enableCache( true );
        try
        {
            Client client = createClient( );
            Provider llmProvider = createProvider( "LLM" );
            Provider embedProvider = createProvider( "EMBEDDING" );
            Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );
            Dataset dataset = createDataset( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );
            BotDatasetHome.associate( new BotDataset( bot.getId( ), dataset.getId( ) ) );

            List<Integer> firstRead = BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) );
            assertEquals( 1, firstRead.size( ) );
            firstRead.clear( );

            List<Integer> secondRead = BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) );
            assertEquals( 1, secondRead.size( ), "caller mutation must not corrupt the cached dataset ids" );
        }
        finally
        {
            cache.enableCache( bPreviousStatus );
        }
    }

    /**
     * Creates a persisted Client to be used as the foreign key parent of a Bot and a Dataset.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client BotDataset Test" );
        client.setCode( "CLIENT_BOTDATASET_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a persisted Provider to be used as a foreign key parent of a Bot and a Dataset.
     *
     * @param strType
     *            The provider type
     * @return The created Provider with its generated primary key
     */
    private Provider createProvider( String strType )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Provider " + strType );
        provider.setProviderType( strType );
        provider.setDeploymentModelName( "model-" + strType );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates a persisted Bot to be used as a foreign key parent of a bot dataset association.
     *
     * @param nClientId
     *            The parent client identifier
     * @param nLlmProviderId
     *            The LLM provider identifier
     * @param nEmbedProviderId
     *            The embedding provider identifier
     * @return The created Bot with its generated primary key
     */
    private Bot createBot( int nClientId, int nLlmProviderId, int nEmbedProviderId )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot Dataset Test" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nLlmProviderId );
        bot.setEmbedProviderId( nEmbedProviderId );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }

    /**
     * Creates a persisted Dataset to be used as a foreign key parent of a bot dataset association.
     *
     * @param nClientId
     *            The parent client identifier
     * @param nLlmProviderId
     *            The LLM provider identifier
     * @param nEmbedProviderId
     *            The embedding provider identifier
     * @return The created Dataset with its generated primary key
     */
    private Dataset createDataset( int nClientId, int nLlmProviderId, int nEmbedProviderId )
    {
        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Dataset BotDataset Test" );
        dataset.setClientId( nClientId );
        dataset.setLlmProviderId( nLlmProviderId );
        dataset.setEmbedProviderId( nEmbedProviderId );
        return DatasetHome.create( dataset );
    }
}
