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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link BotChatMemory}, the database-backed langchain4j {@link dev.langchain4j.memory.ChatMemory} implementation. These prove the
 * persistence round-trip: a user message and an assistant reply survive a reload; the bot system prompt is always prepended (and a transient SystemMessage is
 * never stored); empty assistant messages are dropped; assistant source tags are stripped; tool-call requests and tool results are serialised and faithfully
 * reconstructed; clearing wipes the history; and the token cap keeps the most recent exchange. The system prompt is set so the prepended SystemMessage anchors
 * the message list.
 */
public class BotChatMemoryTest extends AbstractPlatformDbTest
{
    private static final String SYSTEM_PROMPT = "You are a helpful assistant.";
    private static final String USER_ID = "chat-memory-user";

    private Client _client;
    private Provider _llmProvider;
    private Provider _embedProvider;

    /**
     * Builds the foreign-key parent chain (client and two providers) shared by every test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client ChatMemory Test" );
        _client.setCode( "CLIENT_CHATMEMORY_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _llmProvider = createProvider( "LLM" );
        _embedProvider = createProvider( "EMBEDDING" );
    }

    /**
     * A user message and an assistant reply are persisted and reloaded in order, with the bot system prompt prepended.
     */
    @Test
    public void testRoundTripUserAndAssistant( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        memory.add( UserMessage.from( "Hello" ) );
        memory.add( AiMessage.from( "Hi there" ) );

        List<ChatMessage> messages = memory.messages( );
        assertEquals( 3, messages.size( ) );
        assertTrue( messages.get( 0 ) instanceof SystemMessage );
        assertEquals( SYSTEM_PROMPT, ( (SystemMessage) messages.get( 0 ) ).text( ) );
        assertTrue( messages.get( 1 ) instanceof UserMessage );
        assertEquals( "Hello", ( (UserMessage) messages.get( 1 ) ).singleText( ) );
        assertTrue( messages.get( 2 ) instanceof AiMessage );
        assertEquals( "Hi there", ( (AiMessage) messages.get( 2 ) ).text( ) );
    }

    /**
     * A transient SystemMessage handed to add() is never stored; the only system message comes from the bot prompt.
     */
    @Test
    public void testTransientSystemMessageNotStored( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        memory.add( SystemMessage.from( "transient system" ) );
        memory.add( UserMessage.from( "Question" ) );

        List<ChatMessage> messages = memory.messages( );
        long systemCount = messages.stream( ).filter( m -> m instanceof SystemMessage ).count( );
        assertEquals( 1, systemCount );
        assertEquals( SYSTEM_PROMPT, ( (SystemMessage) messages.get( 0 ) ).text( ) );
    }

    /**
     * An empty assistant message is not persisted.
     */
    @Test
    public void testEmptyAssistantMessageDropped( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        memory.add( UserMessage.from( "Hello" ) );
        memory.add( AiMessage.from( "   " ) );

        List<ChatMessage> messages = memory.messages( );
        long aiCount = messages.stream( ).filter( m -> m instanceof AiMessage ).count( );
        assertEquals( 0, aiCount );
    }

    /**
     * Source citation tags are stripped from a stored assistant message and surrounding whitespace is collapsed.
     */
    @Test
    public void testAssistantSourceTagsStripped( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        memory.add( AiMessage.from( "Answer [Source: doc.pdf] more" ) );

        AiMessage stored = (AiMessage) memory.messages( ).stream( ).filter( m -> m instanceof AiMessage ).findFirst( ).get( );
        assertEquals( "Answer more", stored.text( ) );
    }

    /**
     * An assistant message carrying tool-execution requests is serialised and reconstructed with its tool name and arguments intact.
     */
    @Test
    public void testToolRequestRoundTrip( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        ToolExecutionRequest request = ToolExecutionRequest.builder( ).id( "call-1" ).name( "searchDataset" ).arguments( "{\"query\":\"contract\"}" ).build( );
        memory.add( AiMessage.from( request ) );

        AiMessage stored = (AiMessage) memory.messages( ).stream( ).filter( m -> m instanceof AiMessage && ( (AiMessage) m ).hasToolExecutionRequests( ) )
                .findFirst( ).get( );
        assertEquals( 1, stored.toolExecutionRequests( ).size( ) );
        assertEquals( "searchDataset", stored.toolExecutionRequests( ).get( 0 ).name( ) );
        assertEquals( "{\"query\":\"contract\"}", stored.toolExecutionRequests( ).get( 0 ).arguments( ) );
    }

    /**
     * A tool-execution result is serialised and reconstructed with its id, tool name and text payload.
     */
    @Test
    public void testToolResultRoundTrip( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        memory.add( ToolExecutionResultMessage.from( "call-1", "searchDataset", "3 results found" ) );

        ToolExecutionResultMessage stored = (ToolExecutionResultMessage) memory.messages( ).stream( ).filter( m -> m instanceof ToolExecutionResultMessage )
                .findFirst( ).get( );
        assertEquals( "call-1", stored.id( ) );
        assertEquals( "searchDataset", stored.toolName( ) );
        assertEquals( "3 results found", stored.text( ) );
    }

    /**
     * Clearing the memory removes the stored exchange, leaving only the prepended system prompt.
     */
    @Test
    public void testClearRemovesHistory( )
    {
        Bot bot = createBot( 0 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );
        memory.add( UserMessage.from( "Hello" ) );
        memory.add( AiMessage.from( "Hi" ) );

        memory.clear( );

        List<ChatMessage> messages = memory.messages( );
        assertEquals( 1, messages.size( ) );
        assertTrue( messages.get( 0 ) instanceof SystemMessage );
    }

    /**
     * Under a small token cap the history is trimmed, yet the system prompt and the most recent user message are always retained.
     */
    @Test
    public void testTokenLimitKeepsSystemAndLastUserMessage( )
    {
        Bot bot = createBot( 40 );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );
        for ( int i = 0; i < 10; i++ )
        {
            memory.add( UserMessage.from( "This is a fairly long user question number " + i + " padded with extra words to consume tokens" ) );
            memory.add( AiMessage.from( "This is a fairly long assistant answer number " + i + " padded with extra words to consume tokens" ) );
        }

        List<ChatMessage> messages = memory.messages( );
        assertTrue( messages.size( ) < 21 );
        assertTrue( messages.get( 0 ) instanceof SystemMessage );
        boolean hasUserMessage = messages.stream( ).anyMatch( m -> m instanceof UserMessage );
        assertTrue( hasUserMessage );
    }

    /**
     * Creates a persisted provider of the given type.
     *
     * @param strType
     *            the provider type
     * @return the persisted provider
     */
    private Provider createProvider( String strType )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Provider " + strType );
        provider.setProviderType( strType );
        provider.setDeploymentModelName( "model-" + strType );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates a persisted bot wired to the shared parent chain with the given token cap and the shared system prompt.
     *
     * @param nMaxTokens
     *            the maximum tokens for the conversation window (0 disables trimming)
     * @return the persisted bot
     */
    private Bot createBot( int nMaxTokens )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot ChatMemory Test" );
        bot.setBotSystemPrompt( SYSTEM_PROMPT );
        bot.setClientId( _client.getId( ) );
        bot.setLlmProviderId( _llmProvider.getId( ) );
        bot.setEmbedProviderId( _embedProvider.getId( ) );
        bot.setMaxTokens( nMaxTokens );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }
}
