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

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.mcp.BotMcpServer;
import fr.paris.lutece.plugins.platform.business.mcp.BotMcpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedbackHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Verifies that the ON DELETE CASCADE foreign keys declared on the children of the {@code platform_bot} root table are actually enforced on HSQL. One child row
 * is created in each child table through its Home, the bot is removed once via {@link BotHome#remove(int)}, and every child is then asserted to have
 * disappeared.
 */
public class BotCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full child graph of a single Bot (pipeline association, conversation, MCP server association, dataset association, message feedback, user rate
     * limit), asserts every child exists, removes the Bot once, then asserts the cascade deleted each child. The Pipeline, Dataset, McpServer and the
     * Client/Provider FK parents that are not children of the Bot are cleaned up explicitly at the end.
     */
    @Test
    public void testBotDeleteCascadesToChildren( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );

        int nPipelineId = createPipeline( client.getId( ) );
        Dataset dataset = createDataset( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );
        McpServer mcpServer = createMcpServer( );

        BotPipeline botPipeline = new BotPipeline( );
        botPipeline.setBotId( bot.getId( ) );
        botPipeline.setPipelineId( nPipelineId );
        botPipeline.setToolDescription( "Cascade tool" );
        BotPipelineHome.create( botPipeline );

        BotConversation conversation = new BotConversation( );
        conversation.setBotId( bot.getId( ) );
        conversation.setConversationUuid( UUID.randomUUID( ).toString( ) );
        conversation.setUserId( "cascade-user" );
        BotConversationHome.create( conversation );

        ConversationMessage message = new ConversationMessage( );
        message.setConversationId( conversation.getId( ) );
        message.setMessage( "Cascade message" );
        message.setRole( "assistant" );
        ConversationMessageHome.create( message );

        BotMcpServerHome.associate( new BotMcpServer( bot.getId( ), mcpServer.getId( ) ) );

        BotDatasetHome.associate( new BotDataset( bot.getId( ), dataset.getId( ) ) );

        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        feedback.setMessageId( message.getId( ) );
        feedback.setUserId( "cascade-user" );
        feedback.setBotId( bot.getId( ) );
        feedback.setClientId( client.getId( ) );
        feedback.setIsPositive( true );
        feedback.setComment( "Cascade feedback" );
        feedback.setStatus( ConversationMessageFeedback.Status.PENDING );
        ConversationMessageFeedbackHome.create( feedback );

        BotUserRateLimit rateLimit = new BotUserRateLimit( );
        rateLimit.setUserId( "cascade-user" );
        rateLimit.setBotId( bot.getId( ) );
        rateLimit.setMessageCount( 1 );
        rateLimit.setDateFirstMessage( new Timestamp( System.currentTimeMillis( ) ) );
        BotUserRateLimitHome.create( rateLimit );

        assertFalse( BotPipelineHome.getBotPipelinesByBotId( bot.getId( ) ).isEmpty( ) );
        assertTrue( BotConversationHome.findByPrimaryKey( conversation.getId( ) ).isPresent( ) );
        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).contains( mcpServer.getId( ) ) );
        assertTrue( BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) ).contains( dataset.getId( ) ) );
        assertTrue( ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) ).isPresent( ) );
        assertTrue( BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) ).isPresent( ) );

        BotHome.remove( bot.getId( ) );

        assertTrue( BotPipelineHome.getBotPipelinesByBotId( bot.getId( ) ).isEmpty( ) );
        assertTrue( BotConversationHome.findByPrimaryKey( conversation.getId( ) ).isEmpty( ) );
        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).isEmpty( ) );
        assertTrue( BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) ).isEmpty( ) );
        assertTrue( ConversationMessageFeedbackHome.findByPrimaryKey( feedback.getId( ) ).isEmpty( ) );
        assertTrue( BotUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) ).isEmpty( ) );
    }

    /**
     * Creates a persisted Client to be used as a foreign key parent of the Bot, Pipeline and Dataset.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client Cascade Test" );
        client.setCode( "CLIENT_BOT_CASCADE_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a persisted Provider to be used as a foreign key parent of the Bot and the Dataset.
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
     * Creates the root Bot whose deletion is exercised by the cascade test.
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
        bot.setBotName( "Bot Cascade Test" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nLlmProviderId );
        bot.setEmbedProviderId( nEmbedProviderId );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }

    /**
     * Creates a persisted Pipeline to be referenced by a bot/pipeline association.
     *
     * @param nClientId
     *            The parent client identifier
     * @return The generated pipeline identifier
     */
    private int createPipeline( int nClientId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline Cascade Test" );
        pipeline.setIdClient( nClientId );
        return PipelineHome.create( pipeline );
    }

    /**
     * Creates a persisted Dataset to be referenced by a bot/dataset association.
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
        dataset.setDatasetName( "Dataset Cascade Test" );
        dataset.setClientId( nClientId );
        dataset.setLlmProviderId( nLlmProviderId );
        dataset.setEmbedProviderId( nEmbedProviderId );
        return DatasetHome.create( dataset );
    }

    /**
     * Creates a persisted MCP server to be referenced by a bot/MCP server association.
     *
     * @return The created McpServer with its generated primary key
     */
    private McpServer createMcpServer( )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( "MCP Cascade Test" );
        mcpServer.setTransportType( "streamable_http" );
        mcpServer.setUrl( "http://localhost:4010/mcp" );
        mcpServer.setEnabled( true );
        return McpServerHome.create( mcpServer );
    }
}
