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
package fr.paris.lutece.plugins.platform.service.pipeline.trigger;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTrigger;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineTriggerHome;
import fr.paris.lutece.plugins.platform.service.pipeline.IPipelineService;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineService;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Daemon that evaluates active pipeline triggers every minute and fires matching ones.
 */
public class PipelineTriggerDaemon extends Daemon
{
    private static final String LOG_TRIGGER_FIRED = "Trigger '%s' (id=%d) fired for pipeline %d - executionId: %s";
    private static final String LOG_TRIGGER_ERROR = "Error while evaluating trigger %d : %s";
    private static final ObjectMapper _mapper = new ObjectMapper( );
    private final CronTriggerHandler _cronHandler = new CronTriggerHandler( );

    /**
     * {@inheritDoc}
     */
    @Override
    public void run( )
    {
        List<PipelineTrigger> activeTriggers = PipelineTriggerHome.findAllActive( );

        for ( PipelineTrigger trigger : activeTriggers )
        {
            processTrigger( trigger );
        }
    }

    /**
     * Evaluates a single trigger and fires it if conditions are met.
     *
     * @param trigger
     *            The trigger to evaluate
     */
    private void processTrigger( PipelineTrigger trigger )
    {
        try
        {
            ITriggerHandler handler = getHandler( trigger.getTriggerType( ) );
            if ( handler == null || !handler.shouldTrigger( trigger.getConfiguration( ), trigger.getLastTriggeredAt( ) ) )
            {
                return;
            }

            Map<String, Object> inputs = parseInputData( trigger.getInputData( ) );
            IPipelineService pipelineService = CDI.current( ).select( PipelineService.class ).get( );
            String executionId = pipelineService.executePipeline( trigger.getIdPipeline( ), trigger.getIdClient( ), inputs );

            PipelineTriggerHome.updateLastTriggeredAt( trigger.getId( ), new Timestamp( System.currentTimeMillis( ) ) );
            AppLogService.info( String.format( LOG_TRIGGER_FIRED, trigger.getName( ), trigger.getId( ), trigger.getIdPipeline( ), executionId ) );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( LOG_TRIGGER_ERROR, trigger.getId( ), e.getMessage( ) ), e );
        }
    }

    /**
     * Returns the handler for the given trigger type.
     *
     * @param strType
     *            The trigger type
     * @return The handler, or null if unknown
     */
    private ITriggerHandler getHandler( String strType )
    {
        if ( _cronHandler.getType( ).equals( strType ) )
        {
            return _cronHandler;
        }
        return null;
    }

    /**
     * Parses input data JSON string into a map.
     *
     * @param strInputData
     *            The JSON string
     * @return The parsed map, empty if null or invalid
     */
    private Map<String, Object> parseInputData( String strInputData )
    {
        if ( strInputData == null || strInputData.isEmpty( ) )
        {
            return new HashMap<>( );
        }
        try
        {
            return _mapper.readValue( strInputData, new TypeReference<Map<String, Object>>( )
            {
            } );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error while parsing trigger inputs: {}", e.getMessage( ), e );
            return new HashMap<>( );
        }
    }
}
