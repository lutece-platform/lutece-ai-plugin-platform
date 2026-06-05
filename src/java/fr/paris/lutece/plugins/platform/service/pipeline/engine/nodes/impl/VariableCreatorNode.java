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

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableResolver;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_VARIABLE_CREATOR, name = "Créateur de variable", description = "Crée une variable avec une valeur calculée à partir d'un template" )
public class VariableCreatorNode extends AbstractPipelineNode
{

    private static final String VARIABLE_NAME_KEY = "variable_name";
    private static final String VARIABLE_NAME_TITLE = "Nom de la variable";
    private static final String VARIABLE_NAME_DESCRIPTION = "Nom de la variable à créer";
    private static final String TEMPLATE_KEY = "template";
    private static final String TEMPLATE_TITLE = "Template de valeur";
    private static final String TEMPLATE_DESCRIPTION = "Template string avec des références de variables à combiner (ex: '{{node1.output1}} combiné avec {{node2.output2}}')";
    private static final String DYNAMIC_OUTPUT_DESCRIPTION = "Variable créée dynamiquement (utiliser la valeur de variable_name)";

    public static final PipelineVariable VARIABLE_NAME = new PipelineVariable.Builder( VARIABLE_NAME_KEY ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( VARIABLE_NAME_TITLE ).description( VARIABLE_NAME_DESCRIPTION ).build( );

    public static final PipelineVariable TEMPLATE = new PipelineVariable.Builder( TEMPLATE_KEY ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( TEMPLATE_TITLE ).description( TEMPLATE_DESCRIPTION ).build( );

    /**
     * Constructor for VariableCreatorNode.
     */
    public VariableCreatorNode( )
    {
        super( PipelineConstants.NODE_TYPE_VARIABLE_CREATOR );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( VARIABLE_NAME );
        variables.add( TEMPLATE );
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        Map<String, Object> nodeData = config.getData( );
        String variableName = getVariableName( nodeData );
        String template = getTemplate( nodeData );
        String resolvedValue = resolveTemplate( template, context );

        createOutputVariable( context, config, variableName, resolvedValue );
        return context;
    }

    /**
     * Gets the variable name from node data.
     *
     * @param nodeData
     *            the node configuration data
     * @return the variable name
     */
    private String getVariableName( Map<String, Object> nodeData )
    {
        return getParam( nodeData, VARIABLE_NAME );
    }

    /**
     * Gets the template from node data.
     *
     * @param nodeData
     *            the node configuration data
     * @return the template string
     */
    private String getTemplate( Map<String, Object> nodeData )
    {
        return getParam( nodeData, TEMPLATE );
    }

    /**
     * Resolves the template with context variables.
     *
     * @param template
     *            the template string
     * @param context
     *            the pipeline context
     * @return the resolved value
     */
    private String resolveTemplate( String template, PipelineContext context )
    {
        return PipelineVariableResolver.resolve( template, context );
    }

    /**
     * Creates the output variable with the resolved value.
     *
     * @param context
     *            the pipeline context
     * @param config
     *            the node configuration
     * @param variableName
     *            the name of the variable to create
     * @param resolvedValue
     *            the resolved value
     */
    private void createOutputVariable( PipelineContext context, PipelineNodeConfig config, String variableName, String resolvedValue )
    {
        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( variableName, resolvedValue );
        prepareOutputData( context, config, outputData );
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
