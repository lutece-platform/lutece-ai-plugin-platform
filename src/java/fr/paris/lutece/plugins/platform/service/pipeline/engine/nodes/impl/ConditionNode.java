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

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_CONDITION, name = "Condition", description = "Évalue une condition basée sur une expression régulière et dirige le flux vers différentes sorties" )
public class ConditionNode extends AbstractPipelineNode
{
    private static final String INPUT_KEY_TITLE = "Clé d'entrée";
    private static final String INPUT_KEY_DESCRIPTION = "Référence à la valeur d'entrée (ex: '{{nodeId.outputKey}}')";
    private static final String CONDITION_TITLE = "Condition (regex)";
    private static final String CONDITION_DESCRIPTION = "Expression régulière à évaluer sur la valeur d'entrée";

    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String TRUE_OUTPUT_PORT_DESCRIPTION = "Sortie si la condition est vraie";
    private static final String FALSE_OUTPUT_PORT_DESCRIPTION = "Sortie si la condition est fausse";
    private static final String CONDITION_RESULT_KEY = "condition_result";

    private static final String CONDITION_RESULT_DESCRIPTION = "Résultat de la condition (true/false)";
    private static final String INPUT_VALUE_DESCRIPTION = "Valeur d'entrée évaluée";

    private static final String LOG_INPUT_NULL = "The input for condition evaluation is null.";
    private static final String LOG_CONDITION_ERROR = "Error while evaluating the regex condition '";
    private static final String LOG_INPUT_PART = "' on input '";
    private static final String LOG_ERROR_PART = "': ";
    private static final String LOG_OUTPUT_PORT_ERROR = "Unable to determine the output port for the CONDITION node ";
    private static final String LOG_DEFAULT_FALSE = ". Defaulting to false.";
    public static final String TRUE_OUTPUT_PORT = "true";
    public static final String FALSE_OUTPUT_PORT = "false";

    public static final PipelineVariable INPUT_KEY = new PipelineVariable.Builder( PipelineConstants.KEY_INPUT_KEY )
            .type( PipelineVariable.VariableType.STRING ).required( true ).title( INPUT_KEY_TITLE ).description( INPUT_KEY_DESCRIPTION ).build( );

    public static final PipelineVariable CONDITION = new PipelineVariable.Builder( PipelineConstants.KEY_CONDITION )
            .type( PipelineVariable.VariableType.STRING ).required( true ).title( CONDITION_TITLE ).description( CONDITION_DESCRIPTION ).build( );

    /**
     * Default constructor for ConditionNode. Initializes the node with the condition node type.
     */
    public ConditionNode( )
    {
        super( PipelineConstants.NODE_TYPE_CONDITION );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( INPUT_KEY );
        variables.add( CONDITION );
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, DEFAULT_INPUT_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( TRUE_OUTPUT_PORT, TRUE_OUTPUT_PORT_DESCRIPTION );
        ports.put( FALSE_OUTPUT_PORT, FALSE_OUTPUT_PORT_DESCRIPTION );
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

        boolean evaluationResult = evaluateCondition( condition, inputKeyTemplate );
        String resultPort = evaluationResult ? TRUE_OUTPUT_PORT : FALSE_OUTPUT_PORT;

        Map<String, Object> outputData = createOutputData( inputKeyTemplate, resultPort );
        prepareOutputData( context, config, outputData );

        return context;
    }

    /**
     * Creates the output data map with input value and condition result.
     *
     * @param inputKeyTemplate
     *            the input value template
     * @param resultPort
     *            the result port name
     * @return the output data map
     */
    private Map<String, Object> createOutputData( String inputKeyTemplate, String resultPort )
    {
        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( PipelineConstants.KEY_INPUT_VALUE, inputKeyTemplate );
        outputData.put( CONDITION_RESULT_KEY, resultPort );
        return outputData;
    }

    /**
     * Evaluates the condition using regex pattern matching on the input.
     *
     * @param condition
     *            the regex condition to evaluate
     * @param input
     *            the input string to match against
     * @return true if the condition matches, false otherwise
     */
    private boolean evaluateCondition( String condition, String input )
    {
        if ( input == null )
        {
            AppLogService.error( LOG_INPUT_NULL );
            return false;
        }

        try
        {
            Pattern pattern = Pattern.compile( condition );
            return pattern.matcher( input ).find( );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}{}{}{}{}", LOG_CONDITION_ERROR, condition, LOG_INPUT_PART, input, LOG_ERROR_PART, e.getMessage( ), e );
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<String> determineOutputPort( PipelineContext context, PipelineNodeConfig config )
    {
        Map<String, Object> nodeOutput = context.getNodeOutput( config.getId( ) );
        if ( nodeOutput != null && nodeOutput.containsKey( CONDITION_RESULT_KEY ) )
        {
            return CompletableFuture.completedFuture( nodeOutput.get( CONDITION_RESULT_KEY ).toString( ) );
        }
        AppLogService.error( "{}{}{}", LOG_OUTPUT_PORT_ERROR, config.getId( ), LOG_DEFAULT_FALSE );
        return CompletableFuture.completedFuture( FALSE_OUTPUT_PORT );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( CONDITION_RESULT_KEY, CONDITION_RESULT_DESCRIPTION );
        outputs.put( PipelineConstants.KEY_INPUT_VALUE, INPUT_VALUE_DESCRIPTION );
        return outputs;
    }
}
