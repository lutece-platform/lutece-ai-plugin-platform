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
package fr.paris.lutece.plugins.platform.business.mcp;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for {@link BotMcpServerHome}, the bot &harr; MCP server association table.
 *
 * <p>
 * The association rows carry NOT NULL foreign keys to {@code platform_bot} and {@code platform_mcp_server}, so a full parent chain (client + provider + bot +
 * mcp server) is created up front and torn down at the end.
 * </p>
 */
public class BotMcpServerHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises associate / select / dissociate on a bot &harr; MCP server pair, then the bulk replace and cascade-by-key removal helpers, creating and
     * cleaning up the required FK parents.
     */
    @Test
    public void testAssociation( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Bot bot = createBot( client.getId( ), provider.getId( ) );
        McpServer mcpServer = createMcpServer( "Assoc MCP Server" );

        BotMcpServer association = new BotMcpServer( bot.getId( ), mcpServer.getId( ) );
        BotMcpServerHome.associate( association );

        List<Integer> serverIds = BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) );
        assertTrue( serverIds.contains( mcpServer.getId( ) ) );

        List<Integer> botIds = BotMcpServerHome.getBotIdsByMcpServerId( mcpServer.getId( ) );
        assertTrue( botIds.contains( bot.getId( ) ) );

        BotMcpServerHome.dissociate( association );
        assertFalse( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).contains( mcpServer.getId( ) ) );

        BotMcpServerHome.associateMcpServers( bot.getId( ), List.of( mcpServer.getId( ) ) );
        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).contains( mcpServer.getId( ) ) );

        BotMcpServerHome.associateMcpServers( bot.getId( ), null );
        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).isEmpty( ) );

        BotMcpServerHome.associate( association );
        BotMcpServerHome.removeByMcpServerId( mcpServer.getId( ) );
        assertTrue( BotMcpServerHome.getBotIdsByMcpServerId( mcpServer.getId( ) ).isEmpty( ) );

        BotMcpServerHome.associate( association );
        BotMcpServerHome.removeByBotId( bot.getId( ) );
        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).isEmpty( ) );
    }

    /**
     * A replacement batch that fails mid-insert (foreign-key violation on a nonexistent server id) must leave the previous associations untouched: the delete +
     * inserts are atomic.
     */
    @Test
    public void testAssociateMcpServersIsAtomicOnFailure( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Bot bot = createBot( client.getId( ), provider.getId( ) );
        McpServer existing = createMcpServer( "Atomic MCP Server" );

        BotMcpServerHome.associateMcpServers( bot.getId( ), List.of( existing.getId( ) ) );

        assertThrows( Exception.class, ( ) -> BotMcpServerHome.associateMcpServers( bot.getId( ), List.of( existing.getId( ), 999999 ) ),
                "a nonexistent server id must violate the FK and fail the batch" );

        assertTrue( BotMcpServerHome.getMcpServerIdsByBotId( bot.getId( ) ).contains( existing.getId( ) ),
                "a failed replacement batch must not destroy the previous associations" );
    }

    /**
     * Creates a minimal client FK parent.
     *
     * @return The persisted client
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Test Client" );
        client.setCode( "test-mcp-client" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a minimal provider FK parent (reused as both LLM and embedding provider on the bot).
     *
     * @return The persisted provider
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Test Provider" );
        provider.setProviderType( "LLM" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates a minimal bot FK parent.
     *
     * @param nClientId
     *            The client identifier
     * @param nProviderId
     *            The provider identifier used for both LLM and embedding slots
     * @return The persisted bot
     */
    private Bot createBot( int nClientId, int nProviderId )
    {
        Bot bot = new Bot( );
        bot.setBotName( "Test Bot" );
        bot.setClientId( nClientId );
        bot.setLlmProviderId( nProviderId );
        bot.setEmbedProviderId( nProviderId );
        bot.setMaxTokens( 4000 );
        return BotHome.create( bot );
    }

    /**
     * Creates a minimal MCP server FK parent.
     *
     * @param strName
     *            The MCP server name
     * @return The persisted MCP server
     */
    private McpServer createMcpServer( String strName )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( strName );
        mcpServer.setTransportType( "streamable_http" );
        mcpServer.setUrl( "http://localhost:4010/mcp" );
        mcpServer.setEnabled( true );
        return McpServerHome.create( mcpServer );
    }
}
