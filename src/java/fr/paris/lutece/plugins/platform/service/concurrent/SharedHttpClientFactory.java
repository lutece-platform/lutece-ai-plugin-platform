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

import com.azure.core.http.HttpClientProvider;
import com.azure.core.util.HttpClientOptions;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;

import fr.paris.lutece.portal.service.util.AppPropertiesService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Produces a singleton {@link HttpClient} (langchain4j abstraction) backed by a single shared {@link java.net.http.HttpClient}.
 *
 * <p>
 * The naive pattern (creating a new {@code java.net.http.HttpClient} per chat-model instantiation) leaks one NIO SelectorManager thread and one connection pool
 * per call. This factory provides a single shared instance: one SelectorManager total, one connection pool reused across providers.
 * </p>
 *
 * <p>
 * No custom executor is supplied to the underlying client: it keeps the JDK's internal, self-throttling cached thread pool. Passing the bounded
 * {@link BlockingIO} executor here is an anti-pattern — a single {@code RejectedExecutionException} from a bounded executor permanently shuts down the client's
 * {@code SelectorManager} (JDK-8277969), bricking every subsequent outbound call until redeploy. Concurrency back-pressure toward the database is applied at
 * request admission (the {@code @BlockingIO} {@code runAsync} in the query services), not on the HTTP transport. Sharing one client already guarantees a single
 * pool.
 * </p>
 *
 * <p>
 * {@link java.net.http.HttpClient} is documented thread-safe; sharing one instance across providers is safe and recommended by the JDK.
 * </p>
 *
 * <p>
 * Usage in langchain4j builders:
 *
 * <pre>{@code
 * MistralAiStreamingChatModel.builder()
 *     .httpClientBuilder( sharedHttpClientFactory.builder() )
 *     ...
 * }</pre>
 * </p>
 */
@ApplicationScoped
public class SharedHttpClientFactory
{
    private static final String PROPERTY_CONNECT_TIMEOUT_SECONDS = "platform.http.connectTimeoutSeconds";
    private static final String PROPERTY_READ_TIMEOUT_SECONDS = "platform.http.readTimeoutSeconds";
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 300;

    private java.net.http.HttpClient _sharedJavaHttpClient;

    private Duration _defaultReadTimeout;

    private com.azure.core.http.HttpClient _sharedAzureHttpClient;

    private HttpClientProvider _azureProvider;

    /**
     * Initializes the shared underlying clients once, at CDI bean activation. Both the raw JDK HTTP client (Mistral/Anthropic via langchain4j) and the Azure
     * JDK HTTP client keep the JDK's internal self-throttling executor; HTTP/1.1 is forced to avoid the h2c upgrade that some endpoints reject. The connect
     * timeout is a property of the shared client; the read timeout default is applied per request by the builds of {@link #builder()}. A single Azure
     * {@link HttpClientProvider} facade is created which always returns the same shared Azure {@link com.azure.core.http.HttpClient}.
     */
    @PostConstruct
    void init( )
    {
        int connectSeconds = AppPropertiesService.getPropertyInt( PROPERTY_CONNECT_TIMEOUT_SECONDS, DEFAULT_CONNECT_TIMEOUT_SECONDS );
        _sharedJavaHttpClient = java.net.http.HttpClient.newBuilder( ).version( java.net.http.HttpClient.Version.HTTP_1_1 )
                .connectTimeout( Duration.ofSeconds( connectSeconds ) ).build( );
        _defaultReadTimeout = Duration.ofSeconds( AppPropertiesService.getPropertyInt( PROPERTY_READ_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS ) );

        _sharedAzureHttpClient = new com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder( ).build( );
        _azureProvider = new SharedAzureHttpClientProvider( _sharedAzureHttpClient );
    }

    /**
     * Returns the shared raw {@link java.net.http.HttpClient}, for services that issue HTTP calls directly (e.g. the Mistral OCR parser) so they reuse the
     * single SelectorManager and connection pool. Callers set their own per-request timeout.
     *
     * @return the shared JDK HttpClient
     */
    public java.net.http.HttpClient javaHttpClient( )
    {
        return _sharedJavaHttpClient;
    }

