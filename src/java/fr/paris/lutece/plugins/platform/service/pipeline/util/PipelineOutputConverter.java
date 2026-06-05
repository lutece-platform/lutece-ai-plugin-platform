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

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineOutputVariableDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting pipeline output data between different formats. Provides methods to convert between Map and DTO representations of pipeline
 * outputs.
 */
public class PipelineOutputConverter
{
    /**
     * Converts a Map of pipeline outputs to a List of PipelineOutputVariableDTO objects.
     *
     * @param outputsMap
     *            the map containing output names as keys and values as objects
     * @return a list of PipelineOutputVariableDTO objects, empty list if input is null
     */
    public static List<PipelineOutputVariableDTO> convertMapToDTO( Map<String, Object> outputsMap )
    {
        List<PipelineOutputVariableDTO> outputs = new ArrayList<>( );

        if ( outputsMap != null )
        {
            for ( Map.Entry<String, Object> entry : outputsMap.entrySet( ) )
            {
                outputs.add( new PipelineOutputVariableDTO( entry.getKey( ), entry.getValue( ) ) );
            }
        }

        return outputs;
    }

    /**
     * Converts a List of PipelineOutputVariableDTO objects to a Map.
     *
     * @param outputsList
     *            the list of PipelineOutputVariableDTO objects to convert
     * @return a map with output names as keys and values as objects, empty map if input is null
     */
    public static Map<String, Object> convertDTOToMap( List<PipelineOutputVariableDTO> outputsList )
    {
        Map<String, Object> outputsMap = new HashMap<>( );

        if ( outputsList != null )
        {
            for ( PipelineOutputVariableDTO output : outputsList )
            {
                if ( output.getName( ) != null )
                {
                    outputsMap.put( output.getName( ), output.getValue( ) );
                }
            }
        }

        return outputsMap;
    }

    /**
     * Finds a specific output variable by name in the given list.
     *
     * @param outputs
     *            the list of PipelineOutputVariableDTO objects to search in
     * @param name
     *            the name of the output variable to find
     * @return the matching PipelineOutputVariableDTO object, or null if not found or parameters are null
     */
    public static PipelineOutputVariableDTO findOutputByName( List<PipelineOutputVariableDTO> outputs, String name )
    {
        if ( outputs != null && name != null )
        {
            return outputs.stream( ).filter( output -> name.equals( output.getName( ) ) ).findFirst( ).orElse( null );
        }

        return null;
    }

    /**
     * Checks if an output variable with the specified name exists in the given list.
     *
     * @param outputs
     *            the list of PipelineOutputVariableDTO objects to check
     * @param name
     *            the name of the output variable to look for
     * @return true if an output with the given name exists, false otherwise
     */
    public static boolean hasOutput( List<PipelineOutputVariableDTO> outputs, String name )
    {
        return findOutputByName( outputs, name ) != null;
    }
}
