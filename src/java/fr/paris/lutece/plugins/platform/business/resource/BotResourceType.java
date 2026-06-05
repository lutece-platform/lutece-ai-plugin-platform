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
package fr.paris.lutece.plugins.platform.business.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.api.user.User;

@ApplicationScoped
@Named( "platform.botResourceType" )
public class BotResourceType implements IPlatformResourceType
{
    public static final String PROPERTY_RESOURCE_NAME = "platform.agent.resource.name";
    public static final String PROPERTY_RESOURCE_DESCRIPTION = "platform.agent.resource.description";

    private static final String JSP_FO_BASE_URL = "jsp/site/Portal.jsp";
    private static final String JSP_MANAGE_PROVIDERS_URL = "jsp/admin/plugins/platform/ManageAgentProviders.jsp";
    private static final String JSP_MANAGE_MCP_SERVERS_URL = "jsp/admin/plugins/platform/ManageAgentMcpServers.jsp";
    private static final String VIEW_VIEW_BOT = "page=agent_bot&view=viewBot&bot_id=";
    private static final String VIEW_CREATE_BOT_WITH_CLIENT = "page=agent_bot&view=createBot&client_id=";
    private static final String VIEW_MANAGE_PROVIDERS = "view=manageProviders";
    private static final String VIEW_MANAGE_MCP_SERVERS = "view=manageMcpServers";
    private static final String VIEW_RESOURCE_OBSERVABILITY = "page=agent_observability&view=resourceObservability";
    private static final String URL_SEPARATOR = "?";
    private static final String PROVIDER_CONFIG_NAME = "Gestion des fournisseurs";
    private static final String PROVIDER_CONFIG_DESCRIPTION = "Configurer les fournisseurs LLM et d'embeddings";
    private static final String PROVIDER_CONFIG_ICON = "database";
    private static final String MCP_SERVER_CONFIG_NAME = "Serveurs MCP";
    private static final String MCP_SERVER_CONFIG_DESCRIPTION = "Configurer les serveurs MCP (Model Context Protocol) utilisables comme tools par les bots";
    private static final String MCP_SERVER_CONFIG_ICON = "plug";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceType( )
    {
        return Bot.RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey( )
    {
        return PROPERTY_RESOURCE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptionKey( )
    {
        return PROPERTY_RESOURCE_DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIcon( )
    {
        return "message-chatbot";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( )
    {
        List<Bot> listBots = BotHome.getBotsList( );
        return listBots.stream( ).map( this::convertToResource ).filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformResourceItem getResource( int nIdResource, int nClientId, User user )
    {
        Bot bot = BotHome.findByPrimaryKey( nIdResource ).orElse( null );
        if ( bot != null && AgentRBACService.canViewBot( bot, user ) )
        {
            return convertToResource( bot, user );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( int clientId, User user )
    {
        List<Bot> listBots = BotHome.getBotsListByClientId( clientId );
        return listBots.stream( ).filter( bot -> AgentRBACService.canViewBot( bot, user ) ).map( bot -> convertToResource( bot, user ) )
                .filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * Converts a Bot object to a PlatformResourceItem
     *
     * @param bot
     *            the bot to convert
     * @return the converted PlatformResourceItem or null if bot is null
     */
    private PlatformResourceItem convertToResource( Bot bot )
    {
        if ( bot == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( bot.getId( ) );
        resource.setName( bot.getBotName( ) );
        resource.setDescription( bot.getBotDescription( ) );
        resource.setType( Bot.RESOURCE_TYPE );
        resource.setClientId( bot.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_VIEW_BOT + bot.getId( ) );
        resource.setIcon( bot.getLogoBase64( ) );

        return resource;
    }

    /**
     * Converts a Bot object to a PlatformResourceItem with RBAC permissions
     *
     * @param bot
     *            the bot to convert
     * @param user
     *            the current user for RBAC permissions
     * @return the converted PlatformResourceItem with permissions or null if bot is null
     */
    private PlatformResourceItem convertToResource( Bot bot, User user )
    {
        if ( bot == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( bot.getId( ) );
        resource.setName( bot.getBotName( ) );
        resource.setDescription( bot.getBotDescription( ) );
        resource.setType( Bot.RESOURCE_TYPE );
        resource.setClientId( bot.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_VIEW_BOT + bot.getId( ) );
        resource.setIcon( bot.getLogoBase64( ) );

        resource.setUserCanView( AgentRBACService.canViewBot( bot, user ) );
        resource.setUserCanModify( AgentRBACService.canModifyBot( bot, user ) );
        resource.setUserCanDelete( AgentRBACService.canDeleteBot( bot, user ) );

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreateUrl( String idClient )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_CREATE_BOT_WITH_CLIENT + idClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceConfigItem> getConfigList( User user )
    {
        List<PlatformResourceConfigItem> configItems = new ArrayList<>( );

        PlatformResourceConfigItem providerConfig = new PlatformResourceConfigItem( );
        providerConfig.setName( PROVIDER_CONFIG_NAME );
        providerConfig.setDescription( PROVIDER_CONFIG_DESCRIPTION );
        providerConfig.setUrl( JSP_MANAGE_PROVIDERS_URL + URL_SEPARATOR + VIEW_MANAGE_PROVIDERS );
        providerConfig.setIcon( PROVIDER_CONFIG_ICON );
        configItems.add( providerConfig );

        PlatformResourceConfigItem mcpServerConfig = new PlatformResourceConfigItem( );
        mcpServerConfig.setName( MCP_SERVER_CONFIG_NAME );
        mcpServerConfig.setDescription( MCP_SERVER_CONFIG_DESCRIPTION );
        mcpServerConfig.setUrl( JSP_MANAGE_MCP_SERVERS_URL + URL_SEPARATOR + VIEW_MANAGE_MCP_SERVERS );
        mcpServerConfig.setIcon( MCP_SERVER_CONFIG_ICON );
        configItems.add( mcpServerConfig );

        return configItems;
    }

    @Override
    public String getObservabilityListUrl( int nClientId, String strResourceId )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_RESOURCE_OBSERVABILITY + "&client_id=" + nClientId + "&resource_type=" + Bot.RESOURCE_TYPE
                + "&resource_id=" + strResourceId;
    }

    @Override
    public String getSkinExecutionDetailTemplatePath( )
    {
        return "/skin/plugins/platform/observability/bot_execution_details.html";
    }
}
