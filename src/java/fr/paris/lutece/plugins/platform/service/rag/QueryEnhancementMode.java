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
package fr.paris.lutece.plugins.platform.service.rag;

/**
 * Enum representing the different query enhancement modes available for RAG queries.
 */
public enum QueryEnhancementMode
{

    /**
     * No query enhancement - use the query as-is after compression
     */
    NONE( "NONE" ),

    /**
     * Query expansion only - generate multiple query variants
     */
    EXPANSION_ONLY( "EXPANSION_ONLY" ),

    /**
     * HyDE only - generate a hypothetical document from the compressed query
     */
    HYDE_ONLY( "HYDE_ONLY" ),

    /**
     * Query expansion followed by HyDE on each variant
     */
    EXPANSION_THEN_HYDE( "EXPANSION_THEN_HYDE" ),

    /**
     * Query expansion plus one global HyDE on the compressed query
     */
    EXPANSION_PLUS_HYDE_GLOBAL( "EXPANSION_PLUS_HYDE_GLOBAL" );

    private final String code;

    QueryEnhancementMode( String code )
    {
        this.code = code;
    }

    /**
     * Get the string code for this mode
     *
     * @return the mode code
     */
    public String getCode( )
    {
        return code;
    }

    /**
     * Get a QueryEnhancementMode from its string code
     *
     * @param code
     *            the mode code
     * @return the corresponding QueryEnhancementMode, or NONE if not found
     */
    public static QueryEnhancementMode fromCode( String code )
    {
        if ( code == null )
        {
            return NONE;
        }

        for ( QueryEnhancementMode mode : values( ) )
        {
            if ( mode.code.equals( code ) )
            {
                return mode;
            }
        }
        return NONE;
    }

    /**
     * Check if this mode includes query expansion
     *
     * @return true if query expansion is enabled
     */
    public boolean includesExpansion( )
    {
        return this == EXPANSION_ONLY || this == EXPANSION_THEN_HYDE || this == EXPANSION_PLUS_HYDE_GLOBAL;
    }

    /**
     * Check if this mode includes HyDE
     *
     * @return true if HyDE is enabled
     */
    public boolean includesHyde( )
    {
        return this == HYDE_ONLY || this == EXPANSION_THEN_HYDE || this == EXPANSION_PLUS_HYDE_GLOBAL;
    }

    /**
     * Check if HyDE should be applied to each variant
     *
     * @return true if HyDE should be applied per variant
     */
    public boolean isHydePerVariant( )
    {
        return this == EXPANSION_THEN_HYDE;
    }
}
