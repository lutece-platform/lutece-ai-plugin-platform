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

import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpTransportTypeConstants;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Business service for MCP server configuration. Owns the MCP domain rules (supported transport types) and keeps the persisted configuration and the live MCP
 * client cache consistent: every mutation that changes a server's configuration drops the stale live client in {@link McpClientManager}.
 */
@ApplicationScoped
@Named( "platform.mcpServerService" )
public class McpServerService
{
    @Inject
    private McpClientManager _mcpClientManager;

    /**
     * Creates a new MCP server after validating its transport type.
     *
     * @param mcpServer
     *            The MCP server to create (already populated from the request)
     * @return The created MCP server
     * @throws InvalidRequestException
     *             if the transport type is not a supported MCP transport
     */
    public McpServer createServer( McpServer mcpServer ) throws InvalidRequestException
    {
        validateTransport( mcpServer.getTransportType( ) );
        return McpServerHome.create( mcpServer );
    }

    /**
     * Applies the given configuration to the MCP server identified by the supplied id, validates its transport type, persists the change and invalidates the
     * live client so it is rebuilt with the new configuration.
     *
     * @param id
     *            The identifier of the MCP server to update
     * @param mcpServer
     *            The MCP server carrying the new configuration
     * @return The updated MCP server
     * @throws ResourceNotFoundException
     *             if no MCP server exists for the given id
     * @throws InvalidRequestException
     *             if the transport type is not a supported MCP transport
     */
    public McpServer updateServer( int id, McpServer mcpServer ) throws ResourceNotFoundException, InvalidRequestException
    {
        if ( McpServerHome.findByPrimaryKey( id ).isEmpty( ) )
        {
            throw new ResourceNotFoundException( );
        }
        validateTransport( mcpServer.getTransportType( ) );
        mcpServer.setId( id );
        McpServer updated = McpServerHome.update( mcpServer );
        _mcpClientManager.invalidate( id );
        return updated;
    }

    /**
     * Removes the MCP server identified by the given id and invalidates the live client so no stale connection survives the deletion.
     *
     * @param id
     *            The identifier of the MCP server to remove
     */
    public void deleteServer( int id )
    {
        McpServerHome.remove( id );
        _mcpClientManager.invalidate( id );
    }

    /**
     * Validates that the given transport type is one of the supported MCP transport constants.
     *
     * @param transportType
     *            The transport type to validate
     * @throws InvalidRequestException
     *             if the transport type is not supported
     */
    private void validateTransport( String transportType ) throws InvalidRequestException
    {
        if ( !McpTransportTypeConstants.isSupported( transportType ) )
        {
            throw new InvalidRequestException( "Unsupported MCP transport type: " + transportType );
        }
    }
}
