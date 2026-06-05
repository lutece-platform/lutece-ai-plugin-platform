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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_RETRY_MANAGER, name = "Gestionnaire de relance", description = "Gère les tentatives de relance d'une séquence en limitant le nombre maximum d'essais" )
public class RetryManagerNode extends AbstractPipelineNode
{
    public static final String RETRY_OUTPUT_PORT = "retry";
    public static final String FAILED_OUTPUT_PORT = "failed";

    private static final String RETRY_MANAGER_COUNT_KEY = "retry_manager_count";
    private static final String RETRY_DECISION_KEY = "retry_decision";
    private static final String MAX_RETRIES_NAME = "max_retries";

    private static final String MAX_RETRIES_TITLE = "Nombre maximum de relances";
    private static final String MAX_RETRIES_DESCRIPTION = "Nombre maximum de fois que le chemin 'retry' peut être emprunté (ex: 3)";
    private static final String INPUT_PORT_DESCRIPTION = "Entrée déclenchant la vérification de relance";
    private static final String RETRY_PORT_DESCRIPTION = "Chemin de sortie si la relance est autorisée (count < max_retries)";
    private static final String FAILED_PORT_DESCRIPTION = "Chemin de sortie si max_retries est atteint ou dépassé";
    private static final String COUNT_OUTPUT_DESCRIPTION = "Compteur de tentatives";
    private static final String DECISION_OUTPUT_DESCRIPTION = "Décision de relance (retry/failed)";

    private static final String INVALID_FORMAT_ERROR = "Invalid format for max_retries in node ";
    private static final String DEFAULT_VALUE_ERROR = ". Falling back to default value 0.";
    private static final String MAX_RETRIES_REACHED_ERROR = "Maximum number of retries (";
    private static final String RETRIES_DISABLED_ERROR = ") reached or retries disabled. Failure.";
    private static final String OUTPUT_PORT_ERROR = "Unable to determine the output port for the RETRY_MANAGER node ";
    private static final String DEFAULT_TO_FAILED_ERROR = ". Defaulting to failed.";

    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final int DEFAULT_RETRY_COUNT = 0;

    public static final PipelineVariable MAX_RETRIES = new PipelineVariable.Builder( MAX_RETRIES_NAME ).type( PipelineVariable.VariableType.NUMBER )
            .required( false ).title( MAX_RETRIES_TITLE ).description( MAX_RETRIES_DESCRIPTION ).defaultValue( DEFAULT_MAX_RETRIES ).build( );

    /**
     * Constructor for RetryManagerNode.
     */
    public RetryManagerNode( )
    {
        super( PipelineConstants.NODE_TYPE_RETRY_MANAGER );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( MAX_RETRIES );
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, INPUT_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( RETRY_OUTPUT_PORT, RETRY_PORT_DESCRIPTION );
        ports.put( FAILED_OUTPUT_PORT, FAILED_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        int maxRetries = extractMaxRetries( config );
        int currentRetryCount = getCurrentRetryCount( context, config );
        String resultPort = determineResultPort( currentRetryCount, maxRetries );
        Map<String, Object> outputData = createOutputData( currentRetryCount, resultPort );
        prepareOutputData( context, config, outputData );
        return context;
    }

    /**
     * Extracts the maximum retry count from the node configuration.
     *
     * @param config
     *            the pipeline node configuration
     * @return the maximum retry count, or default value if invalid
     */
    private int extractMaxRetries( PipelineNodeConfig config )
    {
        Map<String, Object> nodeData = config.getData( );
        int maxRetries = DEFAULT_MAX_RETRIES;

        if ( nodeData != null && nodeData.containsKey( MAX_RETRIES.getName( ) ) )
        {
            Object maxRetriesObj = nodeData.get( MAX_RETRIES.getName( ) );
            if ( maxRetriesObj instanceof Number )
            {
                maxRetries = ( (Number) maxRetriesObj ).intValue( );
            }
            else
            {
                AppLogService.error( "{}{}{}", INVALID_FORMAT_ERROR, config.getId( ), DEFAULT_VALUE_ERROR );
            }
        }

        return Math.max( maxRetries, DEFAULT_MAX_RETRIES );
    }

    /**
     * Gets the current retry count from the pipeline context.
     *
     * @param context
     *            the pipeline context
     * @param config
     *            the pipeline node configuration
     * @return the current retry count
     */
    private int getCurrentRetryCount( PipelineContext context, PipelineNodeConfig config )
    {
        Map<String, Object> previousOutput = context.getNodeOutput( config.getId( ) );
        int currentRetryCount = DEFAULT_RETRY_COUNT;

        if ( previousOutput != null && previousOutput.containsKey( RETRY_MANAGER_COUNT_KEY ) )
        {
            Object countObj = previousOutput.get( RETRY_MANAGER_COUNT_KEY );
            if ( countObj instanceof Number )
            {
                currentRetryCount = ( (Number) countObj ).intValue( );
            }
        }

        return currentRetryCount;
    }

    /**
     * Determines the output port based on retry count and maximum retries.
     *
     * @param currentRetryCount
     *            the current retry count
     * @param maxRetries
     *            the maximum number of retries allowed
     * @return the output port name (retry or failed)
     */
    private String determineResultPort( int currentRetryCount, int maxRetries )
    {
        if ( currentRetryCount < maxRetries )
        {
            return RETRY_OUTPUT_PORT;
        }
        else
        {
            AppLogService.error( "{}{}{}", MAX_RETRIES_REACHED_ERROR, maxRetries, RETRIES_DISABLED_ERROR );
            return FAILED_OUTPUT_PORT;
        }
    }

    /**
     * Creates the output data map with retry count and decision.
     *
     * @param currentRetryCount
     *            the current retry count
     * @param maxRetries
     *            the maximum number of retries
     * @param resultPort
     *            the determined result port
     * @return the output data map
     */
    private Map<String, Object> createOutputData( int currentRetryCount, String resultPort )
    {
        Map<String, Object> outputData = new HashMap<>( );

        if ( RETRY_OUTPUT_PORT.equals( resultPort ) )
        {
            outputData.put( RETRY_MANAGER_COUNT_KEY, currentRetryCount + 1 );
        }
        else
        {
            outputData.put( RETRY_MANAGER_COUNT_KEY, currentRetryCount );
        }

        outputData.put( RETRY_DECISION_KEY, resultPort );
        return outputData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<String> determineOutputPort( PipelineContext context, PipelineNodeConfig config )
    {
        Map<String, Object> nodeOutput = context.getNodeOutput( config.getId( ) );
        if ( nodeOutput != null && nodeOutput.containsKey( RETRY_DECISION_KEY ) )
        {
            return CompletableFuture.completedFuture( nodeOutput.get( RETRY_DECISION_KEY ).toString( ) );
        }
        AppLogService.error( "{}{}{}", OUTPUT_PORT_ERROR, config.getId( ), DEFAULT_TO_FAILED_ERROR );
        return CompletableFuture.completedFuture( FAILED_OUTPUT_PORT );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( RETRY_MANAGER_COUNT_KEY, COUNT_OUTPUT_DESCRIPTION );
        outputs.put( RETRY_DECISION_KEY, DECISION_OUTPUT_DESCRIPTION );
        return outputs;
    }
}
