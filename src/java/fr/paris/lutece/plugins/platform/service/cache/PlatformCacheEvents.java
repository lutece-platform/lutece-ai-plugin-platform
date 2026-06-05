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
package fr.paris.lutece.plugins.platform.service.cache;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Static façade used by the Home classes (which are not CDI beans) to broadcast an {@link EntityChangedEvent} after a mutation. Firing is synchronous, so
 * observing caches are invalidated within the same request/transaction, before the caller returns.
 */
public final class PlatformCacheEvents
{
    /**
     * Private constructor — utility class.
     */
    private PlatformCacheEvents( )
    {
    }

    /**
     * Broadcasts a configuration-entity mutation so that every interested cache invalidates its keys.
     *
     * @param resource
     *            the mutated configuration entity
     * @param id
     *            the primary key of the mutated entity
     * @param action
     *            the kind of mutation
     */
    public static void fire( PlatformResource resource, int id, EntityChangedEvent.Action action )
    {
        CDI.current( ).getBeanManager( ).getEvent( ).fire( new EntityChangedEvent( resource, id, action ) );
    }
}
