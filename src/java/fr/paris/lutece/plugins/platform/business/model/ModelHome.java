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

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.util.ReferenceList;

/**
 * This class provides instances management methods for Model objects
 */
public final class ModelHome
{
    private static IModelDAO _dao = CDI.current( ).select( IModelDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private ModelHome( )
    {
    }

    /**
     * Creates a new Model
     *
     * @param model
     *            The Model object to create
     * @return The created Model
     */
    public static Model create( Model model )
    {
        _dao.insert( model, _plugin );
        return model;
    }

    /**
     * Updates a Model
     *
     * @param model
     *            The Model object to update
     * @return The updated Model
     */
    public static Model update( Model model )
    {
        _dao.store( model, _plugin );
        return model;
    }

    /**
     * Removes a Model
     *
     * @param nKey
     *            The Model identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Finds a Model by its primary key
     *
     * @param nKey
     *            The Model identifier
     * @return An Optional containing the Model if found, empty otherwise
     */
    public static Optional<Model> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of all Models
     *
     * @return The list of Models
     */
    public static List<Model> getModelsList( )
    {
        return _dao.selectModelsList( _plugin );
    }

    /**
     * Returns the list of Models for a specific client
     *
     * @param clientId
     *            The client identifier
     * @return The list of Models for the specified client
     */
    public static List<Model> getModelsListByClientId( int clientId )
    {
        return _dao.selectModelsListByClientId( clientId, _plugin );
    }

    /**
     * Finds a Model by client ID and provider ID
     *
     * @param clientId
     *            The client identifier
     * @param providerId
     *            The provider identifier
     * @return An Optional containing the Model if found, empty otherwise
     */
    public static Optional<Model> findByClientIdAndProviderId( int clientId, int providerId )
    {
        return _dao.selectByClientIdAndProviderId( clientId, providerId, _plugin );
    }

    /**
     * Returns a reference list of all Models (ID and provider name) for RBAC purposes
     *
     * @return A ReferenceList containing model IDs and provider names
     */
    public static ReferenceList getModelsReferenceList( )
    {
        return _dao.selectModelsReferenceList( _plugin );
    }
}
