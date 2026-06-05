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

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVariableDTO;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineVariableConverter
{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Resolves input variables for a pipeline version from its inputSchema.
     *
     * @param version
     *            the pipeline version
     * @return the list of input variable DTOs, empty list if no schema defined
     */
    public static List<PipelineVariableDTO> resolveInputVariables( PipelineVersion version )
    {
        String inputSchema = version.getInputSchema( );
        if ( inputSchema != null && !inputSchema.trim( ).isEmpty( ) )
        {
            try
            {
                return OBJECT_MAPPER.readValue( inputSchema, OBJECT_MAPPER.getTypeFactory( ).constructCollectionType( List.class, PipelineVariableDTO.class ) );
            }
            catch( Exception e )
            {
                AppLogService.error( "Erreur lors du parsing du inputSchema", e );
            }
        }
        return List.of( );
    }

    /**
     * Converts a list of PipelineVariable objects to a list of PipelineVariableDTO objects.
     *
     * @param variables
     *            the list of PipelineVariable objects to convert
     * @return a list of converted PipelineVariableDTO objects
     */
    public static List<PipelineVariableDTO> convertToDTO( List<PipelineVariable> variables )
    {
        return variables.stream( ).map( PipelineVariableConverter::convertToDTO ).toList( );
    }

    /**
     * Converts a single PipelineVariable object to a PipelineVariableDTO object.
     *
     * @param variable
     *            the PipelineVariable object to convert
     * @return the converted PipelineVariableDTO object
     */
    public static PipelineVariableDTO convertToDTO( PipelineVariable variable )
    {
        PipelineVariableDTO dto = new PipelineVariableDTO( );

        dto.setName( variable.getName( ) );
        dto.setType( variable.getType( ).name( ) );
        dto.setRequired( variable.isRequired( ) );
        dto.setDescription( variable.getDescription( ) );
        dto.setTitle( variable.getTitle( ) );
        dto.setDefaultValue( variable.getDefaultValue( ) );
        dto.setMinValue( variable.getMinValue( ) );
        dto.setMaxValue( variable.getMaxValue( ) );
        dto.setPattern( variable.getPattern( ) );
        dto.setPlaceholder( variable.getPlaceholder( ) );
        dto.setMinLength( variable.getMinLength( ) );
        dto.setMaxLength( variable.getMaxLength( ) );
        dto.setTextarea( variable.isTextarea( ) );
        dto.setRows( variable.getRows( ) );
        dto.setMultiple( variable.isMultiple( ) );
        dto.setAcceptedContentTypes( variable.getAcceptedContentTypes( ) );

        if ( variable.getEnumOptions( ) != null && !variable.getEnumOptions( ).isEmpty( ) )
        {
            List<PipelineVariableDTO.EnumOptionDTO> enumOptions = variable.getEnumOptions( ).stream( )
                    .map( option -> new PipelineVariableDTO.EnumOptionDTO( option.getValue( ), option.getLabel( ), option.getDescription( ) ) ).toList( );
            dto.setEnumOptions( enumOptions );
        }

        if ( variable.getObjectFields( ) != null )
        {
            Map<String, PipelineVariableDTO> objectFields = variable.getObjectFields( ).entrySet( ).stream( )
                    .collect( Collectors.toMap( Map.Entry::getKey, entry -> convertToDTO( entry.getValue( ) ) ) );
            dto.setObjectFields( objectFields );
        }

        return dto;
    }
}
