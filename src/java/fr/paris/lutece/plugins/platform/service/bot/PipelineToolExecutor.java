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

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineService;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

public class PipelineToolExecutor implements ToolExecutor
{

    private static final String ERROR_PIPELINE_NOT_FOUND = "PipelineToolExecutor: Pipeline not found: ";
    private static final String ERROR_PIPELINE_NOT_FOUND_MESSAGE = "Pipeline non trouvé: ";
    private static final String ERROR_EXECUTING_PIPELINE = "Error executing pipeline ";
    private static final String ERROR_EXECUTING_PIPELINE_AS_TOOL = " as tool";
    private static final String ERROR_EXECUTING_PIPELINE_MESSAGE = "Erreur lors de l'exécution du pipeline: ";
    private static final String PIPELINE_STILL_RUNNING_MESSAGE = "Le pipeline est toujours en cours d'exécution après ";
    private static final String SECONDS_EXECUTION_ID = " secondes. ID d'exécution: ";
    private static final String PIPELINE_SUCCESS_NO_RESULT = "Pipeline exécuté avec succès mais aucun résultat retourné.";
    private static final String UNKNOWN_ERROR = "Erreur inconnue";
    private static final String PIPELINE_STILL_RUNNING_STATUS = "Pipeline toujours en cours d'exécution. Statut: ";
    private static final String UNKNOWN_STATUS = "inconnu";
    private static final long WAIT_TIMEOUT_MS = 60_000;
    private static final long INITIAL_POLL_INTERVAL_MS = 200;
    private static final long MAX_POLL_INTERVAL_MS = 2_000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    private final int _nPipelineId;
    private final PipelineService _pipelineService = CDI.current( ).select( PipelineService.class ).get( );

    /**
     * Constructor for PipelineToolExecutor
     *
     * @param nPipelineId
     *            the pipeline identifier
     */
    public PipelineToolExecutor( int nPipelineId )
    {
        _nPipelineId = nPipelineId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute( ToolExecutionRequest toolExecutionRequest, Object memoryId )
    {
        try
        {
            Map<String, Object> arguments = parseArguments( toolExecutionRequest.arguments( ) );

            Optional<Pipeline> optPipeline = PipelineHome.findByPrimaryKey( _nPipelineId );
            if ( !optPipeline.isPresent( ) )
            {
                AppLogService.error( "{}{}", ERROR_PIPELINE_NOT_FOUND, _nPipelineId );
                return ERROR_PIPELINE_NOT_FOUND_MESSAGE + _nPipelineId;
            }

            Pipeline pipeline = optPipeline.get( );

            String executionId = _pipelineService.executePipeline( _nPipelineId, pipeline.getIdClient( ), arguments );

            return waitForPipelineCompletion( executionId );

        }
        catch( InterruptedException e )
        {
            Thread.currentThread( ).interrupt( );
            AppLogService.error( "{}{}{}", ERROR_EXECUTING_PIPELINE, _nPipelineId, ERROR_EXECUTING_PIPELINE_AS_TOOL, e );
            return ERROR_EXECUTING_PIPELINE_MESSAGE + e.getMessage( );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}{}", ERROR_EXECUTING_PIPELINE, _nPipelineId, ERROR_EXECUTING_PIPELINE_AS_TOOL, e );
            return ERROR_EXECUTING_PIPELINE_MESSAGE + e.getMessage( );
        }
    }

    /**
     * Parse JSON arguments into a Map
     *
     * @param argumentsJson
     *            the JSON string containing arguments
     * @return a Map of parsed arguments
     * @throws Exception
     *             if parsing fails
     */
    private Map<String, Object> parseArguments( String argumentsJson ) throws Exception
    {
        if ( argumentsJson == null || argumentsJson.trim( ).isEmpty( ) )
        {
            return Map.of( );
        }
        return OBJECT_MAPPER.readValue( argumentsJson, new TypeReference<Map<String, Object>>( )
        {
        } );
    }

    /**
     * Waits for pipeline completion by polling the execution status with exponential backoff (200 ms doubling up to 2 s) until a terminal status is reached or
     * the 60 s deadline expires.
     *
     * @param executionId
     *            the execution identifier
     * @return the execution result as a formatted string
     * @throws InterruptedException
     *             if thread sleep is interrupted
     */
    private String waitForPipelineCompletion( String executionId ) throws InterruptedException
    {
        long deadline = System.currentTimeMillis( ) + WAIT_TIMEOUT_MS;
        long pollInterval = INITIAL_POLL_INTERVAL_MS;

        while ( System.currentTimeMillis( ) < deadline )
        {
            Thread.sleep( Math.min( pollInterval, Math.max( 1, deadline - System.currentTimeMillis( ) ) ) );
            pollInterval = Math.min( pollInterval * 2, MAX_POLL_INTERVAL_MS );

            Optional<PipelineExecution> optExecution = _pipelineService.getExecution( executionId );
            if ( optExecution.isPresent( ) )
            {
                PipelineExecution execution = optExecution.get( );
                String status = execution.getStatus( );

                if ( PipelineExecution.STATUS_COMPLETED.equals( status ) || PipelineExecution.STATUS_FAILED.equals( status ) )
                {
                    return formatExecutionResult( execution );
                }
            }
        }

        return PIPELINE_STILL_RUNNING_MESSAGE + ( WAIT_TIMEOUT_MS / 1000 ) + SECONDS_EXECUTION_ID + executionId;
    }

    /**
     * Format the execution result into a readable string
     *
     * @param execution
     *            the pipeline execution object
     * @return a formatted result string
     */
    private String formatExecutionResult( PipelineExecution execution )
    {
        if ( null == execution.getStatus( ) )
        {
            return PIPELINE_STILL_RUNNING_STATUS + UNKNOWN_STATUS;
        }
        switch( execution.getStatus( ) )
        {
            case PipelineExecution.STATUS_COMPLETED:
                if ( execution.getOutputs( ) != null && !execution.getOutputs( ).isEmpty( ) )
                {
                    return execution.getOutputs( );
                }
                return PIPELINE_SUCCESS_NO_RESULT;
            case PipelineExecution.STATUS_FAILED:
                return ERROR_EXECUTING_PIPELINE_MESSAGE + ( execution.getError( ) != null ? execution.getError( ) : UNKNOWN_ERROR );
            default:
                return PIPELINE_STILL_RUNNING_STATUS + execution.getStatus( );
        }
    }
}
