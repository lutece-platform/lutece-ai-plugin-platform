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
package fr.paris.lutece.plugins.platform.service.conversation.dto;

import java.util.List;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.conversation.MessageDTO;

/**
 * Typed result of resolving a conversation timeline for the front-office. Carries the owning {@link Bot} (already RBAC-enriched when the user is allowed to
 * view it), the ordered list of {@link MessageDTO} messages and a visibility flag, so the controller only has to place the data in the model.
 */
public final class ConversationView
{
    private final String _conversationUuid;
    private final Bot _bot;
    private final List<MessageDTO> _messages;
    private final boolean _accessGranted;

    /**
     * Builds a conversation view.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param bot
     *            the owning bot, RBAC-enriched, or {@code null} when the conversation or bot could not be resolved
     * @param messages
     *            the ordered conversation messages (never {@code null})
     * @param accessGranted
     *            {@code true} when the user is allowed to view the conversation
     */
    public ConversationView( String conversationUuid, Bot bot, List<MessageDTO> messages, boolean accessGranted )
    {
        _conversationUuid = conversationUuid;
        _bot = bot;
        _messages = messages;
        _accessGranted = accessGranted;
    }

    /**
     * Returns the conversation UUID.
     *
     * @return the conversation UUID
     */
    public String getConversationUuid( )
    {
        return _conversationUuid;
    }

    /**
     * Returns the owning bot, RBAC-enriched, or {@code null} when it could not be resolved.
     *
     * @return the bot or {@code null}
     */
    public Bot getBot( )
    {
        return _bot;
    }

    /**
     * Returns the ordered conversation messages.
     *
     * @return the messages (never {@code null})
     */
    public List<MessageDTO> getMessages( )
    {
        return _messages;
    }

    /**
     * Indicates whether the user is allowed to view the conversation.
     *
     * @return {@code true} if access is granted
     */
    public boolean isAccessGranted( )
    {
        return _accessGranted;
    }
}
