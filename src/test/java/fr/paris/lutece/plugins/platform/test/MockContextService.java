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
package fr.paris.lutece.plugins.platform.test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Stands in for the container-provided {@code java:comp/DefaultContextService} when running under Java SE. The site's {@code server.xml} supplies the real
 * {@link ContextService} in production; {@code library-lutece-unit-testing} ships mocks for the managed executors/thread-factory but none for
 * {@link ContextService}, so the platform services that inject it ({@code BotQueryService}, {@code BotMemoryService}, {@code ModelService},
 * {@code ModelQueryService}) would leave Weld with an unsatisfied dependency at bootstrap.
 *
 * <p>
 * This mock applies identity context propagation: every contextual wrapper returns its argument unchanged, which is safe whether or not the bean is ever
 * actually invoked by a test. It follows the same {@code @Alternative @Priority} pattern as the library mocks.
 * </p>
 */
@ApplicationScoped
@Alternative
@Priority( 10 )
public class MockContextService implements ContextService
{
    /**
     * {@inheritDoc} Returns the callable unchanged (no context capture).
     */
    @Override
    public <R> Callable<R> contextualCallable( Callable<R> callable )
    {
        return callable;
    }

    /**
     * {@inheritDoc} Returns the consumer unchanged (no context capture).
     */
    @Override
    public <T, U> BiConsumer<T, U> contextualConsumer( BiConsumer<T, U> consumer )
    {
        return consumer;
    }

    /**
     * {@inheritDoc} Returns the consumer unchanged (no context capture).
     */
    @Override
    public <T> Consumer<T> contextualConsumer( Consumer<T> consumer )
    {
        return consumer;
    }

    /**
     * {@inheritDoc} Returns the function unchanged (no context capture).
     */
    @Override
    public <T, U, R> BiFunction<T, U, R> contextualFunction( BiFunction<T, U, R> function )
    {
        return function;
    }

    /**
     * {@inheritDoc} Returns the function unchanged (no context capture).
     */
    @Override
    public <T, R> Function<T, R> contextualFunction( Function<T, R> function )
    {
        return function;
    }

    /**
     * {@inheritDoc} Returns the runnable unchanged (no context capture).
     */
    @Override
    public Runnable contextualRunnable( Runnable runnable )
    {
        return runnable;
    }

    /**
     * {@inheritDoc} Returns the supplier unchanged (no context capture).
     */
    @Override
    public <R> Supplier<R> contextualSupplier( Supplier<R> supplier )
    {
        return supplier;
    }

    /**
     * {@inheritDoc} Returns the subscriber unchanged (no context capture).
     */
    @Override
    public <T> Flow.Subscriber<T> contextualSubscriber( Flow.Subscriber<T> subscriber )
    {
        return subscriber;
    }

    /**
     * {@inheritDoc} Returns the processor unchanged (no context capture).
     */
    @Override
    public <T, R> Flow.Processor<T, R> contextualProcessor( Flow.Processor<T, R> processor )
    {
        return processor;
    }

    /**
     * {@inheritDoc} Returns the instance unchanged (no contextual proxy created).
     */
    @Override
    public <T> T createContextualProxy( T instance, Class<T> intf )
    {
        return instance;
    }

    /**
     * {@inheritDoc} Returns the instance unchanged (no contextual proxy created).
     */
    @Override
    public Object createContextualProxy( Object instance, Class<?>... interfaces )
    {
        return instance;
    }

    /**
     * {@inheritDoc} Returns the instance unchanged (no contextual proxy created).
     */
    @Override
    public <T> T createContextualProxy( T instance, Map<String, String> executionProperties, Class<T> intf )
    {
        return instance;
    }

    /**
     * {@inheritDoc} Returns the instance unchanged (no contextual proxy created).
     */
    @Override
    public Object createContextualProxy( Object instance, Map<String, String> executionProperties, Class<?>... interfaces )
    {
        return instance;
    }

    /**
     * {@inheritDoc} Returns a same-thread executor (no context capture).
     */
    @Override
    public Executor currentContextExecutor( )
    {
        return Runnable::run;
    }

    /**
     * {@inheritDoc} Returns an empty map (no execution properties tracked).
     */
    @Override
    public Map<String, String> getExecutionProperties( Object contextualProxy )
    {
        return Collections.emptyMap( );
    }

    /**
     * {@inheritDoc} Returns the stage unchanged (no context capture).
     */
    @Override
    public <T> CompletableFuture<T> withContextCapture( CompletableFuture<T> future )
    {
        return future;
    }

    /**
     * {@inheritDoc} Returns the stage unchanged (no context capture).
     */
    @Override
    public <T> CompletionStage<T> withContextCapture( CompletionStage<T> stage )
    {
        return stage;
    }
}
