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

import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * Class representing trace information for a platform resource node
 */
public class PlatformResourceNodeTrace
{
    private int id;
    private int idNodeExecution;
    private String message;
    private String data;
    private Timestamp timestamp;
    private PlatformResourceNodeTraceStatus status;
    private BigDecimal cost;

    /**
     * Returns the ID of the trace
     *
     * @return The trace ID
     */
    public int getId( )
    {
        return id;
    }

    /**
     * Sets the ID of the trace
     *
     * @param id
     *            The trace ID to set
     */
    public void setId( int id )
    {
        this.id = id;
    }

    /**
     * Returns the ID of the node execution
     *
     * @return The node execution ID
     */
    public int getIdNodeExecution( )
    {
        return idNodeExecution;
    }

    /**
     * Sets the ID of the node execution
     *
     * @param idNodeExecution
     *            The node execution ID to set
     */
    public void setIdNodeExecution( int idNodeExecution )
    {
        this.idNodeExecution = idNodeExecution;
    }

    /**
     * Returns the message of the trace
     *
     * @return The trace message
     */
    public String getMessage( )
    {
        return message;
    }

    /**
     * Sets the message of the trace
     *
     * @param message
     *            The trace message to set
     */
    public void setMessage( String message )
    {
        this.message = message;
    }

    /**
     * Returns the data of the trace
     *
     * @return The trace data
     */
    public String getData( )
    {
        return data;
    }

    /**
     * Sets the data of the trace
     *
     * @param data
     *            The trace data to set
     */
    public void setData( String data )
    {
        this.data = data;
    }

    /**
     * Returns the timestamp of the trace
     *
     * @return The trace timestamp
     */
    public Timestamp getTimestamp( )
    {
        return timestamp;
    }

    /**
     * Sets the timestamp of the trace
     *
     * @param timestamp
     *            The trace timestamp to set
     */
    public void setTimestamp( Timestamp timestamp )
    {
        this.timestamp = timestamp;
    }

    /**
     * Returns the status of the trace
     *
     * @return The trace status
     */
    public PlatformResourceNodeTraceStatus getStatus( )
    {
        return status;
    }

    /**
     * Sets the status of the trace
     *
     * @param status
     *            The trace status to set
     */
    public void setStatus( PlatformResourceNodeTraceStatus status )
    {
        this.status = status;
    }

    /**
     * Sets the status of the trace from a string value
     *
     * @param status
     *            The string representation of the status to set
     */
    public void setStatus( String status )
    {
        this.status = PlatformResourceNodeTraceStatus.fromString( status );
    }

    /**
     * Returns the cost of the trace
     *
     * @return The trace cost
     */
    public BigDecimal getCost( )
    {
        return cost;
    }

    /**
     * Sets the cost of the trace
     *
     * @param cost
     *            The trace cost to set
     */
    public void setCost( BigDecimal cost )
    {
        this.cost = cost;
    }
}
