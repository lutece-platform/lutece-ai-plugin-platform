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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * JDBC implementation of IMcpServerDAO
 */
@ApplicationScoped
@Named( "platform.mcpServerDAO" )
public class McpServerDAO implements IMcpServerDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_mcp_server (mcp_server_name, mcp_server_description, transport_type, url, headers, timeout_ms, enabled, tool_filter, log_requests) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_mcp_server WHERE mcp_server_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_mcp_server SET mcp_server_name = ?, mcp_server_description = ?, transport_type = ?, url = ?, headers = ?, timeout_ms = ?, enabled = ?, tool_filter = ?, log_requests = ?, updated_at = CURRENT_TIMESTAMP WHERE mcp_server_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT mcp_server_id, mcp_server_name, mcp_server_description, transport_type, url, headers, timeout_ms, enabled, tool_filter, log_requests, created_at, updated_at FROM platform_mcp_server";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE mcp_server_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( McpServer mcpServer, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, mcpServer.getName( ) );
            daoUtil.setString( nIndex++, mcpServer.getDescription( ) );
            daoUtil.setString( nIndex++, mcpServer.getTransportType( ) );
            daoUtil.setString( nIndex++, mcpServer.getUrl( ) );
            daoUtil.setString( nIndex++, mcpServer.getHeadersJson( ) );
            daoUtil.setInt( nIndex++, mcpServer.getTimeoutMs( ) );
            daoUtil.setBoolean( nIndex++, mcpServer.isEnabled( ) );
            daoUtil.setString( nIndex++, mcpServer.getToolFilter( ) );
            daoUtil.setBoolean( nIndex++, mcpServer.isLogRequests( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                mcpServer.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<McpServer> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            McpServer mcpServer = null;
            if ( daoUtil.next( ) )
            {
                mcpServer = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( mcpServer );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( McpServer mcpServer, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, mcpServer.getName( ) );
            daoUtil.setString( nIndex++, mcpServer.getDescription( ) );
            daoUtil.setString( nIndex++, mcpServer.getTransportType( ) );
            daoUtil.setString( nIndex++, mcpServer.getUrl( ) );
            daoUtil.setString( nIndex++, mcpServer.getHeadersJson( ) );
            daoUtil.setInt( nIndex++, mcpServer.getTimeoutMs( ) );
            daoUtil.setBoolean( nIndex++, mcpServer.isEnabled( ) );
            daoUtil.setString( nIndex++, mcpServer.getToolFilter( ) );
            daoUtil.setBoolean( nIndex++, mcpServer.isLogRequests( ) );
            daoUtil.setInt( nIndex, mcpServer.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<McpServer> selectMcpServersList( Plugin plugin )
    {
        return selectAll( SQL_QUERY_SELECTALL, plugin );
    }

    /**
     * Executes a parameterless SELECT query and returns the resulting list of MCP servers
     *
     * @param strQuery
     *            The SQL query
     * @param plugin
     *            The plugin
     * @return The list of MCP servers
     */
    private List<McpServer> selectAll( String strQuery, Plugin plugin )
    {
        List<McpServer> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( strQuery, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
            return list;
        }
    }

    /**
     * Builds an McpServer from the current row of the DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil cursor
     * @return The MCP server
     */
    private McpServer loadFromDaoUtil( DAOUtil daoUtil )
    {
        McpServer mcpServer = new McpServer( );
        int nIndex = 1;
        mcpServer.setId( daoUtil.getInt( nIndex++ ) );
        mcpServer.setName( daoUtil.getString( nIndex++ ) );
        mcpServer.setDescription( daoUtil.getString( nIndex++ ) );
        mcpServer.setTransportType( daoUtil.getString( nIndex++ ) );
        mcpServer.setUrl( daoUtil.getString( nIndex++ ) );
        mcpServer.setHeadersJson( daoUtil.getString( nIndex++ ) );
        mcpServer.setTimeoutMs( daoUtil.getInt( nIndex++ ) );
        mcpServer.setEnabled( daoUtil.getBoolean( nIndex++ ) );
        mcpServer.setToolFilter( daoUtil.getString( nIndex++ ) );
        mcpServer.setLogRequests( daoUtil.getBoolean( nIndex++ ) );
        mcpServer.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        mcpServer.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return mcpServer;
    }
}
