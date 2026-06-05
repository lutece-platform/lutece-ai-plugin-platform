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
 * Home class for ConversationMessage management
 */
public final class ConversationMessageHome
{
    private static Plugin _plugin = PluginService.getPlugin( "platform" );
    private static IConversationMessageDAO _dao = CDI.current( ).select( IConversationMessageDAO.class ).get( );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private ConversationMessageHome( )
    {
    }

    /**
     * Create a new conversation message
     *
     * @param message
     *            The message to create
     * @return The newly created message ID
     */
    public static int create( ConversationMessage message )
    {
        return _dao.insert( message, _plugin );
    }

    /**
     * Find a message by its primary key
     *
     * @param nId
     *            The message ID
     * @return An Optional containing the message if found
     */
    public static Optional<ConversationMessage> findByPrimaryKey( int nId )
    {
        return _dao.load( nId, _plugin );
    }

    /**
     * Get all messages for a conversation
     *
     * @param nConversationId
     *            The conversation ID
     * @return The list of messages
     */
    public static List<ConversationMessage> getMessagesByConversationId( int nConversationId )
    {
        return _dao.selectByConversationId( nConversationId, _plugin );
    }

    /**
     * Remove a message
     *
     * @param nId
     *            The message ID
     */
    public static void remove( int nId )
    {
        _dao.delete( nId, _plugin );
    }

    /**
     * Remove all messages for a conversation
     *
     * @param nConversationId
     *            The conversation ID
     */
    public static void removeByConversationId( int nConversationId )
    {
        _dao.deleteByConversationId( nConversationId, _plugin );
    }
}
