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
package fr.paris.lutece.plugins.platform.business.vision;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for {@link VisionHome} on HSQL.
 *
 * <p>
 * A {@code platform_vision} row carries NOT NULL foreign keys to {@code platform_client} and {@code platform_provider}, so both parents are created up front
 * and torn down at the end.
 * </p>
 */
public class VisionHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a Vision: create, findByPrimaryKey, update, remove. A parent Client and Provider are created first to satisfy the
     * {@code client_id} and {@code provider_id} foreign keys and removed at the end.
     */
    @Test
    public void testBusinessVision( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );

        Vision vision = new Vision( );
        vision.setVisionTitle( "Vision Title 1" );
        vision.setVisionDescription( "Vision Description 1" );
        vision.setClientId( client.getId( ) );
        vision.setProviderId( provider.getId( ) );

        VisionHome.create( vision );

        Optional<Vision> optStored = VisionHome.findByPrimaryKey( vision.getId( ) );
        assertTrue( optStored.isPresent( ) );
        Vision visionStored = optStored.get( );
        assertEquals( visionStored.getVisionTitle( ), vision.getVisionTitle( ) );
        assertEquals( visionStored.getVisionDescription( ), vision.getVisionDescription( ) );
        assertEquals( visionStored.getClientId( ), vision.getClientId( ) );
        assertEquals( visionStored.getProviderId( ), vision.getProviderId( ) );

        vision.setVisionTitle( "Vision Title 2" );
        vision.setVisionDescription( "Vision Description 2" );
        VisionHome.update( vision );

        visionStored = VisionHome.findByPrimaryKey( vision.getId( ) ).get( );
        assertEquals( visionStored.getVisionTitle( ), vision.getVisionTitle( ) );
        assertEquals( visionStored.getVisionDescription( ), vision.getVisionDescription( ) );

        VisionHome.getVisionsList( );
        VisionHome.getVisionsListByClientId( client.getId( ) );
        VisionHome.getVisionsReferenceList( );

        VisionHome.remove( vision.getId( ) );
        optStored = VisionHome.findByPrimaryKey( vision.getId( ) );
        assertFalse( optStored.isPresent( ) );
    }

    /**
     * Creates a minimal client FK parent.
     *
     * @return The persisted client
     */
    private Client createClient( )
    {
        Client client = new Client( );
        client.setName( "Test Client" );
        client.setCode( "test-vision-client" );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates a minimal provider FK parent.
     *
     * @return The persisted provider
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Test Provider" );
        provider.setProviderType( "LLM" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }
}
