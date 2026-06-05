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
package fr.paris.lutece.plugins.platform.service.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionHome;
import fr.paris.lutece.plugins.platform.service.rbac.PlatformClientResourceIdService;
import fr.paris.lutece.plugins.platform.service.rbac.PlatformObservabilityResourceIdService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.CryptoService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Service class for managing Client entities with RBAC integration
 */
@ApplicationScoped
@Named( "platform.clientService" )
public class ClientService
{
    private static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    private static final String MESSAGE_ACCESS_DENIED = "platform.message.error.permission.denied";
    private static final String RBAC_WILDCARD = "*";
    private static final String API_KEY_PREFIX = "pk_";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int API_KEY_RANDOM_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom( );

    ClientService( )
    {
    }

    /**
     * Generates a new high-entropy API key (prefix + 32 random bytes, base64url without padding).
     *
     * @return the cleartext API key
     */
    public String generateApiKey( )
    {
        byte [ ] randomBytes = new byte [ API_KEY_RANDOM_BYTES];
        RANDOM.nextBytes( randomBytes );
        return API_KEY_PREFIX + Base64.getUrlEncoder( ).withoutPadding( ).encodeToString( randomBytes );
    }

    /**
     * Computes the storable SHA-256 hex digest of a cleartext API key. The digest is deterministic (no salt) so authentication can match the presented key
     * against the stored hash; the key entropy makes brute force infeasible.
     *
     * @param apiKey
     *            the cleartext API key
     * @return the hex-encoded SHA-256 digest
     */
    public String hashApiKey( String apiKey )
    {
        return CryptoService.encrypt( apiKey, HASH_ALGORITHM );
    }

    /**
     * Get all clients that the user has permission to view
     *
     * @param user
     *            The current user
     * @return Collection of authorized clients
     */
    public Collection<Client> getAuthorizedClients( User user )
    {
        return RBACService.getAuthorizedCollection( ClientHome.getClientsList( ), PlatformClientResourceIdService.PERMISSION_VIEW, user );
    }

    /**
     * Gets all clients the user may view, each enriched with its RBAC permission flags and total cost, ready to render. Wraps the per-client permission
     * enrichment loop and the single-query total-cost enrichment so the controller gets a ready-to-render collection.
     *
     * @param user
     *            The current user
     * @return the enriched authorized clients
     */
    public Collection<Client> getAuthorizedClientsEnriched( User user )
    {
        Collection<Client> clients = getAuthorizedClients( user );
        for ( Client client : clients )
        {
            enrichClientWithPermissions( client, user );
        }
        enrichClientsWithTotalCost( clients );
        return clients;
    }

    /**
     * Get list of authorized client IDs
     *
     * @param user
     *            The current user
     * @return List of client IDs
     */
    public List<Integer> getAuthorizedClientIds( User user )
    {
        return getAuthorizedClients( user ).stream( ).map( Client::getId ).toList( );
    }

    /**
     * Get a client by ID if user has view permission
     *
     * @param id
     *            The client ID
     * @param user
     *            The current user
     * @return The client if authorized
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public Client getClient( int id, User user ) throws AccessDeniedException
    {
        checkPermission( id, PlatformClientResourceIdService.PERMISSION_VIEW, user );

        Optional<Client> client = ClientHome.findByPrimaryKey( id );
        return client.orElseThrow( ( ) -> new AppException( ERROR_RESOURCE_NOT_FOUND ) );
    }

    /**
     * Create a new client if user has create permission. A fresh API key is generated server-side; only its SHA-256 hash is persisted. The cleartext key is
     * returned to the caller for one-time display and can never be retrieved again.
     *
     * @param client
     *            The client to create
     * @param user
     *            The current user
     * @return the cleartext API key, shown once
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public String createClient( Client client, User user ) throws AccessDeniedException
    {
        checkPermission( RBAC_WILDCARD, PlatformClientResourceIdService.PERMISSION_CREATE, user );
        String apiKey = generateApiKey( );
        client.setCode( hashApiKey( apiKey ) );
        ClientHome.create( client );
        return apiKey;
    }

    /**
     * Regenerates the API key of an existing client if user has modify permission. The previous key stops working immediately; only the new hash is persisted
     * and the cleartext key is returned for one-time display.
     *
     * @param id
     *            The client ID
     * @param user
     *            The current user
     * @return the new cleartext API key, shown once
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public String regenerateApiKey( int id, User user ) throws AccessDeniedException
    {
        checkPermission( id, PlatformClientResourceIdService.PERMISSION_MODIFY, user );
        Client client = ClientHome.findByPrimaryKey( id ).orElseThrow( ( ) -> new AppException( ERROR_RESOURCE_NOT_FOUND ) );
        String apiKey = generateApiKey( );
        client.setCode( hashApiKey( apiKey ) );
        ClientHome.update( client );
        return apiKey;
    }

    /**
     * Update a client if user has modify permission
     *
     * @param client
     *            The client to update
     * @param user
     *            The current user
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public void updateClient( Client client, User user ) throws AccessDeniedException
    {
        checkPermission( client.getId( ), PlatformClientResourceIdService.PERMISSION_MODIFY, user );
        ClientHome.update( client );
    }

    /**
     * Delete a client if user has delete permission
     *
     * @param id
     *            The client ID to delete
     * @param user
     *            The current user
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public void deleteClient( int id, User user ) throws AccessDeniedException
    {
        checkPermission( id, PlatformClientResourceIdService.PERMISSION_DELETE, user );
        ClientHome.remove( id );
    }

    /**
     * Get all permissions for the current user
     *
     * @param user
     *            The current user
     * @return Map of permission names to boolean values
     */
    public Map<String, Boolean> getPermissions( User user )
    {
        Map<String, Boolean> permissions = new HashMap<>( );

        permissions.put( "canCreate", RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, RBAC_WILDCARD,
                PlatformClientResourceIdService.PERMISSION_CREATE, user ) );

