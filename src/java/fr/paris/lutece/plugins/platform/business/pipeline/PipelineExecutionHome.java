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
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * PipelineExecution DAO Home class
 */
public final class PipelineExecutionHome
{
    private static IPipelineExecutionDAO _dao = CDI.current( ).select( IPipelineExecutionDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PipelineExecutionHome( )
    {
    }

    /**
     * Create a new execution
     *
     * @param execution
     *            The execution
     * @return The execution ID
     */
    public static int create( PipelineExecution execution )
    {
        return _dao.insert( execution, _plugin );
    }

    /**
     * Update an execution
     *
     * @param execution
     *            The execution
     * @return The execution
     */
    public static PipelineExecution update( PipelineExecution execution )
    {
        _dao.store( execution, _plugin );
        return execution;
    }

    /**
     * Delete an execution
     *
     * @param nExecutionId
     *            The execution ID
     */
    public static void remove( int nExecutionId )
    {
        _dao.delete( nExecutionId, _plugin );
    }

    /**
     * Counts executions in a given status for a given pipeline.
     *
     * @param strStatus
     *            the status
     * @param nPipelineId
     *            the pipeline identifier
     * @return the count
     */
    public static int countByStatusAndPipeline( String strStatus, int nPipelineId )
    {
        return _dao.countByStatusAndPipeline( strStatus, nPipelineId, _plugin );
    }

    /**
     * Counts executions in a given status across all pipelines.
     *
     * @param strStatus
     *            the status
     * @return the count
     */
    public static int countByStatus( String strStatus )
    {
        return _dao.countByStatus( strStatus, _plugin );
    }

    /**
     * Atomically activates a pending execution when concurrency limits allow it.
     *
     * @param strExecutionId
     *            the execution identifier
     * @param nPipelineId
     *            the pipeline identifier
     * @param nMaxWorkers
     *            the per-pipeline concurrency limit
     * @param nMaxActiveGlobal
     *            the global concurrency limit
     * @return {@code true} if activated (moved to RUNNING)
     */
    public static boolean tryActivate( String strExecutionId, int nPipelineId, int nMaxWorkers, int nMaxActiveGlobal )
    {
        return _dao.tryActivate( strExecutionId, nPipelineId, nMaxWorkers, nMaxActiveGlobal, _plugin );
    }

    /**
     * Marks orphaned PENDING/RUNNING executions as FAILED at startup.
     */
    public static void failOrphanedExecutions( )
    {
        _dao.failOrphanedExecutions( _plugin );
    }

    /**
     * Find an execution by ID
     *
     * @param nExecutionId
     *            The execution ID
     * @return The execution if found, empty otherwise
     */
    public static Optional<PipelineExecution> findByPrimaryKey( int nExecutionId )
    {
        return _dao.load( nExecutionId, _plugin );
    }

    /**
     * Find an execution by execution ID string
     *
     * @param strExecutionId
     *            The execution ID string
     * @return The execution if found, empty otherwise
     */
    public static Optional<PipelineExecution> findByExecutionId( String strExecutionId )
    {
        return _dao.loadByExecutionId( strExecutionId, _plugin );
    }

    /**
     * Find all executions
     *
     * @return The list of executions
     */
    public static List<PipelineExecution> findAll( )
    {
        return _dao.selectAll( _plugin );
    }

    /**
     * Find executions by pipeline ID
     *
     * @param nPipelineId
     *            The pipeline ID
     * @return The list of executions
     */
    public static List<PipelineExecution> findByPipelineId( int nPipelineId )
    {
        return _dao.selectByPipelineId( nPipelineId, _plugin );
    }

    /**
     * Find executions by client ID
     *
     * @param nClientId
     *            The client ID
     * @return The list of executions
     */
    public static List<PipelineExecution> findByClientId( int nClientId )
    {
        return _dao.selectByClientId( nClientId, _plugin );
    }

    /**
     * Finds executions by user ID and pipeline ID.
     *
     * @param strUserId
     *            the user identifier
     * @param nPipelineId
     *            the pipeline identifier
     * @return list of matching executions
     */
    public static List<PipelineExecution> findByUserIdAndPipelineId( String strUserId, int nPipelineId )
    {
        return _dao.selectByUserIdAndPipelineId( strUserId, nPipelineId, _plugin );
    }

    /**
     * Find executions for a pipeline within a date range
     *
     * @param nPipelineId
     *            The pipeline ID
     * @param startDate
     *            The start date
     * @param endDate
     *            The end date
     * @return List of executions in the specified date range
     */
    public static List<PipelineExecution> findByPipelineIdAndDateRange( int nPipelineId, Timestamp startDate, Timestamp endDate )
    {
        return _dao.selectByPipelineIdAndDateRange( nPipelineId, startDate, endDate, _plugin );
    }
}
