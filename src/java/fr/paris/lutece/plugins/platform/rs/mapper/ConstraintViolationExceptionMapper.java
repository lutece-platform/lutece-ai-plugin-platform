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

import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * Maps a Jakarta Bean Validation {@link ConstraintViolationException} (raised when a JAX-RS resource parameter annotated with {@code @Valid} or a constraint
 * fails validation) to a {@code 400 Bad Request} JSON response, aggregating every violated field into a single human-readable message.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException>
{
    /**
     * Builds a {@code 400 Bad Request} JSON response listing all the constraint violations.
     *
     * @param exception
     *            the validation exception carrying the set of violations
     * @return a JSON response with the {@code 400} status and the aggregated violation messages
     */
    @Override
    public Response toResponse( ConstraintViolationException exception )
    {
        String message = exception.getConstraintViolations( ).stream( ).map( ConstraintViolationExceptionMapper::formatViolation )
                .collect( Collectors.joining( "; " ) );

        return Response.status( Response.Status.BAD_REQUEST ).type( MediaType.APPLICATION_JSON )
                .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "VALIDATION_ERROR", message ) ) ).build( );
    }

    /**
     * Formats a single violation as {@code field: message}, keeping only the leaf property name.
     *
     * @param violation
     *            the constraint violation to format
     * @return the formatted violation string
     */
    private static String formatViolation( ConstraintViolation<?> violation )
    {
        String path = violation.getPropertyPath( ).toString( );
        int idx = path.lastIndexOf( '.' );
        String field = idx >= 0 ? path.substring( idx + 1 ) : path;

        return field + ": " + violation.getMessage( );
    }
}
