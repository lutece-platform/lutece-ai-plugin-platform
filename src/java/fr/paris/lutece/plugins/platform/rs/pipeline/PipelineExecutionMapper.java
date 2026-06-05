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
package fr.paris.lutece.plugins.platform.rs.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecutionDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineOutputVariableDTO;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Maps pipeline execution entities to the API DTO shared by the client and admin REST endpoints.
 */
public final class PipelineExecutionMapper
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Private constructor to prevent instantiation.
     */
    private PipelineExecutionMapper( )
    {
    }

    /**
     * Converts a pipeline execution entity to its API DTO, parsing the persisted outputs JSON into typed output variables when the execution completed and
     * carrying the error message when it failed.
     *
     * @param execution
     *            the execution entity
     * @return the API DTO
     */
    public static PipelineExecutionDTO toDTO( PipelineExecution execution )
    {
        PipelineExecutionDTO dto = new PipelineExecutionDTO( execution.getExecutionId( ), execution.getIdPipeline( ), execution.getStatus( ) );
        dto.setClientId( execution.getIdClient( ) );
        dto.setCreationDate( execution.getCreationDate( ) );
        dto.setCompletionDate( execution.getCompletionDate( ) );
        if ( PipelineExecution.STATUS_COMPLETED.equals( execution.getStatus( ) ) )
        {
            dto.setOutputs( parseOutputs( execution.getOutputs( ) ) );
        }
        else if ( PipelineExecution.STATUS_FAILED.equals( execution.getStatus( ) ) )
        {
            dto.setError( execution.getError( ) );
        }
        return dto;
    }

    /**
     * Parses the persisted outputs JSON of an execution into typed output variables.
     *
     * @param outputsJson
     *            the JSON payload
     * @return the parsed outputs or an empty list on failure
     */
    private static List<PipelineOutputVariableDTO> parseOutputs( String outputsJson )
    {
        if ( outputsJson == null || outputsJson.trim( ).isEmpty( ) )
        {
            return new ArrayList<>( );
        }
        try
        {
            return OBJECT_MAPPER.readValue( outputsJson, new TypeReference<List<PipelineOutputVariableDTO>>( )
            {
            } );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error parsing pipeline execution outputs", e );
            return new ArrayList<>( );
        }
    }
}
