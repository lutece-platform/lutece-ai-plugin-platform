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
package fr.paris.lutece.plugins.platform.business.conversation;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Class representing a message within a conversation
 */
public class ConversationMessage implements Serializable
{
    /**
     * Role of a message authored by the end user.
     */
    public static final String ROLE_USER = "user";

    /**
     * Role of a message authored by the assistant (LLM).
     */
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * Role of a message carrying a tool execution result.
     */
    public static final String ROLE_TOOL = "tool";

    private static final long serialVersionUID = 1L;
    private int _nId;
    private int _nConversationId;
    private String _strMessage;
    private String _strRole;
    private Timestamp _timestampCreatedAt;

    /**
     * Gets the message ID
     *
     * @return The message ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the message ID
     *
     * @param nId
     *            The message ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the conversation ID
     *
     * @return The conversation ID
     */
    public int getConversationId( )
    {
        return _nConversationId;
    }

    /**
     * Sets the conversation ID
     *
     * @param nConversationId
     *            The conversation ID
     */
    public void setConversationId( int nConversationId )
    {
        _nConversationId = nConversationId;
    }

    /**
     * Gets the message content
     *
     * @return The message content
     */
    public String getMessage( )
    {
        return _strMessage;
    }

    /**
     * Sets the message content
     *
     * @param strMessage
     *            The message content
     */
    public void setMessage( String strMessage )
    {
        _strMessage = strMessage;
    }

    /**
     * Gets the role of the message sender
     *
     * @return The role
     */
    public String getRole( )
    {
        return _strRole;
    }

    /**
     * Sets the role of the message sender
     *
     * @param strRole
     *            The role
     */
    public void setRole( String strRole )
    {
        _strRole = strRole;
    }

    /**
     * Gets the creation timestamp
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
}
