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
package fr.paris.lutece.plugins.platform.web;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.portal.service.util.AppPathService;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.web.dto.ProviderOption;
import fr.paris.lutece.plugins.platform.service.conversation.FeedbackService;
import fr.paris.lutece.portal.service.message.SiteMessage;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.message.SiteMessageService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.util.url.UrlItem;

/**
 * Abstract base class for the Platform Agent XPage hierarchy. Contains shared constants used across multiple feature layers, the feedback service field, and
 * shared protected helpers.
 */
public abstract class AbstractAgentXPage extends MVCApplication
{
    protected static final String VIEW_VIEW_BOT = "viewBot";

    protected static final String PARAMETER_CLIENT_ID = "client_id";
    protected static final String PARAMETER_BOT_ID = "bot_id";
    protected static final String PARAMETER_PIPELINE_ID = "id_pipeline";
    protected static final String PARAMETER_VERSION_ID = "id_version";
    protected static final String PARAMETER_DATASET_ID = "dataset_id";
    protected static final String PARAMETER_DOCUMENT_ID = "document_id";
    protected static final String PARAMETER_VISION_ID = "vision_id";
    protected static final String PARAMETER_EXTRACTOR_ID = "extractor_id";
    protected static final String PARAMETER_FIELD_ID = "field_id";
    protected static final String PARAMETER_MODEL_ID = "model_id";

    protected static final String MESSAGE_ACCESS_DENIED = "platform.agent.error.access.denied";
    protected static final String MESSAGE_INVALID_REQUEST = "platform.agent.error.invalidRequest";

    protected static final String URL_PORTAL = "jsp/site/Portal.jsp";
    protected static final String URL_VIEW_CLIENT = "jsp/site/Portal.jsp?page=platform&view=viewClient&id=";

    protected static final String MARK_APP_BASE_URL = "app_base_url";
    protected static final String MARK_BOT = "bot";
    protected static final String MARK_PIPELINE = "pipeline";
    protected static final String MARK_DATASET = "dataset";
    protected static final String MARK_VISION = "vision";
    protected static final String MARK_MODEL = "model";
    protected static final String MARK_USER = "user";
    protected static final String MARK_CLIENT_ID = "client_id";
    protected static final String MARK_STATS = "stats";
    protected static final String MARK_HEADER_CURRENT_VIEW = "header_current_view";
    protected static final String MARK_IS_PUBLISHED = "is_published";
    protected static final String MARK_SUBSCRIPTION = "subscription";
    protected static final String MARK_LLM_PROVIDERS = "llm_providers";
    protected static final String MARK_EMBED_PROVIDERS = "embed_providers";
    protected static final String MARK_DATASETS_LOCAL = "datasets_local";
    protected static final String MARK_DATASETS_EXTERNAL = "datasets_external";
    protected static final String MARK_SELECTED_DATASETS = "selected_datasets";
    protected static final String MARK_PIPELINES = "pipelines";
    protected static final String MARK_SELECTED_PIPELINES = "selected_pipelines";
    protected static final String MARK_MCP_SERVERS = "mcp_servers";
    protected static final String MARK_SELECTED_MCP_SERVERS = "selected_mcp_servers";

    protected static final String PROVIDER_TYPE_LLM = "llm";
    protected static final String PROVIDER_TYPE_EMBEDDING = "embedding";

    @Inject
    protected FeedbackService _feedbackService;

