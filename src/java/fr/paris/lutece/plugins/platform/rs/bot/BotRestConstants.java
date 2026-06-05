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
package fr.paris.lutece.plugins.platform.rs.bot;

/**
 * REST contract shared by the client and admin bot endpoints (paths and JSON keys).
 */
public final class BotRestConstants
{
    /**
     * Root path of the bot resources.
     */
    public static final String BOTS_PATH = "bots";

    /**
     * Path of the streaming query : a single POST whose response body is the SSE event stream.
     */
    public static final String QUERY_STREAM_PATH = "query/stream";

    /**
     * Path of a conversation history.
     */
    public static final String CONVERSATION_HISTORY_PATH = "conversation/{conversation_uuid}";

    /**
     * Path listing the conversations of a user.
     */
    public static final String USER_CONVERSATIONS_PATH = "user/conversations";

    /**
     * Path serving a source document file.
     */
    public static final String DOCUMENT_FILE_PATH = "document/{bot_id}/{dataset_id}/{document_id}";

    /**
     * Path posting a feedback on a message.
     */
    public static final String MESSAGE_FEEDBACK_PATH = "message/{message_id}/feedback";

    /**
     * JSON key of the conversation UUID.
     */
    public static final String CONVERSATION_UUID_KEY = "conversationUuid";

    /**
     * JSON key of the messages list.
     */
    public static final String MESSAGES_KEY = "messages";

    /**
     * JSON key of the conversations list.
     */
    public static final String CONVERSATIONS_KEY = "conversations";

    /**
     * Content-Disposition format for document downloads.
     */
    public static final String ATTACHMENT_FILENAME_FORMAT = "attachment; filename=\"%s\"";

    /**
     * Private constructor to prevent instantiation.
     */
    private BotRestConstants( )
    {
    }
}
