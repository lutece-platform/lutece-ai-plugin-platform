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
package fr.paris.lutece.plugins.platform.service.concurrent;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Wires the platform's three CDI qualifiers ({@link BlockingIO}, {@link Orchestration}, {@link Scheduler}) to executors declared in the site's
 * {@code server.xml}.
 *
 * <p>
 * All three are looked up from {@code server.xml}. {@code concurrent/blockingIO} is declared there with {@code virtual="true"} (one Java 21 virtual thread per
 * blocking task — LLM streaming, embeddings, OCR, HTTP) plus a {@code concurrencyPolicy} that bounds concurrency, providing backpressure toward the database.
 * Keeping the definition in {@code server.xml} lets the deployment tune both the threading model and the bound without touching code, and stays portable (an EE
 * 10 site omits {@code virtual} for a platform pool).
 * </p>
 *
 * <p>
 * <b>Orchestration</b> and <b>Scheduler</b> are platform pools (CPU-bound dispatch / low-frequency cleanup): they do not use virtual threads.
 * </p>
 *
 * <p>
 * Each executor references {@code LuteceContextService} so the application classloader (TCCL), security and JNDI metadata are propagated — required by
 * lutece-core's {@code DAOUtil}, which calls {@code CDI.current()} from its field initializer.
 * </p>
 */
@ApplicationScoped
public class ConcurrencyDefinitions
{
    @Resource( lookup = "concurrent/blockingIO" )
    private ManagedExecutorService _blockingExecutor;

    @Resource( lookup = "concurrent/orchestration" )
    private ManagedExecutorService _orchestrationExecutor;

    @Resource( lookup = "concurrent/scheduler" )
    private ManagedScheduledExecutorService _scheduledExecutor;

    /**
     * Produces the executor for blocking I/O (LLM streaming, embeddings, OCR, HTTP), backed by the {@code concurrent/blockingIO} executor declared in the
     * site's {@code server.xml}.
     *
     * @return the blocking-I/O managed executor
     */
    @Produces
    @ApplicationScoped
    @BlockingIO
    ManagedExecutorService produceBlockingExecutor( )
    {
        return _blockingExecutor;
    }

    /**
     * Produces the executor used for pipeline orchestration (node dispatch, async coordination). Runs on platform threads since coordination is CPU-bound.
     *
     * @return the managed executor
     */
    @Produces
    @ApplicationScoped
    @Orchestration
    ManagedExecutorService produceOrchestrationExecutor( )
    {
        return _orchestrationExecutor;
    }

    /**
     * Produces the scheduler used for periodic platform tasks (SSE cleanup, queue housekeeping).
     *
     * @return the managed scheduled executor
     */
    @Produces
    @ApplicationScoped
    @Scheduler
    ManagedScheduledExecutorService produceScheduler( )
    {
        return _scheduledExecutor;
    }
}
