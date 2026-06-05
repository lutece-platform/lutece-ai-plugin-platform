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
package fr.paris.lutece.plugins.platform.business.pipeline;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.BotPipeline;
import fr.paris.lutece.plugins.platform.business.bot.BotPipelineHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Verifies that removing a Pipeline (the root entity) cascades to all its children declared with ON DELETE CASCADE foreign keys: platform_bot_pipeline,
 * platform_pipeline_user_rate_limit, platform_pipeline_execution and platform_pipeline_version. Runs on HSQL where foreign keys are enforced.
 */
public class PipelineCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full Pipeline graph (Client, two Providers, Bot, Pipeline and one child per cascading table), asserts every child exists, removes the Pipeline
     * once and asserts every child has been cascade-deleted while the unrelated parents (Client, Providers, Bot) survive. Cleans up the surviving parents at
     * the end.
     */
    @Test
    public void testRemovePipelineCascadesToChildren( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );

        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline Cascade Test" );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( client.getId( ) );
        PipelineHome.create( pipeline );
        int nPipelineId = pipeline.getId( );

        BotPipeline botPipeline = new BotPipeline( );
        botPipeline.setBotId( bot.getId( ) );
        botPipeline.setPipelineId( nPipelineId );
        botPipeline.setToolDescription( "Cascade tool" );
        BotPipelineHome.create( botPipeline );

        PipelineUserRateLimit rateLimit = new PipelineUserRateLimit( );
        rateLimit.setUserId( "cascade-user" );
        rateLimit.setPipelineId( nPipelineId );
        rateLimit.setExecutionCount( 1 );
        rateLimit.setDateFirstExecution( new Timestamp( System.currentTimeMillis( ) ) );
        PipelineUserRateLimitHome.create( rateLimit );

        PipelineExecution execution = new PipelineExecution( );
        execution.setIdPipeline( nPipelineId );
        execution.setIdClient( client.getId( ) );
        execution.setExecutionId( "cascade-exec-" + System.currentTimeMillis( ) );
        execution.setCreationDate( new Timestamp( System.currentTimeMillis( ) ) );
        execution.setStatus( PipelineExecution.STATUS_PENDING );
        execution.setInputs( "{}" );
        execution.setUserId( "cascade-user" );
        PipelineExecutionHome.create( execution );

        PipelineVersion version = new PipelineVersion( );
        version.setIdPipeline( nPipelineId );
        version.setVersionName( "v1" );
        version.setDescription( "Cascade version" );
        version.setFlow( "{\"nodes\":[]}" );
        version.setCurrent( false );
        version.setInputSchema( "{}" );
        version.setCreationDate( new Timestamp( System.currentTimeMillis( ) ) );
        PipelineVersionHome.create( version );

        assertFalse( BotPipelineHome.getBotPipelinesByBotId( bot.getId( ) ).isEmpty( ) );
        assertTrue( PipelineUserRateLimitHome.findByUserIdAndPipelineId( "cascade-user", nPipelineId ).isPresent( ) );
        assertFalse( PipelineExecutionHome.findByPipelineId( nPipelineId ).isEmpty( ) );
        assertFalse( PipelineVersionHome.findByPipelineId( nPipelineId ).isEmpty( ) );

        PipelineHome.remove( nPipelineId );

        assertTrue( BotPipelineHome.getBotPipelinesByBotId( bot.getId( ) ).isEmpty( ) );
        assertFalse( PipelineUserRateLimitHome.findByUserIdAndPipelineId( "cascade-user", nPipelineId ).isPresent( ) );
        assertTrue( PipelineExecutionHome.findByPipelineId( nPipelineId ).isEmpty( ) );
        assertTrue( PipelineVersionHome.findByPipelineId( nPipelineId ).isEmpty( ) );
    }

    /**
     * Creates a persisted Client to satisfy the id_client foreign keys of the Bot, the Pipeline and the PipelineExecution.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client Pipeline Cascade Test" );
        client.setCode( "CLIENT_PIPELINE_CASCADE_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a persisted Provider to satisfy a foreign key of the Bot.
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
     * Creates a persisted Bot used as the parent of the bot pipeline association.
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
        bot.setBotName( "Bot Pipeline Cascade Test" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nLlmProviderId );
        bot.setEmbedProviderId( nEmbedProviderId );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }
}
