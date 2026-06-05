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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.service.concurrent.Scheduler;
import fr.paris.lutece.plugins.platform.service.event.domain.BotStreamCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotStreamErrorEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelStreamCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.ModelStreamErrorEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineExecutionCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PipelineExecutionFailedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PlatformDomainEvent;
import jakarta.annotation.PreDestroy;
import fr.paris.lutece.portal.service.util.AppLogService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * SSE stream multiplexer. Observes the typed sealed {@link PlatformDomainEvent} hierarchy and forwards each event to every matching subscription. Subscriptions
 * are filtered by {@code operationId} and optional {@code resourceType}. Terminal events (stream completed / failed) trigger automatic subscription cleanup.
 * <p>
 * Each subscription's lifecycle is bound to a single live HTTP connection (the streaming POST that opened it) : it is registered when generation starts and
 * dropped on the terminal event or when the client disconnects. A periodic keep-alive comment (SSE {@code :} line) keeps idle connections open through proxies
 * and doubles as dead-connection detection (a failed send unregisters the subscription).
 */
@ApplicationScoped
@Named( "platform.platformSseStreamManager" )
public class PlatformSseStreamManager
{
    private static final int KEEPALIVE_INITIAL_DELAY_SECONDS = 20;
    private static final int KEEPALIVE_PERIOD_SECONDS = 20;
    private static final String KEEPALIVE_COMMENT = "keepalive";
    private static final String SUBSCRIPTION_ID_SEPARATOR = "_sse_";
    private static final String LOG_INITIALIZED = "PlatformSseStreamManager initialized.";
    private static final String LOG_STREAM_REGISTERED = "SSE stream registered for operationId: %s, subscriptionId: %s";
    private static final String LOG_STREAM_UNREGISTERED = "SSE stream unregistered: %s";
    private static final String LOG_SHUTTING_DOWN = "PlatformSseStreamManager shutting down...";
    private static final String LOG_SHUTDOWN_COMPLETE = "PlatformSseStreamManager shutdown complete.";
    private static final String ERROR_REGISTER_SSE_STREAM = "Error registering SSE stream for operationId: %s";
    private static final String ERROR_SENDING_SSE_EVENT = "Error sending SSE event for subscription: %s";
    private static final String ERROR_PROCESSING_PLATFORM_EVENT = "Error processing platform event for SSE subscription: %s";
    private static final String ERROR_CLOSING_EVENT_SINK = "Error closing SSE event sink";
    private static final String EXCEPTION_FAILED_REGISTER = "Failed to register SSE stream";

