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

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Timeout contract of {@link SharedHttpClientFactory}: the {@code readTimeout} configured by a langchain4j model builder on the facade MUST be enforced per
 * request — otherwise every LLM call runs without any timeout and a hanging endpoint blocks a {@code @BlockingIO} thread forever. The other half of the
 * contract is preserved sharing: every built client must reuse the same underlying {@link java.net.http.HttpClient} (single connection pool and selector
 * thread).
 */
public class SharedHttpClientFactoryTimeoutTest extends AbstractPlatformDbTest
{
    private HttpServer _server;

    /**
     * Starts a local HTTP server whose handler answers after a 2-second delay.
     *
     * @throws Exception
     *             if the server cannot start
     */
    @BeforeEach
    public void startSlowServer( ) throws Exception
    {
        _server = HttpServer.create( new InetSocketAddress( 0 ), 0 );
        _server.createContext( "/slow", exchange -> {
            try
            {
                Thread.sleep( 2000 );
            }
            catch( InterruptedException e )
            {
                Thread.currentThread( ).interrupt( );
            }
            byte [ ] body = "ok".getBytes( );
            exchange.sendResponseHeaders( 200, body.length );
            try ( OutputStream os = exchange.getResponseBody( ) )
            {
                os.write( body );
            }
        } );
        _server.start( );
    }

    /**
     * Stops the local server.
     */
    @AfterEach
    public void stopServer( )
    {
        _server.stop( 0 );
    }

    /**
     * A readTimeout configured on the facade builder is enforced on the request: a server slower than the timeout must produce a langchain4j TimeoutException,
     * not a successful response.
     */
    @Test
    public void testReadTimeoutIsEnforcedPerRequest( )
    {
        SharedHttpClientFactory factory = CDI.current( ).select( SharedHttpClientFactory.class ).get( );
        HttpClient client = factory.builder( ).readTimeout( Duration.ofMillis( 200 ) ).build( );

        HttpRequest request = HttpRequest.builder( ).method( HttpMethod.GET ).url( serverUrl( ) ).build( );

        assertThrows( TimeoutException.class, ( ) -> client.execute( request ),
                "the readTimeout configured by the provider was discarded: the LLM call has no timeout at all" );
    }

    /**
     * Every client produced by the facade shares the same underlying java.net.http.HttpClient (one connection pool, one selector thread) — the reason the
     * factory exists.
     */
    @Test
    public void testBuiltClientsShareTheUnderlyingJdkClient( ) throws Exception
    {
        SharedHttpClientFactory factory = CDI.current( ).select( SharedHttpClientFactory.class ).get( );

        HttpClient first = factory.builder( ).readTimeout( Duration.ofSeconds( 30 ) ).build( );
        HttpClient second = factory.builder( ).readTimeout( Duration.ofSeconds( 60 ) ).build( );

        assertSame( delegateOf( first ), delegateOf( second ), "built clients must share the same underlying java.net.http.HttpClient" );
    }

    /**
     * Builds the URL of the slow endpoint.
     *
     * @return the endpoint URL
     */
    private String serverUrl( )
    {
        return "http://localhost:" + _server.getAddress( ).getPort( ) + "/slow";
    }

    /**
     * Extracts the underlying java.net.http.HttpClient delegate of a langchain4j JdkHttpClient (field pinned by the langchain4j version of the pom).
     *
     * @param client
     *            the langchain4j client
     * @return the JDK delegate
     * @throws Exception
     *             if reflection fails
     */
    private java.net.http.HttpClient delegateOf( HttpClient client ) throws Exception
    {
        Field field = JdkHttpClient.class.getDeclaredField( "delegate" );
        field.setAccessible( true );
        return (java.net.http.HttpClient) field.get( client );
    }
}
