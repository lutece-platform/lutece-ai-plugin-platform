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
package fr.paris.lutece.plugins.platform.business.client;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test the CRUD cycle of the ClientHome facade on HSQL.
 */
public class ClientHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a Client: create, findByPrimaryKey, update, remove.
     */
    @Test
    public void testBusinessClient( )
    {
        Client client = new Client( );
        client.setName( "Client Name" );
        client.setCode( "client-code-test" );
        client.setDescription( "Description 1" );
        client.setActive( true );

        ClientHome.create( client );

        Optional<Client> optStored = ClientHome.findByPrimaryKey( client.getId( ) );
        assertTrue( optStored.isPresent( ) );
        Client clientStored = optStored.get( );
        assertEquals( clientStored.getName( ), client.getName( ) );
        assertEquals( clientStored.getCode( ), client.getCode( ) );
        assertEquals( clientStored.getDescription( ), client.getDescription( ) );
        assertEquals( clientStored.isActive( ), client.isActive( ) );

        client.setDescription( "Description 2" );
        client.setActive( false );
        ClientHome.update( client );

        clientStored = ClientHome.findByPrimaryKey( client.getId( ) ).get( );
        assertEquals( clientStored.getDescription( ), client.getDescription( ) );
        assertEquals( clientStored.isActive( ), client.isActive( ) );

        ClientHome.remove( client.getId( ) );
        optStored = ClientHome.findByPrimaryKey( client.getId( ) );
        assertFalse( optStored.isPresent( ) );

        ClientHome.getClientsList( );
        ClientHome.getActiveClientsList( );
    }
}
