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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult;
import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult.ErrorCode;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for the pure logic of {@link PipelineTriggerService}: the configuration assembly schema (interval vs cron, built with Jackson) and the form
 * validation rules (required name, required schedule, JSON validity of the optional input data) that surface as typed {@link ErrorCode} values. These exercise
 * no DB, but obtain the service through CDI like the rest of the suite, since it is an {@code @ApplicationScoped} bean.
 */
public class PipelineTriggerServiceTest extends AbstractPlatformDbTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Resolves the CDI-managed pipeline trigger service.
     *
     * @return the pipeline trigger service instance
     */
    private PipelineTriggerService service( )
    {
        return CDI.current( ).select( PipelineTriggerService.class ).get( );
    }

    /**
     * Interval mode yields a JSON object carrying the parsed interval as a numeric interval_minutes entry, trimming surrounding whitespace.
     */
    @Test
    public void testBuildConfigurationIntervalMode( ) throws Exception
    {
        String config = service( ).buildConfiguration( "interval", null, " 15 " );

        JsonNode node = OBJECT_MAPPER.readTree( config );
        assertTrue( node.has( "interval_minutes" ), "interval config must carry interval_minutes" );
        assertEquals( 15, node.get( "interval_minutes" ).asInt( ), "interval value must be the parsed integer" );
        assertTrue( node.get( "interval_minutes" ).isInt( ), "interval value must be numeric, not a string" );
    }

    /**
     * A non-numeric interval cannot be parsed and yields a null configuration rather than a malformed JSON.
     */
    @Test
    public void testBuildConfigurationIntervalNotNumericReturnsNull( )
    {
        assertNull( service( ).buildConfiguration( "interval", null, "abc" ), "non-numeric interval must return null" );
    }

    /**
     * An interval mode with an empty interval value falls through to the cron branch, which is also empty here, yielding null.
     */
    @Test
    public void testBuildConfigurationIntervalEmptyReturnsNull( )
    {
        assertNull( service( ).buildConfiguration( "interval", null, "" ), "empty interval with no cron must return null" );
    }

    /**
     * Cron mode (any non-interval type) yields a JSON object carrying the cron expression under cron_expression.
     */
    @Test
    public void testBuildConfigurationCronMode( ) throws Exception
    {
        String config = service( ).buildConfiguration( "CRON", "0 0 * * *", null );

        JsonNode node = OBJECT_MAPPER.readTree( config );
        assertTrue( node.has( "cron_expression" ), "cron config must carry cron_expression" );
        assertEquals( "0 0 * * *", node.get( "cron_expression" ).asText( ), "cron expression must be preserved verbatim" );
    }

    /**
     * When neither a usable interval nor a cron expression is provided, no configuration is produced.
     */
    @Test
    public void testBuildConfigurationNoScheduleReturnsNull( )
    {
        assertNull( service( ).buildConfiguration( "CRON", null, null ), "no schedule must return null" );
        assertNull( service( ).buildConfiguration( "CRON", "", null ), "empty cron must return null" );
    }

    /**
     * A complete form (name + assembled configuration + well-formed input data) validates with no error.
     */
    @Test
    public void testValidateTriggerValid( )
    {
        TriggerValidationResult result = service( ).validateTrigger( "nightly", "{\"cron_expression\":\"0 0 * * *\"}", "{\"key\":\"value\"}" );

        assertTrue( result.isValid( ), "a complete form must be valid" );
        assertTrue( result.getErrors( ).isEmpty( ), "a valid result carries no error code" );
    }

    /**
     * An absent or blank name surfaces the typed MISSING_NAME error.
     */
    @Test
    public void testValidateTriggerMissingName( )
    {
        TriggerValidationResult nullName = service( ).validateTrigger( null, "{}", null );
        assertFalse( nullName.isValid( ), "a null name must be rejected" );
        assertTrue( nullName.getErrors( ).contains( ErrorCode.MISSING_NAME ), "null name must surface MISSING_NAME" );

        TriggerValidationResult blankName = service( ).validateTrigger( "   ", "{}", null );
        assertTrue( blankName.getErrors( ).contains( ErrorCode.MISSING_NAME ), "blank name must surface MISSING_NAME" );
    }

    /**
     * A null configuration (no schedule assembled) surfaces the typed MISSING_SCHEDULE error.
     */
    @Test
    public void testValidateTriggerMissingSchedule( )
    {
        TriggerValidationResult result = service( ).validateTrigger( "nightly", null, null );

        assertFalse( result.isValid( ), "a missing schedule must be rejected" );
        assertTrue( result.getErrors( ).contains( ErrorCode.MISSING_SCHEDULE ), "null configuration must surface MISSING_SCHEDULE" );
    }

    /**
     * Malformed input data JSON surfaces the typed INVALID_INPUT_DATA error, while a blank input is tolerated.
     */
    @Test
    public void testValidateTriggerInvalidInputData( )
    {
        TriggerValidationResult invalid = service( ).validateTrigger( "nightly", "{}", "{not json" );
        assertFalse( invalid.isValid( ), "malformed input data must be rejected" );
        assertTrue( invalid.getErrors( ).contains( ErrorCode.INVALID_INPUT_DATA ), "malformed input data must surface INVALID_INPUT_DATA" );

        TriggerValidationResult blankInput = service( ).validateTrigger( "nightly", "{}", "   " );
        assertTrue( blankInput.isValid( ), "blank input data must be tolerated" );
    }

    /**
     * Several broken fields accumulate their respective error codes rather than short-circuiting on the first.
     */
    @Test
    public void testValidateTriggerAccumulatesErrors( )
    {
        TriggerValidationResult result = service( ).validateTrigger( "", null, "}{" );

        assertFalse( result.isValid( ), "a form with several issues must be invalid" );
        assertTrue( result.getErrors( ).contains( ErrorCode.MISSING_NAME ), "missing name must be reported" );
        assertTrue( result.getErrors( ).contains( ErrorCode.MISSING_SCHEDULE ), "missing schedule must be reported" );
        assertTrue( result.getErrors( ).contains( ErrorCode.INVALID_INPUT_DATA ), "invalid input data must be reported" );
        assertEquals( 3, result.getErrors( ).size( ), "all three independent errors must accumulate" );
    }
}
