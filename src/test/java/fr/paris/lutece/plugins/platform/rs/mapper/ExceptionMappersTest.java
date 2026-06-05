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

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.security.SecurityErrorType;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.exception.PayloadTooLargeException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.security.PlatformSecurityException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Behavioural tests for the REST exception mappers — the single conversion point between business/security exceptions and HTTP responses. These pin the
 * status-code contract of each mapper (the part of the REST surface that the SE test harness can verify without a JAX-RS runtime).
 */
public class ExceptionMappersTest extends AbstractPlatformDbTest
{
    /**
     * Each AgentServiceException subtype maps to its documented HTTP status, and the response carries the exception message as JSON.
     */
    @Test
    public void testAgentServiceExceptionStatusContract( )
    {
        AgentServiceExceptionMapper mapper = new AgentServiceExceptionMapper( );

        assertEquals( 400, mapper.toResponse( new InvalidRequestException( "bad input" ) ).getStatus( ) );
        assertEquals( 403, mapper.toResponse( new AccessDeniedException( "denied" ) ).getStatus( ) );
        assertEquals( 413, mapper.toResponse( new PayloadTooLargeException( "too large" ) ).getStatus( ) );
        assertEquals( 404, mapper.toResponse( new ResourceNotFoundException( ) ).getStatus( ) );
        assertEquals( 500, mapper.toResponse( new AgentServiceException( "boom" ) ).getStatus( ) );

        Response response = mapper.toResponse( new InvalidRequestException( "bad input" ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getMediaType( ) );
        assertTrue( response.getEntity( ).toString( ).contains( "bad input" ) );
    }

    /**
     * A security exception maps to the status carried by its error type, defaulting to 403 when no type is set, with a JSON body.
     */
    @Test
    public void testSecurityExceptionStatusContract( )
    {
        PlatformSecurityExceptionMapper mapper = new PlatformSecurityExceptionMapper( );

        assertEquals( 401, mapper.toResponse( new PlatformSecurityException( "no key", SecurityErrorType.UNAUTHORIZED ) ).getStatus( ) );
        assertEquals( 403, mapper.toResponse( new PlatformSecurityException( "no sub", SecurityErrorType.FORBIDDEN ) ).getStatus( ) );
        assertEquals( 403, mapper.toResponse( new PlatformSecurityException( "untyped", null ) ).getStatus( ) );

        Response response = mapper.toResponse( new PlatformSecurityException( "no key", SecurityErrorType.UNAUTHORIZED ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getMediaType( ) );
        assertTrue( response.getEntity( ).toString( ).contains( "no key" ) );
    }

    /**
     * A bean-validation failure maps to 400 with a JSON body; violations built by the runtime validator are aggregated as {@code field: message}.
     */
    @Test
    public void testConstraintViolationMapsTo400( )
    {
        ConstraintViolationExceptionMapper mapper = new ConstraintViolationExceptionMapper( );

        Response empty = mapper.toResponse( new ConstraintViolationException( Set.of( ) ) );
        assertEquals( 400, empty.getStatus( ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, empty.getMediaType( ) );
    }

    /**
     * An executor rejection (bulkhead back-pressure) maps to 503 with a Retry-After hint, never to a 500.
     */
    @Test
    public void testRejectedExecutionMapsTo503WithRetryAfter( )
    {
        RejectedExecutionExceptionMapper mapper = new RejectedExecutionExceptionMapper( );

        Response response = mapper.toResponse( new RejectedExecutionException( "queue full" ) );

        assertEquals( 503, response.getStatus( ) );
        assertEquals( "5", String.valueOf( response.getHeaders( ).getFirst( HttpHeaders.RETRY_AFTER ) ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getMediaType( ) );
    }

    /**
     * The catch-all mapper lets WebApplicationException responses pass through untouched and converts any other throwable to an opaque 500 that never leaks the
     * internal exception message.
     */
    @Test
    public void testUncaughtMapperPassthroughAndOpaque500( )
    {
        UncaughtExceptionMapper mapper = new UncaughtExceptionMapper( );

        Response notFound = mapper.toResponse( new WebApplicationException( Response.status( Response.Status.NOT_FOUND ).build( ) ) );
        assertEquals( 404, notFound.getStatus( ) );

        Response internal = mapper.toResponse( new IllegalStateException( "secret internal detail" ) );
        assertEquals( 500, internal.getStatus( ) );
        assertEquals( MediaType.APPLICATION_JSON_TYPE, internal.getMediaType( ) );
        assertFalse( internal.getEntity( ).toString( ).contains( "secret internal detail" ) );
    }
}
