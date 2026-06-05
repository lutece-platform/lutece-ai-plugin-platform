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
package fr.paris.lutece.plugins.platform.rs.mapper;

import fr.paris.lutece.plugins.platform.service.exception.PayloadTooLargeException;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps any {@link AgentServiceException} (the common base of the plugin's business exceptions) to a JSON HTTP response. The concrete subtype determines the
 * status code: invalid request to 400, access denied to 403, every {@code *NotFound} to 404; anything else falls back to 500. Uncaught technical throwables
 * remain handled by the core {@code UncaughtThrowableMapper}.
 */
@Provider
public class AgentServiceExceptionMapper implements ExceptionMapper<AgentServiceException>
{
    /**
     * Builds the JSON response for the given business exception, logging only genuine server-side faults.
     *
     * @param exception
     *            the business exception raised by a service or resource
     * @return a JSON response carrying the resolved status and the exception message
     */
    @Override
    public Response toResponse( AgentServiceException exception )
    {
        Response.Status status = resolveStatus( exception );

        if ( status == Response.Status.INTERNAL_SERVER_ERROR )
        {
            AppLogService.error( "Unhandled agent service error: {}", exception.getMessage( ), exception );
        }

        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), exception.getMessage( ) ) ) )
                .type( MediaType.APPLICATION_JSON ).build( );
    }

    /**
     * Resolves the HTTP status that matches the concrete exception subtype.
     *
     * @param exception
     *            the business exception
     * @return the matching HTTP status
     */
    private static Response.Status resolveStatus( AgentServiceException exception )
    {
        if ( exception instanceof InvalidRequestException )
        {
            return Response.Status.BAD_REQUEST;
        }
        if ( exception instanceof AccessDeniedException )
        {
            return Response.Status.FORBIDDEN;
        }
        if ( exception instanceof PayloadTooLargeException )
        {
            return Response.Status.REQUEST_ENTITY_TOO_LARGE;
        }
        if ( exception instanceof ResourceNotFoundException )
        {
            return Response.Status.NOT_FOUND;
        }
        return Response.Status.INTERNAL_SERVER_ERROR;
    }
}
