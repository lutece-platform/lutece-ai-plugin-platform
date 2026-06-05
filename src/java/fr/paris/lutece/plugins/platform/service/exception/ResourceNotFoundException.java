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
 * Exception thrown when a requested resource (bot, dataset, document, pipeline, version, vision, model, MCP server, conversation...) cannot be found. Mapped to
 * HTTP 404 by the REST exception mapper — a single type guarantees every not-found case maps to 404 by construction.
 */
public class ResourceNotFoundException extends AgentServiceException
{
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "The requested resource was not found";

    /**
     * Constructor with the default message.
     */
    public ResourceNotFoundException( )
    {
        super( DEFAULT_MESSAGE );
    }

    /**
     * Constructor with message.
     *
     * @param message
     *            the error message
     */
    public ResourceNotFoundException( String message )
    {
        super( message );
    }

    /**
     * Constructor with message and cause.
     *
     * @param message
     *            the error message
     * @param cause
     *            the underlying cause
     */
    public ResourceNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
