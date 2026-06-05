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
package fr.paris.lutece.plugins.platform.business.dataset;

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

/**
 * Implementation of the DatasetDocumentJobDAO interface
 */
@ApplicationScoped
@Named( "platform.datasetDocumentJobDAO" )
public class DatasetDocumentJobDAO implements IDatasetDocumentJobDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_document_job (document_id, dataset_id, status, error_message) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_document_job WHERE job_id = ?";
    private static final String SQL_QUERY_DELETE_BY_DATASET = "DELETE FROM platform_document_job WHERE dataset_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_document_job SET document_id = ?, dataset_id = ?, status = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP WHERE job_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT job_id, document_id, dataset_id, status, error_message, created_at, updated_at FROM platform_document_job";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE job_id = ?";
    private static final String SQL_QUERY_SELECT_BY_DATASET_ID = SQL_QUERY_SELECTALL + " WHERE dataset_id = ?";
    private static final String SQL_QUERY_SELECT_BY_STATUS = SQL_QUERY_SELECTALL + " WHERE status = ?";
    private static final String SQL_QUERY_CLAIM_SELECT = "SELECT status FROM platform_document_job WHERE job_id = ? FOR UPDATE";
    private static final String SQL_QUERY_CLAIM_UPDATE = "UPDATE platform_document_job SET status = ?, attempts = attempts + 1 WHERE job_id = ?";
    private static final String SQL_QUERY_UPDATE_STATUS = "UPDATE platform_document_job SET status = ?, error_message = ? WHERE job_id = ?";
    private static final String SQL_QUERY_REQUEUE_STALE = "UPDATE platform_document_job SET status = ? WHERE status = ? AND attempts < ? AND updated_at < ?";
    private static final String SQL_QUERY_FAIL_STALE = "UPDATE platform_document_job SET status = ?, error_message = ? WHERE status = ? AND attempts >= ? AND updated_at < ?";
    private static final String MSG_STALE_ABANDONED = "Abandoned after reaching max attempts (stale processing job recovered)";
    private static final String LOG_CLAIM_ERROR = "[DatasetDocumentJobDAO] Failed to claim job {}";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DatasetDocumentJob job, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, job.getDocumentId( ) );
            daoUtil.setInt( nIndex++, job.getDatasetId( ) );
            daoUtil.setString( nIndex++, job.getStatus( ) );
            daoUtil.setString( nIndex, job.getErrorMessage( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                job.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DatasetDocumentJob> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            DatasetDocumentJob job = null;
            if ( daoUtil.next( ) )
            {
                job = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( job );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByDatasetId( int nDatasetId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_DATASET, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( DatasetDocumentJob job, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, job.getDocumentId( ) );
            daoUtil.setInt( nIndex++, job.getDatasetId( ) );
            daoUtil.setString( nIndex++, job.getStatus( ) );
            daoUtil.setString( nIndex++, job.getErrorMessage( ) );
            daoUtil.setInt( nIndex, job.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocumentJob> selectJobsList( Plugin plugin )
    {
        List<DatasetDocumentJob> jobsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                jobsList.add( loadFromDaoUtil( daoUtil ) );
            }
            return jobsList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocumentJob> selectJobsByDatasetId( int nDatasetId, Plugin plugin )
    {
        List<DatasetDocumentJob> jobsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_DATASET_ID, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                jobsList.add( loadFromDaoUtil( daoUtil ) );
            }
            return jobsList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DatasetDocumentJob> selectJobsByStatus( String strStatus, Plugin plugin )
    {
        List<DatasetDocumentJob> jobsList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_STATUS, plugin ) )
        {
            daoUtil.setString( 1, strStatus );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                jobsList.add( loadFromDaoUtil( daoUtil ) );
            }
            return jobsList;
        }
    }

    /**
     * Load a DatasetDocumentJob from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil containing the data
     * @return The DatasetDocumentJob
     */
    private DatasetDocumentJob loadFromDaoUtil( DAOUtil daoUtil )
    {
        DatasetDocumentJob job = new DatasetDocumentJob( );
        int nIndex = 1;
        job.setId( daoUtil.getInt( nIndex++ ) );
        job.setDocumentId( daoUtil.getInt( nIndex++ ) );
        job.setDatasetId( daoUtil.getInt( nIndex++ ) );
        job.setStatus( daoUtil.getString( nIndex++ ) );
        job.setErrorMessage( daoUtil.getString( nIndex++ ) );
        job.setCreated( daoUtil.getTimestamp( nIndex++ ) );
        job.setUpdated( daoUtil.getTimestamp( nIndex ) );
        return job;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean claim( int nJobId, Plugin plugin )
    {
        boolean bClaimed = false;
        TransactionManager.beginTransaction( plugin );
        try
        {
            try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_CLAIM_SELECT, plugin ) )
            {
                daoUtil.setInt( 1, nJobId );
                daoUtil.executeQuery( );
                bClaimed = daoUtil.next( ) && DatasetDocumentJob.STATUS_PENDING.equals( daoUtil.getString( 1 ) );
            }
            if ( bClaimed )
            {
                try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_CLAIM_UPDATE, plugin ) )
                {
                    daoUtil.setString( 1, DatasetDocumentJob.STATUS_PROCESSING );
                    daoUtil.setInt( 2, nJobId );
                    daoUtil.executeUpdate( );
                }
            }
            TransactionManager.commitTransaction( plugin );
        }
        catch( Exception e )
        {
            TransactionManager.rollBack( plugin );
            AppLogService.error( LOG_CLAIM_ERROR, nJobId, e );
            bClaimed = false;
        }
        return bClaimed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus( int nJobId, String strStatus, String strErrorMessage, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE_STATUS, plugin ) )
        {
            daoUtil.setString( 1, strStatus );
            daoUtil.setString( 2, strErrorMessage );
            daoUtil.setInt( 3, nJobId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requeueStaleJobs( long lTimeoutMs, int nMaxAttempts, Plugin plugin )
    {
        Timestamp threshold = new Timestamp( System.currentTimeMillis( ) - lTimeoutMs );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_REQUEUE_STALE, plugin ) )
        {
            daoUtil.setString( 1, DatasetDocumentJob.STATUS_PENDING );
            daoUtil.setString( 2, DatasetDocumentJob.STATUS_PROCESSING );
            daoUtil.setInt( 3, nMaxAttempts );
            daoUtil.setTimestamp( 4, threshold );
            daoUtil.executeUpdate( );
        }
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FAIL_STALE, plugin ) )
        {
            daoUtil.setString( 1, DatasetDocumentJob.STATUS_ERROR );
            daoUtil.setString( 2, MSG_STALE_ABANDONED );
            daoUtil.setString( 3, DatasetDocumentJob.STATUS_PROCESSING );
            daoUtil.setInt( 4, nMaxAttempts );
            daoUtil.setTimestamp( 5, threshold );
            daoUtil.executeUpdate( );
        }
    }
}
