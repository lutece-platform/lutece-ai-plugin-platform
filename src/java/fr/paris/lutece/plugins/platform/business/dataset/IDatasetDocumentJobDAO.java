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

/**
 * Interface for dataset document job DAO
 */
public interface IDatasetDocumentJobDAO
{
    /**
     * Insert a new job in the database
     *
     * @param job
     *            The job object to insert
     * @param plugin
     *            The plugin
     */
    void insert( DatasetDocumentJob job, Plugin plugin );

    /**
     * Load a job by its primary key
     *
     * @param nKey
     *            The job primary key
     * @param plugin
     *            The plugin
     * @return The job if found, empty otherwise
     */
    Optional<DatasetDocumentJob> load( int nKey, Plugin plugin );

    /**
     * Delete a job from the database
     *
     * @param nKey
     *            The job primary key
     * @param plugin
     *            The plugin
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Delete all jobs associated with a dataset
     *
     * @param nDatasetId
     *            The dataset ID
     * @param plugin
     *            The plugin
     */
    void deleteByDatasetId( int nDatasetId, Plugin plugin );

    /**
     * Update a job in the database
     *
     * @param job
     *            The job to update
     * @param plugin
     *            The plugin
     */
    void store( DatasetDocumentJob job, Plugin plugin );

    /**
     * Get all jobs
     *
     * @param plugin
     *            The plugin
     * @return The list of all jobs
     */
    List<DatasetDocumentJob> selectJobsList( Plugin plugin );

    /**
     * Get all jobs associated with a dataset
     *
     * @param nDatasetId
     *            The dataset ID
     * @param plugin
     *            The plugin
     * @return The list of jobs for the dataset
     */
    List<DatasetDocumentJob> selectJobsByDatasetId( int nDatasetId, Plugin plugin );

    /**
     * Get all jobs with a specific status
     *
     * @param strStatus
     *            The status
     * @param plugin
     *            The plugin
     * @return The list of jobs with the status
     */
    List<DatasetDocumentJob> selectJobsByStatus( String strStatus, Plugin plugin );

    /**
     * Moves a pending job to {@code processing} and increments its attempts counter, atomically under a row lock.
     *
     * @param nJobId
     *            the job identifier to claim
     * @param plugin
     *            the plugin
     * @return {@code true} if the job was pending and is now claimed by this caller
     */
    boolean claim( int nJobId, Plugin plugin );

    /**
     * Updates a job final status and error message in a single statement.
     *
     * @param nJobId
     *            the job identifier
     * @param strStatus
     *            the new status
     * @param strErrorMessage
     *            the error message, or {@code null}
     * @param plugin
     *            the plugin
     */
    void updateStatus( int nJobId, String strStatus, String strErrorMessage, Plugin plugin );

    /**
     * Re-queues jobs stuck in {@code processing} past the timeout to {@code pending}, or marks them {@code error} once no attempt remains.
     *
     * @param lTimeoutMs
     *            inactivity timeout in milliseconds since {@code updated_at}
     * @param nMaxAttempts
     *            maximum number of attempts before giving up
     * @param plugin
     *            the plugin
     */
    void requeueStaleJobs( long lTimeoutMs, int nMaxAttempts, Plugin plugin );
}
