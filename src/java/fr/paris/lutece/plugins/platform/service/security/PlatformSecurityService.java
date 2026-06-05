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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.security.SecurityErrorType;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named( "platform.platformSecurityService" )
public class PlatformSecurityService
{
    private static final String ERROR_MISSING_CLIENT = "Invalid or missing client code";
    private static final String ERROR_CLIENT_NOT_FOUND = "Client not found";
    private static final String ERROR_NO_SUBSCRIPTION = "Client does not have required subscription";
    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ClientService clientService;

    /**
     * Validates a client based on their client code for a specific resource
     *
     * @param clientCode
     *            The client code to validate
     * @param resourceType
     *            The type of resource being accessed
     * @param resourceId
     *            The ID of the resource being accessed
     * @return The validated Client object
     * @throws PlatformSecurityException
     *             If validation fails due to missing or invalid client code, client not found, or missing subscription
     */
    public Client validateClient( String clientCode, String resourceType, String resourceId )
    {
        Client client = requireActiveClient( clientCode );
        if ( !subscriptionService.hasActiveSubscription( client.getId( ), resourceType, resourceId ) )
        {
            throw new PlatformSecurityException( ERROR_NO_SUBSCRIPTION, SecurityErrorType.FORBIDDEN );
        }

        return client;
    }

    /**
     * Validates a client based on their client code without checking resource access
     *
     * @param clientCode
     *            The client code to validate
     * @return The validated Client object
     * @throws PlatformSecurityException
     *             If validation fails due to missing or invalid client code, or client not found
     */
    public Client validateClient( String clientCode )
    {
        return requireActiveClient( clientCode );
    }

    /**
     * Resolves an active client by its API key from the cached active list (config cache, hot path: every REST call goes through here — the list is small and
     * served from memory). The presented key is hashed (SHA-256) and matched against the stored hash in constant time; the cleartext key is never persisted nor
     * logged. Missing or unknown keys raise UNAUTHORIZED; technical failures (DB down...) propagate untouched so they surface as 5xx instead of being disguised
     * as authentication errors.
     *
     * @param clientCode
     *            The cleartext API key from the request
     * @return the active client
     * @throws PlatformSecurityException
     *             if the key is missing or matches no active client
     */
    private Client requireActiveClient( String clientCode )
    {
        if ( clientCode == null || clientCode.trim( ).isEmpty( ) )
        {
            throw new PlatformSecurityException( ERROR_MISSING_CLIENT, SecurityErrorType.UNAUTHORIZED );
        }
        byte [ ] presentedHash = clientService.hashApiKey( clientCode ).getBytes( StandardCharsets.UTF_8 );
        return ClientHome.getActiveClientsList( ).stream( )
                .filter( client -> client.getCode( ) != null && MessageDigest.isEqual( presentedHash, client.getCode( ).getBytes( StandardCharsets.UTF_8 ) ) )
                .findFirst( ).orElseThrow( ( ) -> new PlatformSecurityException( ERROR_CLIENT_NOT_FOUND, SecurityErrorType.UNAUTHORIZED ) );
    }
}
