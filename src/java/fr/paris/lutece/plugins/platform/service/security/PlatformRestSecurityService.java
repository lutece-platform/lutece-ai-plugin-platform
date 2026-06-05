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

import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.client.Client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service class providing security validation for the platform REST API
 */
@ApplicationScoped
@Named( "platform.platformRestSecurityService" )
public class PlatformRestSecurityService
{
    private static final String HEADER_CLIENT_CODE = "X-Client-Code";

    @Inject
    private PlatformSecurityService securityService;

    /**
     * Validates a client request with resource access control
     *
     * @param request
     *            The HTTP request to validate
     * @param resourceType
     *            The type of resource being accessed
     * @param resourceId
     *            The identifier of the resource being accessed
     * @return The validated client if successful
     */
    public Client validateRequest( HttpServletRequest request, String resourceType, String resourceId )
    {
        String clientCode = request.getHeader( HEADER_CLIENT_CODE );
        return securityService.validateClient( clientCode, resourceType, resourceId );
    }

    /**
     * Validates a client request without specific resource access control
     *
     * @param request
     *            The HTTP request to validate
     * @return The validated client if successful
     */
    public Client validateRequest( HttpServletRequest request )
    {
        String clientCode = request.getHeader( HEADER_CLIENT_CODE );
        return securityService.validateClient( clientCode );
    }

}
