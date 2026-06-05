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
package fr.paris.lutece.plugins.platform.business.provider;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Test class for the ProviderHome data access facade
 */
public class ProviderHomeTest extends AbstractPlatformDbTest
{
    /**
     * Tests the full CRUD lifecycle of a Provider through ProviderHome : create, findByPrimaryKey, update, remove and list
     */
    @Test
    public void testBusinessProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "Provider 1" );
        provider.setProviderDescription( "ProviderDescription 1" );
        provider.setProviderType( "llm" );
        provider.setProviderVendor( "azure_openai" );
        provider.setDeploymentName( "DeploymentName 1" );
        provider.setDeploymentModelName( "DeploymentModelName 1" );
        provider.setDeploymentEndpoint( "http://localhost/v1" );
        provider.setDeploymentApiVersion( "2024-01-01" );
        provider.setDeploymentApiKey( "ApiKey 1" );
        provider.setTokenInputPrice1M( 1.5 );
        provider.setTokenOutputPrice1M( 2.5 );
        provider.setDocumentAnalysisPrice1000Pages( 10.0 );

        ProviderHome.create( provider );

        Optional<Provider> optProviderStored = ProviderHome.findByPrimaryKey( provider.getId( ) );
        assertNotNull( optProviderStored );
        assertTrue( optProviderStored.isPresent( ) );
        Provider providerStored = optProviderStored.get( );
        assertEquals( providerStored.getProviderName( ), provider.getProviderName( ) );
        assertEquals( providerStored.getProviderDescription( ), provider.getProviderDescription( ) );
        assertEquals( providerStored.getProviderType( ), provider.getProviderType( ) );
        assertEquals( providerStored.getDeploymentModelName( ), provider.getDeploymentModelName( ) );
        assertEquals( providerStored.getDeploymentApiKey( ), provider.getDeploymentApiKey( ) );

        provider.setProviderDescription( "ProviderDescription 2" );
        ProviderHome.update( provider );
        optProviderStored = ProviderHome.findByPrimaryKey( provider.getId( ) );
        assertTrue( optProviderStored.isPresent( ) );
        assertEquals( optProviderStored.get( ).getProviderDescription( ), provider.getProviderDescription( ) );

        ProviderHome.remove( provider.getId( ) );
        optProviderStored = ProviderHome.findByPrimaryKey( provider.getId( ) );
        assertFalse( optProviderStored.isPresent( ) );

        ProviderHome.getProvidersList( );
    }
}
