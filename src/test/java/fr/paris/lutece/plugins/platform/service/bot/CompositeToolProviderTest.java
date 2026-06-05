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

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.bot.BotPipeline;
import fr.paris.lutece.plugins.platform.business.bot.BotPipelineHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Tool-name uniqueness contract of {@link CompositeToolProvider}: two pipelines whose names normalize to the same tool name (e.g. "Search Docs" and "search
 * docs" both become {@code pipeline_search_docs}) must not produce a duplicated tool registration — langchain4j's ToolService rejects duplicated names with an
 * IllegalConfigurationException at chat time, killing the whole request.
 */
public class CompositeToolProviderTest extends AbstractPlatformDbTest
{
    /**
     * Two pipelines with colliding normalized names yield a single registered tool, not a duplicate that would crash the chat at AiServices time.
     */
    @Test
    public void testCollidingPipelineNamesAreDeduplicated( )
    {
        Client client = createClient( );
        Bot bot = createBot( client );
        associatePipeline( bot, createPipeline( client, "Search Docs" ) );
        associatePipeline( bot, createPipeline( client, "search docs" ) );

        CompositeToolProvider provider = new CompositeToolProvider( bot, null, "op-test" );
        ToolProviderResult result = provider.provideTools( new ToolProviderRequest( "memory-test", UserMessage.from( "hello" ) ) );

        Set<String> names = new HashSet<>( );
        for ( AiServiceTool tool : result.aiServiceTools( ) )
        {
            assertTrue( names.add( tool.name( ) ), "duplicated tool name registered: " + tool.name( ) );
        }
        assertEquals( 1, names.size( ), "the two colliding pipelines must collapse into a single tool" );
    }

    /**
     * Creates and persists a client.
     *
     * @return the persisted client
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Client tools" );
        client.setCode( "CLIENT_TOOLS_TEST" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates and persists a bot without builtin tools or MCP servers.
     *
     * @param client
     *            the owning client
     * @return the persisted bot
     */
    private Bot createBot( Client client )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Tools Provider" );
        provider.setProviderType( "llm" );
        provider.setDeploymentModelName( "model" );
        provider.setDeploymentApiKey( "key" );
        ProviderHome.create( provider );

        Bot bot = new Bot( );
        bot.setBotName( "Tools Bot" );
        bot.setBotDescription( "Tools test bot" );
        bot.setBotSystemPrompt( "prompt" );
        bot.setClientId( client.getId( ) );
        bot.setLlmProviderId( provider.getId( ) );
        bot.setEmbedProviderId( provider.getId( ) );
        bot.setMaxTokens( 1000 );
        bot.setTemperature( 0.5 );
        BotHome.create( bot );
        return bot;
    }

    /**
     * Creates and persists a pipeline with the given name.
     *
     * @param client
     *            the owning client
     * @param strName
     *            the pipeline name
     * @return the persisted pipeline
     */
    private Pipeline createPipeline( Client client, String strName )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( strName );
        pipeline.setDescription( "Pipeline " + strName );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setRateLimitByUserByDay( 10 );
        pipeline.setIdClient( client.getId( ) );
        PipelineHome.create( pipeline );
        return pipeline;
    }

    /**
     * Associates a pipeline to a bot as a tool.
     *
     * @param bot
     *            the bot
     * @param pipeline
     *            the pipeline to expose as a tool
     */
    private void associatePipeline( Bot bot, Pipeline pipeline )
    {
        BotPipeline botPipeline = new BotPipeline( );
        botPipeline.setBotId( bot.getId( ) );
        botPipeline.setPipelineId( pipeline.getId( ) );
        botPipeline.setToolDescription( "Tool for " + pipeline.getName( ) );
        BotPipelineHome.create( botPipeline );
    }
}
