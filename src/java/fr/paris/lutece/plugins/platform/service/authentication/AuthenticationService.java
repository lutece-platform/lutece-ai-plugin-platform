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
package fr.paris.lutece.plugins.platform.service.authentication;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.service.security.PlatformRestSecurityService;
import fr.paris.lutece.plugins.platform.service.security.PlatformSecurityException;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for handling client authentication operations. Provides methods for client authentication with exception handling and safe authentication methods
 * that return result objects instead of throwing exceptions.
 */
@ApplicationScoped
public class AuthenticationService
{
    @Inject
    private PlatformRestSecurityService securityService;

    /**
     * Default constructor for CDI.
     */
    AuthenticationService( )
    {
    }

    /**
     * Authenticates a client from the HTTP request.
     *
     * @param request
     *            the HTTP servlet request
     * @return the authenticated client
     * @throws PlatformSecurityException
     *             if authentication fails
     */
    public Client authenticateClient( HttpServletRequest request ) throws PlatformSecurityException
    {
        return securityService.validateRequest( request );
    }

    /**
     * Authenticates a client from the HTTP request with specific resource validation.
     *
     * @param request
     *            the HTTP servlet request
     * @param resourceType
     *            the type of resource to validate access for
     * @param resourceId
     *            the ID of the resource to validate access for
     * @return the authenticated client
     * @throws PlatformSecurityException
     *             if authentication fails
     */
    public Client authenticateClient( HttpServletRequest request, String resourceType, String resourceId ) throws PlatformSecurityException
    {
        if ( resourceType != null && resourceId != null )
        {
            return securityService.validateRequest( request, resourceType, resourceId );
        }
        else
        {
            return securityService.validateRequest( request );
        }
    }

}
