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
package fr.paris.lutece.plugins.platform.service.security;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Template of the per-user daily rate limit algorithm shared by the bot and pipeline services: resolve the resource's daily limit (absent resource denies,
 * non-positive limit allows), create the counter on first use, reset it after 24 hours, then increment it atomically while strictly below the limit (the
 * conditional UPDATE makes concurrent checks race-free). Subclasses only provide the per-domain storage hooks and wording.
 *
 * @param <E>
 *            the rate limit entity of the domain
 */
public abstract class AbstractUserRateLimitService<E>
{
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Erreur interne du serveur";
    private static final String RATE_LIMIT_CHECK_ERROR_MESSAGE = "Error while checking the rate limit";
    private static final String CLEANUP_COMPLETED_MESSAGE = "Cleanup of expired rate limit entries completed";
    private static final String CLEANUP_ERROR_MESSAGE = "Error while cleaning up rate limit entries";
    private static final String DATE_PATTERN = "dd/MM/yyyy à HH:mm";

    protected static final int HOURS_IN_DAY = 24;

    /**
     * Checks if a user has exceeded the daily rate limit for a specific resource.
     *
     * @param userId
     *            the user identifier
     * @param resourceId
     *            the resource identifier
     * @return RateLimitResult containing the check result
     */
    public RateLimitResult checkRateLimit( String userId, int resourceId )
    {
        try
        {
            OptionalInt optLimit = findDailyLimit( resourceId );
            if ( optLimit.isEmpty( ) )
            {
                return new RateLimitResult( false, resourceNotFoundMessage( ), null );
            }

            int dailyLimit = optLimit.getAsInt( );
            if ( dailyLimit <= 0 )
            {
                return new RateLimitResult( true, null, null );
            }

            return processRateLimitCheck( userId, resourceId, dailyLimit );
        }
        catch( Exception e )
        {
            AppLogService.error( RATE_LIMIT_CHECK_ERROR_MESSAGE, e );
            return new RateLimitResult( false, true, INTERNAL_SERVER_ERROR_MESSAGE, null );
        }
    }

    /**
     * Cleans up expired rate limit entries older than 24 hours.
     */
    public void cleanExpiredEntries( )
    {
        try
        {
            removeEntriesBefore( Timestamp.valueOf( LocalDateTime.now( ).minusHours( HOURS_IN_DAY ) ) );
            AppLogService.info( CLEANUP_COMPLETED_MESSAGE );
        }
        catch( Exception e )
        {
            AppLogService.error( CLEANUP_ERROR_MESSAGE, e );
        }
    }

    /**
     * Runs the create / reset / atomic-increment sequence for a user and resource.
     *
     * @param userId
     *            the user identifier
     * @param resourceId
     *            the resource identifier
     * @param dailyLimit
     *            the daily limit
     * @return RateLimitResult containing the check result
     */
    private RateLimitResult processRateLimitCheck( String userId, int resourceId, int dailyLimit )
    {
        Timestamp now = Timestamp.valueOf( LocalDateTime.now( ) );
        E entry = findEntry( userId, resourceId ).orElse( null );

        if ( entry == null )
        {
            createEntry( userId, resourceId, now );
            return new RateLimitResult( true, null, null );
        }

        LocalDateTime windowStart = windowStart( entry ).toLocalDateTime( );

        if ( ChronoUnit.HOURS.between( windowStart, now.toLocalDateTime( ) ) >= HOURS_IN_DAY )
        {
            resetEntry( entry, now );
            return new RateLimitResult( true, null, null );
        }

        if ( incrementIfBelow( entry, dailyLimit ) )
        {
            return new RateLimitResult( true, null, null );
        }

        return createExceededResult( windowStart, dailyLimit );
    }

    /**
     * Builds the denial result, formatting the reset time in the user-facing message.
     *
     * @param windowStart
     *            the start of the current 24-hour window
     * @param dailyLimit
     *            the daily limit
     * @return RateLimitResult denying the request
     */
    private RateLimitResult createExceededResult( LocalDateTime windowStart, int dailyLimit )
    {
        LocalDateTime resetTime = windowStart.plusHours( HOURS_IN_DAY );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( DATE_PATTERN, Locale.FRANCE );
        String errorMessage = String.format( exceededMessageTemplate( ), dailyLimit, resetTime.format( formatter ) );
        return new RateLimitResult( false, errorMessage, Timestamp.valueOf( resetTime ) );
    }

    /**
     * Resolves the daily limit configured on the resource.
     *
     * @param resourceId
     *            the resource identifier
     * @return the limit, or empty when the resource does not exist
     */
    protected abstract OptionalInt findDailyLimit( int resourceId );

    /**
     * Loads the rate limit entry of a user for a resource.
     *
     * @param userId
     *            the user identifier
     * @param resourceId
     *            the resource identifier
     * @return the entry, or empty on first use
     */
    protected abstract Optional<E> findEntry( String userId, int resourceId );

    /**
     * Creates the first entry of a window with an initial count of one.
     *
     * @param userId
     *            the user identifier
     * @param resourceId
     *            the resource identifier
     * @param now
     *            the window start
     */
    protected abstract void createEntry( String userId, int resourceId, Timestamp now );

    /**
     * Returns the start of the entry's current 24-hour window.
     *
     * @param entry
     *            the rate limit entry
     * @return the window start
     */
    protected abstract Timestamp windowStart( E entry );

    /**
     * Resets the entry for a new 24-hour window with an initial count of one.
     *
     * @param entry
     *            the rate limit entry
     * @param now
     *            the new window start
     */
    protected abstract void resetEntry( E entry, Timestamp now );

    /**
     * Atomically increments the entry's count while strictly below the limit.
     *
     * @param entry
     *            the rate limit entry
     * @param dailyLimit
     *            the daily limit
     * @return true if incremented, false when the limit is reached
     */
    protected abstract boolean incrementIfBelow( E entry, int dailyLimit );

    /**
     * Deletes the entries whose window started before the cutoff.
     *
     * @param cutoff
     *            the expiry cutoff
     */
    protected abstract void removeEntriesBefore( Timestamp cutoff );

    /**
     * Returns the user-facing message for an unknown resource.
     *
     * @return the message
     */
    protected abstract String resourceNotFoundMessage( );

    /**
     * Returns the user-facing denial template, with the limit and formatted reset time as {@code %d} and {@code %s} placeholders.
     *
     * @return the template
     */
    protected abstract String exceededMessageTemplate( );
}
