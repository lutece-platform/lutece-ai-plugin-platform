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
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlatformResourceExecutionHome
{
    private static final IPlatformResourceExecutionDAO _dao = CDI.current( ).select( IPlatformResourceExecutionDAO.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PlatformResourceExecutionHome( )
    {
    }

    /**
     * Creates a new platform resource execution. Generates a UUID if executionId is null and sets start time to current time if not provided.
     *
     * @param execution
     *            The resource execution to create
     * @return The execution ID of the newly created resource execution
     */
    public static String create( PlatformResourceExecution execution )
    {
        if ( execution.getExecutionId( ) == null )
        {
            execution.setExecutionId( UUID.randomUUID( ).toString( ) );
        }
        if ( execution.getStartTime( ) == null )
        {
            execution.setStartTime( new Timestamp( System.currentTimeMillis( ) ) );
        }
        return _dao.insert( execution, _plugin );
    }

    /**
     * Updates an existing platform resource execution.
     *
     * @param execution
     *            The resource execution to update
     */
    public static void update( PlatformResourceExecution execution )
    {
        _dao.update( execution, _plugin );
    }

    /**
     * Updates only the total_cost column of an execution. Used by cost-refresh operations that must not race with concurrent completion calls writing status /
     * end_time.
     *
     * @param executionId
     *            The execution ID
     * @param totalCost
     *            The new total cost
     */
    public static void updateCost( String executionId, BigDecimal totalCost )
    {
        _dao.updateCost( executionId, totalCost, _plugin );
    }

    /**
     * Finds a platform resource execution by its primary key.
     *
     * @param executionId
     *            The ID of the resource execution to find
     * @return An Optional containing the found execution, or empty if not found
     */
    public static Optional<PlatformResourceExecution> findByPrimaryKey( String executionId )
    {
        return _dao.load( executionId, _plugin );
    }

    /**
     * Gets all executions for a specific resource type and ID.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The ID of the resource
     * @return List of executions for the specified resource
     */
    public static List<PlatformResourceExecution> getPlatformResourceExecutions( String resourceType, String resourceId )
    {
        return _dao.getPlatformResourceExecutions( resourceType, resourceId, _plugin );
    }

    /**
     * Removes a platform resource execution.
     *
     * @param executionId
     *            The ID of the execution to remove
     */
    public static void remove( String executionId )
    {
        _dao.delete( executionId, _plugin );
    }

    /**
     * Gets all pending platform resource executions.
     *
     * @return List of pending executions
     */
    public static List<PlatformResourceExecution> getPendingExecutions( )
    {
        return _dao.getPendingExecutions( _plugin );
    }

    /**
     * Gets all executions for a specific resource within a date range.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The ID of the resource
     * @param startDate
     *            The start of the date range
     * @param endDate
     *            The end of the date range
     * @return List of executions for the specified resource within the date range
     */
    public static List<PlatformResourceExecution> getPlatformResourceExecutionsByDateRange( String resourceType, String resourceId, Timestamp startDate,
            Timestamp endDate )
    {
        return _dao.getPlatformResourceExecutionsByDateRange( resourceType, resourceId, startDate, endDate, _plugin );
    }

    /**
     * Gets the total costs aggregated by resource for a specific client.
     *
     * @param clientId
     *            The ID of the client
     * @return A map of resource type to a map of resource ID to total cost
     */
    public static Map<String, Map<String, BigDecimal>> getTotalCostsByResource( int clientId )
    {
        return _dao.getTotalCostsByResource( clientId, _plugin );
    }

    /**
     * Gets the execution statistics aggregated by resource for a specific client.
     *
     * @param clientId
     *            The ID of the client
     * @return List of resource statistics
     */
    public static List<ResourceStats> getStatsByResource( int clientId )
    {
        return _dao.getStatsByResource( clientId, _plugin );
    }

    /**
     * Gets the execution statistics aggregated by resource for a specific client within a date range.
     *
     * @param clientId
     *            The ID of the client
     * @param startDate
     *            The start of the date range
     * @param endDate
     *            The end of the date range
     * @return List of resource statistics within the date range
     */
    public static List<ResourceStats> getStatsByResourceAndDateRange( int clientId, Timestamp startDate, Timestamp endDate )
    {
        return _dao.getStatsByResourceAndDateRange( clientId, startDate, endDate, _plugin );
    }

    /**
     * Gets the total cost aggregated by client for all clients.
     *
     * @return A map of client ID to total cost
     */
    public static Map<Integer, Double> getTotalCostByClient( )
    {
        return _dao.getTotalCostByClient( _plugin );
    }

    /**
     * Gets the daily execution statistics for a specific resource within a date range.
     *
     * @param resourceType
     *            The type of resource
     * @param resourceId
     *            The ID of the resource
     * @param startDate
     *            The start of the date range
     * @param endDate
     *            The end of the date range
     * @return List of daily resource statistics within the date range
     */
    public static List<DailyResourceStats> getDailyStatsByResource( String resourceType, String resourceId, Timestamp startDate, Timestamp endDate )
    {
        return _dao.getDailyStatsByResource( resourceType, resourceId, startDate, endDate, _plugin );
    }

    /**
     * Gets the daily execution statistics for a specific client within a date range.
     *
     * @param clientId
     *            The ID of the client
     * @param startDate
     *            The start of the date range
     * @param endDate
     *            The end of the date range
     * @return List of daily resource statistics for the client within the date range
     */
    public static List<DailyResourceStats> getDailyStatsByClient( int clientId, Timestamp startDate, Timestamp endDate )
    {
        return _dao.getDailyStatsByClient( clientId, startDate, endDate, _plugin );
    }
}
