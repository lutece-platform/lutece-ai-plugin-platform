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
import java.util.List;

import fr.paris.lutece.portal.service.plugin.Plugin;

public interface IPlatformResourceNodeExecutionDAO
{
    /**
     * Insert a new platform resource node execution
     *
     * @param nodeExecution
     *            The node execution to insert
     * @param plugin
     *            The plugin
     */
    void insert( PlatformResourceNodeExecution nodeExecution, Plugin plugin );

    /**
     * Update an existing platform resource node execution
     *
     * @param nodeExecution
     *            The node execution to update
     * @param plugin
     *            The plugin
     */
    void update( PlatformResourceNodeExecution nodeExecution, Plugin plugin );

    /**
     * Updates only the total_cost column of a node execution. Used by cost-refresh operations (after trace addition) that must not overwrite status / end_time
     * set by concurrent completion calls.
     *
     * @param idNodeExecution
     *            The node execution ID
     * @param totalCost
     *            The new total cost
     * @param plugin
     *            The plugin
     */
    void updateCost( int idNodeExecution, BigDecimal totalCost, Plugin plugin );

    /**
     * Get all node executions for a given execution ID
     *
     * @param executionId
     *            The execution ID
     * @param plugin
     *            The plugin
     * @return The list of node executions
     */
    List<PlatformResourceNodeExecution> getNodeExecutions( String executionId, Plugin plugin );

    /**
     * Delete all node executions by execution ID
     *
     * @param executionId
     *            The execution ID
     * @param plugin
     *            The plugin
     */
    void deleteByExecutionId( String executionId, Plugin plugin );
}
