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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Contract of the {@link PipelineVariableValue} wrapper: it carries the raw resolved value and renders it through toString for string interpolation (the only
 * consumers are the variable resolver's getValue and toString paths).
 */
public class PipelineVariableValueTest
{
    /**
     * The wrapper exposes the raw value untouched, including null.
     */
    @Test
    public void testRawValueIsExposed( )
    {
        assertEquals( "hello", new PipelineVariableValue( "hello" ).getValue( ) );
        assertEquals( 42, new PipelineVariableValue( 42 ).getValue( ) );
        assertEquals( Map.of( "k", "v" ), new PipelineVariableValue( Map.of( "k", "v" ) ).getValue( ) );
        assertEquals( List.of( 1, 2 ), new PipelineVariableValue( List.of( 1, 2 ) ).getValue( ) );
        assertNull( new PipelineVariableValue( null ).getValue( ) );
    }

    /**
     * toString renders the value for interpolation in pipeline strings.
     */
    @Test
    public void testToStringRendersValue( )
    {
        assertEquals( "hello", new PipelineVariableValue( "hello" ).toString( ) );
        assertEquals( "42", new PipelineVariableValue( 42 ).toString( ) );
    }
}
