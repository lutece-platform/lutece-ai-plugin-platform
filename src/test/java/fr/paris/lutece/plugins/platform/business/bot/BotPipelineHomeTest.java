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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for BotPipelineHome on HSQL
 */
public class BotPipelineHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full create / findByPrimaryKey / update / remove cycle of BotPipelineHome. The parent Bot and Pipeline (and their own Client and Provider
     * parents) are created first to satisfy the foreign keys, then removed at the end.
     */
    @Test
    public void testBusinessBotPipeline( )
    {
        Client client = createClient( );
        Provider llmProvider = createProvider( "LLM" );
        Provider embedProvider = createProvider( "EMBEDDING" );
        Bot bot = createBot( client.getId( ), llmProvider.getId( ), embedProvider.getId( ) );
        int nPipelineId = createPipeline( client.getId( ) );

        BotPipeline botPipeline = new BotPipeline( );
        botPipeline.setBotId( bot.getId( ) );
        botPipeline.setPipelineId( nPipelineId );
        botPipeline.setToolDescription( "ToolDescription 1" );

        BotPipelineHome.create( botPipeline );
        BotPipeline stored = BotPipelineHome.findByPrimaryKey( botPipeline.getId( ) );
        assertNotNull( stored );
        assertEquals( botPipeline.getToolDescription( ), stored.getToolDescription( ) );
        assertEquals( botPipeline.getPipelineId( ), stored.getPipelineId( ) );

        botPipeline.setToolDescription( "ToolDescription 2" );
        BotPipelineHome.update( botPipeline );
        stored = BotPipelineHome.findByPrimaryKey( botPipeline.getId( ) );
        assertNotNull( stored );
        assertEquals( botPipeline.getToolDescription( ), stored.getToolDescription( ) );

        BotPipelineHome.getBotPipelinesByBotId( bot.getId( ) );

        BotPipelineHome.remove( botPipeline.getId( ) );
        stored = BotPipelineHome.findByPrimaryKey( botPipeline.getId( ) );
        assertNull( stored );
    }

    /**
     * Creates a persisted Client to be used as the foreign key parent of a Bot and a Pipeline.
     *
     * @return The created Client with its generated primary key
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client BotPipeline Test" );
        client.setCode( "CLIENT_BOTPIPELINE_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a persisted Provider to be used as a foreign key parent of a Bot.
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
     * Creates a persisted Bot to be used as a foreign key parent of a bot pipeline association.
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
        bot.setBotName( "Bot Pipeline Test" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nLlmProviderId );
        bot.setEmbedProviderId( nEmbedProviderId );
        bot.setMaxTokens( 4000 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }

    /**
     * Creates a persisted Pipeline to be used as a foreign key parent of a bot pipeline association.
     *
     * @param nClientId
     *            The parent client identifier
     * @return The generated pipeline identifier
     */
    private int createPipeline( int nClientId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline BotPipeline Test" );
        pipeline.setIdClient( nClientId );
        return PipelineHome.create( pipeline );
    }
}
