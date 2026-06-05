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

public final class BotEventTypes
{
    public static final String STREAM_REQUEST_RECEIVED = "BOT_STREAM_REQUEST_RECEIVED";
    public static final String TOKEN_STREAMED = "BOT_TOKEN_STREAMED";
    public static final String SOURCES_RETRIEVED = "BOT_SOURCES_RETRIEVED";
    public static final String STREAM_COMPLETED = "BOT_STREAM_COMPLETED";
    public static final String STREAM_ERROR = "BOT_STREAM_ERROR";
    public static final String DATASETS_ROUTED = "DATASETS_ROUTED";
    public static final String CONVERSATION_CREATED = "BOT_CONVERSATION_CREATED";
    public static final String CONVERSATION_ACCESSED = "BOT_CONVERSATION_ACCESSED";
    public static final String MESSAGE_ADDED_USER = "BOT_MESSAGE_ADDED_USER";
    public static final String MESSAGE_ADDED_ASSISTANT = "BOT_MESSAGE_ADDED_ASSISTANT";
    public static final String CONVERSATION_HISTORY_REQUESTED = "BOT_CONVERSATION_HISTORY_REQUESTED";
    public static final String CONVERSATION_HISTORY_DELIVERED = "BOT_CONVERSATION_HISTORY_DELIVERED";
    public static final String CONVERSATION_DELETED = "BOT_CONVERSATION_DELETED";
    public static final String USER_CONVERSATIONS_DELETED = "BOT_USER_CONVERSATIONS_DELETED";
    public static final String USER_CONVERSATIONS_REQUESTED = "BOT_USER_CONVERSATIONS_REQUESTED";
    public static final String USER_CONVERSATIONS_DELIVERED = "BOT_USER_CONVERSATIONS_DELIVERED";
    public static final String DOCUMENT_ACCESS_REQUESTED = "BOT_DOCUMENT_ACCESS_REQUESTED";
    public static final String DOCUMENT_DELIVERED = "BOT_DOCUMENT_DELIVERED";
    public static final String TOOL_PIPELINE_STARTED = "BOT_TOOL_PIPELINE_STARTED";
    public static final String TOOL_PIPELINE_COMPLETED = "BOT_TOOL_PIPELINE_COMPLETED";
    public static final String TOOL_PIPELINE_FAILED = "BOT_TOOL_PIPELINE_FAILED";
    public static final String BUILTIN_TOOL_STARTED = "BOT_BUILTIN_TOOL_STARTED";
    public static final String BUILTIN_TOOL_COMPLETED = "BOT_BUILTIN_TOOL_COMPLETED";
    public static final String BUILTIN_TOOL_FAILED = "BOT_BUILTIN_TOOL_FAILED";

    public static final class PayloadKeys
    {
        public static final String BOT_ID = "botId";
        public static final String CONVERSATION_UUID = "conversationUuid";
        public static final String USER_ID = "userId";
        public static final String QUERY = "query";
        public static final String TOKEN = "token";
        public static final String SOURCES = "sources";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String ERROR_DETAILS = "errorDetails";
        public static final String MESSAGE_CONTENT = "messageContent";
        public static final String MESSAGE_ROLE = "messageRole";
        public static final String MESSAGE_COUNT = "messageCount";
        public static final String CONVERSATION_COUNT = "conversationCount";
        public static final String DOCUMENT_ID = "documentId";
        public static final String DATASET_ID = "datasetId";
        public static final String DOCUMENT_NAME = "documentName";
        public static final String MESSAGE_ID = "messageId";
        public static final String PIPELINE_ID = "pipelineId";
        public static final String PIPELINE_NAME = "pipelineName";
        public static final String EXECUTION_ID = "executionId";
        public static final String TOOL_NAME = "toolName";
        public static final String TOOL_ARGS = "toolArgs";
        public static final String TOOL_RESULT_PREVIEW = "toolResultPreview";

        /**
         * Private constructor
         */
        private PayloadKeys( )
        {
        }
    }

    /**
     * Private constructor
     */
    private BotEventTypes( )
    {
    }
}
