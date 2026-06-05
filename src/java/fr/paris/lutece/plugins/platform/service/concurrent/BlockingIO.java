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

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI qualifier for the platform executor dedicated to blocking I/O calls (LLM streaming, embeddings, OCR, S3 reads, HTTP requests).
 *
 * <p>
 * Produced in {@link ConcurrencyDefinitions} by looking up the {@code concurrent/blockingIO} managed executor declared in the site's {@code server.xml}. On the
 * current target runtime (Open Liberty 25 / Jakarta EE 10, no {@code concurrent-3.1}) this is a <b>bounded platform pool</b>: the policy's {@code max} limits
 * concurrency and provides backpressure toward the database.
 * </p>
 *
 * <p>
 * Declaring it in {@code server.xml} (rather than {@code @ManagedExecutorDefinition}) keeps the Java portable: on an EE 11 / Open Liberty 26+ site, adding
 * {@code virtual="true"} on the nested {@code <concurrencyPolicy>} switches every task to its own Java 21 virtual thread with no code change. NOTE: in Liberty
 * the {@code virtual} attribute belongs on {@code <concurrencyPolicy>}, not on {@code <managedExecutorService>} — it is silently ignored on the latter.
 * </p>
 *
 * <p>
 * Isolated from {@link Orchestration} and {@link Scheduler} so one saturation cannot starve the others.
 * </p>
 *
 * <p>
 * Usage: {@code @Inject @BlockingIO ManagedExecutorService _executor;}
 * </p>
 */
@Qualifier
@Retention( RetentionPolicy.RUNTIME )
@Target( {
        ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE
} )
public @interface BlockingIO
{
    /**
     * AnnotationLiteral for programmatic CDI lookup, e.g. {@code CDI.current().select(ManagedExecutorService.class, BlockingIO.Literal.INSTANCE).get()}.
     */
    @SuppressWarnings( "all" )
    final class Literal extends AnnotationLiteral<BlockingIO> implements BlockingIO
    {
        private static final long serialVersionUID = 1L;

        /** Singleton instance for programmatic CDI selection. */
        public static final Literal INSTANCE = new Literal( );
    }
}
