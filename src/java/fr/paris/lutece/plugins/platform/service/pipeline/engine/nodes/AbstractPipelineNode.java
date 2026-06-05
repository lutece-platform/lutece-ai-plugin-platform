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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import fr.paris.lutece.plugins.platform.service.concurrent.BlockingIO;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineConfigurationException;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableResolver;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableValidator;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.spi.CDI;

public abstract class AbstractPipelineNode implements IPipelineNode
{

    private static final String DEFAULT_INPUT_PORT_LABEL = "Port d'entrée par défaut";
    private static final String DEFAULT_OUTPUT_PORT_LABEL = "Port de sortie par défaut";
    protected static final String INPUT_PORT_KEY = "input";
    protected static final String OUTPUT_PORT_KEY = "output";
    private static final String TRUE_VALUE = "true";
    private static final String YES_VALUE = "yes";
    private static final String ONE_VALUE = "1";
    private static final String LOG_EXECUTION_ERROR = "Error while executing node %s %s: %s";
    private static final String LOG_NUMBER_FORMAT_ERROR = "Invalid number format for %s: %s";
    private static final String ERROR_NODE_EXECUTION = "Error while executing node %s: %s";
    private static final String ERROR_INVALID_CONFIG = "Invalid configuration for node %s '%s': %s";
    private static final String ERROR_REQUIRED_PARAM = "Parameter '%s' is required for node %s";

    private static final Pattern VARIABLE_REFERENCE_PATTERN = Pattern.compile( "\\{\\{[^}]+\\}\\}" );

    /**
     * Container-managed Virtual Thread executor used to run each node body. Resolved once via programmatic CDI lookup since this class is abstract and not a
     * CDI-managed bean itself.
     */
    protected static final ManagedExecutorService BLOCKING_EXECUTOR = CDI.current( ).select( ManagedExecutorService.class, BlockingIO.Literal.INSTANCE ).get( );

    private final String nodeType;
    private final Map<String, String> inputPorts;
    private final Map<String, String> outputPorts;

    /**
     * Builds a node bound to the given type and initializes its input and output ports.
     *
     * @param nodeType
     *            the node type identifier
     */
    protected AbstractPipelineNode( String nodeType )
    {
        this.nodeType = nodeType;
        this.inputPorts = initializeInputPorts( );
        this.outputPorts = initializeOutputPorts( );
    }

