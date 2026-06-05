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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.rs.mapper.PlatformSecurityExceptionMapper;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.security.SecurityErrorType;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.core.Response;

/**
 * Behavioural tests for {@link PlatformRestSecurityService}, the REST boundary that pulls the client code from the {@code X-Client-Code} header and maps
 * security error types to HTTP statuses. These verify the header-name contract, delegation to client validation and the error-to-status mapping.
 */
public class PlatformRestSecurityServiceTest extends AbstractPlatformDbTest
{
    private static final String HEADER_CLIENT_CODE = "X-Client-Code";

    /**
     * Resolves the CDI-managed REST security service.
     *
     * @return the REST security service instance
     */
    private PlatformRestSecurityService service( )
    {
        return CDI.current( ).select( PlatformRestSecurityService.class ).get( );
    }

    /**
     * A request carrying the X-Client-Code header for an active client validates and returns that client.
     */
    @Test
    public void testValidateRequestReadsClientCodeHeader( )
    {
        Client client = new Client( );
        client.setName( "Rest Client" );
        client.setCode( CDI.current( ).select( ClientService.class ).get( ).hashApiKey( "rest-sec-ok" ) );
        client.setActive( true );
        ClientHome.create( client );

        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addHeader( HEADER_CLIENT_CODE, "rest-sec-ok" );

        Client validated = service( ).validateRequest( request );

        assertEquals( client.getId( ), validated.getId( ) );
    }

    /**
     * A request with no X-Client-Code header is rejected as unauthorized.
     */
    @Test
    public void testValidateRequestWithoutHeaderIsUnauthorized( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        PlatformRestSecurityService service = service( );

        PlatformSecurityException ex = assertThrows( PlatformSecurityException.class, ( ) -> service.validateRequest( request ) );
        assertEquals( SecurityErrorType.UNAUTHORIZED, ex.getErrorType( ) );
    }

    /**
     * Security error types map to their HTTP statuses, and a null error type maps to null.
     */
    @Test
    public void testConvertErrorTypeToHttpStatus( )
    {
        assertEquals( Response.Status.UNAUTHORIZED,
                PlatformSecurityExceptionMapper.resolveStatus( new PlatformSecurityException( "x", SecurityErrorType.UNAUTHORIZED ) ) );
        assertEquals( Response.Status.FORBIDDEN,
                PlatformSecurityExceptionMapper.resolveStatus( new PlatformSecurityException( "x", SecurityErrorType.FORBIDDEN ) ) );
        assertEquals( Response.Status.FORBIDDEN, PlatformSecurityExceptionMapper.resolveStatus( new PlatformSecurityException( "x", null ) ) );
    }
}
