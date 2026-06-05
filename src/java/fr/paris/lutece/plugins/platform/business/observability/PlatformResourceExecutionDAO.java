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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

@ApplicationScoped
@Named( "platform.platformResourceExecutionDAO" )
public class PlatformResourceExecutionDAO implements IPlatformResourceExecutionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_observability_resource_execution "
            + "(execution_id, resource_type, resource_id, client_id, status, start_time, end_time, error_message, input_data, output_data, total_cost) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_observability_resource_execution "
            + "SET status = ?, start_time = ?, end_time = ?, error_message = ?, input_data = ?, output_data = ?, client_id = ?, "
            + "resource_type = ?, resource_id = ?, total_cost = ? " + "WHERE execution_id = ?";
    private static final String SQL_QUERY_UPDATE_COST = "UPDATE platform_observability_resource_execution SET total_cost = ? WHERE execution_id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT execution_id, resource_type, resource_id, client_id, status, "
            + "start_time, end_time, error_message, input_data, output_data, total_cost "
            + "FROM platform_observability_resource_execution WHERE execution_id = ?";
    private static final String SQL_QUERY_SELECT_BY_RESOURCE = "SELECT execution_id, resource_type, resource_id, client_id, status, "
            + "start_time, end_time, error_message, input_data, output_data, total_cost "
            + "FROM platform_observability_resource_execution WHERE resource_type = ? AND resource_id = ? ORDER BY start_time DESC";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_observability_resource_execution WHERE execution_id = ?";
    private static final String SQL_QUERY_SELECT_PENDING = "SELECT execution_id, resource_type, resource_id, client_id, status, "
            + "start_time, end_time, error_message, input_data, output_data, total_cost "
            + "FROM platform_observability_resource_execution WHERE status IN ('PENDING', 'RUNNING') ORDER BY start_time";
    private static final String SQL_QUERY_SELECT_BY_RESOURCE_DATE_RANGE = "SELECT execution_id, resource_type, resource_id, client_id, status, "
            + "start_time, end_time, error_message, input_data, output_data, total_cost "
            + "FROM platform_observability_resource_execution WHERE resource_type = ? AND resource_id = ? "
            + "AND start_time >= ? AND start_time <= ? ORDER BY start_time DESC";

    private static final String SQL_QUERY_SELECT_TOTAL_COST_BY_RESOURCE = "SELECT resource_type, resource_id, SUM(total_cost) as total_cost "
            + "FROM platform_observability_resource_execution WHERE client_id = ? " + "GROUP BY resource_type, resource_id";

    private static final String SQL_QUERY_SELECT_TOTAL_COST_BY_CLIENT = "SELECT client_id, COALESCE(SUM(total_cost), 0) as total_cost "
            + "FROM platform_observability_resource_execution GROUP BY client_id";

    private static final String SQL_QUERY_SELECT_STATS_BY_RESOURCE = "SELECT resource_type, resource_id, "
            + "SUM(total_cost) as total_cost, COUNT(*) as execution_count, " + "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as success_count, "
            + "MAX(start_time) as last_execution " + "FROM platform_observability_resource_execution WHERE client_id = ? "
            + "GROUP BY resource_type, resource_id";

    private static final String SQL_QUERY_SELECT_DAILY_STATS_BY_RESOURCE = "SELECT DATE(start_time) as day, COUNT(*) as execution_count, "
            + "COALESCE(SUM(total_cost), 0) as total_cost, " + "AVG(TIMESTAMPDIFF(SECOND, start_time, end_time)) as avg_duration, "
            + "SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) as error_count " + "FROM platform_observability_resource_execution "
            + "WHERE resource_type = ? AND resource_id = ? AND start_time >= ? AND start_time <= ? " + "GROUP BY DATE(start_time) ORDER BY day";

    private static final String SQL_QUERY_SELECT_DAILY_STATS_BY_CLIENT = "SELECT DATE(start_time) as day, COUNT(*) as execution_count, "
            + "COALESCE(SUM(total_cost), 0) as total_cost, " + "AVG(TIMESTAMPDIFF(SECOND, start_time, end_time)) as avg_duration, "
            + "SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) as error_count " + "FROM platform_observability_resource_execution "
            + "WHERE client_id = ? AND start_time >= ? AND start_time <= ? " + "GROUP BY DATE(start_time) ORDER BY day";

    private static final String SQL_QUERY_SELECT_DAILY_STATS_BY_CLIENT_ALL = "SELECT DATE(start_time) as day, COUNT(*) as execution_count, "
            + "COALESCE(SUM(total_cost), 0) as total_cost, " + "AVG(TIMESTAMPDIFF(SECOND, start_time, end_time)) as avg_duration, "
            + "SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) as error_count " + "FROM platform_observability_resource_execution " + "WHERE client_id = ? "
            + "GROUP BY DATE(start_time) ORDER BY day";

    private static final String SQL_QUERY_SELECT_STATS_BY_RESOURCE_DATE_RANGE = "SELECT resource_type, resource_id, "
            + "SUM(total_cost) as total_cost, COUNT(*) as execution_count, " + "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as success_count, "
            + "MAX(start_time) as last_execution " + "FROM platform_observability_resource_execution WHERE client_id = ? "
            + "AND start_time >= ? AND start_time <= ? " + "GROUP BY resource_type, resource_id";

    @Override
    public String insert( PlatformResourceExecution execution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, execution.getExecutionId( ) );
            daoUtil.setString( nIndex++, execution.getResourceType( ) );
            daoUtil.setString( nIndex++, execution.getResourceId( ) );
            daoUtil.setInt( nIndex++, execution.getClientId( ) );
            daoUtil.setString( nIndex++, execution.getStatus( ).getValue( ) );
            daoUtil.setTimestamp( nIndex++, execution.getStartTime( ) );
            daoUtil.setTimestamp( nIndex++, execution.getEndTime( ) );
            daoUtil.setString( nIndex++, execution.getErrorMessage( ) );
            daoUtil.setString( nIndex++, execution.getInputData( ) );
            daoUtil.setString( nIndex++, execution.getOutputData( ) );
            daoUtil.setBigDecimal( nIndex, execution.getTotalCost( ) );
            daoUtil.executeUpdate( );
            return execution.getExecutionId( );
        }
    }

    @Override
    public void update( PlatformResourceExecution execution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, execution.getStatus( ).getValue( ) );
            daoUtil.setTimestamp( nIndex++, execution.getStartTime( ) );
            daoUtil.setTimestamp( nIndex++, execution.getEndTime( ) );
            daoUtil.setString( nIndex++, execution.getErrorMessage( ) );
            daoUtil.setString( nIndex++, execution.getInputData( ) );
            daoUtil.setString( nIndex++, execution.getOutputData( ) );
            daoUtil.setInt( nIndex++, execution.getClientId( ) );
            daoUtil.setString( nIndex++, execution.getResourceType( ) );
            daoUtil.setString( nIndex++, execution.getResourceId( ) );
            daoUtil.setBigDecimal( nIndex++, execution.getTotalCost( ) );
            daoUtil.setString( nIndex, execution.getExecutionId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Updates only the total_cost column to avoid overwriting status / end_time set by concurrent completion calls.
     *
     * @param executionId
     *            The execution ID
     * @param totalCost
     *            The new total cost
     * @param plugin
     *            The plugin
     */
    @Override
    public void updateCost( String executionId, BigDecimal totalCost, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE_COST, plugin ) )
        {
            daoUtil.setBigDecimal( 1, totalCost );
            daoUtil.setString( 2, executionId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public Optional<PlatformResourceExecution> load( String executionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setString( 1, executionId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( map( daoUtil ) );
            }
            return Optional.empty( );
        }
    }

    @Override
    public List<PlatformResourceExecution> getPlatformResourceExecutions( String resourceType, String resourceId, Plugin plugin )
    {
        List<PlatformResourceExecution> executions = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_RESOURCE, plugin ) )
        {
            daoUtil.setString( 1, resourceType );
            daoUtil.setString( 2, resourceId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executions.add( map( daoUtil ) );
            }
        }
        return executions;
    }

    @Override
    public void delete( String executionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setString( 1, executionId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<PlatformResourceExecution> getPendingExecutions( Plugin plugin )
    {
        List<PlatformResourceExecution> executions = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_PENDING, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executions.add( map( daoUtil ) );
            }
        }
        return executions;
    }

    @Override
    public List<PlatformResourceExecution> getPlatformResourceExecutionsByDateRange( String resourceType, String resourceId, Timestamp startDate,
            Timestamp endDate, Plugin plugin )
    {
        List<PlatformResourceExecution> executions = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_RESOURCE_DATE_RANGE, plugin ) )
        {
            daoUtil.setString( 1, resourceType );
            daoUtil.setString( 2, resourceId );
            daoUtil.setTimestamp( 3, startDate );
            daoUtil.setTimestamp( 4, endDate );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executions.add( map( daoUtil ) );
            }
        }
        return executions;
    }

    /**
     * Maps the current row of the given DAOUtil to a PlatformResourceExecution
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to map
     * @return the mapped PlatformResourceExecution
     */
    private PlatformResourceExecution map( DAOUtil daoUtil )
    {
        PlatformResourceExecution execution = new PlatformResourceExecution( );
        int nIndex = 1;
        execution.setExecutionId( daoUtil.getString( nIndex++ ) );
        execution.setResourceType( daoUtil.getString( nIndex++ ) );
        execution.setResourceId( daoUtil.getString( nIndex++ ) );
        execution.setClientId( daoUtil.getInt( nIndex++ ) );
        execution.setStatus( PlatformResourceExecutionStatus.fromString( daoUtil.getString( nIndex++ ) ) );
        execution.setStartTime( daoUtil.getTimestamp( nIndex++ ) );
        execution.setEndTime( daoUtil.getTimestamp( nIndex++ ) );
        execution.setErrorMessage( daoUtil.getString( nIndex++ ) );
        execution.setInputData( daoUtil.getString( nIndex++ ) );
        execution.setOutputData( daoUtil.getString( nIndex++ ) );
        execution.setTotalCost( daoUtil.getBigDecimal( nIndex ) );
        return execution;
    }

    @Override
    public Map<String, Map<String, BigDecimal>> getTotalCostsByResource( int clientId, Plugin plugin )
    {
        Map<String, Map<String, BigDecimal>> costsByResource = new HashMap<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_TOTAL_COST_BY_RESOURCE, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                String resourceType = daoUtil.getString( 1 );
                String resourceId = daoUtil.getString( 2 );
                BigDecimal totalCost = daoUtil.getBigDecimal( 3 );
                if ( resourceType != null && resourceId != null )
                {
                    costsByResource.computeIfAbsent( resourceType, k -> new HashMap<>( ) );
                    costsByResource.get( resourceType ).put( resourceId, totalCost != null ? totalCost : BigDecimal.ZERO );
                }
            }
        }
        return costsByResource;
    }

    @Override
    public List<ResourceStats> getStatsByResource( int clientId, Plugin plugin )
    {
        List<ResourceStats> statsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_STATS_BY_RESOURCE, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ResourceStats stats = mapStats( daoUtil );
                if ( stats != null )
                {
                    statsList.add( stats );
                }
            }
        }
        return statsList;
    }

    @Override
    public List<ResourceStats> getStatsByResourceAndDateRange( int clientId, Timestamp startDate, Timestamp endDate, Plugin plugin )
    {
        List<ResourceStats> statsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_STATS_BY_RESOURCE_DATE_RANGE, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.setTimestamp( 2, startDate );
            daoUtil.setTimestamp( 3, endDate );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                ResourceStats stats = mapStats( daoUtil );
                if ( stats != null )
                {
                    statsList.add( stats );
                }
            }
        }
        return statsList;
    }

    /**
     * Maps the current row of the given DAOUtil to a ResourceStats, computing the success rate
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to map
     * @return the mapped ResourceStats, or null if the resource type or id is null
     */
    private ResourceStats mapStats( DAOUtil daoUtil )
    {
        String resourceType = daoUtil.getString( 1 );
        String resourceId = daoUtil.getString( 2 );

        if ( resourceType == null || resourceId == null )
        {
            return null;
        }

        BigDecimal totalCost = daoUtil.getBigDecimal( 3 );
        int executionCount = daoUtil.getInt( 4 );
        int successCount = daoUtil.getInt( 5 );
        Timestamp lastExecution = daoUtil.getTimestamp( 6 );

        ResourceStats stats = new ResourceStats( );
        stats.setResourceType( resourceType );
        stats.setResourceId( resourceId );
        stats.setTotalCost( totalCost != null ? totalCost.doubleValue( ) : 0.0 );
        stats.setExecutionCount( executionCount );
        stats.setSuccessRate( executionCount > 0 ? ( successCount * 100.0 / executionCount ) : 0.0 );
        stats.setLastExecution( lastExecution );

        return stats;
    }

    @Override
    public Map<Integer, Double> getTotalCostByClient( Plugin plugin )
    {
        Map<Integer, Double> costByClient = new HashMap<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_TOTAL_COST_BY_CLIENT, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                int clientId = daoUtil.getInt( 1 );
                BigDecimal totalCost = daoUtil.getBigDecimal( 2 );
                costByClient.put( clientId, totalCost != null ? totalCost.doubleValue( ) : 0.0 );
            }
        }
        return costByClient;
    }

    @Override
    public List<DailyResourceStats> getDailyStatsByResource( String resourceType, String resourceId, Timestamp startDate, Timestamp endDate, Plugin plugin )
    {
        List<DailyResourceStats> statsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_DAILY_STATS_BY_RESOURCE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, resourceType );
            daoUtil.setString( nIndex++, resourceId );
            daoUtil.setTimestamp( nIndex++, startDate );
            daoUtil.setTimestamp( nIndex, endDate );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                statsList.add( mapDailyStats( daoUtil ) );
            }
        }
        return statsList;
    }

    @Override
    public List<DailyResourceStats> getDailyStatsByClient( int clientId, Timestamp startDate, Timestamp endDate, Plugin plugin )
    {
        List<DailyResourceStats> statsList = new ArrayList<>( );
        boolean hasDateRange = startDate != null && endDate != null;
        String sql = hasDateRange ? SQL_QUERY_SELECT_DAILY_STATS_BY_CLIENT : SQL_QUERY_SELECT_DAILY_STATS_BY_CLIENT_ALL;

        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, clientId );
            if ( hasDateRange )
            {
                daoUtil.setTimestamp( nIndex++, startDate );
                daoUtil.setTimestamp( nIndex, endDate );
            }
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                statsList.add( mapDailyStats( daoUtil ) );
            }
        }
        return statsList;
    }

    /**
     * Maps the current row of the given DAOUtil to a DailyResourceStats
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to map
     * @return the mapped DailyResourceStats
     */
    private DailyResourceStats mapDailyStats( DAOUtil daoUtil )
    {
        DailyResourceStats stats = new DailyResourceStats( );
        stats.setDay( daoUtil.getDate( 1 ) );
        stats.setExecutionCount( daoUtil.getInt( 2 ) );
        BigDecimal cost = daoUtil.getBigDecimal( 3 );
        stats.setTotalCost( cost != null ? cost.doubleValue( ) : 0.0 );
        stats.setAvgDurationSeconds( daoUtil.getDouble( 4 ) );
        stats.setErrorCount( daoUtil.getInt( 5 ) );
        return stats;
    }
}
