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
package fr.paris.lutece.plugins.platform.service.pipeline;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecutionHome;
import fr.paris.lutece.plugins.platform.service.concurrent.Scheduler;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineJobActivatedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineJobQueuedEvent;
import jakarta.annotation.PreDestroy;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

/**
 * Manager for pipeline jobs handling creation, activation, monitoring and cleanup operations. Concurrency limits (global and per-pipeline) are enforced
 * atomically in the database through {@code SELECT ... FOR UPDATE} on the parent pipeline row, so the limits hold across application instances sharing the same
 * database. The execution itself (the {@link CompletableFuture}) lives in this JVM, hence the in-memory job registry.
 */
@ApplicationScoped
public class PipelineJobManager
{

    private static final long CLEANUP_INTERVAL_MS = 600_000L;
    private static final long JOB_RETENTION_TIME_MS = 86_400_000L;
    private static final String PROPERTY_MAX_ACTIVE_JOBS = "pipelines.maxActiveJobs";
    private static final String PROPERTY_MAX_PENDING_JOBS = "pipelines.maxPendingJobs";
    private static final int MAX_ACTIVE_JOBS = AppPropertiesService.getPropertyInt( PROPERTY_MAX_ACTIVE_JOBS, 5 );
    public static final int MAX_PENDING_JOBS = AppPropertiesService.getPropertyInt( PROPERTY_MAX_PENDING_JOBS, 1000 );
    private static final String LOG_CLEANUP_ERROR = "Error in the job cleanup thread";
    private static final String LOG_CLEANUP_STARTED = "Job cleanup thread started";

    public enum JobStatus
    {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public static class JobInfo
    {
        private final String jobId;
        private final int pipelineId;
        private final int maxConcurrentWorkers;
        private volatile JobStatus status;
        private final long creationTime;

        /**
         * Constructs a new JobInfo instance and updates its status when the execution future completes.
         *
         * @param jobId
         *            the unique identifier for the job
         * @param pipelineId
         *            the pipeline identifier
         * @param maxConcurrentWorkers
         *            the per-pipeline maximum of concurrent running executions
         * @param future
         *            the CompletableFuture handling the job execution
         */
        public JobInfo( String jobId, int pipelineId, int maxConcurrentWorkers, CompletableFuture<Map<String, Object>> future )
        {
            this.jobId = jobId;
            this.pipelineId = pipelineId;
            this.maxConcurrentWorkers = maxConcurrentWorkers;
            this.status = JobStatus.QUEUED;
            this.creationTime = System.currentTimeMillis( );

            future.whenComplete( ( result, error ) -> this.status = error != null ? JobStatus.FAILED : JobStatus.COMPLETED );
        }

        /**
         * Returns the job identifier
         *
         * @return the job identifier
         */
        public String getJobId( )
        {
            return jobId;
        }

        /**
         * Returns the pipeline identifier
         *
         * @return the pipeline identifier
         */
        public int getPipelineId( )
        {
            return pipelineId;
        }

        /**
         * Returns the max concurrent workers
         *
         * @return the max concurrent workers
         */
        public int getMaxConcurrentWorkers( )
        {
            return maxConcurrentWorkers;
        }

        /**
         * Returns the status
         *
         * @return the status
         */
        public JobStatus getStatus( )
        {
            return status;
        }

        /**
         * Sets the job status.
         *
         * @param status
         *            the new job status
         */
        public void setStatus( JobStatus status )
        {
            this.status = status;
        }

        /**
         * Returns the creation time
         *
         * @return the creation time
         */
        public long getCreationTime( )
        {
            return creationTime;
        }
    }

    private final Map<String, JobInfo> jobs = new ConcurrentHashMap<>( );

    @Inject
    private PipelineService pipelineService;

    @Inject
    @Scheduler
    private ManagedScheduledExecutorService _scheduler;

    @Inject
    private Event<PipelineJobQueuedEvent> _jobQueuedEvent;

    @Inject
    private Event<PipelineJobActivatedEvent> _jobActivatedEvent;

    private ScheduledFuture<?> _cleanupFuture;

    /**
     * Default constructor for CDI.
     */
    public PipelineJobManager( )
    {
    }

    /**
     * Fails executions orphaned by a previous process, then schedules the periodic cleanup of old jobs.
     */
    @PostConstruct
    void init( )
    {
        PipelineExecutionHome.failOrphanedExecutions( );
        startCleanupThread( );
    }

