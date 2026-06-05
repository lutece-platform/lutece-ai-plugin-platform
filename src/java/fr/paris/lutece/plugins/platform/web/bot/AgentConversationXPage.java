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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.conversation.ConversationHistoryDTO;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.service.bot.BotQueryService;
import fr.paris.lutece.plugins.platform.service.conversation.dto.ConversationView;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

/**
 * XPage handling Conversation and Feedback views.
 */
@RequestScoped
@Named( "platform.xpage.agent_conversation" )
@Controller( xpageName = "agent_conversation", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentConversationXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    @Inject
    private BotQueryService _botQueryService;

    protected static final String TEMPLATE_VIEW_CONVERSATION = "/skin/plugins/platform/view_conversation.html";
    protected static final String TEMPLATE_MANAGE_FEEDBACKS = "/skin/plugins/platform/manage_feedbacks.html";

    protected static final String PARAMETER_CONVERSATION_UUID = "conversation_uuid";
    protected static final String PARAMETER_STATUS = "status";
    protected static final String PARAMETER_FEEDBACK_ID = "feedback_id";

    protected static final String MARK_CONVERSATION_HISTORY = "conversation_history";
    protected static final String MARK_FEEDBACKS = "feedbacks";
    protected static final String MARK_SELECTED_BOT_ID = "selected_bot_id";
    protected static final String MARK_CURRENT_STATUS = "current_status";

    protected static final String VIEW_VIEW_CONVERSATION = "viewConversation";
    protected static final String VIEW_MANAGE_FEEDBACKS = "manageFeedbacks";

    protected static final String ACTION_MARK_FEEDBACK_PROCESSED = "markFeedbackProcessed";

    protected static final String MESSAGE_BOT_NOT_FOUND = "platform.agent.error.bot.not.found";

    /**
     * Returns the conversation timeline view. Delegates resolution, history aggregation and the RBAC view check to {@link BotQueryService}, then only places
     * the typed result in the model.
     *
     * @param request
     *            the HTTP request
     * @return the XPage for the conversation view
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    @View( value = VIEW_VIEW_CONVERSATION, defaultView = true )
    public XPage viewConversation( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        Models model = _models;

        String strConversationUuid = request.getParameter( PARAMETER_CONVERSATION_UUID );
        if ( strConversationUuid != null && !strConversationUuid.isEmpty( ) )
        {
            LuteceUser user = requireUser( request );
            ConversationView view = _botQueryService.loadConversationView( strConversationUuid, user );

            if ( view.isAccessGranted( ) )
            {
                model.put( MARK_BOT, view.getBot( ) );
                addBotSidebarToModel( model, view.getBot( ), VIEW_VIEW_CONVERSATION );
            }
            model.put( MARK_CONVERSATION_HISTORY, new ConversationHistoryDTO( strConversationUuid, view.getMessages( ) ) );
        }

        return getXPage( TEMPLATE_VIEW_CONVERSATION, locale, model );
    }

    /**
     * Returns the feedbacks management view. The status-filtered, conversation-enriched feedback list is built entirely by {@link #_feedbackService}.
     *
     * @param request
     *            the HTTP request
     * @return the XPage for the feedbacks management view
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    @View( VIEW_MANAGE_FEEDBACKS )
    public XPage manageFeedbacks( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        Models model = _models;

        Bot bot = loadBotForModel( request, model, VIEW_MANAGE_FEEDBACKS );
        if ( bot != null )
        {
            String strStatus = request.getParameter( PARAMETER_STATUS );
            ConversationMessageFeedback.Status status = null;
            if ( strStatus != null && !strStatus.trim( ).isEmpty( ) )
            {
                status = ConversationMessageFeedback.Status.fromValue( strStatus );
                model.put( MARK_CURRENT_STATUS, strStatus );
            }
            model.put( MARK_FEEDBACKS, _feedbackService.getEnrichedFeedbacksForBot( bot.getId( ), status ) );
        }

        return getXPage( TEMPLATE_MANAGE_FEEDBACKS, locale, model );
    }

    /**
     * Marks a feedback as processed. The authorization rule and the status update are delegated to {@link #_feedbackService}; the controller only maps the
     * typed exceptions to a user message and redirects.
     *
     * @param request
     *            the HTTP request
     * @return redirect to the feedbacks view
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    @Action( ACTION_MARK_FEEDBACK_PROCESSED )
    public XPage doMarkFeedbackProcessed( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        String strFeedbackId = request.getParameter( PARAMETER_FEEDBACK_ID );
        String strBotId = request.getParameter( PARAMETER_BOT_ID );

        Integer nFeedbackId = parseIntOrNull( strFeedbackId );
        Integer nBotId = parseIntOrNull( strBotId );
        if ( nFeedbackId != null && nBotId != null )
        {
            LuteceUser user = requireUser( request );
            try
            {
                _feedbackService.markFeedbackProcessed( nFeedbackId, nBotId, user );
            }
            catch( AccessDeniedException e )
            {
                addError( MESSAGE_ACCESS_DENIED, locale );
            }
            catch( InvalidRequestException e )
            {
                addError( MESSAGE_BOT_NOT_FOUND, locale );
            }

            Map<String, String> params = new HashMap<>( );
            params.put( PARAMETER_BOT_ID, strBotId );
            return redirect( request, VIEW_MANAGE_FEEDBACKS, params );
        }

        return redirect( request, VIEW_MANAGE_FEEDBACKS );
    }

    /**
     * Resolves the bot from the {@code bot_id} request parameter, enriches it with the user permissions and places it together with its sidebar in the model.
     * Shared load + RBAC enrichment + model preamble for the feedback views.
     *
     * @param request
     *            the HTTP request
     * @param model
     *            the template model to populate
     * @param strCurrentView
     *            the current view name used to highlight the active sidebar item
     * @return the enriched bot, or {@code null} when no valid bot is found
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    private Bot loadBotForModel( HttpServletRequest request, Models model, String strCurrentView ) throws UserNotSignedException
    {
        Integer nBotId = parseIntOrNull( request.getParameter( PARAMETER_BOT_ID ) );
        if ( nBotId == null )
        {
            return null;
        }

        model.put( MARK_SELECTED_BOT_ID, nBotId );
        LuteceUser user = requireUser( request );
        Bot bot = _botQueryService.loadEnrichedBot( nBotId, user );
        if ( bot != null )
        {
            model.put( MARK_BOT, bot );
            addBotSidebarToModel( model, bot, strCurrentView );
        }
        return bot;
    }

    /**
     * Parses an integer request parameter, returning {@code null} when it is missing or not a number.
     *
     * @param strValue
     *            the raw parameter value
     * @return the parsed integer, or {@code null}
     */
    private static Integer parseIntOrNull( String strValue )
    {
        if ( strValue == null || strValue.trim( ).isEmpty( ) )
        {
            return null;
        }
        try
        {
            return Integer.valueOf( strValue.trim( ) );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }
}