        Collection<Client> authorizedClients = getAuthorizedClients( user );
        permissions.put( "canView", !authorizedClients.isEmpty( ) );

        boolean canModify = false;
        boolean canDelete = false;

        for ( Client client : ClientHome.getClientsList( ) )
        {
            if ( RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( client.getId( ) ),
                    PlatformClientResourceIdService.PERMISSION_MODIFY, user ) )
            {
                canModify = true;
            }

            if ( RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, String.valueOf( client.getId( ) ),
                    PlatformClientResourceIdService.PERMISSION_DELETE, user ) )
            {
                canDelete = true;
            }

            if ( canModify && canDelete )
            {
                break;
            }
        }

        permissions.put( "canModify", canModify );
        permissions.put( "canDelete", canDelete );

        return permissions;
    }

    /**
     * Enriches a client object with RBAC permissions for the current user
     *
     * @param client
     *            The client to enrich
     * @param user
     *            The current user
     */
    public void enrichClientWithPermissions( Client client, User user )
    {
        String clientId = String.valueOf( client.getId( ) );

        boolean canView = RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, clientId, PlatformClientResourceIdService.PERMISSION_VIEW,
                user );

        boolean canModify = RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, clientId,
                PlatformClientResourceIdService.PERMISSION_MODIFY, user );

        boolean canDelete = RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, clientId,
                PlatformClientResourceIdService.PERMISSION_DELETE, user );

        boolean canViewObservability = RBACService.isAuthorized( PlatformObservabilityResourceIdService.RESOURCE_TYPE, clientId,
                PlatformObservabilityResourceIdService.PERMISSION_VIEW_OBSERVABILITY, user );

        client.setUserCanView( canView );
        client.setUserCanModify( canModify );
        client.setUserCanDelete( canDelete );
        client.setUserCanViewObservability( canViewObservability );
    }

    /**
     * Enriches a collection of clients with their total cost from observability stats. Uses a single query to get costs for all clients.
     *
     * @param clients
     *            The clients to enrich
     */
    public void enrichClientsWithTotalCost( Collection<Client> clients )
    {
        Map<Integer, Double> costByClient = PlatformResourceExecutionHome.getTotalCostByClient( );
        for ( Client client : clients )
        {
            Double cost = costByClient.get( client.getId( ) );
            client.setTotalCost( cost != null ? cost : 0.0 );
        }
    }

    /**
     * Check if user has permission to view observability for a client
     *
     * @param id
     *            The client ID
     * @param user
     *            The current user
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    public void checkObservabilityPermission( int id, User user ) throws AccessDeniedException
    {
        String resourceId = String.valueOf( id );

        if ( !RBACService.isAuthorized( PlatformObservabilityResourceIdService.RESOURCE_TYPE, resourceId,
                PlatformObservabilityResourceIdService.PERMISSION_VIEW_OBSERVABILITY, user ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }

    /**
     * Check if user has specific permission for a client
     *
     * @param id
     *            The client ID or wildcard
     * @param permission
     *            The permission to check
     * @param user
     *            The current user
     * @throws AccessDeniedException
     *             If user lacks permission
     */
    private void checkPermission( Object id, String permission, User user ) throws AccessDeniedException
    {
        String resourceId = id instanceof Integer ? String.valueOf( id ) : (String) id;

        if ( !RBACService.isAuthorized( PlatformClientResourceIdService.RESOURCE_TYPE, resourceId, permission, user ) )
        {
            throw new AccessDeniedException( MESSAGE_ACCESS_DENIED );
        }
    }
}
