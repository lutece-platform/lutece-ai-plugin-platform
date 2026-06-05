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
import java.util.List;
import java.util.Optional;

/**
 * Interface for ConversationMessageFeedback DAO
 */
public interface IConversationMessageFeedbackDAO
{
    /**
     * Insert a new feedback
     *
     * @param feedback
     *            The feedback to insert
     * @param plugin
     *            The plugin
     * @return The generated ID
     */
    int insert( ConversationMessageFeedback feedback, Plugin plugin );

    /**
     * Load a feedback by its ID
     *
     * @param nId
     *            The feedback ID
     * @param plugin
     *            The plugin
     * @return The feedback if found
     */
    Optional<ConversationMessageFeedback> load( int nId, Plugin plugin );

    /**
     * Update a feedback
     *
     * @param feedback
     *            The feedback to update
     * @param plugin
     *            The plugin
     */
    void store( ConversationMessageFeedback feedback, Plugin plugin );

    /**
     * Delete a feedback by its ID
     *
     * @param nId
     *            The feedback ID
     * @param plugin
     *            The plugin
     */
    void delete( int nId, Plugin plugin );

    /**
     * Find feedback by message ID
     *
     * @param nMessageId
     *            The message ID
     * @param plugin
     *            The plugin
     * @return The feedback if found
     */
    Optional<ConversationMessageFeedback> findByMessageId( int nMessageId, Plugin plugin );

    /**
     * Find all feedbacks for a bot
     *
     * @param nBotId
     *            The bot ID
     * @param plugin
     *            The plugin
     * @return List of feedbacks
     */
    List<ConversationMessageFeedback> findByBotId( int nBotId, Plugin plugin );

    /**
     * Find all feedbacks for a bot with a specific status
     *
     * @param nBotId
     *            The bot ID
     * @param status
     *            The status to filter by
     * @param plugin
     *            The plugin
     * @return List of feedbacks
     */
    List<ConversationMessageFeedback> findByBotIdAndStatus( int nBotId, ConversationMessageFeedback.Status status, Plugin plugin );

    /**
     * Find all feedbacks for a user
     *
     * @param strUserId
     *            The user ID
     * @param plugin
     *            The plugin
     * @return List of feedbacks
     */
    List<ConversationMessageFeedback> findByUserId( String strUserId, Plugin plugin );

    /**
     * Delete all feedbacks for a message
     *
     * @param nMessageId
     *            The message ID
     * @param plugin
     *            The plugin
     */
    void deleteByMessageId( int nMessageId, Plugin plugin );
}
