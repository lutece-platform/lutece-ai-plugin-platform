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
import fr.paris.lutece.portal.service.plugin.Plugin;

/**
 * Interface for BotMcpServer DAO operations (N-N association between Bot and McpServer)
 */
public interface IBotMcpServerDAO
{

    /**
     * Associates a bot with an MCP server
     *
     * @param botMcpServer
     *            The association object
     * @param plugin
     *            The plugin
     */
    void associate( BotMcpServer botMcpServer, Plugin plugin );

    /**
     * Removes an association between a bot and an MCP server
     *
     * @param botMcpServer
     *            The association object
     * @param plugin
     *            The plugin
     */
    void dissociate( BotMcpServer botMcpServer, Plugin plugin );

    /**
     * Retrieves all MCP server ids associated with a specific bot
     *
     * @param nBotId
     *            The bot identifier
     * @param plugin
     *            The plugin
     * @return A list of MCP server identifiers
     */
    List<Integer> selectMcpServerIdsByBotId( int nBotId, Plugin plugin );

    /**
     * Retrieves all bot ids associated with a specific MCP server
     *
     * @param nMcpServerId
     *            The MCP server identifier
     * @param plugin
     *            The plugin
     * @return A list of bot identifiers
     */
    List<Integer> selectBotIdsByMcpServerId( int nMcpServerId, Plugin plugin );

    /**
     * Deletes all associations for a given bot
     *
     * @param nBotId
     *            The bot identifier
     * @param plugin
     *            The plugin
     */
    void deleteByBotId( int nBotId, Plugin plugin );

    /**
     * Deletes all associations for a given MCP server
     *
     * @param nMcpServerId
     *            The MCP server identifier
     * @param plugin
     *            The plugin
     */
    void deleteByMcpServerId( int nMcpServerId, Plugin plugin );
}
