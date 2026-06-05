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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableResolver;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_END, name = "Fin", description = "Termine un pipeline et produit le résultat final" )
public class EndNode extends AbstractPipelineNode
{
    private static final String ITEM_VARIABLE_NAME = "item";

    private static final String OUTPUTS_TITLE = "Sorties finales";
    private static final String ITEM_TITLE = "Item de sortie";
    private static final String OUTPUT_KEY_TITLE = "Nom du champ de sortie";
    private static final String OUTPUT_VALUE_TITLE = "Valeur du champ de sortie";

    private static final String OUTPUTS_DESCRIPTION = "Définit les sorties finales du pipeline. Chaque item doit contenir : outputKey (clé de l'objet, String) et outputValue (valeur, String, pouvant contenir des références de variables {{nodeId.outputKey}} à résoudre).";
    private static final String ITEM_DESCRIPTION = "Item de sortie avec deux propriétés : outputKey (String, nom du champ de sortie), outputValue (String, valeur à produire)";
    private static final String OUTPUT_KEY_DESCRIPTION = "Nom du champ de sortie (clé de l'objet)";
    private static final String OUTPUT_VALUE_DESCRIPTION = "Valeur du champ de sortie (String), pouvant contenir des références de variables.";
    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String DYNAMIC_OUTPUT_DESCRIPTION = "Sorties définies dans la configuration outputs";

    public static final PipelineVariable OUTPUTS = new PipelineVariable.Builder( PipelineConstants.KEY_OUTPUTS ).type( PipelineVariable.VariableType.ARRAY )
            .required( false ).title( OUTPUTS_TITLE ).description( OUTPUTS_DESCRIPTION )
            .itemSchema( new PipelineVariable.Builder( ITEM_VARIABLE_NAME ).type( PipelineVariable.VariableType.OBJECT ).title( ITEM_TITLE )
                    .description( ITEM_DESCRIPTION )
                    .objectField( PipelineConstants.KEY_OUTPUT_KEY,
                            new PipelineVariable.Builder( PipelineConstants.KEY_OUTPUT_KEY ).type( PipelineVariable.VariableType.STRING )
                                    .title( OUTPUT_KEY_TITLE ).description( OUTPUT_KEY_DESCRIPTION ).build( ) )
                    .objectField( PipelineConstants.KEY_OUTPUT_VALUE, new PipelineVariable.Builder( PipelineConstants.KEY_OUTPUT_VALUE )
                            .type( PipelineVariable.VariableType.STRING ).title( OUTPUT_VALUE_TITLE ).description( OUTPUT_VALUE_DESCRIPTION ).build( ) )
                    .build( ) )
            .build( );

    /**
     * Constructs a new EndNode instance.
     */
    public EndNode( )
    {
        super( PipelineConstants.NODE_TYPE_END );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( OUTPUTS );
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
        return Collections.unmodifiableMap( ports );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        return Collections.emptyMap( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        Map<String, Object> configData = config.getData( );
        Map<String, Object> finalOutputs = new HashMap<>( );

        processOutputsConfiguration( configData, context, finalOutputs );
        prepareOutputData( context, config, finalOutputs );

        return context;
    }

    /**
     * Processes the outputs configuration from the node configuration data.
     *
     * @param configData
     *            the configuration data map
     * @param context
     *            the pipeline context for variable resolution
     * @param finalOutputs
     *            the map to store processed outputs
     */
    private void processOutputsConfiguration( Map<String, Object> configData, PipelineContext context, Map<String, Object> finalOutputs )
    {
        if ( configData != null && configData.containsKey( OUTPUTS.getName( ) ) )
        {
            Object outputsConfig = configData.get( OUTPUTS.getName( ) );
            if ( outputsConfig instanceof Iterable )
            {
                processOutputItems( (Iterable<?>) outputsConfig, context, finalOutputs );
            }
        }
    }

    /**
     * Processes individual output items from the outputs configuration.
     *
     * @param outputsConfig
     *            the iterable containing output items
     * @param context
     *            the pipeline context for variable resolution
     * @param finalOutputs
     *            the map to store processed outputs
     */
    private void processOutputItems( Iterable<?> outputsConfig, PipelineContext context, Map<String, Object> finalOutputs )
    {
        for ( Object itemObj : outputsConfig )
        {
            if ( itemObj instanceof Map )
            {
                processOutputItem( (Map<?, ?>) itemObj, context, finalOutputs );
            }
        }
    }

    /**
     * Processes a single output item and adds it to the final outputs.
     *
     * @param item
     *            the output item map containing key and value
     * @param context
     *            the pipeline context for variable resolution
     * @param finalOutputs
     *            the map to store processed outputs
     */
    private void processOutputItem( Map<?, ?> item, PipelineContext context, Map<String, Object> finalOutputs )
    {
        Object keyObj = item.get( PipelineConstants.KEY_OUTPUT_KEY );
        Object valueObj = item.get( PipelineConstants.KEY_OUTPUT_VALUE );

        if ( keyObj != null && valueObj != null )
        {
            String key = keyObj.toString( );
            Object resolvedValue = PipelineVariableResolver.resolveValue( valueObj.toString( ), context );
            if ( resolvedValue != null )
            {
                finalOutputs.put( key, resolvedValue );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( PipelineConstants.DYNAMIC_OUTPUT_KEY, DYNAMIC_OUTPUT_DESCRIPTION );
        return outputs;
    }
}