    /**
     * Triggers eager instantiation of this bean when the web application context starts.
     *
     * <p>
     * The event payload is typed {@link ServletContext} (not the built-in {@code Startup} event) so the observer only fires inside a real web container —
     * matching lutece-core's {@code AppInitListener}. In a Java SE unit-test container there is no {@code ServletContext}, so orphaned-execution recovery is
     * skipped until the core is initialized, avoiding a startup ordering crash against an uninitialized {@code PluginService}.
     * </p>
     *
     * @param context
     *            the servlet context of the starting web application (content unused)
     */
    void onStartup( @Observes @Initialized( ApplicationScoped.class ) ServletContext context )
    {
    }

    /**
     * Creates a new job and either activates it immediately (when concurrency limits allow) or leaves it queued.
     *
     * @param jobId
     *            the unique job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param maxConcurrentWorkers
     *            the maximum number of concurrent workers for the pipeline
     * @param future
     *            the CompletableFuture for job execution
     * @return the created JobInfo instance
     */
    public JobInfo createJob( String jobId, int pipelineId, int maxConcurrentWorkers, CompletableFuture<Map<String, Object>> future )
    {
        JobInfo jobInfo = new JobInfo( jobId, pipelineId, maxConcurrentWorkers, future );
        jobs.put( jobId, jobInfo );

        if ( PipelineExecutionHome.tryActivate( jobId, pipelineId, maxConcurrentWorkers, MAX_ACTIVE_JOBS ) )
        {
            jobInfo.setStatus( JobStatus.RUNNING );
        }
        else
        {
            int position = PipelineExecutionHome.countByStatusAndPipeline( PipelineExecution.STATUS_PENDING, pipelineId );
            dispatchQueuedEvent( jobId, pipelineId, position, position );
        }

        return jobInfo;
    }

    /**
     * Activates a queued job and starts pipeline execution.
     *
     * @param jobId
     *            the job identifier to activate
     */
    public void activateJob( String jobId )
    {
        JobInfo jobInfo = jobs.get( jobId );
        if ( jobInfo != null && jobInfo.getStatus( ) == JobStatus.QUEUED )
        {
            jobInfo.setStatus( JobStatus.RUNNING );
            dispatchActivatedEvent( jobId, jobInfo.getPipelineId( ) );
            pipelineService.startPipeline( jobId );
        }
    }

    /**
     * Tries to activate as many locally-queued jobs as the concurrency limits allow. Called once a job completes so freed slots are filled. The atomic database
     * claim guarantees limits are respected even when several jobs are activated in the same pass.
     */
    public void activateNextQueued( )
    {
        for ( JobInfo jobInfo : jobs.values( ) )
        {
            if ( jobInfo.getStatus( ) == JobStatus.QUEUED
                    && PipelineExecutionHome.tryActivate( jobInfo.getJobId( ), jobInfo.getPipelineId( ), jobInfo.getMaxConcurrentWorkers( ), MAX_ACTIVE_JOBS ) )
            {
                activateJob( jobInfo.getJobId( ) );
            }
        }
    }

    /**
     * Schedules the periodic cleanup of old completed jobs on the container-managed scheduler.
     */
    private void startCleanupThread( )
    {
        _cleanupFuture = _scheduler.scheduleAtFixedRate( this::safeCleanupOldJobs, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS );
        AppLogService.info( LOG_CLEANUP_STARTED );
    }

    /**
     * Wraps the cleanup invocation in a safe try/catch so that one failure does not cancel the periodic schedule.
     */
    private void safeCleanupOldJobs( )
    {
        try
        {
            cleanupOldJobs( );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_CLEANUP_ERROR, e );
        }
    }

    /**
     * Cancels the cleanup schedule on CDI shutdown.
     */
    @PreDestroy
    void shutdown( )
    {
        if ( _cleanupFuture != null )
        {
            _cleanupFuture.cancel( false );
        }
    }

    /**
     * Removes old completed jobs that exceed retention time.
     */
    private void cleanupOldJobs( )
    {
        long cutoffTime = System.currentTimeMillis( ) - JOB_RETENTION_TIME_MS;
        jobs.entrySet( ).removeIf( entry -> {
            JobInfo job = entry.getValue( );
            return job.getStatus( ) != JobStatus.RUNNING && job.getStatus( ) != JobStatus.QUEUED && job.getCreationTime( ) < cutoffTime;
        } );
    }

    /**
     * Dispatches a job queued event.
     *
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param position
     *            the queue position
     * @param queueSize
     *            the total queue size
     */
    private void dispatchQueuedEvent( String jobId, int pipelineId, int position, int queueSize )
    {
        _jobQueuedEvent.fire( PipelineJobQueuedEvent.now( jobId, pipelineId, position, queueSize ) );
    }

    /**
     * Dispatches a job activated event.
     *
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     */
    private void dispatchActivatedEvent( String jobId, int pipelineId )
    {
        _jobActivatedEvent.fire( PipelineJobActivatedEvent.now( jobId, pipelineId ) );
    }
}
