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
package fr.paris.lutece.plugins.platform.rs.util;

import fr.paris.lutece.portal.service.util.AppLogService;

import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Utility class for SSE REST operations
 */
public final class SseRestUtils
{

    private static final String ERROR_EVENT_NAME = "error";
    private static final String ERROR_KEY = "error";
    private static final String LOG_ERROR_SENDING_EVENT = "Error sending error event to SSE";
    private static final String LOG_ERROR_CLOSING_SINK = "Error closing SSE event sink";

    /**
     * Private constructor
     */
    private SseRestUtils( )
    {
    }

    /**
     * Sends an error event to the SSE event sink and closes it. If the sink is null or already closed, does nothing.
     *
     * @param eventSink
     *            the SSE event sink to send the error to and close
     * @param sse
     *            the SSE context used to build the outbound event
     * @param errorMessage
     *            the error message to include in the event payload
     */
    public static void closeEventSinkWithError( SseEventSink eventSink, Sse sse, String errorMessage )
    {
        if ( eventSink != null && !eventSink.isClosed( ) )
        {
            try
            {
                OutboundSseEvent errorEvent = sse.newEventBuilder( ).name( ERROR_EVENT_NAME ).data( Map.of( ERROR_KEY, errorMessage ) )
                        .mediaType( MediaType.APPLICATION_JSON_TYPE ).build( );
                eventSink.send( errorEvent ).whenComplete( ( result, error ) -> eventSink.close( ) );
            }
            catch( Exception e )
            {
                AppLogService.error( LOG_ERROR_SENDING_EVENT, e );
                try
                {
                    eventSink.close( );
                }
                catch( Exception ex )
                {
                    AppLogService.error( LOG_ERROR_CLOSING_SINK, ex );
                }
            }
        }
    }
}
