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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Cache for dataset configuration. RAG-enabled conversations reload their datasets by primary key on each query; datasets are stable configuration, cached per
 * id and invalidated on mutation.
 */
@ApplicationScoped
public class DatasetCacheService extends AbstractPlatformCacheService
{
    private static final String CACHE_NAME = "platform.datasetCacheService";
    private static final String KEY_PREFIX = "platform.dataset.";
    private static final String BOT_DATASETS_PREFIX = "platform.bot.datasets.";

    /**
     * Registers the cache at bean initialization.
     */
    @PostConstruct
    public void init( )
    {
        initCache( CACHE_NAME, String.class, Object.class );
    }

    /**
     * Returns the cache name.
     *
     * @return the cache name
     */
    @Override
    public String getName( )
    {
        return CACHE_NAME;
    }

    /**
     * Returns the cache key for a dataset by id.
     *
     * @param nId
     *            the dataset id
     * @return the cache key
     */
    public static String getKey( int nId )
    {
        return KEY_PREFIX + nId;
    }

    /**
     * Returns the cache key for the dataset ids associated with a bot.
     *
     * @param nBotId
     *            the bot id
     * @return the cache key
     */
    public static String getBotDatasetsKey( int nBotId )
    {
        return BOT_DATASETS_PREFIX + nBotId;
    }

    /**
     * Invalidates dataset-related cache entries on mutation. A dataset update drops its own key; a dataset removal cascades (in the DB) to {@code bot_dataset}
     * rows of unknown bots, so the whole cache is cleared. A bot mutation (incl. its removal, which cascades to its associations) drops that bot's dataset-id
     * list. Observes every {@link EntityChangedEvent}.
     *
     * @param event
     *            the entity-changed event
     */
    public void onEntityChanged( @Observes EntityChangedEvent event )
    {
        switch( event.resource( ) )
        {
            case DATASET:
                if ( event.action( ) == EntityChangedEvent.Action.REMOVED )
                {
                    clear( );
                }
                else
                {
                    remove( getKey( event.id( ) ) );
                }
                break;
            case BOT:
                remove( getBotDatasetsKey( event.id( ) ) );
                break;
            default:
                break;
        }
    }
}
