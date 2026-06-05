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
package fr.paris.lutece.plugins.platform.service.pipeline.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PipelineContext
{

    private static final String SYSTEM_NODE_ID = "sys";

    private final Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>( );
    private final Map<String, Object> initialInputs;
    private final String jobId;
    private final int pipelineId;
    private final String observabilityId;

    /**
     * Constructs a new PipelineContext with the specified job ID, pipeline ID, observability ID and initial inputs.
     *
     * @param jobId
     *            the job identifier
     * @param pipelineId
     *            the pipeline identifier
     * @param observabilityId
     *            the observability execution identifier
     * @param initialInputs
     *            the initial inputs for the pipeline
     */
    public PipelineContext( String jobId, int pipelineId, String observabilityId, Map<String, Object> initialInputs )
    {
        this.jobId = jobId;
        this.pipelineId = pipelineId;
        this.observabilityId = observabilityId;
        this.initialInputs = initialInputs != null ? new HashMap<>( initialInputs ) : new HashMap<>( );
        this.nodeOutputs.put( SYSTEM_NODE_ID, this.initialInputs );
    }

    /**
     * Adds the output of a node to the context.
     *
     * @param nodeId
     *            the node identifier
     * @param output
     *            the output to add
     */
    public void addNodeOutput( String nodeId, Map<String, Object> output )
    {
        nodeOutputs.put( nodeId, output == null ? new HashMap<>( ) : new HashMap<>( output ) );
    }

    /**
     * Retrieves the output of a specific node.
     *
     * @param nodeId
     *            the node identifier
     * @return the node output map or null if not found
     */
    public Map<String, Object> getNodeOutput( String nodeId )
    {
        return nodeOutputs.get( nodeId );
    }

    /**
     * Retrieves a specific value from a node's output.
     *
     * @param nodeId
     *            the node identifier
     * @param key
     *            the key to retrieve
     * @return an Optional containing the value if found
     */
    public Optional<Object> getValue( String nodeId, String key )
    {
        return Optional.ofNullable( nodeOutputs.get( nodeId ) ).map( outputs -> outputs.get( key ) );
    }

    /**
     * Returns a copy of all node outputs.
     *
     * @return a concurrent map containing all node outputs
     */
    public Map<String, Map<String, Object>> getAllOutputs( )
    {
        return new ConcurrentHashMap<>( nodeOutputs );
    }

    /**
     * Returns the job identifier.
     *
     * @return the job ID
     */
    public String getJobId( )
    {
        return jobId;
    }

    /**
     * Returns the pipeline identifier.
     *
     * @return the pipeline ID
     */
    public int getPipelineId( )
    {
        return pipelineId;
    }

    /**
     * Returns the observability execution identifier.
     *
     * @return the observability ID
     */
    public String getObservabilityId( )
    {
        return observabilityId;
    }
}
