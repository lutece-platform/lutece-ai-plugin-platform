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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.websocket.WebSocketMcpTransport;
import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpTransportTypeConstants;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Long-lived registry of MCP clients, keyed by McpServer id. MCP clients keep an internal ping loop so they must be reused across bot invocations rather than
 * rebuilt per request.
 */
@ApplicationScoped
@Named( "platform.mcpClientManager" )
public class McpClientManager
{
    private static final ObjectMapper MAPPER = new ObjectMapper( );
    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<Map<String, String>>( )
    {
    };

    private final Map<Integer, VersionedClient> _clientsById = new ConcurrentHashMap<>( );

    /**
     * Private constructor — singleton
     */
    McpClientManager( )
    {
    }

    /**
     * Cache entry pairing a client with the server version it was built from, so the stale-detection and the rebuild happen atomically inside a single map
     * compute.
     *
     * @param client
     *            the built MCP client
     * @param version
     *            the server's updatedAt epoch used as version
     */
    private record VersionedClient(McpClient client, long version) {
    }

    /**
     * Returns the cached MCP client for a server, building it on first access. The cached client is closed and rebuilt when the server's updatedAt changes
     * between calls; detection and rebuild are atomic per server id (single {@link ConcurrentHashMap#compute}).
     *
     * @param mcpServer
     *            The MCP server configuration
     * @return The MCP client, or null if construction failed
     */
    public McpClient getOrBuild( McpServer mcpServer )
    {
        if ( mcpServer == null || !mcpServer.isEnabled( ) )
        {
            return null;
        }
        long version = mcpServer.getUpdatedAt( ) != null ? mcpServer.getUpdatedAt( ).getTime( ) : 0L;
        VersionedClient entry = _clientsById.compute( mcpServer.getId( ), ( id, existing ) -> {
            if ( existing != null && existing.version( ) == version )
            {
                return existing;
            }
            if ( existing != null )
            {
                closeQuietly( existing.client( ) );
            }
            try
            {
                return new VersionedClient( build( mcpServer ), version );
            }
            catch( RuntimeException e )
            {
                AppLogService.error( "Failed to build MCP client for server {}", mcpServer.getName( ), e );
                return null;
            }
        } );
        return entry != null ? entry.client( ) : null;
    }

    /**
     * Closes and removes the cached client for a given server id
     *
     * @param mcpServerId
     *            The server id
     */
    public void invalidate( int mcpServerId )
    {
        VersionedClient removed = _clientsById.remove( mcpServerId );
        if ( removed != null )
        {
            closeQuietly( removed.client( ) );
        }
    }

    /**
     * Closes every cached MCP client at application shutdown so their internal ping loops and transport threads do not outlive the deployment.
     */
    @PreDestroy
    void shutdown( )
    {
        _clientsById.values( ).forEach( entry -> closeQuietly( entry.client( ) ) );
        _clientsById.clear( );
    }

    /**
     * Constructs a fresh MCP client for the given server configuration
     *
     * @param mcpServer
     *            The MCP server
     * @return The configured McpClient
     */
    private McpClient build( McpServer mcpServer )
    {
        McpTransport transport = buildTransport( mcpServer );
        Duration timeout = effectiveTimeout( mcpServer );
        return new DefaultMcpClient.Builder( ).key( mcpServer.getName( ) ).transport( transport ).toolExecutionTimeout( timeout ).build( );
    }

    /**
     * Resolves the configured timeout of a server, with a 60-second default. Applied both to the transport (connection) and to tool executions.
     *
     * @param mcpServer
     *            The MCP server
     * @return the effective timeout
     */
    private Duration effectiveTimeout( McpServer mcpServer )
    {
        return Duration.ofMillis( mcpServer.getTimeoutMs( ) > 0 ? mcpServer.getTimeoutMs( ) : 60000 );
    }

    /**
     * Builds the MCP transport according to the server's transport type
     *
     * @param mcpServer
     *            The MCP server
     * @return The configured McpTransport
     */
    @SuppressWarnings( "removal" )
    private McpTransport buildTransport( McpServer mcpServer )
    {
        Map<String, String> headers = parseHeaders( mcpServer.getHeadersJson( ) );
        Duration timeout = effectiveTimeout( mcpServer );
        String type = mcpServer.getTransportType( );
        if ( McpTransportTypeConstants.TRANSPORT_SSE.equals( type ) )
        {
            return new HttpMcpTransport.Builder( ).sseUrl( mcpServer.getUrl( ) ).customHeaders( headers ).timeout( timeout )
                    .logRequests( mcpServer.isLogRequests( ) ).logResponses( mcpServer.isLogRequests( ) ).build( );
        }
        if ( McpTransportTypeConstants.TRANSPORT_WEBSOCKET.equals( type ) )
        {
            Map<String, String> finalHeaders = headers;
            return new WebSocketMcpTransport.Builder( ).url( mcpServer.getUrl( ) ).headersSupplier( ( ) -> finalHeaders ).timeout( timeout )
                    .logRequests( mcpServer.isLogRequests( ) ).logResponses( mcpServer.isLogRequests( ) ).build( );
        }
        return new StreamableHttpMcpTransport.Builder( ).url( mcpServer.getUrl( ) ).customHeaders( headers ).timeout( timeout )
                .logRequests( mcpServer.isLogRequests( ) ).logResponses( mcpServer.isLogRequests( ) ).build( );
    }

    /**
     * Parses the JSON-encoded header map. Returns an empty map on null/blank input or parse error.
     *
     * @param json
     *            The JSON string
     * @return The headers map (never null)
     */
    private Map<String, String> parseHeaders( String json )
    {
        if ( json == null || json.trim( ).isEmpty( ) )
        {
            return Collections.emptyMap( );
        }
        try
        {
            Map<String, String> parsed = MAPPER.readValue( json, HEADERS_TYPE );
            return parsed != null ? parsed : Collections.emptyMap( );
        }
        catch( Exception e )
        {
            AppLogService.error( "Invalid MCP headers JSON, ignoring: {}", json, e );
            return new HashMap<>( );
        }
    }

    /**
     * Closes an MCP client without throwing
     *
     * @param client
     *            The client to close (nullable)
     */
    private void closeQuietly( McpClient client )
    {
        if ( client == null )
        {
            return;
        }
        try
        {
            client.close( );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error while closing MCP client", e );
        }
    }
}
