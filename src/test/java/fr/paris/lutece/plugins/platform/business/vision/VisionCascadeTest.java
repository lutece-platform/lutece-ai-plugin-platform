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

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Cascade test for {@link VisionHome} on HSQL.
 *
 * <p>
 * Proves that the {@code ON DELETE CASCADE} foreign key from {@code platform_vision_extractor.vision_id} to {@code platform_vision} is enforced: removing the
 * root vision must wipe its extractor children. HSQL honours foreign keys, so the cascade is exercised for real.
 * </p>
 */
public class VisionCascadeTest extends AbstractPlatformDbTest
{
    /**
     * Builds the full graph (Client, Provider, root Vision, one VisionExtractor child), asserts the child exists, removes the root vision once, then asserts
     * the extractor child was cascaded away. The remaining FK parents are torn down at the end.
     */
    @Test
    public void testCascadeOnDeleteVision( )
    {
        Client client = createClient( );
        Provider provider = createProvider( );

        Vision vision = new Vision( );
        vision.setVisionTitle( "Cascade Vision" );
        vision.setVisionDescription( "Cascade Vision Description" );
        vision.setClientId( client.getId( ) );
        vision.setProviderId( provider.getId( ) );
        VisionHome.create( vision );

        VisionExtractor extractor = new VisionExtractor( );
        extractor.setVisionId( vision.getId( ) );
        extractor.setExtractorName( "Cascade Extractor" );
        extractor.setExtractorDescription( "Cascade Extractor Description" );
        VisionExtractorHome.create( extractor );

        assertTrue( VisionExtractorHome.findByPrimaryKey( extractor.getId( ) ).isPresent( ) );
        assertFalse( VisionExtractorHome.findByVisionId( vision.getId( ) ).isEmpty( ) );

        VisionHome.remove( vision.getId( ) );

        assertTrue( VisionExtractorHome.findByPrimaryKey( extractor.getId( ) ).isEmpty( ) );
        assertTrue( VisionExtractorHome.findByVisionId( vision.getId( ) ).isEmpty( ) );
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
        client.setCode( "test-vision-cascade-client" );
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
