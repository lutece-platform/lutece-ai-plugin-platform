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
package fr.paris.lutece.plugins.platform.service.subscription;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.rbac.PlatformClientResourceIdService;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.plugins.platform.service.subscription.dto.SubscriptionListView;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.rbac.RBACService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service class for managing platform subscriptions.
 */
@ApplicationScoped
public class SubscriptionService
{

    @Inject
    private ClientService _clientService;

    /**
     * Resolves the subscription list for the manage view, applying an optional client filter. Parses the raw client identifier, performs an RBAC-checked client
     * lookup and returns the subscriptions of that client; on a missing, invalid or unauthorized client filter it falls back safely to the full subscription
     * list.
     *
     * @param strClientId
     *            the raw client identifier request parameter, may be null or empty
     * @param user
     *            the user performing the lookup
     * @return a typed view object carrying the resolved client (null when the full list is returned) and the subscriptions to display
     */
    public SubscriptionListView resolveSubscriptions( String strClientId, User user )
    {
        if ( strClientId != null && !strClientId.isEmpty( ) )
        {
            try
            {
                int nClientId = Integer.parseInt( strClientId );
                Client client = _clientService.getClient( nClientId, user );
                return new SubscriptionListView( client, SubscriptionHome.getSubscriptionsByClientId( nClientId ) );
            }
            catch( NumberFormatException | AccessDeniedException e )
            {
                return new SubscriptionListView( null, SubscriptionHome.getSubscriptionsList( ) );
            }
        }
        return new SubscriptionListView( null, SubscriptionHome.getSubscriptionsList( ) );
    }

    /**
     * Retrieves all clients that the user has the specified permission for.
     *
     * @param user
     *            The user to check permissions for
     * @param permission
     *            The required permission
     * @return Collection of authorized clients
     */
    public Collection<Client> getAuthorizedClientsForUser( User user, String permission )
    {
        return RBACService.getAuthorizedCollection( ClientHome.getClientsList( ), permission, user );
    }

    /**
     * Retrieves all clients that the user can modify subscriptions for.
     *
     * @param user
     *            The user to check permissions for
     * @return Collection of authorized clients
     */
    public Collection<Client> getAuthorizedSubscriptionClients( User user )
    {
        return getAuthorizedClientsForUser( user, PlatformClientResourceIdService.PERMISSION_MODIFY );
    }

    /**
     * Creates or reactivates a subscription for a client to a resource.
     *
     * @param clientId
     *            The client ID
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The resource identifier
     * @param user
     *            The user performing the action
     * @return The created or updated subscription
     * @throws AccessDeniedException
     *             If the user lacks required permissions
     */
    public Subscription subscribe( int clientId, String resourceType, String resourceId, User user ) throws AccessDeniedException
    {
        if ( !RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( clientId ),
                PlatformClientResourceIdService.PERMISSION_MODIFY, user ) )
        {
            throw new AccessDeniedException( "Unauthorized subscription attempt" );
        }

        Subscription subscription = SubscriptionHome.findByClientAndResource( clientId, resourceType, resourceId ).orElse( null );
        if ( subscription != null )
        {
            if ( subscription.getStatus( ) != SubscriptionStatus.ACTIVE )
            {
                subscription.setStatus( SubscriptionStatus.ACTIVE );
                subscription.setSubscriptionDate( new Timestamp( System.currentTimeMillis( ) ) );
                return SubscriptionHome.update( subscription );
            }
            return subscription;
        }

