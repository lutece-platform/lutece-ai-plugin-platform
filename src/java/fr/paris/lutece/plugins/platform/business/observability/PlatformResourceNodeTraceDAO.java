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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Named( "platform.platformResourceNodeTraceDAO" )
public class PlatformResourceNodeTraceDAO implements IPlatformResourceNodeTraceDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_observability_resource_node_trace "
            + "(id_node_execution, message, data, timestamp, status, cost) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_QUERY_SELECT = "SELECT id_trace, id_node_execution, message, "
            + "data, timestamp, status, cost FROM platform_observability_resource_node_trace WHERE id_node_execution = ? " + "ORDER BY timestamp ASC";

    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_observability_resource_node_trace " + "WHERE id_node_execution = ?";

    @Override
    public void insert( PlatformResourceNodeTrace trace, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, trace.getIdNodeExecution( ) );
            daoUtil.setString( nIndex++, trace.getMessage( ) );
            daoUtil.setString( nIndex++, trace.getData( ) );
            daoUtil.setTimestamp( nIndex++, trace.getTimestamp( ) );
            daoUtil.setString( nIndex++, trace.getStatus( ).getValue( ) );
            daoUtil.setBigDecimal( nIndex, trace.getCost( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<PlatformResourceNodeTrace> getNodeTraces( int idNodeExecution, Plugin plugin )
    {
        List<PlatformResourceNodeTrace> traces = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, idNodeExecution );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                traces.add( map( daoUtil ) );
            }
        }
        return traces;
    }

    @Override
    public void deleteByNodeExecutionId( int idNodeExecution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, idNodeExecution );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Builds a {@link PlatformResourceNodeTrace} from the current row of the given DAOUtil.
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to read
     * @return the node trace mapped from the current row
     */
    private PlatformResourceNodeTrace map( DAOUtil daoUtil )
    {
        PlatformResourceNodeTrace trace = new PlatformResourceNodeTrace( );
        int nIndex = 1;
        trace.setId( daoUtil.getInt( nIndex++ ) );
        trace.setIdNodeExecution( daoUtil.getInt( nIndex++ ) );
        trace.setMessage( daoUtil.getString( nIndex++ ) );
        trace.setData( daoUtil.getString( nIndex++ ) );
        trace.setTimestamp( daoUtil.getTimestamp( nIndex++ ) );
        trace.setStatus( PlatformResourceNodeTraceStatus.fromString( daoUtil.getString( nIndex++ ) ) );
        trace.setCost( daoUtil.getBigDecimal( nIndex ) );
        return trace;
    }
}
