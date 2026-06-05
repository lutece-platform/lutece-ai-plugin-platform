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
package fr.paris.lutece.plugins.platform.service.observability.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Observability date window. Encapsulates the offset applied to "now" to compute the lower bound of an observability query, replacing the duplicated
 * date-range-key Calendar arithmetic that previously lived in the controller.
 */
public enum DateRange
{
    /** All-time window. Bounds resolve to a 10-year-back lower bound so daily-stats queries still get a valid range. */
    ALL( "all", 10, ChronoUnit.YEARS ),
    /** Last 7 days. */
    SEVEN_DAYS( "7days", 7, ChronoUnit.DAYS ),
    /** Last 15 days. */
    FIFTEEN_DAYS( "15days", 15, ChronoUnit.DAYS ),
    /** Last month. */
    MONTH( "month", 1, ChronoUnit.MONTHS ),
    /** Last 3 months. */
    THREE_MONTHS( "3months", 3, ChronoUnit.MONTHS );

    private final String _strKey;
    private final long _lAmount;
    private final ChronoUnit _unit;

    /**
     * Builds a date range.
     *
     * @param strKey
     *            the request/template key identifying this range
     * @param lAmount
     *            the offset amount subtracted from now to get the lower bound
     * @param unit
     *            the offset unit
     */
    DateRange( String strKey, long lAmount, ChronoUnit unit )
    {
        _strKey = strKey;
        _lAmount = lAmount;
        _unit = unit;
    }

    /**
     * Gets the request/template key for this range.
     *
     * @return the key
     */
    public String getKey( )
    {
        return _strKey;
    }

    /**
     * Resolves the date range key to the matching enum, falling back to {@link #ALL} when the key is null, empty or unknown.
     *
     * @param strKey
     *            the date range key
     * @return the matching date range, never null
     */
    public static DateRange fromKey( String strKey )
    {
        if ( strKey == null || strKey.isEmpty( ) )
        {
            return ALL;
        }
        for ( DateRange range : values( ) )
        {
            if ( range._strKey.equals( strKey ) )
            {
                return range;
            }
        }
        return ALL;
    }

    /**
     * Computes the [start, end] timestamp bounds of this range, end being the current instant.
     *
     * @return a two-element array holding the start and end timestamps
     */
    public Timestamp [ ] resolveBounds( )
    {
        LocalDateTime now = LocalDateTime.now( );
        Timestamp end = Timestamp.valueOf( now );
        Timestamp start = Timestamp.valueOf( now.minus( _lAmount, _unit ) );
        return new Timestamp [ ] {
                start, end
        };
    }
}
