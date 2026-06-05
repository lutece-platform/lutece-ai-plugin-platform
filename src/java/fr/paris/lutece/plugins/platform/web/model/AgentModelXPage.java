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
package fr.paris.lutece.plugins.platform.web.model;

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

import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.model.ModelCrudService;
import fr.paris.lutece.plugins.platform.service.model.dto.ModelCreateResult;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

/**
 * XPage handling Model views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_model" )
@Controller( xpageName = "agent_model", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentModelXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    protected static final String TEMPLATE_CREATE_MODEL = "/skin/plugins/platform/create_model.html";
    protected static final String TEMPLATE_MODIFY_MODEL = "/skin/plugins/platform/modify_model.html";
    protected static final String TEMPLATE_VIEW_MODEL = "/skin/plugins/platform/view_model.html";
    protected static final String TEMPLATE_API_MODEL = "/skin/plugins/platform/api_model.html";

    protected static final String PARAMETER_PROVIDER_ID = "provider_id";

    protected static final String MARK_PROVIDERS = "providers";

    protected static final String VIEW_CREATE_MODEL = "createModel";
    protected static final String VIEW_MODIFY_MODEL = "modifyModel";
    protected static final String VIEW_VIEW_MODEL = "viewModel";
    protected static final String VIEW_API_MODEL = "apiModel";

    protected static final String ACTION_CREATE_MODEL = "doCreateModel";
    protected static final String ACTION_MODIFY_MODEL = "doModifyModel";
    protected static final String ACTION_CONFIRM_REMOVE_MODEL = "confirmRemoveModel";
    protected static final String ACTION_REMOVE_MODEL = "doRemoveModel";

    private static final String XPAGE_NAME = "agent_model";

    protected static final String MESSAGE_MODEL_NOT_FOUND = "platform.agent.error.model.not.found";
    protected static final String MESSAGE_CONFIRM_REMOVE_MODEL = "platform.agent.message.confirmRemoveModel";
    protected static final String MESSAGE_MODEL_ALREADY_EXISTS = "platform.agent.error.model.already.exists";
    protected static final String INFO_MODEL_CREATED = "platform.agent.info.model.created";
    protected static final String INFO_MODEL_UPDATED = "platform.agent.info.model.updated";
    protected static final String INFO_MODEL_REMOVED = "platform.agent.info.model.removed";

    @Inject
    private ModelCrudService _modelCrudService;

    /**
     * Returns the model readonly view page.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_VIEW_MODEL )
    public XPage viewModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        ModelAccess access = loadModelAndCheck( request, Model::isUserCanView, true );
        if ( access.hasRedirect( ) )
        {
            return access.redirect( );
        }

        Model modelObj = access.model( );
        Models model = _models;
        model.put( MARK_MODEL, modelObj );
        model.put( MARK_CLIENT_ID, String.valueOf( modelObj.getClientId( ) ) );

        addModelSidebarToModel( model, modelObj, VIEW_VIEW_MODEL );

        return getXPage( TEMPLATE_VIEW_MODEL, getLocale( request ), model );
    }

    /**
     * Returns the model API documentation view.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_API_MODEL )
    public XPage apiModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        ModelAccess access = loadModelAndCheck( request, Model::isUserCanView, true );
        if ( access.hasRedirect( ) )
        {
            return access.redirect( );
        }

        Optional<Model> optApiModel = _modelCrudService.getModelWithProvider( access.model( ).getId( ) );
        if ( optApiModel.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_MODEL_NOT_FOUND );
        }

        Model modelObj = optApiModel.get( );
        Models model = _models;
        model.put( MARK_MODEL, modelObj );
        model.put( MARK_CLIENT_ID, String.valueOf( modelObj.getClientId( ) ) );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );

        addModelSidebarToModel( model, modelObj, VIEW_API_MODEL );

        return getXPage( TEMPLATE_API_MODEL, getLocale( request ), model );
    }

    /**
     * Displays the create model view page.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( value = VIEW_CREATE_MODEL, defaultView = true )
    public XPage createModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateModel( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_USER, user );
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        model.put( MARK_PROVIDERS, ProviderHome.getProvidersList( ) );

        return getXPage( TEMPLATE_CREATE_MODEL, locale, model );
    }

    /**
     * Displays the modify model view page.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_MODIFY_MODEL )
    public XPage modifyModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        ModelAccess access = loadModelAndCheck( request, Model::isUserCanModify, false );
        if ( access.hasRedirect( ) )
        {
            return access.redirect( );
        }

        Model modelObj = access.model( );
        Models model = _models;
        model.put( MARK_MODEL, modelObj );
        model.put( MARK_USER, access.user( ) );
        model.put( MARK_PROVIDERS, ProviderHome.getProvidersList( ) );
        model.put( MARK_CLIENT_ID, String.valueOf( modelObj.getClientId( ) ) );

        addModelSidebarToModel( model, modelObj, VIEW_MODIFY_MODEL );

        return getXPage( TEMPLATE_MODIFY_MODEL, getLocale( request ), model );
    }

    /**
     * Handles the create model action.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     */
    @Action( ACTION_CREATE_MODEL )
    public XPage doCreateModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateModel( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        int nClientId = Integer.parseInt( strClientId );
        int nProviderId = Integer.parseInt( request.getParameter( PARAMETER_PROVIDER_ID ) );

        ModelCreateResult result = _modelCrudService.createModel( nClientId, nProviderId );
        if ( result.status( ) == ModelCreateResult.ModelCreateStatus.DUPLICATE )
        {
            return redirectToPortalWithMessage( request, MESSAGE_MODEL_ALREADY_EXISTS );
        }

        addInfo( INFO_MODEL_CREATED, locale );
        return redirectToViewModel( request, result.model( ).getId( ), strClientId );
    }

    /**
     * Handles the modify model action.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     */
    @Action( ACTION_MODIFY_MODEL )
    public XPage doModifyModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        ModelAccess access = loadModelAndCheck( request, Model::isUserCanModify, false );
        if ( access.hasRedirect( ) )
        {
            return access.redirect( );
        }

        int nProviderId = Integer.parseInt( request.getParameter( PARAMETER_PROVIDER_ID ) );
        try
        {
            _modelCrudService.updateModelProvider( access.model( ).getId( ), nProviderId );
        }
        catch( ResourceNotFoundException e )
        {
            return redirectToPortalWithMessage( request, MESSAGE_MODEL_NOT_FOUND );
        }

        addInfo( INFO_MODEL_UPDATED, getLocale( request ) );
        return redirectToViewModel( request, access.model( ).getId( ), request.getParameter( PARAMETER_CLIENT_ID ) );
    }

    /**
     * Shows a confirmation message before removing a model.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_MODEL )
    public XPage confirmRemoveModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nModelId = Integer.parseInt( request.getParameter( PARAMETER_MODEL_ID ) );
        Optional<Model> optModel = _modelCrudService.getModelWithProvider( nModelId );

        if ( optModel.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_MODEL_NOT_FOUND );
        }
        if ( !AgentRBACService.canDeleteModel( optModel.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_MODEL, MESSAGE_CONFIRM_REMOVE_MODEL, PARAMETER_MODEL_ID );
    }

    /**
     * Handles the remove model action.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     */
    @Action( ACTION_REMOVE_MODEL )
    public XPage doRemoveModel( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        ModelAccess access = loadModelAndCheck( request, Model::isUserCanDelete, false );
        if ( access.hasRedirect( ) )
        {
            return access.redirect( );
        }

        int nClientId;
        try
        {
            nClientId = _modelCrudService.removeModel( access.model( ).getId( ) );
        }
        catch( ResourceNotFoundException e )
        {
            return redirectToPortalWithMessage( request, MESSAGE_MODEL_NOT_FOUND );
        }

        addInfo( INFO_MODEL_REMOVED, getLocale( request ) );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Loads the requested model, enriches it with RBAC permissions and applies the given access predicate. Centralizes the parse model_id, lookup, not-found
     * and permission-denied handling shared by every model handler.
     *
     * @param request
     *            the HTTP request
     * @param accessPredicate
     *            the permission predicate the user must satisfy on the model
     * @param redirectToPortalOnDenied
     *            {@code true} to redirect to the portal with an access-denied message, {@code false} to add an error and redirect back to the model view
     * @return the access outcome, carrying either the loaded model or the redirect to return
     * @throws UserNotSignedException
     *             if no user is signed in
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    private ModelAccess loadModelAndCheck( HttpServletRequest request, Predicate<Model> accessPredicate, boolean redirectToPortalOnDenied )
            throws UserNotSignedException, SiteMessageException
    {
        int nModelId = Integer.parseInt( request.getParameter( PARAMETER_MODEL_ID ) );
        Optional<Model> optModel = _modelCrudService.getModelWithProvider( nModelId );

        if ( optModel.isEmpty( ) )
        {
            return ModelAccess.redirect( redirectToPortalWithMessage( request, MESSAGE_MODEL_NOT_FOUND ) );
        }

        Model modelObj = optModel.get( );
        LuteceUser user = requireUser( request );
        AgentRBACService.enrichWithPermissions( modelObj, user );

        if ( !accessPredicate.test( modelObj ) )
        {
            if ( redirectToPortalOnDenied )
            {
                return ModelAccess.redirect( redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED ) );
            }
            addError( MESSAGE_ACCESS_DENIED, getLocale( request ) );
            return ModelAccess.redirect( redirectToResourceView( request, VIEW_VIEW_MODEL, PARAMETER_MODEL_ID, modelObj.getId( ) ) );
        }

        return ModelAccess.granted( modelObj, user );
    }

    /**
     * Builds a redirect to the model view, preserving the client_id parameter when present.
     *
     * @param request
     *            the HTTP request
     * @param nModelId
     *            the model identifier to display
     * @param strClientId
     *            the owning client identifier, may be {@code null}
     * @return the redirect XPage
     * @throws UserNotSignedException
     *             if no user is signed in
     */
    private XPage redirectToViewModel( HttpServletRequest request, int nModelId, String strClientId ) throws UserNotSignedException
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_MODEL_ID, String.valueOf( nModelId ) );
        if ( strClientId != null )
        {
            params.put( PARAMETER_CLIENT_ID, strClientId );
        }
        return redirect( request, VIEW_VIEW_MODEL, params );
    }

    /**
     * Adds the model sidebar data (current view and publication status) to the view model.
     *
     * @param model
     *            the view model to enrich
     * @param modelObj
     *            the provider-enriched model being displayed
     * @param currentView
     *            the name of the current view
     */
    private void addModelSidebarToModel( Models model, Model modelObj, String currentView )
    {
        model.put( MARK_HEADER_CURRENT_VIEW, currentView );
        addPublicationStatusToModel( model, Model.RESOURCE_TYPE, modelObj.getId( ) );
    }

    /**
     * Outcome of {@link #loadModelAndCheck(HttpServletRequest, Predicate, boolean)}: either the granted model with its user, or a redirect to return as-is.
     *
     * @param model
     *            the loaded model when access is granted, {@code null} otherwise
     * @param user
     *            the signed-in user when access is granted, {@code null} otherwise
     * @param redirect
     *            the redirect XPage when access is denied or the model is not found, {@code null} otherwise
     */
    private record ModelAccess(Model model, LuteceUser user, XPage redirect) {
        /**
         * Builds a granted outcome.
         *
         * @param model
         *            the loaded model
         * @param user
         *            the signed-in user
         * @return a granted access outcome
         */
        static ModelAccess granted( Model model, LuteceUser user )
        {
            return new ModelAccess( model, user, null );
        }

        /**
         * Builds a denied outcome carrying the redirect to return.
         *
         * @param redirect
         *            the redirect XPage
         * @return a redirect access outcome
         */
        static ModelAccess redirect( XPage redirect )
        {
            return new ModelAccess( null, null, redirect );
        }

        /**
         * Tells whether this outcome is a redirect rather than a granted access.
         *
         * @return {@code true} if a redirect must be returned
         */
        boolean hasRedirect( )
        {
            return redirect != null;
        }
    }
}
