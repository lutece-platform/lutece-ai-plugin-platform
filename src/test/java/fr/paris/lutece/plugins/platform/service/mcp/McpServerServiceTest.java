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

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpTransportTypeConstants;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link McpServerService}. These exercise the MCP server business rules against HSQL — supported-transport validation that gates every
 * mutating create/update, the not-found contract on update, and the create/update/delete persistence cycle against {@link McpServerHome} — together with the
 * {@link McpClientManager} invalidation side-effect that keeps the live client cache consistent with the persisted configuration.
 */
public class McpServerServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed MCP server service.
     *
     * @return the MCP server service instance
     */
    private McpServerService service( )
    {
        return CDI.current( ).select( McpServerService.class ).get( );
    }

    /**
     * Resolves the CDI-managed MCP client manager.
     *
     * @return the MCP client manager instance
     */
    private McpClientManager clientManager( )
    {
        return CDI.current( ).select( McpClientManager.class ).get( );
    }

    /**
     * Builds an in-memory MCP server populated with valid configuration, without persisting it.
     *
     * @param name
     *            the server name
     * @param transportType
     *            the transport type
     * @return the populated, non-persisted MCP server
     */
    private McpServer newServer( String name, String transportType )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( name );
        mcpServer.setTransportType( transportType );
        mcpServer.setUrl( "http://localhost:4010/mcp" );
        mcpServer.setEnabled( true );
        return mcpServer;
    }

    /**
     * createServer persists a server when the transport type is one of the supported constants, assigning it a generated id.
     */
    @Test
    public void testCreateServerWithSupportedTransportPersists( ) throws InvalidRequestException
    {
        McpServer created = service( ).createServer( newServer( "mcp-svc-create", McpTransportTypeConstants.TRANSPORT_STREAMABLE_HTTP ) );

        assertTrue( created.getId( ) > 0 );
        Optional<McpServer> stored = McpServerHome.findByPrimaryKey( created.getId( ) );
        assertTrue( stored.isPresent( ) );
        assertEquals( McpTransportTypeConstants.TRANSPORT_STREAMABLE_HTTP, stored.get( ).getTransportType( ) );
    }

    /**
     * createServer accepts every transport constant in the supported set.
     */
    @Test
    public void testCreateServerAcceptsAllSupportedTransports( ) throws InvalidRequestException
    {
        for ( String transport : McpTransportTypeConstants.SUPPORTED_TRANSPORT_TYPES )
        {
            McpServer created = service( ).createServer( newServer( "mcp-svc-" + transport, transport ) );
            assertTrue( created.getId( ) > 0, "transport " + transport + " should be accepted" );
            McpServerHome.remove( created.getId( ) );
        }
    }

    /**
     * createServer rejects an unsupported transport type with the typed {@link InvalidRequestException}, and does not persist anything.
     */
    @Test
    public void testCreateServerRejectsUnsupportedTransport( )
    {
        McpServerService svc = service( );
        McpServer rejected = newServer( "mcp-svc-bad", "stdio" );

        assertThrows( InvalidRequestException.class, ( ) -> svc.createServer( rejected ) );
        assertEquals( 0, rejected.getId( ) );
    }

    /**
     * createServer rejects a null transport type with the typed {@link InvalidRequestException}.
     */
    @Test
    public void testCreateServerRejectsNullTransport( )
    {
        McpServerService svc = service( );
        McpServer rejected = newServer( "mcp-svc-null", null );

        assertThrows( InvalidRequestException.class, ( ) -> svc.createServer( rejected ) );
    }

    /**
     * updateServer applies the new configuration to an existing server and persists it.
     */
    @Test
    public void testUpdateServerPersistsNewConfiguration( ) throws InvalidRequestException, ResourceNotFoundException
    {
        McpServer created = service( ).createServer( newServer( "mcp-svc-update", McpTransportTypeConstants.TRANSPORT_SSE ) );

        McpServer changes = newServer( "mcp-svc-update-renamed", McpTransportTypeConstants.TRANSPORT_WEBSOCKET );
        changes.setEnabled( false );
        McpServer updated = service( ).updateServer( created.getId( ), changes );

        assertEquals( created.getId( ), updated.getId( ) );
        McpServer stored = McpServerHome.findByPrimaryKey( created.getId( ) ).get( );
        assertEquals( "mcp-svc-update-renamed", stored.getName( ) );
        assertEquals( McpTransportTypeConstants.TRANSPORT_WEBSOCKET, stored.getTransportType( ) );
        assertFalse( stored.isEnabled( ) );
    }

    /**
     * updateServer throws the typed {@link ResourceNotFoundException} when no server exists for the given id.
     */
    @Test
    public void testUpdateServerUnknownIdThrowsNotFound( )
    {
        McpServerService svc = service( );
        McpServer changes = newServer( "mcp-svc-missing", McpTransportTypeConstants.TRANSPORT_STREAMABLE_HTTP );

        assertThrows( ResourceNotFoundException.class, ( ) -> svc.updateServer( 999999, changes ) );
    }

    /**
     * updateServer validates the transport type before persisting: an unsupported transport on an existing server is rejected and leaves the stored
     * configuration untouched.
     */
    @Test
    public void testUpdateServerRejectsUnsupportedTransport( ) throws InvalidRequestException
    {
        McpServer created = service( ).createServer( newServer( "mcp-svc-update-bad", McpTransportTypeConstants.TRANSPORT_SSE ) );
        McpServerService svc = service( );
        int id = created.getId( );
        McpServer changes = newServer( "mcp-svc-update-bad-renamed", "grpc" );

        assertThrows( InvalidRequestException.class, ( ) -> svc.updateServer( id, changes ) );

        McpServer stored = McpServerHome.findByPrimaryKey( id ).get( );
        assertEquals( "mcp-svc-update-bad", stored.getName( ) );
        assertEquals( McpTransportTypeConstants.TRANSPORT_SSE, stored.getTransportType( ) );
    }

    /**
     * deleteServer removes the persisted server, and the client manager invalidation it triggers is a no-op-safe operation even for a server with no live
     * client.
     */
    @Test
    public void testDeleteServerRemovesPersistedRow( ) throws InvalidRequestException
    {
        McpServer created = service( ).createServer( newServer( "mcp-svc-delete", McpTransportTypeConstants.TRANSPORT_STREAMABLE_HTTP ) );
        int id = created.getId( );

        service( ).deleteServer( id );

        assertFalse( McpServerHome.findByPrimaryKey( id ).isPresent( ) );
    }

    /**
     * The client manager invalidate side-effect invoked by update and delete is safe to call directly in SE for an id that was never built into a live client.
     */
    @Test
    public void testClientManagerInvalidateIsSafeForUnknownId( )
    {
        assertDoesNotThrow( ( ) -> clientManager( ).invalidate( 888888 ) );
    }
}