    private final Map<String, SseSubscription> _activeSubscriptions = new ConcurrentHashMap<>( );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );
    @Inject
    @ConfigProperty( name = "platform.agent.sse.max.concurrent.streams.global", defaultValue = "100" )
    private int maxConcurrentStreamsGlobal;

    @Inject
    @ConfigProperty( name = "platform.agent.sse.max.concurrent.streams.per.client.user", defaultValue = "3" )
    private int maxConcurrentStreamsPerClientUser;

    @Inject
    @Scheduler
    private ManagedScheduledExecutorService _scheduler;

    private ScheduledFuture<?> _keepAliveFuture;

    /**
     * Schedules the periodic keep-alive on the container-managed scheduler.
     */
    @PostConstruct
    void init( )
    {
        _keepAliveFuture = _scheduler.scheduleAtFixedRate( this::sendKeepAlive, KEEPALIVE_INITIAL_DELAY_SECONDS, KEEPALIVE_PERIOD_SECONDS, TimeUnit.SECONDS );
        AppLogService.info( LOG_INITIALIZED );
    }

    /**
     * Registers an SSE stream for event notifications, bound to the live connection that opened it. The caller (a streaming POST resource) registers the sink
     * with the same {@code operationId} the generation will emit on, before kicking off generation : there is no init/events split and no registration race.
     *
     * @param operationId
     *            the operation identifier the generation emits events on
     * @param resourceType
     *            the resource type (or null to match every resource type)
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @return an AutoCloseable to unregister the stream
     */
    public AutoCloseable registerSseStream( String operationId, String resourceType, SseEventSink eventSink, Sse sse )
    {
        return registerSseStream( operationId, resourceType, eventSink, sse, 0, null );
    }

    /**
     * Registers an SSE stream, recording the owning client/user so the per-client-user concurrency cap can be enforced on the live subscriptions.
     *
     * @param operationId
     *            the operation identifier the generation emits events on
     * @param resourceType
     *            the resource type (or null to match every resource type)
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @param clientId
     *            the owning client identifier (0 for session/back-office streams)
     * @param userId
     *            the owning user identifier (null for session/back-office streams)
     * @return an AutoCloseable to unregister the stream
     */
    public AutoCloseable registerSseStream( String operationId, String resourceType, SseEventSink eventSink, Sse sse, int clientId, String userId )
    {
        final String subscriptionId = generateSubscriptionId( operationId );
        try
        {
            SseSubscription subscription = new SseSubscription( subscriptionId, operationId, resourceType, eventSink, sse, clientId, userId );
            _activeSubscriptions.put( subscriptionId, subscription );
            AppLogService.info( String.format( LOG_STREAM_REGISTERED, operationId, subscriptionId ) );

            return ( ) -> unregisterSseStream( subscriptionId );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_REGISTER_SSE_STREAM, operationId ), e );
            closeEventSink( eventSink );
            throw new RuntimeException( EXCEPTION_FAILED_REGISTER, e );
        }
    }

    /**
     * Observes every typed domain event and dispatches it to every matching SSE subscription. Filtering is done at runtime on {@code operationId} + optional
     * {@code resourceType}.
     *
     * @param event
     *            the typed domain event fired by the producer
     */
    public void onDomainEvent( @Observes PlatformDomainEvent event )
    {
        for ( SseSubscription subscription : _activeSubscriptions.values( ) )
        {
            if ( shouldProcessEvent( subscription.getOperationId( ), subscription.getResourceType( ), event ) )
            {
                handleDomainEvent( subscription.getSubscriptionId( ), subscription.getEventSink( ), subscription.getSse( ), event );
            }
        }
    }

    /**
     * Checks if a new stream can be created based on global limit.
     *
     * @return true if a new stream can be created
     */
    public boolean canCreateNewStream( )
    {
        return _activeSubscriptions.size( ) < maxConcurrentStreamsGlobal;
    }

    /**
     * Checks if a new stream can be created for a specific client user.
     *
     * @param clientId
     *            the client identifier
     * @param userId
     *            the user identifier
     * @return true if a new stream can be created for the user
     */
    public boolean canCreateNewStreamForClientUser( int clientId, String userId )
    {
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            return false;
        }

        long currentClientUserStreams = _activeSubscriptions.values( ).stream( ).filter( subscription -> matchesClientUser( subscription, clientId, userId ) )
                .count( );

        return currentClientUserStreams < maxConcurrentStreamsPerClientUser;
    }

    /**
     * Releases the keep-alive task and active SSE subscriptions on CDI shutdown.
     */
    @PreDestroy
    void shutdown( )
    {
        AppLogService.info( LOG_SHUTTING_DOWN );
        if ( _keepAliveFuture != null )
        {
            _keepAliveFuture.cancel( false );
        }
        _activeSubscriptions.values( ).forEach( SseSubscription::close );
        _activeSubscriptions.clear( );
        AppLogService.info( LOG_SHUTDOWN_COMPLETE );
    }

    /**
     * Generates a unique subscription ID.
     *
     * @param operationId
     *            the operation identifier
     * @return the generated subscription ID
     */
    private String generateSubscriptionId( String operationId )
    {
        return operationId + SUBSCRIPTION_ID_SEPARATOR + System.currentTimeMillis( );
    }

    /**
     * Tells whether a given subscription should receive the given event.
     *
     * @param operationId
     *            the subscription operation identifier
     * @param resourceType
     *            the subscription resource type filter (or null to match any)
     * @param event
     *            the typed domain event
     * @return true if the event should be forwarded
     */
    private boolean shouldProcessEvent( String operationId, String resourceType, PlatformDomainEvent event )
    {
        return operationId.equals( event.operationId( ) ) && ( resourceType == null || resourceType.equals( event.resourceType( ) ) );
    }

    /**
     * Serializes and forwards a domain event to a subscription.
     *
     * @param subscriptionId
     *            the subscription identifier
     * @param eventSink
     *            the SSE event sink
     * @param sse
     *            the SSE instance
     * @param event
     *            the typed domain event
     */
    private void handleDomainEvent( String subscriptionId, SseEventSink eventSink, Sse sse, PlatformDomainEvent event )
    {
        SseSubscription subscription = _activeSubscriptions.get( subscriptionId );
        if ( subscription == null || eventSink.isClosed( ) )
        {
            return;
        }

        try
        {
            OutboundSseEvent sseEvent = createSseEvent( sse, event );
            sendSseEvent( subscriptionId, eventSink, subscription, event, sseEvent );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_PROCESSING_PLATFORM_EVENT, subscriptionId ), e );
            unregisterSseStream( subscriptionId );
        }
    }

    /**
     * Creates an SSE event from a domain event. The wire JSON shape is the event record itself, the SSE event name comes from
     * {@link PlatformDomainEvent#eventName()}.
     *
     * @param sse
     *            the SSE instance
     * @param event
     *            the domain event
     * @return the outbound SSE event
     * @throws Exception
     *             if serialization fails
     */
    private OutboundSseEvent createSseEvent( Sse sse, PlatformDomainEvent event ) throws Exception
    {
        String eventData = OBJECT_MAPPER.writeValueAsString( event );
        return sse.newEventBuilder( ).name( event.eventName( ) ).data( eventData ).mediaType( MediaType.APPLICATION_JSON_TYPE ).build( );
    }

    /**
     * Sends an SSE event and wires success/error callbacks.
     *
     * @param subscriptionId
     *            the subscription identifier
     * @param eventSink
     *            the SSE event sink
     * @param subscription
     *            the SSE subscription
     * @param event
     *            the domain event
     * @param sseEvent
     *            the outbound SSE event
     */
    private void sendSseEvent( String subscriptionId, SseEventSink eventSink, SseSubscription subscription, PlatformDomainEvent event,
            OutboundSseEvent sseEvent )
    {
        eventSink.send( sseEvent ).thenRun( ( ) -> handleSuccessfulSend( subscriptionId, subscription, event ) )
                .exceptionally( throwable -> handleFailedSend( subscriptionId, throwable ) );
    }

    /**
     * Handles successful event send and triggers cleanup on terminal events.
     *
     * @param subscriptionId
     *            the subscription identifier
     * @param subscription
     *            the SSE subscription
     * @param event
     *            the domain event
     */
    private void handleSuccessfulSend( String subscriptionId, SseSubscription subscription, PlatformDomainEvent event )
    {
        subscription.updateLastAccessed( );
        if ( isTerminalEvent( event ) )
        {
            unregisterSseStream( subscriptionId );
        }
    }

    /**
     * Handles failed event send by unregistering the broken subscription.
     *
     * @param subscriptionId
     *            the subscription identifier
     * @param throwable
     *            the exception that occurred
     * @return null
     */
    private Void handleFailedSend( String subscriptionId, Throwable throwable )
    {
        AppLogService.error( String.format( ERROR_SENDING_SSE_EVENT, subscriptionId ), throwable );
        unregisterSseStream( subscriptionId );
        return null;
    }

    /**
     * Tells whether the event closes its operation lifecycle.
     *
     * @param event
     *            the domain event
     * @return true when the event is the last expected for this operation
     */
    private boolean isTerminalEvent( PlatformDomainEvent event )
    {
        return event instanceof PipelineExecutionCompletedEvent || event instanceof PipelineExecutionFailedEvent || event instanceof BotStreamCompletedEvent
                || event instanceof BotStreamErrorEvent || event instanceof ModelStreamCompletedEvent || event instanceof ModelStreamErrorEvent;
    }

    /**
     * Unregisters an SSE stream.
     *
     * @param subscriptionId
     *            the subscription identifier
     */
    private void unregisterSseStream( String subscriptionId )
    {
        SseSubscription subscription = _activeSubscriptions.remove( subscriptionId );
        if ( subscription != null )
        {
            subscription.close( );
            AppLogService.info( String.format( LOG_STREAM_UNREGISTERED, subscriptionId ) );
        }
    }

    /**
     * Closes an event sink.
     *
     * @param eventSink
     *            the event sink to close
     */
    private void closeEventSink( SseEventSink eventSink )
    {
        if ( eventSink != null && !eventSink.isClosed( ) )
        {
            try
            {
                eventSink.close( );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_CLOSING_EVENT_SINK, e );
            }
        }
    }

    /**
     * Sends a keep-alive comment to every active subscription. Long idle gaps (LLM first-token latency, tool calls) would otherwise be dropped by intermediate
     * proxies after 60-120s ; the comment keeps the connection warm. A send that completes exceptionally means the client is gone, so the subscription is
     * dropped — this is also how dead connections are reaped.
     */
    private void sendKeepAlive( )
    {
        for ( SseSubscription subscription : _activeSubscriptions.values( ) )
        {
            SseEventSink eventSink = subscription.getEventSink( );
            if ( eventSink.isClosed( ) )
            {
                unregisterSseStream( subscription.getSubscriptionId( ) );
                continue;
            }
            OutboundSseEvent comment = subscription.getSse( ).newEventBuilder( ).comment( KEEPALIVE_COMMENT ).build( );
            final String subscriptionId = subscription.getSubscriptionId( );
            eventSink.send( comment ).thenRun( ( ) -> { } ).exceptionally( throwable -> handleFailedSend( subscriptionId, throwable ) );
        }
    }

    /**
     * Checks if a subscription matches a client user.
     *
     * @param subscription
     *            the subscription to check
     * @param clientId
     *            the client identifier
     * @param userId
     *            the user identifier
     * @return true if the subscription matches the client user
     */
    private boolean matchesClientUser( SseSubscription subscription, int clientId, String userId )
    {
        return subscription.getClientId( ) == clientId && userId.equals( subscription.getUserId( ) );
    }
}
