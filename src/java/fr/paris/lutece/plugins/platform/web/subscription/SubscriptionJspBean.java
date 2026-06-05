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
package fr.paris.lutece.plugins.platform.web.subscription;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractPlatformJspBean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.plugins.platform.service.subscription.dto.SubscriptionListView;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.util.url.UrlItem;

@RequestScoped
@Named
@Controller( controllerJsp = "ManageSubscriptions.jsp", controllerPath = "jsp/admin/plugins/platform/", right = "PLATFORM_SUBSCRIPTION_MANAGEMENT" )
public class SubscriptionJspBean extends AbstractPlatformJspBean
{
    @Inject
    private Models _models;

    private static final String TEMPLATE_MANAGE_SUBSCRIPTIONS = "/admin/plugins/platform/manage_subscriptions.html";

    private static final String PARAMETER_ID_SUBSCRIPTION = "id_subscription";
    private static final String PARAMETER_ID_CLIENT = "id_client";

    private static final String PROPERTY_PAGE_TITLE_MANAGE_SUBSCRIPTIONS = "platform.manage_subscriptions.pageTitle";

    private static final String MARK_SUBSCRIPTION_LIST = "subscription_list";
    private static final String MARK_CLIENT_LIST = "client_list";
    private static final String MARK_PERMISSIONS = "permissions";
    private static final String MARK_CLIENT = "client";

    private static final String VIEW_MANAGE_SUBSCRIPTIONS = "manageSubscriptions";

    private static final String ACTION_DELETE_SUBSCRIPTION = "deleteSubscription";
    private static final String ACTION_CANCEL_SUBSCRIPTION = "cancelSubscription";
    private static final String ACTION_CONFIRM_DELETE_SUBSCRIPTION = "confirmDeleteSubscription";

    private static final String INFO_SUBSCRIPTION_DELETED = "platform.info.subscription.deleted";
    private static final String INFO_SUBSCRIPTION_CANCELLED = "platform.info.subscription.cancelled";
    private static final String ERROR_SUBSCRIPTION_NOT_FOUND = "platform.error.subscription.notFound";

    private static final String MESSAGE_CONFIRM_DELETE_SUBSCRIPTION = "platform.message.confirmDeleteSubscription";

    @Inject
    private SubscriptionService _subscriptionService;

    /**
     * Returns the page to manage subscriptions
     *
     * @param request
     *            The HTTP request
     * @return The page to manage subscriptions
     */
    @View( value = VIEW_MANAGE_SUBSCRIPTIONS, defaultView = true )
    public String getManageSubscriptions( HttpServletRequest request )
    {
        User user = (User) getUser( );
        SubscriptionListView view = _subscriptionService.resolveSubscriptions( request.getParameter( PARAMETER_ID_CLIENT ), user );

        Models model = _models;
        addRBACPermissionsToModel( model );
        model.put( MARK_SUBSCRIPTION_LIST, view.getSubscriptions( ) );
        model.put( MARK_CLIENT_LIST, _clientService.getAuthorizedClients( user ) );
        model.put( MARK_PERMISSIONS, _clientService.getPermissions( user ) );
        model.put( MARK_CLIENT, view.getClient( ) );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_SUBSCRIPTIONS, TEMPLATE_MANAGE_SUBSCRIPTIONS, model );
    }

    /**
     * Returns the confirmation page for subscription deletion
     *
     * @param request
     *            The HTTP request
     * @return The confirmation page
     */
    @Action( ACTION_CONFIRM_DELETE_SUBSCRIPTION )
    public String getConfirmDeleteSubscription( HttpServletRequest request )
    {
        String strId = request.getParameter( PARAMETER_ID_SUBSCRIPTION );
        UrlItem url = new UrlItem( getActionUrl( ACTION_DELETE_SUBSCRIPTION ) );
        url.addParameter( PARAMETER_ID_SUBSCRIPTION, strId );

        String strClientId = request.getParameter( PARAMETER_ID_CLIENT );
        if ( strClientId != null && !strClientId.isEmpty( ) )
        {
            url.addParameter( PARAMETER_ID_CLIENT, strClientId );
        }

        return redirect( request,
                AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_DELETE_SUBSCRIPTION, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Processes the deletion of a subscription
     *
     * @param request
     *            The HTTP request
     * @return The next URL to redirect to
     * @throws AccessDeniedException
     *             If the user is not authorized
     */
    @Action( ACTION_DELETE_SUBSCRIPTION )
    public String doDeleteSubscription( HttpServletRequest request ) throws AccessDeniedException
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_SUBSCRIPTION ) );
        if ( _subscriptionService.deleteSubscription( nId, (User) getUser( ) ) )
        {
            addInfo( INFO_SUBSCRIPTION_DELETED, getLocale( ) );
        }
        else
        {
            addError( ERROR_SUBSCRIPTION_NOT_FOUND, getLocale( ) );
        }

        return redirectView( request, VIEW_MANAGE_SUBSCRIPTIONS );
    }

    /**
     * Processes the cancellation of a subscription
     *
     * @param request
     *            The HTTP request
     * @return The next URL to redirect to
     * @throws AccessDeniedException
     *             If the user is not authorized
     */
    @Action( ACTION_CANCEL_SUBSCRIPTION )
    public String doCancelSubscription( HttpServletRequest request ) throws AccessDeniedException
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_ID_SUBSCRIPTION ) );
        if ( _subscriptionService.unsubscribe( nId, (User) getUser( ) ) >= 0 )
        {
            addInfo( INFO_SUBSCRIPTION_CANCELLED, getLocale( ) );
        }
        else
        {
            addError( ERROR_SUBSCRIPTION_NOT_FOUND, getLocale( ) );
        }

        return redirectView( request, VIEW_MANAGE_SUBSCRIPTIONS );
    }
}
