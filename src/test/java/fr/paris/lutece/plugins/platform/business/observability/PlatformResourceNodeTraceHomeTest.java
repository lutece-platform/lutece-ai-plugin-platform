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

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test suite for the PlatformResourceNodeTraceHome CRUD operations.
 */
public class PlatformResourceNodeTraceHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the create / read / remove cycle of PlatformResourceNodeTraceHome. The full parent chain (Client, execution, node execution) is created first
     * to satisfy the foreign keys, then removed at the end. The Home exposes no update, so only create, getNodeTraces and removeByNodeExecutionId are covered.
     */
    @Test
    public void testBusinessPlatformResourceNodeTrace( )
    {
        Client client = new Client( );
        client.setName( "Observability Trace Test Client" );
        client.setCode( "OBS_TRACE_TEST_" + System.currentTimeMillis( ) );
        Client createdClient = ClientHome.create( client );

        PlatformResourceExecution execution = new PlatformResourceExecution( );
        execution.setResourceType( "PIPELINE" );
        execution.setResourceId( "9" );
        execution.setClientId( createdClient.getId( ) );
        execution.setStatus( PlatformResourceExecutionStatus.RUNNING );
        String executionId = PlatformResourceExecutionHome.create( execution );

        PlatformResourceNodeExecution nodeExecution = new PlatformResourceNodeExecution( );
        nodeExecution.setExecutionId( executionId );
        nodeExecution.setNodeId( "node-trace" );
        nodeExecution.setNodeName( "Trace Node" );
        nodeExecution.setStatus( PlatformResourceExecutionStatus.RUNNING );
        nodeExecution.setExecutionOrder( 1 );
        PlatformResourceNodeExecutionHome.create( nodeExecution );

        int idNodeExecution = PlatformResourceNodeExecutionHome.getNodeExecutions( executionId ).get( 0 ).getId( );

        PlatformResourceNodeTrace trace = new PlatformResourceNodeTrace( );
        trace.setIdNodeExecution( idNodeExecution );
        trace.setMessage( "Node started" );
        trace.setData( "{\"step\":\"init\"}" );
        trace.setStatus( PlatformResourceNodeTraceStatus.INFO );

        PlatformResourceNodeTraceHome.create( trace );

        List<PlatformResourceNodeTrace> stored = PlatformResourceNodeTraceHome.getNodeTraces( idNodeExecution );
        assertEquals( 1, stored.size( ) );
        assertEquals( "Node started", stored.get( 0 ).getMessage( ) );
        assertEquals( PlatformResourceNodeTraceStatus.INFO, stored.get( 0 ).getStatus( ) );

        PlatformResourceNodeTraceHome.removeByNodeExecutionId( idNodeExecution );
        stored = PlatformResourceNodeTraceHome.getNodeTraces( idNodeExecution );
        assertTrue( stored.isEmpty( ) );
    }
}
