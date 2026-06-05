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
 * CRUD test for {@link VisionExtractorHome} on HSQL.
 *
 * <p>
 * A {@code platform_vision_extractor} row carries a NOT NULL foreign key to {@code platform_vision}, which itself depends on a client and a provider. The full
 * parent chain is created up front and torn down at the end.
 * </p>
 */
public class VisionExtractorHomeTest extends AbstractPlatformDbTest
{
    /**
     * Exercises the full CRUD cycle on a VisionExtractor: create, findByPrimaryKey, update, remove. A parent Vision (with its own Client and Provider parents)
     * is created first to satisfy the {@code vision_id} foreign key and removed at the end.
     */
    @Test
    public void testBusinessVisionExtractor( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );
        Vision vision = createVision( client.getId( ), provider.getId( ) );

        VisionExtractor extractor = new VisionExtractor( );
        extractor.setVisionId( vision.getId( ) );
        extractor.setExtractorName( "Extractor Name 1" );
        extractor.setExtractorDescription( "Extractor Description 1" );

        VisionExtractorHome.create( extractor );

        Optional<VisionExtractor> optStored = VisionExtractorHome.findByPrimaryKey( extractor.getId( ) );
        assertTrue( optStored.isPresent( ) );
        VisionExtractor extractorStored = optStored.get( );
        assertEquals( extractorStored.getVisionId( ), extractor.getVisionId( ) );
        assertEquals( extractorStored.getExtractorName( ), extractor.getExtractorName( ) );
        assertEquals( extractorStored.getExtractorDescription( ), extractor.getExtractorDescription( ) );

        extractor.setExtractorName( "Extractor Name 2" );
        extractor.setExtractorDescription( "Extractor Description 2" );
        VisionExtractorHome.update( extractor );

        extractorStored = VisionExtractorHome.findByPrimaryKey( extractor.getId( ) ).get( );
        assertEquals( extractorStored.getExtractorName( ), extractor.getExtractorName( ) );
        assertEquals( extractorStored.getExtractorDescription( ), extractor.getExtractorDescription( ) );

        VisionExtractorHome.findByVisionId( vision.getId( ) );

        VisionExtractorHome.remove( extractor.getId( ) );
        optStored = VisionExtractorHome.findByPrimaryKey( extractor.getId( ) );
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
        client.setCode( "test-extractor-client" );
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

    /**
     * Creates a minimal vision FK parent.
     *
     * @param nClientId
     *            The client identifier
     * @param nProviderId
     *            The provider identifier
     * @return The persisted vision
     */
    private Vision createVision( int nClientId, int nProviderId )
    {
        Vision vision = new Vision( );
        vision.setVisionTitle( "Vision for extractor" );
        vision.setClientId( nClientId );
        vision.setProviderId( nProviderId );
        return VisionHome.create( vision );
    }
}
