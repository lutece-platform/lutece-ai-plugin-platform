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
package fr.paris.lutece.plugins.platform.business.bot;

import java.util.List;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.plugins.platform.service.cache.DatasetCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;

/**
 * This class provides instances management methods for BotDataset objects
 */
public final class BotDatasetHome
{
    private static IBotDatasetDAO _dao = CDI.current( ).select( IBotDatasetDAO.class ).get( );
    private static final DatasetCacheService _cache = CDI.current( ).select( DatasetCacheService.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private BotDatasetHome( )
    {
    }

    /**
     * Creates an association between a bot and a dataset
     *
     * @param botDataset
     *            The association to create
     */
    public static void associate( BotDataset botDataset )
    {
        _dao.associate( botDataset, _plugin );
        PlatformCacheEvents.fire( PlatformResource.BOT, botDataset.getBotId( ), Action.UPDATED );
    }

    /**
     * Returns a list of dataset identifiers associated with a given bot
     *
     * @param nBotId
     *            The bot identifier
     * @return The list of dataset identifiers
     */
    @SuppressWarnings( "unchecked" )
    public static List<Integer> getDatasetIdsByBotId( int nBotId )
    {
        String strKey = DatasetCacheService.getBotDatasetsKey( nBotId );
        Object cached = _cache.get( strKey );

        if ( cached != null )
        {
            return (List<Integer>) cached;
        }

        List<Integer> result = _dao.selectDatasetIdsByBotId( nBotId, _plugin );
        _cache.put( strKey, result );
        return result;
    }

    /**
     * Returns a list of bot identifiers associated with a given dataset
     *
     * @param nDatasetId
     *            The dataset identifier
     * @return The list of bot identifiers
     */
    public static List<Integer> getBotIdsByDatasetId( int nDatasetId )
    {
        return _dao.selectBotIdsByDatasetId( nDatasetId, _plugin );
    }

    /**
     * Removes all associations for a given bot
     *
     * @param nBotId
     *            The bot identifier
     */
    public static void removeByBotId( int nBotId )
    {
        _dao.deleteByBotId( nBotId, _plugin );
        PlatformCacheEvents.fire( PlatformResource.BOT, nBotId, Action.UPDATED );
    }

}
