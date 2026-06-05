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
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Cron-based trigger handler. Supports two modes: - interval_minutes: fires every N minutes since last execution - cron_expression: 5-field cron (minute, hour,
 * day-of-month, month, day-of-week)
 */
public class CronTriggerHandler implements ITriggerHandler
{
    public static final String TYPE = "CRON";
    public static final String KEY_INTERVAL_MINUTES = "interval_minutes";
    public static final String KEY_CRON_EXPRESSION = "cron_expression";
    private static final String LOG_PARSE_ERROR = "Erreur de parsing de la configuration du trigger CRON : %s";
    private static final ObjectMapper _mapper = new ObjectMapper( );

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType( )
    {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldTrigger( String strConfiguration, Timestamp lastTriggeredAt )
    {
        try
        {
            JsonNode config = _mapper.readTree( strConfiguration );

            if ( config.has( KEY_INTERVAL_MINUTES ) )
            {
                return shouldTriggerByInterval( config.get( KEY_INTERVAL_MINUTES ).asInt( ), lastTriggeredAt );
            }

            if ( config.has( KEY_CRON_EXPRESSION ) )
            {
                return shouldTriggerByCron( config.get( KEY_CRON_EXPRESSION ).asText( ), lastTriggeredAt );
            }

            return false;
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( LOG_PARSE_ERROR, e.getMessage( ) ), e );
            return false;
        }
    }

    /**
     * Checks if enough time has passed since last execution.
     *
     * @param nIntervalMinutes
     *            The interval in minutes
     * @param lastTriggeredAt
     *            The last execution timestamp
     * @return true if the interval has elapsed
     */
    private boolean shouldTriggerByInterval( int nIntervalMinutes, Timestamp lastTriggeredAt )
    {
        if ( lastTriggeredAt == null )
        {
            return true;
        }
        long elapsedMinutes = ( System.currentTimeMillis( ) - lastTriggeredAt.getTime( ) ) / 60000;
        return elapsedMinutes >= nIntervalMinutes;
    }

    /**
     * Checks if the current time matches a 5-field cron expression. Prevents double-firing by checking lastTriggeredAt is not in the same minute.
     *
     * @param strCron
     *            The cron expression (minute hour day-of-month month day-of-week)
     * @param lastTriggeredAt
     *            The last execution timestamp
     * @return true if the current time matches and hasn't fired this minute
     */
    private boolean shouldTriggerByCron( String strCron, Timestamp lastTriggeredAt )
    {
        LocalDateTime now = LocalDateTime.now( );

        if ( lastTriggeredAt != null )
        {
            LocalDateTime lastTime = lastTriggeredAt.toInstant( ).atZone( ZoneId.systemDefault( ) ).toLocalDateTime( );
            if ( lastTime.getYear( ) == now.getYear( ) && lastTime.getDayOfYear( ) == now.getDayOfYear( ) && lastTime.getHour( ) == now.getHour( )
                    && lastTime.getMinute( ) == now.getMinute( ) )
            {
                return false;
            }
        }

        String [ ] fields = strCron.trim( ).split( "\\s+" );
        if ( fields.length != 5 )
        {
            return false;
        }

        return matchesCronField( fields [0], now.getMinute( ) ) && matchesCronField( fields [1], now.getHour( ) )
                && matchesCronField( fields [2], now.getDayOfMonth( ) ) && matchesCronField( fields [3], now.getMonthValue( ) )
                && matchesCronField( fields [4], now.getDayOfWeek( ).getValue( ) % 7 );
    }

    /**
     * Checks if a value matches a single cron field. Supports: * (any), N (exact), star/N (every N), comma-separated values.
     *
     * @param strField
     *            The cron field pattern
     * @param nValue
     *            The current value to check
     * @return true if the value matches the field
     */
    private boolean matchesCronField( String strField, int nValue )
    {
        if ( "*".equals( strField ) )
        {
            return true;
        }

        if ( strField.startsWith( "*/" ) )
        {
            int step = Integer.parseInt( strField.substring( 2 ) );
            return step > 0 && nValue % step == 0;
        }

        for ( String part : strField.split( "," ) )
        {
            if ( Integer.parseInt( part.trim( ) ) == nValue )
            {
                return true;
            }
        }

        return false;
    }
}
