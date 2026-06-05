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
package fr.paris.lutece.plugins.platform.service.observability;

import fr.paris.lutece.plugins.platform.service.concurrent.Scheduler;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Write-behind sink for observability persistence. Hot-path threads enqueue persistence tasks instead of hitting the database directly; a single periodic drain
 * applies them sequentially on the {@link Scheduler} pool. Observability therefore holds at most one DB connection at a time and can never starve the request
 * connection pool under load.
 *
 * <p>
 * The queue is bounded: when saturated, tasks are dropped and counted rather than blocking the business path — observability must never degrade request
 * latency.
 * </p>
 *
 * <p>
 * Sequential FIFO draining preserves per-execution ordering (start before complete), so cost recomputations that read prior writes see a consistent state.
 * </p>
 */
@ApplicationScoped
public class ObservabilityWriter
{
    private static final int QUEUE_CAPACITY = 50000;
    private static final long DRAIN_INTERVAL_MS = 200;
    private static final int DRAIN_BATCH_MAX = 2000;
    private static final String WARN_QUEUE_FULL = "Observability queue saturated, dropped {} task(s) total";
    private static final String ERROR_TASK_FAILED = "Observability persistence task failed";
    private static final String ERROR_DRAIN_FAILED = "Observability drain cycle failed";

    private final BlockingQueue<Runnable> _queue = new LinkedBlockingQueue<>( QUEUE_CAPACITY );
    private final AtomicLong _dropped = new AtomicLong( );

    @Inject
    @Scheduler
    private ManagedScheduledExecutorService _scheduler;

    /**
     * Schedules the periodic drain once the bean is initialized.
     */
    @PostConstruct
    void start( )
    {
        _scheduler.scheduleWithFixedDelay( this::drain, DRAIN_INTERVAL_MS, DRAIN_INTERVAL_MS, TimeUnit.MILLISECONDS );
    }

    /**
     * Enqueues a persistence task for asynchronous execution. Never blocks: a saturated queue drops the task and increments the drop counter, keeping the hot
     * path free.
     *
     * @param task
     *            the persistence operation to run off the request thread
     */
    public void submit( Runnable task )
    {
        if ( !_queue.offer( task ) )
        {
            long total = _dropped.incrementAndGet( );
            if ( total % 1000 == 1 )
            {
                AppLogService.error( WARN_QUEUE_FULL, total );
            }
        }
    }

    /**
     * Drains a bounded batch and applies each task sequentially on the scheduler thread, so observability persistence uses a single concurrent DB connection. A
     * task failure is logged and skipped; the drain itself is guarded so the recurring schedule survives.
     */
    private void drain( )
    {
        try
        {
            List<Runnable> batch = new ArrayList<>( DRAIN_BATCH_MAX );
            _queue.drainTo( batch, DRAIN_BATCH_MAX );

            for ( Runnable task : batch )
            {
                try
                {
                    task.run( );
                }
                catch( Exception e )
                {
                    AppLogService.error( ERROR_TASK_FAILED, e );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_DRAIN_FAILED, e );
        }
    }
}
