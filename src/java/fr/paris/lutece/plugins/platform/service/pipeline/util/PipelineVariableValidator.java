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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import fr.paris.lutece.portal.service.util.AppLogService;

public class PipelineVariableValidator
{

    private static final String ERROR_NULL_VARIABLE_DEFINITION = "The variable definition is null";
    private static final String ERROR_REQUIRED_VARIABLE_MISSING = "Required variable '%s' is missing and has no default value";
    private static final String ERROR_VALUE_MUST_BE_STRING = "The value for '%s' must be a string";
    private static final String ERROR_VALUE_MUST_BE_NUMBER = "The value for '%s' must be a number";
    private static final String ERROR_VALUE_MUST_BE_BOOLEAN = "The value for '%s' must be a boolean";
    private static final String ERROR_VALUE_MUST_BE_OBJECT = "The value for '%s' must be an object/map";
    private static final String ERROR_VALUE_MUST_BE_ARRAY = "The value for '%s' must be an array/list";
    private static final String ERROR_PATTERN_NOT_MATCH = "The value for '%s' does not match the pattern: %s";
    private static final String ERROR_VALUE_TOO_SHORT = "The value for '%s' is too short (min: %d)";
    private static final String ERROR_VALUE_TOO_LONG = "The value for '%s' is too long (max: %d)";
    private static final String ERROR_CONVERSION_TO_NUMBER = "Failed to convert the value for '%s' to a number";
    private static final String ERROR_VALUE_BELOW_MINIMUM = "The value for '%s' is below the minimum: %s";
    private static final String ERROR_VALUE_ABOVE_MAXIMUM = "The value for '%s' exceeds the maximum: %s";
    private static final String ERROR_INVALID_ENUM_OPTION = "The value '%s' for '%s' is not in the list of valid options: %s";
    private static final String ERROR_VALUE_MUST_BE_FILE = "The value for '%s' must be a file object {fileName, contentType, content}";
    private static final String ERROR_VALUE_MUST_BE_FILE_ARRAY = "The value for '%s' must be an array of file objects";
    private static final String ERROR_FILE_MISSING_FIELD = "The file for '%s' must contain the fields: fileName, contentType and fileKey or content";
    private static final String LOG_INVALID_REGEX_PATTERN = "Invalid regex pattern '%s' for variable '%s': %s";
    private static final String OPTION_SEPARATOR = ", ";
    private static final String ERROR_SEPARATOR = "; ";
    private static final String QUOTE = "'";
    private static final String VALID_RESULT = "Valid";
    private static final String INVALID_RESULT_PREFIX = "Invalid: ";
    private static final String COLON_SPACE = ": ";

    /**
     * Validates a pipeline variable against the provided value
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate
     * @return ValidationResult containing validation status and error messages
     */
    public static ValidationResult validate( PipelineVariable variable, Object value )
    {
        if ( variable == null )
        {
            return ValidationResult.error( ERROR_NULL_VARIABLE_DEFINITION );
        }

        if ( PipelineVariableTypeUtils.isNullOrEmpty( value ) )
        {
            if ( variable.isRequired( ) && !variable.hasDefaultValue( ) )
            {
                return ValidationResult.error( String.format( ERROR_REQUIRED_VARIABLE_MISSING, variable.getName( ) ) );
            }
            return ValidationResult.success( );
        }

        ValidationResult typeResult = validateType( variable, value );
        if ( !typeResult.isValid( ) )
        {
            return typeResult;
        }

        return validateByType( variable, value );
    }

