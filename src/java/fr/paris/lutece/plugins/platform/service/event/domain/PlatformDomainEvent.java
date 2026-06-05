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

/**
 * Sealed root type for every typed domain event fired through the CDI event bus. SSE consumers observe this sealed type and dispatch by pattern matching to a
 * deterministic JSON shape per record.
 */
public sealed interface PlatformDomainEvent permits PipelineExecutionStartedEvent, PipelineExecutionCompletedEvent, PipelineExecutionFailedEvent,
        PipelineNodeStartedEvent, PipelineNodeCompletedEvent, PipelineNodeFailedEvent, PipelineNodePortChosenEvent, PipelineNodeStreamEvent,
        PipelineJobQueuedEvent, PipelineJobActivatedEvent, PipelineTokenUsageRecordedEvent, ModelTokenEvent, ModelStreamCompletedEvent, ModelStreamErrorEvent,
        ModelToolCallsEvent, BotStreamStartedEvent, BotTokenEvent, BotStreamCompletedEvent, BotStreamErrorEvent, BotSourcesRetrievedEvent, BotToolStartedEvent,
        BotToolCompletedEvent, BotToolFailedEvent, DatasetsRoutedEvent
{
    /**
     * Operation identifier used by SSE consumers to match an event with the subscriber.
     *
     * @return operation identifier
     */
    String operationId( );

    /**
     * Event creation timestamp in milliseconds since epoch.
     *
     * @return timestamp in milliseconds
     */
    long timestamp( );

    /**
     * Resource type bucket the event belongs to (PIPELINE, BOT, MODEL...). Used by SSE filtering.
     *
     * @return resource type
     */
    String resourceType( );

    /**
     * Stable event name emitted on the SSE stream so the legacy frontend keeps the same wire format.
     *
     * @return event name
     */
    String eventName( );
}
