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
 * Base runtime exception for all agent service related errors. This exception serves as the parent class for all specific agent service exceptions, providing a
 * common exception hierarchy for the agent module.
 */
public class AgentServiceException extends RuntimeException
{
    private static final String DEFAULT_AGENT_SERVICE_ERROR_MESSAGE = "An error occurred in the agent service";

    /**
     * Constructs a new AgentServiceException with the specified detail message.
     *
     * @param message
     *            the detail message explaining the error
     */
    public AgentServiceException( String message )
    {
        super( message );
    }

    /**
     * Constructs a new AgentServiceException with the specified detail message and cause.
     *
     * @param message
     *            the detail message explaining the error
     * @param cause
     *            the cause of this exception
     */
    public AgentServiceException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Constructs a new AgentServiceException with a default message.
     */
    public AgentServiceException( )
    {
        super( DEFAULT_AGENT_SERVICE_ERROR_MESSAGE );
    }

    /**
     * Constructs a new AgentServiceException with the specified cause and a default message.
     *
     * @param cause
     *            the cause of this exception
     */
    public AgentServiceException( Throwable cause )
    {
        super( DEFAULT_AGENT_SERVICE_ERROR_MESSAGE, cause );
    }
}