    /**
     * Validates multiple pipeline variables against their corresponding values
     *
     * @param variables
     *            list of pipeline variable definitions
     * @param values
     *            map of variable names to their values
     * @return ValidationResult containing overall validation status
     */
    public static ValidationResult validateAll( List<PipelineVariable> variables, Map<String, Object> values )
    {
        if ( variables == null || variables.isEmpty( ) )
        {
            return ValidationResult.success( );
        }

        Map<String, Object> validValues = values != null ? values : new HashMap<>( );
        ValidationResult result = ValidationResult.success( );

        for ( PipelineVariable variable : variables )
        {
            Object value = getValueWithDefault( variable, validValues );
            ValidationResult individualResult = validate( variable, value );
            if ( !individualResult.isValid( ) )
            {
                result.addError( variable.getName( ) + COLON_SPACE + individualResult.getErrorMessage( ) );
            }
        }
        return result;
    }

    /**
     * Validates the type compatibility of a value against variable type requirements
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate
     * @return ValidationResult indicating type validation success or failure
     */
    private static ValidationResult validateType( PipelineVariable variable, Object value )
    {
        PipelineVariable.VariableType type = variable.getType( );
        String variableName = variable.getName( );

        switch( type )
        {
            case STRING:
                return !PipelineVariableTypeUtils.isString( value ) ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_STRING, variableName ) )
                        : ValidationResult.success( );
            case NUMBER:
                return !PipelineVariableTypeUtils.isNumber( value ) && !PipelineVariableTypeUtils.canConvertToNumber( value )
                        ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_NUMBER, variableName ) )
                        : ValidationResult.success( );
            case BOOLEAN:
                return !PipelineVariableTypeUtils.isBoolean( value ) && !PipelineVariableTypeUtils.canConvertToBoolean( value )
                        ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_BOOLEAN, variableName ) )
                        : ValidationResult.success( );
            case OBJECT:
                return !PipelineVariableTypeUtils.isMap( value ) ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_OBJECT, variableName ) )
                        : ValidationResult.success( );
            case ARRAY:
                return !PipelineVariableTypeUtils.isList( value ) ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_ARRAY, variableName ) )
                        : ValidationResult.success( );
            case FILE:
                return validateFileType( variable, value );
            default:
                return ValidationResult.success( );
        }
    }

    /**
     * Validates FILE type structure (object or array of objects).
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate
     * @return ValidationResult indicating type validation success or failure
     */
    private static ValidationResult validateFileType( PipelineVariable variable, Object value )
    {
        if ( variable.isMultiple( ) )
        {
            if ( !PipelineVariableTypeUtils.isList( value ) )
            {
                return ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_FILE_ARRAY, variable.getName( ) ) );
            }
            for ( Object item : (List<?>) value )
            {
                if ( !PipelineVariableTypeUtils.isMap( item ) )
                {
                    return ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_FILE, variable.getName( ) ) );
                }
            }
        }
        else
        {
            if ( !PipelineVariableTypeUtils.isMap( value ) )
            {
                return ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_FILE, variable.getName( ) ) );
            }
        }
        return ValidationResult.success( );
    }

    /**
     * Validates string value against pattern and length constraints
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the string value to validate
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateString( PipelineVariable variable, String value )
    {
        if ( variable.getPattern( ) != null && !value.isEmpty( ) )
        {
            ValidationResult patternResult = validatePattern( variable, value );
            if ( !patternResult.isValid( ) )
            {
                return patternResult;
            }
        }
        return validateStringLength( variable, value );
    }

    /**
     * Validates numeric value against minimum and maximum constraints
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the numeric value to validate
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateNumber( PipelineVariable variable, Double value )
    {
        if ( value == null )
        {
            return ValidationResult.error( String.format( ERROR_CONVERSION_TO_NUMBER, variable.getName( ) ) );
        }

        if ( variable.getMinValue( ) != null && value < variable.getMinValue( ) )
        {
            return ValidationResult.error( String.format( ERROR_VALUE_BELOW_MINIMUM, variable.getName( ), variable.getMinValue( ) ) );
        }

        if ( variable.getMaxValue( ) != null && value > variable.getMaxValue( ) )
        {
            return ValidationResult.error( String.format( ERROR_VALUE_ABOVE_MAXIMUM, variable.getName( ), variable.getMaxValue( ) ) );
        }

        return ValidationResult.success( );
    }

    /**
     * Validates enum value against allowed options
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the string value to validate
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateEnum( PipelineVariable variable, String value )
    {
        if ( variable.getEnumOptions( ).isEmpty( ) )
        {
            return ValidationResult.success( );
        }

        if ( !variable.isValidOption( value ) )
        {
            String validOptions = buildValidOptionsString( variable.getEnumOptions( ) );
            return ValidationResult.error( String.format( ERROR_INVALID_ENUM_OPTION, value, variable.getName( ), validOptions ) );
        }

        return ValidationResult.success( );
    }

    /**
     * Validates array type value
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate as array
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateArray( PipelineVariable variable, Object value )
    {
        return !PipelineVariableTypeUtils.isList( value ) ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_ARRAY, variable.getName( ) ) )
                : ValidationResult.success( );
    }

    /**
     * Validates object type value
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate as object
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateObject( PipelineVariable variable, Object value )
    {
        return !PipelineVariableTypeUtils.isMap( value ) ? ValidationResult.error( String.format( ERROR_VALUE_MUST_BE_OBJECT, variable.getName( ) ) )
                : ValidationResult.success( );
    }

    /**
     * Validates value based on the variable type using appropriate validation method
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateByType( PipelineVariable variable, Object value )
    {
        PipelineVariable.VariableType type = variable.getType( );

        switch( type )
        {
            case STRING:
                return validateString( variable, value.toString( ) );
            case NUMBER:
                return validateNumber( variable, PipelineVariableTypeUtils.toDouble( value ) );
            case ENUM:
                return validateEnum( variable, value.toString( ) );
            case ARRAY:
                return validateArray( variable, value );
            case OBJECT:
                return validateObject( variable, value );
            case FILE:
                return validateFile( variable, value );
            default:
                return ValidationResult.success( );
        }
    }

    /**
     * Validates FILE value content (required fields: fileName, contentType, content).
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the value to validate
     * @return ValidationResult indicating validation success or failure
     */
    @SuppressWarnings( "unchecked" )
    private static ValidationResult validateFile( PipelineVariable variable, Object value )
    {
        if ( variable.isMultiple( ) )
        {
            for ( Object item : (List<?>) value )
            {
                ValidationResult result = validateSingleFileObject( variable.getName( ), (Map<String, Object>) item );
                if ( !result.isValid( ) )
                {
                    return result;
                }
            }
        }
        else
        {
            return validateSingleFileObject( variable.getName( ), (Map<String, Object>) value );
        }
        return ValidationResult.success( );
    }

    /**
     * Validates a single file object has required fields.
     *
     * @param variableName
     *            the variable name for error messages
     * @param fileMap
     *            the file object to validate
     * @return ValidationResult indicating validation success or failure
     */
    private static ValidationResult validateSingleFileObject( String variableName, Map<String, Object> fileMap )
    {
        boolean hasIdentifiers = fileMap.containsKey( "fileName" ) && fileMap.containsKey( "contentType" );
        boolean hasContent = fileMap.containsKey( "content" ) || fileMap.containsKey( "fileKey" );
        if ( !hasIdentifiers || !hasContent )
        {
            return ValidationResult.error( String.format( ERROR_FILE_MISSING_FIELD, variableName ) );
        }
        return ValidationResult.success( );
    }

    /**
     * Retrieves value from map with fallback to default value if available
     *
     * @param variable
     *            the pipeline variable definition
     * @param values
     *            map containing variable values
     * @return the value or default value if null/empty
     */
    private static Object getValueWithDefault( PipelineVariable variable, Map<String, Object> values )
    {
        Object value = values.get( variable.getName( ) );
        if ( PipelineVariableTypeUtils.isNullOrEmpty( value ) && variable.hasDefaultValue( ) )
        {
            value = variable.getDefaultValue( );
        }
        return value;
    }

    /**
     * Validates string value against regex pattern
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the string value to validate
     * @return ValidationResult indicating pattern validation success or failure
     */
    private static ValidationResult validatePattern( PipelineVariable variable, String value )
    {
        try
        {
            Pattern pattern = Pattern.compile( variable.getPattern( ) );
            if ( !pattern.matcher( value ).matches( ) )
            {
                return ValidationResult.error( String.format( ERROR_PATTERN_NOT_MATCH, variable.getName( ), variable.getPattern( ) ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( LOG_INVALID_REGEX_PATTERN, variable.getPattern( ), variable.getName( ), e.getMessage( ) ) );
        }
        return ValidationResult.success( );
    }

    /**
     * Validates string length against minimum and maximum constraints
     *
     * @param variable
     *            the pipeline variable definition
     * @param value
     *            the string value to validate
     * @return ValidationResult indicating length validation success or failure
     */
    private static ValidationResult validateStringLength( PipelineVariable variable, String value )
    {
        if ( variable.getMinLength( ) != null && value.length( ) < variable.getMinLength( ) )
        {
            return ValidationResult.error( String.format( ERROR_VALUE_TOO_SHORT, variable.getName( ), variable.getMinLength( ) ) );
        }

        if ( variable.getMaxLength( ) != null && value.length( ) > variable.getMaxLength( ) )
        {
            return ValidationResult.error( String.format( ERROR_VALUE_TOO_LONG, variable.getName( ), variable.getMaxLength( ) ) );
        }

        return ValidationResult.success( );
    }

    /**
     * Builds formatted string representation of valid enum options
     *
     * @param options
     *            list of enum options
     * @return formatted string with all valid options
     */
    private static String buildValidOptionsString( List<PipelineVariable.EnumOption> options )
    {
        StringBuilder validOptions = new StringBuilder( );
        for ( int i = 0; i < options.size( ); i++ )
        {
            if ( i > 0 )
                validOptions.append( OPTION_SEPARATOR );
            validOptions.append( QUOTE ).append( options.get( i ).getValue( ) ).append( QUOTE );
        }
        return validOptions.toString( );
    }

    public static class ValidationResult
    {
        private boolean valid;
        private StringBuilder errorMessage;

        /**
         * Builds a validation result with the given validity and an optional initial error message
         *
         * @param valid
         *            whether the result is valid
         * @param errorMessage
         *            the initial error message, or null
         */
        private ValidationResult( boolean valid, String errorMessage )
        {
            this.valid = valid;
            this.errorMessage = new StringBuilder( );
            if ( errorMessage != null && !errorMessage.isEmpty( ) )
            {
                this.errorMessage.append( errorMessage );
            }
        }

        /**
         * Creates a successful validation result
         *
         * @return a valid validation result
         */
        public static ValidationResult success( )
        {
            return new ValidationResult( true, null );
        }

        /**
         * Creates a failed validation result holding the given error message
         *
         * @param message
         *            the error message
         * @return an invalid validation result
         */
        public static ValidationResult error( String message )
        {
            return new ValidationResult( false, message );
        }

        /**
         * Returns whether the result is valid
         *
         * @return true if valid
         */
        public boolean isValid( )
        {
            return valid;
        }

        /**
         * Returns the error message
         *
         * @return the error message
         */
        public String getErrorMessage( )
        {
            return errorMessage.toString( );
        }

        /**
         * Appends an error message to the result and marks it as invalid
         *
         * @param error
         *            the error message to append
         */
        public void addError( String error )
        {
            if ( error != null && !error.isEmpty( ) )
            {
                if ( errorMessage.length( ) > 0 )
                {
                    errorMessage.append( ERROR_SEPARATOR );
                }
                errorMessage.append( error );
                valid = false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( )
        {
            return valid ? VALID_RESULT : INVALID_RESULT_PREFIX + errorMessage;
        }
    }
}
