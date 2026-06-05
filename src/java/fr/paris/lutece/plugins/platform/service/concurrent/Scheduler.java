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
 * CDI qualifier for the platform ManagedScheduledExecutorService used for periodic cleanup tasks (SSE subscription expiry, pipeline job retention, idle
 * resource sweeping).
 *
 * <p>
 * Backed by the {@code concurrent/scheduler} managed scheduled executor declared in the site's {@code server.xml} and produced in
 * {@link ConcurrencyDefinitions}. The pool is kept small since scheduled tasks are few and lightweight — isolating them guarantees cleanup keeps running even
 * when {@link BlockingIO} saturates. Actual sizing lives in the site's {@code server.xml}.
 * </p>
 *
 * <p>
 * Usage: {@code @Inject @Scheduler ManagedScheduledExecutorService _scheduler;}
 * </p>
 */
@Qualifier
@Retention( RetentionPolicy.RUNTIME )
@Target( {
        ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE
} )
public @interface Scheduler
{
    /**
     * AnnotationLiteral for programmatic CDI lookup.
     */
    @SuppressWarnings( "all" )
    final class Literal extends AnnotationLiteral<Scheduler> implements Scheduler
    {
        private static final long serialVersionUID = 1L;

        /** Singleton instance for programmatic CDI selection. */
        public static final Literal INSTANCE = new Literal( );
    }
}
