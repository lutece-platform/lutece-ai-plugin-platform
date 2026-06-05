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

import java.util.List;
import java.util.Map;

/**
 * Utility class for handling pipeline variable type conversions and validations. Provides methods to convert between different data types and check type
 * compatibility.
 */
public final class PipelineVariableTypeUtils
{

    public static final String BOOLEAN_TRUE = "true";
    public static final String BOOLEAN_FALSE = "false";
    public static final String BOOLEAN_YES = "yes";
    public static final String BOOLEAN_NO = "no";
    public static final String BOOLEAN_ONE = "1";
    public static final String BOOLEAN_ZERO = "0";
    public static final String NULL_STRING = "null";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PipelineVariableTypeUtils( )
    {
    }

    /**
     * Converts an object value to Boolean type. Supports Boolean objects and String representations of boolean values.
     *
     * @param value
     *            the object to convert to Boolean
     * @return Boolean representation of the value, or null if conversion is not possible
     */
    public static Boolean toBoolean( Object value )
    {
        return switch( value )
        {
            case Boolean b -> b;
            case String s -> {
                String str = s.toLowerCase( ).trim( );
                yield BOOLEAN_TRUE.equals( str ) || BOOLEAN_YES.equals( str ) || BOOLEAN_ONE.equals( str );
            }
            case null, default -> null;
        };
    }

    /**
     * Checks if an object value can be converted to Boolean type.
     *
     * @param value
     *            the object to check for Boolean conversion compatibility
     * @return true if the value can be converted to Boolean, false otherwise
     */
    public static boolean canConvertToBoolean( Object value )
    {
        return switch( value )
        {
            case Boolean b -> true;
            case String s -> {
                String str = s.toLowerCase( ).trim( );
                yield BOOLEAN_TRUE.equals( str ) || BOOLEAN_FALSE.equals( str ) || BOOLEAN_YES.equals( str ) || BOOLEAN_NO.equals( str )
                        || BOOLEAN_ONE.equals( str ) || BOOLEAN_ZERO.equals( str );
            }
            case null, default -> false;
        };
    }

    /**
     * Converts an object value to Integer type. Supports Number objects and String representations of integer values.
     *
     * @param value
     *            the object to convert to Integer
     * @return Integer representation of the value, or null if conversion is not possible
     */
    public static Integer toInteger( Object value )
    {
        return switch( value )
        {
            case Number n -> n.intValue( );
            case String s -> {
                try
                {
                    yield Integer.parseInt( s.trim( ) );
                }
                catch( NumberFormatException e )
                {
                    yield null;
                }
            }
            case null, default -> null;
        };
    }

    /**
     * Converts an object value to Double type. Supports Number objects and String representations of double values.
     *
     * @param value
     *            the object to convert to Double
     * @return Double representation of the value, or null if conversion is not possible
     */
    public static Double toDouble( Object value )
    {
        return switch( value )
        {
            case Number n -> n.doubleValue( );
            case String s -> {
                try
                {
                    yield Double.parseDouble( s.trim( ) );
                }
                catch( NumberFormatException e )
                {
                    yield null;
                }
            }
            case null, default -> null;
        };
    }

    /**
     * Checks if an object value can be converted to a numeric type.
     *
     * @param value
     *            the object to check for numeric conversion compatibility
     * @return true if the value can be converted to a number, false otherwise
     */
    public static boolean canConvertToNumber( Object value )
    {
        return toDouble( value ) != null;
    }

    /**
     * Checks if an object value is null or empty. For String objects, also checks if the trimmed string is empty.
     *
     * @param value
     *            the object to check
     * @return true if the value is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty( Object value )
    {
        return value == null || ( value instanceof String s && s.trim( ).isEmpty( ) );
    }

    /**
     * Converts an object value to String representation.
     *
     * @param value
     *            the object to convert to String
     * @return String representation of the value, or "null" if value is null
     */
    public static String toString( Object value )
    {
        return value != null ? value.toString( ) : NULL_STRING;
    }

    /**
     * Checks if an object is of String type.
     *
     * @param value
     *            the object to check
     * @return true if the value is a String instance, false otherwise
     */
    public static boolean isString( Object value )
    {
        return value instanceof String;
    }

    /**
     * Checks if an object is of Number type.
     *
     * @param value
     *            the object to check
     * @return true if the value is a Number instance, false otherwise
     */
    public static boolean isNumber( Object value )
    {
        return value instanceof Number;
    }

    /**
     * Checks if an object is of Boolean type.
     *
     * @param value
     *            the object to check
     * @return true if the value is a Boolean instance, false otherwise
     */
    public static boolean isBoolean( Object value )
    {
        return value instanceof Boolean;
    }

    /**
     * Checks if an object is of Map type.
     *
     * @param value
     *            the object to check
     * @return true if the value is a Map instance, false otherwise
     */
    public static boolean isMap( Object value )
    {
        return value instanceof Map;
    }

    /**
     * Checks if an object is of List type.
     *
     * @param value
     *            the object to check
     * @return true if the value is a List instance, false otherwise
     */
    public static boolean isList( Object value )
    {
        return value instanceof List;
    }
}
