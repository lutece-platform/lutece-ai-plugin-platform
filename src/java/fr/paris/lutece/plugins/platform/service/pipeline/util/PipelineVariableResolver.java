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
package fr.paris.lutece.plugins.platform.service.pipeline.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.portal.service.util.AppLogService;

public class PipelineVariableResolver
{

    private static final Pattern VARIABLE_PATTERN = Pattern.compile( "\\{\\{([^}]+)\\}\\}" );
    private static final int MAX_RECURSION_DEPTH = 10;
    private static final ThreadLocal<Set<String>> RESOLUTION_PATH = ThreadLocal.withInitial( HashSet::new );
    private static final String LOG_PREFIX = "VariableResolver: ";
    private static final String MAX_DEPTH_ERROR_MSG = "Maximum recursion depth (%d) reached for template: %s";
    private static final String CIRCULAR_REF_ERROR_MSG = "Circular reference detected: ";
    private static final String CIRCULAR_REF_SEPARATOR = " → ";
    private static final String DOT_SEPARATOR = "\\.";
    private static final String COERCE_ERROR_MSG = "Failed to convert the value '%s' to type %s: %s";
    private static final int VARIABLE_PARTS_COUNT = 2;

    /**
     * Resolves variables in a template string using the provided pipeline context.
     *
     * @param template
     *            The template string containing variables to resolve
     * @param context
     *            The pipeline context containing variable values
     * @return The resolved template string with variables replaced by their values
     */
    public static String resolve( String template, PipelineContext context )
    {
        RESOLUTION_PATH.get( ).clear( );
        return resolveRecursive( template, context, 0 );
    }

    /**
     * Recursively resolves variables in a template string with depth tracking.
     *
     * @param template
     *            The template string to resolve
     * @param context
     *            The pipeline context containing variable values
     * @param depth
     *            The current recursion depth
     * @return The resolved template string
     * @throws PipelineVariableResolutionException
     *             if maximum depth or circular reference is detected
     */
    private static String resolveRecursive( String template, PipelineContext context, int depth )
    {
        if ( template == null || context == null )
        {
            return template;
        }

        if ( depth >= MAX_RECURSION_DEPTH )
        {
            String message = String.format( MAX_DEPTH_ERROR_MSG, MAX_RECURSION_DEPTH, template );
            AppLogService.error( "{}{}", LOG_PREFIX, message );
            throw new PipelineVariableResolutionException( message );
        }

        Matcher matcher = VARIABLE_PATTERN.matcher( template );
        if ( !matcher.find( ) )
        {
            return template;
        }

        matcher.reset( );
        StringBuffer resolvedString = new StringBuffer( );
        boolean changed = false;

        while ( matcher.find( ) )
        {
            String fullMatch = matcher.group( 0 );
            String variableName = matcher.group( 1 );
            Set<String> resolutionPath = RESOLUTION_PATH.get( );

            if ( resolutionPath.contains( variableName ) )
            {
                String cyclePath = String.join( CIRCULAR_REF_SEPARATOR, resolutionPath ) + CIRCULAR_REF_SEPARATOR + variableName;
                String errorMsg = CIRCULAR_REF_ERROR_MSG + cyclePath;
                AppLogService.error( "{}{}", LOG_PREFIX, errorMsg );
                throw new PipelineVariableResolutionException( errorMsg );
            }

            resolutionPath.add( variableName );
            try
            {
                Optional<PipelineVariableValue> valueOptional = resolveVariable( variableName, context );
                if ( valueOptional.isPresent( ) )
                {
                    changed = true;
                    String replacement = valueOptional.map( Object::toString ).orElse( fullMatch );
                    matcher.appendReplacement( resolvedString, Matcher.quoteReplacement( replacement ) );
                }
                else
                {
                    matcher.appendReplacement( resolvedString, Matcher.quoteReplacement( fullMatch ) );
                }
            }
            finally
            {
                resolutionPath.remove( variableName );
            }
        }

        matcher.appendTail( resolvedString );
        return changed ? resolveRecursive( resolvedString.toString( ), context, depth + 1 ) : resolvedString.toString( );
    }

