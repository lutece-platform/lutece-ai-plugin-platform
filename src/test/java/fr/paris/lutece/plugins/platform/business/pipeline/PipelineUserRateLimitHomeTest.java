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
 * Test suite for the PipelineUserRateLimitHome CRUD operations.
 */
public class PipelineUserRateLimitHomeTest extends AbstractPlatformDbTest
{
    /**
     * incrementIfBelow only increments while strictly below the limit, atomically: at the limit it returns false and the stored count never exceeds the limit.
     */
    @Test
    public void testIncrementIfBelowStopsAtLimit( )
    {
        Client client = new Client( );
        client.setName( "Pipeline Incr Test Client" );
        client.setCode( "pipeline-incr-test-client" );
        client.setActive( true );
        ClientHome.create( client );

        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline for atomic increment" );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( client.getId( ) );
        PipelineHome.create( pipeline );

        PipelineUserRateLimit rateLimit = new PipelineUserRateLimit( );
        rateLimit.setUserId( "user-incr" );
        rateLimit.setPipelineId( pipeline.getId( ) );
        rateLimit.setExecutionCount( 4 );
        rateLimit.setDateFirstExecution( new Timestamp( System.currentTimeMillis( ) ) );
        PipelineUserRateLimitHome.create( rateLimit );

        assertTrue( PipelineUserRateLimitHome.incrementIfBelow( rateLimit.getId( ), 5 ), "count 4 < limit 5: increment must succeed" );
        assertFalse( PipelineUserRateLimitHome.incrementIfBelow( rateLimit.getId( ), 5 ), "count 5 = limit 5: increment must be refused" );
        assertEquals( 5, PipelineUserRateLimitHome.findByPrimaryKey( rateLimit.getId( ) ).orElseThrow( ).getExecutionCount( ),
                "the stored count must never exceed the limit" );
    }

    /**
     * Full CRUD round-trip of the PipelineUserRateLimit Home.
     */
    @Test
    public void testBusinessPipelineUserRateLimit( )
    {
        Client client = new Client( );
        client.setName( "Pipeline Rate Limit Test Client" );
        client.setCode( "pipeline-rate-limit-test-client" );
        client.setActive( true );
        ClientHome.create( client );

        Pipeline pipeline = new Pipeline( );
        pipeline.setName( "Pipeline for rate limit" );
        pipeline.setMaxConcurrentWorkers( 1 );
        pipeline.setIdClient( client.getId( ) );
        PipelineHome.create( pipeline );

        PipelineUserRateLimit rateLimit = new PipelineUserRateLimit( );
        rateLimit.setUserId( "user-1" );
        rateLimit.setPipelineId( pipeline.getId( ) );
        rateLimit.setExecutionCount( 1 );
        rateLimit.setDateFirstExecution( new Timestamp( System.currentTimeMillis( ) ) );

        int nId = PipelineUserRateLimitHome.create( rateLimit );
        Optional<PipelineUserRateLimit> stored = PipelineUserRateLimitHome.findByPrimaryKey( nId );
        assertTrue( stored.isPresent( ) );
        assertEquals( rateLimit.getExecutionCount( ), stored.get( ).getExecutionCount( ) );

        rateLimit.setExecutionCount( 5 );
        PipelineUserRateLimitHome.update( rateLimit );
        stored = PipelineUserRateLimitHome.findByPrimaryKey( nId );
        assertTrue( stored.isPresent( ) );
        assertEquals( rateLimit.getExecutionCount( ), stored.get( ).getExecutionCount( ) );

        PipelineUserRateLimitHome.remove( nId );
        stored = PipelineUserRateLimitHome.findByPrimaryKey( nId );
        assertFalse( stored.isPresent( ) );
    }
}
