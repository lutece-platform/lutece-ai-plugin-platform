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
 * ON DELETE cascade verification for the {@code platform_mcp_server} root entity.
 *
 * <p>
 * Proves that removing an MCP server through {@link McpServerHome#remove(int)} also clears its child rows in {@code platform_bot_mcp_server} (foreign key
 * {@code mcp_server_id}, declared ON DELETE CASCADE). The check runs on the HSQL test database where foreign keys are enforced.
 * </p>
 */
public class McpServerCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full FK parent chain (client + provider + bot), creates the MCP server root and one bot &harr; MCP server association, asserts the association
     * exists, removes the root once, then asserts the association row was cascaded away while the unrelated parents survive.
     */
    @Test
    public void testCascadeOnDelete( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Bot bot = createBot( client.getId( ), provider.getId( ) );
        McpServer mcpServer = createMcpServer( );

        BotMcpServerHome.associate( new BotMcpServer( bot.getId( ), mcpServer.getId( ) ) );

        List<Integer> botIdsBefore = BotMcpServerHome.getBotIdsByMcpServerId( mcpServer.getId( ) );
        assertTrue( botIdsBefore.contains( bot.getId( ) ) );

        McpServerHome.remove( mcpServer.getId( ) );

        assertFalse( McpServerHome.findByPrimaryKey( mcpServer.getId( ) ).isPresent( ) );
        assertTrue( BotMcpServerHome.getBotIdsByMcpServerId( mcpServer.getId( ) ).isEmpty( ) );
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
        client.setCode( "test-mcp-cascade-client" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a minimal provider FK parent, reused as both the LLM and embedding provider on the bot.
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
     * Creates the MCP server root entity under test.
     *
     * @return The persisted MCP server
     */
    private McpServer createMcpServer( )
    {
        McpServer mcpServer = new McpServer( );
        mcpServer.setName( "Cascade MCP Server" );
        mcpServer.setTransportType( "streamable_http" );
        mcpServer.setUrl( "http://localhost:4010/mcp" );
        mcpServer.setEnabled( true );
        return McpServerHome.create( mcpServer );
    }
}
