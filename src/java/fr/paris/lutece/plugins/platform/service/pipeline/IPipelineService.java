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
package fr.paris.lutece.plugins.platform.service.pipeline;

import java.util.Map;
import java.util.Optional;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineRequestDTO;

/**
 * Interface for pipeline service operations
 */
public interface IPipelineService
{

    /**
     * Executes a pipeline with the latest version
     *
     * @param pipelineId
     *            the unique identifier of the pipeline
     * @param clientId
     *            the client identifier requesting the execution
     * @param inputs
     *            the input parameters for the pipeline execution
     * @return the execution identifier
     */
    String executePipeline( int pipelineId, int clientId, Map<String, Object> inputs );

    /**
     * Executes a pipeline with a specific version
     *
     * @param pipelineId
     *            the unique identifier of the pipeline
     * @param versionName
     *            the specific version name to execute
     * @param clientId
     *            the client identifier requesting the execution
     * @param inputs
     *            the input parameters for the pipeline execution
     * @return the execution identifier
     */
    String executePipeline( int pipelineId, String versionName, int clientId, Map<String, Object> inputs );

    /**
     * Executes a pipeline with a PipelineRequestDTO
     *
     * @param request
     *            the pipeline request containing all execution parameters
     * @param clientId
     *            the client identifier requesting the execution
     * @return the execution identifier
     */
    String executePipeline( PipelineRequestDTO request, int clientId );

    /**
     * Retrieves a pipeline execution by its identifier
     *
     * @param executionId
     *            the unique identifier of the execution
     * @return an Optional containing the PipelineExecution if found, empty otherwise
     */
    Optional<PipelineExecution> getExecution( String executionId );

    /**
     * Cancels a running pipeline execution
     *
     * @param executionId
     *            the unique identifier of the execution to cancel
     * @return true if the execution was successfully cancelled, false otherwise
     */
    boolean cancelExecution( String executionId );

    /**
     * Retrieves all available node types for pipeline construction
     *
     * @return a map containing node type definitions and configurations
     */
    Map<String, Object> getNodeTypes( );

    /**
     * Checks that a pipeline flow is structurally sound for persistence: parseable JSON with nodes and edges present. Deliberately lenient — the editor
     * autosaves work-in-progress flows (empty nodes, transient cycles), so executable validity is only enforced by the PipelineGraph constructor at execution
     * time.
     *
     * @param flow
     *            the pipeline flow configuration to check
     * @return true if the flow can be safely persisted, false otherwise
     */
    boolean validatePipelineFlow( String flow );

}
