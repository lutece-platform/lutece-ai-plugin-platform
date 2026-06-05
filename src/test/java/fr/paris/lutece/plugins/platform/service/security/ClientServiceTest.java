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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link ClientService}, the RBAC-integrated client facade used by the admin UI. These verify that every mutating or reading operation is
 * denied for a caller with no roles, that the permission map and per-client permission flags collapse to false for such a caller, and that cost enrichment
 * defaults to zero when there is no observability data. A roleless {@link AdminUser} is the canonical unauthorized caller.
 */
public class ClientServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed client service.
     *
     * @return the client service instance
     */
    private ClientService service( )
    {
        return CDI.current( ).select( ClientService.class ).get( );
    }

    /**
     * Creates and persists an active client with the given code.
     *
     * @param code
     *            the unique client code
     * @return the persisted client
     */
    private Client createClient( String code )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( code );
        client.setActive( true );
        ClientHome.create( client );
        return client;
    }

    /**
     * Read, create, update, delete and observability checks are all denied for a caller holding none of the required roles.
     */
    @Test
    public void testRbacGateDeniesEveryOperation( )
    {
        Client client = createClient( "client-svc-rbac" );
        AdminUser user = new AdminUser( );
        ClientService service = service( );

        assertThrows( AccessDeniedException.class, ( ) -> service.getClient( client.getId( ), user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.createClient( new Client( ), user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.updateClient( client, user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.deleteClient( client.getId( ), user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.checkObservabilityPermission( client.getId( ), user ) );

        assertTrue( ClientHome.findByPrimaryKey( client.getId( ) ).isPresent( ) );
    }

    /**
     * A roleless caller sees no authorized clients.
     */
    @Test
    public void testAuthorizedClientsEmptyForRolelessUser( )
    {
        createClient( "client-svc-auth" );
        AdminUser user = new AdminUser( );

        assertTrue( service( ).getAuthorizedClients( user ).isEmpty( ) );
        assertEquals( List.of( ), service( ).getAuthorizedClientIds( user ) );
    }

    /**
     * The aggregated permission map reports every capability as false for a roleless caller, even when clients exist.
     */
    @Test
    public void testPermissionMapAllFalse( )
    {
        createClient( "client-svc-perms" );
        AdminUser user = new AdminUser( );

        Map<String, Boolean> permissions = service( ).getPermissions( user );

        assertFalse( permissions.get( "canCreate" ) );
        assertFalse( permissions.get( "canView" ) );
        assertFalse( permissions.get( "canModify" ) );
        assertFalse( permissions.get( "canDelete" ) );
    }

    /**
     * Per-client permission enrichment leaves all flags false for a roleless caller.
     */
    @Test
    public void testEnrichWithPermissionsSetsAllFalse( )
    {
        Client client = createClient( "client-svc-enrich" );
        AdminUser user = new AdminUser( );

        service( ).enrichClientWithPermissions( client, user );

        assertFalse( client.isUserCanView( ) );
        assertFalse( client.isUserCanModify( ) );
        assertFalse( client.isUserCanDelete( ) );
        assertFalse( client.isUserCanViewObservability( ) );
    }

    /**
     * Cost enrichment defaults to zero for a client that has no recorded executions.
     */
    @Test
    public void testEnrichWithTotalCostDefaultsToZero( )
    {
        Client client = createClient( "client-svc-cost" );

        service( ).enrichClientsWithTotalCost( List.of( client ) );

        assertEquals( 0.0, client.getTotalCost( ) );
    }

    /**
     * Generated API keys carry the expected prefix, are unique across calls, and their SHA-256 hash is deterministic, hex-encoded and distinct from the
     * cleartext (hash-at-rest contract).
     */
    @Test
    public void testGeneratedApiKeyAndHashContract( )
    {
        ClientService service = service( );

        String key = service.generateApiKey( );
        String otherKey = service.generateApiKey( );

        assertTrue( key.startsWith( "pk_" ) );
        assertTrue( key.length( ) >= 40 );
        assertTrue( !key.equals( otherKey ) );

        String hash = service.hashApiKey( key );
        assertEquals( hash, service.hashApiKey( key ) );
        assertEquals( 64, hash.length( ) );
        assertTrue( !hash.equals( key ) );
    }

    /**
     * A client whose stored code is the hash of a generated key is authenticated by the security service with the cleartext key, and rejected once the key is
     * replaced (regeneration semantics).
     */
    @Test
    public void testStoredHashAuthenticatesCleartextKey( )
    {
        ClientService service = service( );
        String key = service.generateApiKey( );

        Client client = new Client( );
        client.setName( "Client key-roundtrip" );
        client.setCode( service.hashApiKey( key ) );
        client.setActive( true );
        ClientHome.create( client );

        PlatformSecurityService securityService = CDI.current( ).select( PlatformSecurityService.class ).get( );
        assertEquals( client.getId( ), securityService.validateClient( key ).getId( ) );

        client.setCode( service.hashApiKey( service.generateApiKey( ) ) );
        ClientHome.update( client );
        assertThrows( PlatformSecurityException.class, ( ) -> securityService.validateClient( key ) );
    }
}
