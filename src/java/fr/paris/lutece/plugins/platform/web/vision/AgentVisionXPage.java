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
package fr.paris.lutece.plugins.platform.web.vision;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionFieldType;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.service.exception.InvalidFieldTypeException;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.vision.VisionService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

/**
 * XPage handling Vision, Extractor and ExtractorField views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_vision" )
@Controller( xpageName = "agent_vision", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentVisionXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;
    protected static final String TEMPLATE_CREATE_VISION = "/skin/plugins/platform/create_vision.html";
    protected static final String TEMPLATE_API_VISION = "/skin/plugins/platform/api_vision.html";
    protected static final String TEMPLATE_MODIFY_VISION = "/skin/plugins/platform/modify_vision.html";
    protected static final String TEMPLATE_MODIFY_VISION_EXTRACTORS = "/skin/plugins/platform/modify_vision_extractors.html";
    protected static final String TEMPLATE_VIEW_VISION = "/skin/plugins/platform/view_vision.html";
    protected static final String TEMPLATE_TEST_VISION = "/skin/plugins/platform/test_vision.html";

    protected static final String PARAMETER_FIELD_NAME = "field_name";
    protected static final String PARAMETER_FIELD_DESCRIPTION = "field_description";
    protected static final String PARAMETER_FIELD_TYPE = "field_type";
    protected static final String PARAMETER_EXTRACTOR_NAME = "extractor_name";
    protected static final String PARAMETER_EXTRACTOR_DESCRIPTION = "extractor_description";

    protected static final String MARK_VISION_PROVIDERS = "vision_providers";
    protected static final String MARK_FIELD_TYPES = "field_types";
    protected static final String MARK_PROVIDER = "provider";

    protected static final String VIEW_CREATE_VISION = "createVision";
    protected static final String VIEW_MODIFY_VISION = "modifyVision";
    protected static final String VIEW_MODIFY_VISION_EXTRACTORS = "modifyVisionExtractors";
    protected static final String VIEW_TEST_VISION = "testVision";
    protected static final String VIEW_VIEW_VISION = "viewVision";
    protected static final String VIEW_API_VISION = "apiVision";

    protected static final String ACTION_CREATE_VISION = "doCreateVision";
    protected static final String ACTION_MODIFY_VISION = "doModifyVision";
    protected static final String ACTION_CONFIRM_REMOVE_VISION = "confirmRemoveVision";
    protected static final String ACTION_REMOVE_VISION = "doRemoveVision";
    protected static final String ACTION_ADD_EXTRACTOR = "doAddExtractor";
    protected static final String ACTION_MODIFY_EXTRACTOR = "doModifyExtractor";
    protected static final String ACTION_CONFIRM_REMOVE_EXTRACTOR = "confirmRemoveExtractor";
    protected static final String ACTION_REMOVE_EXTRACTOR = "doRemoveExtractor";
    protected static final String ACTION_ADD_EXTRACTOR_FIELD = "doAddExtractorField";
    protected static final String ACTION_MODIFY_EXTRACTOR_FIELD = "doModifyExtractorField";
    protected static final String ACTION_CONFIRM_REMOVE_EXTRACTOR_FIELD = "confirmRemoveExtractorField";
    protected static final String ACTION_REMOVE_EXTRACTOR_FIELD = "doRemoveExtractorField";

    private static final String XPAGE_NAME = "agent_vision";

    protected static final String MESSAGE_VISION_NOT_FOUND = "platform.agent.error.vision.not.found";
    protected static final String MESSAGE_CONFIRM_REMOVE_VISION = "platform.agent.message.confirmRemoveVision";
    protected static final String MESSAGE_CONFIRM_REMOVE_EXTRACTOR = "platform.agent.message.confirmRemoveExtractor";
    protected static final String MESSAGE_CONFIRM_REMOVE_EXTRACTOR_FIELD = "platform.agent.message.confirmRemoveExtractorField";
    protected static final String MESSAGE_EXTRACTOR_CREATED = "platform.agent.info.extractor.created";
    protected static final String MESSAGE_EXTRACTOR_UPDATED = "platform.agent.info.extractor.updated";
    protected static final String MESSAGE_EXTRACTOR_REMOVED = "platform.agent.info.extractor.removed";
    protected static final String MESSAGE_FIELD_CREATED = "platform.agent.info.extractor_field.created";
    protected static final String MESSAGE_FIELD_UPDATED = "platform.agent.info.extractor_field.updated";
    protected static final String MESSAGE_FIELD_REMOVED = "platform.agent.info.extractor_field.removed";
    protected static final String INFO_VISION_CREATED = "platform.agent.info.vision.created";
    protected static final String INFO_VISION_UPDATED = "platform.agent.info.vision.updated";
    protected static final String INFO_VISION_REMOVED = "platform.agent.info.vision.removed";
    protected static final String ERROR_EXTRACTOR_NOT_FOUND = "platform.agent.error.extractor.not.found";
    protected static final String ERROR_FIELD_NOT_FOUND = "platform.agent.error.field.not.found";
    protected static final String ERROR_INVALID_FIELD_TYPE = "platform.agent.error.invalid.field.type";

    @Inject
    private VisionService _visionService;

    /**
     * Returns the vision readonly view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( value = VIEW_VIEW_VISION, defaultView = true )
    public XPage viewVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Optional<Vision> optVision = loadVisionForView( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Vision vision = optVision.get( );
        Models model = _models;
        model.put( MARK_VISION, vision );
        model.put( MARK_CLIENT_ID, resolveClientId( request, vision ) );
        _visionService.resolveProvider( vision ).ifPresent( p -> model.put( MARK_PROVIDER, p ) );

        addVisionSidebarToModel( model, vision, VIEW_VIEW_VISION );

        return getXPage( TEMPLATE_VIEW_VISION, getLocale( request ), model );
    }

    /**
     * Returns the vision API documentation view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_API_VISION )
    public XPage apiVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Optional<Vision> optVision = loadVisionForView( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Vision vision = optVision.get( );
        Models model = _models;
        model.put( MARK_VISION, vision );
        model.put( MARK_CLIENT_ID, resolveClientId( request, vision ) );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );

        addVisionSidebarToModel( model, vision, VIEW_API_VISION );

        return getXPage( TEMPLATE_API_VISION, getLocale( request ), model );
    }

    /**
     * Displays the create vision view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_CREATE_VISION )
    public XPage createVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateVision( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_USER, user );
        model.put( MARK_VISION_PROVIDERS, ProviderHome.getProvidersByType( PROVIDER_TYPE_LLM ) );
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );

        return getXPage( TEMPLATE_CREATE_VISION, getLocale( request ), model );
    }

    /**
     * Displays the modify vision view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_VISION )
    public XPage modifyVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Vision> optVision = _visionService.loadVisionWithExtractorsAndFields( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }

        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, user );

        if ( !vision.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_VISION, PARAMETER_VISION_ID, vision.getId( ) );
        }

        Models model = _models;
        model.put( MARK_VISION, vision );
        model.put( MARK_USER, user );
        model.put( MARK_VISION_PROVIDERS, ProviderHome.getProvidersByType( PROVIDER_TYPE_LLM ) );
        model.put( MARK_FIELD_TYPES, Arrays.asList( VisionFieldType.values( ) ) );
        model.put( MARK_CLIENT_ID, resolveClientId( request, vision ) );

        addVisionSidebarToModel( model, vision, VIEW_MODIFY_VISION );

        return getXPage( TEMPLATE_MODIFY_VISION, locale, model );
    }

    /**
     * Handles the create vision action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_VISION )
    public XPage doCreateVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateVision( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Vision vision = new Vision( );
        populate( vision, request );

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        Integer clientId = strClientId != null && !strClientId.isEmpty( ) ? Integer.valueOf( strClientId ) : null;
        if ( clientId != null )
        {
            vision.setClientId( clientId );
        }
        if ( !validateBean( vision, request.getLocale( ) ) )
        {
            return redirect( request, URL_PORTAL );
        }

        Vision created = _visionService.createVision( vision, clientId );

        addInfo( INFO_VISION_CREATED, locale );
        return redirect( request, VIEW_MODIFY_VISION, buildVisionParams( created.getId( ), strClientId ) );
    }

    /**
     * Handles the modify vision action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_VISION )
    public XPage doModifyVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nVisionId = parseVisionId( request );
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( nVisionId );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }

        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, user );

        if ( !vision.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_VISION, PARAMETER_VISION_ID, vision.getId( ) );
        }

        populate( vision, request );
        if ( !validateBean( vision, request.getLocale( ) ) )
        {
            return redirect( request, VIEW_MODIFY_VISION, buildVisionParams( nVisionId, null ) );
        }

        _visionService.updateVision( vision );

        addInfo( INFO_VISION_UPDATED, locale );
        return redirect( request, VIEW_MODIFY_VISION, buildVisionParams( nVisionId, request.getParameter( PARAMETER_CLIENT_ID ) ) );
    }

    /**
     * Shows a confirmation message before removing a vision.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_VISION )
    public XPage confirmRemoveVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }
        if ( !AgentRBACService.canDeleteVision( optVision.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_VISION, MESSAGE_CONFIRM_REMOVE_VISION, PARAMETER_VISION_ID, PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove vision action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_VISION )
    public XPage doRemoveVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }

        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, user );

        if ( !vision.isUserCanDelete( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_VISION, PARAMETER_VISION_ID, vision.getId( ) );
        }

        int nClientId = _visionService.removeVision( vision.getId( ) );

        addInfo( INFO_VISION_REMOVED, locale );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Displays the modify vision extractors view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_VISION_EXTRACTORS )
    public XPage modifyVisionExtractors( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Vision> optVision = _visionService.loadVisionWithExtractorsAndFields( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }

        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, user );

        if ( !vision.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return redirectToResourceView( request, VIEW_VIEW_VISION, PARAMETER_VISION_ID, vision.getId( ) );
        }

        Models model = _models;
        model.put( MARK_VISION, vision );
        model.put( MARK_USER, user );
        model.put( MARK_FIELD_TYPES, Arrays.asList( VisionFieldType.values( ) ) );
        model.put( MARK_CLIENT_ID, resolveClientId( request, vision ) );

        addVisionSidebarToModel( model, vision, VIEW_MODIFY_VISION_EXTRACTORS );

        return getXPage( TEMPLATE_MODIFY_VISION_EXTRACTORS, locale, model );
    }

    /**
     * Displays the test vision view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_TEST_VISION )
    public XPage testVision( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        Optional<Vision> optVision = _visionService.loadVisionWithExtractorsAndFields( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }

        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, user );

        if ( !vision.isUserCanView( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_VISION, vision );
        model.put( MARK_USER, user );
        model.put( MARK_CLIENT_ID, resolveClientId( request, vision ) );

        addVisionSidebarToModel( model, vision, VIEW_TEST_VISION );

        return getXPage( TEMPLATE_TEST_VISION, locale, model );
    }

    /**
     * Handles the add extractor action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_ADD_EXTRACTOR )
    public XPage doAddExtractor( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireModifiableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        _visionService.addExtractor( nVisionId, request.getParameter( PARAMETER_EXTRACTOR_NAME ), request.getParameter( PARAMETER_EXTRACTOR_DESCRIPTION ) );
        addInfo( MESSAGE_EXTRACTOR_CREATED, locale );

        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Handles the modify extractor action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_EXTRACTOR )
    public XPage doModifyExtractor( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireModifiableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        int nExtractorId = Integer.parseInt( request.getParameter( PARAMETER_EXTRACTOR_ID ) );
        boolean updated = _visionService.updateExtractor( nExtractorId, request.getParameter( PARAMETER_EXTRACTOR_NAME ),
                request.getParameter( PARAMETER_EXTRACTOR_DESCRIPTION ) );

        if ( updated )
        {
            addInfo( MESSAGE_EXTRACTOR_UPDATED, locale );
        }
        else
        {
            addError( ERROR_EXTRACTOR_NOT_FOUND, locale );
        }
        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Shows a confirmation message before removing an extractor.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_EXTRACTOR )
    public XPage confirmRemoveExtractor( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }
        if ( !AgentRBACService.canModifyVision( optVision.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_EXTRACTOR, MESSAGE_CONFIRM_REMOVE_EXTRACTOR, PARAMETER_EXTRACTOR_ID, PARAMETER_VISION_ID,
                PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove extractor action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_EXTRACTOR )
    public XPage doRemoveExtractor( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireDeletableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        _visionService.removeExtractor( Integer.parseInt( request.getParameter( PARAMETER_EXTRACTOR_ID ) ) );
        addInfo( MESSAGE_EXTRACTOR_REMOVED, locale );

        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Handles the add extractor field action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_ADD_EXTRACTOR_FIELD )
    public XPage doAddExtractorField( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireModifiableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        int nExtractorId = Integer.parseInt( request.getParameter( PARAMETER_EXTRACTOR_ID ) );
        try
        {
            _visionService.addExtractorField( nExtractorId, request.getParameter( PARAMETER_FIELD_NAME ), request.getParameter( PARAMETER_FIELD_DESCRIPTION ),
                    request.getParameter( PARAMETER_FIELD_TYPE ) );
        }
        catch( InvalidFieldTypeException e )
        {
            addError( ERROR_INVALID_FIELD_TYPE, locale );
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        addInfo( MESSAGE_FIELD_CREATED, locale );
        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Handles the modify extractor field action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_EXTRACTOR_FIELD )
    public XPage doModifyExtractorField( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireModifiableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        int nFieldId = Integer.parseInt( request.getParameter( PARAMETER_FIELD_ID ) );
        try
        {
            boolean updated = _visionService.updateExtractorField( nFieldId, request.getParameter( PARAMETER_FIELD_NAME ),
                    request.getParameter( PARAMETER_FIELD_DESCRIPTION ), request.getParameter( PARAMETER_FIELD_TYPE ) );
            if ( updated )
            {
                addInfo( MESSAGE_FIELD_UPDATED, locale );
            }
            else
            {
                addError( ERROR_FIELD_NOT_FOUND, locale );
            }
        }
        catch( InvalidFieldTypeException e )
        {
            addError( ERROR_INVALID_FIELD_TYPE, locale );
        }

        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Shows a confirmation message before removing an extractor field.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_EXTRACTOR_FIELD )
    public XPage confirmRemoveExtractorField( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );

        if ( optVision.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_VISION_NOT_FOUND );
        }
        if ( !AgentRBACService.canModifyVision( optVision.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_EXTRACTOR_FIELD, MESSAGE_CONFIRM_REMOVE_EXTRACTOR_FIELD, PARAMETER_FIELD_ID,
                PARAMETER_VISION_ID, PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove extractor field action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_EXTRACTOR_FIELD )
    public XPage doRemoveExtractorField( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nVisionId = parseVisionId( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<Vision> optVision = requireDeletableVision( request );
        if ( optVision.isEmpty( ) )
        {
            return redirectToVisionExtractors( request, nVisionId, strClientId );
        }

        _visionService.removeExtractorField( Integer.parseInt( request.getParameter( PARAMETER_FIELD_ID ) ) );
        addInfo( MESSAGE_FIELD_REMOVED, locale );

        return redirectToVisionExtractors( request, nVisionId, strClientId );
    }

    /**
     * Loads the vision targeted by the request with its extractor graph and enforces the read permission.
     *
     * @param request
     *            the HTTP request carrying the vision identifier
     * @return the readable vision, or an empty Optional when missing or access is denied
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    private Optional<Vision> loadVisionForView( HttpServletRequest request ) throws UserNotSignedException
    {
        Optional<Vision> optVision = _visionService.loadVisionWithExtractorsAndFields( parseVisionId( request ) );
        if ( optVision.isEmpty( ) )
        {
            return optVision;
        }
        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, requireUser( request ) );
        return vision.isUserCanView( ) ? optVision : Optional.empty( );
    }

    /**
     * Loads the vision targeted by the request and enforces the modify permission, emitting the appropriate user message when the vision is missing or the user
     * is not allowed to modify it.
     *
     * @param request
     *            the HTTP request carrying the vision identifier
     * @return the modifiable vision, or an empty Optional when missing or access is denied
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    private Optional<Vision> requireModifiableVision( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );
        if ( optVision.isEmpty( ) )
        {
            addError( MESSAGE_VISION_NOT_FOUND, locale );
            return Optional.empty( );
        }
        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, requireUser( request ) );
        if ( !vision.isUserCanModify( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return Optional.empty( );
        }
        return optVision;
    }

    /**
     * Loads the vision targeted by the request and enforces the delete permission, emitting the appropriate user message when the vision is missing or the user
     * is not allowed to delete from it.
     *
     * @param request
     *            the HTTP request carrying the vision identifier
     * @return the deletable vision, or an empty Optional when missing or access is denied
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    private Optional<Vision> requireDeletableVision( HttpServletRequest request ) throws UserNotSignedException
    {
        Locale locale = getLocale( request );
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( parseVisionId( request ) );
        if ( optVision.isEmpty( ) )
        {
            addError( MESSAGE_VISION_NOT_FOUND, locale );
            return Optional.empty( );
        }
        Vision vision = optVision.get( );
        AgentRBACService.enrichWithPermissions( vision, requireUser( request ) );
        if ( !vision.isUserCanDelete( ) )
        {
            addError( MESSAGE_ACCESS_DENIED, locale );
            return Optional.empty( );
        }
        return optVision;
    }

    /**
     * Reads and parses the vision identifier from the request.
     *
     * @param request
     *            the HTTP request
     * @return the vision identifier
     */
    private int parseVisionId( HttpServletRequest request )
    {
        return Integer.parseInt( request.getParameter( PARAMETER_VISION_ID ) );
    }

    /**
     * Resolves the client identifier to expose in the model, falling back to the vision owning client when the request carries none.
     *
     * @param request
     *            the HTTP request
     * @param vision
     *            the current vision
     * @return the client identifier as a string
     */
    private String resolveClientId( HttpServletRequest request, Vision vision )
    {
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        return strClientId != null ? strClientId : String.valueOf( vision.getClientId( ) );
    }

    /**
     * Builds the redirect parameters carrying the vision identifier and, when present, the client identifier.
     *
     * @param visionId
     *            the vision identifier
     * @param strClientId
     *            the client identifier, may be null
     * @return the redirect parameters map
     */
    private Map<String, String> buildVisionParams( int visionId, String strClientId )
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_VISION_ID, String.valueOf( visionId ) );
        if ( strClientId != null )
        {
            params.put( PARAMETER_CLIENT_ID, strClientId );
        }
        return params;
    }

    /**
     * Redirects to the modify-extractors view of the given vision.
     *
     * @param request
     *            the HTTP request
     * @param visionId
     *            the vision identifier
     * @param strClientId
     *            the client identifier, may be null
     * @return the redirect XPage
     */
    private XPage redirectToVisionExtractors( HttpServletRequest request, int visionId, String strClientId )
    {
        return redirect( request, VIEW_MODIFY_VISION_EXTRACTORS, buildVisionParams( visionId, strClientId ) );
    }

    /**
     * Adds the vision sidebar data (current view and publication status) to the model.
     *
     * @param model
     *            the model to populate
     * @param vision
     *            the vision resource
     * @param currentView
     *            the current view name
     */
    private void addVisionSidebarToModel( Models model, Vision vision, String currentView )
    {
        model.put( MARK_HEADER_CURRENT_VIEW, currentView );
        addPublicationStatusToModel( model, Vision.RESOURCE_TYPE, vision.getId( ) );
    }
}
