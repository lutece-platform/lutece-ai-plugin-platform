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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.security.PlatformSecurityException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.plugins.platform.service.security.ClientService;

/**
 * Behavioural tests for {@link AuthenticationService}: the throwing entry point on both the success and failure paths. The wrapper's failure branch builds a
 * JAX-RS {@code Response}, which requires a {@code RuntimeDelegate} provider only present on the OpenLiberty runtime — that branch is therefore covered by the
 * REST smoke tests, not by this Java SE suite.
 */
public class AuthenticationServiceTest extends AbstractPlatformDbTest
{
    private static final String HEADER_CLIENT_CODE = "X-Client-Code";

    /**
     * Resolves the CDI-managed authentication service.
     *
     * @return the authentication service instance
     */
    private AuthenticationService service( )
    {
        return CDI.current( ).select( AuthenticationService.class ).get( );
    }

    /**
     * Creates an active client subscribed to the given resource and returns it.
     *
     * @param code
     *            the unique client code
     * @param resourceType
     *            the subscribed resource type
     * @param resourceId
     *            the subscribed resource id
     * @return the persisted client
     */
    private Client createSubscribedClient( String code, String resourceType, String resourceId )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( CDI.current( ).select( ClientService.class ).get( ).hashApiKey( code ) );
        client.setActive( true );
        ClientHome.create( client );

        Subscription sub = new Subscription( );
        sub.setClientId( client.getId( ) );
        sub.setResourceType( resourceType );
        sub.setResourceId( resourceId );
        sub.setStatus( SubscriptionStatus.ACTIVE );
        SubscriptionHome.create( sub );
        return client;
    }

    /**
     * Builds a request carrying the given client code in the X-Client-Code header.
     *
     * @param clientCode
     *            the client code, or null for none
     * @return the mock request
     */
    private MockHttpServletRequest requestWithCode( String clientCode )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        if ( clientCode != null )
        {
            request.addHeader( HEADER_CLIENT_CODE, clientCode );
        }
        return request;
    }

    /**
     * The throwing entry point returns the authenticated client directly on the success path.
     */
    @Test
    public void testAuthenticateClientSuccess( )
    {
        Client client = createSubscribedClient( "auth-ok", "bot", "r1" );

        Client authenticated = service( ).authenticateClient( requestWithCode( "auth-ok" ), "bot", "r1" );

        assertEquals( client.getId( ), authenticated.getId( ) );
    }

    /**
     * The throwing entry point propagates a security exception when no client code is supplied.
     */
    @Test
    public void testAuthenticateClientThrowsWithoutCode( )
    {
        AuthenticationService service = service( );
        MockHttpServletRequest request = requestWithCode( null );

        assertThrows( PlatformSecurityException.class, ( ) -> service.authenticateClient( request ) );
    }
}
