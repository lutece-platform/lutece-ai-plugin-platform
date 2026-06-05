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

public class PlatformResourceNodeExecution
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    private int id;
    private String executionId;
    private String nodeId;
    private String nodeName;
    private PlatformResourceExecutionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;
    private String inputData;
    private String outputData;
    private String errorMessage;
    private int executionOrder;
    private List<PlatformResourceNodeTrace> traces;
    private BigDecimal totalCost;

    /**
     * Returns the ID of this execution
     *
     * @return The execution ID
     */
    public int getId( )
    {
        return id;
    }

    /**
     * Sets the ID of this execution
     *
     * @param id
     *            The new execution ID
     */
    public void setId( int id )
    {
        this.id = id;
    }

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
     *            The new execution identifier
     */
    public void setExecutionId( String executionId )
    {
        this.executionId = executionId;
    }

    /**
     * Returns the node identifier
     *
     * @return The node identifier
     */
    public String getNodeId( )
    {
        return nodeId;
    }

    /**
     * Sets the node identifier
     *
     * @param nodeId
     *            The new node identifier
     */
    public void setNodeId( String nodeId )
    {
        this.nodeId = nodeId;
    }

    /**
     * Returns the node name
     *
     * @return The node name
     */
    public String getNodeName( )
    {
        return nodeName;
    }

    /**
     * Sets the node name
     *
     * @param nodeName
     *            The new node name
     */
    public void setNodeName( String nodeName )
    {
        this.nodeName = nodeName;
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
     *            The new execution status
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
     * Returns the start time of this execution
     *
     * @return The start time
     */
    public Timestamp getStartTime( )
    {
        return startTime;
    }

    /**
     * Sets the start time of this execution
     *
     * @param startTime
     *            The new start time
     */
    public void setStartTime( Timestamp startTime )
    {
        this.startTime = startTime;
    }

    /**
     * Returns the end time of this execution
     *
     * @return The end time
     */
    public Timestamp getEndTime( )
    {
        return endTime;
    }

    /**
     * Sets the end time of this execution
     *
     * @param endTime
     *            The new end time
     */
    public void setEndTime( Timestamp endTime )
    {
        this.endTime = endTime;
    }

    /**
     * Returns the input data for this execution
     *
     * @return The input data
     */
    public String getInputData( )
    {
        return inputData;
    }

    /**
     * Sets the input data for this execution
     *
     * @param inputData
     *            The new input data
     */
    public void setInputData( String inputData )
    {
        this.inputData = inputData;
    }

    /**
     * Returns the output data from this execution
     *
     * @return The output data
     */
    public String getOutputData( )
    {
        return outputData;
    }

    /**
     * Sets the output data from this execution
     *
     * @param outputData
     *            The new output data
     */
    public void setOutputData( String outputData )
    {
        this.outputData = outputData;
    }

    /**
     * Returns the error message if any
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
     *            The new error message
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the execution order
     *
     * @return The execution order
     */
    public int getExecutionOrder( )
    {
        return executionOrder;
    }

    /**
     * Sets the execution order
     *
     * @param executionOrder
     *            The new execution order
     */
    public void setExecutionOrder( int executionOrder )
    {
        this.executionOrder = executionOrder;
    }

    /**
     * Returns the list of traces for this execution
     *
     * @return The list of traces
     */
    public List<PlatformResourceNodeTrace> getTraces( )
    {
        return traces;
    }

    /**
     * Sets the list of traces for this execution
     *
     * @param traces
     *            The new list of traces
     */
    public void setTraces( List<PlatformResourceNodeTrace> traces )
    {
        this.traces = traces;
    }

    /**
     * Returns the total cost of this execution
     *
     * @return The total cost
     */
    public BigDecimal getTotalCost( )
    {
        return totalCost;
    }

    /**
     * Sets the total cost of this execution
     *
     * @param totalCost
     *            The new total cost
     */
    public void setTotalCost( BigDecimal totalCost )
    {
        this.totalCost = totalCost;
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
