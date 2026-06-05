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
package fr.paris.lutece.plugins.platform.service.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Verifies that {@link ClientCacheService} invalidates both cached client lists on any client mutation, and only for the {@link PlatformResource#CLIENT}
 * resource.
 */
public class ClientCacheServiceTest extends AbstractPlatformDbTest
{
    private ClientCacheService _cache;
    private boolean _previousStatus;

    /**
     * Enables the cache before each test, remembering the previous status.
     */
    @BeforeEach
    public void enableCache( )
    {
        _cache = CDI.current( ).select( ClientCacheService.class ).get( );
        _previousStatus = _cache.isCacheEnable( );
        _cache.enableCache( true );
    }

    /**
     * Restores the previous cache status after each test.
     */
    @AfterEach
    public void restoreCache( )
    {
        _cache.clear( );
        _cache.enableCache( _previousStatus );
    }

    /**
     * A client CREATED event evicts both cached lists.
     */
    @Test
    public void testCreatedEventInvalidatesBothLists( )
    {
        _cache.put( ClientCacheService.getAllListKey( ), "all" );
        _cache.put( ClientCacheService.getActiveListKey( ), "active" );
        assertNotNull( _cache.get( ClientCacheService.getAllListKey( ) ) );
        assertNotNull( _cache.get( ClientCacheService.getActiveListKey( ) ) );

        PlatformCacheEvents.fire( PlatformResource.CLIENT, 1, EntityChangedEvent.Action.CREATED );

        assertNull( _cache.get( ClientCacheService.getAllListKey( ) ) );
        assertNull( _cache.get( ClientCacheService.getActiveListKey( ) ) );
    }

    /**
     * A client REMOVED event evicts both cached lists.
     */
    @Test
    public void testRemovedEventInvalidatesBothLists( )
    {
        _cache.put( ClientCacheService.getAllListKey( ), "all" );
        _cache.put( ClientCacheService.getActiveListKey( ), "active" );

        PlatformCacheEvents.fire( PlatformResource.CLIENT, 1, EntityChangedEvent.Action.REMOVED );

        assertNull( _cache.get( ClientCacheService.getAllListKey( ) ) );
        assertNull( _cache.get( ClientCacheService.getActiveListKey( ) ) );
    }

    /**
     * An event for another resource must not evict the client lists.
     */
    @Test
    public void testOtherResourceEventDoesNotInvalidateLists( )
    {
        _cache.put( ClientCacheService.getAllListKey( ), "all" );

        PlatformCacheEvents.fire( PlatformResource.BOT, 1, EntityChangedEvent.Action.UPDATED );

        assertNotNull( _cache.get( ClientCacheService.getAllListKey( ) ) );
    }
}
