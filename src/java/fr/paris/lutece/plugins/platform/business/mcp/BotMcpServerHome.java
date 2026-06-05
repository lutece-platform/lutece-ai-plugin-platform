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
package fr.paris.lutece.plugins.platform.business.mcp;

import java.util.List;
import fr.paris.lutece.util.sql.TransactionManager;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Static facade for managing BotMcpServer associations
 */
public final class BotMcpServerHome
{
    private static IBotMcpServerDAO _dao = CDI.current( ).select( IBotMcpServerDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor — facade only
     */
    private BotMcpServerHome( )
    {
    }

    /**
     * Creates an association between a bot and an MCP server
     *
     * @param botMcpServer
     *            The association to create
     */
    public static void associate( BotMcpServer botMcpServer )
    {
        _dao.associate( botMcpServer, _plugin );
    }

    /**
     * Removes an association between a bot and an MCP server
     *
     * @param botMcpServer
     *            The association to remove
     */
    public static void dissociate( BotMcpServer botMcpServer )
    {
        _dao.dissociate( botMcpServer, _plugin );
    }

    /**
     * Returns a list of MCP server identifiers associated with a given bot
     *
     * @param nBotId
     *            The bot identifier
     * @return The list of MCP server identifiers
     */
    public static List<Integer> getMcpServerIdsByBotId( int nBotId )
    {
        return _dao.selectMcpServerIdsByBotId( nBotId, _plugin );
    }

    /**
     * Returns a list of bot identifiers associated with a given MCP server
     *
     * @param nMcpServerId
     *            The MCP server identifier
     * @return The list of bot identifiers
     */
    public static List<Integer> getBotIdsByMcpServerId( int nMcpServerId )
    {
        return _dao.selectBotIdsByMcpServerId( nMcpServerId, _plugin );
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
    }

    /**
     * Removes all associations for a given MCP server
     *
     * @param nMcpServerId
     *            The MCP server identifier
     */
    public static void removeByMcpServerId( int nMcpServerId )
    {
        _dao.deleteByMcpServerId( nMcpServerId, _plugin );
    }

    /**
     * Replaces the bot ↔ MCP server associations atomically: drops existing rows for the bot and inserts the supplied list in a single transaction, so a
     * failure mid-insert cannot lose the previous associations. A null or empty list simply clears all associations.
     *
     * @param nBotId
     *            The bot identifier
     * @param mcpServerIds
     *            The MCP server identifiers to associate (nullable)
     */
    public static void associateMcpServers( int nBotId, List<Integer> mcpServerIds )
    {
        TransactionManager.beginTransaction( _plugin );
        try
        {
            removeByBotId( nBotId );
            if ( mcpServerIds != null )
            {
                for ( Integer nServerId : mcpServerIds )
                {
                    associate( new BotMcpServer( nBotId, nServerId ) );
                }
            }
            TransactionManager.commitTransaction( _plugin );
        }
        catch( Exception e )
        {
            TransactionManager.rollBack( _plugin );
            throw e;
        }
    }
}
