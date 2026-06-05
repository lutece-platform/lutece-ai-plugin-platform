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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

import java.util.List;

/**
 * This class provides instances management methods (create, find, ...) for BotPipeline objects
 */
public final class BotPipelineHome
{
    private static IBotPipelineDAO _dao = CDI.current( ).select( IBotPipelineDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class need not be instantiated
     */
    private BotPipelineHome( )
    {
    }

    /**
     * Create an instance of the botPipeline class
     *
     * @param botPipeline
     *            The instance of the BotPipeline which contains the informations to store
     * @return The instance of botPipeline which has been created with its primary key.
     */
    public static BotPipeline create( BotPipeline botPipeline )
    {
        _dao.insert( botPipeline, _plugin );
        return botPipeline;
    }

    /**
     * Update of the botPipeline which is specified in parameter
     *
     * @param botPipeline
     *            The instance of the BotPipeline which contains the data to store
     * @return The instance of the botPipeline which has been updated
     */
    public static BotPipeline update( BotPipeline botPipeline )
    {
        _dao.store( botPipeline, _plugin );
        return botPipeline;
    }

    /**
     * Remove the botPipeline whose identifier is specified in parameter
     *
     * @param nKey
     *            The botPipeline Id
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Remove all botPipelines for a specific bot
     *
     * @param nBotId
     *            The bot Id
     */
    public static void removeByBotId( int nBotId )
    {
        _dao.deleteByBotId( nBotId, _plugin );
    }

    /**
     * Returns an instance of a botPipeline whose identifier is specified in parameter
     *
     * @param nKey
     *            The botPipeline primary key
     * @return an instance of BotPipeline
     */
    public static BotPipeline findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Load the data of all the botPipelines for a specific bot and returns them as a list
     *
     * @param nBotId
     *            The bot Id
     * @return the list which contains the data of all the botPipelines
     */
    public static List<BotPipeline> getBotPipelinesByBotId( int nBotId )
    {
        return _dao.selectBotPipelinesByBotId( nBotId, _plugin );
    }

    /**
     * Get the list of pipeline IDs associated with a bot
     *
     * @param nBotId
     *            The bot Id
     * @return the list of pipeline IDs
     */
    public static List<Integer> getPipelineIdsByBotId( int nBotId )
    {
        return _dao.selectPipelineIdsByBotId( nBotId, _plugin );
    }

    /**
     * Associate pipelines with a bot
     *
     * @param nBotId
     *            The bot Id
     * @param pipelineIds
     *            The list of pipeline IDs to associate
     */
    public static void associatePipelines( int nBotId, List<Integer> pipelineIds )
    {
        removeByBotId( nBotId );

        if ( pipelineIds != null )
        {
            for ( Integer nPipelineId : pipelineIds )
            {
                BotPipeline botPipeline = new BotPipeline( );
                botPipeline.setBotId( nBotId );
                botPipeline.setPipelineId( nPipelineId );
                create( botPipeline );
            }
        }
    }
}
