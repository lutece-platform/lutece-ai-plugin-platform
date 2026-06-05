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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;

/**
 * Abstract base class for Platform front-office XPages. Provides flash message handling for messages that survive HTTP redirects.
 */
public abstract class AbstractPlatformFrontXPage extends MVCApplication
{
    private static final long serialVersionUID = 1L;

    private static final String SESSION_FLASH_INFOS = "platform_flash_infos";
    private static final String SESSION_FLASH_ERRORS = "platform_flash_errors";
    private static final String SESSION_FLASH_WARNINGS = "platform_flash_warnings";

    protected static final String MARK_USER = "user";
    protected static final String MARK_CLIENT = "client";
    protected static final String MARK_CLIENT_LIST = "client_list";

    /**
     * Returns the authenticated FO user. Throws {@link UserNotSignedException} if no user is bound to the session — Lutece-core catches the exception and
     * redirects to the login page automatically.
     *
     * @param request
     *            The HTTP request
     * @return the authenticated user (never null)
     * @throws UserNotSignedException
     *             if no user is signed in (triggers redirect to login)
     */
    protected LuteceUser getUser( HttpServletRequest request ) throws UserNotSignedException
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );
        if ( user == null )
        {
            throw new UserNotSignedException( );
        }
        return user;
    }

    /**
     * Adds an info flash message that will survive HTTP redirects
     *
     * @param request
     *            The HTTP request
     * @param strMessageKey
     *            The i18n message key
     */
    protected void addFlashInfo( HttpServletRequest request, String strMessageKey ) throws UserNotSignedException
    {
        String strMessage = I18nService.getLocalizedString( strMessageKey, getLocale( request ) );
        saveFlashMessage( request, SESSION_FLASH_INFOS, strMessage );
    }

    /**
     * Adds an error flash message that will survive HTTP redirects
     *
     * @param request
     *            The HTTP request
     * @param strMessageKey
     *            The i18n message key
     */
    protected void addFlashError( HttpServletRequest request, String strMessageKey ) throws UserNotSignedException
    {
        String strMessage = I18nService.getLocalizedString( strMessageKey, getLocale( request ) );
        saveFlashMessage( request, SESSION_FLASH_ERRORS, strMessage );
    }

    /**
     * Adds a warning flash message that will survive HTTP redirects
     *
     * @param request
     *            The HTTP request
     * @param strMessageKey
     *            The i18n message key
     */
    protected void addFlashWarning( HttpServletRequest request, String strMessageKey ) throws UserNotSignedException
    {
        String strMessage = I18nService.getLocalizedString( strMessageKey, getLocale( request ) );
        saveFlashMessage( request, SESSION_FLASH_WARNINGS, strMessage );
    }

    /**
     * Retrieves and applies flash messages from session. This method should be called at the beginning of each view.
     *
     * @param request
     *            The HTTP request
     */
    protected void applyFlashMessages( HttpServletRequest request ) throws UserNotSignedException
    {
        List<String> infos = getAndClearFlashMessages( request, SESSION_FLASH_INFOS );
        for ( String info : infos )
        {
            addInfo( info );
        }

        List<String> errors = getAndClearFlashMessages( request, SESSION_FLASH_ERRORS );
        for ( String error : errors )
        {
            addError( error );
        }

        List<String> warnings = getAndClearFlashMessages( request, SESSION_FLASH_WARNINGS );
        for ( String warning : warnings )
        {
            addWarning( warning );
        }
    }

    /**
     * Adds RBAC permissions to the model for the current user
     *
     * @param model
     *            The model map
     * @param user
     *            The current user (LuteceUser implements User)
     * @param clientService
     *            The client service
     */
    protected void addRBACPermissionsToModel( Models model, User user, ClientService clientService )
    {
        Map<String, Object> rbac = new HashMap<>( );
        Map<String, Boolean> clientPermissions = clientService.getPermissions( user );
        rbac.put( "client", clientPermissions );
        model.put( "rbac", rbac );
    }

    /**
     * Saves a flash message in session
     */
    @SuppressWarnings( "unchecked" )
    private void saveFlashMessage( HttpServletRequest request, String sessionKey, String strMessage ) throws UserNotSignedException
    {
        HttpSession session = request.getSession( true );
        List<String> messages = (List<String>) session.getAttribute( sessionKey );
        if ( messages == null )
        {
            messages = new ArrayList<>( );
        }
        messages.add( strMessage );
        session.setAttribute( sessionKey, messages );
    }

    /**
     * Retrieves and clears flash messages from session
     */
    @SuppressWarnings( "unchecked" )
    private List<String> getAndClearFlashMessages( HttpServletRequest request, String sessionKey ) throws UserNotSignedException
    {
        HttpSession session = request.getSession( true );
        List<String> messages = (List<String>) session.getAttribute( sessionKey );
        if ( messages == null )
        {
            messages = new ArrayList<>( );
        }
        session.removeAttribute( sessionKey );
        return messages;
    }
}
