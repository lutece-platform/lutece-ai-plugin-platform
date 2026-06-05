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

import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.security.SecurityErrorType;
import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link PlatformSecurityService#validateClient}, the REST entry-point gatekeeper. These trace the full validation flow against HSQL:
 * missing code, unknown code, inactive client, missing subscription and the authorized path — asserting both the outcome and the HTTP-mapped error type.
 */
public class PlatformSecurityServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed security service.
     *
     * @return the security service instance
     */
    private PlatformSecurityService service( )
    {
        return CDI.current( ).select( PlatformSecurityService.class ).get( );
    }

    /**
     * Creates and persists a client whose stored code is the SHA-256 hash of the given cleartext API key (hash-at-rest contract: validation is performed with
     * the cleartext key).
     *
     * @param code
     *            the cleartext API key
     * @param active
     *            whether the client is active
     * @return the persisted client
     */
    private Client createClient( String code, boolean active )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( CDI.current( ).select( ClientService.class ).get( ).hashApiKey( code ) );
        client.setActive( active );
        ClientHome.create( client );
        return client;
    }

    /**
     * A null or blank client code is rejected as unauthorized.
     */
    @Test
    public void testMissingClientCodeIsUnauthorized( )
    {
        PlatformSecurityService service = service( );

        PlatformSecurityException nullEx = assertThrows( PlatformSecurityException.class, ( ) -> service.validateClient( null ) );
        assertEquals( SecurityErrorType.UNAUTHORIZED, nullEx.getErrorType( ) );

        PlatformSecurityException blankEx = assertThrows( PlatformSecurityException.class, ( ) -> service.validateClient( "   " ) );
        assertEquals( SecurityErrorType.UNAUTHORIZED, blankEx.getErrorType( ) );
    }

    /**
     * An unknown client code is rejected as unauthorized.
     */
    @Test
    public void testUnknownClientIsUnauthorized( )
    {
        PlatformSecurityService service = service( );

        PlatformSecurityException ex = assertThrows( PlatformSecurityException.class, ( ) -> service.validateClient( "does-not-exist-code" ) );
        assertEquals( SecurityErrorType.UNAUTHORIZED, ex.getErrorType( ) );
    }

    /**
     * An inactive client is invisible to validation and reported as unauthorized.
     */
    @Test
    public void testInactiveClientIsUnauthorized( )
    {
        createClient( "sec-inactive", false );
        PlatformSecurityService service = service( );

        PlatformSecurityException ex = assertThrows( PlatformSecurityException.class, ( ) -> service.validateClient( "sec-inactive" ) );
        assertEquals( SecurityErrorType.UNAUTHORIZED, ex.getErrorType( ) );
    }

    /**
     * An active client without the required subscription is rejected as forbidden when access to a resource is checked.
     */
    @Test
    public void testActiveClientWithoutSubscriptionIsForbidden( )
    {
        createClient( "sec-no-sub", true );
        PlatformSecurityService service = service( );

        PlatformSecurityException ex = assertThrows( PlatformSecurityException.class, ( ) -> service.validateClient( "sec-no-sub", "bot", "r1" ) );
        assertEquals( SecurityErrorType.FORBIDDEN, ex.getErrorType( ) );
    }

    /**
     * An active client with an active subscription passes resource-scoped validation and the client is returned.
     */
    @Test
    public void testActiveClientWithSubscriptionIsAuthorized( )
    {
        Client client = createClient( "sec-ok", true );
        Subscription sub = new Subscription( );
        sub.setClientId( client.getId( ) );
        sub.setResourceType( "bot" );
        sub.setResourceId( "r1" );
        sub.setStatus( SubscriptionStatus.ACTIVE );
        SubscriptionHome.create( sub );

        Client validated = service( ).validateClient( "sec-ok", "bot", "r1" );

        assertEquals( client.getId( ), validated.getId( ) );
        assertEquals( client.getCode( ), validated.getCode( ) );
    }

    /**
     * The code-only validation returns an active client without requiring any subscription.
     */
    @Test
    public void testValidateClientWithoutResourceCheck( )
    {
        Client client = createClient( "sec-code-only", true );

        Client validated = service( ).validateClient( "sec-code-only" );

        assertEquals( client.getId( ), validated.getId( ) );
    }
}