    /**
     * Resolves a single variable by extracting its node ID and key from the variable name.
     *
     * @param variableName
     *            The variable name in format "nodeId.key"
     * @param context
     *            The pipeline context to retrieve the value from
     * @return Optional containing the resolved variable value, or empty if not found
     */
    public static Optional<PipelineVariableValue> resolveVariable( String variableName, PipelineContext context )
    {
        if ( variableName == null || !variableName.contains( "." ) )
        {
            return Optional.empty( );
        }

        String [ ] parts = variableName.split( DOT_SEPARATOR, VARIABLE_PARTS_COUNT );
        String nodeId = parts [0];
        String key = parts [1];

        Optional<Object> rawValue = context.getValue( nodeId, key );
        return rawValue.map( PipelineVariableValue::new );
    }

    /**
     * Resolves all string values in a map using the provided pipeline context.
     *
     * @param dataMap
     *            The map containing values to resolve
     * @param context
     *            The pipeline context for variable resolution
     * @return A new map with resolved string values, or null if input is null
     */
    public static Map<String, Object> resolveMapValues( Map<String, Object> dataMap, PipelineContext context )
    {
        if ( dataMap == null )
            return null;

        Map<String, Object> resolvedMap = new HashMap<>( );
        for ( Map.Entry<String, Object> entry : dataMap.entrySet( ) )
        {
            Object value = entry.getValue( );
            if ( value instanceof String s )
            {
                resolvedMap.put( entry.getKey( ), resolveValue( s, context ) );
            }
            else
            {
                resolvedMap.put( entry.getKey( ), value );
            }
        }
        return resolvedMap;
    }

    /**
     * Resolves a string value, returning the raw object if the entire string is a single variable reference pointing to a complex value (Map, List), or the
     * resolved string otherwise.
     *
     * @param value
     *            the string value to resolve
     * @param context
     *            the pipeline context
     * @return the resolved value — an Object (Map/List) for direct references, or a String for text templates
     */
    public static Object resolveValue( String value, PipelineContext context )
    {
        Matcher matcher = VARIABLE_PATTERN.matcher( value.trim( ) );
        if ( matcher.matches( ) )
        {
            String variableName = matcher.group( 1 );
            Optional<PipelineVariableValue> resolved = resolveVariable( variableName, context );
            if ( resolved.isPresent( ) )
            {
                Object rawValue = resolved.get( ).getValue( );
                if ( rawValue instanceof java.util.Map || rawValue instanceof java.util.List )
                {
                    return rawValue;
                }
            }
        }
        return resolve( value, context );
    }

    /**
     * Validates if a value is compatible with the specified pipeline variable type.
     *
     * @param variable
     *            The pipeline variable definition containing type and constraints
     * @param value
     *            The value to validate
     * @return true if the value is valid for the variable type, false otherwise
     */
    public static boolean validateValue( PipelineVariable variable, Object value )
    {
        if ( PipelineVariableTypeUtils.isNullOrEmpty( value ) )
        {
            return !variable.isRequired( ) || variable.hasDefaultValue( );
        }

        switch( variable.getType( ) )
        {
            case STRING:
                return PipelineVariableTypeUtils.isString( value );
            case NUMBER:
                return PipelineVariableTypeUtils.isNumber( value );
            case BOOLEAN:
                return PipelineVariableTypeUtils.isBoolean( value );
            case OBJECT:
                return PipelineVariableTypeUtils.isMap( value );
            case ARRAY:
                return PipelineVariableTypeUtils.isList( value );
            default:
                return false;
        }
    }

    /**
     * Coerces a value to match the expected type of a pipeline variable.
     *
     * @param variable
     *            The pipeline variable definition containing the target type
     * @param value
     *            The value to coerce
     * @return The coerced value, or the original value if coercion fails
     */
    public static Object coerceValue( PipelineVariable variable, Object value )
    {
        if ( PipelineVariableTypeUtils.isNullOrEmpty( value ) )
        {
            return variable.hasDefaultValue( ) ? variable.getDefaultValue( ) : null;
        }

        try
        {
            switch( variable.getType( ) )
            {
                case STRING:
                    return value.toString( );
                case NUMBER:
                    return PipelineVariableTypeUtils.isString( value ) ? PipelineVariableTypeUtils.toDouble( value ) : value;
                case BOOLEAN:
                    return PipelineVariableTypeUtils.isString( value ) ? PipelineVariableTypeUtils.toBoolean( value ) : value;
                default:
                    return value;
            }
        }
        catch( Exception e )
        {
            String errorMsg = String.format( COERCE_ERROR_MSG, value, variable.getType( ), e.getMessage( ) );
            AppLogService.error( errorMsg, e );
            return value;
        }
    }
}
