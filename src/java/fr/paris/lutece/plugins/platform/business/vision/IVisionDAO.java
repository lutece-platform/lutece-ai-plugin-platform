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

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceList;

/**
 * Interface for Vision Data Access Object operations
 */
public interface IVisionDAO
{

    /**
     * Insert a new vision into the database
     *
     * @param vision
     *            the vision object to insert
     * @param plugin
     *            the plugin context
     */
    void insert( Vision vision, Plugin plugin );

    /**
     * Update an existing vision in the database
     *
     * @param vision
     *            the vision object to update
     * @param plugin
     *            the plugin context
     */
    void store( Vision vision, Plugin plugin );

    /**
     * Delete a vision from the database by its key
     *
     * @param nKey
     *            the primary key of the vision to delete
     * @param plugin
     *            the plugin context
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Load a vision from the database by its key
     *
     * @param nKey
     *            the primary key of the vision to load
     * @param plugin
     *            the plugin context
     * @return an Optional containing the vision if found, empty otherwise
     */
    Optional<Vision> load( int nKey, Plugin plugin );

    /**
     * Select all visions from the database
     *
     * @param plugin
     *            the plugin context
     * @return a list of all visions
     */
    List<Vision> selectVisionsList( Plugin plugin );

    /**
     * Select all visions from the database for a specific client
     *
     * @param clientId
     *            the client identifier
     * @param plugin
     *            the plugin context
     * @return a list of visions for the specified client
     */
    List<Vision> selectVisionsListByClientId( int clientId, Plugin plugin );

    /**
     * Returns a reference list of all Visions (ID and name only) for RBAC purposes
     *
     * @param plugin
     *            The Plugin using this data access service
     * @return A ReferenceList containing vision IDs and names
     */
    ReferenceList selectVisionsReferenceList( Plugin plugin );
}
