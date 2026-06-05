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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Optional;

public final class BotConversationHome
{
    private static Plugin _plugin = PluginService.getPlugin( "platform" );
    private static IBotConversationDAO _dao = CDI.current( ).select( IBotConversationDAO.class ).get( );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private BotConversationHome( )
    {
    }

    /**
     * Creates a new bot conversation in the database
     *
     * @param conversation
     *            The conversation to create
     * @return The newly created conversation ID
     */
    public static int create( BotConversation conversation )
    {
        return _dao.insert( conversation, _plugin );
    }

    /**
     * Updates a bot conversation in the database
     *
     * @param conversation
     *            The conversation to update
     */
    public static void update( BotConversation conversation )
    {
        _dao.store( conversation, _plugin );
    }

    /**
     * Loads a bot conversation by its primary key
     *
     * @param nId
     *            The conversation ID
     * @return An Optional containing the conversation if found
     */
    public static Optional<BotConversation> findByPrimaryKey( int nId )
    {
        return _dao.load( nId, _plugin );
    }

    /**
     * Finds a bot conversation by its UUID
     *
     * @param uuid
     *            The conversation UUID
     * @return An Optional containing the conversation if found
     */
    public static Optional<BotConversation> findByUuid( String uuid )
    {
        return _dao.findByUuid( uuid, _plugin );
    }

    /**
     * Retrieves all conversations associated with a specific bot
     *
     * @param nBotId
     *            The bot ID
     * @return A list of conversations for the given bot
     */
    public static List<BotConversation> getConversationsByBotId( int nBotId )
    {
        return _dao.selectByBotId( nBotId, _plugin );
    }

    /**
     * Retrieves all conversations associated with a specific user
     *
     * @param userId
     *            The user ID
     * @return A list of conversations for the given user
     */
    public static List<BotConversation> getConversationsByUserId( String userId )
    {
        return _dao.selectByUserId( userId, _plugin );
    }

    /**
     * Removes a bot conversation from the database
     *
     * @param nId
     *            The ID of the conversation to remove
     */
    public static void remove( int nId )
    {
        _dao.delete( nId, _plugin );
    }

    /**
     * Retrieves all conversations associated with a specific bot and user
     *
     * @param nBotId
     *            The bot ID
     * @param userId
     *            The user ID
     * @return A list of conversations for the given bot and user
     */
    public static List<BotConversation> getConversationsByBotIdAndUserId( int nBotId, String userId )
    {
        return _dao.selectByBotIdAndUserId( nBotId, userId, _plugin );
    }
}