    /**
     * Returns the default input ports of the node. Subclasses may override to declare custom ports.
     *
     * @return an unmodifiable map of input port keys to labels
     */
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, DEFAULT_INPUT_PORT_LABEL );
        return Collections.unmodifiableMap( ports );
    }

    /**
     * Returns the default output ports of the node. Subclasses may override to declare custom ports.
     *
     * @return an unmodifiable map of output port keys to labels
     */
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( OUTPUT_PORT_KEY, DEFAULT_OUTPUT_PORT_LABEL );
        return Collections.unmodifiableMap( ports );
    }

    @Override
    public CompletableFuture<PipelineContext> execute( PipelineContext context, PipelineNodeConfig config )
    {
        return CompletableFuture.supplyAsync( ( ) -> {
            try
            {
                Map<String, Object> nodeData = config.getData( );
                Map<String, Object> resolvedData = resolveVariables( nodeData, context );
                validateNodeConfig( config.getId( ), resolvedData );
                return executeNode( context, config, resolvedData );
            }
            catch( Exception e )
            {
                String errorMsg = String.format( LOG_EXECUTION_ERROR, nodeType, config.getId( ), e.getMessage( ) );
                AppLogService.error( errorMsg, e );
                throw new RuntimeException( String.format( ERROR_NODE_EXECUTION, config.getId( ), e.getMessage( ) ), e );
            }
        }, BLOCKING_EXECUTOR );
    }

    /**
     * Validates the resolved node data against the configurable variables, skipping unresolved variable references and file variables, and throws if the
     * configuration is invalid.
     *
     * @param nodeId
     *            the node identifier
     * @param resolvedData
     *            the node data with variables already resolved
     */
    protected void validateNodeConfig( String nodeId, Map<String, Object> resolvedData )
    {
        List<PipelineVariable> variables = getConfigurableVariables( );

        Map<String, Object> dataToValidate = new HashMap<>( );
        Set<String> fileVariableNames = new HashSet<>( );
        for ( PipelineVariable var : variables )
        {
            if ( var.getType( ) == PipelineVariable.VariableType.FILE )
            {
                fileVariableNames.add( var.getName( ) );
            }
        }

        for ( Map.Entry<String, Object> entry : resolvedData.entrySet( ) )
        {
            String key = entry.getKey( );
            Object value = entry.getValue( );
            if ( value instanceof String && VARIABLE_REFERENCE_PATTERN.matcher( (String) value ).find( ) )
            {
                continue;
            }
            if ( fileVariableNames.contains( key ) )
            {
                continue;
            }
            dataToValidate.put( key, value );
        }

        List<PipelineVariable> variablesToValidate = variables.stream( ).filter( v -> v.getType( ) != PipelineVariable.VariableType.FILE ).toList( );

        PipelineVariableValidator.ValidationResult result = PipelineVariableValidator.validateAll( variablesToValidate, dataToValidate );
        if ( !result.isValid( ) )
        {
            throw new PipelineConfigurationException( String.format( ERROR_INVALID_CONFIG, nodeType, nodeId, result.getErrorMessage( ) ) );
        }
    }

    /**
     * Executes the node logic with pre-resolved variable data.
     *
     * @param context
     *            the pipeline execution context
     * @param config
     *            the node configuration
     * @param resolvedData
     *            the node data with all variables already resolved
     * @return the updated pipeline context
     */
    protected abstract PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData );

    @Override
    public String getNodeType( )
    {
        return nodeType;
    }

    @Override
    public Map<String, String> getInputPorts( )
    {
        return inputPorts;
    }

    @Override
    public Map<String, String> getOutputPorts( )
    {
        return outputPorts;
    }

    @Override
    public CompletableFuture<String> determineOutputPort( PipelineContext context, PipelineNodeConfig config )
    {
        String defaultPort = outputPorts.isEmpty( ) ? "" : outputPorts.keySet( ).iterator( ).next( );
        return CompletableFuture.completedFuture( defaultPort );
    }

    /**
     * Returns the string value of a variable from the data map, throwing if a required variable is missing or blank.
     *
     * @param data
     *            the node data map
     * @param var
     *            the variable descriptor
     * @return the variable value as a string, or null if absent and not required
     */
    protected String getParam( Map<String, Object> data, PipelineVariable var )
    {
        Object value = data.get( var.getName( ) );
        if ( var.isRequired( ) && ( value == null || value.toString( ).trim( ).isEmpty( ) ) )
        {
            throw new IllegalArgumentException( String.format( ERROR_REQUIRED_PARAM, var.getName( ), nodeType ) );
        }
        return value != null ? value.toString( ) : null;
    }

    /**
     * Returns the string value of a parameter by name, falling back to the supplied default value when absent.
     *
     * @param data
     *            the node data map
     * @param name
     *            the parameter name
     * @param defaultValue
     *            the value returned when the data does not provide one
     * @return the resolved string value
     */
    protected String getParamWithDefault( Map<String, Object> data, String name, String defaultValue )
    {
        Object value = data.get( name );
        return value != null ? value.toString( ) : defaultValue;
    }

    /**
     * Returns the string value of a variable, falling back to the variable's own default value and then to the supplied default value when absent.
     *
     * @param data
     *            the node data map
     * @param var
     *            the variable descriptor
     * @param defaultValue
     *            the value returned when neither the data nor the variable provide one
     * @return the resolved string value
     */
    protected String getParamWithDefault( Map<String, Object> data, PipelineVariable var, String defaultValue )
    {
        Object value = data.get( var.getName( ) );
        if ( value != null )
        {
            return value.toString( );
        }
        Object varDefault = var.getDefaultValue( );
        return varDefault != null ? varDefault.toString( ) : defaultValue;
    }

    /**
     * Returns the value of a variable as a Double, parsing strings when needed and falling back to the variable's default value or minimum value.
     *
     * @param data
     *            the node data map
     * @param var
     *            the variable descriptor
     * @return the resolved double value
     */
    protected Double getParamAsDouble( Map<String, Object> data, PipelineVariable var )
    {
        Object value = data.get( var.getName( ) );
        if ( value instanceof Number )
        {
            return ( (Number) value ).doubleValue( );
        }
        else if ( value instanceof String )
        {
            try
            {
                return Double.parseDouble( (String) value );
            }
            catch( NumberFormatException e )
            {
                AppLogService.error( String.format( LOG_NUMBER_FORMAT_ERROR, var.getName( ), value ) );
            }
        }
        Object defaultValue = var.getDefaultValue( );
        if ( defaultValue instanceof Number )
        {
            return ( (Number) defaultValue ).doubleValue( );
        }
        return var.getMinValue( ) != null ? var.getMinValue( ) : 0.0;
    }

    /**
     * Returns the value of a variable as a boolean, interpreting "true", "yes" and "1" string values as true and falling back to the variable's default value.
     *
     * @param data
     *            the node data map
     * @param var
     *            the variable descriptor
     * @return the resolved boolean value
     */
    protected boolean getParamAsBoolean( Map<String, Object> data, PipelineVariable var )
    {
        Object value = data.get( var.getName( ) );
        if ( value instanceof Boolean )
        {
            return (Boolean) value;
        }
        else if ( value instanceof String )
        {
            String strValue = ( (String) value ).toLowerCase( );
            return TRUE_VALUE.equals( strValue ) || YES_VALUE.equals( strValue ) || ONE_VALUE.equals( strValue );
        }
        Object defaultValue = var.getDefaultValue( );
        return defaultValue instanceof Boolean ? (Boolean) defaultValue : false;
    }

    /**
     * Returns the value of a variable as an int, parsing strings when needed and falling back to the variable's default value.
     *
     * @param data
     *            the node data map
     * @param var
     *            the variable descriptor
     * @return the resolved integer value
     */
    protected int getParamAsInteger( Map<String, Object> data, PipelineVariable var )
    {
        Object value = data.get( var.getName( ) );
        if ( value instanceof Number )
        {
            return ( (Number) value ).intValue( );
        }
        else if ( value instanceof String )
        {
            try
            {
                return Integer.parseInt( (String) value );
            }
            catch( NumberFormatException e )
            {
                AppLogService.error( String.format( LOG_NUMBER_FORMAT_ERROR, var.getName( ), value ) );
            }
        }
        Object defaultValue = var.getDefaultValue( );
        return defaultValue instanceof Number ? ( (Number) defaultValue ).intValue( ) : 0;
    }

    /**
     * Resolves every variable reference contained in the node data against the pipeline context.
     *
     * @param nodeData
     *            the raw node data
     * @param context
     *            the pipeline execution context
     * @return the node data with variables resolved
     */
    protected Map<String, Object> resolveVariables( Map<String, Object> nodeData, PipelineContext context )
    {
        return PipelineVariableResolver.resolveMapValues( nodeData, context );
    }

    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        return new ArrayList<>( );
    }

    /**
     * Stores a copy of the node output data into the pipeline context under the node identifier.
     *
     * @param context
     *            the pipeline execution context
     * @param config
     *            the node configuration
     * @param outputData
     *            the output data to register, may be null
     */
    protected void prepareOutputData( PipelineContext context, PipelineNodeConfig config, Map<String, Object> outputData )
    {
        context.addNodeOutput( config.getId( ), outputData != null ? new HashMap<>( outputData ) : new HashMap<>( ) );
    }
}
