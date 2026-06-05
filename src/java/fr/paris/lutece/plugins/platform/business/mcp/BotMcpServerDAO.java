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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * JDBC implementation of IBotMcpServerDAO
 */
@ApplicationScoped
@Named( "platform.botMcpServerDAO" )
public class BotMcpServerDAO implements IBotMcpServerDAO
{
    private static final String SQL_QUERY_ASSOCIATE = "INSERT INTO platform_bot_mcp_server (bot_id, mcp_server_id) VALUES (?, ?)";
    private static final String SQL_QUERY_DISSOCIATE = "DELETE FROM platform_bot_mcp_server WHERE bot_id = ? AND mcp_server_id = ?";
    private static final String SQL_QUERY_SELECT_SERVERS_BY_BOT = "SELECT mcp_server_id FROM platform_bot_mcp_server WHERE bot_id = ?";
    private static final String SQL_QUERY_SELECT_BOTS_BY_SERVER = "SELECT bot_id FROM platform_bot_mcp_server WHERE mcp_server_id = ?";
    private static final String SQL_QUERY_DELETE_BY_BOT = "DELETE FROM platform_bot_mcp_server WHERE bot_id = ?";
    private static final String SQL_QUERY_DELETE_BY_SERVER = "DELETE FROM platform_bot_mcp_server WHERE mcp_server_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void associate( BotMcpServer botMcpServer, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_ASSOCIATE, plugin ) )
        {
            daoUtil.setInt( 1, botMcpServer.getBotId( ) );
            daoUtil.setInt( 2, botMcpServer.getMcpServerId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dissociate( BotMcpServer botMcpServer, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DISSOCIATE, plugin ) )
        {
            daoUtil.setInt( 1, botMcpServer.getBotId( ) );
            daoUtil.setInt( 2, botMcpServer.getMcpServerId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> selectMcpServerIdsByBotId( int nBotId, Plugin plugin )
    {
        return selectIds( SQL_QUERY_SELECT_SERVERS_BY_BOT, nBotId, plugin );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> selectBotIdsByMcpServerId( int nMcpServerId, Plugin plugin )
    {
        return selectIds( SQL_QUERY_SELECT_BOTS_BY_SERVER, nMcpServerId, plugin );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByBotId( int nBotId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_BOT, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByMcpServerId( int nMcpServerId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_SERVER, plugin ) )
        {
            daoUtil.setInt( 1, nMcpServerId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Generic single-int-parameter id selector
     *
     * @param strQuery
     *            SQL query selecting a single int column
     * @param nParam
     *            The int parameter
     * @param plugin
     *            The plugin
     * @return The list of ids
     */
    private List<Integer> selectIds( String strQuery, int nParam, Plugin plugin )
    {
        List<Integer> idList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( strQuery, plugin ) )
        {
            daoUtil.setInt( 1, nParam );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                idList.add( daoUtil.getInt( 1 ) );
            }
            return idList;
        }
    }
}
