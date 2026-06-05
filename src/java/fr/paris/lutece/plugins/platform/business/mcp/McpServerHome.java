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
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Static facade for managing McpServer entities
 */
public final class McpServerHome
{
    private static IMcpServerDAO _dao = CDI.current( ).select( IMcpServerDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor — facade only
     */
    private McpServerHome( )
    {
    }

    /**
     * Creates a new MCP server
     *
     * @param mcpServer
     *            The MCP server to create
     * @return The created MCP server
     */
    public static McpServer create( McpServer mcpServer )
    {
        _dao.insert( mcpServer, _plugin );
        return mcpServer;
    }

    /**
     * Updates an MCP server
     *
     * @param mcpServer
     *            The MCP server to update
     * @return The updated MCP server
     */
    public static McpServer update( McpServer mcpServer )
    {
        _dao.store( mcpServer, _plugin );
        return mcpServer;
    }

    /**
     * Removes an MCP server by ID
     *
     * @param nKey
     *            The MCP server identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Loads an MCP server by ID
     *
     * @param nKey
     *            The MCP server identifier
     * @return An Optional containing the MCP server, or empty if not found
     */
    public static Optional<McpServer> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of all MCP servers
     *
     * @return The list of MCP servers
     */
    public static List<McpServer> getMcpServersList( )
    {
        return _dao.selectMcpServersList( _plugin );
    }

}
