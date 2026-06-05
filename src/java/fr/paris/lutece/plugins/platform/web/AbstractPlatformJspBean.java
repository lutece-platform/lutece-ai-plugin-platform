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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.util.ErrorMessage;

public abstract class AbstractPlatformJspBean extends MVCAdminJspBean
{
    @Inject
    protected ClientService _clientService;

    protected static final String PARAMETER_CLIENT_ID = "client_id";
    protected static final String MARK_CLIENT = "client";
    protected static final String PARAMETER_VIEW = "view";
    protected static final String PARAMETER_ID = "id";
    protected static final String PARAMETER_ACTIVE_TAB = "active_tab";
    private static final String SESSION_FLASH_ERRORS = "platform.flash.errors";
    private static final String SESSION_FLASH_INFOS = "platform.flash.infos";
    private static final String SESSION_FLASH_WARNINGS = "platform.flash.warnings";

    /**
     * Redirects to the given view, carrying over the client identifier request parameter when present, plus the supplied parameter.
     *
     * @param request
     *            the HTTP request
     * @param strView
     *            the target view name
     * @param strParamName
     *            the name of the extra parameter to add
     * @param nParamValue
     *            the value of the extra parameter
     * @return the redirect URL
     */
    protected String redirectWithClientIdIfPresent( HttpServletRequest request, String strView, String strParamName, int nParamValue )
    {
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        if ( strClientId != null && !strClientId.isEmpty( ) )
        {
            try
            {
                Map<String, String> additionalParams = new HashMap<>( );
                additionalParams.put( PARAMETER_CLIENT_ID, strClientId );
                additionalParams.put( strParamName, String.valueOf( nParamValue ) );
                return redirect( request, strView, additionalParams );
            }
            catch( NumberFormatException e )
            {
            }
        }
        return redirect( request, strView, strParamName, nParamValue );
    }

    /**
     * Redirects to the given view, carrying over the client identifier request parameter when present.
     *
     * @param request
     *            the HTTP request
     * @param strView
     *            the target view name
     * @return the redirect URL
     */
    protected String redirectViewWithClientIdIfPresent( HttpServletRequest request, String strView )
    {
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        if ( strClientId != null && !strClientId.isEmpty( ) )
        {
            Map<String, String> additionalParams = new HashMap<>( );
            additionalParams.put( PARAMETER_CLIENT_ID, strClientId );
            return redirect( request, strView, additionalParams );
        }
        return redirectView( request, strView );
    }

    /**
     * Save flash messages to session before redirect
     *
     * @param request
     *            The HTTP request
     */
    protected void saveFlashMessages( HttpServletRequest request )
    {
        HttpSession session = request.getSession( );

        List<ErrorMessage> currentErrors = getCurrentErrors( );
        List<ErrorMessage> currentInfos = getCurrentInfos( );
        List<ErrorMessage> currentWarnings = getCurrentWarnings( );

        if ( !currentErrors.isEmpty( ) )
        {
            session.setAttribute( SESSION_FLASH_ERRORS, new ArrayList<>( currentErrors ) );
        }
        if ( !currentInfos.isEmpty( ) )
        {
            session.setAttribute( SESSION_FLASH_INFOS, new ArrayList<>( currentInfos ) );
        }
        if ( !currentWarnings.isEmpty( ) )
        {
            session.setAttribute( SESSION_FLASH_WARNINGS, new ArrayList<>( currentWarnings ) );
        }

        clearCurrentMessages( );
    }

