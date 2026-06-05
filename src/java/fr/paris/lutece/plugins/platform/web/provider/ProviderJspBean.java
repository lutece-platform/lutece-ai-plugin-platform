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
package fr.paris.lutece.plugins.platform.web.provider;

import jakarta.inject.Inject;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.util.url.UrlItem;

/**
 * JSP Bean for managing Agent Provider administration
 */
@RequestScoped
@Named
@Controller( controllerJsp = "ManageAgentProviders.jsp", controllerPath = "jsp/admin/plugins/platform/", right = "PLATFORM_CONFIG_MANAGEMENT" )
public class ProviderJspBean extends MVCAdminJspBean
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_MANAGE_PROVIDERS = "/admin/plugins/platform/manage_providers.html";
    private static final String TEMPLATE_CREATE_PROVIDER = "/admin/plugins/platform/create_provider.html";
    private static final String TEMPLATE_MODIFY_PROVIDER = "/admin/plugins/platform/modify_provider.html";

    private static final String PARAMETER_PROVIDER_ID = "provider_id";

    private static final String MARK_PROVIDER = "provider";
    private static final String MARK_PROVIDER_LIST = "provider_list";

    private static final String VIEW_MANAGE_PROVIDERS = "manageProviders";
    private static final String VIEW_CREATE_PROVIDER = "createProvider";
    private static final String VIEW_MODIFY_PROVIDER = "modifyProvider";

    private static final String ACTION_CREATE_PROVIDER = "createProvider";
    private static final String ACTION_MODIFY_PROVIDER = "modifyProvider";
    private static final String ACTION_CONFIRM_REMOVE_PROVIDER = "confirmRemoveProvider";
    private static final String ACTION_REMOVE_PROVIDER = "removeProvider";

    private static final String MESSAGE_PROVIDER_CREATED = "platform.agent.info.provider.created";
    private static final String MESSAGE_PROVIDER_UPDATED = "platform.agent.info.provider.updated";
    private static final String MESSAGE_PROVIDER_REMOVED = "platform.agent.info.provider.removed";
    private static final String MESSAGE_CONFIRM_REMOVE_PROVIDER = "platform.agent.message.confirmRemoveProvider";
    private static final String MESSAGE_PROVIDER_NOT_FOUND = "platform.agent.error.provider.not.found";
    private static final String MESSAGE_PROVIDER_VALIDATION_ERROR = "platform.agent.error.provider.validation";
    private static final String MESSAGE_MANAGE_PROVIDERS_PAGE_TITLE = "platform.agent.manage_providers.pageTitle";
    private static final String MESSAGE_CREATE_PROVIDER_PAGE_TITLE = "platform.agent.create_provider.pageTitle";
    private static final String MESSAGE_MODIFY_PROVIDER_PAGE_TITLE = "platform.agent.modify_provider.pageTitle";

    private static final String VALIDATION_ATTRIBUTES_PREFIX = "platform.agent";

    /**
     * Gets the providers management page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_MANAGE_PROVIDERS )
    public String getManageProviders( HttpServletRequest request )
    {
        Models model = _models;
        List<Provider> providerList = ProviderHome.getProvidersList( );
        model.put( MARK_PROVIDER_LIST, providerList );

        return getPage( MESSAGE_MANAGE_PROVIDERS_PAGE_TITLE, TEMPLATE_MANAGE_PROVIDERS, model );
    }

    /**
     * Gets the provider creation page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_CREATE_PROVIDER )
    public String getCreateProvider( HttpServletRequest request )
    {
        Models model = _models;

        return getPage( MESSAGE_CREATE_PROVIDER_PAGE_TITLE, TEMPLATE_CREATE_PROVIDER, model );
    }

    /**
     * Process the creation form of a provider
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_CREATE_PROVIDER )
    public String doCreateProvider( HttpServletRequest request )
    {
        Provider provider = new Provider( );
        populate( provider, request );

        if ( !validateBean( provider, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            addError( MESSAGE_PROVIDER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_CREATE_PROVIDER );
        }

        ProviderHome.create( provider );
        addInfo( MESSAGE_PROVIDER_CREATED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE_PROVIDERS );
    }

    /**
     * Gets the provider modification page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_MODIFY_PROVIDER )
    public String getModifyProvider( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_PROVIDER_ID ) );

        return ProviderHome.findByPrimaryKey( nId ).map( provider -> {
            Models model = _models;
            model.put( MARK_PROVIDER, provider );
            return getPage( MESSAGE_MODIFY_PROVIDER_PAGE_TITLE, TEMPLATE_MODIFY_PROVIDER, model );
        } ).orElseGet( ( ) -> {
            addError( MESSAGE_PROVIDER_NOT_FOUND, getLocale( ) );
            return redirectView( request, VIEW_MANAGE_PROVIDERS );
        } );
    }

    /**
     * Process the modification form of a provider
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_MODIFY_PROVIDER )
    public String doModifyProvider( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_PROVIDER_ID ) );

        Optional<Provider> optProvider = ProviderHome.findByPrimaryKey( nId );
        if ( optProvider.isEmpty( ) )
        {
            addError( MESSAGE_PROVIDER_NOT_FOUND, getLocale( ) );
            return redirectView( request, VIEW_MANAGE_PROVIDERS );
        }

        Provider provider = optProvider.get( );
        populate( provider, request );

        if ( !validateBean( provider, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            addError( MESSAGE_PROVIDER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_MODIFY_PROVIDER );
        }

        ProviderHome.update( provider );
        addInfo( MESSAGE_PROVIDER_UPDATED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE_PROVIDERS );
    }

    /**
     * Displays the confirmation message before removing a provider
     *
     * @param request
     *            The HTTP request
     * @return The confirmation page
     */
    @Action( ACTION_CONFIRM_REMOVE_PROVIDER )
    public String getConfirmRemoveProvider( HttpServletRequest request )
    {
        String strId = request.getParameter( PARAMETER_PROVIDER_ID );
        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_PROVIDER ) );
        url.addParameter( PARAMETER_PROVIDER_ID, strId );

        return redirect( request,
                AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_PROVIDER, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Performs the removal of a provider
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_REMOVE_PROVIDER )
    public String doRemoveProvider( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_PROVIDER_ID ) );
        ProviderHome.remove( nId );

        addInfo( MESSAGE_PROVIDER_REMOVED, getLocale( ) );

        return redirectView( request, VIEW_MANAGE_PROVIDERS );
    }
}
