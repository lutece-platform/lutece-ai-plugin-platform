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
package fr.paris.lutece.plugins.platform.rs.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.service.exception.PayloadTooLargeException;

/**
 * Bounded-read contract of {@link RequestBodyReader}: a body within the limit is returned intact, a body over the limit fails fast with the typed 413 exception
 * instead of being buffered whole.
 */
public class RequestBodyReaderTest
{
    /**
     * A body within the limit is returned intact, including the exact-limit edge case.
     */
    @Test
    public void testBodyWithinLimitIsReturned( ) throws Exception
    {
        byte [ ] payload = "hello".getBytes( );

        assertArrayEquals( payload, RequestBodyReader.readBounded( new ByteArrayInputStream( payload ), 10 ) );
        assertArrayEquals( payload, RequestBodyReader.readBounded( new ByteArrayInputStream( payload ), 5 ) );
    }

    /**
     * A body over the limit triggers the typed payload-too-large exception.
     */
    @Test
    public void testBodyOverLimitIsRejected( )
    {
        byte [ ] payload = "oversized payload".getBytes( );

        assertThrows( PayloadTooLargeException.class, ( ) -> RequestBodyReader.readBounded( new ByteArrayInputStream( payload ), 5 ) );
    }
}
