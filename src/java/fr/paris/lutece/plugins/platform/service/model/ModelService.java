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
package fr.paris.lutece.plugins.platform.service.model;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.azure.core.http.ProxyOptions;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.embedding.EmbeddingModel;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderVendorConstants;
import fr.paris.lutece.portal.service.util.AppLogService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.paris.lutece.plugins.platform.service.concurrent.SharedHttpClientFactory;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for creating various language models
 */
@ApplicationScoped
@Named( "platform.modelService" )
public class ModelService
{
    private static final String ERROR_PROVIDER_NULL_TRACKED_CHAT = "Provider is null in createTrackedChatModel. Execution ID: ";
    private static final String ERROR_EXECUTION_ID_NULL = "executionId is null in createTrackedChatModel. Creating an untracked model.";
    private static final String ERROR_LLM_NODE_ID_NULL = "llmNodeId is null in createTrackedChatModel. ExecutionId: ";
    private static final String ERROR_PROVIDER_NULL_TRACKED_STREAMING = "Provider is null in createTrackedStreamingChatModel. Execution ID: ";
    private static final String ERROR_PROVIDER_NULL_EMBEDDING = "Provider is null in createEmbeddingModel. Cannot create the model.";

    private static final String EXCEPTION_PROVIDER_NULL_TRACKED_CHAT = "Provider cannot be null to create a tracked ChatModel.";
    private static final String EXCEPTION_LLM_NODE_ID_NULL = "llmNodeId cannot be null for a tracked model.";
    private static final String EXCEPTION_PROVIDER_NULL_TRACKED_STREAMING = "Provider cannot be null to create a tracked StreamingChatModel.";
    private static final String EXCEPTION_PROVIDER_NULL_EMBEDDING = "Provider cannot be null to create an EmbeddingModel.";
    private static final String EXCEPTION_EXECUTION_ID_NULL = "executionId cannot be null for a tracked model.";

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 32000;

    private static final String ERROR_PROXY_MISCONFIGURED = "Proxy is enabled but host or port is not configured properly";
    private static final String NODE_ID_API_CHAT_PREFIX = "model-api-chat-";
    private static final String NODE_ID_API_STREAMING_PREFIX = "model-api-streaming-";

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.enabled", defaultValue = "false" )
    private boolean proxyEnabled;

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.host" )
    private Optional<String> proxyHost;

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.port" )
    private Optional<String> proxyPortStr;

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.type", defaultValue = "HTTP" )
    private String proxyType;

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.username" )
    private Optional<String> proxyUsername;

    @Inject
    @ConfigProperty( name = "platform.agent.model.proxy.password" )
    private Optional<String> proxyPassword;

    @Resource( lookup = "java:comp/DefaultContextService" )
    private ContextService _contextService;

    @Inject
    private SharedHttpClientFactory _sharedHttpClientFactory;

    /**
     * Creates ProxyOptions from configuration if proxy is enabled
     *
     * @return ProxyOptions if proxy is enabled, null otherwise
     */
    private ProxyOptions createProxyOptions( )
    {
        if ( !proxyEnabled )
        {
            return null;
        }

        String host = proxyHost.orElse( "" );
        String portStr = proxyPortStr.orElse( "" );
        if ( host.isEmpty( ) || portStr.isEmpty( ) )
        {
            AppLogService.error( ERROR_PROXY_MISCONFIGURED );
            return null;
        }

        try
        {
            int proxyPort = Integer.parseInt( portStr );

            ProxyOptions.Type type = ProxyOptions.Type.valueOf( proxyType.toUpperCase( ) );
            ProxyOptions proxyOptions = new ProxyOptions( type, new InetSocketAddress( host, proxyPort ) );

            String username = proxyUsername.orElse( "" );
            String password = proxyPassword.orElse( "" );
            if ( !username.isEmpty( ) && !password.isEmpty( ) )
            {
                proxyOptions.setCredentials( username, password );
            }

            return proxyOptions;
        }
        catch( NumberFormatException e )
        {
            AppLogService.error( "Invalid proxy port configuration: {}", portStr, e );
            return null;
        }
        catch( IllegalArgumentException e )
        {
            AppLogService.error( "Invalid proxy type configuration", e );
            return null;
        }
    }

