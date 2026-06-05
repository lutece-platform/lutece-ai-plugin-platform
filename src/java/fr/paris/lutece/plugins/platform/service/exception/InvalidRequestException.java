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
package fr.paris.lutece.plugins.platform.service.exception;

/**
 * Exception thrown when a request is invalid or malformed. This exception extends AgentServiceException to provide specific handling for cases where request
 * validation fails.
 */
public class InvalidRequestException extends AgentServiceException
{
    private static final String DEFAULT_INVALID_REQUEST_MESSAGE = "The request is invalid or malformed";

    /**
     * Constructs a new InvalidRequestException with the specified detail message.
     *
     * @param message
     *            the detail message explaining the request validation failure
     */
    public InvalidRequestException( String message )
    {
        super( message );
    }

    /**
     * Constructs a new InvalidRequestException with a default message.
     */
    public InvalidRequestException( )
    {
        super( DEFAULT_INVALID_REQUEST_MESSAGE );
    }

    /**
     * Constructs a new InvalidRequestException with the specified detail message and cause.
     *
     * @param message
     *            the detail message explaining the request validation failure
     * @param cause
     *            the cause of this exception
     */
    public InvalidRequestException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
