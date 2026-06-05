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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an edge connection between two nodes in a pipeline configuration. An edge defines the connection from a source node's output port to a target
 * node's input port.
 */
public class PipelineEdge
{

    private static final String JSON_SOURCE_PROPERTY = "source";
    private static final String JSON_SOURCE_PORT_PROPERTY = "sourcePort";
    private static final String JSON_TARGET_PROPERTY = "target";
    private static final String JSON_TARGET_PORT_PROPERTY = "targetPort";

    @JsonProperty( JSON_SOURCE_PROPERTY )
    private String source;

    @JsonProperty( JSON_SOURCE_PORT_PROPERTY )
    private String sourcePort;

    @JsonProperty( JSON_TARGET_PROPERTY )
    private String target;

    @JsonProperty( JSON_TARGET_PORT_PROPERTY )
    private String targetPort;

    /**
     * Gets the source node identifier.
     *
     * @return the source node identifier
     */
    public String getSource( )
    {
        return source;
    }

    /**
     * Sets the source node identifier.
     *
     * @param source
     *            the source node identifier to set
     */
    public void setSource( String source )
    {
        this.source = source;
    }

    /**
     * Gets the source port identifier.
     *
     * @return the source port identifier
     */
    public String getSourcePort( )
    {
        return sourcePort;
    }

    /**
     * Sets the source port identifier.
     *
     * @param sourcePort
     *            the source port identifier to set
     */
    public void setSourcePort( String sourcePort )
    {
        this.sourcePort = sourcePort;
    }

    /**
     * Gets the target node identifier.
     *
     * @return the target node identifier
     */
    public String getTarget( )
    {
        return target;
    }

    /**
     * Sets the target node identifier.
     *
     * @param target
     *            the target node identifier to set
     */
    public void setTarget( String target )
    {
        this.target = target;
    }

    /**
     * Gets the target port identifier.
     *
     * @return the target port identifier
     */
    public String getTargetPort( )
    {
        return targetPort;
    }

    /**
     * Sets the target port identifier.
     *
     * @param targetPort
     *            the target port identifier to set
     */
    public void setTargetPort( String targetPort )
    {
        this.targetPort = targetPort;
    }
}
