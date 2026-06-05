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
package fr.paris.lutece.plugins.platform.business.pipeline;

import fr.paris.lutece.portal.service.plugin.Plugin;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * PipelineExecution DAO interface
 */
public interface IPipelineExecutionDAO
{
    /**
     * Insert a new execution
     *
     * @param execution
     *            The execution object
     * @param plugin
     *            The plugin
     * @return The newly created execution ID
     */
    int insert( PipelineExecution execution, Plugin plugin );

    /**
     * Counts executions in a given status for a given pipeline.
     *
     * @param strStatus
     *            the status
     * @param nPipelineId
     *            the pipeline identifier
     * @param plugin
     *            the plugin
     * @return the count
     */
    int countByStatusAndPipeline( String strStatus, int nPipelineId, Plugin plugin );

    /**
     * Counts executions in a given status across all pipelines.
     *
     * @param strStatus
     *            the status
     * @param plugin
     *            the plugin
     * @return the count
     */
    int countByStatus( String strStatus, Plugin plugin );

    /**
     * Moves a pending execution to {@code RUNNING} when the global and per-pipeline concurrency limits allow it, atomically under a row lock on the parent
     * pipeline.
     *
     * @param strExecutionId
     *            the execution identifier
     * @param nPipelineId
     *            the pipeline identifier
     * @param nMaxWorkers
     *            the per-pipeline maximum of concurrent running executions
     * @param nMaxActiveGlobal
     *            the global maximum of concurrent running executions
     * @param plugin
     *            the plugin
     * @return {@code true} if the execution was moved to {@code RUNNING}
     */
    boolean tryActivate( String strExecutionId, int nPipelineId, int nMaxWorkers, int nMaxActiveGlobal, Plugin plugin );

    /**
     * Marks every {@code PENDING}/{@code RUNNING} execution as {@code FAILED}.
     *
     * @param plugin
     *            the plugin
     */
    void failOrphanedExecutions( Plugin plugin );

    /**
     * Update an execution
     *
     * @param execution
     *            The execution
     * @param plugin
     *            The plugin
     */
    void store( PipelineExecution execution, Plugin plugin );

    /**
     * Delete an execution
     *
     * @param nExecutionId
     *            The execution ID
     * @param plugin
     *            The plugin
     */
    void delete( int nExecutionId, Plugin plugin );

    /**
     * Load an execution by ID
     *
     * @param nExecutionId
     *            The execution ID
     * @param plugin
     *            The plugin
     * @return The execution if found, empty otherwise
     */
    Optional<PipelineExecution> load( int nExecutionId, Plugin plugin );

    /**
     * Load an execution by execution ID string
     *
     * @param strExecutionId
     *            The execution ID string
     * @param plugin
     *            The plugin
     * @return The execution if found, empty otherwise
     */
    Optional<PipelineExecution> loadByExecutionId( String strExecutionId, Plugin plugin );

    /**
     * Load all executions
     *
     * @param plugin
     *            The plugin
     * @return The list of executions
     */
    List<PipelineExecution> selectAll( Plugin plugin );

    /**
     * Load executions by pipeline ID
     *
     * @param nPipelineId
     *            The pipeline ID
     * @param plugin
     *            The plugin
     * @return The list of executions
     */
    List<PipelineExecution> selectByPipelineId( int nPipelineId, Plugin plugin );

    /**
     * Load executions by client ID
     *
     * @param nClientId
     *            The client ID
     * @param plugin
     *            The plugin
     * @return The list of executions
     */
    List<PipelineExecution> selectByClientId( int nClientId, Plugin plugin );

    List<PipelineExecution> selectByPipelineIdAndDateRange( int nPipelineId, Timestamp startDate, Timestamp endDate, Plugin plugin );

    /**
     * Selects executions by user ID and pipeline ID.
     *
     * @param strUserId
     *            the user identifier
     * @param nPipelineId
     *            the pipeline identifier
     * @param plugin
     *            the plugin
     * @return list of matching executions, ordered by creation date desc
     */
    List<PipelineExecution> selectByUserIdAndPipelineId( String strUserId, int nPipelineId, Plugin plugin );

}
