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
package fr.paris.lutece.plugins.platform.service.bot;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import dev.langchain4j.memory.ChatMemory;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Service for managing bot conversation memory and persistence
 */
@ApplicationScoped
@Named( "platform.botMemoryService" )
public class BotMemoryService
{
    @Resource( lookup = "java:comp/DefaultContextService" )
    private ContextService _contextService;

    /**
     * Private constructor for singleton pattern
     */
    BotMemoryService( )
    {
    }

    /**
     * Gets an existing conversation or creates a new one
     *
     * @param botId
     *            the bot identifier
     * @param conversationUuid
     *            the conversation UUID (can be null)
     * @param userId
     *            the user identifier
     * @return the conversation UUID
     */
    public String getOrCreateConversation( int botId, String conversationUuid, String userId )
    {
        if ( conversationUuid != null && !conversationUuid.isEmpty( ) )
        {
            BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
            if ( conversation != null )
            {
                BotConversationHome.update( conversation );
                return conversation.getConversationUuid( );
            }
        }

        String newUuid = UUID.randomUUID( ).toString( );
        BotConversation conversation = new BotConversation( );
        conversation.setBotId( botId );
        conversation.setConversationUuid( newUuid );
        conversation.setUserId( userId );
        BotConversationHome.create( conversation );

        return newUuid;
    }

    /**
     * Gets all conversations for a specific user
     *
     * @param userId
     *            the user identifier
     * @return list of bot conversations
     */
    public List<BotConversation> getConversationsByUserId( String userId )
    {
        return BotConversationHome.getConversationsByUserId( userId );
    }

    /**
     * Creates a persistent chat memory implementation
     *
     * @param bot
     *            the bot
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user identifier
     * @return a ChatMemory implementation
     */
    public ChatMemory createPersistentChatMemory( Bot bot, String conversationUuid, String userId )
    {
        Executor ctxExecutor = _contextService.currentContextExecutor( );
        return new ContextualChatMemory( new BotChatMemory( bot, conversationUuid, userId ), ctxExecutor );
    }

    /**
     * Deletes a conversation and all its messages
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user identifier
     */
    public void deleteConversation( String conversationUuid, String userId )
    {
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation != null && conversation.getUserId( ).equals( userId ) )
        {
            BotConversationHome.remove( conversation.getId( ) );
        }
    }

}
