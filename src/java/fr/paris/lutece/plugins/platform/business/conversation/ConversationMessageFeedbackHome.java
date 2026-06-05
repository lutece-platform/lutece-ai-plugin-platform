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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Optional;

/**
 * Home class for ConversationMessageFeedback
 */
public final class ConversationMessageFeedbackHome
{
    private static final IConversationMessageFeedbackDAO _dao = CDI.current( ).select( IConversationMessageFeedbackDAO.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class should not be instantiated
     */
    private ConversationMessageFeedbackHome( )
    {
    }

    /**
     * Create an instance of the conversationMessageFeedback class
     *
     * @param feedback
     *            The instance of the ConversationMessageFeedback which contains the informations to store
     * @return The instance of conversationMessageFeedback which has been created with its primary key.
     */
    public static ConversationMessageFeedback create( ConversationMessageFeedback feedback )
    {
        _dao.insert( feedback, _plugin );
        return feedback;
    }

    /**
     * Update of the conversationMessageFeedback which is specified in parameter
     *
     * @param feedback
     *            The instance of the ConversationMessageFeedback which contains the data to store
     * @return The instance of the conversationMessageFeedback which has been updated
     */
    public static ConversationMessageFeedback update( ConversationMessageFeedback feedback )
    {
        _dao.store( feedback, _plugin );
        return feedback;
    }

    /**
     * Remove the conversationMessageFeedback whose identifier is specified in parameter
     *
     * @param nKey
     *            The conversationMessageFeedback Id
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Returns an instance of a conversationMessageFeedback whose identifier is specified in parameter
     *
     * @param nKey
     *            The conversationMessageFeedback primary key
     * @return an instance of ConversationMessageFeedback
     */
    public static Optional<ConversationMessageFeedback> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Load the data of all the conversationMessageFeedback objects for a specific message and returns them as a list
     *
     * @param nMessageId
     *            The message ID
     * @return The list which contains the data of all the conversationMessageFeedback objects
     */
    public static Optional<ConversationMessageFeedback> findByMessageId( int nMessageId )
    {
        return _dao.findByMessageId( nMessageId, _plugin );
    }

    /**
     * Load the data of all the conversationMessageFeedback objects for a specific bot and returns them as a list
     *
     * @param nBotId
     *            The bot ID
     * @return The list which contains the data of all the conversationMessageFeedback objects
     */
    public static List<ConversationMessageFeedback> getConversationMessageFeedbacksList( int nBotId )
    {
        return _dao.findByBotId( nBotId, _plugin );
    }

    /**
     * Load the data of all the conversationMessageFeedback objects for a specific bot and status and returns them as a list
     *
     * @param nBotId
     *            The bot ID
     * @param status
     *            The status to filter by
     * @return The list which contains the data of all the conversationMessageFeedback objects
     */
    public static List<ConversationMessageFeedback> getConversationMessageFeedbacksList( int nBotId, ConversationMessageFeedback.Status status )
    {
        return _dao.findByBotIdAndStatus( nBotId, status, _plugin );
    }

    /**
     * Load the data of all the conversationMessageFeedback objects for a specific user and returns them as a list
     *
     * @param strUserId
     *            The user ID
     * @return The list which contains the data of all the conversationMessageFeedback objects
     */
    public static List<ConversationMessageFeedback> getConversationMessageFeedbacksListByUserId( String strUserId )
    {
        return _dao.findByUserId( strUserId, _plugin );
    }

    /**
     * Delete all feedbacks for a specific message
     *
     * @param nMessageId
     *            The message ID
     */
    public static void removeByMessageId( int nMessageId )
    {
        _dao.deleteByMessageId( nMessageId, _plugin );
    }
}
