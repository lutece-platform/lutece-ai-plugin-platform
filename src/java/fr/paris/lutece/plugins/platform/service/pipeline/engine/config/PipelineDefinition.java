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
import java.util.List;
import java.util.Map;

/**
 * Represents a complete pipeline definition containing nodes, edges and root node configuration. This class defines the structure and flow of a processing
 * pipeline.
 */
public class PipelineDefinition
{

    private static final String JSON_NODES_PROPERTY = "nodes";
    private static final String JSON_EDGES_PROPERTY = "edges";
    private static final String JSON_ROOT_NODE_ID_PROPERTY = "rootNodeId";

    @JsonProperty( JSON_NODES_PROPERTY )
    private Map<String, PipelineNodeConfig> nodes;

    @JsonProperty( JSON_EDGES_PROPERTY )
    private List<PipelineEdge> edges;

    @JsonProperty( JSON_ROOT_NODE_ID_PROPERTY )
    private String rootNodeId;

    /**
     * Gets the pipeline nodes mapped by their identifiers.
     *
     * @return the nodes map
     */
    public Map<String, PipelineNodeConfig> getNodes( )
    {
        return nodes;
    }

    /**
     * Sets the pipeline nodes mapped by their identifiers.
     *
     * @param nodes
     *            the nodes map to set
     */
    public void setNodes( Map<String, PipelineNodeConfig> nodes )
    {
        this.nodes = nodes;
    }

    /**
     * Gets the list of edges connecting the nodes.
     *
     * @return the edges list
     */
    public List<PipelineEdge> getEdges( )
    {
        return edges;
    }

    /**
     * Sets the list of edges connecting the nodes.
     *
     * @param edges
     *            the edges list to set
     */
    public void setEdges( List<PipelineEdge> edges )
    {
        this.edges = edges;
    }

    /**
     * Gets the root node identifier where pipeline execution starts.
     *
     * @return the root node identifier
     */
    public String getRootNodeId( )
    {
        return rootNodeId;
    }

    /**
     * Sets the root node identifier where pipeline execution starts.
     *
     * @param rootNodeId
     *            the root node identifier to set
     */
    public void setRootNodeId( String rootNodeId )
    {
        this.rootNodeId = rootNodeId;
    }
}
