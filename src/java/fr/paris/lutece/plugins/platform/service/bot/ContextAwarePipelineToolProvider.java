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

import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolCompletedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolFailedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.BotToolStartedEvent;
import fr.paris.lutece.plugins.platform.service.event.domain.PlatformDomainEvent;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Context-aware wrapper for {@link PipelineToolProvider} that fires typed CDI events around each tool execution.
 */
public class ContextAwarePipelineToolProvider implements ToolProvider
{

    private final PipelineToolProvider delegate;
    private final Bot bot;
    private final String operationId;
    private final jakarta.enterprise.event.Event<Object> cdiEvent;

    /**
     * Constructor.
     *
     * @param bot
     *            the bot owning the pipeline tools
     * @param operationId
     *            the SSE operation identifier
     */
    public ContextAwarePipelineToolProvider( Bot bot, String operationId )
    {
        this.delegate = new PipelineToolProvider( bot );
        this.bot = bot;
        this.operationId = operationId;
        this.cdiEvent = CDI.current( ).getBeanManager( ).getEvent( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolProviderResult provideTools( ToolProviderRequest toolProviderRequest )
    {
        ToolProviderResult originalResult = delegate.provideTools( toolProviderRequest );
        ToolProviderResult.Builder resultBuilder = ToolProviderResult.builder( );

        for ( AiServiceTool tool : originalResult.aiServiceTools( ) )
        {
            ToolSpecification spec = tool.toolSpecification( );
            ToolExecutor originalExecutor = tool.toolExecutor( );

            ToolExecutor wrappedExecutor = ( toolExecutionRequest, memoryId ) -> {
                fire( BotToolStartedEvent.forPipeline( operationId, bot.getId( ), spec.name( ), null ) );
                try
                {
                    String result = originalExecutor.execute( toolExecutionRequest, memoryId );
                    fire( BotToolCompletedEvent.forPipeline( operationId, bot.getId( ), spec.name( ), null ) );
                    return result;
                }
                catch( RuntimeException e )
                {
                    fire( BotToolFailedEvent.forPipeline( operationId, bot.getId( ), spec.name( ), e.getMessage( ) ) );
                    throw e;
                }
            };

            resultBuilder.add( spec, wrappedExecutor );
        }

        return resultBuilder.build( );
    }

    /**
     * Fires a typed domain event via the CDI Event instance captured at construction time. Tool executors run on unmanaged threads (langchain4j ForkJoinPool)
     * where {@code CDI.current()} fails — the Event is therefore resolved on the managed constructor thread and reused. Safe because the only observer is
     * {@code @ApplicationScoped} (no request context needed for a synchronous fire).
     *
     * @param event
     *            the event to fire
     */
    private void fire( PlatformDomainEvent event )
    {
        cdiEvent.fire( event );
    }
}
