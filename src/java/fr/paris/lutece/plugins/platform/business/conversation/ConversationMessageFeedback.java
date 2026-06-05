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
 * Class representing feedback for a conversation message
 */
public class ConversationMessageFeedback implements Serializable
{
    private static final long serialVersionUID = 1L;

    public enum Status
    {
        PENDING( "pending" ),
        PROCESSED( "processed" );

        private final String _strValue;

        Status( String strValue )
        {
            _strValue = strValue;
        }

        /**
         * Returns the string value of the status
         *
         * @return the string value of the status
         */
        public String getValue( )
        {
            return _strValue;
        }

        /**
         * Returns the status matching the given string value, defaulting to PENDING when no match is found
         *
         * @param value
         *            the string value to match
         * @return the matching status, or PENDING if none matches
         */
        public static Status fromValue( String value )
        {
            for ( Status status : Status.values( ) )
            {
                if ( status.getValue( ).equals( value ) )
                {
                    return status;
                }
            }
            return PENDING;
        }
    }

    private int _nId;
    private int _nMessageId;
    private String _strUserId;
    private int _nBotId;
    private int _nClientId;
    private boolean _bIsPositive;
    private String _strComment;
    private Status _status = Status.PENDING;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;
    private String _strConversationUuid;

    /**
     * Gets the feedback ID
     *
     * @return The feedback ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the feedback ID
     *
     * @param nId
     *            The feedback ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the message ID
     *
     * @return The message ID
     */
    public int getMessageId( )
    {
        return _nMessageId;
    }

    /**
     * Sets the message ID
     *
     * @param nMessageId
     *            The message ID
     */
    public void setMessageId( int nMessageId )
    {
        _nMessageId = nMessageId;
    }

    /**
     * Gets the user ID
     *
     * @return The user ID
     */
    public String getUserId( )
    {
        return _strUserId;
    }

    /**
     * Sets the user ID
     *
     * @param strUserId
     *            The user ID
     */
    public void setUserId( String strUserId )
    {
        _strUserId = strUserId;
    }

    /**
     * Gets the bot ID
     *
     * @return The bot ID
     */
    public int getBotId( )
    {
        return _nBotId;
    }

    /**
     * Sets the bot ID
     *
     * @param nBotId
     *            The bot ID
     */
    public void setBotId( int nBotId )
    {
        _nBotId = nBotId;
    }

    /**
     * Gets the client ID
     *
     * @return The client ID
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Sets the client ID
     *
     * @param nClientId
     *            The client ID
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Gets whether the feedback is positive
     *
     * @return true if positive, false if negative
     */
    public boolean isPositive( )
    {
        return _bIsPositive;
    }

    /**
     * Sets whether the feedback is positive
     *
     * @param bIsPositive
     *            true if positive, false if negative
     */
    public void setIsPositive( boolean bIsPositive )
    {
        _bIsPositive = bIsPositive;
    }

    /**
     * Gets the feedback comment
     *
     * @return The feedback comment
     */
    public String getComment( )
    {
        return _strComment;
    }

    /**
     * Sets the feedback comment
     *
     * @param strComment
     *            The feedback comment
     */
    public void setComment( String strComment )
    {
        _strComment = strComment;
    }

    /**
     * Gets the feedback status
     *
     * @return The feedback status
     */
    public Status getStatus( )
    {
        return _status;
    }

    /**
     * Sets the feedback status
     *
     * @param status
     *            The feedback status
     */
    public void setStatus( Status status )
    {
        _status = status;
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

    /**
     * Gets the update timestamp
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

    /**
     * Gets the conversation UUID
     *
     * @return The conversation UUID
     */
    public String getConversationUuid( )
    {
        return _strConversationUuid;
    }

    /**
     * Sets the conversation UUID
     *
     * @param strConversationUuid
     *            The conversation UUID
     */
    public void setConversationUuid( String strConversationUuid )
    {
        _strConversationUuid = strConversationUuid;
    }
}
