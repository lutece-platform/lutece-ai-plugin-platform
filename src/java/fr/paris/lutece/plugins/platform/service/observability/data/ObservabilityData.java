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
package fr.paris.lutece.plugins.platform.service.observability.data;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Data class for capturing and serializing observability information. Provides factory methods for creating different types of observability records.
 */
public class ObservabilityData
{
    private static final String JSON_PROPERTY_TYPE = "type";
    private static final String JSON_PROPERTY_ATTRIBUTES = "attributes";

    private static final String TYPE_INPUT = "INPUT";
    private static final String TYPE_OUTPUT = "OUTPUT";
    private static final String TYPE_SUCCESS = "SUCCESS";
    private static final String TYPE_ERROR = "ERROR";

    private static final String KEY_OPERATION = "operation";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_RESULT = "result";
    private static final String KEY_DETAILS = "details";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATA = "data";
    private static final String KEY_SUCCESS = "success";

    private static final String ERROR_SERIALIZING_JSON = "Error serializing ObservabilityData to JSON";
    private static final String EMPTY_JSON = "{}";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( ).configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );

    @JsonProperty( JSON_PROPERTY_TYPE )
    private final String _type;

    @JsonProperty( JSON_PROPERTY_ATTRIBUTES )
    private final Map<String, Object> _attributes;

    /**
     * Constructs a new ObservabilityData instance with the specified type and attributes.
     *
     * @param type
     *            the type identifier for this observability data
     * @param attributes
     *            a map of key-value pairs representing the data attributes
     */
    public ObservabilityData( String type, Map<String, Object> attributes )
    {
        _type = type;
        _attributes = attributes != null ? attributes : new LinkedHashMap<>( );
    }

    /**
     * Creates an ObservabilityData instance from a type and variable key-value pairs.
     *
     * @param type
     *            the type identifier for this observability data
     * @param keyValues
     *            an array of alternating keys and values
     * @return a new ObservabilityData instance with the specified attributes
     */
    public static ObservabilityData of( String type, Object... keyValues )
    {
        Map<String, Object> attributes = new LinkedHashMap<>( );

        for ( int i = 0; i < keyValues.length - 1; i += 2 )
        {
            String key = String.valueOf( keyValues [i] );
            Object value = keyValues [i + 1];
            attributes.put( key, value );
        }

        return new ObservabilityData( type, attributes );
    }

    /**
     * Creates an INPUT type ObservabilityData for recording operation inputs.
     *
     * @param operation
     *            the name of the operation being performed
     * @param description
     *            a description of the input
     * @return a new ObservabilityData instance of type INPUT
     */
    public static ObservabilityData input( String operation, String description )
    {
        return of( TYPE_INPUT, KEY_OPERATION, operation, KEY_DESCRIPTION, description );
    }

    /**
     * Creates an OUTPUT type ObservabilityData for recording operation outputs.
     *
     * @param result
     *            the result of the operation
     * @param details
     *            additional details about the output
     * @return a new ObservabilityData instance of type OUTPUT
     */
    public static ObservabilityData output( String result, String details )
    {
        return of( TYPE_OUTPUT, KEY_RESULT, result, KEY_DETAILS, details );
    }

    /**
     * Creates a trace ObservabilityData for recording intermediate execution information.
     *
     * @param traceType
     *            the type of trace being recorded
     * @param message
     *            a message describing the trace
     * @param data
     *            additional data associated with the trace
     * @return a new ObservabilityData instance with the specified trace type
     */
    public static ObservabilityData trace( String traceType, String message, Object data )
    {
        return of( traceType, KEY_MESSAGE, message, KEY_DATA, data );
    }

    /**
     * Creates a SUCCESS type ObservabilityData for recording successful operations.
     *
     * @param message
     *            a message describing the success
     * @return a new ObservabilityData instance of type SUCCESS
     */
    public static ObservabilityData success( String message )
    {
        return of( TYPE_SUCCESS, KEY_SUCCESS, true, KEY_MESSAGE, message );
    }

    /**
     * Creates an ERROR type ObservabilityData for recording failed operations.
     *
     * @param message
     *            a message describing the error
     * @return a new ObservabilityData instance of type ERROR
     */
    public static ObservabilityData error( String message )
    {
        return of( TYPE_ERROR, KEY_SUCCESS, false, KEY_MESSAGE, message );
    }

    /**
     * Returns the type identifier of this observability data.
     *
     * @return the type string
     */
    public String getType( )
    {
        return _type;
    }

    /**
     * Returns the attributes map of this observability data.
     *
     * @return the attributes map
     */
    public Map<String, Object> getAttributes( )
    {
        return _attributes;
    }

    /**
     * Serializes this ObservabilityData instance to a JSON string.
     *
     * @return the JSON representation of this object, or an empty JSON object if serialization fails
     */
    public String toJson( )
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString( this );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( ERROR_SERIALIZING_JSON, e );
            return EMPTY_JSON;
        }
    }

}