    /**
     * Returns the singleton Azure {@link HttpClientProvider}, used by langchain4j-azure-open-ai builders via their {@code .httpClientProvider(...)} method.
     *
     * @return the shared Azure HttpClientProvider
     */
    public HttpClientProvider azureProvider( )
    {
        return _azureProvider;
    }

    /**
     * Returns the singleton Azure {@link com.azure.core.http.HttpClient}, for Azure SDK builders that expose {@code .httpClient(HttpClient)} directly (e.g.
     * {@code DocumentIntelligenceClientBuilder}).
     *
     * @return the shared Azure HttpClient
     */
    public com.azure.core.http.HttpClient azureHttpClient( )
    {
        return _sharedAzureHttpClient;
    }

    /**
     * Returns a per-call {@link HttpClientBuilder} whose {@link HttpClientBuilder#build()} produces a lightweight {@code JdkHttpClient} wrapper sharing the
     * single underlying {@link java.net.http.HttpClient} (one SelectorManager, one connection pool) while honoring the per-build {@code readTimeout} on every
     * request. A build without an explicit read timeout gets the factory default, so no LLM call ever runs unbounded. The connect timeout is fixed on the
     * shared client and per-build values are ignored.
     *
     * @return a builder facade that shares the underlying HttpClient
     */
    public HttpClientBuilder builder( )
    {
        return new SharedJdkHttpClientBuilder( _sharedJavaHttpClient, _defaultReadTimeout );
    }

    /**
     * Azure {@link HttpClientProvider} facade that always returns the same shared {@link com.azure.core.http.HttpClient}. Required because
     * langchain4j-azure-open-ai invokes {@code httpClientProvider.createInstance(...)} once per chat-model build, which would otherwise create a fresh HTTP
     * client (and SelectorManager thread) each time.
     */
    private static final class SharedAzureHttpClientProvider implements HttpClientProvider
    {
        private final com.azure.core.http.HttpClient _client;

        SharedAzureHttpClientProvider( com.azure.core.http.HttpClient client )
        {
            _client = client;
        }

        /**
         * Returns the shared Azure HttpClient, ignoring per-invocation configuration.
         *
         * @return the shared HttpClient
         */
        @Override
        public com.azure.core.http.HttpClient createInstance( )
        {
            return _client;
        }

        /**
         * Returns the shared Azure HttpClient, ignoring per-invocation options.
         *
         * @param clientOptions
         *            the requested options (ignored)
         * @return the shared HttpClient
         */
        @Override
        public com.azure.core.http.HttpClient createInstance( HttpClientOptions clientOptions )
        {
            return _client;
        }
    }

    /**
     * Builder facade required by langchain4j chat-model builders. Each {@link #build()} produces a lightweight {@code JdkHttpClient} wrapper that reuses the
     * shared {@link java.net.http.HttpClient} delegate while enforcing the configured (or default) read timeout on every request.
     */
    private static final class SharedJdkHttpClientBuilder implements HttpClientBuilder
    {
        private final java.net.http.HttpClient _sharedClient;
        private final Duration _defaultReadTimeout;
        private Duration _connectTimeout;
        private Duration _readTimeout;

        SharedJdkHttpClientBuilder( java.net.http.HttpClient sharedClient, Duration defaultReadTimeout )
        {
            _sharedClient = sharedClient;
            _defaultReadTimeout = defaultReadTimeout;
        }

        /**
         * Returns the connect timeout configured via {@link #connectTimeout(Duration)}.
         *
         * @return the connect timeout, or null if unset
         */
        @Override
        public Duration connectTimeout( )
        {
            return _connectTimeout;
        }

        /**
         * Stores the connect timeout for langchain4j API compatibility. The shared HttpClient ignores it — connect timeouts are properties of the underlying
         * client and cannot be changed per call.
         *
         * @param timeout
         *            the connect timeout
         * @return this builder
         */
        @Override
        public HttpClientBuilder connectTimeout( Duration timeout )
        {
            _connectTimeout = timeout;
            return this;
        }

