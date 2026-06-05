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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PipelineVariable
{

    private static final String ERROR_NULL_NAME = "The variable name cannot be null";
    private static final String ERROR_NULL_TYPE = "The variable type cannot be null";
    private static final String VARIABLE_PREFIX = "Variable{name='";
    private static final String TYPE_LABEL = ", type=";
    private static final String REQUIRED_LABEL = ", required=";
    private static final String TITLE_LABEL = ", title='";
    private static final String DESCRIPTION_LABEL = ", description='";
    private static final String DEFAULT_VALUE_LABEL = ", defaultValue=";
    private static final String RANGE_LABEL = ", range=[";
    private static final String OPTIONS_LABEL = ", options=[";
    private static final String TEXTAREA_LABEL = ", textarea=true";
    private static final String ROWS_LABEL = ", rows=";
    private static final String RANGE_SEPARATOR = "..";
    private static final String VALUE_SEPARATOR = ", ";
    private static final char QUOTE = '\'';
    private static final char CLOSING_BRACKET = '}';
    private static final char CLOSING_SQUARE_BRACKET = ']';

    private final String name;
    private final VariableType type;
    private final boolean required;
    private final String description;
    private final String title;
    private final Object defaultValue;
    private final Double minValue;
    private final Double maxValue;
    private final List<EnumOption> enumOptions;
    private final String pattern;
    private final String placeholder;
    private final Integer minLength;
    private final Integer maxLength;
    private final PipelineVariable objectItemSchema;
    private final Map<String, PipelineVariable> objectFields;
    private final boolean isTextarea;
    private final Integer rows;
    private final boolean multiple;
    private final List<String> acceptedContentTypes;

    public enum VariableType
    {
        STRING,
        NUMBER,
        BOOLEAN,
        OBJECT,
        ARRAY,
        ENUM,
        FILE
    }

    public static class EnumOption
    {
        private final String value;
        private final String label;
        private final String description;

        /**
         * Creates an EnumOption with value, label and description.
         *
         * @param value
         *            the option value
         * @param label
         *            the option label
         * @param description
         *            the option description
         */
        public EnumOption( String value, String label, String description )
        {
            this.value = value;
            this.label = label;
            this.description = description;
        }

        /**
         * Creates an EnumOption with value and label.
         *
         * @param value
         *            the option value
         * @param label
         *            the option label
         */
        public EnumOption( String value, String label )
        {
            this( value, label, null );
        }

        /**
         * Creates an EnumOption with only value.
         *
         * @param value
         *            the option value
         */
        public EnumOption( String value )
        {
            this( value, value, null );
        }

        /**
         * Gets the option value.
         *
         * @return the value
         */
        public String getValue( )
        {
            return value;
        }

        /**
         * Gets the option label.
         *
         * @return the label
         */
        public String getLabel( )
        {
            return label;
        }

        /**
         * Gets the option description.
         *
         * @return the description
         */
        public String getDescription( )
        {
            return description;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( )
        {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
                return true;
            if ( o == null || getClass( ) != o.getClass( ) )
                return false;
            EnumOption that = (EnumOption) o;
            return Objects.equals( value, that.value );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode( )
        {
            return Objects.hash( value );
        }
    }

    /**
     * Creates a PipelineVariable from a builder.
     *
     * @param builder
     *            the builder instance
     */
    private PipelineVariable( Builder builder )
    {
        this.name = builder.name;
        this.type = builder.type;
        this.required = builder.required;
        this.description = builder.description;
        this.title = builder.title;
        this.defaultValue = builder.defaultValue;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.enumOptions = Collections.unmodifiableList( builder.enumOptions != null ? new ArrayList<>( builder.enumOptions ) : List.of( ) );
        this.pattern = builder.pattern;
        this.placeholder = builder.placeholder;
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.objectItemSchema = builder.objectItemSchema;
        this.objectFields = builder.objectFields != null ? Collections.unmodifiableMap( new HashMap<>( builder.objectFields ) ) : null;
        this.isTextarea = builder.isTextarea;
        this.rows = builder.rows;
        this.multiple = builder.multiple;
        this.acceptedContentTypes = builder.acceptedContentTypes != null ? Collections.unmodifiableList( new ArrayList<>( builder.acceptedContentTypes ) )
                : List.of( );
    }

    /**
     * Gets the variable name.
     *
     * @return the name
     */
    public String getName( )
    {
        return name;
    }

    /**
     * Gets the variable type.
     *
     * @return the type
     */
    public VariableType getType( )
    {
        return type;
    }

    /**
     * Checks if the variable is required.
     *
     * @return true if required
     */
    public boolean isRequired( )
    {
        return required;
    }

    /**
     * Gets the variable description.
     *
     * @return the description
     */
    public String getDescription( )
    {
        return description;
    }

    /**
     * Gets the variable title.
     *
     * @return the title
     */
    public String getTitle( )
    {
        return title;
    }

    /**
     * Gets the default value.
     *
     * @return the default value
     */
    public Object getDefaultValue( )
    {
        return defaultValue;
    }

    /**
     * Checks if a default value is set.
     *
     * @return true if default value exists
     */
    public boolean hasDefaultValue( )
    {
        return defaultValue != null;
    }

    /**
     * Gets the minimum value.
     *
     * @return the minimum value
     */
    public Double getMinValue( )
    {
        return minValue;
    }

    /**
     * Gets the maximum value.
     *
     * @return the maximum value
     */
    public Double getMaxValue( )
    {
        return maxValue;
    }

    /**
     * Gets the enum options.
     *
     * @return the enum options list
     */
    public List<EnumOption> getEnumOptions( )
    {
        return enumOptions;
    }

    /**
     * Gets the validation pattern.
     *
     * @return the pattern
     */
    public String getPattern( )
    {
        return pattern;
    }

    /**
     * Gets the placeholder text.
     *
     * @return the placeholder
     */
    public String getPlaceholder( )
    {
        return placeholder;
    }

    /**
     * Gets the minimum length.
     *
     * @return the minimum length
     */
    public Integer getMinLength( )
    {
        return minLength;
    }

    /**
     * Gets the maximum length.
     *
     * @return the maximum length
     */
    public Integer getMaxLength( )
    {
        return maxLength;
    }

    /**
     * Gets the object item schema.
     *
     * @return the object item schema
     */
    public PipelineVariable getObjectItemSchema( )
    {
        return objectItemSchema;
    }

    /**
     * Gets the object fields.
     *
     * @return the object fields map
     */
    public Map<String, PipelineVariable> getObjectFields( )
    {
        return objectFields;
    }

    /**
     * Checks if this is a textarea field.
     *
     * @return true if textarea
     */
    public boolean isTextarea( )
    {
        return isTextarea;
    }

    /**
     * Gets the number of rows for textarea.
     *
     * @return the number of rows
     */
    public Integer getRows( )
    {
        return rows;
    }

    /**
     * Checks if this variable accepts multiple values.
     *
     * @return true if multiple
     */
    public boolean isMultiple( )
    {
        return multiple;
    }

    /**
     * Gets the accepted content types for FILE variables.
     *
     * @return the accepted content types list
     */
    public List<String> getAcceptedContentTypes( )
    {
        return acceptedContentTypes;
    }

    /**
     * Validates if a number value is within the defined range.
     *
     * @param value
     *            the value to validate
     * @return true if value is in range
     */
    public boolean isInRange( Number value )
    {
        if ( value == null || ( type != VariableType.NUMBER ) )
        {
            return false;
        }
        double doubleValue = value.doubleValue( );
        boolean valid = true;
        if ( minValue != null )
        {
            valid = valid && doubleValue >= minValue;
        }
        if ( maxValue != null )
        {
            valid = valid && doubleValue <= maxValue;
        }
        return valid;
    }

    /**
     * Validates if a string value is a valid enum option.
     *
     * @param value
     *            the value to validate
     * @return true if value is valid option
     */
    public boolean isValidOption( String value )
    {
        if ( value == null || type != VariableType.ENUM || enumOptions.isEmpty( ) )
        {
            return false;
        }
        return enumOptions.stream( ).anyMatch( option -> option.getValue( ).equals( value ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass( ) != o.getClass( ) )
            return false;
        PipelineVariable variable = (PipelineVariable) o;
        return required == variable.required && Objects.equals( name, variable.name ) && type == variable.type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode( )
    {
        return Objects.hash( name, type, required );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( )
    {
        StringBuilder sb = new StringBuilder( );
        sb.append( VARIABLE_PREFIX ).append( name ).append( QUOTE );
        sb.append( TYPE_LABEL ).append( type );
        sb.append( REQUIRED_LABEL ).append( required );
        if ( title != null )
        {
            sb.append( TITLE_LABEL ).append( title ).append( QUOTE );
        }
        if ( description != null )
        {
            sb.append( DESCRIPTION_LABEL ).append( description ).append( QUOTE );
        }
        if ( defaultValue != null )
        {
            sb.append( DEFAULT_VALUE_LABEL ).append( defaultValue );
        }
        if ( minValue != null || maxValue != null )
        {
            sb.append( RANGE_LABEL );
            if ( minValue != null )
                sb.append( minValue );
            sb.append( RANGE_SEPARATOR );
            if ( maxValue != null )
                sb.append( maxValue );
            sb.append( CLOSING_SQUARE_BRACKET );
        }
        if ( !enumOptions.isEmpty( ) )
        {
            sb.append( OPTIONS_LABEL );
            boolean first = true;
            for ( EnumOption option : enumOptions )
            {
                if ( !first )
                    sb.append( VALUE_SEPARATOR );
                sb.append( option.getValue( ) );
                first = false;
            }
            sb.append( CLOSING_SQUARE_BRACKET );
        }
        if ( isTextarea )
        {
            sb.append( TEXTAREA_LABEL );
            if ( rows != null )
            {
                sb.append( ROWS_LABEL ).append( rows );
            }
        }
        sb.append( CLOSING_BRACKET );
        return sb.toString( );
    }

    public static class Builder
    {
        private final String name;
        private VariableType type = VariableType.STRING;
        private boolean required = false;
        private String description;
        private String title;
        private Object defaultValue;
        private Double minValue;
        private Double maxValue;
        private List<EnumOption> enumOptions;
        private String pattern;
        private String placeholder;
        private Integer minLength;
        private Integer maxLength;
        private PipelineVariable objectItemSchema;
        private Map<String, PipelineVariable> objectFields;
        private boolean isTextarea = false;
        private Integer rows;
        private boolean multiple = false;
        private List<String> acceptedContentTypes;

        /**
         * Creates a new Builder with the specified name.
         *
         * @param name
         *            the variable name
         */
        public Builder( String name )
        {
            this.name = Objects.requireNonNull( name, ERROR_NULL_NAME );
        }

        /**
         * Sets the variable type.
         *
         * @param type
         *            the variable type
         * @return this builder
         */
        public Builder type( VariableType type )
        {
            this.type = Objects.requireNonNull( type, ERROR_NULL_TYPE );
            return this;
        }

        /**
         * Sets whether the variable is required.
         *
         * @param required
         *            true if required
         * @return this builder
         */
        public Builder required( boolean required )
        {
            this.required = required;
            return this;
        }

        /**
         * Sets the variable description.
         *
         * @param description
         *            the description
         * @return this builder
         */
        public Builder description( String description )
        {
            this.description = description;
            return this;
        }

        /**
         * Sets the variable title.
         *
         * @param title
         *            the title
         * @return this builder
         */
        public Builder title( String title )
        {
            this.title = title;
            return this;
        }

        /**
         * Sets the default value.
         *
         * @param defaultValue
         *            the default value
         * @return this builder
         */
        public Builder defaultValue( Object defaultValue )
        {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the value range.
         *
         * @param min
         *            the minimum value
         * @param max
         *            the maximum value
         * @return this builder
         */
        public Builder range( Double min, Double max )
        {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        /**
         * Sets the minimum value.
         *
         * @param min
         *            the minimum value
         * @return this builder
         */
        public Builder min( Double min )
        {
            this.minValue = min;
            return this;
        }

        /**
         * Sets the maximum value.
         *
         * @param max
         *            the maximum value
         * @return this builder
         */
        public Builder max( Double max )
        {
            this.maxValue = max;
            return this;
        }

        /**
         * Sets the enum options.
         *
         * @param options
         *            the list of enum options
         * @return this builder
         */
        public Builder options( List<EnumOption> options )
        {
            if ( options != null )
            {
                if ( this.enumOptions == null )
                {
                    this.enumOptions = new ArrayList<>( );
                }
                this.enumOptions.addAll( options );
                if ( !options.isEmpty( ) )
                {
                    this.type = VariableType.ENUM;
                }
            }
            return this;
        }

        /**
         * Sets the enum options from varargs.
         *
         * @param options
         *            the enum options
         * @return this builder
         */
        public Builder options( EnumOption... options )
        {
            return options( List.of( options ) );
        }

        /**
         * Sets the enum options from string values.
         *
         * @param values
         *            the string values
         * @return this builder
         */
        public Builder options( String... values )
        {
            List<EnumOption> options = new ArrayList<>( );
            for ( String value : values )
            {
                options.add( new EnumOption( value ) );
            }
            return options( options );
        }

        /**
         * Sets the validation pattern.
         *
         * @param pattern
         *            the pattern
         * @return this builder
         */
        public Builder pattern( String pattern )
        {
            this.pattern = pattern;
            return this;
        }

        /**
         * Sets the placeholder text.
         *
         * @param placeholder
         *            the placeholder
         * @return this builder
         */
        public Builder placeholder( String placeholder )
        {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Sets the length constraints.
         *
         * @param min
         *            the minimum length
         * @param max
         *            the maximum length
         * @return this builder
         */
        public Builder length( Integer min, Integer max )
        {
            this.minLength = min;
            this.maxLength = max;
            return this;
        }

        /**
         * Sets the minimum length.
         *
         * @param min
         *            the minimum length
         * @return this builder
         */
        public Builder minLength( Integer min )
        {
            this.minLength = min;
            return this;
        }

        /**
         * Sets the maximum length.
         *
         * @param max
         *            the maximum length
         * @return this builder
         */
        public Builder maxLength( Integer max )
        {
            this.maxLength = max;
            return this;
        }

        /**
         * Sets the item schema for arrays.
         *
         * @param schema
         *            the item schema
         * @return this builder
         */
        public Builder itemSchema( PipelineVariable schema )
        {
            this.objectItemSchema = schema;
            return this;
        }

        /**
         * Adds an object field.
         *
         * @param name
         *            the field name
         * @param variable
         *            the field variable
         * @return this builder
         */
        public Builder objectField( String name, PipelineVariable variable )
        {
            if ( this.objectFields == null )
                this.objectFields = new HashMap<>( );
            this.objectFields.put( name, variable );
            return this;
        }

        /**
         * Sets whether this is a textarea field.
         *
         * @param isTextarea
         *            true if textarea
         * @return this builder
         */
        public Builder textarea( boolean isTextarea )
        {
            this.isTextarea = isTextarea;
            return this;
        }

        /**
         * Sets the number of rows for textarea.
         *
         * @param rows
         *            the number of rows
         * @return this builder
         */
        public Builder rows( Integer rows )
        {
            this.rows = rows;
            return this;
        }

        /**
         * Sets whether this variable accepts multiple values.
         *
         * @param multiple
         *            true if multiple
         * @return this builder
         */
        public Builder multiple( boolean multiple )
        {
            this.multiple = multiple;
            return this;
        }

        /**
         * Sets the accepted content types for FILE variables.
         *
         * @param contentTypes
         *            the accepted content types
         * @return this builder
         */
        public Builder acceptedContentTypes( String... contentTypes )
        {
            this.acceptedContentTypes = List.of( contentTypes );
            return this;
        }

        /**
         * Builds the PipelineVariable instance.
         *
         * @return the PipelineVariable instance
         */
        public PipelineVariable build( )
        {
            return new PipelineVariable( this );
        }
    }
}
