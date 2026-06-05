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

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;

/**
 * Interface for ConversationMessage data access
 */
public interface IConversationMessageDAO
{

    /**
     * Insert a new conversation message
     *
     * @param message
     *            The ConversationMessage object
     * @param plugin
     *            The plugin
     * @return The new primary key
     */
    int insert( ConversationMessage message, Plugin plugin );

    /**
     * Load a message by ID
     *
     * @param nId
     *            The message ID
     * @param plugin
     *            The plugin
     * @return An Optional containing the message if found
     */
    Optional<ConversationMessage> load( int nId, Plugin plugin );

    /**
     * Get all messages for a conversation
     *
     * @param nConversationId
     *            The conversation ID
     * @param plugin
     *            The plugin
     * @return The list of conversation messages
     */
    List<ConversationMessage> selectByConversationId( int nConversationId, Plugin plugin );

    /**
     * Delete a message
     *
     * @param nId
     *            The message ID
     * @param plugin
     *            The plugin
     */
    void delete( int nId, Plugin plugin );

    /**
     * Delete all messages for a conversation
     *
     * @param nConversationId
     *            The conversation ID
     * @param plugin
     *            The plugin
     */
    void deleteByConversationId( int nConversationId, Plugin plugin );
}
