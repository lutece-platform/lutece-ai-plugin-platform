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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Named( "platform.botConversationDAO" )
public class BotConversationDAO implements IBotConversationDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_bot_conversation (bot_id, conversation_uuid, user_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_bot_conversation SET bot_id = ?, conversation_uuid = ?, user_id = ?, updated_at = NOW() WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, bot_id, conversation_uuid, user_id, created_at, updated_at FROM platform_bot_conversation WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_UUID = "SELECT id, bot_id, conversation_uuid, user_id, created_at, updated_at FROM platform_bot_conversation WHERE conversation_uuid = ?";
    private static final String SQL_QUERY_SELECT_BY_BOT_ID = "SELECT id, bot_id, conversation_uuid, user_id, created_at, updated_at FROM platform_bot_conversation WHERE bot_id = ? ORDER BY updated_at DESC";
    private static final String SQL_QUERY_SELECT_BY_USER_ID = "SELECT id, bot_id, conversation_uuid, user_id, created_at, updated_at FROM platform_bot_conversation WHERE user_id = ? ORDER BY updated_at DESC";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_bot_conversation WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_BOT_ID_AND_USER_ID = "SELECT id, bot_id, conversation_uuid, user_id, created_at, updated_at "
            + "FROM platform_bot_conversation " + "WHERE bot_id = ? AND user_id = ? " + "ORDER BY updated_at DESC";

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert( BotConversation conversation, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, conversation.getBotId( ) );
            daoUtil.setString( nIndex++, conversation.getConversationUuid( ) );
            daoUtil.setString( nIndex++, conversation.getUserId( ) );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                conversation.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }

            return conversation.getId( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( BotConversation conversation, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, conversation.getBotId( ) );
            daoUtil.setString( nIndex++, conversation.getConversationUuid( ) );
            daoUtil.setString( nIndex++, conversation.getUserId( ) );
            daoUtil.setInt( nIndex, conversation.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BotConversation> load( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                BotConversation conversation = new BotConversation( );
                int nIndex = 1;
                conversation.setId( daoUtil.getInt( nIndex++ ) );
                conversation.setBotId( daoUtil.getInt( nIndex++ ) );
                conversation.setConversationUuid( daoUtil.getString( nIndex++ ) );
                conversation.setUserId( daoUtil.getString( nIndex++ ) );
                conversation.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
                conversation.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );

                return Optional.of( conversation );
            }

            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BotConversation> findByUuid( String uuid, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_UUID, plugin ) )
        {
            daoUtil.setString( 1, uuid );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                BotConversation conversation = new BotConversation( );
                int nIndex = 1;
                conversation.setId( daoUtil.getInt( nIndex++ ) );
                conversation.setBotId( daoUtil.getInt( nIndex++ ) );
                conversation.setConversationUuid( daoUtil.getString( nIndex++ ) );
                conversation.setUserId( daoUtil.getString( nIndex++ ) );
                conversation.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
                conversation.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );

                return Optional.of( conversation );
            }

            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BotConversation> selectByUserId( String userId, Plugin plugin )
    {
        List<BotConversation> conversationList = new ArrayList<>( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_USER_ID, plugin ) )
        {
            daoUtil.setString( 1, userId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                BotConversation conversation = new BotConversation( );
                int nIndex = 1;
                conversation.setId( daoUtil.getInt( nIndex++ ) );
                conversation.setBotId( daoUtil.getInt( nIndex++ ) );
                conversation.setConversationUuid( daoUtil.getString( nIndex++ ) );
                conversation.setUserId( daoUtil.getString( nIndex++ ) );
                conversation.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
                conversation.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );

                conversationList.add( conversation );
            }
        }

        return conversationList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BotConversation> selectByBotId( int nBotId, Plugin plugin )
    {
        List<BotConversation> conversationList = new ArrayList<>( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_BOT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                BotConversation conversation = new BotConversation( );
                int nIndex = 1;
                conversation.setId( daoUtil.getInt( nIndex++ ) );
                conversation.setBotId( daoUtil.getInt( nIndex++ ) );
                conversation.setConversationUuid( daoUtil.getString( nIndex++ ) );
                conversation.setUserId( daoUtil.getString( nIndex++ ) );
                conversation.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
                conversation.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );

                conversationList.add( conversation );
            }
        }

        return conversationList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BotConversation> selectByBotIdAndUserId( int nBotId, String userId, Plugin plugin )
    {
        List<BotConversation> conversationList = new ArrayList<>( );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_BOT_ID_AND_USER_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.setString( 2, userId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                BotConversation conversation = new BotConversation( );
                int nIndex = 1;
                conversation.setId( daoUtil.getInt( nIndex++ ) );
                conversation.setBotId( daoUtil.getInt( nIndex++ ) );
                conversation.setConversationUuid( daoUtil.getString( nIndex++ ) );
                conversation.setUserId( daoUtil.getString( nIndex++ ) );
                conversation.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
                conversation.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );

                conversationList.add( conversation );
            }
        }

        return conversationList;
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
}
