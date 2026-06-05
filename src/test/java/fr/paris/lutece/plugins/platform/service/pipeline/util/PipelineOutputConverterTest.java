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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineOutputVariableDTO;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineOutputConverter}. These verify the Map/DTO conversions used when exposing pipeline node outputs to the agent layer,
 * including round-trip fidelity, null handling and name-based lookup.
 */
public class PipelineOutputConverterTest extends AbstractPlatformDbTest
{
    /**
     * Converting a map to DTOs and back reproduces the original entries, regardless of map ordering.
     */
    @Test
    public void testMapToDtoRoundTrip( )
    {
        Map<String, Object> source = new LinkedHashMap<>( );
        source.put( "answer", "42" );
        source.put( "score", 0.9d );

        List<PipelineOutputVariableDTO> dtos = PipelineOutputConverter.convertMapToDTO( source );
        Map<String, Object> back = PipelineOutputConverter.convertDTOToMap( dtos );

        assertEquals( 2, dtos.size( ) );
        assertEquals( source, back );
    }

    /**
     * Null inputs yield an empty list and an empty map rather than throwing.
     */
    @Test
    public void testNullInputsYieldEmptyCollections( )
    {
        assertTrue( PipelineOutputConverter.convertMapToDTO( null ).isEmpty( ) );
        assertTrue( PipelineOutputConverter.convertDTOToMap( null ).isEmpty( ) );
    }

    /**
     * A DTO with a null name is dropped when converting back to a map, since it cannot be keyed.
     */
    @Test
    public void testDtoWithNullNameIsSkipped( )
    {
        List<PipelineOutputVariableDTO> dtos = new ArrayList<>( );
        dtos.add( new PipelineOutputVariableDTO( "kept", "v" ) );
        dtos.add( new PipelineOutputVariableDTO( null, "lost" ) );

        Map<String, Object> map = PipelineOutputConverter.convertDTOToMap( dtos );

        assertEquals( 1, map.size( ) );
        assertEquals( "v", map.get( "kept" ) );
    }

    /**
     * Lookup by name returns the matching DTO and reports presence, while an unknown name returns null and absence; null arguments are handled safely.
     */
    @Test
    public void testFindAndHasOutput( )
    {
        List<PipelineOutputVariableDTO> dtos = PipelineOutputConverter.convertMapToDTO( Map.of( "a", 1, "b", 2 ) );

        assertNotNull( PipelineOutputConverter.findOutputByName( dtos, "a" ) );
        assertEquals( 1, PipelineOutputConverter.findOutputByName( dtos, "a" ).getValue( ) );
        assertTrue( PipelineOutputConverter.hasOutput( dtos, "b" ) );
        assertFalse( PipelineOutputConverter.hasOutput( dtos, "missing" ) );
        assertNull( PipelineOutputConverter.findOutputByName( dtos, null ) );
        assertNull( PipelineOutputConverter.findOutputByName( null, "a" ) );
    }
}
