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
package fr.paris.lutece.plugins.platform.service.pipeline.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Typed result of trigger form validation. Holds presentation-neutral error codes; the controller boundary maps each code to an i18n key. A result with an
 * empty error list is considered valid.
 */
public final class TriggerValidationResult
{
    /**
     * Error codes produced by the trigger validation rules. The controller boundary owns the mapping to i18n keys.
     */
    public enum ErrorCode
    {
        MISSING_NAME,
        MISSING_SCHEDULE,
        INVALID_INPUT_DATA
    }

    private final List<ErrorCode> _errors;

    /**
     * Builds a validation result from the collected error codes.
     *
     * @param errors
     *            the error codes (may be empty for a valid result)
     */
    public TriggerValidationResult( List<ErrorCode> errors )
    {
        _errors = errors == null ? new ArrayList<>( ) : new ArrayList<>( errors );
    }

    /**
     * Indicates whether the validated trigger form is valid.
     *
     * @return true when no error was collected
     */
    public boolean isValid( )
    {
        return _errors.isEmpty( );
    }

    /**
     * Returns the collected error codes.
     *
     * @return an unmodifiable list of error codes (possibly empty)
     */
    public List<ErrorCode> getErrors( )
    {
        return Collections.unmodifiableList( _errors );
    }
}