    /**
     * Creates a chat language model with cost tracking capability
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @param temperature
     *            The temperature parameter for the model (0.0 to 1.0)
     * @return A configured ChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or llmNodeId is null
     */
    public ChatModel createTrackedChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder, double temperature )
    {
        CostTrackingListener listener = buildTrackingListener( provider, executionId, llmNodeId, nodeOrder );

        if ( ProviderVendorConstants.VENDOR_ANTHROPIC.equals( provider.getProviderVendor( ) ) )
        {
            return createAnthropicChatModel( provider, temperature, listener );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralChatModel( provider, temperature, listener );
        }
        return createAzureOpenAiChatModel( provider, temperature, listener );
    }

    /**
     * Builds an Azure OpenAI chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Azure OpenAI ChatModel
     */
    private ChatModel createAzureOpenAiChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder( ).endpoint( provider.getDeploymentEndpoint( ) )
                .apiKey( provider.getDeploymentApiKey( ) ).deploymentName( provider.getDeploymentName( ) ).temperature( temperature )
                .listeners( Collections.singletonList( listener ) ).httpClientProvider( _sharedHttpClientFactory.azureProvider( ) );

        if ( provider.getDeploymentApiVersion( ) != null && !provider.getDeploymentApiVersion( ).isEmpty( ) )
        {
            builder.serviceVersion( provider.getDeploymentApiVersion( ) );
        }

        ProxyOptions proxyOptions = createProxyOptions( );
        if ( proxyOptions != null )
        {
            builder.proxyOptions( proxyOptions );
        }

        return builder.build( );
    }

    /**
     * Builds an Anthropic chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Anthropic ChatModel
     */
    private ChatModel createAnthropicChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder( ).apiKey( provider.getDeploymentApiKey( ) )
                .modelName( provider.getDeploymentModelName( ) ).temperature( temperature ).maxTokens( DEFAULT_MAX_TOKENS )
                .listeners( Collections.singletonList( listener ) ).httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Creates a chat language model with cost tracking capability using default temperature
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @return A configured ChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or llmNodeId is null
     */
    public ChatModel createTrackedChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder )
    {
        return createTrackedChatModel( provider, executionId, llmNodeId, nodeOrder, DEFAULT_TEMPERATURE );
    }

    /**
     * Creates a streaming chat language model with cost tracking capability
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @param temperature
     *            The temperature parameter for the model (0.0 to 1.0)
     * @return A configured StreamingChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or llmNodeId is null
     */
    public StreamingChatModel createTrackedStreamingChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder, double temperature )
    {
        CostTrackingListener listener = buildTrackingListener( provider, executionId, llmNodeId, nodeOrder );

        if ( ProviderVendorConstants.VENDOR_ANTHROPIC.equals( provider.getProviderVendor( ) ) )
        {
            return createAnthropicStreamingChatModel( provider, temperature, listener );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralStreamingChatModel( provider, temperature, listener );
        }
        return createAzureOpenAiStreamingChatModel( provider, temperature, listener );
    }

    /**
     * Builds an Azure OpenAI streaming chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Azure OpenAI StreamingChatModel
     */
    private StreamingChatModel createAzureOpenAiStreamingChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder( ).endpoint( provider.getDeploymentEndpoint( ) )
                .apiKey( provider.getDeploymentApiKey( ) ).deploymentName( provider.getDeploymentName( ) ).temperature( temperature )
                .listeners( Collections.singletonList( listener ) ).httpClientProvider( _sharedHttpClientFactory.azureProvider( ) );

        if ( provider.getDeploymentApiVersion( ) != null && !provider.getDeploymentApiVersion( ).isEmpty( ) )
        {
            builder.serviceVersion( provider.getDeploymentApiVersion( ) );
        }

        ProxyOptions proxyOptions = createProxyOptions( );
        if ( proxyOptions != null )
        {
            builder.proxyOptions( proxyOptions );
        }

        return builder.build( );
    }

    /**
     * Builds an Anthropic streaming chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Anthropic StreamingChatModel
     */
    private StreamingChatModel createAnthropicStreamingChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder builder = AnthropicStreamingChatModel.builder( )
                .apiKey( provider.getDeploymentApiKey( ) ).modelName( provider.getDeploymentModelName( ) ).temperature( temperature )
                .maxTokens( DEFAULT_MAX_TOKENS ).listeners( Collections.singletonList( listener ) ).httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Creates a streaming chat language model with cost tracking capability using default temperature
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @return A configured StreamingChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or llmNodeId is null
     */
    public StreamingChatModel createTrackedStreamingChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder )
    {
        return createTrackedStreamingChatModel( provider, executionId, llmNodeId, nodeOrder, DEFAULT_TEMPERATURE );
    }

    /**
     * Creates a tracked JSON chat model for JSON response format
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @param temperature
     *            The temperature parameter for the model (0.0 to 1.0)
     * @return A configured ChatModel with JSON response format and cost tracking
     * @throws IllegalArgumentException
     *             if provider, executionId, or llmNodeId is null
     */
    public ChatModel createTrackedJsonChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder, double temperature )
    {
        CostTrackingListener listener = buildTrackingListener( provider, executionId, llmNodeId, nodeOrder );

        if ( ProviderVendorConstants.VENDOR_ANTHROPIC.equals( provider.getProviderVendor( ) ) )
        {
            return createAnthropicChatModel( provider, temperature, listener );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralJsonChatModel( provider, temperature, listener );
        }
        return createAzureOpenAiJsonChatModel( provider, temperature, listener );
    }

    /**
     * Builds an Azure OpenAI chat model configured with a JSON response format
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Azure OpenAI ChatModel using the JSON response format
     */
    private ChatModel createAzureOpenAiJsonChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder( ).endpoint( provider.getDeploymentEndpoint( ) )
                .apiKey( provider.getDeploymentApiKey( ) ).deploymentName( provider.getDeploymentName( ) ).temperature( temperature )
                .responseFormat( ResponseFormat.JSON ).listeners( Collections.singletonList( listener ) )
                .httpClientProvider( _sharedHttpClientFactory.azureProvider( ) );

        if ( provider.getDeploymentApiVersion( ) != null && !provider.getDeploymentApiVersion( ).isEmpty( ) )
        {
            builder.serviceVersion( provider.getDeploymentApiVersion( ) );
        }

        ProxyOptions proxyOptions = createProxyOptions( );
        if ( proxyOptions != null )
        {
            builder.proxyOptions( proxyOptions );
        }

        return builder.build( );
    }

    /**
     * Creates a tracked JSON chat model for JSON response format using default temperature
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @return A configured ChatModel with JSON response format and cost tracking
     * @throws IllegalArgumentException
     *             if provider, executionId, or llmNodeId is null
     */
    public ChatModel createTrackedJsonChatModel( Provider provider, String executionId, String llmNodeId, int nodeOrder )
    {
        return createTrackedJsonChatModel( provider, executionId, llmNodeId, nodeOrder, DEFAULT_TEMPERATURE );
    }

    /**
     * Creates an embedding model for vector representations
     *
     * @param provider
     *            The provider containing deployment information
     * @return A configured EmbeddingModel
     * @throws IllegalArgumentException
     *             if provider is null
     */
    public EmbeddingModel createEmbeddingModel( Provider provider )
    {
        if ( provider == null )
        {
            AppLogService.error( ERROR_PROVIDER_NULL_EMBEDDING );
            throw new IllegalArgumentException( EXCEPTION_PROVIDER_NULL_EMBEDDING );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralEmbeddingModel( provider );
        }
        AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder( ).endpoint( provider.getDeploymentEndpoint( ) )
                .apiKey( provider.getDeploymentApiKey( ) ).deploymentName( provider.getDeploymentName( ) )
                .httpClientProvider( _sharedHttpClientFactory.azureProvider( ) );

        if ( provider.getDeploymentApiVersion( ) != null && !provider.getDeploymentApiVersion( ).isEmpty( ) )
        {
            builder.serviceVersion( provider.getDeploymentApiVersion( ) );
        }

        ProxyOptions proxyOptions = createProxyOptions( );
        if ( proxyOptions != null )
        {
            builder.proxyOptions( proxyOptions );
        }

        return builder.build( );
    }

    /**
     * Creates a chat language model with cost tracking for REST API calls
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking (typically the streamId or operationId from the API)
     * @param temperature
     *            The temperature parameter for the model (0.0 to 1.0)
     * @return A configured ChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or executionId is null
     */
    public ChatModel createApiChatModel( Provider provider, String executionId, double temperature )
    {
        if ( provider == null )
        {
            AppLogService.error( "{}{}", ERROR_PROVIDER_NULL_TRACKED_CHAT, executionId );
            throw new IllegalArgumentException( EXCEPTION_PROVIDER_NULL_TRACKED_CHAT );
        }
        if ( executionId == null )
        {
            AppLogService.error( ERROR_EXECUTION_ID_NULL );
            throw new IllegalArgumentException( EXCEPTION_EXECUTION_ID_NULL );
        }
        String nodeId = NODE_ID_API_CHAT_PREFIX + executionId;
        Executor ctxExecutor = _contextService.currentContextExecutor( );
        CostTrackingListener listener = new CostTrackingListener( executionId, nodeId, provider.getTokenInputPrice1M( ), provider.getTokenOutputPrice1M( ),
                provider.getDeploymentModelName( ), 1, ctxExecutor );

        if ( ProviderVendorConstants.VENDOR_ANTHROPIC.equals( provider.getProviderVendor( ) ) )
        {
            return createAnthropicChatModel( provider, temperature, listener );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralChatModel( provider, temperature, listener );
        }
        return createAzureOpenAiChatModel( provider, temperature, listener );
    }

    /**
     * Creates a streaming chat language model with cost tracking for REST API calls
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking (typically the streamId or operationId from the API)
     * @param temperature
     *            The temperature parameter for the model (0.0 to 1.0)
     * @return A configured StreamingChatModel with cost tracking
     * @throws IllegalArgumentException
     *             if provider or executionId is null
     */
    public StreamingChatModel createApiStreamingChatModel( Provider provider, String executionId, double temperature )
    {
        if ( provider == null )
        {
            AppLogService.error( "{}{}", ERROR_PROVIDER_NULL_TRACKED_STREAMING, executionId );
            throw new IllegalArgumentException( EXCEPTION_PROVIDER_NULL_TRACKED_STREAMING );
        }
        if ( executionId == null )
        {
            AppLogService.error( ERROR_EXECUTION_ID_NULL );
            throw new IllegalArgumentException( EXCEPTION_EXECUTION_ID_NULL );
        }
        String nodeId = NODE_ID_API_STREAMING_PREFIX + executionId;
        Executor ctxExecutor = _contextService.currentContextExecutor( );
        CostTrackingListener listener = new CostTrackingListener( executionId, nodeId, provider.getTokenInputPrice1M( ), provider.getTokenOutputPrice1M( ),
                provider.getDeploymentModelName( ), 1, ctxExecutor );

        if ( ProviderVendorConstants.VENDOR_ANTHROPIC.equals( provider.getProviderVendor( ) ) )
        {
            return createAnthropicStreamingChatModel( provider, temperature, listener );
        }
        if ( ProviderVendorConstants.VENDOR_MISTRAL.equals( provider.getProviderVendor( ) ) )
        {
            return createMistralStreamingChatModel( provider, temperature, listener );
        }
        return createAzureOpenAiStreamingChatModel( provider, temperature, listener );
    }

    /**
     * Builds a Mistral chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Mistral ChatModel
     */
    private ChatModel createMistralChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        MistralAiChatModel.MistralAiChatModelBuilder builder = MistralAiChatModel.builder( ).apiKey( provider.getDeploymentApiKey( ) )
                .modelName( provider.getDeploymentModelName( ) ).temperature( temperature ).maxTokens( DEFAULT_MAX_TOKENS )
                .listeners( Collections.singletonList( listener ) ).httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Builds a Mistral streaming chat model from the given provider, temperature and cost tracking listener
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Mistral StreamingChatModel
     */
    private StreamingChatModel createMistralStreamingChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        MistralAiStreamingChatModel.MistralAiStreamingChatModelBuilder builder = MistralAiStreamingChatModel.builder( )
                .apiKey( provider.getDeploymentApiKey( ) ).modelName( provider.getDeploymentModelName( ) ).temperature( temperature )
                .maxTokens( DEFAULT_MAX_TOKENS ).listeners( Collections.singletonList( listener ) ).httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Builds a Mistral chat model configured with a JSON response format
     *
     * @param provider
     *            the provider containing deployment information
     * @param temperature
     *            the temperature parameter for the model
     * @param listener
     *            the cost tracking listener attached to the model
     * @return a configured Mistral ChatModel using the JSON response format
     */
    private ChatModel createMistralJsonChatModel( Provider provider, double temperature, CostTrackingListener listener )
    {
        MistralAiChatModel.MistralAiChatModelBuilder builder = MistralAiChatModel.builder( ).apiKey( provider.getDeploymentApiKey( ) )
                .modelName( provider.getDeploymentModelName( ) ).temperature( temperature ).maxTokens( DEFAULT_MAX_TOKENS )
                .responseFormat( ResponseFormat.JSON ).listeners( Collections.singletonList( listener ) )
                .httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Builds a Mistral embedding model from the given provider
     *
     * @param provider
     *            the provider containing deployment information
     * @return a configured Mistral EmbeddingModel
     */
    private EmbeddingModel createMistralEmbeddingModel( Provider provider )
    {
        MistralAiEmbeddingModel.MistralAiEmbeddingModelBuilder builder = MistralAiEmbeddingModel.builder( ).apiKey( provider.getDeploymentApiKey( ) )
                .modelName( provider.getDeploymentModelName( ) ).httpClientBuilder( _sharedHttpClientFactory.builder( ) );

        if ( provider.getDeploymentEndpoint( ) != null && !provider.getDeploymentEndpoint( ).isEmpty( ) )
        {
            builder.baseUrl( provider.getDeploymentEndpoint( ) );
        }

        return builder.build( );
    }

    /**
     * Validates the tracking arguments and builds the cost tracking listener shared by every tracked model factory (chat, streaming, JSON).
     *
     * @param provider
     *            The provider containing deployment information
     * @param executionId
     *            The execution ID for tracking
     * @param llmNodeId
     *            The LLM node ID for tracking
     * @param nodeOrder
     *            The order of the node in the execution flow
     * @return the configured listener
     * @throws IllegalArgumentException
     *             if provider, executionId or llmNodeId is null
     */
    private CostTrackingListener buildTrackingListener( Provider provider, String executionId, String llmNodeId, int nodeOrder )
    {
        if ( provider == null )
        {
            AppLogService.error( "{}{}", ERROR_PROVIDER_NULL_TRACKED_CHAT, executionId );
            throw new IllegalArgumentException( EXCEPTION_PROVIDER_NULL_TRACKED_CHAT );
        }
        if ( executionId == null )
        {
            AppLogService.error( ERROR_EXECUTION_ID_NULL );
            throw new IllegalArgumentException( EXCEPTION_EXECUTION_ID_NULL );
        }
        if ( llmNodeId == null )
        {
            AppLogService.error( "{}{}", ERROR_LLM_NODE_ID_NULL, executionId );
            throw new IllegalArgumentException( EXCEPTION_LLM_NODE_ID_NULL );
        }
        Executor ctxExecutor = _contextService.currentContextExecutor( );
        return new CostTrackingListener( executionId, llmNodeId, provider.getTokenInputPrice1M( ), provider.getTokenOutputPrice1M( ),
                provider.getDeploymentModelName( ), nodeOrder, ctxExecutor );
    }
}
