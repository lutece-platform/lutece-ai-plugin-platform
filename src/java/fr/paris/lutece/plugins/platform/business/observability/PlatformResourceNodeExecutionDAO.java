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
import java.util.ArrayList;
import java.util.List;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

@ApplicationScoped
@Named( "platform.platformResourceNodeExecutionDAO" )
public class PlatformResourceNodeExecutionDAO implements IPlatformResourceNodeExecutionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_observability_resource_node_execution "
            + "(execution_id, node_id, node_name, status, start_time, end_time, input_data, output_data, error_message, execution_order, total_cost) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_observability_resource_node_execution "
            + "SET status = ?, start_time = ?, end_time = ?, input_data = ?, output_data = ?, error_message = ?, total_cost = ? "
            + "WHERE id_node_execution = ?";
    private static final String SQL_QUERY_UPDATE_COST = "UPDATE platform_observability_resource_node_execution SET total_cost = ? WHERE id_node_execution = ?";
    private static final String SQL_QUERY_SELECT_BY_EXECUTION = "SELECT id_node_execution, execution_id, "
            + "node_id, node_name, status, start_time, end_time, input_data, output_data, error_message, execution_order, total_cost "
            + "FROM platform_observability_resource_node_execution WHERE execution_id = ? ORDER BY execution_order";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_observability_resource_node_execution WHERE execution_id = ?";

    @Override
    public void insert( PlatformResourceNodeExecution nodeExecution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, nodeExecution.getExecutionId( ) );
            daoUtil.setString( nIndex++, nodeExecution.getNodeId( ) );
            daoUtil.setString( nIndex++, nodeExecution.getNodeName( ) );
            daoUtil.setString( nIndex++, nodeExecution.getStatus( ).getValue( ) );
            daoUtil.setTimestamp( nIndex++, nodeExecution.getStartTime( ) );
            daoUtil.setTimestamp( nIndex++, nodeExecution.getEndTime( ) );
            daoUtil.setString( nIndex++, nodeExecution.getInputData( ) );
            daoUtil.setString( nIndex++, nodeExecution.getOutputData( ) );
            daoUtil.setString( nIndex++, nodeExecution.getErrorMessage( ) );
            daoUtil.setInt( nIndex++, nodeExecution.getExecutionOrder( ) );
            daoUtil.setBigDecimal( nIndex, nodeExecution.getTotalCost( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                nodeExecution.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public void update( PlatformResourceNodeExecution nodeExecution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, nodeExecution.getStatus( ).getValue( ) );
            daoUtil.setTimestamp( nIndex++, nodeExecution.getStartTime( ) );
            daoUtil.setTimestamp( nIndex++, nodeExecution.getEndTime( ) );
            daoUtil.setString( nIndex++, nodeExecution.getInputData( ) );
            daoUtil.setString( nIndex++, nodeExecution.getOutputData( ) );
            daoUtil.setString( nIndex++, nodeExecution.getErrorMessage( ) );
            daoUtil.setBigDecimal( nIndex++, nodeExecution.getTotalCost( ) );
            daoUtil.setInt( nIndex, nodeExecution.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Updates only the total_cost column to avoid overwriting status / end_time set by concurrent completion calls.
     *
     * @param idNodeExecution
     *            The node execution ID
     * @param totalCost
     *            The new total cost
     * @param plugin
     *            The plugin
     */
    @Override
    public void updateCost( int idNodeExecution, BigDecimal totalCost, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE_COST, plugin ) )
        {
            daoUtil.setBigDecimal( 1, totalCost );
            daoUtil.setInt( 2, idNodeExecution );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<PlatformResourceNodeExecution> getNodeExecutions( String executionId, Plugin plugin )
    {
        List<PlatformResourceNodeExecution> nodeExecutions = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_EXECUTION, plugin ) )
        {
            daoUtil.setString( 1, executionId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                nodeExecutions.add( map( daoUtil ) );
            }
        }
        return nodeExecutions;
    }

    @Override
    public void deleteByExecutionId( String executionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setString( 1, executionId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Maps the current row of a DAOUtil result set to a PlatformResourceNodeExecution.
     *
     * @param daoUtil
     *            the DAOUtil positioned on a result row
     * @return the mapped node execution
     */
    private PlatformResourceNodeExecution map( DAOUtil daoUtil )
    {
        PlatformResourceNodeExecution nodeExecution = new PlatformResourceNodeExecution( );
        int nIndex = 1;
        nodeExecution.setId( daoUtil.getInt( nIndex++ ) );
        nodeExecution.setExecutionId( daoUtil.getString( nIndex++ ) );
        nodeExecution.setNodeId( daoUtil.getString( nIndex++ ) );
        nodeExecution.setNodeName( daoUtil.getString( nIndex++ ) );
        nodeExecution.setStatus( PlatformResourceExecutionStatus.fromString( daoUtil.getString( nIndex++ ) ) );
        nodeExecution.setStartTime( daoUtil.getTimestamp( nIndex++ ) );
        nodeExecution.setEndTime( daoUtil.getTimestamp( nIndex++ ) );
        nodeExecution.setInputData( daoUtil.getString( nIndex++ ) );
        nodeExecution.setOutputData( daoUtil.getString( nIndex++ ) );
        nodeExecution.setErrorMessage( daoUtil.getString( nIndex++ ) );
        nodeExecution.setExecutionOrder( daoUtil.getInt( nIndex++ ) );
        nodeExecution.setTotalCost( daoUtil.getBigDecimal( nIndex ) );
        return nodeExecution;
    }
}
