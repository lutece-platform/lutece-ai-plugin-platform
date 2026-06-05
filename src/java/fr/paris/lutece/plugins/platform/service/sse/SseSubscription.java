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
package fr.paris.lutece.plugins.platform.service.sse;

import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Represents an SSE subscription. Holds the {@link SseEventSink} and {@link Sse} required by the CDI observer in
 * {@code PlatformSseStreamManager.onPlatformEvent} to build and send outbound SSE events.
 */
public class SseSubscription implements AutoCloseable
{
    private static final String ERROR_CLOSING_SSE_EVENT_SINK = "Error closing SSE event sink for: %s";

    private final String subscriptionId;
    private final String operationId;
    private final String resourceType;
    private final SseEventSink eventSink;
    private final Sse sse;
    private final int clientId;
    private final String userId;
    private volatile long lastAccessed;
    private volatile boolean closed = false;

    /**
     * Constructor for SseSubscription
     *
     * @param subscriptionId
     *            the subscription identifier
     * @param operationId
     *            the operation identifier
     * @param resourceType
     *            the resource type
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the JAX-RS SSE context for building outbound events
     * @param clientId
     *            the client identifier
     * @param userId
     *            the user identifier
     */
    public SseSubscription( String subscriptionId, String operationId, String resourceType, SseEventSink eventSink, Sse sse, int clientId, String userId )
    {
        this.subscriptionId = subscriptionId;
        this.operationId = operationId;
        this.resourceType = resourceType;
        this.eventSink = eventSink;
        this.sse = sse;
        this.clientId = clientId;
        this.userId = userId;
        this.lastAccessed = System.currentTimeMillis( );
    }

    /**
     * Gets the SSE event sink for this subscription.
     *
     * @return the SSE event sink
     */
    public SseEventSink getEventSink( )
    {
        return eventSink;
    }

    /**
     * Gets the JAX-RS SSE context for this subscription.
     *
     * @return the SSE context
     */
    public Sse getSse( )
    {
        return sse;
    }

    /**
     * Gets the subscription identifier
     *
     * @return the subscription identifier
     */
    public String getSubscriptionId( )
    {
        return subscriptionId;
    }

    /**
     * Gets the operation identifier
     *
     * @return the operation identifier
     */
    public String getOperationId( )
    {
        return operationId;
    }

    /**
     * Gets the resource type associated with the subscription
     *
     * @return the resource type
     */
    public String getResourceType( )
    {
        return resourceType;
    }

    /**
     * Gets the client identifier
     *
     * @return the client identifier
     */
    public int getClientId( )
    {
        return clientId;
    }

    /**
     * Gets the user identifier
     *
     * @return the user identifier
     */
    public String getUserId( )
    {
        return userId;
    }

    /**
     * Gets the last accessed timestamp
     *
     * @return the last accessed timestamp
     */
    public long getLastAccessed( )
    {
        return lastAccessed;
    }

    /**
     * Updates the last accessed timestamp
     */
    public void updateLastAccessed( )
    {
        this.lastAccessed = System.currentTimeMillis( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close( )
    {
        if ( !closed )
        {
            closed = true;
            closeEventSink( );
        }
    }

    /**
     * Closes the event sink
     */
    private void closeEventSink( )
    {
        try
        {
            if ( eventSink != null && !eventSink.isClosed( ) )
            {
                eventSink.close( );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_CLOSING_SSE_EVENT_SINK, subscriptionId ), e );
        }
    }
}
