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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.util.*;

public class PipelineOutputAnalyzer
{
    private static final String NODES_KEY = "nodes";
    private static final String TYPE_KEY = "type";
    private static final String DATA_KEY = "data";
    private static final String END_NODE_TYPE = "END";
    private static final String PARSE_ERROR_MESSAGE = "Error parsing pipeline flow JSON for outputs";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Analyzes a pipeline flow JSON to identify declared output variables.
     *
     * @param flowJson
     *            the JSON string representing the pipeline flow
     * @return a PipelineOutputAnalysisResult containing analyzed output variables
     */
    public PipelineOutputAnalysisResult analyzeRequiredOutputs( String flowJson )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        try
        {
            JsonNode flowNode = OBJECT_MAPPER.readTree( flowJson );
            JsonNode nodesNode = flowNode.get( NODES_KEY );

            if ( nodesNode == null || !nodesNode.isObject( ) )
            {
                return new PipelineOutputAnalysisResult( variables );
            }

            extractOutputsFromEndNodes( nodesNode, variables );

            return new PipelineOutputAnalysisResult( variables );
        }
        catch( JsonProcessingException e )
        {
            AppLogService.error( PARSE_ERROR_MESSAGE, e );
            return new PipelineOutputAnalysisResult( variables );
        }
    }

    /**
     * Extracts output variables from End nodes in the pipeline.
     *
     * @param nodesNode
     *            the JSON node containing all pipeline nodes
     * @param variables
     *            the list to populate with output variables
     */
    private void extractOutputsFromEndNodes( JsonNode nodesNode, List<PipelineVariable> variables )
    {
        Iterator<String> nodeIds = nodesNode.fieldNames( );
        Set<String> processedOutputKeys = new HashSet<>( );

        while ( nodeIds.hasNext( ) )
        {
            String nodeId = nodeIds.next( );
            JsonNode nodeNode = nodesNode.get( nodeId );

            if ( isEndNode( nodeNode ) )
            {
                extractOutputsFromNode( nodeNode, variables, processedOutputKeys );
            }
        }
    }

    /**
     * Checks if a node is an End node.
     *
     * @param nodeNode
     *            the JSON node to check
     * @return true if the node is an End node
     */
    private boolean isEndNode( JsonNode nodeNode )
    {
        return nodeNode != null && nodeNode.has( TYPE_KEY ) && END_NODE_TYPE.equals( nodeNode.get( TYPE_KEY ).asText( ) );
    }

    /**
     * Extracts output variables from a specific End node.
     *
     * @param nodeNode
     *            the End node to process
     * @param variables
     *            the list to populate with output variables
     * @param processedOutputKeys
     *            set to track already processed output keys
     */
    private void extractOutputsFromNode( JsonNode nodeNode, List<PipelineVariable> variables, Set<String> processedOutputKeys )
    {
        if ( !nodeNode.has( DATA_KEY ) )
        {
            return;
        }

        JsonNode dataNode = nodeNode.get( DATA_KEY );
        if ( !dataNode.has( PipelineConstants.KEY_OUTPUTS ) )
        {
            return;
        }

        JsonNode outputsNode = dataNode.get( PipelineConstants.KEY_OUTPUTS );
        if ( !outputsNode.isArray( ) )
        {
            return;
        }

        for ( JsonNode outputItem : outputsNode )
        {
            processOutputItem( outputItem, variables, processedOutputKeys );
        }
    }

    /**
     * Processes a single output item from the outputs array.
     *
     * @param outputItem
     *            the output item to process
     * @param variables
     *            the list to populate with output variables
     * @param processedOutputKeys
     *            set to track already processed output keys
     */
    private void processOutputItem( JsonNode outputItem, List<PipelineVariable> variables, Set<String> processedOutputKeys )
    {
        if ( !outputItem.isObject( ) )
        {
            return;
        }

        if ( !outputItem.has( PipelineConstants.KEY_OUTPUT_KEY ) )
        {
            return;
        }

        String outputKey = outputItem.get( PipelineConstants.KEY_OUTPUT_KEY ).asText( );

        if ( processedOutputKeys.contains( outputKey ) )
        {
            return;
        }

        processedOutputKeys.add( outputKey );

        String description = "";
        if ( outputItem.has( PipelineConstants.KEY_OUTPUT_VALUE ) )
        {
            String outputValue = outputItem.get( PipelineConstants.KEY_OUTPUT_VALUE ).asText( );
            if ( outputValue != null && !outputValue.isEmpty( ) )
            {
                description = outputValue;
            }
        }

        PipelineVariable.Builder builder = new PipelineVariable.Builder( outputKey ).type( PipelineVariable.VariableType.STRING ).required( false )
                .title( outputKey ).description( description );

        variables.add( builder.build( ) );
    }

    /**
     * Result class for pipeline output analysis.
     */
    public static class PipelineOutputAnalysisResult
    {
        private final List<PipelineVariable> variables;

        /**
         * Constructor
         *
         * @param variables
         *            the analyzed output variables
         */
        public PipelineOutputAnalysisResult( List<PipelineVariable> variables )
        {
            this.variables = Collections.unmodifiableList( new ArrayList<>( variables ) );
        }

        /**
         * Returns the analyzed output variables
         *
         * @return the analyzed output variables
         */
        public List<PipelineVariable> getVariables( )
        {
            return variables;
        }
    }
}
