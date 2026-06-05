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
package fr.paris.lutece.plugins.platform.business.client;

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;

/**
 * Data Access Object (DAO) interface for Client management.
 */
public interface IClientDAO
{
    /**
     * Creates a new Client in the database.
     *
     * @param client
     *            The Client object to insert
     * @param plugin
     *            The Plugin object
     */
    void insert( Client client, Plugin plugin );

    /**
     * Updates an existing Client in the database.
     *
     * @param client
     *            The Client object to update
     * @param plugin
     *            The Plugin object
     */
    void store( Client client, Plugin plugin );

    /**
     * Removes a Client from the database.
     *
     * @param nKey
     *            The Client identifier
     * @param plugin
     *            The Plugin object
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Loads a Client by its identifier.
     *
     * @param nKey
     *            The Client identifier
     * @param plugin
     *            The Plugin object
     * @return An Optional containing the Client if found
     */
    Optional<Client> load( int nKey, Plugin plugin );

    /**
     * Retrieves all Clients from the database.
     *
     * @param plugin
     *            The Plugin object
     * @return List of all Clients
     */
    List<Client> selectClientsList( Plugin plugin );

    /**
     * Retrieves only active Clients from the database.
     *
     * @param plugin
     *            The Plugin object
     * @return List of active Clients
     */
    List<Client> selectActiveClientsList( Plugin plugin );
}
