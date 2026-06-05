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

import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVariableDTO;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.bot.BotPipeline;
import fr.paris.lutece.plugins.platform.business.bot.BotPipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersionHome;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableConverter;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tool provider for dynamic pipeline tools
 */
public class PipelineToolProvider implements ToolProvider
{
    private final Bot _bot;

    /**
     * Constructor
     *
     * @param bot
     *            the bot for which to provide pipeline tools
     */
    public PipelineToolProvider( Bot bot )
    {
        _bot = bot;
    }

    /**
     * Provides tools based on the pipelines associated with the bot
     *
     * @param toolProviderRequest
     *            the tool provider request
     * @return the tool provider result containing the available tools
     */
    @Override
    public ToolProviderResult provideTools( ToolProviderRequest toolProviderRequest )
    {
        ToolProviderResult.Builder resultBuilder = ToolProviderResult.builder( );
        Set<String> registeredNames = new HashSet<>( );

        try
        {
            List<BotPipeline> botPipelines = BotPipelineHome.getBotPipelinesByBotId( _bot.getId( ) );

            for ( BotPipeline botPipeline : botPipelines )
            {
                try
                {
                    Optional<Pipeline> optPipeline = PipelineHome.findByPrimaryKey( botPipeline.getPipelineId( ) );
                    Pipeline pipeline = optPipeline.orElse( null );
                    if ( pipeline != null && !registeredNames.add( createToolName( pipeline.getName( ) ) ) )
                    {
                        AppLogService.error( "Duplicate pipeline tool name '{}' for bot {} (pipeline {}) — skipping", createToolName( pipeline.getName( ) ),
                                _bot.getId( ), pipeline.getId( ) );
                        continue;
                    }
                    if ( pipeline != null )
                    {
                        ToolSpecification toolSpec = createToolSpecification( pipeline, botPipeline );

                        PipelineToolExecutor pipelineExecutor = new PipelineToolExecutor( pipeline.getId( ) );
                        ToolExecutor toolExecutor = ( toolExecutionRequest, memoryId ) -> {
                            String result = pipelineExecutor.execute( toolExecutionRequest, memoryId );
                            return result;
                        };

                        resultBuilder.add( toolSpec, toolExecutor );
                    }
                }
                catch( Exception e )
                {
                    AppLogService.error( "Error creating tool for pipeline {}", botPipeline.getPipelineId( ), e );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error providing pipeline tools for bot {}", _bot.getId( ), e );
        }

        return resultBuilder.build( );
    }

    /**
     * Creates a tool specification for a pipeline
     *
     * @param pipeline
     *            the pipeline
     * @param botPipeline
     *            the bot-pipeline association
     * @return the tool specification
     */
    private ToolSpecification createToolSpecification( Pipeline pipeline, BotPipeline botPipeline )
    {
        JsonObjectSchema parameters;
        try
        {
            PipelineVersion currentVersion = PipelineVersionHome.findCurrentVersion( pipeline.getId( ) ).orElse( null );

            if ( currentVersion != null )
            {
                List<PipelineVariableDTO> requiredInputs = PipelineVariableConverter.resolveInputVariables( currentVersion );

                JsonObjectSchema.Builder parametersBuilder = JsonObjectSchema.builder( );
                for ( PipelineVariableDTO variable : requiredInputs )
                {
                    String description = variable.getDescription( ) != null ? variable.getDescription( ) : variable.getName( );
                    JsonSchemaElement property = createJsonSchemaElement( variable.getType( ), description );
                    parametersBuilder.addProperty( variable.getName( ), property );
                }

                parameters = parametersBuilder.build( );
            }
            else
            {
                parameters = JsonObjectSchema.builder( ).build( );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error extracting pipeline inputs for pipeline {}", pipeline.getId( ), e );
            parameters = JsonObjectSchema.builder( ).build( );
        }

        String toolName = createToolName( pipeline.getName( ) );
        String toolDescription = botPipeline.getToolDescription( ) != null && !botPipeline.getToolDescription( ).trim( ).isEmpty( )
                ? botPipeline.getToolDescription( )
                : pipeline.getDescription( );

        return ToolSpecification.builder( ).name( toolName ).description( toolDescription ).parameters( parameters ).build( );
    }

    /**
     * Creates a valid tool name from pipeline name
     *
     * @param pipelineName
     *            the pipeline name
     * @return the tool name
     */
    private String createToolName( String pipelineName )
    {
        return "pipeline_" + pipelineName.toLowerCase( ).replaceAll( "[^a-z0-9]", "_" ).replaceAll( "_+", "_" ).replaceAll( "^_|_$", "" );
    }

    /**
     * Creates a JSON Schema element for a pipeline variable type
     *
     * @param pipelineType
     *            the pipeline variable type
     * @param description
     *            the description
     * @return the JSON Schema element
     */
    private JsonSchemaElement createJsonSchemaElement( String pipelineType, String description )
    {
        if ( pipelineType == null )
        {
            return JsonStringSchema.builder( ).description( description ).build( );
        }

        switch( pipelineType.toUpperCase( ) )
        {
            case "STRING":
            case "ENUM":
                return JsonStringSchema.builder( ).description( description ).build( );
            case "NUMBER":
                return JsonNumberSchema.builder( ).description( description ).build( );
            case "BOOLEAN":
                return JsonBooleanSchema.builder( ).description( description ).build( );
            case "OBJECT":
                return JsonObjectSchema.builder( ).description( description ).build( );
            case "ARRAY":
                return JsonStringSchema.builder( ).description( description ).build( );
            default:
                return JsonStringSchema.builder( ).description( description ).build( );
        }
    }
}
