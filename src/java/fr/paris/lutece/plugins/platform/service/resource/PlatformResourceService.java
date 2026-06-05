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
package fr.paris.lutece.plugins.platform.service.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.resource.PlatformResourceConfigItem;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Service for managing platform resources from different modules
 */
@ApplicationScoped
public class PlatformResourceService
{
    @Inject
    private Instance<IPlatformResourceType> _resourceTypes;

    /**
     * Get all platform resources from all registered IPlatformResource beans
     *
     * @return List of platform resources
     */
    public List<IPlatformResourceType> getAllResourceType( )
    {
        List<IPlatformResourceType> list = new ArrayList<>( );
        _resourceTypes.forEach( list::add );
        return list;
    }

    /**
     * Get a specific resource type by its identifier
     *
     * @param resourceType
     *            The resource type identifier
     * @return The IPlatformResourceType or null if not found
     */
    public IPlatformResourceType getResourceType( String resourceType )
    {
        return getAllResourceType( ).stream( ).filter( rt -> rt.getResourceType( ).equals( resourceType ) ).findFirst( ).orElse( null );
    }

    /**
     * Aggregates the configuration items of every registered resource type for the given user, keyed by resource type identifier. Resource types exposing an
     * empty or null configuration list are omitted.
     *
     * @param user
     *            The user requesting the configuration
     * @return A map of resource type identifier to its non-empty list of configuration items, in registration order
     */
    public Map<String, List<PlatformResourceConfigItem>> getConfigListsByType( User user )
    {
        Map<String, List<PlatformResourceConfigItem>> configListsByType = new LinkedHashMap<>( );
        for ( IPlatformResourceType resourceType : getAllResourceType( ) )
        {
            List<PlatformResourceConfigItem> configList = resourceType.getConfigList( user );
            if ( configList != null && !configList.isEmpty( ) )
            {
                configListsByType.put( resourceType.getResourceType( ), configList );
            }
        }
        return configListsByType;
    }

}
