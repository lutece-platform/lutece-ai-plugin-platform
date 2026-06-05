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

import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Last-resort mapper: turns any uncaught exception into a JSON 500 (and logs it), so REST endpoints never need their own catch-all try/catch. JAX-RS exceptions
 * keep their own response (404, 405...).
 */
@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Throwable>
{
    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";

    /**
     * Maps an uncaught exception to a JSON response.
     *
     * @param exception
     *            the uncaught exception
     * @return the HTTP response
     */
    @Override
    public Response toResponse( Throwable exception )
    {
        if ( exception instanceof WebApplicationException webApplicationException )
        {
            return webApplicationException.getResponse( );
        }
        AppLogService.error( "Uncaught exception on REST endpoint", exception );
        return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "INTERNAL_ERROR", INTERNAL_ERROR_MESSAGE ) ) ).type( MediaType.APPLICATION_JSON )
                .build( );
    }
}
