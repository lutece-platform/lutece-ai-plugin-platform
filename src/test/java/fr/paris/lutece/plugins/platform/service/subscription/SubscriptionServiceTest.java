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
package fr.paris.lutece.plugins.platform.service.subscription;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionStatus;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.subscription.SubscriptionHome;
import fr.paris.lutece.plugins.platform.service.subscription.dto.SubscriptionListView;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link SubscriptionService}. These exercise the subscription business logic against HSQL — status-aware active check, status
 * transitions via the permission-free overload, query facades — and the RBAC security contract that gates every user-facing mutation. An {@link AdminUser} with
 * no roles is used as the canonical unauthorized caller, since RBAC denies a user holding none of the required roles.
 */
public class SubscriptionServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed subscription service.
     *
     * @return the subscription service instance
     */
    private SubscriptionService service( )
    {
        return CDI.current( ).select( SubscriptionService.class ).get( );
    }

    /**
     * Creates and persists an active parent client with the given code.
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
     * Creates and persists a subscription with the given status.
     *
     * @param clientId
     *            the parent client id
     * @param resourceType
     *            the resource type
     * @param resourceId
     *            the resource id
     * @param status
     *            the subscription status
     * @return the persisted subscription
     */
    private Subscription createSubscription( int clientId, String resourceType, String resourceId, SubscriptionStatus status )
    {
        Subscription subscription = new Subscription( );
        subscription.setClientId( clientId );
        subscription.setResourceType( resourceType );
        subscription.setResourceId( resourceId );
        subscription.setStatus( status );
        return SubscriptionHome.create( subscription );
    }

    /**
     * An active subscription reports as active; once cancelled through the permission-free overload, the same resource no longer reports active.
     */
    @Test
    public void testHasActiveSubscriptionReflectsStatus( ) throws AccessDeniedException
    {
        Client client = createClient( "sub-svc-active" );
        assertFalse( service( ).hasActiveSubscription( client.getId( ), "bot", "r1" ) );

        Subscription sub = createSubscription( client.getId( ), "bot", "r1", SubscriptionStatus.ACTIVE );
        assertTrue( service( ).hasActiveSubscription( client.getId( ), "bot", "r1" ) );

        service( ).unsubscribe( sub.getId( ) );
        assertFalse( service( ).hasActiveSubscription( client.getId( ), "bot", "r1" ) );
    }

    /**
     * A suspended subscription is not considered active.
     */
    @Test
    public void testSuspendedSubscriptionIsNotActive( )
    {
        Client client = createClient( "sub-svc-susp" );
        createSubscription( client.getId( ), "pipeline", "r2", SubscriptionStatus.SUSPENDED );

        assertFalse( service( ).hasActiveSubscription( client.getId( ), "pipeline", "r2" ) );
    }

    /**
     * The permission-free unsubscribe flips an existing subscription to CANCELLED, and is a no-op when the subscription does not exist.
     */
    @Test
    public void testUnsubscribeWithoutPermission( ) throws AccessDeniedException
    {
        Client client = createClient( "sub-svc-cancel" );
        Subscription sub = createSubscription( client.getId( ), "bot", "r3", SubscriptionStatus.ACTIVE );

        service( ).unsubscribe( sub.getId( ) );
        assertEquals( SubscriptionStatus.CANCELLED, SubscriptionHome.findByPrimaryKey( sub.getId( ) ).get( ).getStatus( ) );

        assertDoesNotThrow( ( ) -> service( ).unsubscribe( 999999 ) );
    }

    /**
     * The query facades return the subscriptions of a client and of a resource.
     */
    @Test
    public void testQueryFacades( )
    {
        Client client = createClient( "sub-svc-query" );
        createSubscription( client.getId( ), "bot", "q1", SubscriptionStatus.ACTIVE );
        createSubscription( client.getId( ), "pipeline", "q2", SubscriptionStatus.ACTIVE );

        assertEquals( 2, service( ).getSubscriptionsByClientId( client.getId( ) ).size( ) );
        List<Subscription> byResource = service( ).getResourceSubscriptions( "bot", "q1" );
        assertEquals( 1, byResource.size( ) );
        assertEquals( client.getId( ), byResource.get( 0 ).getClientId( ) );
    }

    /**
     * Every user-facing mutation is gated by RBAC: a caller with no roles is denied on subscribe, suspend, unsubscribe and delete.
     */
    @Test
    public void testRbacGateDeniesUnauthorizedUser( )
    {
        Client client = createClient( "sub-svc-rbac" );
        Subscription sub = createSubscription( client.getId( ), "bot", "r4", SubscriptionStatus.ACTIVE );
        AdminUser user = new AdminUser( );
        SubscriptionService service = service( );

        assertThrows( AccessDeniedException.class, ( ) -> service.subscribe( client.getId( ), "bot", "r5", user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.suspend( sub.getId( ), user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.unsubscribe( sub.getId( ), user ) );
        assertThrows( AccessDeniedException.class, ( ) -> service.deleteSubscription( sub.getId( ), user ) );

        assertEquals( SubscriptionStatus.ACTIVE, SubscriptionHome.findByPrimaryKey( sub.getId( ) ).get( ).getStatus( ) );
    }

    /**
     * Mutating an absent subscription must be reported to the caller (no silent pretend-success) : suspend and deleteSubscription return false, unsubscribe
     * returns its -1 sentinel.
     *
     * @throws Exception
     *             never, the ids do not exist
     */
    @Test
    public void testMutationsOnAbsentSubscriptionAreReported( ) throws Exception
    {
        AdminUser user = new AdminUser( );
        SubscriptionService service = service( );

        assertFalse( service.suspend( 999999, user ), "suspending an absent subscription must report false" );
        assertFalse( service.deleteSubscription( 999999, user ), "deleting an absent subscription must report false" );
        assertEquals( -1, service.unsubscribe( 999999, user ), "unsubscribing an absent subscription must report its sentinel" );
    }

    /**
     * A blank client filter returns the full list with no resolved client. With an empty parameter, no parse and no RBAC lookup happen, so the view carries a
     * null client and the global subscription list including a foreign client's subscription.
     */
    @Test
    public void testResolveSubscriptionsBlankClientReturnsFullList( )
    {
        Client client = createClient( "sub-svc-resolve-blank" );
        Subscription sub = createSubscription( client.getId( ), "bot", "rb1", SubscriptionStatus.ACTIVE );
        AdminUser user = new AdminUser( );

        SubscriptionListView nullView = service( ).resolveSubscriptions( null, user );
        assertNull( nullView.getClient( ), "Null client filter yields no resolved client" );
        assertTrue( nullView.getSubscriptions( ).stream( ).anyMatch( s -> s.getId( ) == sub.getId( ) ), "Null client filter returns the global list" );

        SubscriptionListView blankView = service( ).resolveSubscriptions( "", user );
        assertNull( blankView.getClient( ), "Empty client filter yields no resolved client" );
        assertTrue( blankView.getSubscriptions( ).stream( ).anyMatch( s -> s.getId( ) == sub.getId( ) ), "Empty client filter returns the global list" );
    }

    /**
     * A non-numeric client filter triggers a NumberFormatException internally and falls back to the full subscription list with a null resolved client.
     */
    @Test
    public void testResolveSubscriptionsInvalidClientFallsBackToFullList( )
    {
        Client client = createClient( "sub-svc-resolve-invalid" );
        Subscription sub = createSubscription( client.getId( ), "bot", "ri1", SubscriptionStatus.ACTIVE );
        AdminUser user = new AdminUser( );

        SubscriptionListView view = service( ).resolveSubscriptions( "not-a-number", user );

        assertNull( view.getClient( ), "Invalid client filter yields no resolved client" );
        assertTrue( view.getSubscriptions( ).stream( ).anyMatch( s -> s.getId( ) == sub.getId( ) ), "Invalid client filter returns the global list" );
    }

    /**
     * A numeric but unauthorized client filter is denied by RBAC during the client lookup; the AccessDeniedException is caught and the service falls back to
     * the full subscription list with a null resolved client. The caller is an AdminUser holding no roles.
     */
    @Test
    public void testResolveSubscriptionsUnauthorizedClientFallsBackToFullList( )
    {
        Client client = createClient( "sub-svc-resolve-denied" );
        Subscription sub = createSubscription( client.getId( ), "bot", "rd1", SubscriptionStatus.ACTIVE );
        AdminUser user = new AdminUser( );

        SubscriptionListView view = service( ).resolveSubscriptions( String.valueOf( client.getId( ) ), user );

        assertNull( view.getClient( ), "Unauthorized client filter yields no resolved client" );
        assertTrue( view.getSubscriptions( ).stream( ).anyMatch( s -> s.getId( ) == sub.getId( ) ), "Unauthorized client filter returns the global list" );
    }
}
