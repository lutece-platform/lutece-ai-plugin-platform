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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineVariableTypeUtils} type coercion and detection logic. These exercise the conversion rules that the pipeline engine
 * relies on when reading user-supplied variable values, independently of any database or container state.
 */
public class PipelineVariableTypeUtilsTest extends AbstractPlatformDbTest
{
    /**
     * Booleans, and the textual forms "true", "yes" and "1" coerce to true; "false", "no" and "0" coerce to false; case and surrounding whitespace are ignored.
     */
    @Test
    public void testToBooleanRecognisesTextualForms( )
    {
        assertEquals( Boolean.TRUE, PipelineVariableTypeUtils.toBoolean( Boolean.TRUE ) );
        assertEquals( Boolean.TRUE, PipelineVariableTypeUtils.toBoolean( "true" ) );
        assertEquals( Boolean.TRUE, PipelineVariableTypeUtils.toBoolean( "  YES " ) );
        assertEquals( Boolean.TRUE, PipelineVariableTypeUtils.toBoolean( "1" ) );
        assertEquals( Boolean.FALSE, PipelineVariableTypeUtils.toBoolean( "false" ) );
        assertEquals( Boolean.FALSE, PipelineVariableTypeUtils.toBoolean( "no" ) );
        assertEquals( Boolean.FALSE, PipelineVariableTypeUtils.toBoolean( "0" ) );
    }

    /**
     * An unrecognised boolean token coerces to false (only a null input yields null), while {@code canConvertToBoolean} stays consistent with the accepted
     * token set.
     */
    @Test
    public void testBooleanConversionRejectsUnknownTokens( )
    {
        assertEquals( Boolean.FALSE, PipelineVariableTypeUtils.toBoolean( "maybe" ) );
        assertNull( PipelineVariableTypeUtils.toBoolean( (Object) null ) );
        assertTrue( PipelineVariableTypeUtils.canConvertToBoolean( "no" ) );
        assertTrue( PipelineVariableTypeUtils.canConvertToBoolean( "0" ) );
        assertFalse( PipelineVariableTypeUtils.canConvertToBoolean( "maybe" ) );
        assertFalse( PipelineVariableTypeUtils.canConvertToBoolean( 42 ) );
    }

    /**
     * Numeric parsing accepts {@link Number} instances and trimmed numeric strings, and returns null for non-numeric text.
     */
    @Test
    public void testNumericConversion( )
    {
        assertEquals( Integer.valueOf( 7 ), PipelineVariableTypeUtils.toInteger( " 7 " ) );
        assertEquals( Integer.valueOf( 3 ), PipelineVariableTypeUtils.toInteger( 3.9d ) );
        assertNull( PipelineVariableTypeUtils.toInteger( "abc" ) );
        assertEquals( Double.valueOf( 2.5d ), PipelineVariableTypeUtils.toDouble( "2.5" ) );
        assertNull( PipelineVariableTypeUtils.toDouble( "x" ) );
        assertTrue( PipelineVariableTypeUtils.canConvertToNumber( "12.0" ) );
        assertFalse( PipelineVariableTypeUtils.canConvertToNumber( "twelve" ) );
    }

    /**
     * Null and blank strings are considered empty, whereas any non-blank string or non-string object is not.
     */
    @Test
    public void testIsNullOrEmpty( )
    {
        assertTrue( PipelineVariableTypeUtils.isNullOrEmpty( null ) );
        assertTrue( PipelineVariableTypeUtils.isNullOrEmpty( "   " ) );
        assertFalse( PipelineVariableTypeUtils.isNullOrEmpty( "x" ) );
        assertFalse( PipelineVariableTypeUtils.isNullOrEmpty( 0 ) );
    }

    /**
     * Type predicates discriminate String, Number, Boolean, Map and List instances as expected.
     */
    @Test
    public void testTypePredicates( )
    {
        assertTrue( PipelineVariableTypeUtils.isString( "x" ) );
        assertTrue( PipelineVariableTypeUtils.isNumber( 1 ) );
        assertTrue( PipelineVariableTypeUtils.isBoolean( true ) );
        assertTrue( PipelineVariableTypeUtils.isMap( Map.of( ) ) );
        assertTrue( PipelineVariableTypeUtils.isList( List.of( ) ) );
        assertFalse( PipelineVariableTypeUtils.isList( Map.of( ) ) );
        assertFalse( PipelineVariableTypeUtils.isMap( List.of( ) ) );
    }
}
