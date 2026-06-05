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
package fr.paris.lutece.plugins.platform.business.bot;

import java.io.Serializable;
import java.sql.Timestamp;

public class BotConversation implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nBotId;
    private String _strConversationUuid;
    private String _strUserId;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    /**
     * Returns the ID of the conversation
     *
     * @return The conversation ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the ID of the conversation
     *
     * @param nId
     *            The conversation ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the bot ID associated with this conversation
     *
     * @return The bot ID
     */
    public int getBotId( )
    {
        return _nBotId;
    }

    /**
     * Sets the bot ID for this conversation
     *
     * @param nBotId
     *            The bot ID
     */
    public void setBotId( int nBotId )
    {
        _nBotId = nBotId;
    }

    /**
     * Returns the UUID of the conversation
     *
     * @return The conversation UUID
     */
    public String getConversationUuid( )
    {
        return _strConversationUuid;
    }

    /**
     * Sets the UUID of the conversation
     *
     * @param strConversationUuid
     *            The conversation UUID
     */
    public void setConversationUuid( String strConversationUuid )
    {
        _strConversationUuid = strConversationUuid;
    }

    /**
     * Returns the creation timestamp of the conversation
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp of the conversation
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the last update timestamp of the conversation
     *
     * @return The last update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the last update timestamp of the conversation
     *
     * @param timestampUpdatedAt
     *            The last update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Returns the user ID associated with this conversation
     *
     * @return The user ID
     */
    public String getUserId( )
    {
        return _strUserId;
    }

    /**
     * Sets the user ID for this conversation
     *
     * @param strUserId
     *            The user ID
     */
    public void setUserId( String strUserId )
    {
        _strUserId = strUserId;
    }
}
