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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable.VariableType;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineVariableResolver}. These verify how the engine interpolates {@code {{nodeId.key}}} placeholders from a
 * {@link PipelineContext}, including multi-pass resolution, missing references, runaway recursion and type coercion of resolved values.
 */
public class PipelineVariableResolverTest extends AbstractPlatformDbTest
{
    private static final String JOB_ID = "job-1";
    private static final String OBS_ID = "obs-1";

    /**
     * Builds a context whose system inputs (node "sys") are the given map.
     *
     * @param initialInputs
     *            the initial inputs
     * @return a ready-to-use pipeline context
     */
    private PipelineContext contextWithInputs( Map<String, Object> initialInputs )
    {
        return new PipelineContext( JOB_ID, 1, OBS_ID, initialInputs );
    }

    /**
     * A placeholder embedded in surrounding text is replaced by the matching system input, leaving the rest of the string intact.
     */
    @Test
    public void testResolveSubstitutesPlaceholderInText( )
    {
        PipelineContext context = contextWithInputs( Map.of( "name", "World" ) );

        assertEquals( "Hello World!", PipelineVariableResolver.resolve( "Hello {{sys.name}}!", context ) );
    }

    /**
     * When a resolved value itself contains another placeholder, resolution runs again on the result until no placeholder remains.
     */
    @Test
    public void testResolveIsRecursiveAcrossNodes( )
    {
        PipelineContext context = contextWithInputs( new HashMap<>( ) );
        context.addNodeOutput( "a", Map.of( "x", "{{b.y}}" ) );
        context.addNodeOutput( "b", Map.of( "y", "final" ) );

        assertEquals( "final", PipelineVariableResolver.resolve( "{{a.x}}", context ) );
    }

    /**
     * An unknown reference is left verbatim in the output rather than being blanked, so the failure is visible downstream.
     */
    @Test
    public void testResolveLeavesUnknownReferenceUntouched( )
    {
        PipelineContext context = contextWithInputs( Map.of( "name", "World" ) );

        assertEquals( "{{missing.key}}", PipelineVariableResolver.resolve( "{{missing.key}}", context ) );
    }

    /**
     * A self-referential placeholder cannot stabilise and is stopped by the recursion-depth guard, surfacing as a resolution exception rather than looping
     * forever.
     */
    @Test
    public void testResolveDetectsRunawayRecursion( )
    {
        PipelineContext context = contextWithInputs( new HashMap<>( ) );
        context.addNodeOutput( "a", Map.of( "x", "{{a.x}}" ) );

        assertThrows( PipelineVariableResolutionException.class, ( ) -> PipelineVariableResolver.resolve( "{{a.x}}", context ) );
    }

    /**
     * When the whole string is a single reference to a complex value, {@code resolveValue} returns the raw object (here a Map) instead of its string form.
     */
    @Test
    public void testResolveValueReturnsRawComplexValue( )
    {
        PipelineContext context = contextWithInputs( new HashMap<>( ) );
        Map<String, Object> payload = Map.of( "k", "v" );
        context.addNodeOutput( "a", Map.of( "obj", payload ) );

        Object resolved = PipelineVariableResolver.resolveValue( "{{a.obj}}", context );

        assertTrue( resolved instanceof Map );
        assertEquals( payload, resolved );
    }

    /**
     * {@code resolveMapValues} interpolates string entries against the context and passes non-string entries through unchanged.
     */
    @Test
    public void testResolveMapValuesOnlyTouchesStrings( )
    {
        PipelineContext context = contextWithInputs( Map.of( "city", "Paris" ) );
        Map<String, Object> data = new HashMap<>( );
        data.put( "label", "in {{sys.city}}" );
        data.put( "count", 3 );

        Map<String, Object> resolved = PipelineVariableResolver.resolveMapValues( data, context );

        assertEquals( "in Paris", resolved.get( "label" ) );
        assertEquals( 3, resolved.get( "count" ) );
    }

    /**
     * {@code validateValue} accepts a value matching the declared type and rejects a mismatch; a missing optional value is accepted.
     */
    @Test
    public void testValidateValueAgainstDeclaredType( )
    {
        PipelineVariable number = new PipelineVariable.Builder( "n" ).type( VariableType.NUMBER ).build( );
        PipelineVariable optionalString = new PipelineVariable.Builder( "s" ).type( VariableType.STRING ).required( false ).build( );

        assertTrue( PipelineVariableResolver.validateValue( number, 12 ) );
        assertFalse( PipelineVariableResolver.validateValue( number, "not-a-number" ) );
        assertTrue( PipelineVariableResolver.validateValue( optionalString, null ) );
    }

    /**
     * {@code coerceValue} converts a numeric string to a Double for a NUMBER variable and falls back to the declared default when the value is empty.
     */
    @Test
    public void testCoerceValueConvertsAndDefaults( )
    {
        PipelineVariable number = new PipelineVariable.Builder( "n" ).type( VariableType.NUMBER ).defaultValue( 5.0d ).build( );

        assertEquals( 2.5d, PipelineVariableResolver.coerceValue( number, "2.5" ) );
        assertEquals( 5.0d, PipelineVariableResolver.coerceValue( number, "" ) );
    }
}
