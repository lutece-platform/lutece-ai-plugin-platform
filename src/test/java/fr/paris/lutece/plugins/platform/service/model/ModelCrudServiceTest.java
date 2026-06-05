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
package fr.paris.lutece.plugins.platform.service.model;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.model.dto.ModelCreateResult;
import fr.paris.lutece.plugins.platform.service.model.dto.ModelCreateResult.ModelCreateStatus;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link ModelCrudService}. These exercise the model CRUD and enrichment logic against HSQL: the uniqueness invariant on creation (one
 * model per client/provider pair), provider reassignment, removal returning the owning client, and lazy provider enrichment. The service is an
 * {@code @ApplicationScoped} CDI bean and is resolved through the CDI container; the underlying graph (client and providers) is seeded directly through the
 * Home facades.
 */
public class ModelCrudServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed model CRUD service.
     *
     * @return the model CRUD service instance
     */
    private ModelCrudService service( )
    {
        return CDI.current( ).select( ModelCrudService.class ).get( );
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
     * Creates and persists a provider with all NOT NULL columns populated so the insert is valid on HSQL.
     *
     * @param name
     *            the provider name
     * @return the persisted provider, with its generated identifier set
     */
    private Provider createProvider( String name )
    {
        Provider provider = new Provider( );
        provider.setProviderName( name );
        provider.setProviderType( "chat" );
        provider.setProviderVendor( "mistral" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-api-key" );
        ProviderHome.create( provider );
        return provider;
    }

    /**
     * A first creation for a client/provider pair succeeds and persists a row; a second creation for the same pair returns a DUPLICATE result without inserting
     * a second row, enforcing the at-most-one-model-per-pair invariant.
     */
    @Test
    public void testCreateModelEnforcesUniqueness( )
    {
        Client client = createClient( "model-svc-unique" );
        Provider provider = createProvider( "model-svc-unique-prov" );

        ModelCreateResult first = service( ).createModel( client.getId( ), provider.getId( ) );
        assertEquals( ModelCreateStatus.CREATED, first.status( ) );
        assertNotNull( first.model( ) );
        assertTrue( first.model( ).getId( ) > 0 );

        ModelCreateResult second = service( ).createModel( client.getId( ), provider.getId( ) );
        assertEquals( ModelCreateStatus.DUPLICATE, second.status( ) );
        assertNull( second.model( ) );

        assertEquals( 1, ModelHome.getModelsListByClientId( client.getId( ) ).size( ) );
    }

    /**
     * The same client may hold one model per distinct provider: creating against a second provider succeeds rather than being rejected as a duplicate.
     */
    @Test
    public void testCreateModelDistinctProvidersAllowed( )
    {
        Client client = createClient( "model-svc-distinct" );
        Provider provider1 = createProvider( "model-svc-distinct-p1" );
        Provider provider2 = createProvider( "model-svc-distinct-p2" );

        ModelCreateResult r1 = service( ).createModel( client.getId( ), provider1.getId( ) );
        ModelCreateResult r2 = service( ).createModel( client.getId( ), provider2.getId( ) );

        assertEquals( ModelCreateStatus.CREATED, r1.status( ) );
        assertEquals( ModelCreateStatus.CREATED, r2.status( ) );
        assertEquals( 2, ModelHome.getModelsListByClientId( client.getId( ) ).size( ) );
    }

    /**
     * Reassigning a model to a new provider persists the change and returns the updated model carrying the new provider identifier.
     *
     * @throws ResourceNotFoundException
     *             never, the model is seeded beforehand
     */
    @Test
    public void testUpdateModelProvider( ) throws ResourceNotFoundException
    {
        Client client = createClient( "model-svc-update" );
        Provider provider1 = createProvider( "model-svc-update-p1" );
        Provider provider2 = createProvider( "model-svc-update-p2" );

        Model created = service( ).createModel( client.getId( ), provider1.getId( ) ).model( );

        Model updated = service( ).updateModelProvider( created.getId( ), provider2.getId( ) );
        assertEquals( provider2.getId( ), updated.getProviderId( ) );

        Optional<Model> stored = ModelHome.findByPrimaryKey( created.getId( ) );
        assertTrue( stored.isPresent( ) );
        assertEquals( provider2.getId( ), stored.get( ).getProviderId( ) );
    }

    /**
     * Updating a non-existent model raises the typed {@link ResourceNotFoundException}.
     */
    @Test
    public void testUpdateModelProviderUnknownThrows( )
    {
        ModelCrudService service = service( );
        assertThrows( ResourceNotFoundException.class, ( ) -> service.updateModelProvider( 999999, 1 ) );
    }

    /**
     * Removing a model deletes the row and returns the owning client identifier for the post-delete redirect.
     *
     * @throws ResourceNotFoundException
     *             never, the model is seeded beforehand
     */
    @Test
    public void testRemoveModelReturnsOwningClient( ) throws ResourceNotFoundException
    {
        Client client = createClient( "model-svc-remove" );
        Provider provider = createProvider( "model-svc-remove-prov" );

        Model created = service( ).createModel( client.getId( ), provider.getId( ) ).model( );

        int ownerId = service( ).removeModel( created.getId( ) );
        assertEquals( client.getId( ), ownerId );
        assertFalse( ModelHome.findByPrimaryKey( created.getId( ) ).isPresent( ) );
    }

    /**
     * Removing a non-existent model raises the typed {@link ResourceNotFoundException}.
     */
    @Test
    public void testRemoveModelUnknownThrows( )
    {
        ModelCrudService service = service( );
        assertThrows( ResourceNotFoundException.class, ( ) -> service.removeModel( 999999 ) );
    }

    /**
     * Loading a model with its provider enriches the transient provider association from the persisted provider identifier.
     */
    @Test
    public void testGetModelWithProviderEnrichesProvider( )
    {
        Client client = createClient( "model-svc-enrich" );
        Provider provider = createProvider( "model-svc-enrich-prov" );

        Model created = service( ).createModel( client.getId( ), provider.getId( ) ).model( );

        Optional<Model> enriched = service( ).getModelWithProvider( created.getId( ) );
        assertTrue( enriched.isPresent( ) );
        assertNotNull( enriched.get( ).getProvider( ) );
        assertEquals( provider.getId( ), enriched.get( ).getProvider( ).getId( ) );
        assertEquals( "model-svc-enrich-prov", enriched.get( ).getProvider( ).getProviderName( ) );
    }

    /**
     * Loading a non-existent model returns an empty Optional rather than throwing.
     */
    @Test
    public void testGetModelWithProviderUnknownReturnsEmpty( )
    {
        assertTrue( service( ).getModelWithProvider( 999999 ).isEmpty( ) );
    }
}
