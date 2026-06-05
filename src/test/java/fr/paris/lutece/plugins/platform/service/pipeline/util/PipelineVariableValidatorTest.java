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

import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable.VariableType;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableValidator.ValidationResult;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineVariableValidator}. These cover the constraint matrix applied to pipeline input variables: requiredness, type
 * compatibility, string length, numeric range, enum membership and multi-variable aggregation.
 */
public class PipelineVariableValidatorTest extends AbstractPlatformDbTest
{
    /**
     * A required variable with neither a value nor a default fails validation, whereas the same variable with a default value passes.
     */
    @Test
    public void testRequiredVariableNeedsValueOrDefault( )
    {
        PipelineVariable required = new PipelineVariable.Builder( "title" ).type( VariableType.STRING ).required( true ).build( );
        assertFalse( PipelineVariableValidator.validate( required, null ).isValid( ) );

        PipelineVariable withDefault = new PipelineVariable.Builder( "title" ).type( VariableType.STRING ).required( true ).defaultValue( "x" ).build( );
        assertTrue( PipelineVariableValidator.validate( withDefault, null ).isValid( ) );
    }

    /**
     * An optional variable left empty is valid and produces no error message.
     */
    @Test
    public void testOptionalVariableMayBeEmpty( )
    {
        PipelineVariable optional = new PipelineVariable.Builder( "note" ).type( VariableType.STRING ).required( false ).build( );

        assertTrue( PipelineVariableValidator.validate( optional, "" ).isValid( ) );
    }

    /**
     * A value whose runtime type contradicts the declared type is rejected at the type-check stage.
     */
    @Test
    public void testTypeMismatchIsRejected( )
    {
        PipelineVariable list = new PipelineVariable.Builder( "items" ).type( VariableType.ARRAY ).build( );

        assertFalse( PipelineVariableValidator.validate( list, "not-a-list" ).isValid( ) );
        assertTrue( PipelineVariableValidator.validate( list, List.of( 1, 2 ) ).isValid( ) );
    }

    /**
     * String length constraints reject values that are too short or too long and accept values within the bounds.
     */
    @Test
    public void testStringLengthBounds( )
    {
        PipelineVariable code = new PipelineVariable.Builder( "code" ).type( VariableType.STRING ).length( 2, 4 ).build( );

        assertFalse( PipelineVariableValidator.validate( code, "a" ).isValid( ) );
        assertFalse( PipelineVariableValidator.validate( code, "abcde" ).isValid( ) );
        assertTrue( PipelineVariableValidator.validate( code, "abc" ).isValid( ) );
    }

    /**
     * Numeric range constraints reject values outside [min, max] and accept values inside, including numeric strings that coerce to a number.
     */
    @Test
    public void testNumberRangeBounds( )
    {
        PipelineVariable score = new PipelineVariable.Builder( "score" ).type( VariableType.NUMBER ).range( 0.0d, 10.0d ).build( );

        assertFalse( PipelineVariableValidator.validate( score, -1 ).isValid( ) );
        assertFalse( PipelineVariableValidator.validate( score, 11 ).isValid( ) );
        assertTrue( PipelineVariableValidator.validate( score, 5 ).isValid( ) );
        assertTrue( PipelineVariableValidator.validate( score, "7.5" ).isValid( ) );
    }

    /**
     * An enum variable accepts only declared options and rejects any other value.
     */
    @Test
    public void testEnumMembership( )
    {
        PipelineVariable colour = new PipelineVariable.Builder( "colour" ).options( "red", "green", "blue" ).build( );

        assertTrue( PipelineVariableValidator.validate( colour, "green" ).isValid( ) );
        assertFalse( PipelineVariableValidator.validate( colour, "purple" ).isValid( ) );
    }

    /**
     * A regex pattern constraint rejects non-matching strings and accepts matching ones.
     */
    @Test
    public void testStringPattern( )
    {
        PipelineVariable slug = new PipelineVariable.Builder( "slug" ).type( VariableType.STRING ).pattern( "[a-z]+" ).build( );

        assertTrue( PipelineVariableValidator.validate( slug, "abc" ).isValid( ) );
        assertFalse( PipelineVariableValidator.validate( slug, "ABC123" ).isValid( ) );
    }

    /**
     * {@code validateAll} reports the whole set as invalid when any variable fails, applies declared defaults for absent values, and accumulates per-variable
     * error messages.
     */
    @Test
    public void testValidateAllAggregatesAndAppliesDefaults( )
    {
        PipelineVariable required = new PipelineVariable.Builder( "name" ).type( VariableType.STRING ).required( true ).build( );
        PipelineVariable withDefault = new PipelineVariable.Builder( "lang" ).type( VariableType.STRING ).required( true ).defaultValue( "fr" ).build( );

        ValidationResult result = PipelineVariableValidator.validateAll( List.of( required, withDefault ), Map.of( ) );

        assertFalse( result.isValid( ) );
        assertTrue( result.getErrorMessage( ).contains( "name" ) );
        assertFalse( result.getErrorMessage( ).contains( "lang" ) );
    }

    /**
     * A null variable definition yields an invalid result rather than throwing.
     */
    @Test
    public void testNullVariableDefinitionIsInvalid( )
    {
        assertFalse( PipelineVariableValidator.validate( null, "anything" ).isValid( ) );
    }
}
