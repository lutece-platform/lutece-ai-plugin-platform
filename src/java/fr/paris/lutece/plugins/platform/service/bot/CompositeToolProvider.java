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
package fr.paris.lutece.plugins.platform.service.bot;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.service.bot.tools.FindDocumentsTool;
import fr.paris.lutece.plugins.platform.service.mcp.McpToolProviderFactory;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.plugins.platform.service.bot.tools.FindFoldersTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.GrepTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.ListDocumentsTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.ListFoldersTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.ReadDocumentTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.SearchInDatasetTool;
import fr.paris.lutece.plugins.platform.service.bot.tools.SearchInDocumentTool;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolFailedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolStartedEvent;

import jakarta.enterprise.inject.spi.CDI;

/**
 * ToolProvider that aggregates pipeline tools (legacy) and built-in dataset tools. Built-in tools are added only if the bot has the
 * {@code enable_builtin_tools} flag enabled and at least one accessible dataset.
 */
public class CompositeToolProvider implements ToolProvider
{
    private static final int PREVIEW_MAX_CHARS = 500;

    private final ContextAwarePipelineToolProvider pipelineDelegate;
    private final ToolProvider mcpDelegate;
    private final Bot bot;
    private final List<Dataset> botDatasets;
    private final String operationId;
    private final jakarta.enterprise.event.Event<Object> cdiEvent;

