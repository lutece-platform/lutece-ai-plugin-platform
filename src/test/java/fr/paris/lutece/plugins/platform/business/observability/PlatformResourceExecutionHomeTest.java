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
package fr.paris.lutece.plugins.platform.business.observability;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test suite for the PlatformResourceExecutionHome CRUD operations.
 */
public class PlatformResourceExecutionHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full create / findByPrimaryKey / update / remove cycle of PlatformResourceExecutionHome. A parent Client is created first to satisfy the
     * client_id foreign key, then removed at the end of the test.
     */
    @Test
    public void testBusinessPlatformResourceExecution( )
    {
        Client client = new Client( );
        client.setName( "Observability Test Client" );
        client.setCode( "OBS_EXEC_TEST_" + System.currentTimeMillis( ) );
        Client createdClient = ClientHome.create( client );

        PlatformResourceExecution execution = new PlatformResourceExecution( );
        execution.setResourceType( "BOT" );
        execution.setResourceId( "42" );
        execution.setClientId( createdClient.getId( ) );
        execution.setStatus( PlatformResourceExecutionStatus.PENDING );
        execution.setInputData( "{\"prompt\":\"hello\"}" );

        String executionId = PlatformResourceExecutionHome.create( execution );
        assertNotNull( executionId );

        Optional<PlatformResourceExecution> stored = PlatformResourceExecutionHome.findByPrimaryKey( executionId );
        assertTrue( stored.isPresent( ) );
        assertEquals( execution.getResourceType( ), stored.get( ).getResourceType( ) );
        assertEquals( execution.getResourceId( ), stored.get( ).getResourceId( ) );
        assertEquals( PlatformResourceExecutionStatus.PENDING, stored.get( ).getStatus( ) );

        execution.setStatus( PlatformResourceExecutionStatus.COMPLETED );
        execution.setOutputData( "{\"answer\":\"world\"}" );
        PlatformResourceExecutionHome.update( execution );

        stored = PlatformResourceExecutionHome.findByPrimaryKey( executionId );
        assertTrue( stored.isPresent( ) );
        assertEquals( PlatformResourceExecutionStatus.COMPLETED, stored.get( ).getStatus( ) );
        assertEquals( "{\"answer\":\"world\"}", stored.get( ).getOutputData( ) );

        PlatformResourceExecutionHome.remove( executionId );
        stored = PlatformResourceExecutionHome.findByPrimaryKey( executionId );
        assertTrue( stored.isEmpty( ) );
    }
}
