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
package fr.paris.lutece.plugins.platform.service.event.domain;

import fr.paris.lutece.plugins.platform.service.event.ModelEventTypes;
import fr.paris.lutece.plugins.platform.business.model.Model;

/**
 * Fired for each token emitted by a model streaming response.
 *
 * @param operationId
 *            stream identifier
 * @param timestamp
 *            event timestamp in ms
 * @param providerId
 *            model provider identifier
 * @param token
 *            streamed token
 */
public record ModelTokenEvent(String operationId, long timestamp, int providerId, String token) implements PlatformDomainEvent {
    /**
     * Factory with current timestamp.
     *
     * @param operationId
     *            stream identifier
     * @param providerId
     *            model provider identifier
     * @param token
     *            token
     * @return the event instance
     */
    public static ModelTokenEvent now( String operationId, int providerId, String token )
    {
        return new ModelTokenEvent( operationId, System.currentTimeMillis( ), providerId, token );
    }

    @Override
    public String resourceType( )
    {
        return Model.RESOURCE_TYPE;
    }

    @Override
    public String eventName( )
    {
        return ModelEventTypes.TOKEN_STREAMED;
    }
}
