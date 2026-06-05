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
package fr.paris.lutece.plugins.platform.business.observability;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PlatformResourceExecution
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    private String executionId;
    private String resourceType;
    private String resourceId;
    private int clientId;
    private PlatformResourceExecutionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;
    private String errorMessage;
    private String inputData;
    private String outputData;
    private List<PlatformResourceNodeExecution> nodeExecutions;
    private BigDecimal totalCost;
    private String resourceTypeName;
    private String resourceName;

    /**
     * Returns the execution identifier
     *
     * @return The execution identifier
     */
    public String getExecutionId( )
    {
        return executionId;
    }

    /**
     * Sets the execution identifier
     *
     * @param executionId
     *            The execution identifier
     */
    public void setExecutionId( String executionId )
    {
        this.executionId = executionId;
    }

    /**
     * Returns the resource type
     *
     * @return The resource type
     */
    public String getResourceType( )
    {
        return resourceType;
    }

    /**
     * Sets the resource type
     *
     * @param resourceType
     *            The resource type
     */
    public void setResourceType( String resourceType )
    {
        this.resourceType = resourceType;
    }

    /**
     * Returns the resource identifier
     *
     * @return The resource identifier
     */
    public String getResourceId( )
    {
        return resourceId;
    }

    /**
     * Sets the resource identifier
     *
     * @param resourceId
     *            The resource identifier
     */
    public void setResourceId( String resourceId )
    {
        this.resourceId = resourceId;
    }

    /**
     * Returns the client identifier
     *
     * @return The client identifier
     */
    public int getClientId( )
    {
        return clientId;
    }

    /**
     * Sets the client identifier
     *
     * @param clientId
     *            The client identifier
     */
    public void setClientId( int clientId )
    {
        this.clientId = clientId;
    }

    /**
     * Returns the execution status
     *
     * @return The execution status
     */
    public PlatformResourceExecutionStatus getStatus( )
    {
        return status;
    }

    /**
     * Sets the execution status
     *
     * @param status
     *            The execution status
     */
    public void setStatus( PlatformResourceExecutionStatus status )
    {
        this.status = status;
    }

    /**
     * Sets the execution status from a string representation
     *
     * @param status
     *            The string representation of the status
     */
    public void setStatus( String status )
    {
        this.status = PlatformResourceExecutionStatus.fromString( status );
    }

    /**
     * Returns the execution start time
     *
     * @return The start time
     */
    public Timestamp getStartTime( )
    {
        return startTime;
    }

    /**
     * Sets the execution start time
     *
     * @param startTime
     *            The start time
     */
    public void setStartTime( Timestamp startTime )
    {
        this.startTime = startTime;
    }

    /**
     * Returns the execution end time
     *
     * @return The end time
     */
    public Timestamp getEndTime( )
    {
        return endTime;
    }

    /**
     * Sets the execution end time
     *
     * @param endTime
     *            The end time
     */
    public void setEndTime( Timestamp endTime )
    {
        this.endTime = endTime;
    }

    /**
     * Returns the error message if execution failed
     *
     * @return The error message
     */
    public String getErrorMessage( )
    {
        return errorMessage;
    }

    /**
     * Sets the error message
     *
     * @param errorMessage
     *            The error message
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the input data for the execution
     *
     * @return The input data
     */
    public String getInputData( )
    {
        return inputData;
    }

    /**
     * Sets the input data for the execution
     *
     * @param inputData
     *            The input data
     */
    public void setInputData( String inputData )
    {
        this.inputData = inputData;
    }

    /**
     * Returns the output data from the execution
     *
     * @return The output data
     */
    public String getOutputData( )
    {
        return outputData;
    }

    /**
     * Sets the output data from the execution
     *
     * @param outputData
     *            The output data
     */
    public void setOutputData( String outputData )
    {
        this.outputData = outputData;
    }

    /**
     * Returns the list of node executions
     *
     * @return The list of node executions
     */
    public List<PlatformResourceNodeExecution> getNodeExecutions( )
    {
        return nodeExecutions;
    }

    /**
     * Sets the list of node executions
     *
     * @param nodeExecutions
     *            The list of node executions
     */
    public void setNodeExecutions( List<PlatformResourceNodeExecution> nodeExecutions )
    {
        this.nodeExecutions = nodeExecutions;
    }

    /**
     * Returns the total cost of the execution
     *
     * @return The total cost
     */
    public BigDecimal getTotalCost( )
    {
        return totalCost;
    }

    /**
     * Sets the total cost of the execution
     *
     * @param totalCost
     *            The total cost
     */
    public void setTotalCost( BigDecimal totalCost )
    {
        this.totalCost = totalCost;
    }

    /**
     * Returns the resource type name
     *
     * @return the resource type name
     */
    public String getResourceTypeName( )
    {
        return resourceTypeName;
    }

    /**
     * Sets the resource type name
     *
     * @param resourceTypeName
     *            the resource type name
     */
    public void setResourceTypeName( String resourceTypeName )
    {
        this.resourceTypeName = resourceTypeName;
    }

    /**
     * Returns the resource name
     *
     * @return the resource name
     */
    public String getResourceName( )
    {
        return resourceName;
    }

    /**
     * Sets the resource name
     *
     * @param resourceName
     *            the resource name
     */
    public void setResourceName( String resourceName )
    {
        this.resourceName = resourceName;
    }

    /**
     * Returns the input data parsed as a Map. Falls back to an empty map if parsing fails.
     *
     * @return the parsed input data
     */
    public Map<String, Object> getParsedInputData( )
    {
        return parseJson( inputData );
    }

    /**
     * Returns the output data parsed as a Map. Falls back to an empty map if parsing fails.
     *
     * @return the parsed output data
     */
    public Map<String, Object> getParsedOutputData( )
    {
        return parseJson( outputData );
    }

    /**
     * Parses a JSON string into a Map, returning an empty map on failure.
     *
     * @param json
     *            the JSON string
     * @return the parsed map
     */
    private static Map<String, Object> parseJson( String json )
    {
        if ( json == null || json.isEmpty( ) )
        {
            return Collections.emptyMap( );
        }
        try
        {
            return OBJECT_MAPPER.readValue( json, new TypeReference<Map<String, Object>>( )
            {
            } );
        }
        catch( Exception e )
        {
            return Collections.emptyMap( );
        }
    }
}
