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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.azure.AzureOpenAiChatModelName;
import dev.langchain4j.model.azure.AzureOpenAiTokenCountEstimator;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotConversation;
import fr.paris.lutece.plugins.platform.business.bot.BotConversationHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessage;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageHome;
import fr.paris.lutece.portal.service.util.AppLogService;

public class BotChatMemory implements ChatMemory
{

    private static final String ERROR_CONVERSATION_NOT_FOUND = "Unable to find conversation with UUID: ";
    private static final String TOKEN_ESTIMATION_ERROR_MESSAGE = "Error estimating tokens with Azure tokenizer, using fallback";
    private static final double TOKEN_FALLBACK_DIVISOR = 4.0;

    private final Bot bot;
    private final String conversationUuid;
    private final String userId;
    private final TokenCountEstimator tokenCountEstimator;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new BotChatMemory instance.
     *
     * @param bot
     *            the bot instance
     * @param conversationUuid
     *            the conversation UUID, can be null for new conversations
     * @param userId
     *            the user identifier
     */
    public BotChatMemory( Bot bot, String conversationUuid, String userId )
    {
        this.bot = bot;
        this.userId = userId;
        this.conversationUuid = ensureConversationExists( conversationUuid );
        this.tokenCountEstimator = new AzureOpenAiTokenCountEstimator( AzureOpenAiChatModelName.GPT_4_O );
        this.objectMapper = createLangchainObjectMapper( );
    }

    /**
     * Creates an ObjectMapper configured with langchain4j mixins for proper serialization.
     *
     * @return configured ObjectMapper
     */
    private ObjectMapper createLangchainObjectMapper( )
    {
        return JsonMapper.builder( ).visibility( PropertyAccessor.FIELD, ANY ).addMixIn( ToolExecutionRequest.class, ToolExecutionRequestMixin.class ).build( );
    }

    /**
     * Mixin class for ToolExecutionRequest serialization.
     */
    @JsonInclude( NON_NULL )
    @JsonDeserialize( builder = ToolExecutionRequest.Builder.class )
    private static abstract class ToolExecutionRequestMixin
    {
    }

