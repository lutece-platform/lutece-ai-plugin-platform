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

import java.util.List;

import fr.paris.lutece.plugins.platform.service.event.ModelEventTypes;
import fr.paris.lutece.plugins.platform.business.model.Model;

/**
 * Fired when a model streaming response ends with tool execution requests, so SSE consumers receive the tool calls instead of silently losing them (the
 * streaming path has no response DTO to carry them, unlike the synchronous path).
 *
 * @param operationId
 *            stream identifier
 * @param timestamp
 *            event timestamp in ms
 * @param providerId
 *            model provider identifier
 * @param toolCalls
 *            the tool calls requested by the model
 */
public record ModelToolCallsEvent(String operationId, long timestamp, int providerId, List<ToolCall> toolCalls) implements PlatformDomainEvent {
    /**
     * One tool call requested by the model.
     *
     * @param id
     *            the provider-assigned call identifier
     * @param name
     *            the tool name
     * @param arguments
     *            the JSON arguments
     */
    public record ToolCall(String id, String name, String arguments) {
    }

    /**
     * Factory with current timestamp.
     *
     * @param operationId
     *            stream identifier
     * @param providerId
     *            provider identifier
     * @param toolCalls
     *            the tool calls requested by the model
     * @return the event instance
     */
    public static ModelToolCallsEvent now( String operationId, int providerId, List<ToolCall> toolCalls )
    {
        return new ModelToolCallsEvent( operationId, System.currentTimeMillis( ), providerId, toolCalls );
    }

    @Override
    public String resourceType( )
    {
        return Model.RESOURCE_TYPE;
    }

    @Override
    public String eventName( )
    {
        return ModelEventTypes.TOOL_CALLS;
    }
}