        subscription = new Subscription( );
        subscription.setClientId( clientId );
        subscription.setResourceType( resourceType );
        subscription.setResourceId( resourceId );
        subscription.setStatus( SubscriptionStatus.ACTIVE );
        subscription.setSubscriptionDate( new Timestamp( System.currentTimeMillis( ) ) );
        return SubscriptionHome.create( subscription );
    }

    /**
     * Cancels a subscription after verifying user permissions and returns the owning client identifier so the caller can redirect without reaching into the
     * Home itself.
     *
     * @param subscriptionId
     *            The subscription to cancel
     * @param user
     *            The user performing the action
     * @return the owning client identifier, or -1 when the subscription no longer exists
     * @throws AccessDeniedException
     *             If the user lacks required permissions
     */
    public int unsubscribe( int subscriptionId, User user ) throws AccessDeniedException
    {
        Subscription subscription = SubscriptionHome.findByPrimaryKey( subscriptionId ).orElse( null );
        if ( subscription == null )
        {
            return -1;
        }

        if ( !RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( subscription.getClientId( ) ),
                PlatformClientResourceIdService.PERMISSION_MODIFY, user ) )
        {
            throw new AccessDeniedException( "Unauthorized unsubscription attempt" );
        }

        subscription.setStatus( SubscriptionStatus.CANCELLED );
        SubscriptionHome.update( subscription );
        return subscription.getClientId( );
    }

    /**
     * Cancels a subscription without permission check.
     *
     * @param subscriptionId
     *            The subscription to cancel
     * @throws AccessDeniedException
     *             If an error occurs during update
     */
    public void unsubscribe( int subscriptionId ) throws AccessDeniedException
    {
        Subscription subscription = SubscriptionHome.findByPrimaryKey( subscriptionId ).orElse( null );
        if ( subscription == null )
        {
            return;
        }

        subscription.setStatus( SubscriptionStatus.CANCELLED );
        SubscriptionHome.update( subscription );
    }

    /**
     * Suspends a subscription after verifying user permissions.
     *
     * @param subscriptionId
     *            The subscription to suspend
     * @param user
     *            The user performing the action
     * @return true when the subscription existed and was suspended, false when it no longer exists
     * @throws AccessDeniedException
     *             If the user lacks required permissions
     */
    public boolean suspend( int subscriptionId, User user ) throws AccessDeniedException
    {
        Subscription subscription = SubscriptionHome.findByPrimaryKey( subscriptionId ).orElse( null );
        if ( subscription == null )
        {
            return false;
        }

        if ( !RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( subscription.getClientId( ) ),
                PlatformClientResourceIdService.PERMISSION_MODIFY, user ) )
        {
            throw new AccessDeniedException( "Unauthorized suspension attempt" );
        }

        subscription.setStatus( SubscriptionStatus.SUSPENDED );
        SubscriptionHome.update( subscription );
        return true;
    }

    /**
     * Retrieves all subscriptions for a given resource.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The resource identifier
     * @return List of subscriptions for the resource
     */
    public List<Subscription> getResourceSubscriptions( String resourceType, String resourceId )
    {
        return SubscriptionHome.getSubscriptionsByResource( resourceType, resourceId );
    }

    /**
     * Retrieves all subscriptions for a given client.
     *
     * @param nClientId
     *            The client identifier
     * @return List of subscriptions for the client
     */
    public List<Subscription> getSubscriptionsByClientId( int nClientId )
    {
        return SubscriptionHome.getSubscriptionsByClientId( nClientId );
    }

    /**
     * Permanently deletes a subscription after verifying user permissions.
     *
     * @param subscriptionId
     *            The subscription to delete
     * @param user
     *            The user performing the action
     * @return true when the subscription existed and was deleted, false when it no longer exists
     * @throws AccessDeniedException
     *             If the user lacks required permissions
     */
    public boolean deleteSubscription( int subscriptionId, User user ) throws AccessDeniedException
    {
        Subscription subscription = SubscriptionHome.findByPrimaryKey( subscriptionId ).orElse( null );
        if ( subscription == null )
        {
            return false;
        }

        ClientHome.findByPrimaryKey( subscription.getClientId( ) ).orElseThrow( ( ) -> new AccessDeniedException( "Invalid client" ) );

        if ( !RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( subscription.getClientId( ) ),
                PlatformClientResourceIdService.PERMISSION_DELETE, user ) )
        {
            throw new AccessDeniedException( "User not authorized to delete subscriptions for this client" );
        }

        SubscriptionHome.remove( subscriptionId );
        return true;
    }

    /**
     * Checks if a client has an active subscription to a resource.
     *
     * @param clientId
     *            The client ID
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The resource identifier
     * @return true if an active subscription exists, false otherwise
     */
    public boolean hasActiveSubscription( int clientId, String resourceType, String resourceId )
    {
        Optional<Subscription> subscription = SubscriptionHome.findByClientAndResource( clientId, resourceType, resourceId );
        return subscription.map( sub -> sub.getStatus( ) == SubscriptionStatus.ACTIVE ).orElse( false );
    }
}