    /**
     * Ensures that a conversation exists for the given UUID. Creates a new conversation if the UUID is null, empty, or doesn't exist.
     *
     * @param conversationUuid
     *            the conversation UUID to check
     * @return the UUID of an existing or newly created conversation
     */
    private String ensureConversationExists( String conversationUuid )
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
        return createNewConversation( );
    }

    /**
     * Creates a new conversation and returns its UUID.
     *
     * @return the UUID of the newly created conversation
     */
    private String createNewConversation( )
    {
        String newUuid = UUID.randomUUID( ).toString( );
        BotConversation conversation = new BotConversation( );
        conversation.setBotId( bot.getId( ) );
        conversation.setConversationUuid( newUuid );
        conversation.setUserId( userId );
        BotConversationHome.create( conversation );
        return newUuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object id( )
    {
        return conversationUuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add( ChatMessage message )
    {
        if ( message instanceof SystemMessage )
        {
            return;
        }

        if ( message instanceof UserMessage )
        {
            UserMessage userMessage = (UserMessage) message;
            String messageText = userMessage.singleText( );
            addUserMessage( messageText );
        }
        else if ( message instanceof AiMessage )
        {
            AiMessage aiMessage = (AiMessage) message;

            if ( aiMessage.hasToolExecutionRequests( ) )
            {
                addAiMessageWithToolRequests( aiMessage );
            }
            else
            {
                String messageText = aiMessage.text( );
                if ( messageText != null && !messageText.trim( ).isEmpty( ) )
                {
                    addAssistantMessage( messageText );
                }
            }
        }
        else if ( message instanceof ToolExecutionResultMessage )
        {
            ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) message;
            addToolExecutionResultMessage( toolResult );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChatMessage> messages( )
    {
        List<ChatMessage> messages = new ArrayList<>( );
        addSystemMessageIfExists( messages );
        addConversationMessages( messages );

        if ( bot.getMaxTokens( ) > 0 )
        {
            return applyTokenLimit( messages );
        }
        return messages;
    }

    /**
     * Adds the system message to the message list if it exists.
     *
     * @param messages
     *            the list to add the system message to
     */
    private void addSystemMessageIfExists( List<ChatMessage> messages )
    {
        if ( bot.getBotSystemPrompt( ) != null && !bot.getBotSystemPrompt( ).trim( ).isEmpty( ) )
        {
            messages.add( SystemMessage.from( bot.getBotSystemPrompt( ) ) );
        }
    }

    /**
     * Adds conversation messages to the messages list.
     *
     * @param messages
     *            the list to add conversation messages to
     */
    private void addConversationMessages( List<ChatMessage> messages )
    {
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation != null )
        {
            List<ConversationMessage> conversationMessages = ConversationMessageHome.getMessagesByConversationId( conversation.getId( ) );
            convertAndAddMessages( messages, conversationMessages );
        }
    }

    /**
     * Converts conversation messages to chat messages and adds them to the list.
     *
     * @param messages
     *            the list to add converted messages to
     * @param conversationMessages
     *            the conversation messages to convert
     */
    private void convertAndAddMessages( List<ChatMessage> messages, List<ConversationMessage> conversationMessages )
    {
        for ( ConversationMessage msg : conversationMessages )
        {
            if ( isMessageEmpty( msg ) )
            {
                continue;
            }

            ChatMessage chatMessage = convertToChatMessage( msg );
            if ( chatMessage != null )
            {
                messages.add( chatMessage );
            }
        }
    }

    /**
     * Checks if a conversation message is empty or null.
     *
     * @param msg
     *            the conversation message to check
     * @return true if the message is empty or null
     */
    private boolean isMessageEmpty( ConversationMessage msg )
    {
        return msg.getMessage( ) == null || msg.getMessage( ).trim( ).isEmpty( );
    }

    /**
     * Converts a single conversation message to the appropriate ChatMessage type.
     *
     * @param msg
     *            the conversation message to convert
     * @return the converted ChatMessage, or null if conversion fails
     */
    private ChatMessage convertToChatMessage( ConversationMessage msg )
    {
        String role = msg.getRole( );
        String messageContent = msg.getMessage( );

        if ( null != role )
            switch( role )
            {
                case ConversationMessage.ROLE_USER:
                    return UserMessage.from( messageContent );
                case ConversationMessage.ROLE_ASSISTANT:
                    return convertAssistantMessage( messageContent );
                case ConversationMessage.ROLE_TOOL:
                    return convertToolMessage( messageContent );
                default:
                    break;
            }

        return null;
    }

    /**
     * Converts an assistant message, handling tool requests if present.
     *
     * @param messageContent
     *            the message content
     * @return the converted AiMessage
     */
    private AiMessage convertAssistantMessage( String messageContent )
    {
        if ( messageContent.startsWith( "[TOOL_REQUESTS]" ) )
        {
            return parseToolRequestsMessage( messageContent );
        }
        return AiMessage.from( messageContent );
    }

    /**
     * Parses a tool requests message from its JSON representation.
     *
     * @param messageContent
     *            the message content containing tool requests
     * @return the parsed AiMessage with tool requests, or an error message
     */
    private AiMessage parseToolRequestsMessage( String messageContent )
    {
        try
        {
            String toolRequestsJson = messageContent.substring( "[TOOL_REQUESTS]".length( ) );
            ToolExecutionRequest [ ] toolRequests = objectMapper.readValue( toolRequestsJson, ToolExecutionRequest [ ].class );
            return AiMessage.from( toolRequests );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "BotChatMemory: Error reconstructing tool requests", e );
            return AiMessage.from( "[TOOL_REQUESTS_ERROR]" );
        }
    }

    /**
     * Converts a tool message to a ToolExecutionResultMessage.
     *
     * @param messageContent
     *            the message content
     * @return the converted ToolExecutionResultMessage
     */
    private ToolExecutionResultMessage convertToolMessage( String messageContent )
    {
        if ( messageContent.startsWith( "[TOOL_RESULT]" ) )
        {
            return parseToolResultMessage( messageContent );
        }

        AppLogService
                .error( "BotChatMemory: Tool message without [TOOL_RESULT] prefix detected, creating default tool result to maintain message consistency" );
        return ToolExecutionResultMessage.from( "unknown", "unknown", messageContent );
    }

    /**
     * Parses a tool result message from its JSON representation.
     *
     * @param messageContent
     *            the message content containing tool result
     * @return the parsed ToolExecutionResultMessage, or an error message
     */
    private ToolExecutionResultMessage parseToolResultMessage( String messageContent )
    {
        try
        {
            String resultJson = messageContent.substring( "[TOOL_RESULT]".length( ) );
            Map<String, Object> resultData = objectMapper.readValue( resultJson, new TypeReference<Map<String, Object>>( )
            {
            } );

            String toolId = extractStringValue( resultData, "toolId", "unknown" );
            String toolName = extractStringValue( resultData, "toolName", "unknown" );
            String result = extractToolResult( resultData );

            return ToolExecutionResultMessage.from( toolId, toolName, result );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "BotChatMemory: Error reconstructing tool result", e );
            return ToolExecutionResultMessage.from( "error", "error", "[TOOL_RESULT_ERROR]" );
        }
    }

    /**
     * Extracts a string value from a map with a default fallback.
     *
     * @param data
     *            the map to extract from
     * @param key
     *            the key to extract
     * @param defaultValue
     *            the default value if key is not found
     * @return the extracted value or default
     */
    private String extractStringValue( Map<String, Object> data, String key, String defaultValue )
    {
        Object value = data.get( key );
        return value != null ? value.toString( ) : defaultValue;
    }

    /**
     * Extracts the tool result from the result data, handling complex objects.
     *
     * @param resultData
     *            the result data map
     * @return the extracted result as a string
     */
    private String extractToolResult( Map<String, Object> resultData )
    {
        Object resultObj = resultData.get( "result" );
        if ( resultObj == null )
        {
            return "";
        }

        if ( resultObj instanceof String )
        {
            return (String) resultObj;
        }

        try
        {
            return objectMapper.writeValueAsString( resultObj );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "BotChatMemory: Failed to re-serialize complex result, using toString()", e );
            return resultObj.toString( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear( )
    {
        BotConversationHome.findByUuid( conversationUuid ).ifPresent( conversation -> ConversationMessageHome.removeByConversationId( conversation.getId( ) ) );
    }

    /**
     * Adds a user message to the conversation.
     *
     * @param message
     *            the user message text
     */
    private void addUserMessage( String message )
    {
        addMessage( message, ConversationMessage.ROLE_USER );
    }

    /**
     * Adds an assistant message to the conversation.
     *
     * @param message
     *            the assistant message text
     */
    private void addAssistantMessage( String message )
    {
        String cleanedMessage = message.replaceAll( "\\[Source[^\\]]*\\]", "" ).replaceAll( "[ \\t]+", " " ).trim( );
        addMessage( cleanedMessage, ConversationMessage.ROLE_ASSISTANT );
    }

    /**
     * Adds an AI message with tool execution requests to the conversation.
     *
     * @param aiMessage
     *            the AI message with tool requests
     */
    private void addAiMessageWithToolRequests( AiMessage aiMessage )
    {
        try
        {
            String toolRequestsJson = objectMapper.writeValueAsString( aiMessage.toolExecutionRequests( ) );
            String messageContent = "[TOOL_REQUESTS]" + toolRequestsJson;
            addMessage( messageContent, ConversationMessage.ROLE_ASSISTANT );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "BotChatMemory: Error serializing tool requests", e );
            addMessage( "[TOOL_REQUESTS_ERROR]", ConversationMessage.ROLE_ASSISTANT );
        }
    }

    /**
     * Adds a tool execution result message to the conversation.
     *
     * @param toolResult
     *            the tool execution result message
     */
    private void addToolExecutionResultMessage( ToolExecutionResultMessage toolResult )
    {
        try
        {
            Map<String, Object> resultData = new HashMap<>( );
            resultData.put( "toolId", toolResult.id( ) != null ? toolResult.id( ) : "unknown" );
            resultData.put( "toolName", toolResult.toolName( ) != null ? toolResult.toolName( ) : "unknown" );
            resultData.put( "result", toolResult.text( ) );

            String resultJson = objectMapper.writeValueAsString( resultData );
            String messageContent = "[TOOL_RESULT]" + resultJson;
            addMessage( messageContent, ConversationMessage.ROLE_TOOL );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( "BotChatMemory: Error saving tool execution result", e );
            addMessage( "[TOOL_RESULT_ERROR]", ConversationMessage.ROLE_TOOL );
        }
    }

    /**
     * Adds a message with the specified role to the conversation.
     *
     * @param message
     *            the message text
     * @param role
     *            the role of the message sender
     */
    private void addMessage( String message, String role )
    {
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation != null )
        {
            ConversationMessage conversationMessage = createConversationMessage( conversation.getId( ), message, role );
            ConversationMessageHome.create( conversationMessage );
        }
        else
        {
            AppLogService.error( "{}{}", ERROR_CONVERSATION_NOT_FOUND, conversationUuid );
        }
    }

    /**
     * Creates a new ConversationMessage object.
     *
     * @param conversationId
     *            the ID of the conversation
     * @param message
     *            the message text
     * @param role
     *            the role of the message sender
     * @return a new ConversationMessage instance
     */
    private ConversationMessage createConversationMessage( int conversationId, String message, String role )
    {
        ConversationMessage conversationMessage = new ConversationMessage( );
        conversationMessage.setConversationId( conversationId );
        conversationMessage.setMessage( message );
        conversationMessage.setRole( role );
        return conversationMessage;
    }

    /**
     * Applies token limit to the message list, keeping the most recent messages.
     *
     * @param messages
     *            the list of messages to apply token limit to
     * @return a new list of messages within the token limit
     */
    private List<ChatMessage> applyTokenLimit( List<ChatMessage> messages )
    {
        if ( messages.isEmpty( ) )
        {
            return messages;
        }

        int maxTokens = bot.getMaxTokens( );
        int currentTokens = estimateTokenCount( messages );

        if ( currentTokens <= maxTokens )
        {
            return messages;
        }

        List<ChatMessage> result = initializeResultWithSystemMessage( messages );
        int resultTokens = estimateTokenCount( result );

        addMessagesWithinTokenLimit( messages, result, maxTokens, resultTokens );

        return result;
    }

    /**
     * Initializes the result list with system message if present.
     *
     * @param messages
     *            the original message list
     * @return a new list containing only the system message if present
     */
    private List<ChatMessage> initializeResultWithSystemMessage( List<ChatMessage> messages )
    {
        List<ChatMessage> result = new ArrayList<>( );
        if ( !messages.isEmpty( ) && messages.get( 0 ) instanceof SystemMessage )
        {
            result.add( messages.get( 0 ) );
        }
        return result;
    }

    /**
     * Adds messages to the result list while staying within token limits.
     *
     * @param messages
     *            the original message list
     * @param result
     *            the result list to add messages to
     * @param maxTokens
     *            the maximum token limit
     * @param currentTokens
     *            the current token count in result
     */
    private void addMessagesWithinTokenLimit( List<ChatMessage> messages, List<ChatMessage> result, int maxTokens, int currentTokens )
    {
        int startIndex = result.isEmpty( ) ? 0 : 1;
        int tokens = currentTokens;

        int lastUserIndex = -1;
        for ( int i = messages.size( ) - 1; i >= 0; i-- )
        {
            if ( messages.get( i ) instanceof UserMessage )
            {
                lastUserIndex = i;
                break;
            }
        }

        for ( int i = messages.size( ) - 1; i >= startIndex; i-- )
        {
            ChatMessage message = messages.get( i );
            int messageTokens = estimateTokenCount( List.of( message ) );

            boolean mustInclude = ( lastUserIndex >= 0 && i >= lastUserIndex );

            boolean isToolResponse = message instanceof ToolExecutionResultMessage;
            boolean hasToolCall = false;
            if ( i > 0 && isToolResponse )
            {
                ChatMessage prevMessage = messages.get( i - 1 );
                if ( prevMessage instanceof AiMessage )
                {
                    hasToolCall = ( (AiMessage) prevMessage ).hasToolExecutionRequests( );
                }
            }

            if ( mustInclude || ( tokens + messageTokens <= maxTokens ) )
            {
                if ( result.contains( message ) )
                {
                    continue;
                }
                if ( result.isEmpty( ) )
                {
                    result.add( message );
                }
                else
                {
                    result.add( 1, message );
                }
                tokens += messageTokens;

                if ( isToolResponse && hasToolCall && i > 0 )
                {
                    ChatMessage toolCallMsg = messages.get( i - 1 );
                    int toolCallTokens = estimateTokenCount( List.of( toolCallMsg ) );
                    if ( !result.contains( toolCallMsg ) )
                    {
                        result.add( 1, toolCallMsg );
                        tokens += toolCallTokens;
                    }
                }
            }
            else if ( !mustInclude )
            {
                break;
            }
        }

    }

    /**
     * Estimates the token count for a list of messages.
     *
     * @param messages
     *            the list of messages to count tokens for
     * @return the estimated token count
     */
    private int estimateTokenCount( List<ChatMessage> messages )
    {
        try
        {
            return tokenCountEstimator.estimateTokenCountInMessages( messages );
        }
        catch( Exception e )
        {
            AppLogService.error( TOKEN_ESTIMATION_ERROR_MESSAGE, e );
            return calculateFallbackTokenCount( messages );
        }
    }

    /**
     * Calculates a fallback token count when the tokenizer fails.
     *
     * @param messages
     *            the list of messages to count tokens for
     * @return the estimated token count using fallback method
     */
    private int calculateFallbackTokenCount( List<ChatMessage> messages )
    {
        return messages.stream( ).mapToInt( msg -> {
            String text = "";
            if ( msg instanceof UserMessage )
            {
                text = ( (UserMessage) msg ).singleText( );
            }
            else if ( msg instanceof AiMessage )
            {
                text = ( (AiMessage) msg ).text( );
            }
            else if ( msg instanceof SystemMessage )
            {
                text = ( (SystemMessage) msg ).text( );
            }
            return (int) Math.ceil( text.length( ) / TOKEN_FALLBACK_DIVISOR );
        } ).sum( );
    }
}
