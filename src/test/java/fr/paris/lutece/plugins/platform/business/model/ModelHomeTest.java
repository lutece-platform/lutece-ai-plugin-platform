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
package fr.paris.lutece.plugins.platform.business.model;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * CRUD test for the {@link ModelHome} facade. A Model carries two NOT NULL foreign keys (client_id, provider_id), so the test first creates a parent Client and
 * Provider, exercises the full create / findByPrimaryKey / update / remove cycle, then removes the parents.
 */
public class ModelHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the Model CRUD cycle on HSQL. The update step re-points the model to a second provider since client_id and provider_id are the only mutable
     * persisted columns.
     */
    @Test
    public void testBusinessModel( )
    {
        Client client = new Client( );
        client.setName( "ModelHomeTest client" );
        client.setCode( "model-home-test-client" );
        client.setDescription( "Parent client for ModelHomeTest" );
        client.setActive( true );
        ClientHome.create( client );

        Provider provider = createProvider( "ModelHomeTest provider 1" );
        Provider provider2 = createProvider( "ModelHomeTest provider 2" );

        Model model = new Model( );
        model.setClientId( client.getId( ) );
        model.setProviderId( provider.getId( ) );
        ModelHome.create( model );

        Optional<Model> optStored = ModelHome.findByPrimaryKey( model.getId( ) );
        assertTrue( optStored.isPresent( ) );
        assertEquals( client.getId( ), optStored.get( ).getClientId( ) );
        assertEquals( provider.getId( ), optStored.get( ).getProviderId( ) );

        model.setProviderId( provider2.getId( ) );
        ModelHome.update( model );
        optStored = ModelHome.findByPrimaryKey( model.getId( ) );
        assertTrue( optStored.isPresent( ) );
        assertEquals( provider2.getId( ), optStored.get( ).getProviderId( ) );

        ModelHome.remove( model.getId( ) );
        optStored = ModelHome.findByPrimaryKey( model.getId( ) );
        assertFalse( optStored.isPresent( ) );

        ModelHome.getModelsList( );
    }

    /**
     * Creates a Provider with all NOT NULL columns populated so the insert is valid on HSQL.
     *
     * @param strName
     *            the provider name
     * @return the created provider, with its generated identifier set
     */
    private Provider createProvider( String strName )
    {
        Provider provider = new Provider( );
        provider.setProviderName( strName );
        provider.setProviderType( "chat" );
        provider.setProviderVendor( "mistral" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-api-key" );
        ProviderHome.create( provider );
        return provider;
    }
}
