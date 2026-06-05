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
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.web.observability.ObservabilityViewMarks;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;
import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientObservabilityView;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientResourcesView;
import fr.paris.lutece.plugins.platform.service.observability.dto.DateRange;
import fr.paris.lutece.plugins.platform.service.observability.dto.GlobalObservabilityStats;
import fr.paris.lutece.plugins.platform.service.resource.PlatformResourceService;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.message.SiteMessage;
import fr.paris.lutece.portal.service.message.SiteMessageService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.ISecurityTokenService;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.util.url.UrlItem;

/**
 * Front-office XPage controller for the Platform plugin. Provides client management, resource viewing, and observability dashboards.
 */
@RequestScoped
@Named( "platform.xpage.platform" )
@Controller( xpageName = "platform", pageTitleI18nKey = "platform.xpage.pageTitle", pagePathI18nKey = "platform.xpage.pagePathLabel" )
public class PlatformXPage extends AbstractPlatformFrontXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_LIST_CLIENTS = "/skin/plugins/platform/list_clients.html";
    private static final String TEMPLATE_VIEW_CLIENT = "/skin/plugins/platform/view_client.html";
    private static final String TEMPLATE_CLIENT_OBSERVABILITY = "/skin/plugins/platform/client_observability.html";
    private static final String TEMPLATE_GLOBAL_OBSERVABILITY = "/skin/plugins/platform/global_observability.html";
    private static final String TEMPLATE_EXECUTION_DETAILS = "/skin/plugins/platform/execution_details.html";
    private static final String TEMPLATE_CREATE_CLIENT = "/skin/plugins/platform/create_client.html";
    private static final String TEMPLATE_MODIFY_CLIENT = "/skin/plugins/platform/modify_client.html";

    private static final String PARAMETER_ID_CLIENT = "id";
    private static final String PARAMETER_ACTIVE_TAB = "active_tab";
    private static final String PARAMETER_CLIENT_ID = "client_id";
    private static final String PARAMETER_ID_SUBSCRIPTION = "id_subscription";

    private static final String MARK_RESOURCE_TYPE_LIST = "resource_type_list";
    private static final String MARK_RESOURCE_ITEM_LIST = "resource_item_list";
    private static final String MARK_SUBSCRIPTIONS = "subscriptions";
    private static final String MARK_SUBSCRIPTIONS_BY_TYPE_AND_ID = "subscriptions_by_type_and_id";
    private static final String MARK_ACTIVE_TAB = "active_tab";
    private static final String MARK_OBSERVABILITY_URLS = "observability_urls";
    private static final String MARK_GLOBAL_SUCCESS_RATE = "global_success_rate";
    private static final String MARK_CLIENT_SERIES_JSON = "client_series_json";
    private static final String MARK_CLIENT_STATS = "client_stats";

    private static final String VIEW_LIST_CLIENTS = "listClients";
    private static final String VIEW_VIEW_CLIENT = "viewClient";
    private static final String VIEW_CLIENT_OBSERVABILITY = "clientObservability";
    private static final String VIEW_GLOBAL_OBSERVABILITY = "globalObservability";
    private static final String VIEW_CREATE_CLIENT = "createClient";
    private static final String VIEW_MODIFY_CLIENT = "modifyClient";

    private static final String ACTION_CREATE_CLIENT = "doCreateClient";
    private static final String ACTION_MODIFY_CLIENT = "doModifyClient";
    private static final String ACTION_REGENERATE_CLIENT_KEY = "doRegenerateClientKey";
    private static final String ACTION_CONFIRM_REMOVE_CLIENT = "confirmRemoveClient";
    private static final String ACTION_REMOVE_CLIENT = "doRemoveClient";
    private static final String ACTION_SUBSCRIBE_TO_RESOURCE = "doSubscribeToResource";
    private static final String ACTION_CANCEL_SUBSCRIPTION = "doCancelSubscription";

    private static final String INFO_CLIENT_CREATED = "platform.info.client.created";
    private static final String INFO_CLIENT_UPDATED = "platform.info.client.updated";
    private static final String INFO_CLIENT_REMOVED = "platform.info.client.removed";
    private static final String INFO_CLIENT_KEY_REGENERATED = "platform.info.client.keyRegenerated";
    private static final String SESSION_NEW_API_KEY = "platform_new_api_key";
    private static final String MARK_NEW_API_KEY = "new_api_key";
    private static final String MARK_REGENERATE_TOKEN = "regenerate_token";
    private static final String INFO_SUBSCRIPTION_CREATED = "platform.info.subscription.created";
    private static final String INFO_SUBSCRIPTION_CANCELLED = "platform.info.subscription.cancelled";
    private static final String MESSAGE_CONFIRM_REMOVE_CLIENT = "platform.message.confirmRemoveClient";
    private static final String MESSAGE_INVALID_TOKEN = "platform.message.error.security.token";

    @Inject
    private ClientService _clientService;

    @Inject
    private PlatformResourceService _platformResourceService;

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private SubscriptionService _subscriptionService;

    @Inject
    private ISecurityTokenService _securityTokenService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Display the list of clients accessible to the current user
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the client list
     * @throws UserNotSignedException
     *             If user is not authenticated
     */
    @View( value = VIEW_LIST_CLIENTS, defaultView = true )
    public XPage getListClients( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = beginView( request );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT_LIST, _clientService.getAuthorizedClientsEnriched( user ) );
        consumeNewApiKey( request, model );

        XPage page = getXPage( TEMPLATE_LIST_CLIENTS, getLocale( request ), model );
        page.setTitle( I18nService.getLocalizedString( "platform.xpage.pageTitle", getLocale( request ) ) );
        return page;
    }

    /**
     * Display a client's resources organized by type in tabs
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the client view
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @View( VIEW_VIEW_CLIENT )
    public XPage getViewClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        Client client = _clientService.getClient( nId, user );
        _clientService.enrichClientWithPermissions( client, user );

        ClientResourcesView view = _observabilityService.getClientResourcesView( nId, user );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT, client );
        model.put( MARK_SUBSCRIPTIONS, view.getSubscriptions( ) );
        model.put( MARK_SUBSCRIPTIONS_BY_TYPE_AND_ID, view.getSubscriptionsByTypeAndId( ) );
        model.put( MARK_RESOURCE_TYPE_LIST, view.getResourceTypes( ) );
        model.put( MARK_RESOURCE_ITEM_LIST, view.getResourcesByType( ) );
        model.put( MARK_ACTIVE_TAB, request.getParameter( PARAMETER_ACTIVE_TAB ) );

        XPage page = getXPage( TEMPLATE_VIEW_CLIENT, getLocale( request ), model );
        page.setTitle( client.getName( ) );
        return page;
    }

    /**
     * Display the observability dashboard for a client
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the observability dashboard
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @View( VIEW_CLIENT_OBSERVABILITY )
    public XPage getClientObservability( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        DateRange range = resolveRange( request );

        Client client = _clientService.getClient( nId, user );
        _clientService.checkObservabilityPermission( nId, user );
        _clientService.enrichClientWithPermissions( client, user );

        ClientObservabilityView view = _observabilityService.getClientObservabilityView( nId, user, range );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT, client );
        model.put( MARK_RESOURCE_TYPE_LIST, view.getResourceTypes( ) );
        model.put( MARK_RESOURCE_ITEM_LIST, view.getResourcesByType( ) );
        model.put( MARK_OBSERVABILITY_URLS, view.getObservabilityUrlsByType( ) );
        model.put( ObservabilityViewMarks.MARK_DATE_RANGE, range.getKey( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_EXECUTIONS, view.getTotalExecutions( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_COST, view.getTotalCost( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_ERRORS, view.getTotalErrors( ) );
        model.put( MARK_GLOBAL_SUCCESS_RATE, view.getGlobalSuccessRate( ) );
        model.put( ObservabilityViewMarks.MARK_AVG_DURATION, view.getAvgDurationSeconds( ) );
        model.put( ObservabilityViewMarks.MARK_DAILY_STATS_JSON, toJson( view.getDailyPoints( ) ) );

        XPage page = getXPage( TEMPLATE_CLIENT_OBSERVABILITY, getLocale( request ), model );
        page.setTitle( I18nService.getLocalizedString( "platform.xpage.observability.title", getLocale( request ) ) );
        return page;
    }

    /**
     * Builds the global observability page aggregating statistics across all clients authorized for the current user
     *
     * @param request
     *            the HTTP request
     * @return the global observability XPage
     * @throws UserNotSignedException
     *             if the user is not authenticated
     * @throws AccessDeniedException
     *             if the user is not authorized to access the page
     */
    @View( VIEW_GLOBAL_OBSERVABILITY )
    public XPage getGlobalObservability( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        DateRange range = resolveRange( request );
        Collection<Client> clients = _clientService.getAuthorizedClients( user );
        Timestamp [ ] bounds = range == DateRange.ALL ? null : range.resolveBounds( );

        GlobalObservabilityStats stats = _observabilityService.getGlobalStats( clients, bounds != null ? bounds [0] : null,
                bounds != null ? bounds [1] : null );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT_LIST, clients );
        model.put( MARK_CLIENT_STATS, stats.getClientStats( ) );
        model.put( ObservabilityViewMarks.MARK_DATE_RANGE, range.getKey( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_EXECUTIONS, stats.getTotalExecutions( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_COST, stats.getTotalCost( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_ERRORS, stats.getTotalErrors( ) );
        model.put( MARK_GLOBAL_SUCCESS_RATE, stats.getGlobalSuccessRate( ) );
        model.put( ObservabilityViewMarks.MARK_AVG_DURATION, stats.getAvgDurationSeconds( ) );
        model.put( MARK_CLIENT_SERIES_JSON, toJson( _observabilityService.buildChartSeries( stats.getClientStats( ) ) ) );

        XPage page = getXPage( TEMPLATE_GLOBAL_OBSERVABILITY, getLocale( request ), model );
        page.setTitle( I18nService.getLocalizedString( "platform.xpage.globalObservability.title", getLocale( request ) ) );
        return page;
    }

    /**
     * Display execution details with node traces
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the execution details
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @View( ObservabilityViewMarks.VIEW_EXECUTION_DETAILS )
    public XPage getExecutionDetails( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        String executionId = request.getParameter( ObservabilityViewMarks.PARAMETER_EXECUTION_ID );
        String resourceType = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE );
        String resourceId = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_ID );

        Client client = _clientService.getClient( nId, user );
        _clientService.checkObservabilityPermission( nId, user );
        _clientService.enrichClientWithPermissions( client, user );

        Optional<PlatformResourceExecution> optExecution = _observabilityService.getExecutionWithDetails( executionId );

        if ( optExecution.isEmpty( ) || client.getId( ) != optExecution.get( ).getClientId( ) )
        {
            return redirectView( request, VIEW_LIST_CLIENTS );
        }

        PlatformResourceExecution execution = optExecution.get( );

        Map<String, Object> metrics = new HashMap<>( );
        metrics.put( "resourceItems", _observabilityService.getResourceItemsForExecutions( Collections.singletonList( execution ) ) );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT, client );
        model.put( ObservabilityViewMarks.MARK_EXECUTION, execution );
        model.put( ObservabilityViewMarks.MARK_METRICS, metrics );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_TYPE, resourceType );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_ID, resourceId );

        String templatePath = getSkinExecutionDetailTemplate( execution.getResourceType( ) );

        XPage page = getXPage( templatePath, getLocale( request ), model );
        page.setTitle( execution.getResourceName( ) + " - " + execution.getExecutionId( ) );
        return page;
    }

    /**
     * Returns the skin execution detail template path for the given resource type, falling back to the default template when none is configured
     *
     * @param resourceType
     *            the resource type key
     * @return the custom skin execution detail template path or the default template path
     */
    private String getSkinExecutionDetailTemplate( String resourceType )
    {
        IPlatformResourceType platformResourceType = _platformResourceService.getResourceType( resourceType );

        if ( platformResourceType != null )
        {
            String customTemplate = platformResourceType.getSkinExecutionDetailTemplatePath( );
            if ( customTemplate != null && !customTemplate.isEmpty( ) )
            {
                return customTemplate;
            }
        }

        return TEMPLATE_EXECUTION_DETAILS;
    }

    /**
     * Display the create client form
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the create client form
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @View( VIEW_CREATE_CLIENT )
    public XPage getCreateClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT, new Client( ) );
        model.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, ACTION_CREATE_CLIENT ) );

        XPage page = getXPage( TEMPLATE_CREATE_CLIENT, getLocale( request ), model );
        page.setTitle( I18nService.getLocalizedString( "platform.create_client.pageTitle", getLocale( request ) ) );
        return page;
    }

    /**
     * Display the modify client form
     *
     * @param request
     *            The HTTP request
     * @return The XPage containing the modify client form
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @View( VIEW_MODIFY_CLIENT )
    public XPage getModifyClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = beginView( request );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        Client client = _clientService.getClient( nId, user );
        _clientService.enrichClientWithPermissions( client, user );

        Models model = newViewModel( user );
        model.put( MARK_CLIENT, client );
        model.put( SecurityTokenService.MARK_TOKEN, _securityTokenService.getToken( request, ACTION_MODIFY_CLIENT ) );
        model.put( MARK_REGENERATE_TOKEN, _securityTokenService.getToken( request, ACTION_REGENERATE_CLIENT_KEY ) );
        consumeNewApiKey( request, model );

        XPage page = getXPage( TEMPLATE_MODIFY_CLIENT, getLocale( request ), model );
        page.setTitle( I18nService.getLocalizedString( "platform.modify_client.pageTitle", getLocale( request ) ) );
        return page;
    }

    /**
     * Process client creation
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_CREATE_CLIENT )
    public XPage doCreateClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );
        validateToken( request, ACTION_CREATE_CLIENT );

        Client client = new Client( );
        populate( client, request );

        if ( !validateBean( client, request.getLocale( ) ) )
        {
            return redirectView( request, VIEW_CREATE_CLIENT );
        }

        String apiKey = _clientService.createClient( client, user );
        request.getSession( true ).setAttribute( SESSION_NEW_API_KEY, apiKey );
        addFlashInfo( request, INFO_CLIENT_CREATED );

        return redirectView( request, VIEW_LIST_CLIENTS );
    }

    /**
     * Regenerates the API key of a client. The previous key stops working immediately; the new key is stored in session for one-time display on the next view.
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_REGENERATE_CLIENT_KEY )
    public XPage doRegenerateClientKey( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );
        validateToken( request, ACTION_REGENERATE_CLIENT_KEY );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        String apiKey = _clientService.regenerateApiKey( nId, user );
        request.getSession( true ).setAttribute( SESSION_NEW_API_KEY, apiKey );
        addFlashInfo( request, INFO_CLIENT_KEY_REGENERATED );

        return redirect( request, VIEW_MODIFY_CLIENT, PARAMETER_ID_CLIENT, nId );
    }

    /**
     * Pops the one-time API key stashed in session by a create or regenerate action, if any, into the model.
     *
     * @param request
     *            The HTTP request
     * @param model
     *            The view model
     */
    private void consumeNewApiKey( HttpServletRequest request, Models model )
    {
        Object apiKey = request.getSession( true ).getAttribute( SESSION_NEW_API_KEY );
        if ( apiKey != null )
        {
            request.getSession( true ).removeAttribute( SESSION_NEW_API_KEY );
            model.put( MARK_NEW_API_KEY, apiKey );
        }
    }

    /**
     * Process client modification
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_MODIFY_CLIENT )
    public XPage doModifyClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );
        validateToken( request, ACTION_MODIFY_CLIENT );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        Client client = _clientService.getClient( nId, user );
        populate( client, request );

        if ( !validateBean( client, request.getLocale( ) ) )
        {
            return redirect( request, VIEW_MODIFY_CLIENT, PARAMETER_ID_CLIENT, client.getId( ) );
        }

        _clientService.updateClient( client, user );
        addFlashInfo( request, INFO_CLIENT_UPDATED );

        return redirect( request, VIEW_VIEW_CLIENT, PARAMETER_ID_CLIENT, client.getId( ) );
    }

    /**
     * Confirm client removal via SiteMessage
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws SiteMessageException
     *             To display the confirmation message
     */
    @Action( ACTION_CONFIRM_REMOVE_CLIENT )
    public XPage getConfirmRemoveClient( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        getUser( request );

        String strId = request.getParameter( PARAMETER_ID_CLIENT );
        UrlItem url = new UrlItem( "jsp/site/Portal.jsp" );
        url.addParameter( "page", "platform" );
        url.addParameter( "action", ACTION_REMOVE_CLIENT );
        url.addParameter( PARAMETER_ID_CLIENT, strId );

        SiteMessageService.setMessage( request, MESSAGE_CONFIRM_REMOVE_CLIENT, SiteMessage.TYPE_CONFIRMATION, url.getUrl( ) );

        return null;
    }

    /**
     * Process client removal
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_REMOVE_CLIENT )
    public XPage doRemoveClient( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );

        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_CLIENT ) );
        _clientService.deleteClient( nId, user );
        addFlashInfo( request, INFO_CLIENT_REMOVED );

        return redirectView( request, VIEW_LIST_CLIENTS );
    }

    /**
     * Subscribe to a resource (publish)
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_SUBSCRIBE_TO_RESOURCE )
    public XPage doSubscribeToResource( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );

        int nClientId = Integer.parseInt( request.getParameter( PARAMETER_CLIENT_ID ) );
        String resourceType = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE );
        String resourceId = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_ID );

        _subscriptionService.subscribe( nClientId, resourceType, resourceId, user );
        addFlashInfo( request, INFO_SUBSCRIPTION_CREATED );

        return redirectToClientTab( request, nClientId );
    }

    /**
     * Cancel a subscription (unpublish)
     *
     * @param request
     *            The HTTP request
     * @return The redirect XPage
     * @throws UserNotSignedException
     *             If user is not authenticated
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    @Action( ACTION_CANCEL_SUBSCRIPTION )
    public XPage doCancelSubscription( HttpServletRequest request ) throws UserNotSignedException, AccessDeniedException
    {
        LuteceUser user = getUser( request );

        int nSubscriptionId = Integer.parseInt( request.getParameter( PARAMETER_ID_SUBSCRIPTION ) );
        int nClientId = _subscriptionService.unsubscribe( nSubscriptionId, user );

        if ( nClientId < 0 )
        {
            return redirectView( request, VIEW_LIST_CLIENTS );
        }

        addFlashInfo( request, INFO_SUBSCRIPTION_CANCELLED );
        return redirectToClientTab( request, nClientId );
    }

    /**
     * Runs the common view preamble: applies pending flash messages and resolves the authenticated user.
     *
     * @param request
     *            The HTTP request
     * @return the authenticated user
     * @throws UserNotSignedException
     *             If user is not authenticated
     */
    private LuteceUser beginView( HttpServletRequest request ) throws UserNotSignedException
    {
        applyFlashMessages( request );
        return getUser( request );
    }

    /**
     * Builds a fresh view model pre-populated with the current user and the RBAC permissions block.
     *
     * @param user
     *            the authenticated user
     * @return the model map
     */
    private Models newViewModel( LuteceUser user )
    {
        Models model = _models;
        model.put( MARK_USER, user );
        addRBACPermissionsToModel( model, user, _clientService );
        return model;
    }

    /**
     * Resolves the requested date range from the request, defaulting to the last three months.
     *
     * @param request
     *            The HTTP request
     * @return the resolved date range, never null
     */
    private DateRange resolveRange( HttpServletRequest request )
    {
        String dateRange = request.getParameter( ObservabilityViewMarks.PARAMETER_DATE_RANGE );
        if ( dateRange == null || dateRange.isEmpty( ) )
        {
            return DateRange.THREE_MONTHS;
        }
        return DateRange.fromKey( dateRange );
    }

    /**
     * Redirects to the client view, preserving the active tab parameter when present.
     *
     * @param request
     *            The HTTP request
     * @param nClientId
     *            the client identifier
     * @return the redirect XPage
     */
    private XPage redirectToClientTab( HttpServletRequest request, int nClientId )
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_ID_CLIENT, String.valueOf( nClientId ) );

        String activeTab = request.getParameter( PARAMETER_ACTIVE_TAB );
        if ( activeTab != null )
        {
            params.put( PARAMETER_ACTIVE_TAB, activeTab );
        }
        return redirect( request, VIEW_VIEW_CLIENT, params );
    }

    /**
     * Validates the CSRF security token for the given action, raising an access-denied error on failure.
     *
     * @param request
     *            The HTTP request
     * @param strAction
     *            the action name the token was issued for
     * @throws AccessDeniedException
     *             If the token is invalid
     */
    private void validateToken( HttpServletRequest request, String strAction ) throws AccessDeniedException
    {
        if ( !_securityTokenService.validate( request, strAction ) )
        {
            throw new AccessDeniedException( I18nService.getLocalizedString( MESSAGE_INVALID_TOKEN, getLocale( request ) ) );
        }
    }

    /**
     * Serializes a typed chart payload to JSON for embedding in a template.
     *
     * @param payload
     *            the typed payload (daily points or chart series)
     * @return the JSON string
     */
    private String toJson( Object payload )
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString( payload );
        }
        catch( JsonProcessingException e )
        {
            throw new IllegalStateException( "Failed to serialize chart payload", e );
        }
    }
}
