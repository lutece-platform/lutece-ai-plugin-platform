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

/**
 * Association between a Bot and an MCP server
 */
public class BotMcpServer implements Serializable
{
    private static final long serialVersionUID = 1L;
    private int _nBotId;
    private int _nMcpServerId;

    /**
     * Default constructor
     */
    public BotMcpServer( )
    {
    }

    /**
     * Constructor with parameters
     *
     * @param nBotId
     *            The bot identifier
     * @param nMcpServerId
     *            The MCP server identifier
     */
    public BotMcpServer( int nBotId, int nMcpServerId )
    {
        _nBotId = nBotId;
        _nMcpServerId = nMcpServerId;
    }

    /**
     * Gets the bot identifier
     *
     * @return The bot identifier
     */
    public int getBotId( )
    {
        return _nBotId;
    }

    /**
     * Sets the bot identifier
     *
     * @param nBotId
     *            The bot identifier
     */
    public void setBotId( int nBotId )
    {
        _nBotId = nBotId;
    }

    /**
     * Gets the MCP server identifier
     *
     * @return The MCP server identifier
     */
    public int getMcpServerId( )
    {
        return _nMcpServerId;
    }

    /**
     * Sets the MCP server identifier
     *
     * @param nMcpServerId
     *            The MCP server identifier
     */
    public void setMcpServerId( int nMcpServerId )
    {
        _nMcpServerId = nMcpServerId;
    }
}
