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
package fr.paris.lutece.plugins.platform.business.subscription;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test the CRUD cycle of the SubscriptionHome facade on HSQL.
 */
public class SubscriptionHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a Subscription: create, findByPrimaryKey, update, remove. A parent Client is created first to satisfy the
     * fk_subscription_client foreign key and removed at the end.
     */
    @Test
    public void testBusinessSubscription( )
    {
        Client client = new Client( );
        client.setName( "Subscription Client" );
        client.setCode( "subscription-client-test" );
        client.setDescription( "Parent client for subscription test" );
        client.setActive( true );
        ClientHome.create( client );

        Subscription subscription = new Subscription( );
        subscription.setClientId( client.getId( ) );
        subscription.setResourceType( "bot" );
        subscription.setResourceId( "resource-1" );
        subscription.setStatus( SubscriptionStatus.ACTIVE );

        SubscriptionHome.create( subscription );

        Optional<Subscription> optStored = SubscriptionHome.findByPrimaryKey( subscription.getId( ) );
        assertTrue( optStored.isPresent( ) );
        Subscription subscriptionStored = optStored.get( );
        assertEquals( subscriptionStored.getClientId( ), subscription.getClientId( ) );
        assertEquals( subscriptionStored.getResourceType( ), subscription.getResourceType( ) );
        assertEquals( subscriptionStored.getResourceId( ), subscription.getResourceId( ) );
        assertEquals( subscriptionStored.getStatus( ), subscription.getStatus( ) );

        subscription.setResourceType( "pipeline" );
        subscription.setResourceId( "resource-2" );
        subscription.setStatus( SubscriptionStatus.SUSPENDED );
        SubscriptionHome.update( subscription );

        subscriptionStored = SubscriptionHome.findByPrimaryKey( subscription.getId( ) ).get( );
        assertEquals( subscriptionStored.getResourceType( ), subscription.getResourceType( ) );
        assertEquals( subscriptionStored.getResourceId( ), subscription.getResourceId( ) );
        assertEquals( subscriptionStored.getStatus( ), subscription.getStatus( ) );

        SubscriptionHome.remove( subscription.getId( ) );
        optStored = SubscriptionHome.findByPrimaryKey( subscription.getId( ) );
        assertFalse( optStored.isPresent( ) );

        SubscriptionHome.getSubscriptionsList( );
    }
}
