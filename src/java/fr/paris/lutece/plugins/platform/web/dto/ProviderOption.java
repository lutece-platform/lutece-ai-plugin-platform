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
package fr.paris.lutece.plugins.platform.web.dto;

import java.io.Serializable;

import fr.paris.lutece.plugins.platform.business.provider.Provider;

/**
 * View model carrying the safe, public fields of a {@link Provider} for front-office selection lists. Strips sensitive fields (deployment endpoint, API key) so
 * they never reach a front-office data model.
 */
public class ProviderOption implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final int _nId;
    private final String _strProviderName;
    private final String _strProviderType;

    /**
     * Builds a provider option from a domain entity, keeping only the fields the view needs.
     *
     * @param provider
     *            the source provider entity
     */
    public ProviderOption( Provider provider )
    {
        _nId = provider.getId( );
        _strProviderName = provider.getProviderName( );
        _strProviderType = provider.getProviderType( );
    }

    /**
     * Gets the provider primary key.
     *
     * @return the provider id
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Gets the provider display name.
     *
     * @return the provider name
     */
    public String getProviderName( )
    {
        return _strProviderName;
    }

    /**
     * Gets the provider type.
     *
     * @return the provider type
     */
    public String getProviderType( )
    {
        return _strProviderType;
    }
}
