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

import java.util.Collections;
import java.util.List;

import fr.paris.lutece.plugins.platform.service.bot.BotEventTypes;
import fr.paris.lutece.plugins.platform.business.bot.Bot;

/**
 * Fired when a bot's RAG router decides which datasets to query for a given user prompt.
 *
 * @param operationId
 *            stream identifier
 * @param timestamp
 *            event timestamp in ms
 * @param botId
 *            bot identifier
 * @param datasets
 *            list of selected datasets descriptors (immutable view)
 */
public record DatasetsRoutedEvent(String operationId, long timestamp, int botId, List<Object> datasets) implements PlatformDomainEvent {
    /**
     * Compact constructor wrapping the datasets list into an immutable view.
     */
    public DatasetsRoutedEvent {
        datasets = datasets == null ? Collections.emptyList( ) : Collections.unmodifiableList( datasets );
    }

    /**
     * Factory with current timestamp.
     *
     * @param operationId
     *            stream identifier
     * @param botId
     *            bot identifier
     * @param datasets
     *            selected datasets
     * @return the event instance
     */
    public static DatasetsRoutedEvent now( String operationId, int botId, List<Object> datasets )
    {
        return new DatasetsRoutedEvent( operationId, System.currentTimeMillis( ), botId, datasets );
    }

    @Override
    public String resourceType( )
    {
        return Bot.RESOURCE_TYPE;
    }

    @Override
    public String eventName( )
    {
        return BotEventTypes.DATASETS_ROUTED;
    }
}
