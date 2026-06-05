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

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.validation.constraints.NotEmpty;

/**
 * MCP server configuration for tool integration via Model Context Protocol
 */
public class McpServer implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;

    @NotEmpty( message = "#i18n{platform.agent.validation.mcpServer.name.notEmpty}" )
    private String _strName;

    private String _strDescription;

    @NotEmpty( message = "#i18n{platform.agent.validation.mcpServer.transportType.notEmpty}" )
    private String _strTransportType;

    @NotEmpty( message = "#i18n{platform.agent.validation.mcpServer.url.notEmpty}" )
    private String _strUrl;

    private String _strHeadersJson;
    private int _nTimeoutMs = 60000;
    private boolean _bEnabled = true;
    private String _strToolFilter;
    private boolean _bLogRequests = false;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    /**
     * Returns the MCP server ID
     *
     * @return The MCP server ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the MCP server ID
     *
     * @param nId
     *            The MCP server ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the MCP server name
     *
     * @return The MCP server name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the MCP server name
     *
     * @param strName
     *            The MCP server name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the MCP server description
     *
     * @return The MCP server description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the MCP server description
     *
     * @param strDescription
     *            The MCP server description
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the transport type (streamable_http, sse, websocket)
     *
     * @return The transport type
     */
    public String getTransportType( )
    {
        return _strTransportType;
    }

    /**
     * Sets the transport type
     *
     * @param strTransportType
     *            The transport type
     */
    public void setTransportType( String strTransportType )
    {
        _strTransportType = strTransportType;
    }

    /**
     * Returns the main URL of the MCP server
     *
     * @return The URL
     */
    public String getUrl( )
    {
        return _strUrl;
    }

    /**
     * Sets the main URL of the MCP server
     *
     * @param strUrl
     *            The URL
     */
    public void setUrl( String strUrl )
    {
        _strUrl = strUrl;
    }

    /**
     * Returns the JSON-serialized headers map (e.g. {"Authorization":"Bearer xxx"})
     *
     * @return The headers JSON
     */
    public String getHeadersJson( )
    {
        return _strHeadersJson;
    }

    /**
     * Sets the JSON-serialized headers map
     *
     * @param strHeadersJson
     *            The headers JSON
     */
    public void setHeadersJson( String strHeadersJson )
    {
        _strHeadersJson = strHeadersJson;
    }

    /**
     * Returns the timeout in milliseconds for MCP requests
     *
     * @return The timeout in milliseconds
     */
    public int getTimeoutMs( )
    {
        return _nTimeoutMs;
    }

    /**
     * Sets the timeout in milliseconds
     *
     * @param nTimeoutMs
     *            The timeout in milliseconds
     */
    public void setTimeoutMs( int nTimeoutMs )
    {
        _nTimeoutMs = nTimeoutMs;
    }

    /**
     * Returns whether this MCP server is enabled
     *
     * @return true if enabled
     */
    public boolean isEnabled( )
    {
        return _bEnabled;
    }

    /**
     * Sets whether this MCP server is enabled
     *
     * @param bEnabled
     *            true to enable
     */
    public void setEnabled( boolean bEnabled )
    {
        _bEnabled = bEnabled;
    }

    /**
     * Returns the optional tool name whitelist (CSV). Null/empty means "all tools allowed".
     *
     * @return The tool filter CSV
     */
    public String getToolFilter( )
    {
        return _strToolFilter;
    }

    /**
     * Sets the optional tool name whitelist (CSV)
     *
     * @param strToolFilter
     *            The tool filter CSV
     */
    public void setToolFilter( String strToolFilter )
    {
        _strToolFilter = strToolFilter;
    }

    /**
     * Returns whether request/response logging is enabled
     *
     * @return true if log requests is enabled
     */
    public boolean isLogRequests( )
    {
        return _bLogRequests;
    }

    /**
     * Sets whether request/response logging is enabled
     *
     * @param bLogRequests
     *            true to enable logging
     */
    public void setLogRequests( boolean bLogRequests )
    {
        _bLogRequests = bLogRequests;
    }

    /**
     * Returns the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }
}
