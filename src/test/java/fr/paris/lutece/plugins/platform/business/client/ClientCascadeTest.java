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
package fr.paris.lutece.plugins.platform.business.client;

import java.sql.Timestamp;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionStatus;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecutionHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Verifies the database-level ON DELETE behavior declared on the foreign keys that reference {@code platform_client}. Removing a Client through
 * {@link ClientHome#remove(int)} must cascade-delete every dependent row (pipeline, dataset, bot, pipeline execution, subscription, vision, model) and must
 * null out the {@code client_id} of every observability resource execution (ON DELETE SET NULL). This proves the constraints reintroduced in
 * {@code create_db_platform.sql} are active on HSQL.
 */
public class ClientCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full child graph under a single Client, asserts each child exists before removal, removes the Client once, then asserts the seven CASCADE
     * children are gone and the SET NULL observability execution survives with a null client reference. Shared parent Providers are torn down at the end.
     */
    @Test
    public void testClientDeleteCascade( )
    {
        Provider llmProvider = createProvider( "Cascade LLM" );
        Provider embedProvider = createProvider( "Cascade Embedding" );

        Client client = new Client( );
        client.setName( "Cascade Root Client" );
        client.setCode( "cascade-root-client" );
        client.setDescription( "Root client for cascade test" );
        client.setActive( true );
        ClientHome.create( client );
        int nClientId = client.getId( );

        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Cascade Pipeline" );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( nClientId );
        PipelineHome.create( pipeline );
        int nPipelineId = pipeline.getId( );

        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Cascade Dataset" );
        dataset.setEmbedProviderId( embedProvider.getId( ) );
        dataset.setLlmProviderId( llmProvider.getId( ) );
        dataset.setClientId( nClientId );
        DatasetHome.create( dataset );
        int nDatasetId = dataset.getId( );

        Bot bot = new Bot( );
        bot.setBotName( "Cascade Bot" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( llmProvider.getId( ) );
        bot.setEmbedProviderId( embedProvider.getId( ) );
        BotHome.create( bot );
        int nBotId = bot.getId( );

        PipelineExecution pipelineExecution = new PipelineExecution( );
        pipelineExecution.setIdPipeline( nPipelineId );
        pipelineExecution.setIdClient( nClientId );
        pipelineExecution.setExecutionId( "cascade-exec-" + System.currentTimeMillis( ) );
        pipelineExecution.setCreationDate( new Timestamp( System.currentTimeMillis( ) ) );
        pipelineExecution.setStatus( PipelineExecution.STATUS_PENDING );
        pipelineExecution.setInputs( "{}" );
        pipelineExecution.setUserId( "cascade-user" );
        int nPipelineExecutionId = PipelineExecutionHome.create( pipelineExecution );

        Subscription subscription = new Subscription( );
        subscription.setClientId( nClientId );
        subscription.setResourceType( "bot" );
        subscription.setResourceId( "cascade-resource" );
        subscription.setStatus( SubscriptionStatus.ACTIVE );
        SubscriptionHome.create( subscription );
        int nSubscriptionId = subscription.getId( );

        Vision vision = new Vision( );
        vision.setVisionTitle( "Cascade Vision" );
        vision.setVisionDescription( "Cascade vision description" );
        vision.setClientId( nClientId );
        vision.setProviderId( llmProvider.getId( ) );
        VisionHome.create( vision );
        int nVisionId = vision.getId( );

        Model model = new Model( );
        model.setClientId( nClientId );
        model.setProviderId( llmProvider.getId( ) );
        ModelHome.create( model );
        int nModelId = model.getId( );

        PlatformResourceExecution resourceExecution = new PlatformResourceExecution( );
        resourceExecution.setResourceType( "BOT" );
        resourceExecution.setResourceId( String.valueOf( nBotId ) );
        resourceExecution.setClientId( nClientId );
        resourceExecution.setStatus( PlatformResourceExecutionStatus.PENDING );
        resourceExecution.setInputData( "{}" );
        String strResourceExecutionId = PlatformResourceExecutionHome.create( resourceExecution );

        assertTrue( PipelineHome.findByPrimaryKey( nPipelineId ).isPresent( ) );
        assertTrue( DatasetHome.findByPrimaryKey( nDatasetId ).isPresent( ) );
        assertTrue( BotHome.findByPrimaryKey( nBotId ).isPresent( ) );
        assertTrue( PipelineExecutionHome.findByPrimaryKey( nPipelineExecutionId ).isPresent( ) );
        assertTrue( SubscriptionHome.findByPrimaryKey( nSubscriptionId ).isPresent( ) );
        assertTrue( VisionHome.findByPrimaryKey( nVisionId ).isPresent( ) );
        assertTrue( ModelHome.findByPrimaryKey( nModelId ).isPresent( ) );
        Optional<PlatformResourceExecution> optExecBefore = PlatformResourceExecutionHome.findByPrimaryKey( strResourceExecutionId );
        assertTrue( optExecBefore.isPresent( ) );
        assertEquals( nClientId, optExecBefore.get( ).getClientId( ) );

        ClientHome.remove( nClientId );

        assertTrue( PipelineHome.findByPrimaryKey( nPipelineId ).isEmpty( ) );
        assertTrue( DatasetHome.findByPrimaryKey( nDatasetId ).isEmpty( ) );
        assertTrue( BotHome.findByPrimaryKey( nBotId ).isEmpty( ) );
        assertTrue( PipelineExecutionHome.findByPrimaryKey( nPipelineExecutionId ).isEmpty( ) );
        assertTrue( SubscriptionHome.findByPrimaryKey( nSubscriptionId ).isEmpty( ) );
        assertTrue( VisionHome.findByPrimaryKey( nVisionId ).isEmpty( ) );
        assertTrue( ModelHome.findByPrimaryKey( nModelId ).isEmpty( ) );

        Optional<PlatformResourceExecution> optExecAfter = PlatformResourceExecutionHome.findByPrimaryKey( strResourceExecutionId );
        assertTrue( optExecAfter.isPresent( ) );
        assertEquals( 0, optExecAfter.get( ).getClientId( ) );
    }

    /**
     * Creates and persists a Provider with all NOT NULL columns populated so the insert is valid on HSQL.
     *
     * @param strName
     *            the provider name
     * @return the persisted Provider with its generated identifier set
     */
    private Provider createProvider( String strName )
    {
        Provider provider = new Provider( );
        provider.setProviderName( strName );
        provider.setProviderType( "LLM" );
        provider.setProviderVendor( "mistral" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }
}
