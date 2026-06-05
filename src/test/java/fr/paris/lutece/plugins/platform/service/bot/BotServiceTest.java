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
package fr.paris.lutece.plugins.platform.service.bot;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotDatasetHome;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.BotPipelineHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.mcp.BotMcpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.bot.dto.BotDetailDTO;
import fr.paris.lutece.plugins.platform.service.bot.dto.DatasetSelectionDTO;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link BotService} exercised against HSQL. They focus on the association orchestration methods (create/update with datasets, pipelines
 * and MCP servers) and the aggregation/partition read methods (bot detail, dataset selection) — verifying the observable persisted state and the assembled
 * value objects rather than getters. Every test seeds the minimal graph it needs through the {@code *Home} facades and cleans it up afterwards.
 */
public class BotServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed bot service.
     *
     * @return the bot service instance
     */
    private BotService service( )
    {
        return CDI.current( ).select( BotService.class ).get( );
    }

    /**
     * Creates and persists an active client with the given code.
     *
     * @param code
     *            the unique client code
     * @return the persisted client carrying its generated identifier
     */
    private Client createClient( String code )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( code );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Builds an in-memory bot (not persisted) owned by the given client.
     *
     * @param name
     *            the bot name
     * @param clientId
     *            the owning client id
     * @param providerId
     *            the seeded provider id used for both the LLM and embedding provider foreign keys
     * @return the unsaved bot
     */
    private Bot newBot( String name, int clientId, int providerId )
    {
        Bot bot = new Bot( );
        bot.setBotName( name );
        bot.setBotDescription( "desc " + name );
        bot.setBotSystemPrompt( "prompt" );
        bot.setClientId( clientId );
        bot.setLlmProviderId( providerId );
        bot.setEmbedProviderId( providerId );
        bot.setMaxTokens( 1000 );
        bot.setTemperature( 0.5 );
        return bot;
    }

    /**
     * Creates and persists a provider used as the embed and LLM provider of a dataset.
     *
     * @param name
     *            the provider name
     * @return the persisted provider carrying its generated identifier
     */
    private Provider createProvider( String name )
    {
        Provider provider = new Provider( );
        provider.setProviderName( name );
        provider.setProviderType( "llm" );
        provider.setDeploymentModelName( "model-" + name );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates and persists a dataset owned by the given client, wired to the given provider for both its embedding and LLM provider references.
     *
     * @param name
     *            the dataset name
     * @param clientId
     *            the owning client id
     * @param providerId
     *            the embed and LLM provider id
     * @return the persisted dataset carrying its generated identifier
     */
    private Dataset createDataset( String name, int clientId, int providerId )
    {
        Dataset dataset = new Dataset( );
        dataset.setDatasetName( name );
        dataset.setDatasetDescription( "desc " + name );
        dataset.setClientId( clientId );
        dataset.setEmbedProviderId( providerId );
        dataset.setLlmProviderId( providerId );
        return DatasetHome.create( dataset );
    }

    /**
     * Creates and persists a pipeline owned by the given client.
     *
     * @param name
     *            the pipeline name
     * @param clientId
     *            the owning client id
     * @return the persisted pipeline carrying its generated identifier
     */
    private Pipeline createPipeline( String name, int clientId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( name );
        pipeline.setDescription( "desc " + name );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( clientId );
        PipelineHome.create( pipeline );
        return pipeline;
    }

    /**
     * Creates and persists an MCP server.
     *
     * @param name
     *            the MCP server name
     * @return the persisted MCP server carrying its generated identifier
     */
    private McpServer createMcpServer( String name )
    {
        McpServer server = new McpServer( );
        server.setName( name );
        server.setTransportType( "streamable_http" );
        server.setUrl( "http://localhost:4010/mcp" );
        server.setEnabled( true );
        return McpServerHome.create( server );
    }

    /**
     * Creating a bot with associations persists the bot (assigning an id) and wires the supplied dataset and MCP server ids; pipelines are not touched by the
     * create overload. The persisted association sets are read back through the Home facades.
     */
    @Test
    public void testCreateBotWithAssociations( )
    {
        Client client = createClient( "bot-svc-create" );
        Provider provider = createProvider( "prov-create" );
        Dataset ds1 = createDataset( "ds-create-1", client.getId( ), provider.getId( ) );
        Dataset ds2 = createDataset( "ds-create-2", client.getId( ), provider.getId( ) );
        McpServer mcp = createMcpServer( "mcp-create" );
        Bot bot = newBot( "bot-create", client.getId( ), provider.getId( ) );

        Bot created = service( ).createBotWithAssociations( bot, List.of( ds1.getId( ), ds2.getId( ) ), List.of( mcp.getId( ) ) );

        assertTrue( created.getId( ) > 0, "bot must be persisted with a generated id" );
        assertTrue( BotHome.findByPrimaryKey( created.getId( ) ).isPresent( ), "bot must be retrievable" );

        List<Integer> datasetIds = BotDatasetHome.getDatasetIdsByBotId( created.getId( ) );
        assertEquals( 2, datasetIds.size( ), "both datasets must be associated" );
        assertTrue( datasetIds.contains( ds1.getId( ) ) && datasetIds.contains( ds2.getId( ) ), "association must reference the seeded datasets" );

        List<Integer> mcpIds = BotMcpServerHome.getMcpServerIdsByBotId( created.getId( ) );
        assertEquals( List.of( mcp.getId( ) ), mcpIds, "the MCP server must be associated" );
    }

    /**
     * Updating a bot with associations fully replaces the dataset, pipeline and MCP server sets: the prior associations are removed and only the newly supplied
     * ids remain. The final association sets are verified through the Home facades.
     */
    @Test
    public void testUpdateBotWithAssociationsReplacesSets( )
    {
        Client client = createClient( "bot-svc-update" );
        Provider provider = createProvider( "prov-update" );
        Dataset dsOld = createDataset( "ds-old", client.getId( ), provider.getId( ) );
        Dataset dsNew = createDataset( "ds-new", client.getId( ), provider.getId( ) );
        Pipeline plOld = createPipeline( "pl-old", client.getId( ) );
        Pipeline plNew = createPipeline( "pl-new", client.getId( ) );
        McpServer mcpOld = createMcpServer( "mcp-old" );
        McpServer mcpNew = createMcpServer( "mcp-new" );
        Bot bot = newBot( "bot-update", client.getId( ), provider.getId( ) );

        service( ).createBotWithAssociations( bot, List.of( dsOld.getId( ) ), List.of( mcpOld.getId( ) ) );
        BotPipelineHome.associatePipelines( bot.getId( ), List.of( plOld.getId( ) ) );

        service( ).updateBotWithAssociations( bot, List.of( dsNew.getId( ) ), List.of( plNew.getId( ) ), List.of( mcpNew.getId( ) ) );

        assertEquals( List.of( dsNew.getId( ) ), BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) ), "dataset set must be replaced by the new id" );
        assertEquals( List.of( plNew.getId( ) ), BotPipelineHome.getPipelineIdsByBotId( bot.getId( ) ), "pipeline set must be replaced by the new id" );
        assertEquals( List.of( mcpNew.getId( ) ), BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ), "MCP set must be replaced by the new id" );
    }

    /**
     * The bot detail aggregates the associated datasets, attached pipelines and MCP servers of a bot. With a fresh bot owning no executions and no feedbacks,
     * the statistics are empty and the pending feedback count is zero, while the three association lists reflect exactly what was wired.
     */
    @Test
    public void testGetBotDetailAggregatesAssociations( )
    {
        Client client = createClient( "bot-svc-detail" );
        Provider provider = createProvider( "prov-detail" );
        Dataset ds = createDataset( "ds-detail", client.getId( ), provider.getId( ) );
        Pipeline pl = createPipeline( "pl-detail", client.getId( ) );
        McpServer mcp = createMcpServer( "mcp-detail" );
        Bot bot = newBot( "bot-detail", client.getId( ), provider.getId( ) );

        service( ).createBotWithAssociations( bot, List.of( ds.getId( ) ), List.of( mcp.getId( ) ) );
        BotPipelineHome.associatePipelines( bot.getId( ), List.of( pl.getId( ) ) );

        BotDetailDTO detail = service( ).getBotDetail( bot.getId( ), client.getId( ) );

        assertEquals( 1, detail.associatedDatasets( ).size( ), "one dataset must be aggregated" );
        assertEquals( ds.getId( ), detail.associatedDatasets( ).get( 0 ).getId( ), "the aggregated dataset must be the seeded one" );
        assertEquals( 1, detail.attachedPipelines( ).size( ), "one pipeline must be aggregated" );
        assertEquals( pl.getId( ), detail.attachedPipelines( ).get( 0 ).getId( ), "the aggregated pipeline must be the seeded one" );
        assertEquals( 1, detail.attachedMcpServers( ).size( ), "one MCP server must be aggregated" );
        assertEquals( mcp.getId( ), detail.attachedMcpServers( ).get( 0 ).getId( ), "the aggregated MCP server must be the seeded one" );
        assertTrue( detail.stats( ).isEmpty( ), "a bot with no execution has no statistics" );
        assertEquals( 0, detail.pendingFeedbackCount( ), "a fresh bot has no pending feedback" );
    }

    /**
     * The dataset selection for a bot partitions every dataset into local (owned by the given client) and external (owned by another client) buckets, enriches
     * the external datasets with their owning client name, and reports the ids currently selected by the bot.
     */
    @Test
    public void testGetDatasetSelectionForBotPartitionsAndEnriches( )
    {
        Client owner = createClient( "bot-svc-sel-owner" );
        Client other = createClient( "bot-svc-sel-other" );
        Provider provider = createProvider( "prov-selection" );
        Dataset local = createDataset( "ds-local", owner.getId( ), provider.getId( ) );
        Dataset external = createDataset( "ds-external", other.getId( ), provider.getId( ) );
        Bot bot = newBot( "bot-selection", owner.getId( ), provider.getId( ) );
        service( ).createBotWithAssociations( bot, List.of( local.getId( ) ), List.of( ) );

        DatasetSelectionDTO selection = service( ).getDatasetSelectionForBot( bot.getId( ), owner.getId( ) );

        assertTrue( selection.localDatasets( ).stream( ).anyMatch( d -> d.getId( ) == local.getId( ) ), "owner dataset must be local" );
        assertTrue( selection.localDatasets( ).stream( ).noneMatch( d -> d.getId( ) == external.getId( ) ), "other client's dataset must not be local" );

        Dataset externalSeen = selection.externalDatasets( ).stream( ).filter( d -> d.getId( ) == external.getId( ) ).findFirst( ).orElse( null );
        assertNotNull( externalSeen, "other client's dataset must be external" );
        assertEquals( other.getName( ), externalSeen.getClientName( ), "external dataset must be enriched with its owning client name" );

        assertEquals( List.of( local.getId( ) ), selection.selectedDatasetIds( ), "selected ids must reflect the bot's current association" );
    }
}
