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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;

/**
 * Contract of an executable pipeline node. Implementations are discovered through the {@code @PipelineNodeType} annotation and instantiated per job by the
 * engine; they declare their ports, the context keys they produce and their configurable variables, and run asynchronously within a pipeline execution.
 */
public interface IPipelineNode
{

    /**
     * Returns the node type identifier, matching the {@code @PipelineNodeType} value used in pipeline definitions.
     *
     * @return the node type identifier
     */
    String getNodeType( );

    /**
     * Returns the input ports accepted by this node.
     *
     * @return a map of input port keys to human-readable labels
     */
    Map<String, String> getInputPorts( );

    /**
     * Returns the output ports exposed by this node, used to route edges towards downstream nodes.
     *
     * @return a map of output port keys to human-readable labels
     */
    Map<String, String> getOutputPorts( );

    /**
     * Returns the context output keys this node produces, as exposed to the configuration UI by the node registry.
     *
     * @return a map of output keys to human-readable descriptions
     */
    Map<String, String> getOutputKeys( );

    /**
     * Executes the node logic asynchronously against the given context.
     *
     * @param context
     *            the pipeline execution context holding inputs and accumulated outputs
     * @param config
     *            the node configuration from the pipeline definition
     * @return a future completing with the enriched context, or completing exceptionally on failure
     */
    CompletableFuture<PipelineContext> execute( PipelineContext context, PipelineNodeConfig config );

    /**
     * Determines the output port through which the execution flow leaves this node, allowing conditional routing.
     *
     * @param context
     *            the pipeline execution context after this node has executed
     * @param config
     *            the node configuration from the pipeline definition
     * @return a future completing with the selected output port key
     */
    CompletableFuture<String> determineOutputPort( PipelineContext context, PipelineNodeConfig config );

    /**
     * Returns the variables a user can configure on this node in the pipeline editor.
     *
     * @return the list of configurable variables
     */
    List<PipelineVariable> getConfigurableVariables( );
}
