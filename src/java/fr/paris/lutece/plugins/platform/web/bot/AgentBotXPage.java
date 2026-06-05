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
package fr.paris.lutece.plugins.platform.web.bot;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.service.bot.BotService;
import fr.paris.lutece.plugins.platform.service.bot.dto.BotDetailDTO;
import fr.paris.lutece.plugins.platform.service.bot.dto.DatasetSelectionDTO;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

/**
 * XPage handling Bot views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_bot" )
@Controller( xpageName = "agent_bot", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentBotXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    @Inject
    private BotService _botService;

    private static final String XPAGE_NAME = "agent_bot";
    protected static final String TEMPLATE_VIEW_BOT = "/skin/plugins/platform/view_bot.html";
    protected static final String TEMPLATE_CREATE_BOT = "/skin/plugins/platform/create_bot.html";
    protected static final String TEMPLATE_MODIFY_BOT = "/skin/plugins/platform/modify_bot.html";
    protected static final String TEMPLATE_API_BOT = "/skin/plugins/platform/api_bot.html";

    protected static final String PARAMETER_BOT_SYSTEM_PROMPT = "bot_system_prompt";
    protected static final String PARAMETER_LOGO_BASE64 = "logo_base64";
    protected static final String PARAMETER_DATASET_IDS = "dataset_ids";
    protected static final String PARAMETER_PIPELINE_IDS = "pipeline_ids";
    protected static final String PARAMETER_MCP_SERVER_IDS = "mcp_server_ids";
    protected static final String PARAMETER_ENABLE_BUILTIN_TOOLS = "enable_builtin_tools";
    private static final String VALUE_TRUE = "true";

    protected static final String MARK_FEEDBACK_COUNT = "feedback_count";

    protected static final String VIEW_CREATE_BOT = "createBot";
    protected static final String VIEW_MODIFY_BOT = "modifyBot";
    protected static final String VIEW_API_BOT = "apiBot";

    protected static final String ACTION_CREATE_BOT = "doCreateBot";
    protected static final String ACTION_MODIFY_BOT = "doModifyBot";
    protected static final String ACTION_CONFIRM_REMOVE_BOT = "confirmRemoveBot";
    protected static final String ACTION_REMOVE_BOT = "doRemoveBot";

    protected static final String MESSAGE_BOT_NOT_FOUND = "platform.agent.error.bot.not.found";
    protected static final String MESSAGE_CONFIRM_REMOVE_BOT = "platform.agent.message.confirmRemoveBot";

    protected static final String INFO_BOT_CREATED = "platform.agent.info.bot.created";
    protected static final String INFO_BOT_UPDATED = "platform.agent.info.bot.updated";
    protected static final String INFO_BOT_REMOVED = "platform.agent.info.bot.removed";
    private static final String LOG_INVALID_ID = "Invalid id parameter: ";

    /**
     * Returns the bot detail view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( value = VIEW_VIEW_BOT, defaultView = true )
    public XPage viewBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        Bot bot = optBot.get( );
        if ( !bot.isUserCanView( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_BOT, bot );
        model.put( MARK_USER, user );
        model.put( MARK_CLIENT_ID, bot.getClientId( ) );
        addProvidersToModel( model );

        BotDetailDTO detail = _botService.getBotDetail( bot.getId( ), bot.getClientId( ) );
        model.put( MARK_DATASETS_LOCAL, detail.associatedDatasets( ) );
        model.put( MARK_PIPELINES, detail.attachedPipelines( ) );
        model.put( MARK_MCP_SERVERS, detail.attachedMcpServers( ) );
        model.put( MARK_FEEDBACK_COUNT, detail.pendingFeedbackCount( ) );
        detail.stats( ).ifPresent( stats -> model.put( MARK_STATS, stats ) );

        addBotSidebarToModel( model, bot, VIEW_VIEW_BOT );

        return getXPage( TEMPLATE_VIEW_BOT, locale, model );
    }

    /**
     * Returns the bot API documentation view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_API_BOT )
    public XPage apiBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        Bot bot = optBot.get( );
        if ( !bot.isUserCanView( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_BOT, bot );
        model.put( MARK_CLIENT_ID, bot.getClientId( ) );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );

        addBotSidebarToModel( model, bot, VIEW_API_BOT );

        return getXPage( TEMPLATE_API_BOT, locale, model );
    }

    /**
     * Returns the bot creation form.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_CREATE_BOT )
    public XPage createBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateBot( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        int nClientId = ( strClientId != null ) ? Integer.parseInt( strClientId ) : 0;

        Models model = _models;
        model.put( MARK_USER, user );
        model.put( MARK_CLIENT_ID, strClientId );
        addProvidersToModel( model );
        addDatasetsToModel( model, 0, nClientId );
        addMcpServersToModel( model, 0 );

        return getXPage( TEMPLATE_CREATE_BOT, locale, model );
    }

    /**
     * Returns the bot modification form.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_BOT )
    public XPage modifyBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        Bot bot = optBot.get( );
        if ( !bot.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_BOT, PARAMETER_BOT_ID, bot.getId( ) );
        }

        Models model = _models;
        model.put( MARK_BOT, bot );
        model.put( MARK_USER, user );
        addProvidersToModel( model );
        addDatasetsToModel( model, bot.getId( ), bot.getClientId( ) );
        addPipelinesToModel( model, bot.getId( ), bot.getClientId( ) );
        addMcpServersToModel( model, bot.getId( ) );

        addBotSidebarToModel( model, bot, VIEW_MODIFY_BOT );

        return getXPage( TEMPLATE_MODIFY_BOT, locale, model );
    }

    /**
     * Processes the creation of a bot.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_BOT )
    public XPage doCreateBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateBot( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Bot bot = new Bot( );
        populateBotFromRequest( bot, request, true );

        if ( !validateBean( bot, request.getLocale( ) ) )
        {
            return redirect( request, URL_PORTAL );
        }

        _botService.createBotWithAssociations( bot, parseIdList( request.getParameterValues( PARAMETER_DATASET_IDS ) ),
                parseIdList( request.getParameterValues( PARAMETER_MCP_SERVER_IDS ) ) );

        addInfo( INFO_BOT_CREATED, locale );
        return redirectToBotView( request, bot.getId( ) );
    }

    /**
     * Processes the modification of a bot.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_BOT )
    public XPage doModifyBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        Bot bot = optBot.get( );
        if ( !bot.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_BOT, PARAMETER_BOT_ID, bot.getId( ) );
        }

        populateBotFromRequest( bot, request, false );

        if ( !validateBean( bot, request.getLocale( ) ) )
        {
            return redirectToResourceView( request, VIEW_MODIFY_BOT, PARAMETER_BOT_ID, bot.getId( ) );
        }

        _botService.updateBotWithAssociations( bot, parseIdList( request.getParameterValues( PARAMETER_DATASET_IDS ) ),
                parseIdList( request.getParameterValues( PARAMETER_PIPELINE_IDS ) ), parseIdList( request.getParameterValues( PARAMETER_MCP_SERVER_IDS ) ) );

        addInfo( INFO_BOT_UPDATED, locale );
        return redirectToBotView( request, bot.getId( ) );
    }

    /**
     * Handles the confirm remove bot action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_BOT )
    public XPage confirmRemoveBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        if ( !optBot.get( ).isUserCanDelete( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_BOT, MESSAGE_CONFIRM_REMOVE_BOT, PARAMETER_BOT_ID );
    }

    /**
     * Processes the removal of a bot.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_BOT )
    public XPage doRemoveBot( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Bot> optBot = loadAndEnrich( request, user );
        if ( optBot.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_BOT_NOT_FOUND );
        }

        Bot bot = optBot.get( );
        if ( !bot.isUserCanDelete( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_BOT, PARAMETER_BOT_ID, bot.getId( ) );
        }

        int nClientId = bot.getClientId( );
        BotHome.remove( bot.getId( ) );

        addInfo( INFO_BOT_REMOVED, locale );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Loads the bot referenced by the request and enriches it with the user's permissions.
     *
     * @param request
     *            The HTTP request carrying the bot id parameter
     * @param user
     *            The current user whose permissions enrich the bot
     * @return the enriched bot, empty when the id is missing, malformed or matches no bot
     */
    private Optional<Bot> loadAndEnrich( HttpServletRequest request, LuteceUser user )
    {
        int nBotId;
        try
        {
            nBotId = Integer.parseInt( request.getParameter( PARAMETER_BOT_ID ) );
        }
        catch( NumberFormatException e )
        {
            return Optional.empty( );
        }
        Optional<Bot> optBot = BotHome.findByPrimaryKey( nBotId );
        optBot.ifPresent( bot -> AgentRBACService.enrichWithPermissions( bot, user ) );
        return optBot;
    }

    /**
     * Populates a bot from the request parameters, including the system prompt, the base64 logo and, when modifying, the built-in tools flag.
     *
     * @param bot
     *            The bot to populate
     * @param request
     *            The HTTP request
     * @param bCreate
     *            true when creating the bot, false when modifying it
     */
    private void populateBotFromRequest( Bot bot, HttpServletRequest request, boolean bCreate )
    {
        populate( bot, request );

        String systemPrompt = request.getParameter( PARAMETER_BOT_SYSTEM_PROMPT );
        if ( systemPrompt != null )
        {
            bot.setBotSystemPrompt( systemPrompt );
        }

        String logoBase64 = request.getParameter( PARAMETER_LOGO_BASE64 );
        if ( logoBase64 != null )
        {
            bot.setLogoBase64( logoBase64.trim( ).isEmpty( ) ? null : logoBase64 );
        }

        if ( !bCreate )
        {
            bot.setEnableBuiltinTools( VALUE_TRUE.equals( request.getParameter( PARAMETER_ENABLE_BUILTIN_TOOLS ) ) );
        }
    }

    /**
     * Redirects to the bot detail view for the given bot id.
     *
     * @param request
     *            The HTTP request
     * @param nBotId
     *            The bot id
     * @return the redirect XPage
     */
    private XPage redirectToBotView( HttpServletRequest request, int nBotId )
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_BOT_ID, String.valueOf( nBotId ) );
        return redirect( request, VIEW_VIEW_BOT, params );
    }

    /**
     * Adds the available datasets, split into local and external lists, and the bot's currently selected ones to the model.
     *
     * @param model
     *            The template model
     * @param nBotId
     *            The bot id (0 for create)
     * @param nClientId
     *            The client id used to separate local datasets from external ones
     */
    private void addDatasetsToModel( Models model, int nBotId, int nClientId )
    {
        DatasetSelectionDTO selection = _botService.getDatasetSelectionForBot( nBotId, nClientId );
        model.put( MARK_DATASETS_LOCAL, selection.localDatasets( ) );
        model.put( MARK_DATASETS_EXTERNAL, selection.externalDatasets( ) );
        model.put( MARK_SELECTED_DATASETS, selection.selectedDatasetIds( ) );
    }

    /**
     * Adds the available pipelines for the client and the bot's currently selected ones to the model.
     *
     * @param model
     *            The template model
     * @param nBotId
     *            The bot id (0 for create)
     * @param nClientId
     *            The client id whose pipelines are listed
     */
    private void addPipelinesToModel( Models model, int nBotId, int nClientId )
    {
        model.put( MARK_PIPELINES, _botService.getPipelinesForClient( nClientId ) );
        model.put( MARK_SELECTED_PIPELINES, _botService.getSelectedPipelineIds( nBotId ) );
    }

    /**
     * Adds the available MCP servers and the bot's currently selected ones to the model.
     *
     * @param model
     *            The template model
     * @param nBotId
     *            The bot id (0 for create)
     */
    protected void addMcpServersToModel( Models model, int nBotId )
    {
        model.put( MARK_MCP_SERVERS, _botService.getAvailableMcpServers( ) );
        model.put( MARK_SELECTED_MCP_SERVERS, _botService.getSelectedMcpServerIds( nBotId ) );
    }

    /**
     * Parses a request parameter array of integer ids, skipping malformed entries (each one logged). A null array yields an empty list.
     *
     * @param values
     *            The raw parameter values (nullable)
     * @return The parsed ids (never null)
     */
    private List<Integer> parseIdList( String [ ] values )
    {
        List<Integer> ids = new ArrayList<>( );
        if ( values == null )
        {
            return ids;
        }
        for ( String strId : values )
        {
            try
            {
                ids.add( Integer.parseInt( strId.trim( ) ) );
            }
            catch( NumberFormatException e )
            {
                AppLogService.error( "{}{}", LOG_INVALID_ID, strId, e );
            }
        }
        return ids;
    }
}
