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

import fr.paris.lutece.plugins.platform.service.pipeline.trigger.CronTriggerHandler;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTrigger;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTriggerHome;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult;
import fr.paris.lutece.plugins.platform.service.pipeline.dto.TriggerValidationResult.ErrorCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Service hosting the trigger domain: configuration assembly, form validation and CRUD/toggle operations on PipelineTrigger. It owns the trigger configuration
 * schema (interval / cron) and the validation rules, leaving the controller to read parameters and map results to the presentation layer.
 */
@ApplicationScoped
@Named( "platform.pipelineTriggerService" )
public class PipelineTriggerService
{
    private static final String TRIGGER_TYPE_INTERVAL = "interval";
    private static final String TRIGGER_NOT_FOUND_MESSAGE = "Trigger not found";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    /**
     * Builds the trigger configuration JSON from the raw schedule parameters. Interval mode produces a numeric interval_minutes entry; otherwise a cron
     * expression entry is produced. Both branches are serialized with Jackson.
     *
     * @param triggerType
     *            the requested trigger type (interval or cron)
     * @param cronExpression
     *            the cron expression (used when not in interval mode)
     * @param intervalMinutes
     *            the interval in minutes (used in interval mode)
     * @return the configuration JSON, or null when no usable schedule was provided
     */
    public String buildConfiguration( String triggerType, String cronExpression, String intervalMinutes )
    {
        if ( TRIGGER_TYPE_INTERVAL.equals( triggerType ) && intervalMinutes != null && !intervalMinutes.isEmpty( ) )
        {
            try
            {
                int nInterval = Integer.parseInt( intervalMinutes.trim( ) );
                return OBJECT_MAPPER.createObjectNode( ).put( CronTriggerHandler.KEY_INTERVAL_MINUTES, nInterval ).toString( );
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        }
        if ( cronExpression != null && !cronExpression.isEmpty( ) )
        {
            return OBJECT_MAPPER.createObjectNode( ).put( CronTriggerHandler.KEY_CRON_EXPRESSION, cronExpression ).toString( );
        }
        return null;
    }

    /**
     * Validates the trigger form fields: name presence, schedule presence and JSON validity of the optional input data.
     *
     * @param name
     *            the trigger name
     * @param configuration
     *            the assembled configuration JSON (null when no schedule was provided)
     * @param inputData
     *            the optional input data JSON
     * @return a typed validation result carrying error codes
     */
    public TriggerValidationResult validateTrigger( String name, String configuration, String inputData )
    {
        List<ErrorCode> errors = new ArrayList<>( );

        if ( name == null || name.trim( ).isEmpty( ) )
        {
            errors.add( ErrorCode.MISSING_NAME );
        }
        if ( configuration == null )
        {
            errors.add( ErrorCode.MISSING_SCHEDULE );
        }
        if ( inputData != null && !inputData.trim( ).isEmpty( ) )
        {
            try
            {
                OBJECT_MAPPER.readTree( inputData );
            }
            catch( Exception e )
            {
                errors.add( ErrorCode.INVALID_INPUT_DATA );
            }
        }
        return new TriggerValidationResult( errors );
    }

    /**
     * Creates and persists a new trigger for a pipeline.
     *
     * @param pipelineId
     *            the owning pipeline identifier
     * @param clientId
     *            the owning client identifier
     * @param name
     *            the trigger name
     * @param configuration
     *            the configuration JSON
     * @param inputData
     *            the optional input data JSON
     * @param active
     *            whether the trigger is active
     * @return the persisted trigger
     */
    public PipelineTrigger createTrigger( int pipelineId, int clientId, String name, String configuration, String inputData, boolean active )
    {
        PipelineTrigger trigger = new PipelineTrigger( );
        trigger.setIdPipeline( pipelineId );
        trigger.setIdClient( clientId );
        trigger.setName( name.trim( ) );
        trigger.setTriggerType( CronTriggerHandler.TYPE );
        trigger.setConfiguration( configuration );
        trigger.setInputData( inputData );
        trigger.setActive( active );
        int nTriggerId = PipelineTriggerHome.create( trigger );
        trigger.setId( nTriggerId );
        return trigger;
    }

    /**
     * Updates an existing trigger's editable fields.
     *
     * @param triggerId
     *            the trigger identifier
     * @param name
     *            the trigger name
     * @param configuration
     *            the configuration JSON
     * @param inputData
     *            the optional input data JSON
     * @param active
     *            whether the trigger is active
     * @return the updated trigger
     * @throws ResourceNotFoundException
     *             if the trigger does not exist
     */
    public PipelineTrigger updateTrigger( int triggerId, String name, String configuration, String inputData, boolean active )
    {
        PipelineTrigger trigger = findTrigger( triggerId );
        trigger.setName( name.trim( ) );
        trigger.setConfiguration( configuration );
        trigger.setInputData( inputData );
        trigger.setActive( active );
        PipelineTriggerHome.update( trigger );
        return trigger;
    }

    /**
     * Flips the active flag of a trigger and persists the change.
     *
     * @param triggerId
     *            the trigger identifier
     * @return the updated trigger
     * @throws ResourceNotFoundException
     *             if the trigger does not exist
     */
    public PipelineTrigger toggleActive( int triggerId )
    {
        PipelineTrigger trigger = findTrigger( triggerId );
        trigger.setActive( !trigger.isActive( ) );
        PipelineTriggerHome.update( trigger );
        return trigger;
    }

    /**
     * Loads a trigger by primary key or throws when absent.
     *
     * @param triggerId
     *            the trigger identifier
     * @return the trigger entity
     * @throws ResourceNotFoundException
     *             if the trigger does not exist
     */
    private PipelineTrigger findTrigger( int triggerId )
    {
        return PipelineTriggerHome.findByPrimaryKey( triggerId ).orElseThrow( ( ) -> new ResourceNotFoundException( TRIGGER_NOT_FOUND_MESSAGE ) );
    }
}
