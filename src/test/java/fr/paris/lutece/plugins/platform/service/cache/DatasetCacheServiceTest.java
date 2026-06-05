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
 * Verifies {@link DatasetCacheService} invalidation, including the cross-entity case that closes the FK-cascade gap: removing a bot must evict that bot's
 * cached dataset-id list ({@code bot_datasets}), which the database cascade would otherwise leave stale.
 */
public class DatasetCacheServiceTest extends AbstractPlatformDbTest
{
    private static final int DATASET_ID = 424242;
    private static final int OTHER_DATASET_ID = 424243;
    private static final int BOT_ID = 525252;

    private DatasetCacheService _cache;
    private boolean _previousStatus;

    /**
     * Enables the cache before each test, remembering the previous status.
     */
    @BeforeEach
    public void enableCache( )
    {
        _cache = CDI.current( ).select( DatasetCacheService.class ).get( );
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
     * A dataset UPDATED event evicts only that dataset's key, not other datasets.
     */
    @Test
    public void testDatasetUpdatedInvalidatesOnlyItsKey( )
    {
        _cache.put( DatasetCacheService.getKey( DATASET_ID ), "sentinel" );
        _cache.put( DatasetCacheService.getKey( OTHER_DATASET_ID ), "other" );

        PlatformCacheEvents.fire( PlatformResource.DATASET, DATASET_ID, EntityChangedEvent.Action.UPDATED );

        assertNull( _cache.get( DatasetCacheService.getKey( DATASET_ID ) ) );
        assertNotNull( _cache.get( DatasetCacheService.getKey( OTHER_DATASET_ID ) ) );
    }

    /**
     * A dataset REMOVED event clears the whole cache (the DB cascade hits bot_dataset rows of unknown bots).
     */
    @Test
    public void testDatasetRemovedClearsCache( )
    {
        _cache.put( DatasetCacheService.getKey( DATASET_ID ), "sentinel" );
        _cache.put( DatasetCacheService.getBotDatasetsKey( BOT_ID ), "assoc" );

        PlatformCacheEvents.fire( PlatformResource.DATASET, DATASET_ID, EntityChangedEvent.Action.REMOVED );

        assertNull( _cache.get( DatasetCacheService.getKey( DATASET_ID ) ) );
        assertNull( _cache.get( DatasetCacheService.getBotDatasetsKey( BOT_ID ) ) );
    }

    /**
     * Removing a bot evicts its cached dataset-id list — the cross-entity invalidation that the FK cascade alone would miss.
     */
    @Test
    public void testBotRemovedInvalidatesBotDatasets( )
    {
        _cache.put( DatasetCacheService.getBotDatasetsKey( BOT_ID ), "assoc" );
        assertNotNull( _cache.get( DatasetCacheService.getBotDatasetsKey( BOT_ID ) ) );

        PlatformCacheEvents.fire( PlatformResource.BOT, BOT_ID, EntityChangedEvent.Action.REMOVED );

        assertNull( _cache.get( DatasetCacheService.getBotDatasetsKey( BOT_ID ) ) );
    }

    /**
     * Changing a bot's dataset associations (a bot UPDATED event) evicts its cached dataset-id list.
     */
    @Test
    public void testBotUpdatedInvalidatesBotDatasets( )
    {
        _cache.put( DatasetCacheService.getBotDatasetsKey( BOT_ID ), "assoc" );

        PlatformCacheEvents.fire( PlatformResource.BOT, BOT_ID, EntityChangedEvent.Action.UPDATED );

        assertNull( _cache.get( DatasetCacheService.getBotDatasetsKey( BOT_ID ) ) );
    }

    /**
     * An unrelated resource event must not touch the dataset cache.
     */
    @Test
    public void testOtherResourceEventDoesNotInvalidateDataset( )
    {
        _cache.put( DatasetCacheService.getKey( DATASET_ID ), "sentinel" );

        PlatformCacheEvents.fire( PlatformResource.PROVIDER, DATASET_ID, EntityChangedEvent.Action.UPDATED );

        assertNotNull( _cache.get( DatasetCacheService.getKey( DATASET_ID ) ) );
    }
}
