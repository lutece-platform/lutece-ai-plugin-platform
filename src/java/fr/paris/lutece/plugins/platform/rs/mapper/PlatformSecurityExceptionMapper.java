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

import fr.paris.lutece.plugins.platform.service.security.PlatformSecurityException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a {@link PlatformSecurityException} to a JSON response whose status comes from the exception's {@code SecurityErrorType} (401 for authentication
 * failures, 403 for authorization failures). This is the single place converting security failures into HTTP responses for the REST layer.
 */
@Provider
public class PlatformSecurityExceptionMapper implements ExceptionMapper<PlatformSecurityException>
{
    /**
     * Builds the JSON response for a security violation.
     *
     * @param exception
     *            the security exception
     * @return a JSON response carrying the error type's HTTP status and the controlled error message
     */
    @Override
    public Response toResponse( PlatformSecurityException exception )
    {
        AppLogService.error( "Security error: {}", exception.getMessage( ), exception );

        Response.Status status = resolveStatus( exception );
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), exception.getMessage( ) ) ) )
                .type( MediaType.APPLICATION_JSON ).build( );
    }

    /**
     * Resolves the HTTP status of a security exception from its error type, defaulting to 403 when no type is set.
     *
     * @param exception
     *            the security exception
     * @return the HTTP status to return
     */
    public static Response.Status resolveStatus( PlatformSecurityException exception )
    {
        return exception.getErrorType( ) != null ? Response.Status.fromStatusCode( exception.getErrorType( ).getStatusCode( ) ) : Response.Status.FORBIDDEN;
    }
}
