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
package fr.paris.lutece.plugins.platform.web.observability;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.dto.DailyStatPoint;
import fr.paris.lutece.plugins.platform.service.observability.dto.DateRange;
import fr.paris.lutece.plugins.platform.service.observability.dto.ResourceObservabilitySummary;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.rbac.PlatformObservabilityResourceIdService;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.plugins.platform.service.resource.PlatformResourceService;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.util.url.UrlItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * XPage handling Observability views.
 */
@RequestScoped
@Named( "platform.xpage.agent_observability" )
@Controller( xpageName = "agent_observability", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentObservabilityXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper JSON = new ObjectMapper( );

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private PlatformResourceService _platformResourceService;

    @Inject
    private IFileStoreServiceProvider _fileStoreServiceProvider;

    private static final String XPAGE_NAME = "agent_observability";

    private static final String MESSAGE_INVALID_REQUEST = "platform.agent.error.observability.invalidRequest";

    protected static final String TEMPLATE_RESOURCE_OBSERVABILITY = "/skin/plugins/platform/resource_observability.html";

    private static final int ITEMS_PER_PAGE = 20;
    protected static final String PARAMETER_PAGE_INDEX = "page_index";

    protected static final String MARK_EXECUTIONS = "executions";
    protected static final String MARK_CLIENT = "client";
    protected static final String MARK_SUCCESS_RATE = "success_rate";
    protected static final String MARK_RESOURCE_ITEMS = "resourceItems";
    protected static final String MARK_DECISION_TREE = "decision_tree";
    protected static final String MARK_PAGINATOR = "paginator";

    protected static final String VIEW_RESOURCE_OBSERVABILITY = "resourceObservability";

    private static final String MARK_FILE_SERVICE = "fileService";

    private static final Map<String, String> HEADER_MARK_BY_TYPE = Map.of( Bot.RESOURCE_TYPE, MARK_BOT, Pipeline.RESOURCE_TYPE, MARK_PIPELINE,
            Dataset.RESOURCE_TYPE, MARK_DATASET, Vision.RESOURCE_TYPE, MARK_VISION, Model.RESOURCE_TYPE, MARK_MODEL, DecisionTree.RESOURCE_TYPE,
            MARK_DECISION_TREE );

    /**
     * Returns the resource observability view, filtered by a specific resource.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( value = VIEW_RESOURCE_OBSERVABILITY, defaultView = true )
    public XPage resourceObservability( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        String resourceType = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE );
        String resourceId = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_ID );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        DateRange range = DateRange.fromKey( request.getParameter( ObservabilityViewMarks.PARAMETER_DATE_RANGE ) );
        String strPageIndex = request.getParameter( PARAMETER_PAGE_INDEX );

        if ( strPageIndex == null || strPageIndex.isEmpty( ) )
        {
            strPageIndex = "1";
        }

        int nClientId;
        int nResourceId;
        try
        {
            nClientId = parseRequiredInt( strClientId );
            nResourceId = parseRequiredInt( resourceId );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_INVALID_REQUEST, locale );
            return redirect( request, AbstractAgentXPage.URL_PORTAL );
        }

        if ( !canViewObservability( nClientId, user ) )
        {
            addError( MESSAGE_INVALID_REQUEST, locale );
            return redirect( request, AbstractAgentXPage.URL_PORTAL );
        }

        Client client = ClientHome.findByPrimaryKey( nClientId ).orElse( null );

        ResourceObservabilitySummary summary = _observabilityService.getResourceSummary( resourceType, resourceId, nClientId, range );
        List<DailyStatPoint> dailyStats = _observabilityService.getDailyStatPoints( resourceType, resourceId, range );

        UrlItem url = new UrlItem( "jsp/site/Portal.jsp" );
        url.addParameter( "page", XPAGE_NAME );
        url.addParameter( "view", VIEW_RESOURCE_OBSERVABILITY );
        url.addParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE, resourceType );
        url.addParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_ID, resourceId );
        url.addParameter( PARAMETER_CLIENT_ID, strClientId );
        url.addParameter( ObservabilityViewMarks.PARAMETER_DATE_RANGE, range.getKey( ) );

        LocalizedPaginator<PlatformResourceExecution> paginator = new LocalizedPaginator<>( summary.getExecutions( ), ITEMS_PER_PAGE, url.getUrl( ),
                PARAMETER_PAGE_INDEX, strPageIndex, locale );

        Map<String, Object> metrics = new HashMap<>( );
        metrics.put( MARK_RESOURCE_ITEMS, _observabilityService.getResourceItemsForExecutions( paginator.getPageItems( ) ) );

        Models model = _models;
        model.put( MARK_CLIENT, client );
        model.put( MARK_EXECUTIONS, paginator.getPageItems( ) );
        model.put( MARK_PAGINATOR, paginator );
        model.put( ObservabilityViewMarks.MARK_METRICS, metrics );
        model.put( ObservabilityViewMarks.MARK_DATE_RANGE, range.getKey( ) );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_TYPE, resourceType );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_ID, resourceId );
        model.put( MARK_CLIENT_ID, nClientId );
        model.put( MARK_USER, user );
        model.put( ObservabilityViewMarks.MARK_TOTAL_EXECUTIONS, summary.getTotalExecutions( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_COST, summary.getTotalCost( ) );
        model.put( MARK_SUCCESS_RATE, summary.getSuccessRate( ) );
        model.put( ObservabilityViewMarks.MARK_TOTAL_ERRORS, summary.getTotalErrors( ) );
        model.put( ObservabilityViewMarks.MARK_AVG_DURATION, summary.getAvgDurationSeconds( ) );
        model.put( ObservabilityViewMarks.MARK_DAILY_STATS_JSON, toJson( dailyStats ) );

        addHeaderForResource( model, resourceType, nResourceId, user, VIEW_RESOURCE_OBSERVABILITY );

        return getXPage( TEMPLATE_RESOURCE_OBSERVABILITY, locale, model );
    }

    /**
     * Displays execution details with node traces and sidebar navigation.
     *
     * @param request
     *            the HTTP request
     * @return the XPage containing the execution details
     */
    @View( ObservabilityViewMarks.VIEW_EXECUTION_DETAILS )
    public XPage executionDetails( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        String executionId = request.getParameter( ObservabilityViewMarks.PARAMETER_EXECUTION_ID );
        String resourceType = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE );
        String resourceId = request.getParameter( ObservabilityViewMarks.PARAMETER_RESOURCE_ID );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        int nClientId;
        int nResourceId;
        try
        {
            nClientId = parseRequiredInt( strClientId );
            nResourceId = parseRequiredInt( resourceId );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_INVALID_REQUEST, locale );
            return redirect( request, AbstractAgentXPage.URL_PORTAL );
        }

        if ( !canViewObservability( nClientId, user ) )
        {
            addError( MESSAGE_INVALID_REQUEST, locale );
            return redirect( request, AbstractAgentXPage.URL_PORTAL );
        }

        Client client = ClientHome.findByPrimaryKey( nClientId ).orElse( null );

        Optional<PlatformResourceExecution> optExecution = _observabilityService.getExecutionWithDetails( executionId );

        if ( optExecution.isEmpty( ) || client == null || nClientId != optExecution.get( ).getClientId( ) )
        {
            Map<String, String> redirectParams = new HashMap<>( );
            redirectParams.put( PARAMETER_CLIENT_ID, strClientId );
            redirectParams.put( ObservabilityViewMarks.PARAMETER_RESOURCE_ID, resourceId );
            if ( resourceType != null )
            {
                redirectParams.put( ObservabilityViewMarks.PARAMETER_RESOURCE_TYPE, resourceType );
            }
            return redirect( request, VIEW_RESOURCE_OBSERVABILITY, redirectParams );
        }

        PlatformResourceExecution execution = optExecution.get( );

        Map<String, Object> metrics = new HashMap<>( );
        metrics.put( MARK_RESOURCE_ITEMS, _observabilityService.getResourceItemsForExecutions( Collections.singletonList( execution ) ) );

        Models model = _models;
        model.put( MARK_CLIENT, client );
        model.put( ObservabilityViewMarks.MARK_EXECUTION, execution );
        model.put( ObservabilityViewMarks.MARK_METRICS, metrics );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_TYPE, resourceType );
        model.put( ObservabilityViewMarks.MARK_RESOURCE_ID, resourceId );
        model.put( MARK_USER, user );
        model.put( MARK_FILE_SERVICE, _fileStoreServiceProvider );

        addHeaderForResource( model, resourceType, nResourceId, user, ObservabilityViewMarks.VIEW_EXECUTION_DETAILS );

        String templatePath = getExecutionDetailTemplate( execution.getResourceType( ) );

        XPage page = getXPage( templatePath, locale, model );
        page.setTitle( execution.getResourceName( ) + " - " + execution.getExecutionId( ) );
        return page;
    }

    /**
     * Parses a mandatory integer request parameter, raising a typed exception on missing or malformed input so the boundary can map it to a user-facing error
     * instead of letting a {@link NumberFormatException} escape as a 500.
     *
     * @param value
     *            the raw parameter value
     * @return the parsed integer
     * @throws InvalidRequestException
     *             when the value is null, empty or not an integer
     */
    private int parseRequiredInt( String value ) throws InvalidRequestException
    {
        if ( value == null || value.isEmpty( ) )
        {
            throw new InvalidRequestException( );
        }
        try
        {
            return Integer.parseInt( value );
        }
        catch( NumberFormatException e )
        {
            throw new InvalidRequestException( );
        }
    }

    /**
     * Serializes the given object to JSON for the view using the shared object mapper.
     *
     * @param value
     *            the object to serialize
     * @return the JSON representation
     */
    private String toJson( Object value )
    {
        try
        {
            return JSON.writeValueAsString( value );
        }
        catch( JsonProcessingException e )
        {
            throw new IllegalStateException( "Failed to serialize view payload", e );
        }
    }

    /**
     * Resolves the execution detail template path for the given resource type, falling back to the default template when the resource type defines no custom
     * one.
     *
     * @param resourceType
     *            the resource type identifier
     * @return the execution detail template path
     */
    private String getExecutionDetailTemplate( String resourceType )
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

        return "/skin/plugins/platform/execution_details.html";
    }

    /**
     * Adds the resource header data (resource entity, permissions, current view and publication status) to the model. The entity resolution and provider
     * hydration are delegated to {@link ObservabilityService#resolveResourceEntity}; this boundary only enriches it with RBAC flags, exposes it under the
     * type-specific mark and resolves its publication status.
     *
     * @param model
     *            the model to populate
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource identifier
     * @param user
     *            the current Lutece user
     * @param currentView
     *            the current view name
     */
    private void addHeaderForResource( Models model, String resourceType, int resourceId, LuteceUser user, String currentView )
    {
        String mark = HEADER_MARK_BY_TYPE.get( resourceType );
        if ( mark == null )
        {
            return;
        }
        _observabilityService.resolveResourceEntity( resourceType, resourceId ).ifPresent( entity -> {
            AgentRBACService.enrichWithPermissions( entity, user );
            model.put( mark, entity );
            model.put( MARK_HEADER_CURRENT_VIEW, currentView );
            addPublicationStatusToModel( model, resourceType, resourceId );
        } );
    }

    /**
     * Checks the RBAC VIEW_OBSERVABILITY permission on the given client for the current user. Observability exposes execution history, costs and traces: it
     * requires its own permission (PLATFORM_OBSERVABILITY) and is NOT implied by the VIEW permission of the underlying resource type.
     *
     * @param nClientId
     *            the client whose observability is requested
     * @param user
     *            the current front-office user
     * @return true if the user is authorized
     */
    private boolean canViewObservability( int nClientId, LuteceUser user )
    {
        return RBACService.isAuthorized( PlatformObservabilityResourceIdService.RESOURCE_TYPE, String.valueOf( nClientId ),
                PlatformObservabilityResourceIdService.PERMISSION_VIEW_OBSERVABILITY, user );
    }
}
