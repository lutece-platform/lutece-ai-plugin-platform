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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test suite for the PipelineExecutionHome CRUD operations.
 */
public class PipelineExecutionHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full create / findByPrimaryKey / update / remove cycle of PipelineExecutionHome. A parent Client and a parent Pipeline are created
     * beforehand to satisfy the id_client and id_pipeline foreign keys and removed at the end.
     */
    @Test
    public void testBusinessPipelineExecution( )
    {
        Client client = new Client( );
        client.setName( "Pipeline Execution Test Client" );
        client.setCode( "pipeline-execution-test-client" );
        client.setActive( true );
        ClientHome.create( client );

        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline for execution" );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( client.getId( ) );
        PipelineHome.create( pipeline );

        PipelineExecution execution = new PipelineExecution( );
        execution.setIdPipeline( pipeline.getId( ) );
        execution.setIdClient( client.getId( ) );
        execution.setExecutionId( "exec-" + System.currentTimeMillis( ) );
        execution.setCreationDate( new Timestamp( System.currentTimeMillis( ) ) );
        execution.setStatus( PipelineExecution.STATUS_PENDING );
        execution.setInputs( "{}" );
        execution.setUserId( "user-1" );

        int nExecutionId = PipelineExecutionHome.create( execution );
        Optional<PipelineExecution> stored = PipelineExecutionHome.findByPrimaryKey( nExecutionId );
        assertTrue( stored.isPresent( ) );
        assertEquals( execution.getStatus( ), stored.get( ).getStatus( ) );

        execution.setStatus( PipelineExecution.STATUS_COMPLETED );
        PipelineExecutionHome.update( execution );
        stored = PipelineExecutionHome.findByPrimaryKey( nExecutionId );
        assertTrue( stored.isPresent( ) );
        assertEquals( execution.getStatus( ), stored.get( ).getStatus( ) );

        PipelineExecutionHome.remove( nExecutionId );
        stored = PipelineExecutionHome.findByPrimaryKey( nExecutionId );
        assertFalse( stored.isPresent( ) );

        PipelineExecutionHome.findByPipelineId( pipeline.getId( ) );
    }
}