    /**
     * Constructor
     *
     * @param bot
     *            The bot for which to provide tools
     * @param botDatasets
     *            Datasets the bot has access to
     * @param operationId
     *            Operation identifier for SSE events
     */
    public CompositeToolProvider( Bot bot, List<Dataset> botDatasets, String operationId )
    {
        this.bot = bot;
        this.botDatasets = botDatasets;
        this.operationId = operationId;
        this.pipelineDelegate = new ContextAwarePipelineToolProvider( bot, operationId );
        this.mcpDelegate = McpToolProviderFactory.buildForBot( bot.getId( ) );
        this.cdiEvent = CDI.current( ).getBeanManager( ).getEvent( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolProviderResult provideTools( ToolProviderRequest request )
    {
        ToolProviderResult.Builder builder = ToolProviderResult.builder( );
        Set<String> registeredNames = new HashSet<>( );

        ToolProviderResult pipelineResult = pipelineDelegate.provideTools( request );
        for ( AiServiceTool tool : pipelineResult.aiServiceTools( ) )
        {
            if ( registerName( registeredNames, tool.name( ) ) )
            {
                builder.add( tool );
            }
        }

        if ( bot.isEnableBuiltinTools( ) && botDatasets != null && !botDatasets.isEmpty( ) )
        {
            addBuiltinTools( builder, registeredNames );
        }

        if ( mcpDelegate != null )
        {
            ToolProviderResult mcpResult = mcpDelegate.provideTools( request );
            for ( AiServiceTool tool : mcpResult.aiServiceTools( ) )
            {
                wrapAndAdd( builder, registeredNames, tool.toolSpecification( ), tool.toolExecutor( ) );
            }
        }

        return builder.build( );
    }

    /**
     * Records a tool name, refusing duplicates. langchain4j's ToolService rejects duplicated tool names with an IllegalConfigurationException at chat time, so
     * the first registration wins (pipeline, then builtin, then MCP) and later duplicates are skipped with a warning.
     *
     * @param registeredNames
     *            The names registered so far
     * @param toolName
     *            The candidate tool name
     * @return true if the name was free and is now registered
     */
    private boolean registerName( Set<String> registeredNames, String toolName )
    {
        if ( !registeredNames.add( toolName ) )
        {
            AppLogService.error( "Duplicate tool name '{}' for bot {} — skipping the later registration", toolName, bot.getId( ) );
            return false;
        }
        return true;
    }

    /**
     * Instantiates the built-in tools and registers their @Tool methods
     *
     * @param builder
     *            The result builder being populated
     * @param registeredNames
     *            The tool names registered so far
     */
    private void addBuiltinTools( ToolProviderResult.Builder builder, Set<String> registeredNames )
    {
        registerToolsOf( builder, registeredNames, new ListFoldersTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new FindFoldersTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new ListDocumentsTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new FindDocumentsTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new ReadDocumentTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new GrepTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new SearchInDatasetTool( botDatasets ) );
        registerToolsOf( builder, registeredNames, new SearchInDocumentTool( botDatasets ) );
    }

    /**
     * Registers all @Tool methods of an object as wrapped executors that emit SSE events
     *
     * @param builder
     *            The result builder
     * @param registeredNames
     *            The tool names registered so far
     * @param toolObject
     *            The tool instance to introspect
     */
    private void registerToolsOf( ToolProviderResult.Builder builder, Set<String> registeredNames, Object toolObject )
    {
        List<ToolSpecification> specs = ToolSpecifications.toolSpecificationsFrom( toolObject );
        for ( ToolSpecification spec : specs )
        {
            Method method = findMethod( toolObject, spec.name( ) );
            if ( method == null )
            {
                continue;
            }
            wrapAndAdd( builder, registeredNames, spec, new DefaultToolExecutor( toolObject, method ) );
        }
    }

    /**
     * Wraps a raw ToolExecutor with SSE event dispatch (started/completed/failed) and registers it if its name is not already taken.
     *
     * Tool bodies run with the application context applied: the assistant builder passes the platform context executor to langchain4j via
     * {@code executeToolsConcurrently(executor)}, so CDI lookups and Home class initialization are safe inside the executor.
     *
     * @param builder
     *            The result builder
     * @param registeredNames
     *            The tool names registered so far
     * @param spec
     *            The tool specification
     * @param rawExecutor
     *            The underlying executor to wrap
     */
    private void wrapAndAdd( ToolProviderResult.Builder builder, Set<String> registeredNames, ToolSpecification spec, ToolExecutor rawExecutor )
    {
        if ( !registerName( registeredNames, spec.name( ) ) )
        {
            return;
        }
        ToolExecutor wrapped = ( request, memoryId ) -> {
            dispatchStarted( spec.name( ), request.arguments( ) );
            try
            {
                String result = rawExecutor.execute( request, memoryId );
                dispatchCompleted( spec.name( ), previewOf( result ) );
                return result;
            }
            catch( RuntimeException e )
            {
                dispatchFailed( spec.name( ), e.getMessage( ) );
                AppLogService.error( "Error executing tool {}", spec.name( ), e );
                throw e;
            }
        };
        builder.add( spec, wrapped );
    }

    /**
     * Builds a short preview of a tool result (for UI display) — truncated to keep SSE payloads small.
     *
     * @param result
     *            The full tool result, possibly null
     * @return The preview, or null when input is null
     */
    private static String previewOf( String result )
    {
        if ( result == null )
        {
            return null;
        }
        return result.length( ) > PREVIEW_MAX_CHARS ? result.substring( 0, PREVIEW_MAX_CHARS ) + "…" : result;
    }

    /**
     * Locates the @Tool method of an instance whose name matches the spec name
     *
     * @param target
     *            The tool instance
     * @param toolName
     *            The expected method/tool name
     * @return The Method or null if not found
     */
    private Method findMethod( Object target, String toolName )
    {
        for ( Method m : target.getClass( ).getDeclaredMethods( ) )
        {
            if ( !m.isAnnotationPresent( Tool.class ) )
            {
                continue;
            }
            Tool annotation = m.getAnnotation( Tool.class );
            String declared = annotation.name( ) == null || annotation.name( ).isEmpty( ) ? m.getName( ) : annotation.name( );
            if ( declared.equals( toolName ) )
            {
                return m;
            }
        }
        return null;
    }

    /**
     * Dispatches BUILTIN_TOOL_STARTED event with the tool arguments as received from the LLM.
     *
     * @param toolName
     *            The tool name
     * @param arguments
     *            The JSON arguments sent by the LLM
     */
    private void dispatchStarted( String toolName, String arguments )
    {
        cdiEvent.fire( BotToolStartedEvent.forBuiltin( operationId, bot.getId( ), toolName, arguments ) );
    }

    /**
     * Dispatches BUILTIN_TOOL_COMPLETED event with the truncated result preview for UI display.
     *
     * @param toolName
     *            The tool name
     * @param resultPreview
     *            A truncated preview of the tool result
     */
    private void dispatchCompleted( String toolName, String resultPreview )
    {
        cdiEvent.fire( BotToolCompletedEvent.forBuiltin( operationId, bot.getId( ), toolName, resultPreview ) );
    }

    /**
     * Dispatches BUILTIN_TOOL_FAILED event with the exception message.
     *
     * @param toolName
     *            The tool name
     * @param errorMessage
     *            The exception message
     */
    private void dispatchFailed( String toolName, String errorMessage )
    {
        cdiEvent.fire( BotToolFailedEvent.forBuiltin( operationId, bot.getId( ), toolName, errorMessage ) );
    }
}
