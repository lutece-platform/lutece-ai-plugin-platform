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
import java.util.regex.Pattern;

import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;

@Dependent
@PipelineNodeType( value = "EVALUATION", name = "Évaluation", description = "Évalue une entrée selon plusieurs conditions pour déterminer un chemin de succès, d'échec ou de relance" )
public class EvaluationNode extends AbstractPipelineNode
{

    public static final String TYPE = "EVALUATION";
    private static final String INPUT_KEY_TITLE = "Clé d'entrée";
    private static final String INPUT_KEY_DESCRIPTION = "Référence à la valeur d'entrée (ex: '{{nodeId.outputKey}}')";
    private static final String CONDITION_TITLE = "Condition de succès (regex)";
    private static final String CONDITION_DESCRIPTION = "Expression régulière pour la condition de succès";
    private static final String RETRY_CONDITION_NAME = "retry_condition";
    private static final String RETRY_CONDITION_TITLE = "Condition de relance (regex)";
    private static final String RETRY_CONDITION_DESCRIPTION = "Expression régulière pour la condition de relance (optionnelle)";
    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String SUCCESS_OUTPUT_PORT_DESCRIPTION = "Chemin de succès";
    private static final String RETRY_OUTPUT_PORT_DESCRIPTION = "Chemin de relance";
    private static final String FAILED_OUTPUT_PORT_DESCRIPTION = "Chemin d'échec";
    private static final String EVALUATION_RESULT_KEY = "evaluation_result";
    private static final String EVALUATION_RESULT_OUTPUT_DESCRIPTION = "Résultat de l'évaluation (success/retry/failed)";
    private static final String INPUT_VALUE_OUTPUT_DESCRIPTION = "Valeur d'entrée évaluée";
    private static final String NULL_INPUT_ERROR_MESSAGE = "The input for condition evaluation is null.";
    private static final String REGEX_EVALUATION_ERROR_MESSAGE = "Error while evaluating the regex condition '";
    private static final String ON_INPUT_ERROR_MESSAGE = "' on input '";
    private static final String OUTPUT_PORT_DETERMINATION_ERROR_MESSAGE = "Unable to determine the output port for the EVALUATION node ";
    private static final String DEFAULT_TO_FAILED_MESSAGE = ". Defaulting to failed.";
    private static final String EMPTY_STRING = "";

    public static final PipelineVariable INPUT_KEY = new PipelineVariable.Builder( PipelineConstants.KEY_INPUT_KEY )
            .type( PipelineVariable.VariableType.STRING ).required( true ).title( INPUT_KEY_TITLE ).description( INPUT_KEY_DESCRIPTION ).build( );

    public static final PipelineVariable CONDITION = new PipelineVariable.Builder( PipelineConstants.KEY_CONDITION )
            .type( PipelineVariable.VariableType.STRING ).required( true ).title( CONDITION_TITLE ).description( CONDITION_DESCRIPTION ).build( );

    public static final PipelineVariable RETRY_CONDITION = new PipelineVariable.Builder( RETRY_CONDITION_NAME ).type( PipelineVariable.VariableType.STRING )
            .required( false ).title( RETRY_CONDITION_TITLE ).description( RETRY_CONDITION_DESCRIPTION ).build( );
    public static final String SUCCESS_OUTPUT_PORT = "success";
    public static final String RETRY_OUTPUT_PORT = "retry";
    public static final String FAILED_OUTPUT_PORT = "failed";

    /**
     * Builds an evaluation node of type EVALUATION
     */
    public EvaluationNode( )
    {
        super( TYPE );
    }

    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( INPUT_KEY );
        variables.add( CONDITION );
        variables.add( RETRY_CONDITION );
        return variables;
    }

    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, DEFAULT_INPUT_PORT_DESCRIPTION );
        return ports;
    }

    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( SUCCESS_OUTPUT_PORT, SUCCESS_OUTPUT_PORT_DESCRIPTION );
        ports.put( RETRY_OUTPUT_PORT, RETRY_OUTPUT_PORT_DESCRIPTION );
        ports.put( FAILED_OUTPUT_PORT, FAILED_OUTPUT_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        String inputKeyTemplate = getParam( resolvedData, INPUT_KEY );
        String condition = getParam( resolvedData, CONDITION );
        String retryCondition = getParamWithDefault( resolvedData, RETRY_CONDITION, EMPTY_STRING );

        boolean isSuccess = evaluateCondition( condition, inputKeyTemplate );
        boolean isRetry = !retryCondition.isEmpty( ) && evaluateCondition( retryCondition, inputKeyTemplate );

        String result;
        if ( isSuccess )
        {
            result = SUCCESS_OUTPUT_PORT;
        }
        else if ( isRetry )
        {
            result = RETRY_OUTPUT_PORT;
        }
        else
        {
            result = FAILED_OUTPUT_PORT;
        }

        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( EVALUATION_RESULT_KEY, result );
        outputData.put( PipelineConstants.KEY_INPUT_VALUE, inputKeyTemplate );

        prepareOutputData( context, config, outputData );
        return context;
    }

    /**
     * Evaluates whether the input matches the given regular expression condition
     *
     * @param condition
     *            the regular expression condition
     * @param input
     *            the input value to test
     * @return true if the condition matches the input, false otherwise or on error
     */
    private boolean evaluateCondition( String condition, String input )
    {
        if ( input == null )
        {
            AppLogService.error( NULL_INPUT_ERROR_MESSAGE );
            return false;
        }

        try
        {
            Pattern pattern = Pattern.compile( condition );
            return pattern.matcher( input ).find( );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}{}{}': {}", REGEX_EVALUATION_ERROR_MESSAGE, condition, ON_INPUT_ERROR_MESSAGE, input, e.getMessage( ), e );
            return false;
        }
    }

    @Override
    public CompletableFuture<String> determineOutputPort( PipelineContext context, PipelineNodeConfig config )
    {
        Map<String, Object> nodeOutput = context.getNodeOutput( config.getId( ) );
        if ( nodeOutput != null && nodeOutput.containsKey( EVALUATION_RESULT_KEY ) )
        {
            return CompletableFuture.completedFuture( nodeOutput.get( EVALUATION_RESULT_KEY ).toString( ) );
        }
        AppLogService.error( "{}{}{}", OUTPUT_PORT_DETERMINATION_ERROR_MESSAGE, config.getId( ), DEFAULT_TO_FAILED_MESSAGE );
        return CompletableFuture.completedFuture( FAILED_OUTPUT_PORT );
    }

    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( EVALUATION_RESULT_KEY, EVALUATION_RESULT_OUTPUT_DESCRIPTION );
        outputs.put( PipelineConstants.KEY_INPUT_VALUE, INPUT_VALUE_OUTPUT_DESCRIPTION );
        return outputs;
    }
}
