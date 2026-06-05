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
package fr.paris.lutece.plugins.platform.service.bot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

/**
 * Decorator around a {@link ChatMemory} that runs every call through a captured executor.
 */
public class ContextualChatMemory implements ChatMemory
{
    private final ChatMemory _delegate;
    private final Executor _ctxExecutor;

    /**
     * Constructs a contextual proxy.
     *
     * @param delegate
     *            the underlying chat memory
     * @param ctxExecutor
     *            the executor used to run each call (typically a contextual executor)
     */
    public ContextualChatMemory( ChatMemory delegate, Executor ctxExecutor )
    {
        this._delegate = delegate;
        this._ctxExecutor = ctxExecutor;
    }

    /**
     * Returns the conversation identifier of the underlying chat memory.
     *
     * @return the identifier
     */
    @Override
    public Object id( )
    {
        return _delegate.id( );
    }

    /**
     * Adds a message to the underlying chat memory.
     *
     * @param message
     *            the message to add
     */
    @Override
    public void add( ChatMessage message )
    {
        _ctxExecutor.execute( ( ) -> _delegate.add( message ) );
    }

    /**
     * Returns the messages from the underlying chat memory, waiting synchronously for the result.
     *
     * @return the list of messages
     */
    @Override
    public List<ChatMessage> messages( )
    {
        CompletableFuture<List<ChatMessage>> future = new CompletableFuture<>( );
        _ctxExecutor.execute( ( ) -> {
            try
            {
                future.complete( _delegate.messages( ) );
            }
            catch( Throwable t )
            {
                future.completeExceptionally( t );
            }
        } );
        return future.join( );
    }

    /**
     * Clears the underlying chat memory.
     */
    @Override
    public void clear( )
    {
        _ctxExecutor.execute( _delegate::clear );
    }
}
