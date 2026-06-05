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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Home class for PlatformResourceNodeExecution objects
 */
public final class PlatformResourceNodeExecutionHome
{
    private static final IPlatformResourceNodeExecutionDAO _dao = CDI.current( ).select( IPlatformResourceNodeExecutionDAO.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PlatformResourceNodeExecutionHome( )
    {
    }

    /**
     * Creates a new node execution in the database
     *
     * @param nodeExecution
     *            The node execution object to create
     */
    public static void create( PlatformResourceNodeExecution nodeExecution )
    {
        if ( nodeExecution.getStartTime( ) == null )
        {
            nodeExecution.setStartTime( new Timestamp( System.currentTimeMillis( ) ) );
        }
        _dao.insert( nodeExecution, _plugin );
    }

    /**
     * Updates a node execution in the database
     *
     * @param nodeExecution
     *            The node execution object to update
     */
    public static void update( PlatformResourceNodeExecution nodeExecution )
    {
        _dao.update( nodeExecution, _plugin );
    }

    /**
     * Updates only the total_cost column of a node execution. Used by cost-refresh operations that must not race with concurrent completion calls writing
     * status / end_time.
     *
     * @param idNodeExecution
     *            The node execution ID
     * @param totalCost
     *            The new total cost
     */
    public static void updateCost( int idNodeExecution, BigDecimal totalCost )
    {
        _dao.updateCost( idNodeExecution, totalCost, _plugin );
    }

    /**
     * Retrieves all node executions for a given execution ID
     *
     * @param executionId
     *            The execution ID to search for
     * @return A list of node executions
     */
    public static List<PlatformResourceNodeExecution> getNodeExecutions( String executionId )
    {
        return _dao.getNodeExecutions( executionId, _plugin );
    }

    /**
     * Removes all node executions with the specified execution ID
     *
     * @param executionId
     *            The execution ID to remove
     */
    public static void removeByExecutionId( String executionId )
    {
        _dao.deleteByExecutionId( executionId, _plugin );
    }
}
