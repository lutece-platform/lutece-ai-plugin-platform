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
package fr.paris.lutece.plugins.platform.business.observability;

import fr.paris.lutece.portal.service.plugin.Plugin;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IPlatformResourceExecutionDAO
{
    /**
     * Inserts a new platform resource execution in the database
     *
     * @param execution
     *            The platform resource execution to insert
     * @param plugin
     *            The plugin
     * @return The newly created execution ID
     */
    String insert( PlatformResourceExecution execution, Plugin plugin );

    /**
     * Updates an existing platform resource execution in the database
     *
     * @param execution
     *            The platform resource execution to update
     * @param plugin
     *            The plugin
     */
    void update( PlatformResourceExecution execution, Plugin plugin );

    /**
     * Updates only the total_cost column of a platform resource execution. Used by cost-refresh operations that must not overwrite status / end_time set by
     * concurrent completion calls.
     *
     * @param executionId
     *            The execution ID
     * @param totalCost
     *            The new total cost
     * @param plugin
     *            The plugin
     */
    void updateCost( String executionId, BigDecimal totalCost, Plugin plugin );

    /**
     * Loads a platform resource execution from the database
     *
     * @param executionId
     *            The ID of the execution to load
     * @param plugin
     *            The plugin
     * @return An optional containing the execution if found, empty otherwise
     */
    Optional<PlatformResourceExecution> load( String executionId, Plugin plugin );

    /**
     * Retrieves all executions for a specific resource
     *
     * @param resourceType
     *            The type of the resource
     * @param resourceId
     *            The ID of the resource
     * @param plugin
     *            The plugin
     * @return A list of platform resource executions
     */
    List<PlatformResourceExecution> getPlatformResourceExecutions( String resourceType, String resourceId, Plugin plugin );

    /**
     * Deletes a platform resource execution from the database
     *
     * @param executionId
     *            The ID of the execution to delete
     * @param plugin
     *            The plugin
     */
    void delete( String executionId, Plugin plugin );

    /**
     * Retrieves all pending executions
     *
     * @param plugin
     *            The plugin
     * @return A list of pending platform resource executions
     */
    List<PlatformResourceExecution> getPendingExecutions( Plugin plugin );

    List<PlatformResourceExecution> getPlatformResourceExecutionsByDateRange( String resourceType, String resourceId, Timestamp startDate, Timestamp endDate,
            Plugin plugin );

    Map<String, Map<String, BigDecimal>> getTotalCostsByResource( int clientId, Plugin plugin );

    List<ResourceStats> getStatsByResource( int clientId, Plugin plugin );

    List<ResourceStats> getStatsByResourceAndDateRange( int clientId, Timestamp startDate, Timestamp endDate, Plugin plugin );

    /**
     * Returns the total cost grouped by client_id in a single query.
     *
     * @param plugin
     *            the Plugin
     * @return a map of clientId to totalCost
     */
    Map<Integer, Double> getTotalCostByClient( Plugin plugin );

    List<DailyResourceStats> getDailyStatsByResource( String resourceType, String resourceId, Timestamp startDate, Timestamp endDate, Plugin plugin );

    List<DailyResourceStats> getDailyStatsByClient( int clientId, Timestamp startDate, Timestamp endDate, Plugin plugin );
}