        /**
         * Returns the read timeout configured via {@link #readTimeout(Duration)}.
         *
         * @return the read timeout, or null if unset
         */
        @Override
        public Duration readTimeout( )
        {
            return _readTimeout;
        }

        /**
         * Stores the read timeout, enforced per request by the client returned by {@link #build()}.
         *
         * @param timeout
         *            the read timeout
         * @return this builder
         */
        @Override
        public HttpClientBuilder readTimeout( Duration timeout )
        {
            _readTimeout = timeout;
            return this;
        }

        /**
         * Builds a lightweight {@code JdkHttpClient} wrapper sharing the underlying {@link java.net.http.HttpClient} and carrying the effective read timeout
         * (per-build value, or the factory default when unset).
         *
         * @return a timeout-aware HTTP client backed by the shared JDK client
         */
        @Override
        public HttpClient build( )
        {
            Duration effectiveReadTimeout = _readTimeout != null ? _readTimeout : _defaultReadTimeout;
            return new JdkHttpClientBuilder( ).httpClientBuilder( new SharingJavaHttpClientBuilder( _sharedClient ) ).readTimeout( effectiveReadTimeout )
                    .build( );
        }
    }

    /**
     * {@link java.net.http.HttpClient.Builder} whose {@link #build()} returns the shared client instead of creating a new one, so each langchain4j
     * {@code JdkHttpClient} wrapper reuses the single SelectorManager and connection pool. All configuration setters are ignored: the shared client is
     * configured once at factory init.
     */
    private static final class SharingJavaHttpClientBuilder implements java.net.http.HttpClient.Builder
    {
        private final java.net.http.HttpClient _sharedClient;

        SharingJavaHttpClientBuilder( java.net.http.HttpClient sharedClient )
        {
            _sharedClient = sharedClient;
        }

        /**
         * Ignores the cookie handler: the shared client is configured at factory init.
         *
         * @param cookieHandler
         *            the cookie handler (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder cookieHandler( CookieHandler cookieHandler )
        {
            return this;
        }

        /**
         * Ignores the connect timeout: it is a property of the shared client.
         *
         * @param duration
         *            the connect timeout (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder connectTimeout( Duration duration )
        {
            return this;
        }

        /**
         * Ignores the SSL context: the shared client is configured at factory init.
         *
         * @param sslContext
         *            the SSL context (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder sslContext( SSLContext sslContext )
        {
            return this;
        }

        /**
         * Ignores the SSL parameters: the shared client is configured at factory init.
         *
         * @param sslParameters
         *            the SSL parameters (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder sslParameters( SSLParameters sslParameters )
        {
            return this;
        }

        /**
         * Ignores the executor: the shared client keeps the JDK's internal executor (see the factory's class Javadoc, JDK-8277969).
         *
         * @param executor
         *            the executor (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder executor( Executor executor )
        {
            return this;
        }

        /**
         * Ignores the redirect policy: the shared client is configured at factory init.
         *
         * @param policy
         *            the redirect policy (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder followRedirects( java.net.http.HttpClient.Redirect policy )
        {
            return this;
        }

        /**
         * Ignores the HTTP version: the shared client forces HTTP/1.1 at factory init.
         *
         * @param version
         *            the HTTP version (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder version( java.net.http.HttpClient.Version version )
        {
            return this;
        }

        /**
         * Ignores the priority: the shared client is configured at factory init.
         *
         * @param priority
         *            the HTTP/2 priority (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder priority( int priority )
        {
            return this;
        }

        /**
         * Ignores the proxy selector: the shared client is configured at factory init.
         *
         * @param proxySelector
         *            the proxy selector (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder proxy( ProxySelector proxySelector )
        {
            return this;
        }

        /**
         * Ignores the authenticator: the shared client is configured at factory init.
         *
         * @param authenticator
         *            the authenticator (ignored)
         * @return this builder
         */
        @Override
        public java.net.http.HttpClient.Builder authenticator( Authenticator authenticator )
        {
            return this;
        }

        /**
         * Returns the shared client instead of building a new one.
         *
         * @return the shared java.net.http.HttpClient
         */
        @Override
        public java.net.http.HttpClient build( )
        {
            return _sharedClient;
        }
    }
}
