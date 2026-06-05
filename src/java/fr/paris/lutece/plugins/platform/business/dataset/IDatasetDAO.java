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
package fr.paris.lutece.plugins.platform.business.dataset;

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceList;

/**
 * Interface for Dataset DAO
 */
public interface IDatasetDAO
{
    /**
     * Insert a new dataset in the database
     *
     * @param dataset
     *            The dataset object to insert
     * @param plugin
     *            The plugin
     */
    void insert( Dataset dataset, Plugin plugin );

    /**
     * Store (update) a dataset in the database
     *
     * @param dataset
     *            The dataset object to store
     * @param plugin
     *            The plugin
     */
    void store( Dataset dataset, Plugin plugin );

    /**
     * Delete a dataset from the database
     *
     * @param nKey
     *            The dataset ID to delete
     * @param plugin
     *            The plugin
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Load a dataset from the database
     *
     * @param nKey
     *            The dataset ID to load
     * @param plugin
     *            The plugin
     * @return An Optional containing the dataset if found
     */
    Optional<Dataset> load( int nKey, Plugin plugin );

    /**
     * Select all datasets from the database
     *
     * @param plugin
     *            The plugin
     * @return The list of all datasets
     */
    List<Dataset> selectDatasetsList( Plugin plugin );

    /**
     * Returns a reference list of all Datasets (ID and name only) for RBAC purposes
     *
     * @param plugin
     *            The Plugin using this data access service
     * @return A ReferenceList containing dataset IDs and names
     */
    ReferenceList selectDatasetsReferenceList( Plugin plugin );

    /**
     * Loads the datasets belonging to a client
     *
     * @param nClientId
     *            The client identifier
     * @param plugin
     *            the Plugin
     * @return The list of datasets of the client
     */
    List<Dataset> selectDatasetsByClientId( int nClientId, Plugin plugin );
}
