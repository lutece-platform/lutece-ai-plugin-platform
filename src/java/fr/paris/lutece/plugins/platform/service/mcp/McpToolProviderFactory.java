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
package fr.paris.lutece.plugins.platform.service.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolProvider;
import fr.paris.lutece.plugins.platform.business.mcp.BotMcpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds an aggregate McpToolProvider from the MCP servers attached to a given bot. Tool names are prefixed by the server's logical key to avoid collisions
 * across servers, and an optional CSV whitelist on the server entity restricts which tools are exposed.
 */
public final class McpToolProviderFactory
{
    private static final String NAME_SEPARATOR = "_";
    private static final String CSV_SEPARATOR = "\\s*,\\s*";

    /**
     * Private constructor — static factory only
     */
    private McpToolProviderFactory( )
    {
    }

    /**
     * Builds an MCP tool provider for the given bot, or null if the bot has no enabled MCP servers
     *
     * @param nBotId
     *            The bot id
     * @return The MCP tool provider, or null if no MCP server is attached / enabled
     */
    public static ToolProvider buildForBot( int nBotId )
    {
        List<McpServer> servers = collectEnabledServersForBot( nBotId );
        List<McpClient> clients = new ArrayList<>( );
        Map<String, Set<String>> allowedToolsByKey = new HashMap<>( );
        McpClientManager clientManager = CDI.current( ).select( McpClientManager.class ).get( );
        for ( McpServer server : servers )
        {
            McpClient client = clientManager.getOrBuild( server );
            if ( client == null )
            {
                AppLogService.error( "MCP client unavailable for server {} (bot {})", server.getName( ), nBotId );
                continue;
            }
            clients.add( client );
            allowedToolsByKey.put( server.getName( ), parseToolFilter( server.getToolFilter( ) ) );
        }
        if ( clients.isEmpty( ) )
        {
            return null;
        }
        return McpToolProvider.builder( ).mcpClients( clients ).filter( ( client, tool ) -> isToolAllowed( allowedToolsByKey, client.key( ), tool.name( ) ) )
                .toolNameMapper( ( client, tool ) -> {
                    String key = client.key( );
                    return key != null && !key.isEmpty( ) ? key + NAME_SEPARATOR + tool.name( ) : tool.name( );
                } ).build( );
    }

    /**
     * Resolves the enabled MCP servers tied to a bot
     *
     * @param nBotId
     *            The bot id
     * @return A non-null list of enabled MCP servers (possibly empty)
     */
    private static List<McpServer> collectEnabledServersForBot( int nBotId )
    {
        List<Integer> serverIds = BotMcpServerHome.getMcpServerIdsByBotId( nBotId );
        List<McpServer> servers = new ArrayList<>( );
        for ( Integer serverId : serverIds )
        {
            McpServerHome.findByPrimaryKey( serverId ).filter( McpServer::isEnabled ).ifPresent( servers::add );
        }
        return servers;
    }

    /**
     * Parses the CSV tool whitelist into a set. Returns null when the filter is null/blank to signal "all tools allowed".
     *
     * @param csv
     *            The CSV string (nullable)
     * @return The parsed allow-set, or null when no filter applies
     */
    private static Set<String> parseToolFilter( String csv )
    {
        if ( csv == null || csv.trim( ).isEmpty( ) )
        {
            return null;
        }
        return Collections.unmodifiableSet( new HashSet<>( Arrays.asList( csv.split( CSV_SEPARATOR ) ) ) );
    }

    /**
     * Tests whether a tool is allowed for the server identified by its key. A null entry in the map (or a missing key) means "all tools allowed".
     *
     * @param allowedToolsByKey
     *            The map of allow-sets keyed by server name
     * @param clientKey
     *            The MCP client's logical key (server name)
     * @param toolName
     *            The unprefixed tool name
     * @return true if the tool is allowed
     */
    private static boolean isToolAllowed( Map<String, Set<String>> allowedToolsByKey, String clientKey, String toolName )
    {
        if ( clientKey == null )
        {
            return true;
        }
        Set<String> allowed = allowedToolsByKey.get( clientKey );
        return allowed == null || allowed.contains( toolName );
    }
}
