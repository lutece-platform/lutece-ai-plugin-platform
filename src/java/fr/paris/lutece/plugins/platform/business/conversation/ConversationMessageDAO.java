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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of IConversationMessageDAO
 */
@ApplicationScoped
@Named( "platform.conversationMessageDAO" )
public class ConversationMessageDAO implements IConversationMessageDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_conversation_message (conversation_id, message, role, created_at) VALUES (?, ?, ?, NOW())";
    private static final String SQL_QUERY_SELECT = "SELECT id, conversation_id, message, role, created_at FROM platform_conversation_message WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_CONVERSATION_ID = "SELECT id, conversation_id, message, role, created_at FROM platform_conversation_message WHERE conversation_id = ? ORDER BY created_at ASC";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_conversation_message WHERE id = ?";
    private static final String SQL_QUERY_DELETE_BY_CONVERSATION_ID = "DELETE FROM platform_conversation_message WHERE conversation_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert( ConversationMessage message, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, message.getConversationId( ) );
            daoUtil.setString( nIndex++, message.getMessage( ) );
            daoUtil.setString( nIndex, message.getRole( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                message.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
            return message.getId( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ConversationMessage> load( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                ConversationMessage message = new ConversationMessage( );
                int nIndex = 1;
                message.setId( daoUtil.getInt( nIndex++ ) );
                message.setConversationId( daoUtil.getInt( nIndex++ ) );
                message.setMessage( daoUtil.getString( nIndex++ ) );
                message.setRole( daoUtil.getString( nIndex++ ) );
                message.setCreatedAt( daoUtil.getTimestamp( nIndex ) );
                return Optional.of( message );
            }
            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConversationMessage> selectByConversationId( int nConversationId, Plugin plugin )
    {
        List<ConversationMessage> messageList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CONVERSATION_ID, plugin ) )
        {
            daoUtil.setInt( 1, nConversationId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ConversationMessage message = new ConversationMessage( );
                int nIndex = 1;
                message.setId( daoUtil.getInt( nIndex++ ) );
                message.setConversationId( daoUtil.getInt( nIndex++ ) );
                message.setMessage( daoUtil.getString( nIndex++ ) );
                message.setRole( daoUtil.getString( nIndex++ ) );
                message.setCreatedAt( daoUtil.getTimestamp( nIndex ) );
                messageList.add( message );
            }
        }
        return messageList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByConversationId( int nConversationId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_CONVERSATION_ID, plugin ) )
        {
            daoUtil.setInt( 1, nConversationId );
            daoUtil.executeUpdate( );
        }
    }
}