    /**
     * Returns the authenticated FO user, or throws {@link UserNotSignedException} if no user is bound to the session. Lutece-core catches that exception and
     * redirects to the login page automatically. Use this in every XPage view that requires authentication.
     *
     * @param request
     *            the HTTP request
     * @return the authenticated user (never null)
     * @throws UserNotSignedException
     *             if no user is signed in (triggers redirect to login)
     */
    protected LuteceUser requireUser( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );
        if ( user == null )
        {
            throw new UserNotSignedException( );
        }
        return user;
    }

    /**
     * Reads a required integer request parameter, stopping the request with an invalid-request site message when the parameter is missing or not a number
     * (instead of crashing with an unhandled NumberFormatException).
     *
     * @param request
     *            the HTTP request
     * @param strParamName
     *            the parameter name
     * @return the parsed integer value
     * @throws SiteMessageException
     *             when the parameter is missing or malformed
     */
    protected int requireIntParam( HttpServletRequest request, String strParamName ) throws SiteMessageException
    {
        String strValue = request.getParameter( strParamName );
        if ( strValue == null || !strValue.matches( "-?\\d+" ) )
        {
            SiteMessageService.setMessage( request, MESSAGE_INVALID_REQUEST, SiteMessage.TYPE_ERROR );
        }
        return Integer.parseInt( strValue );
    }

    /**
     * Shows a confirmation site message before executing a remove action.
     *
     * @param request
     *            the HTTP request
     * @param strXPageName
     *            the xpage name for the confirmation action URL
     * @param strActionRemove
     *            the action name to invoke on confirmation
     * @param strMessageKey
     *            the i18n message key
     * @param paramNames
     *            parameter names to propagate to the confirmation URL
     * @return null (site message is set)
     * @throws SiteMessageException
     *             always
     */
    protected XPage confirmRemove( HttpServletRequest request, String strXPageName, String strActionRemove, String strMessageKey, String... paramNames )
            throws UserNotSignedException, SiteMessageException
    {
        UrlItem url = new UrlItem( "jsp/site/Portal.jsp" );
        url.addParameter( "page", strXPageName );
        url.addParameter( "action", strActionRemove );

        for ( String paramName : paramNames )
        {
            String value = request.getParameter( paramName );
            if ( value != null )
            {
                url.addParameter( paramName, value );
            }
        }

        SiteMessageService.setMessage( request, strMessageKey, SiteMessage.TYPE_CONFIRMATION, url.getUrl( ) );
        return null;
    }

    /**
     * Redirects to the read-only view of a resource, preserving the client_id parameter. Use this when the user lacks MODIFY/DELETE permission but can still
     * VIEW the resource.
     *
     * @param request
     *            the HTTP request
     * @param strViewName
     *            the target view name
     * @param strResourceParam
     *            the resource parameter name (e.g. "bot_id", "tree_id")
     * @param nResourceId
     *            the resource primary key
     * @return the redirect XPage
     */
    protected XPage redirectToResourceView( HttpServletRequest request, String strViewName, String strResourceParam, int nResourceId )
            throws UserNotSignedException
    {
        Map<String, String> params = new HashMap<>( );
        params.put( strResourceParam, String.valueOf( nResourceId ) );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        if ( strClientId != null )
        {
            params.put( PARAMETER_CLIENT_ID, strClientId );
        }
        return redirect( request, strViewName, params );
    }

    /**
     * Displays a SiteMessage error page and redirects to the portal. Use this when the resource is not found or the user has no VIEW permission.
     *
     * @param request
     *            the HTTP request
     * @param strMessageKey
     *            the i18n message key
     * @return null (never actually returned — exception is thrown)
     * @throws SiteMessageException
     *             always
     */
    protected XPage redirectToPortalWithMessage( HttpServletRequest request, String strMessageKey ) throws UserNotSignedException, SiteMessageException
    {
        SiteMessageService.setMessage( request, strMessageKey, SiteMessage.TYPE_STOP, URL_PORTAL );
        return null;
    }

    /**
     * Redirects to the client view in the platform XPage. Builds an absolute URL using the base URL to support deployment under a context path.
     *
     * @param request
     *            the HTTP request
     * @param nClientId
     *            the client primary key
     * @return the redirect XPage
     */
    protected XPage redirectToClientView( HttpServletRequest request, int nClientId ) throws UserNotSignedException
    {
        return redirect( request, AppPathService.getBaseUrl( request ) + URL_VIEW_CLIENT + nClientId );
    }

    /**
     * Adds LLM and embedding providers to the model.
     *
     * @param model
     *            the template model to update
     */
    protected void addProvidersToModel( Models model )
    {
        model.put( MARK_LLM_PROVIDERS, toProviderOptions( ProviderHome.getProvidersByType( PROVIDER_TYPE_LLM ) ) );
        model.put( MARK_EMBED_PROVIDERS, toProviderOptions( ProviderHome.getProvidersByType( PROVIDER_TYPE_EMBEDDING ) ) );
    }

    /**
     * Maps provider entities to view options exposing only public fields, so the deployment endpoint and API key never reach the front-office data model.
     *
     * @param providers
     *            the provider entities
     * @return the corresponding list of safe provider options
     */
    private static List<ProviderOption> toProviderOptions( List<Provider> providers )
    {
        return providers.stream( ).map( ProviderOption::new ).collect( Collectors.toList( ) );
    }

    /**
     * Adds bot sidebar data to the template model.
     *
     * @param model
     *            the template model to update
     * @param bot
     *            the bot whose sidebar is being rendered
     * @param currentView
     *            the current view name used to highlight the active sidebar item
     */
    protected void addBotSidebarToModel( Models model, Bot bot, String currentView )
    {
        model.put( MARK_HEADER_CURRENT_VIEW, currentView );
        addPublicationStatusToModel( model, Bot.RESOURCE_TYPE, bot.getId( ) );
    }

    /**
     * Adds publication status information to the template model. Sets {@code MARK_IS_PUBLISHED} and, when published, also sets {@code MARK_SUBSCRIPTION} with
     * the first active subscription found.
     *
     * @param model
     *            the template model to update
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource primary key
     */
    protected void addPublicationStatusToModel( Models model, String resourceType, int resourceId )
    {
        List<Subscription> subscriptions = SubscriptionHome.getSubscriptionsByResource( resourceType, String.valueOf( resourceId ) );
        boolean isPublished = subscriptions.stream( ).anyMatch( s -> SubscriptionStatus.ACTIVE == s.getStatus( ) );
        model.put( MARK_IS_PUBLISHED, isPublished );
        if ( isPublished )
        {
            subscriptions.stream( ).filter( s -> SubscriptionStatus.ACTIVE == s.getStatus( ) ).findFirst( ).ifPresent( s -> model.put( MARK_SUBSCRIPTION, s ) );
        }
    }

}
