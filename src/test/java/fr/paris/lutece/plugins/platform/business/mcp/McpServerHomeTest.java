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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for {@link McpServerHome} against the HSQL test database.
 */
public class McpServerHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full create / findByPrimaryKey / update / remove cycle on an MCP server.
     */
    @Test
    public void testCrud( )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( "Test MCP Server" );
        mcpServer.setDescription( "Description 1" );
        mcpServer.setTransportType( "streamable_http" );
        mcpServer.setUrl( "http://localhost:4010/mcp" );
        mcpServer.setHeadersJson( "{\"Authorization\":\"Bearer test\"}" );
        mcpServer.setTimeoutMs( 30000 );
        mcpServer.setEnabled( true );
        mcpServer.setToolFilter( "tool_a,tool_b" );
        mcpServer.setLogRequests( false );

        McpServerHome.create( mcpServer );
        assertTrue( mcpServer.getId( ) > 0 );

        Optional<McpServer> optStored = McpServerHome.findByPrimaryKey( mcpServer.getId( ) );
        assertTrue( optStored.isPresent( ) );
        McpServer stored = optStored.get( );
        assertEquals( mcpServer.getName( ), stored.getName( ) );
        assertEquals( mcpServer.getDescription( ), stored.getDescription( ) );
        assertEquals( mcpServer.getTransportType( ), stored.getTransportType( ) );
        assertEquals( mcpServer.getUrl( ), stored.getUrl( ) );
        assertEquals( mcpServer.getHeadersJson( ), stored.getHeadersJson( ) );
        assertEquals( mcpServer.getTimeoutMs( ), stored.getTimeoutMs( ) );
        assertEquals( mcpServer.isEnabled( ), stored.isEnabled( ) );
        assertEquals( mcpServer.getToolFilter( ), stored.getToolFilter( ) );
        assertEquals( mcpServer.isLogRequests( ), stored.isLogRequests( ) );

        mcpServer.setDescription( "Description 2" );
        mcpServer.setTimeoutMs( 45000 );
        mcpServer.setEnabled( false );
        McpServerHome.update( mcpServer );

        optStored = McpServerHome.findByPrimaryKey( mcpServer.getId( ) );
        assertTrue( optStored.isPresent( ) );
        stored = optStored.get( );
        assertEquals( "Description 2", stored.getDescription( ) );
        assertEquals( 45000, stored.getTimeoutMs( ) );
        assertFalse( stored.isEnabled( ) );

        McpServerHome.remove( mcpServer.getId( ) );
        assertFalse( McpServerHome.findByPrimaryKey( mcpServer.getId( ) ).isPresent( ) );
    }

    /**
     * Verifies the list accessors return the created server and exclude it once disabled or removed.
     */
    @Test
    public void testLists( )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( "Listed MCP Server" );
        mcpServer.setTransportType( "sse" );
        mcpServer.setUrl( "http://localhost:4010/sse" );
        mcpServer.setEnabled( true );

        McpServerHome.create( mcpServer );

        List<McpServer> allList = McpServerHome.getMcpServersList( );
        assertTrue( allList.stream( ).anyMatch( s -> s.getId( ) == mcpServer.getId( ) ) );

        McpServerHome.remove( mcpServer.getId( ) );

        allList = McpServerHome.getMcpServersList( );
        assertFalse( allList.stream( ).anyMatch( s -> s.getId( ) == mcpServer.getId( ) ) );
    }
}
