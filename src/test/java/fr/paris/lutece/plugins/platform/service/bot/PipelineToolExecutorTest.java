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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineVersionService;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Tests the thread-interruption contract of PipelineToolExecutor : an InterruptedException raised while waiting for the pipeline completion must restore the
 * interrupt status of the calling thread instead of silently swallowing it.
 */
public class PipelineToolExecutorTest extends AbstractPlatformDbTest
{
    /**
     * Runs execute() on a worker thread and interrupts that worker repeatedly until execute returns : the InterruptedException raised inside the completion
     * wait must leave the interrupt status restored on the worker when execute() returns.
     *
     * @throws InterruptedException
     *             if the test thread itself is interrupted while joining the worker
     */
    @Test
    public void testExecuteRestoresInterruptStatus( ) throws InterruptedException
    {
        Client client = new Client( );
        client.setName( "Client ToolExec Test" );
        client.setCode( "CLIENT_TOOLEXEC_TEST" );
        client.setActive( true );
        client = ClientHome.create( client );

        Pipeline pipeline = PipelineVersionService.createPipelineWithInitialVersion( "tool-exec-interrupt", "interrupt test", 1, 5, client.getId( ) );
        PipelineToolExecutor executor = new PipelineToolExecutor( pipeline.getId( ) );
        ToolExecutionRequest request = ToolExecutionRequest.builder( ).name( "pipeline" ).arguments( "{}" ).build( );

        AtomicBoolean interruptRestored = new AtomicBoolean( false );
        Thread worker = new Thread( ( ) -> {
            executor.execute( request, null );
            interruptRestored.set( Thread.currentThread( ).isInterrupted( ) );
        } );
        worker.start( );

        long deadline = System.currentTimeMillis( ) + 30000;
        while ( worker.isAlive( ) && System.currentTimeMillis( ) < deadline )
        {
            worker.interrupt( );
            Thread.sleep( 100 );
        }
        worker.join( 5000 );

        assertFalse( worker.isAlive( ), "execute() must return after interruption" );
        assertTrue( interruptRestored.get( ), "execute() must restore the interrupt status after an InterruptedException" );
    }
}
