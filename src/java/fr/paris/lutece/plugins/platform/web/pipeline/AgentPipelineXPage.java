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
package fr.paris.lutece.plugins.platform.web.pipeline;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTrigger;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTriggerHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersionHome;
import fr.paris.lutece.plugins.platform.service.exception.CannotRemoveCurrentVersionException;
import fr.paris.lutece.plugins.platform.service.pipeline.IPipelineService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineTriggerService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineVersionService;
import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult;
import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult.ErrorCode;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

/**
 * XPage handling Pipeline and Version views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_pipeline" )
@Controller( xpageName = "agent_pipeline", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentPipelineXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    @Inject
    private ObservabilityService _observabilityService;
    @Inject
    private IPipelineService _pipelineService;
    @Inject
    private PipelineTriggerService _pipelineTriggerService;

    protected static final String TEMPLATE_VIEW_PIPELINE = "/skin/plugins/platform/view_pipeline.html";
    protected static final String TEMPLATE_API_PIPELINE = "/skin/plugins/platform/api_pipeline.html";
    protected static final String TEMPLATE_CREATE_PIPELINE = "/skin/plugins/platform/create_pipeline.html";
    protected static final String TEMPLATE_MODIFY_PIPELINE = "/skin/plugins/platform/modify_pipeline.html";
    protected static final String TEMPLATE_VIEW_VERSION = "/skin/plugins/platform/view_version.html";
    protected static final String TEMPLATE_VIEW_TRIGGERS = "/skin/plugins/platform/view_pipeline_triggers.html";
    protected static final String TEMPLATE_CREATE_TRIGGER = "/skin/plugins/platform/create_pipeline_trigger.html";
    protected static final String TEMPLATE_MODIFY_TRIGGER = "/skin/plugins/platform/modify_pipeline_trigger.html";

    protected static final String PARAMETER_NAME = "name";
    protected static final String PARAMETER_DESCRIPTION = "description";
    protected static final String PARAMETER_MAX_CONCURRENT_WORKERS = "max_concurrent_workers";
    protected static final String PARAMETER_RATE_LIMIT_BY_USER_BY_DAY = "rate_limit_by_user_by_day";

    protected static final String MARK_VERSION = "version";
    protected static final String MARK_VERSION_LIST = "version_list";
    protected static final String MARK_NODE_TYPES_JSON = "node_types_json";
    protected static final String MARK_FLOW_JSON = "flow_json";
    protected static final String MARK_INPUT_SCHEMA_JSON = "input_schema_json";
    protected static final String MARK_TRIGGER = "trigger";
    protected static final String MARK_TRIGGER_LIST = "trigger_list";

    protected static final String VIEW_VIEW_PIPELINE = "viewPipeline";
    protected static final String VIEW_CREATE_PIPELINE = "createPipeline";
    protected static final String VIEW_MODIFY_PIPELINE = "modifyPipeline";
    protected static final String VIEW_VIEW_VERSION = "viewVersion";
    protected static final String VIEW_API_PIPELINE = "apiPipeline";
    protected static final String VIEW_VIEW_TRIGGERS = "viewTriggers";
    protected static final String VIEW_CREATE_TRIGGER = "createTrigger";
    protected static final String VIEW_MODIFY_TRIGGER = "modifyTrigger";

    protected static final String ACTION_CREATE_PIPELINE = "doCreatePipeline";
    protected static final String ACTION_MODIFY_PIPELINE = "doModifyPipeline";
    protected static final String ACTION_CONFIRM_REMOVE_PIPELINE = "confirmRemovePipeline";
    protected static final String ACTION_REMOVE_PIPELINE = "doRemovePipeline";
    protected static final String ACTION_CREATE_VERSION = "doCreateVersion";
    protected static final String ACTION_SET_CURRENT_VERSION = "doSetCurrentVersion";
    protected static final String ACTION_CONFIRM_REMOVE_VERSION = "confirmRemoveVersion";
    protected static final String ACTION_REMOVE_VERSION = "doRemoveVersion";
    protected static final String ACTION_CREATE_TRIGGER = "doCreateTrigger";
    protected static final String ACTION_MODIFY_TRIGGER = "doModifyTrigger";
    protected static final String ACTION_TOGGLE_TRIGGER = "doToggleTrigger";
    protected static final String ACTION_CONFIRM_REMOVE_TRIGGER = "confirmRemoveTrigger";
    protected static final String ACTION_REMOVE_TRIGGER = "doRemoveTrigger";

    private static final String XPAGE_NAME = "agent_pipeline";

    protected static final String MESSAGE_CONFIRM_REMOVE_PIPELINE = "platform.agent.message.confirmRemovePipeline";
    protected static final String MESSAGE_CONFIRM_REMOVE_VERSION = "platform.agent.message.confirmRemoveVersion";
    protected static final String INFO_PIPELINE_CREATED = "platform.agent.info.pipeline.created";
    protected static final String INFO_PIPELINE_UPDATED = "platform.agent.info.pipeline.updated";
    protected static final String INFO_PIPELINE_REMOVED = "platform.agent.info.pipeline.removed";
    protected static final String INFO_VERSION_CREATED = "platform.agent.info.version.created";
    protected static final String INFO_VERSION_SET_CURRENT = "platform.agent.info.version.setCurrent";
    protected static final String INFO_VERSION_REMOVED = "platform.agent.info.version.removed";
    protected static final String ERROR_CANNOT_REMOVE_CURRENT_VERSION = "platform.agent.error.cannotRemoveCurrentVersion";
    protected static final String LOG_ERROR_SERIALIZING_PIPELINE = "Error serializing pipeline data for FO view";
    protected static final String MESSAGE_PIPELINE_NOT_FOUND = "platform.agent.message.pipelineNotFound";
    protected static final String INFO_TRIGGER_CREATED = "platform.agent.info.trigger.created";
    protected static final String INFO_TRIGGER_UPDATED = "platform.agent.info.trigger.updated";
    protected static final String INFO_TRIGGER_REMOVED = "platform.agent.info.trigger.removed";
    protected static final String INFO_TRIGGER_TOGGLED = "platform.agent.info.trigger.toggled";
    protected static final String MESSAGE_CONFIRM_REMOVE_TRIGGER = "platform.agent.message.confirmRemoveTrigger";
    protected static final String ERROR_TRIGGER_NOT_FOUND = "platform.agent.error.trigger.notFound";
    protected static final String ERROR_TRIGGER_MISSING_SCHEDULE = "platform.agent.error.trigger.missingSchedule";
    protected static final String ERROR_TRIGGER_MISSING_NAME = "platform.agent.error.trigger.missingName";
    protected static final String ERROR_TRIGGER_INVALID_INPUT_DATA = "platform.agent.error.trigger.invalidInputData";
    protected static final String PARAMETER_TRIGGER_ID = "id_trigger";
    protected static final String PARAMETER_TRIGGER_NAME = "trigger_name";
    protected static final String PARAMETER_TRIGGER_TYPE = "trigger_type";
    protected static final String PARAMETER_CRON_EXPRESSION = "cron_expression";
    protected static final String PARAMETER_INTERVAL_MINUTES = "interval_minutes";
    protected static final String PARAMETER_INPUT_DATA = "input_data";
    protected static final String PARAMETER_ACTIVE = "active";

    private static final String ACTIVE_CHECKBOX_VALUE = "on";
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String EMPTY_JSON_ARRAY = "[]";

    /**
     * Returns the pipeline detail view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( value = VIEW_VIEW_PIPELINE, defaultView = true )
    public XPage viewPipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanView );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        model.put( MARK_VERSION_LIST, PipelineVersionHome.findByPipelineId( nPipelineId ) );
        model.put( MARK_TRIGGER_LIST, PipelineTriggerHome.findByPipelineId( nPipelineId ) );
        model.put( MARK_USER, user );

        _observabilityService.getStatsForResource( pipeline.getIdClient( ), Pipeline.RESOURCE_TYPE, String.valueOf( pipeline.getId( ) ) )
                .ifPresent( stats -> model.put( MARK_STATS, stats ) );

        addPipelineSidebarToModel( model, pipeline, VIEW_VIEW_PIPELINE );

        return getXPage( TEMPLATE_VIEW_PIPELINE, locale, model );
    }

    /**
     * Returns the pipeline API documentation view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_API_PIPELINE )
    public XPage apiPipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanView );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        model.put( MARK_CLIENT_ID, pipeline.getIdClient( ) );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );

        addPipelineSidebarToModel( model, pipeline, VIEW_API_PIPELINE );

        return getXPage( TEMPLATE_API_PIPELINE, locale, model );
    }

    /**
     * Displays the modify pipeline view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_PIPELINE )
    public XPage modifyPipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        model.put( MARK_USER, user );

        addPipelineSidebarToModel( model, pipeline, VIEW_MODIFY_PIPELINE );

        return getXPage( TEMPLATE_MODIFY_PIPELINE, locale, model );
    }

    /**
     * Returns the pipeline creation form.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     * @throws UserNotSignedException
     *             If the user is not signed in
     * @throws SiteMessageException
     *             If access is denied
     */
    @View( VIEW_CREATE_PIPELINE )
    public XPage createPipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreatePipeline( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        return getXPage( TEMPLATE_CREATE_PIPELINE, locale, model );
    }

    /**
     * Processes the creation of a pipeline.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_PIPELINE )
    public XPage doCreatePipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreatePipeline( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        int nClientId = getIntParam( request, PARAMETER_CLIENT_ID );
        if ( nClientId <= 0 )
        {
            return redirectToPortalWithMessage( request, MESSAGE_INVALID_REQUEST );
        }

        Pipeline pipeline = PipelineVersionService.createPipelineWithInitialVersion( request.getParameter( PARAMETER_NAME ),
                request.getParameter( PARAMETER_DESCRIPTION ), getIntParam( request, PARAMETER_MAX_CONCURRENT_WORKERS ),
                getIntParam( request, PARAMETER_RATE_LIMIT_BY_USER_BY_DAY ), nClientId );

        addInfo( INFO_PIPELINE_CREATED, locale );
        return redirectToPipeline( request, VIEW_VIEW_PIPELINE, pipeline.getId( ) );
    }

    /**
     * Processes the modification of a pipeline.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_PIPELINE )
    public XPage doModifyPipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        pipeline.setName( request.getParameter( PARAMETER_NAME ) );
        pipeline.setDescription( request.getParameter( PARAMETER_DESCRIPTION ) );
        pipeline.setMaxConcurrentWorkers( getIntParam( request, PARAMETER_MAX_CONCURRENT_WORKERS ) );
        pipeline.setRateLimitByUserByDay( getIntParam( request, PARAMETER_RATE_LIMIT_BY_USER_BY_DAY ) );
        PipelineHome.update( pipeline );

        addInfo( INFO_PIPELINE_UPDATED, locale );
        return redirectToPipeline( request, VIEW_VIEW_PIPELINE, nPipelineId );
    }

    /**
     * Shows a confirmation message before removing a pipeline.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_PIPELINE )
    public XPage confirmRemovePipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );
        Optional<Pipeline> optPipeline = PipelineHome.findByPrimaryKey( nPipelineId );

        if ( optPipeline.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_PIPELINE_NOT_FOUND );
        }
        if ( !AgentRBACService.canDeletePipeline( optPipeline.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_PIPELINE, MESSAGE_CONFIRM_REMOVE_PIPELINE, PARAMETER_PIPELINE_ID );
    }

    /**
     * Processes the removal of a pipeline.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_PIPELINE )
    public XPage doRemovePipeline( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanDelete );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        int nClientId = access.pipeline( ).getIdClient( );
        PipelineHome.remove( nPipelineId );

        addInfo( INFO_PIPELINE_REMOVED, locale );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Returns the version detail view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_VIEW_VERSION )
    public XPage viewVersion( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nVersionId = getIntParam( request, PARAMETER_VERSION_ID );
        Optional<PipelineVersion> optVersion = PipelineVersionHome.findByPrimaryKey( nVersionId );

        if ( optVersion.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_PIPELINE_NOT_FOUND );
        }

        PipelineVersion version = optVersion.get( );
        PipelineAccess access = resolveAuthorizedPipeline( request, user, version.getIdPipeline( ), Pipeline::isUserCanView );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        Models model = _models;
        model.put( MARK_VERSION, version );
        model.put( MARK_PIPELINE, access.pipeline( ) );
        model.put( MARK_USER, user );
        model.put( MARK_NODE_TYPES_JSON, serializeNodeTypes( ) );
        model.put( MARK_FLOW_JSON, version.getFlow( ) != null ? version.getFlow( ) : EMPTY_JSON_OBJECT );
        model.put( MARK_INPUT_SCHEMA_JSON, version.getInputSchema( ) != null ? version.getInputSchema( ) : EMPTY_JSON_ARRAY );

        return getXPage( TEMPLATE_VIEW_VERSION, locale, model );
    }

    /**
     * Creates a new version for a pipeline based on the current version.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_VERSION )
    public XPage doCreateVersion( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        PipelineVersionService.createVersionFromCurrent( nPipelineId );

        addInfo( INFO_VERSION_CREATED, locale );
        return redirectToPipeline( request, VIEW_VIEW_PIPELINE, nPipelineId );
    }

    /**
     * Sets a version as the current version.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_SET_CURRENT_VERSION )
    public XPage doSetCurrentVersion( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nVersionId = getIntParam( request, PARAMETER_VERSION_ID );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        PipelineVersionHome.setCurrentVersion( nVersionId, nPipelineId );
        addInfo( INFO_VERSION_SET_CURRENT, locale );

        return redirectToPipeline( request, VIEW_VIEW_PIPELINE, nPipelineId );
    }

    /**
     * Shows a confirmation message before removing a version.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_VERSION )
    public XPage confirmRemoveVersion( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );
        Optional<Pipeline> optPipeline = PipelineHome.findByPrimaryKey( nPipelineId );

        if ( optPipeline.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_PIPELINE_NOT_FOUND );
        }
        if ( !AgentRBACService.canModifyPipeline( optPipeline.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_VERSION, MESSAGE_CONFIRM_REMOVE_VERSION, PARAMETER_VERSION_ID, PARAMETER_PIPELINE_ID,
                PARAMETER_CLIENT_ID );
    }

    /**
     * Removes a version if it's not the current version.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_VERSION )
    public XPage doRemoveVersion( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nVersionId = getIntParam( request, PARAMETER_VERSION_ID );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanDelete );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        try
        {
            PipelineVersionService.removeVersion( nVersionId );
            addInfo( INFO_VERSION_REMOVED, locale );
        }
        catch( CannotRemoveCurrentVersionException e )
        {
            addError( ERROR_CANNOT_REMOVE_CURRENT_VERSION, locale );
        }

        return redirectToPipeline( request, VIEW_VIEW_PIPELINE, nPipelineId );
    }

    /**
     * Displays the triggers list for a pipeline.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @View( VIEW_VIEW_TRIGGERS )
    public XPage viewTriggers( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanView );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        model.put( MARK_TRIGGER_LIST, PipelineTriggerHome.findByPipelineId( nPipelineId ) );
        addPipelineSidebarToModel( model, pipeline, VIEW_VIEW_TRIGGERS );

        return getXPage( TEMPLATE_VIEW_TRIGGERS, locale, model );
    }

    /**
     * Displays the trigger creation form.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @View( VIEW_CREATE_TRIGGER )
    public XPage createTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        addPipelineSidebarToModel( model, pipeline, VIEW_VIEW_TRIGGERS );

        return getXPage( TEMPLATE_CREATE_TRIGGER, locale, model );
    }

    /**
     * Displays the trigger modification form.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @View( VIEW_MODIFY_TRIGGER )
    public XPage modifyTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nTriggerId = getIntParam( request, PARAMETER_TRIGGER_ID );
        Optional<PipelineTrigger> optTrigger = PipelineTriggerHome.findByPrimaryKey( nTriggerId );

        if ( optTrigger.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, ERROR_TRIGGER_NOT_FOUND );
        }

        PipelineTrigger trigger = optTrigger.get( );
        PipelineAccess access = resolveAuthorizedPipeline( request, user, trigger.getIdPipeline( ), Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }

        Models model = _models;
        model.put( MARK_PIPELINE, access.pipeline( ) );
        model.put( MARK_TRIGGER, trigger );
        addPipelineSidebarToModel( model, access.pipeline( ), VIEW_VIEW_TRIGGERS );

        return getXPage( TEMPLATE_MODIFY_TRIGGER, locale, model );
    }

    /**
     * Processes the creation of a trigger.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CREATE_TRIGGER )
    public XPage doCreateTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        String strName = request.getParameter( PARAMETER_TRIGGER_NAME );
        String strConfiguration = buildConfiguration( request );
        String strInputData = request.getParameter( PARAMETER_INPUT_DATA );

        TriggerValidationResult validation = _pipelineTriggerService.validateTrigger( strName, strConfiguration, strInputData );
        if ( !validation.isValid( ) )
        {
            mapTriggerErrors( validation, locale );
            return getXPage( TEMPLATE_CREATE_TRIGGER, locale, buildTriggerFormModel( request, pipeline ) );
        }

        _pipelineTriggerService.createTrigger( nPipelineId, pipeline.getIdClient( ), strName, strConfiguration, strInputData,
                ACTIVE_CHECKBOX_VALUE.equals( request.getParameter( PARAMETER_ACTIVE ) ) );

        addInfo( INFO_TRIGGER_CREATED, locale );
        return redirectToPipeline( request, VIEW_VIEW_TRIGGERS, nPipelineId );
    }

    /**
     * Processes the modification of a trigger.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_MODIFY_TRIGGER )
    public XPage doModifyTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nTriggerId = getIntParam( request, PARAMETER_TRIGGER_ID );
        Optional<PipelineTrigger> optTrigger = PipelineTriggerHome.findByPrimaryKey( nTriggerId );

        if ( optTrigger.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, ERROR_TRIGGER_NOT_FOUND );
        }

        int nPipelineId = optTrigger.get( ).getIdPipeline( );
        PipelineAccess access = resolveAuthorizedPipeline( request, user, nPipelineId, Pipeline::isUserCanModify );
        if ( access.redirect( ) != null )
        {
            return access.redirect( );
        }
        Pipeline pipeline = access.pipeline( );

        String strName = request.getParameter( PARAMETER_TRIGGER_NAME );
        String strConfiguration = buildConfiguration( request );
        String strInputData = request.getParameter( PARAMETER_INPUT_DATA );

        TriggerValidationResult validation = _pipelineTriggerService.validateTrigger( strName, strConfiguration, strInputData );
        if ( !validation.isValid( ) )
        {
            mapTriggerErrors( validation, locale );
            return getXPage( TEMPLATE_MODIFY_TRIGGER, locale, buildTriggerFormModel( request, pipeline ) );
        }

        _pipelineTriggerService.updateTrigger( nTriggerId, strName, strConfiguration, strInputData,
                ACTIVE_CHECKBOX_VALUE.equals( request.getParameter( PARAMETER_ACTIVE ) ) );

        addInfo( INFO_TRIGGER_UPDATED, locale );
        return redirectToPipeline( request, VIEW_VIEW_TRIGGERS, nPipelineId );
    }

    /**
     * Toggles a trigger active/inactive.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_TOGGLE_TRIGGER )
    public XPage doToggleTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTriggerId = getIntParam( request, PARAMETER_TRIGGER_ID );
        Optional<PipelineTrigger> optTrigger = PipelineTriggerHome.findByPrimaryKey( nTriggerId );

        if ( optTrigger.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, ERROR_TRIGGER_NOT_FOUND );
        }

        PipelineTrigger trigger = _pipelineTriggerService.toggleActive( nTriggerId );

        addInfo( INFO_TRIGGER_TOGGLED, locale );
        return redirectToPipeline( request, VIEW_VIEW_TRIGGERS, trigger.getIdPipeline( ) );
    }

    /**
     * Shows a confirmation message before removing a trigger.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_TRIGGER )
    public XPage confirmRemoveTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_TRIGGER, MESSAGE_CONFIRM_REMOVE_TRIGGER, PARAMETER_TRIGGER_ID, PARAMETER_PIPELINE_ID );
    }

    /**
     * Removes a trigger.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_REMOVE_TRIGGER )
    public XPage doRemoveTrigger( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTriggerId = getIntParam( request, PARAMETER_TRIGGER_ID );
        int nPipelineId = getIntParam( request, PARAMETER_PIPELINE_ID );

        PipelineTriggerHome.remove( nTriggerId );
        addInfo( INFO_TRIGGER_REMOVED, locale );

        return redirectToPipeline( request, VIEW_VIEW_TRIGGERS, nPipelineId );
    }

    /**
     * Loads a pipeline, enriches it with the user's permissions and checks the required permission. Returns a holder carrying either the authorized pipeline or
     * the redirect to apply (pipeline not found redirects to the portal; permission denial adds an error and redirects to the pipeline view).
     *
     * @param request
     *            The HTTP request
     * @param user
     *            The current user
     * @param pipelineId
     *            The pipeline identifier
     * @param permission
     *            The permission predicate evaluated on the enriched pipeline
     * @return The access holder
     */
    private PipelineAccess resolveAuthorizedPipeline( HttpServletRequest request, LuteceUser user, int pipelineId, Predicate<Pipeline> permission )
            throws UserNotSignedException, SiteMessageException
    {
        Optional<Pipeline> optPipeline = PipelineHome.findByPrimaryKey( pipelineId );
        if ( optPipeline.isEmpty( ) )
        {
            return new PipelineAccess( null, redirectToPortalWithMessage( request, MESSAGE_PIPELINE_NOT_FOUND ) );
        }

        Pipeline pipeline = optPipeline.get( );
        AgentRBACService.enrichWithPermissions( pipeline, user );
        if ( !permission.test( pipeline ) )
        {
            addError( MESSAGE_ACCESS_DENIED, getLocale( request ) );
            return new PipelineAccess( null, redirectToResourceView( request, VIEW_VIEW_PIPELINE, PARAMETER_PIPELINE_ID, pipeline.getId( ) ) );
        }
        return new PipelineAccess( pipeline, null );
    }

    /**
     * Builds the trigger form model used to re-display a creation or modification form after a validation failure.
     *
     * @param request
     *            The HTTP request
     * @param pipeline
     *            The owning pipeline
     * @return The populated model
     */
    private Models buildTriggerFormModel( HttpServletRequest request, Pipeline pipeline )
    {
        PipelineTrigger trigger = new PipelineTrigger( );
        trigger.setIdPipeline( pipeline.getId( ) );
        trigger.setIdClient( pipeline.getIdClient( ) );
        trigger.setName( request.getParameter( PARAMETER_TRIGGER_NAME ) );
        trigger.setTriggerType( request.getParameter( PARAMETER_TRIGGER_TYPE ) );
        trigger.setInputData( request.getParameter( PARAMETER_INPUT_DATA ) );
        trigger.setActive( ACTIVE_CHECKBOX_VALUE.equals( request.getParameter( PARAMETER_ACTIVE ) ) );

        Models model = _models;
        model.put( MARK_PIPELINE, pipeline );
        model.put( MARK_TRIGGER, trigger );
        model.put( PARAMETER_CRON_EXPRESSION, request.getParameter( PARAMETER_CRON_EXPRESSION ) );
        model.put( PARAMETER_INTERVAL_MINUTES, request.getParameter( PARAMETER_INTERVAL_MINUTES ) );
        addPipelineSidebarToModel( model, pipeline, VIEW_VIEW_TRIGGERS );
        return model;
    }

    /**
     * Maps the trigger validation error codes to user-facing i18n errors.
     *
     * @param validation
     *            The validation result
     * @param locale
     *            The locale for error messages
     */
    private void mapTriggerErrors( TriggerValidationResult validation, Locale locale )
    {
        for ( ErrorCode code : validation.getErrors( ) )
        {
            switch( code )
            {
                case MISSING_NAME:
                    addError( ERROR_TRIGGER_MISSING_NAME, locale );
                    break;
                case MISSING_SCHEDULE:
                    addError( ERROR_TRIGGER_MISSING_SCHEDULE, locale );
                    break;
                case INVALID_INPUT_DATA:
                    addError( ERROR_TRIGGER_INVALID_INPUT_DATA, locale );
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Delegates trigger configuration assembly to the trigger service using the request schedule parameters.
     *
     * @param request
     *            The HTTP request
     * @return The configuration JSON, or null when no schedule was provided
     */
    private String buildConfiguration( HttpServletRequest request )
    {
        return _pipelineTriggerService.buildConfiguration( request.getParameter( PARAMETER_TRIGGER_TYPE ), request.getParameter( PARAMETER_CRON_EXPRESSION ),
                request.getParameter( PARAMETER_INTERVAL_MINUTES ) );
    }

    /**
     * Serializes the available node types metadata to JSON for the version editor, returning an empty object on failure.
     *
     * @return The node types JSON
     */
    private String serializeNodeTypes( )
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString( _pipelineService.getNodeTypes( ) );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_SERIALIZING_PIPELINE, e );
            return EMPTY_JSON_OBJECT;
        }
    }

    /**
     * Redirects to a pipeline-scoped view, forwarding the client identifier when present in the request.
     *
     * @param request
     *            The HTTP request
     * @param view
     *            The target view name
     * @param pipelineId
     *            The pipeline identifier
     * @return The redirect XPage
     */
    private XPage redirectToPipeline( HttpServletRequest request, String view, int pipelineId ) throws UserNotSignedException
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_PIPELINE_ID, String.valueOf( pipelineId ) );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        if ( strClientId != null )
        {
            params.put( PARAMETER_CLIENT_ID, strClientId );
        }
        return redirect( request, view, params );
    }

    /**
     * Reads an integer request parameter, defaulting to 0 when missing or non-numeric.
     *
     * @param request
     *            The HTTP request
     * @param paramName
     *            The parameter name
     * @return The parsed integer, or 0 when absent or malformed
     */
    private int getIntParam( HttpServletRequest request, String paramName )
    {
        String value = request.getParameter( paramName );
        if ( value == null || value.trim( ).isEmpty( ) )
        {
            return 0;
        }
        try
        {
            return Integer.parseInt( value.trim( ) );
        }
        catch( NumberFormatException e )
        {
            return 0;
        }
    }

    /**
     * Adds the pipeline sidebar data to the model, including the current view marker and the publication status.
     *
     * @param model
     *            the view model to populate
     * @param pipeline
     *            the current pipeline
     * @param currentView
     *            the current view identifier
     */
    private void addPipelineSidebarToModel( Models model, Pipeline pipeline, String currentView )
    {
        model.put( MARK_HEADER_CURRENT_VIEW, currentView );
        addPublicationStatusToModel( model, Pipeline.RESOURCE_TYPE, pipeline.getId( ) );
    }

    /**
     * Holder for the result of a pipeline authorization resolution: either an authorized pipeline or a redirect XPage to return.
     *
     * @param pipeline
     *            the authorized pipeline, or null when a redirect must be applied
     * @param redirect
     *            the redirect XPage, or null when the pipeline is authorized
     */
    private record PipelineAccess(Pipeline pipeline, XPage redirect) {
    }
}
