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
package fr.paris.lutece.plugins.platform.business.subscription;

import fr.paris.lutece.portal.service.plugin.Plugin;
import java.util.List;
import java.util.Optional;

/**
 * Interface for managing Subscription data in the database.
 */
public interface ISubscriptionDAO
{
    /**
     * Creates a new Subscription record in the database.
     *
     * @param subscription
     *            The Subscription object to insert
     * @param plugin
     *            The Plugin object
     */
    void insert( Subscription subscription, Plugin plugin );

    /**
     * Updates an existing Subscription record in the database.
     *
     * @param subscription
     *            The Subscription object to update
     * @param plugin
     *            The Plugin object
     */
    void store( Subscription subscription, Plugin plugin );

    /**
     * Removes a Subscription record from the database.
     *
     * @param nKey
     *            The identifier of the Subscription to delete
     * @param plugin
     *            The Plugin object
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Loads a Subscription by its primary key.
     *
     * @param nKey
     *            The identifier of the Subscription to load
     * @param plugin
     *            The Plugin object
     * @return An Optional containing the Subscription if found
     */
    Optional<Subscription> load( int nKey, Plugin plugin );

    /**
     * Retrieves all Subscriptions from the database.
     *
     * @param plugin
     *            The Plugin object
     * @return List of all Subscription objects
     */
    List<Subscription> selectSubscriptionsList( Plugin plugin );

    /**
     * Retrieves all Subscriptions for a specific client.
     *
     * @param nClientId
     *            The client identifier
     * @param plugin
     *            The Plugin object
     * @return List of Subscription objects for the client
     */
    List<Subscription> selectSubscriptionsByClientId( int nClientId, Plugin plugin );

    /**
     * Finds a Subscription by client and resource information.
     *
     * @param nClientId
     *            The client identifier
     * @param strResourceType
     *            The type of resource
     * @param strResourceId
     *            The resource identifier
     * @param plugin
     *            The Plugin object
     * @return An Optional containing the Subscription if found
     */
    Optional<Subscription> findByClientAndResource( int nClientId, String strResourceType, String strResourceId, Plugin plugin );

    /**
     * Retrieves all Subscriptions for a specific resource.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The resource identifier
     * @param plugin
     *            The Plugin object
     * @return List of Subscription objects for the resource
     */
    List<Subscription> selectSubscriptionsByResource( String resourceType, String resourceId, Plugin plugin );

}
