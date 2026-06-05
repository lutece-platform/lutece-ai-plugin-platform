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
 * Implementation of IConversationMessageFeedbackDAO
 */
@ApplicationScoped
@Named( "platform.conversationMessageFeedbackDAO" )
public class ConversationMessageFeedbackDAO implements IConversationMessageFeedbackDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_conversation_message_feedback (message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
    private static final String SQL_QUERY_SELECT = "SELECT id, message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at FROM platform_conversation_message_feedback WHERE id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_conversation_message_feedback SET message_id = ?, user_id = ?, bot_id = ?, client_id = ?, is_positive = ?, comment = ?, status = ?, updated_at = NOW() WHERE id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_conversation_message_feedback WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_MESSAGE_ID = "SELECT id, message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at FROM platform_conversation_message_feedback WHERE message_id = ?";
    private static final String SQL_QUERY_SELECT_BY_BOT_ID = "SELECT id, message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at FROM platform_conversation_message_feedback WHERE bot_id = ? ORDER BY created_at DESC";
    private static final String SQL_QUERY_SELECT_BY_BOT_ID_AND_STATUS = "SELECT id, message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at FROM platform_conversation_message_feedback WHERE bot_id = ? AND status = ? ORDER BY created_at DESC";
    private static final String SQL_QUERY_SELECT_BY_USER_ID = "SELECT id, message_id, user_id, bot_id, client_id, is_positive, comment, status, created_at, updated_at FROM platform_conversation_message_feedback WHERE user_id = ? ORDER BY created_at DESC";
    private static final String SQL_QUERY_DELETE_BY_MESSAGE_ID = "DELETE FROM platform_conversation_message_feedback WHERE message_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert( ConversationMessageFeedback feedback, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, feedback.getMessageId( ) );
            daoUtil.setString( nIndex++, feedback.getUserId( ) );
            daoUtil.setInt( nIndex++, feedback.getBotId( ) );
            daoUtil.setInt( nIndex++, feedback.getClientId( ) );
            daoUtil.setBoolean( nIndex++, feedback.isPositive( ) );
            daoUtil.setString( nIndex++, feedback.getComment( ) );
            daoUtil.setString( nIndex, feedback.getStatus( ).getValue( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                feedback.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
            return feedback.getId( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ConversationMessageFeedback> load( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                ConversationMessageFeedback feedback = mapRow( daoUtil );
                return Optional.of( feedback );
            }
            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( ConversationMessageFeedback feedback, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, feedback.getMessageId( ) );
            daoUtil.setString( nIndex++, feedback.getUserId( ) );
            daoUtil.setInt( nIndex++, feedback.getBotId( ) );
            daoUtil.setInt( nIndex++, feedback.getClientId( ) );
            daoUtil.setBoolean( nIndex++, feedback.isPositive( ) );
            daoUtil.setString( nIndex++, feedback.getComment( ) );
            daoUtil.setString( nIndex++, feedback.getStatus( ).getValue( ) );
            daoUtil.setInt( nIndex, feedback.getId( ) );
            daoUtil.executeUpdate( );
        }
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
    public Optional<ConversationMessageFeedback> findByMessageId( int nMessageId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_MESSAGE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nMessageId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                ConversationMessageFeedback feedback = mapRow( daoUtil );
                return Optional.of( feedback );
            }
            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConversationMessageFeedback> findByBotId( int nBotId, Plugin plugin )
    {
        List<ConversationMessageFeedback> feedbackList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_BOT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ConversationMessageFeedback feedback = mapRow( daoUtil );
                feedbackList.add( feedback );
            }
        }
        return feedbackList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConversationMessageFeedback> findByBotIdAndStatus( int nBotId, ConversationMessageFeedback.Status status, Plugin plugin )
    {
        List<ConversationMessageFeedback> feedbackList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_BOT_ID_AND_STATUS, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.setString( 2, status.getValue( ) );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ConversationMessageFeedback feedback = mapRow( daoUtil );
                feedbackList.add( feedback );
            }
        }
        return feedbackList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConversationMessageFeedback> findByUserId( String strUserId, Plugin plugin )
    {
        List<ConversationMessageFeedback> feedbackList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_USER_ID, plugin ) )
        {
            daoUtil.setString( 1, strUserId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ConversationMessageFeedback feedback = mapRow( daoUtil );
                feedbackList.add( feedback );
            }
        }
        return feedbackList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByMessageId( int nMessageId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_MESSAGE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nMessageId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Builds a feedback entity from the current row of the given DAOUtil.
     *
     * @param daoUtil
     *            The DAOUtil positioned on the current row
     * @return The feedback built from the current row
     */
    private ConversationMessageFeedback mapRow( DAOUtil daoUtil )
    {
        ConversationMessageFeedback feedback = new ConversationMessageFeedback( );
        int nIndex = 1;
        feedback.setId( daoUtil.getInt( nIndex++ ) );
        feedback.setMessageId( daoUtil.getInt( nIndex++ ) );
        feedback.setUserId( daoUtil.getString( nIndex++ ) );
        feedback.setBotId( daoUtil.getInt( nIndex++ ) );
        feedback.setClientId( daoUtil.getInt( nIndex++ ) );
        feedback.setIsPositive( daoUtil.getBoolean( nIndex++ ) );
        feedback.setComment( daoUtil.getString( nIndex++ ) );
        feedback.setStatus( ConversationMessageFeedback.Status.fromValue( daoUtil.getString( nIndex++ ) ) );
        feedback.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        feedback.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return feedback;
    }
}
