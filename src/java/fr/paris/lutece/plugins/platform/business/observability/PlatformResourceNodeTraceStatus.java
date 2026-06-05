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
package fr.paris.lutece.plugins.platform.business.observability;

/**
 * Enumeration of platform resource node trace status values.
 */
public enum PlatformResourceNodeTraceStatus
{
    INFO( "INFO" ),
    WARNING( "WARNING" ),
    ERROR( "ERROR" ),
    DEBUG( "DEBUG" );

    private final String value;

    /**
     * Constructor for the enum with the associated value.
     *
     * @param value
     *            The string value associated with the status
     */
    PlatformResourceNodeTraceStatus( String value )
    {
        this.value = value;
    }

    /**
     * Returns the string value associated with the status.
     *
     * @return The string value
     */
    public String getValue( )
    {
        return value;
    }

    /**
     * Converts a string value to its corresponding enum constant.
     *
     * @param value
     *            The string value to convert
     * @return The corresponding enum constant
     * @throws IllegalArgumentException
     *             if the value doesn't match any enum constant
     */
    public static PlatformResourceNodeTraceStatus fromString( String value )
    {
        for ( PlatformResourceNodeTraceStatus status : PlatformResourceNodeTraceStatus.values( ) )
        {
            if ( status.getValue( ).equals( value ) )
            {
                return status;
            }
        }
        throw new IllegalArgumentException( "Unknown status: " + value );
    }
}
