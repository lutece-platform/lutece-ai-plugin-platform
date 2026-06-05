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
package fr.paris.lutece.plugins.platform.service.sse;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tests the in-band registration contract of PlatformSseStreamManager : a subscription is registered directly against the operation id the streaming POST will
 * emit on (no init/streamId/ownership indirection), and the per-client-user concurrency cap is enforced on the live subscriptions.
 */
public class PlatformSseStreamManagerTest extends AbstractPlatformDbTest
{
    /**
     * Proves that an in-band registration returns a closeable handle that unregisters the subscription.
     */
    @Test
    public void testRegisterSseStreamReturnsCloseableHandle( ) throws Exception
    {
        PlatformSseStreamManager manager = CDI.current( ).select( PlatformSseStreamManager.class ).get( );

        AutoCloseable subscription = manager.registerSseStream( "op-basic-test", Bot.RESOURCE_TYPE, null, null );
        assertNotNull( subscription, "in-band registration must return a closeable handle" );
        subscription.close( );
    }

    /**
     * Proves that the per-client-user concurrency cap (default 3) blocks a further concurrent stream for the same (client, user), leaves other users
     * unaffected, rejects a missing user id, and frees up once subscriptions are closed.
     */
    @Test
    public void testPerClientUserConcurrencyCap( ) throws Exception
    {
        PlatformSseStreamManager manager = CDI.current( ).select( PlatformSseStreamManager.class ).get( );

        AutoCloseable s1 = manager.registerSseStream( "op-cap-1", Bot.RESOURCE_TYPE, null, null, 42, "user-a" );
        AutoCloseable s2 = manager.registerSseStream( "op-cap-2", Bot.RESOURCE_TYPE, null, null, 42, "user-a" );
        AutoCloseable s3 = manager.registerSseStream( "op-cap-3", Bot.RESOURCE_TYPE, null, null, 42, "user-a" );

        assertFalse( manager.canCreateNewStreamForClientUser( 42, "user-a" ), "the per-user cap (3) must block a 4th concurrent stream" );
        assertTrue( manager.canCreateNewStreamForClientUser( 42, "user-b" ), "another user is not capped by user-a's streams" );
        assertFalse( manager.canCreateNewStreamForClientUser( 42, "" ), "a missing user id must be rejected" );

        s1.close( );
        s2.close( );
        s3.close( );
        assertTrue( manager.canCreateNewStreamForClientUser( 42, "user-a" ), "closing the streams frees the per-user budget" );
    }
}
