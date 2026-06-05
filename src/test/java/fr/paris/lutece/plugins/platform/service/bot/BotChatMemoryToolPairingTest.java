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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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
 * Adversarial test for {@link BotChatMemory} token trimming around tool-call / tool-result pairs. Unlike the characterisation tests, the expectations here come
 * from the LLM message-sequence contract, not from the implementation: a valid conversation sent to a model must never contain an orphan tool result (a result
 * whose tool call was trimmed away), an orphan tool call (a call whose result was trimmed away), nor a duplicated tool call or result. The fixture builds a
 * multi-turn conversation where each turn carries a tool call and its result, then sweeps a range of token caps to force the trim boundary to fall in different
 * places, asserting the contract holds for every cap. A baseline with no cap proves the conversation is well-formed before trimming.
 */
public class BotChatMemoryToolPairingTest extends AbstractPlatformDbTest
{
    private static final String USER_ID = "tool-pairing-user";
    private static final int TURNS = 6;
    private static final int [ ] TOKEN_CAPS = {
            0, 30, 50, 70, 90, 120, 160, 220, 300
    };

    private Client _client;
    private Provider _llmProvider;
    private Provider _embedProvider;

    /**
     * Builds the foreign-key parent chain shared by the test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client ToolPairing Test" );
        _client.setCode( "CLIENT_TOOLPAIRING_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _llmProvider = createProvider( "LLM" );
        _embedProvider = createProvider( "EMBEDDING" );
    }

    /**
     * Sweeps several token caps over a tool-heavy conversation and asserts the reconstructed message list is a valid LLM sequence for each cap.
     */
    @Test
    public void testTrimmingPreservesToolPairing( )
    {
        Bot bot = createBot( );
        BotChatMemory memory = new BotChatMemory( bot, null, USER_ID );

        for ( int k = 0; k < TURNS; k++ )
        {
            memory.add( UserMessage.from( "Question number " + k + " asking the assistant to look something up in the dataset" ) );
            ToolExecutionRequest request = ToolExecutionRequest.builder( ).id( "call-" + k ).name( "searchDataset" )
                    .arguments( "{\"query\":\"topic " + k + " with some extra words to weigh tokens\"}" ).build( );
            memory.add( AiMessage.from( request ) );
            memory.add( ToolExecutionResultMessage.from( "call-" + k, "searchDataset", "Result payload for query " + k + " containing several words" ) );
            memory.add( AiMessage.from( "Here is the answer for turn " + k + " summarising what the tool returned in a few words" ) );
        }

        for ( int cap : TOKEN_CAPS )
        {
            bot.setMaxTokens( cap );
            List<ChatMessage> messages = memory.messages( );
            assertValidLlmSequence( messages, cap );
        }
    }

    /**
     * Asserts that a reconstructed message list honours the LLM tool-call contract: no duplicate tool call or result, every tool result is preceded by its
     * matching tool call, and every tool call is followed by its matching result.
     *
     * @param messages
     *            the reconstructed message list
     * @param cap
     *            the token cap under test, used in failure messages
     */
    private void assertValidLlmSequence( List<ChatMessage> messages, int cap )
    {
        List<String> callIdsInOrder = new ArrayList<>( );
        List<String> resultIdsInOrder = new ArrayList<>( );
        Set<String> callIds = new HashSet<>( );
        Set<String> resultIds = new HashSet<>( );

        for ( ChatMessage message : messages )
        {
            if ( message instanceof AiMessage && ( (AiMessage) message ).hasToolExecutionRequests( ) )
            {
                for ( ToolExecutionRequest request : ( (AiMessage) message ).toolExecutionRequests( ) )
                {
                    assertTrue( callIds.add( request.id( ) ), "cap=" + cap + " : duplicate tool call id " + request.id( ) );
                    callIdsInOrder.add( request.id( ) );
                }
            }
            if ( message instanceof ToolExecutionResultMessage )
            {
                String id = ( (ToolExecutionResultMessage) message ).id( );
                assertTrue( resultIds.add( id ), "cap=" + cap + " : duplicate tool result id " + id );
                resultIdsInOrder.add( id );
                assertTrue( callIds.contains( id ), "cap=" + cap + " : orphan tool result " + id + " has no preceding tool call" );
            }
        }

        for ( String callId : callIdsInOrder )
        {
            assertTrue( resultIds.contains( callId ), "cap=" + cap + " : orphan tool call " + callId + " has no matching result" );
        }
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
     * Creates a persisted bot wired to the shared parent chain with a system prompt and no initial token cap.
     *
     * @return the persisted bot
     */
    private Bot createBot( )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Bot ToolPairing Test" );
        bot.setBotSystemPrompt( "You are a helpful assistant." );
        bot.setClientId( _client.getId( ) );
        bot.setLlmProviderId( _llmProvider.getId( ) );
        bot.setEmbedProviderId( _embedProvider.getId( ) );
        bot.setMaxTokens( 0 );
        bot.setTemperature( 0.7 );
        return BotHome.create( bot );
    }
}
