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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.sql.DAOUtil;
import fr.paris.lutece.util.sql.TransactionManager;

@ApplicationScoped
@Named( "platform.pipelineExecutionDAO" )
public class PipelineExecutionDAO implements IPipelineExecutionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_pipeline_execution ( id_pipeline, id_client, execution_id, date_creation, date_completion, status, inputs, outputs, error, user_id ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_pipeline_execution WHERE id_execution = ? ";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_pipeline_execution SET id_pipeline = ?, id_client = ?, execution_id = ?, date_creation = ?, date_completion = ?, status = ?, inputs = ?, outputs = ?, error = ?, user_id = ? WHERE id_execution = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT id_execution, id_pipeline, id_client, execution_id, date_creation, date_completion, status, inputs, outputs, error, user_id FROM platform_pipeline_execution";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_execution = ?";
    private static final String SQL_QUERY_SELECT_BY_EXECUTION_ID = SQL_QUERY_SELECTALL + " WHERE execution_id = ?";
    private static final String SQL_QUERY_SELECT_BY_PIPELINE_ID = SQL_QUERY_SELECTALL + " WHERE id_pipeline = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE id_client = ?";
    private static final String SQL_QUERY_SELECT_BY_PIPELINE_ID_DATE_RANGE = SQL_QUERY_SELECTALL
            + " WHERE id_pipeline = ? AND date_creation BETWEEN ? AND ? ORDER BY date_creation ASC";
    private static final String SQL_QUERY_SELECT_BY_USER_AND_PIPELINE = SQL_QUERY_SELECTALL
            + " WHERE user_id = ? AND id_pipeline = ? ORDER BY date_creation DESC";
    private static final String SQL_QUERY_LOCK_PIPELINE = "SELECT id_pipeline FROM platform_pipeline WHERE id_pipeline = ? FOR UPDATE";
    private static final String SQL_QUERY_STATUS_BY_EXECUTION_ID = "SELECT status FROM platform_pipeline_execution WHERE execution_id = ?";
    private static final String SQL_QUERY_COUNT_BY_STATUS = "SELECT COUNT(*) FROM platform_pipeline_execution WHERE status = ?";
    private static final String SQL_QUERY_COUNT_BY_STATUS_PIPELINE = "SELECT COUNT(*) FROM platform_pipeline_execution WHERE status = ? AND id_pipeline = ?";
    private static final String SQL_QUERY_ACTIVATE = "UPDATE platform_pipeline_execution SET status = ? WHERE execution_id = ? AND status = ?";
    private static final String SQL_QUERY_FAIL_ORPHANED = "UPDATE platform_pipeline_execution SET status = ?, error = ?, date_completion = CURRENT_TIMESTAMP WHERE status IN ( ?, ? )";
    private static final String MSG_ORPHANED = "Execution interrupted by an application restart";
    private static final String LOG_ACTIVATE_ERROR = "[PipelineExecutionDAO] Failed to activate execution {}";

    @Override
    public int insert( PipelineExecution execution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, execution.getIdPipeline( ) );
            daoUtil.setInt( nIndex++, execution.getIdClient( ) );
            daoUtil.setString( nIndex++, execution.getExecutionId( ) );
            daoUtil.setTimestamp( nIndex++, execution.getCreationDate( ) );
            daoUtil.setTimestamp( nIndex++, execution.getCompletionDate( ) );
            daoUtil.setString( nIndex++, execution.getStatus( ) );
            daoUtil.setString( nIndex++, execution.getInputs( ) );
            daoUtil.setString( nIndex++, execution.getOutputs( ) );
            daoUtil.setString( nIndex++, execution.getError( ) );
            daoUtil.setString( nIndex++, execution.getUserId( ) );
            daoUtil.executeUpdate( );

            int generatedKey = 0;
            if ( daoUtil.nextGeneratedKey( ) )
            {
                generatedKey = daoUtil.getGeneratedKeyInt( 1 );
                execution.setId( generatedKey );
            }

            return generatedKey;
        }
    }

    @Override
    public void store( PipelineExecution execution, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, execution.getIdPipeline( ) );
            daoUtil.setInt( nIndex++, execution.getIdClient( ) );
            daoUtil.setString( nIndex++, execution.getExecutionId( ) );
            daoUtil.setTimestamp( nIndex++, execution.getCreationDate( ) );
            daoUtil.setTimestamp( nIndex++, execution.getCompletionDate( ) );
            daoUtil.setString( nIndex++, execution.getStatus( ) );
            daoUtil.setString( nIndex++, execution.getInputs( ) );
            daoUtil.setString( nIndex++, execution.getOutputs( ) );
            daoUtil.setString( nIndex++, execution.getError( ) );
            daoUtil.setString( nIndex++, execution.getUserId( ) );
            daoUtil.setInt( nIndex, execution.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void delete( int nExecutionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nExecutionId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public Optional<PipelineExecution> load( int nExecutionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nExecutionId );
            daoUtil.executeQuery( );
            PipelineExecution execution = null;
            if ( daoUtil.next( ) )
            {
                execution = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( execution );
        }
    }

    @Override
    public Optional<PipelineExecution> loadByExecutionId( String strExecutionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_EXECUTION_ID, plugin ) )
        {
            daoUtil.setString( 1, strExecutionId );
            daoUtil.executeQuery( );
            PipelineExecution execution = null;
            if ( daoUtil.next( ) )
            {
                execution = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( execution );
        }
    }

    @Override
    public List<PipelineExecution> selectAll( Plugin plugin )
    {
        List<PipelineExecution> executionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return executionList;
        }
    }

    @Override
    public List<PipelineExecution> selectByPipelineId( int nPipelineId, Plugin plugin )
    {
        List<PipelineExecution> executionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_PIPELINE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return executionList;
        }
    }

    @Override
    public List<PipelineExecution> selectByClientId( int nClientId, Plugin plugin )
    {
        List<PipelineExecution> executionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nClientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return executionList;
        }
    }

    @Override
    public List<PipelineExecution> selectByPipelineIdAndDateRange( int nPipelineId, Timestamp startDate, Timestamp endDate, Plugin plugin )
    {
        List<PipelineExecution> executionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_PIPELINE_ID_DATE_RANGE, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.setTimestamp( 2, startDate );
            daoUtil.setTimestamp( 3, endDate );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                executionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return executionList;
        }
    }

    @Override
    public List<PipelineExecution> selectByUserIdAndPipelineId( String strUserId, int nPipelineId, Plugin plugin )
    {
        List<PipelineExecution> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_USER_AND_PIPELINE, plugin ) )
        {
            daoUtil.setString( 1, strUserId );
            daoUtil.setInt( 2, nPipelineId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * Builds a pipeline execution from the current row of the given DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil positioned on the current row
     * @return The pipeline execution built from the current row
     */
    private PipelineExecution loadFromDaoUtil( DAOUtil daoUtil )
    {
        PipelineExecution execution = new PipelineExecution( );
        int nIndex = 1;
        execution.setId( daoUtil.getInt( nIndex++ ) );
        execution.setIdPipeline( daoUtil.getInt( nIndex++ ) );
        execution.setIdClient( daoUtil.getInt( nIndex++ ) );
        execution.setExecutionId( daoUtil.getString( nIndex++ ) );
        execution.setCreationDate( daoUtil.getTimestamp( nIndex++ ) );
        execution.setCompletionDate( daoUtil.getTimestamp( nIndex++ ) );
        execution.setStatus( daoUtil.getString( nIndex++ ) );
        execution.setInputs( daoUtil.getString( nIndex++ ) );
        execution.setOutputs( daoUtil.getString( nIndex++ ) );
        execution.setError( daoUtil.getString( nIndex++ ) );
        execution.setUserId( daoUtil.getString( nIndex ) );
        return execution;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countByStatusAndPipeline( String strStatus, int nPipelineId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_COUNT_BY_STATUS_PIPELINE, plugin ) )
        {
            daoUtil.setString( 1, strStatus );
            daoUtil.setInt( 2, nPipelineId );
            daoUtil.executeQuery( );
            return daoUtil.next( ) ? daoUtil.getInt( 1 ) : 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryActivate( String strExecutionId, int nPipelineId, int nMaxWorkers, int nMaxActiveGlobal, Plugin plugin )
    {
        boolean bActivated = false;
        TransactionManager.beginTransaction( plugin );
        try
        {
            try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_LOCK_PIPELINE, plugin ) )
            {
                daoUtil.setInt( 1, nPipelineId );
                daoUtil.executeQuery( );
                daoUtil.next( );
            }

            boolean bPending;
            try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_STATUS_BY_EXECUTION_ID, plugin ) )
            {
                daoUtil.setString( 1, strExecutionId );
                daoUtil.executeQuery( );
                bPending = daoUtil.next( ) && PipelineExecution.STATUS_PENDING.equals( daoUtil.getString( 1 ) );
            }

            int nGlobalRunning = countByStatus( PipelineExecution.STATUS_RUNNING, plugin );
            int nPipelineRunning = countByStatusAndPipeline( PipelineExecution.STATUS_RUNNING, nPipelineId, plugin );

            if ( bPending && nGlobalRunning < nMaxActiveGlobal && nPipelineRunning < nMaxWorkers )
            {
                try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_ACTIVATE, plugin ) )
                {
                    daoUtil.setString( 1, PipelineExecution.STATUS_RUNNING );
                    daoUtil.setString( 2, strExecutionId );
                    daoUtil.setString( 3, PipelineExecution.STATUS_PENDING );
                    daoUtil.executeUpdate( );
                }
                bActivated = true;
            }
            TransactionManager.commitTransaction( plugin );
        }
        catch( Exception e )
        {
            TransactionManager.rollBack( plugin );
            AppLogService.error( LOG_ACTIVATE_ERROR, strExecutionId, e );
            bActivated = false;
        }
        return bActivated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void failOrphanedExecutions( Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FAIL_ORPHANED, plugin ) )
        {
            daoUtil.setString( 1, PipelineExecution.STATUS_FAILED );
            daoUtil.setString( 2, MSG_ORPHANED );
            daoUtil.setString( 3, PipelineExecution.STATUS_PENDING );
            daoUtil.setString( 4, PipelineExecution.STATUS_RUNNING );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Counts executions in a given status across all pipelines.
     *
     * @param strStatus
     *            the status
     * @param plugin
     *            the plugin
     * @return the count
     */
    @Override
    public int countByStatus( String strStatus, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_COUNT_BY_STATUS, plugin ) )
        {
            daoUtil.setString( 1, strStatus );
            daoUtil.executeQuery( );
            return daoUtil.next( ) ? daoUtil.getInt( 1 ) : 0;
        }
    }
}
