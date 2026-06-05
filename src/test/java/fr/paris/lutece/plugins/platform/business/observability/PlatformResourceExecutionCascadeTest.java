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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Verifies the {@code ON DELETE CASCADE} foreign keys rooted on a PlatformResourceExecution. Removing the root must cascade-delete the rows of every child
 * table that references it: {@code platform_observability_resource_node_execution} (execution_id). Foreign keys are active on HSQL, so this test proves the
 * cascade end to end.
 */
public class PlatformResourceExecutionCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Creates a PlatformResourceExecution root (with its required parent Client) plus one PlatformResourceNodeExecution child, asserts the child exists,
     * removes the root once, then asserts the child has been cascade-deleted. The parent Client is removed at the end.
     */
    @Test
    public void testCascadeOnDeletePlatformResourceExecution( )
    {
        Client client = new Client( );
        client.setName( "Cascade Observability Client" );
        client.setCode( "OBS_CASCADE_TEST_" + System.currentTimeMillis( ) );
        Client createdClient = ClientHome.create( client );

        PlatformResourceExecution execution = new PlatformResourceExecution( );
        execution.setResourceType( "PIPELINE" );
        execution.setResourceId( "99" );
        execution.setClientId( createdClient.getId( ) );
        execution.setStatus( PlatformResourceExecutionStatus.RUNNING );
        String executionId = PlatformResourceExecutionHome.create( execution );

        PlatformResourceNodeExecution nodeExecution = new PlatformResourceNodeExecution( );
        nodeExecution.setExecutionId( executionId );
        nodeExecution.setNodeId( "cascade-node-1" );
        nodeExecution.setNodeName( "Cascade Node" );
        nodeExecution.setStatus( PlatformResourceExecutionStatus.PENDING );
        nodeExecution.setExecutionOrder( 1 );
        PlatformResourceNodeExecutionHome.create( nodeExecution );

        assertFalse( PlatformResourceNodeExecutionHome.getNodeExecutions( executionId ).isEmpty( ) );

        PlatformResourceExecutionHome.remove( executionId );

        assertTrue( PlatformResourceExecutionHome.findByPrimaryKey( executionId ).isEmpty( ) );
        assertTrue( PlatformResourceNodeExecutionHome.getNodeExecutions( executionId ).isEmpty( ) );
    }
}
