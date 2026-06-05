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

/**
 * Result of a rate limit check: whether the request is allowed, and when denied, the user-facing message and the quota reset time. Shared by every per-resource
 * rate limit service.
 */
public class RateLimitResult
{
    private final boolean allowed;
    private final boolean internalError;
    private final String errorMessage;
    private final Timestamp resetTime;

    /**
     * Constructs a RateLimitResult.
     *
     * @param allowed
     *            whether the request is allowed
     * @param errorMessage
     *            error message if request is denied
     * @param resetTime
     *            timestamp when the rate limit will reset
     */
    public RateLimitResult( boolean allowed, String errorMessage, Timestamp resetTime )
    {
        this( allowed, false, errorMessage, resetTime );
    }

    /**
     * Full constructor.
     *
     * @param allowed
     *            whether the request is allowed
     * @param internalError
     *            whether the denial is caused by an internal failure of the check itself
     * @param errorMessage
     *            error message if request is denied
     * @param resetTime
     *            timestamp when the rate limit will reset
     */
    public RateLimitResult( boolean allowed, boolean internalError, String errorMessage, Timestamp resetTime )
    {
        this.allowed = allowed;
        this.internalError = internalError;
        this.errorMessage = errorMessage;
        this.resetTime = resetTime;
    }

    /**
     * Tells whether the denial comes from an internal failure of the rate limit check, as opposed to a genuine quota excess. Such a failure must be reported as
     * a server error, never as a rate limit.
     *
     * @return true when the check itself failed
     */
    public boolean isInternalError( )
    {
        return internalError;
    }

    /**
     * Tells whether the request is allowed.
     *
     * @return true if the request may proceed
     */
    public boolean isAllowed( )
    {
        return allowed;
    }

    /**
     * Returns the user-facing denial message.
     *
     * @return the message, or null when the request is allowed
     */
    public String getErrorMessage( )
    {
        return errorMessage;
    }

    /**
     * Returns the time at which the quota resets.
     *
     * @return the reset time, or null when the request is allowed
     */
    public Timestamp getResetTime( )
    {
        return resetTime;
    }
}