    /**
     * Load flash messages from session and add them to current messages
     *
     * @param request
     *            The HTTP request
     */
    @SuppressWarnings( "unchecked" )
    protected void loadFlashMessages( HttpServletRequest request )
    {
        HttpSession session = request.getSession( false );
        if ( session != null )
        {
            List<ErrorMessage> flashErrors = (List<ErrorMessage>) session.getAttribute( SESSION_FLASH_ERRORS );
            List<ErrorMessage> flashInfos = (List<ErrorMessage>) session.getAttribute( SESSION_FLASH_INFOS );
            List<ErrorMessage> flashWarnings = (List<ErrorMessage>) session.getAttribute( SESSION_FLASH_WARNINGS );

            if ( flashErrors != null )
            {
                for ( ErrorMessage error : flashErrors )
                {
                    addError( error.getMessage( ) );
                }
                session.removeAttribute( SESSION_FLASH_ERRORS );
            }

            if ( flashInfos != null )
            {
                for ( ErrorMessage info : flashInfos )
                {
                    addInfo( info.getMessage( ) );
                }
                session.removeAttribute( SESSION_FLASH_INFOS );
            }

            if ( flashWarnings != null )
            {
                for ( ErrorMessage warning : flashWarnings )
                {
                    addWarning( warning.getMessage( ) );
                }
                session.removeAttribute( SESSION_FLASH_WARNINGS );
            }
        }
    }

    /**
     * Override redirect to save flash messages before redirecting
     */
    @Override
    protected String redirect( HttpServletRequest request, String strTarget )
    {
        saveFlashMessages( request );
        return super.redirect( request, strTarget );
    }

    /**
     * Get current errors from parent class - using reflection since _listErrors is private
     */
    @SuppressWarnings( "unchecked" )
    private List<ErrorMessage> getCurrentErrors( )
    {
        try
        {
            java.lang.reflect.Field field = MVCAdminJspBean.class.getDeclaredField( "_listErrors" );
            field.setAccessible( true );
            return (List<ErrorMessage>) field.get( this );
        }
        catch( Exception e )
        {
            return new ArrayList<>( );
        }
    }

    /**
     * Get current infos from parent class - using reflection since _listInfos is private
     */
    @SuppressWarnings( "unchecked" )
    private List<ErrorMessage> getCurrentInfos( )
    {
        try
        {
            java.lang.reflect.Field field = MVCAdminJspBean.class.getDeclaredField( "_listInfos" );
            field.setAccessible( true );
            return (List<ErrorMessage>) field.get( this );
        }
        catch( Exception e )
        {
            return new ArrayList<>( );
        }
    }

    /**
     * Get current warnings from parent class - using reflection since _listWarnings is private
     */
    @SuppressWarnings( "unchecked" )
    private List<ErrorMessage> getCurrentWarnings( )
    {
        try
        {
            java.lang.reflect.Field field = MVCAdminJspBean.class.getDeclaredField( "_listWarnings" );
            field.setAccessible( true );
            return (List<ErrorMessage>) field.get( this );
        }
        catch( Exception e )
        {
            return new ArrayList<>( );
        }
    }

    /**
     * Clear current messages from parent class - using reflection since fields are private
     */
    private void clearCurrentMessages( )
    {
        try
        {
            java.lang.reflect.Field errorsField = MVCAdminJspBean.class.getDeclaredField( "_listErrors" );
            errorsField.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            List<ErrorMessage> errors = (List<ErrorMessage>) errorsField.get( this );
            errors.clear( );

            java.lang.reflect.Field infosField = MVCAdminJspBean.class.getDeclaredField( "_listInfos" );
            infosField.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            List<ErrorMessage> infos = (List<ErrorMessage>) infosField.get( this );
            infos.clear( );

            java.lang.reflect.Field warningsField = MVCAdminJspBean.class.getDeclaredField( "_listWarnings" );
            warningsField.setAccessible( true );
            @SuppressWarnings( "unchecked" )
            List<ErrorMessage> warnings = (List<ErrorMessage>) warningsField.get( this );
            warnings.clear( );
        }
        catch( Exception e )
        {
        }
    }

    /**
     * Add RBAC permissions to the model for template usage. Subclasses must call this explicitly after building their model, because the deprecated v8
     * getModel() override is no longer used.
     *
     * @param model
     *            The model to add permissions to
     */
    protected void addRBACPermissionsToModel( Models model )
    {
        Map<String, Object> rbac = new HashMap<>( );
        Map<String, Boolean> clientPermissions = _clientService.getPermissions( (User) getUser( ) );
        rbac.put( "client", clientPermissions );
        model.put( "rbac", rbac );
    }
}
