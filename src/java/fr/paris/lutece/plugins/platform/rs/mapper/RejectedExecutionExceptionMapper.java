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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.concurrent.RejectedExecutionException;

/**
 * Maps a {@link RejectedExecutionException} to a 503 response with a {@code Retry-After} header.
 *
 * <p>
 * The bounded {@code @BlockingIO} executor (the bulkhead protecting the database) rejects tasks once its queue is full. That rejection is the intended
 * back-pressure under overload; this mapper turns it into the semantically correct {@code 503 Service Unavailable} so clients can retry, instead of a
 * misleading {@code 500}.
 * </p>
 */
@Provider
public class RejectedExecutionExceptionMapper implements ExceptionMapper<RejectedExecutionException>
{
    private static final int RETRY_AFTER_SECONDS = 5;

    private static final String OVERLOADED_MESSAGE = "Service temporarily unavailable due to high load. Please try again in a few moments.";

    /**
     * Builds the 503 response carrying a {@code Retry-After} hint for the rejected request.
     *
     * @param exception
     *            the rejection raised by the bounded executor
     * @return a JSON 503 response with a {@code Retry-After} header
     */
    @Override
    public Response toResponse( RejectedExecutionException exception )
    {
        AppLogService.error( "Request rejected by bounded executor (overload): {}", exception.getMessage( ) );

        return Response.status( Response.Status.SERVICE_UNAVAILABLE ).header( HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "SERVICE_UNAVAILABLE", OVERLOADED_MESSAGE ) ) ).type( MediaType.APPLICATION_JSON )
                .build( );
    }
}
