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
import fr.paris.lutece.util.ReferenceList;

/**
 * Interface for Model data access operations
 */
public interface IModelDAO
{
    /**
     * Inserts a new Model in the database
     *
     * @param model
     *            The Model object to insert
     * @param plugin
     *            The Plugin using this data access service
     */
    void insert( Model model, Plugin plugin );

    /**
     * Updates a Model in the database
     *
     * @param model
     *            The Model object to store
     * @param plugin
     *            The Plugin using this data access service
     */
    void store( Model model, Plugin plugin );

    /**
     * Deletes a Model from the database
     *
     * @param nKey
     *            The Model identifier
     * @param plugin
     *            The Plugin using this data access service
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Loads a Model from the database
     *
     * @param nKey
     *            The Model identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return An Optional containing the Model if found, empty otherwise
     */
    Optional<Model> load( int nKey, Plugin plugin );

    /**
     * Returns the list of all Models
     *
     * @param plugin
     *            The Plugin using this data access service
     * @return The list of Models
     */
    List<Model> selectModelsList( Plugin plugin );

    /**
     * Returns the list of Models for a specific client
     *
     * @param clientId
     *            The client identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return The list of Models for the specified client
     */
    List<Model> selectModelsListByClientId( int clientId, Plugin plugin );

    /**
     * Finds a Model by client ID and provider ID
     *
     * @param clientId
     *            The client identifier
     * @param providerId
     *            The provider identifier
     * @param plugin
     *            The Plugin using this data access service
     * @return An Optional containing the Model if found, empty otherwise
     */
    Optional<Model> selectByClientIdAndProviderId( int clientId, int providerId, Plugin plugin );

    /**
     * Returns a reference list of all Models (ID and provider name) for RBAC purposes
     *
     * @param plugin
     *            The Plugin using this data access service
     * @return A ReferenceList containing model IDs and provider names
     */
    ReferenceList selectModelsReferenceList( Plugin plugin );
}
