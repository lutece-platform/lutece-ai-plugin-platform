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

import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.model.dto.ModelCreateResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Domain service hosting CRUD, enrichment and validation logic for the Model entity. The web boundary delegates all orchestration (uniqueness rule, provider
 * reassignment, removal, provider enrichment and client-code resolution) to this service and only maps the typed results to the view and i18n messages.
 */
@ApplicationScoped
@Named( "platform.modelCrudService" )
public class ModelCrudService
{
    /**
     * Loads a model and lazily enriches it with its provider so the view never has to perform the lookup itself.
     *
     * @param nModelId
     *            the model identifier
     * @return an Optional containing the provider-enriched model if found, empty otherwise
     */
    public Optional<Model> getModelWithProvider( int nModelId )
    {
        return ModelHome.findByPrimaryKey( nModelId ).map( this::enrichWithProvider );
    }

    /**
     * Creates a model for a client and provider, enforcing the uniqueness invariant: at most one model per (client, provider) pair.
     *
     * @param nClientId
     *            the owning client identifier
     * @param nProviderId
     *            the provider identifier
     * @return a typed result carrying either the created model or a DUPLICATE outcome
     */
    public ModelCreateResult createModel( int nClientId, int nProviderId )
    {
        if ( ModelHome.findByClientIdAndProviderId( nClientId, nProviderId ).isPresent( ) )
        {
            return ModelCreateResult.duplicate( );
        }

        Model model = new Model( );
        model.setClientId( nClientId );
        model.setProviderId( nProviderId );
        ModelHome.create( model );

        return ModelCreateResult.created( model );
    }

    /**
     * Reassigns the provider of an existing model and persists the change.
     *
     * @param nModelId
     *            the model identifier
     * @param nProviderId
     *            the new provider identifier
     * @return the updated model
     * @throws ResourceNotFoundException
     *             if no model exists for the given identifier
     */
    public Model updateModelProvider( int nModelId, int nProviderId ) throws ResourceNotFoundException
    {
        Model model = ModelHome.findByPrimaryKey( nModelId ).orElseThrow( ResourceNotFoundException::new );
        model.setProviderId( nProviderId );
        ModelHome.update( model );
        return model;
    }

    /**
     * Removes a model and returns the owning client identifier for the post-delete redirect.
     *
     * @param nModelId
     *            the model identifier
     * @return the owning client identifier
     * @throws ResourceNotFoundException
     *             if no model exists for the given identifier
     */
    public int removeModel( int nModelId ) throws ResourceNotFoundException
    {
        Model model = ModelHome.findByPrimaryKey( nModelId ).orElseThrow( ResourceNotFoundException::new );
        int nClientId = model.getClientId( );
        ModelHome.remove( nModelId );
        return nClientId;
    }

    /**
     * Lazily attaches the provider to the model if not already loaded.
     *
     * @param model
     *            the model to enrich
     * @return the same model instance, with its provider populated when resolvable
     */
    private Model enrichWithProvider( Model model )
    {
        if ( model.getProvider( ) == null )
        {
            ProviderHome.findByPrimaryKey( model.getProviderId( ) ).ifPresent( model::setProvider );
        }
        return model;
    }
}
