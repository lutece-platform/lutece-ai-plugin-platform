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

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Home class for DatasetDocumentJob objects
 */
public final class DatasetDocumentJobHome
{
    private static IDatasetDocumentJobDAO _dao = CDI.current( ).select( IDatasetDocumentJobDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private DatasetDocumentJobHome( )
    {
    }

    /**
     * Create a new dataset document job
     *
     * @param job
     *            The job to create
     * @return The created job
     */
    public static DatasetDocumentJob create( DatasetDocumentJob job )
    {
        _dao.insert( job, _plugin );
        return job;
    }

    /**
     * Update a dataset document job
     *
     * @param job
     *            The job to update
     * @return The updated job
     */
    public static DatasetDocumentJob update( DatasetDocumentJob job )
    {
        _dao.store( job, _plugin );
        return job;
    }

    /**
     * Remove a dataset document job
     *
     * @param nKey
     *            The job primary key
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Find a dataset document job by primary key
     *
     * @param nKey
     *            The job primary key
     * @return The job if found, empty otherwise
     */
    public static Optional<DatasetDocumentJob> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Get all dataset document jobs
     *
     * @return The list of all jobs
     */
    public static List<DatasetDocumentJob> getJobsList( )
    {
        return _dao.selectJobsList( _plugin );
    }

    /**
     * Get all jobs associated with a dataset
     *
     * @param nDatasetId
     *            The dataset ID
     * @return The list of jobs for the dataset
     */
    public static List<DatasetDocumentJob> getJobsByDatasetId( int nDatasetId )
    {
        return _dao.selectJobsByDatasetId( nDatasetId, _plugin );
    }

    /**
     * Get all jobs with a specific status
     *
     * @param strStatus
     *            The status
     * @return The list of jobs with the status
     */
    public static List<DatasetDocumentJob> getJobsByStatus( String strStatus )
    {
        return _dao.selectJobsByStatus( strStatus, _plugin );
    }

    /**
     * Atomically claims a pending job (pending → processing, attempts + 1) using a row-locking transaction.
     *
     * @param nJobId
     *            the job identifier
     * @return {@code true} if the caller won the claim, {@code false} otherwise
     */
    public static boolean claim( int nJobId )
    {
        return _dao.claim( nJobId, _plugin );
    }

    /**
     * Updates a job final status and error message.
     *
     * @param nJobId
     *            the job identifier
     * @param strStatus
     *            the new status
     * @param strErrorMessage
     *            the error message, or {@code null}
     */
    public static void updateStatus( int nJobId, String strStatus, String strErrorMessage )
    {
        _dao.updateStatus( nJobId, strStatus, strErrorMessage, _plugin );
    }

    /**
     * Recovers jobs stuck in {@code processing} past the timeout (re-queue or mark error).
     *
     * @param lTimeoutMs
     *            inactivity timeout in milliseconds
     * @param nMaxAttempts
     *            maximum attempts before giving up
     */
    public static void requeueStaleJobs( long lTimeoutMs, int nMaxAttempts )
    {
        _dao.requeueStaleJobs( lTimeoutMs, nMaxAttempts, _plugin );
    }

    /**
     * Remove all jobs associated with a dataset
     *
     * @param nDatasetId
     *            The dataset ID
     */
    public static void removeByDatasetId( int nDatasetId )
    {
        _dao.deleteByDatasetId( nDatasetId, _plugin );
    }
}
