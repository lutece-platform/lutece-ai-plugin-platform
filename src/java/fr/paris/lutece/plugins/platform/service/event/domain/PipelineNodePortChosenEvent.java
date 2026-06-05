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

import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineEventTypes;

/**
 * Fired when a node decides which output port to follow.
 *
 * @param operationId
 *            execution identifier (jobId)
 * @param timestamp
 *            event timestamp in ms
 * @param pipelineId
 *            pipeline identifier
 * @param nodeId
 *            node identifier
 * @param nodeType
 *            node type
 * @param outputPort
 *            chosen output port
 */
public record PipelineNodePortChosenEvent(String operationId, long timestamp, int pipelineId, String nodeId, String nodeType,
        String outputPort) implements PlatformDomainEvent {
    /**
     * Factory with current timestamp.
     *
     * @param operationId
     *            execution identifier
     * @param pipelineId
     *            pipeline identifier
     * @param nodeId
     *            node identifier
     * @param nodeType
     *            node type
     * @param outputPort
     *            chosen output port
     * @return the event instance
     */
    public static PipelineNodePortChosenEvent now( String operationId, int pipelineId, String nodeId, String nodeType, String outputPort )
    {
        return new PipelineNodePortChosenEvent( operationId, System.currentTimeMillis( ), pipelineId, nodeId, nodeType, outputPort );
    }

    @Override
    public String resourceType( )
    {
        return Pipeline.RESOURCE_TYPE;
    }

    @Override
    public String eventName( )
    {
        return PipelineEventTypes.PORT_CHOSEN;
    }
}
