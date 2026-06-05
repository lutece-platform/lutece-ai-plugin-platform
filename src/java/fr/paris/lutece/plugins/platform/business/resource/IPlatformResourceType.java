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
package fr.paris.lutece.plugins.platform.business.resource;

import java.util.List;

import fr.paris.lutece.api.user.User;

/**
 * Interface for Platform Resource
 */
public interface IPlatformResourceType
{

    /**
     * Gets the resource type identifier
     *
     * @return The resource type as a String
     */
    String getResourceType( );

    /**
     * Gets the resource type label
     *
     * @return The resource type label as a String
     */
    String getNameKey( );

    /**
     * Gets the resource description key
     *
     * @return The resource description key as a String
     */
    String getDescriptionKey( );

    /**
     * Gets the icon representing the resource type
     *
     * @return The icon URL as a String
     */
    String getIcon( );

    /**
     * Retrieves the complete list of platform resources
     *
     * @return A List of PlatformResource objects
     */
    List<PlatformResourceItem> getResourceList( );

    /**
     * Retrieves the list of platform resources accessible to a specific admin user
     *
     * @param user
     *            The AdminUser requesting the resources
     * @param client
     *            The Client context for the request
     * @return A List of PlatformResource objects filtered by user permissions
     */
    List<PlatformResourceItem> getResourceList( int nIdClient, User user );

    /**
     * Gets a specific platform resource by its identifier
     *
     * @param nIdResource
     *            The resource identifier
     * @return The PlatformResource object corresponding to the identifier
     */
    PlatformResourceItem getResource( int nIdResource, int nIdClient, User user );

    /**
     * Gets the administrative URL for a resource
     *
     * @param idClient
     *            The client identifier
     * @return The administrative URL as a String
     */

    String getCreateUrl( String idClient );

    /**
     * Returns the configuration options for this resource type
     *
     * @param user
     *            The current admin user
     * @return List of configuration items
     */
    List<PlatformResourceConfigItem> getConfigList( User user );

    /**
     * Returns the front-office URL for displaying the observability execution list for a specific resource. Each resource type can provide its own list page
     * with custom sidebar and layout.
     *
     * @param nClientId
     *            The client identifier
     * @param strResourceId
     *            The resource identifier
     * @return The observability list URL as a String, or null if not supported
     */
    default String getObservabilityListUrl( int nClientId, String strResourceId )
    {
        return null;
    }

    /**
     * Returns the skin (front-office) template path for displaying execution details specific to this resource type.
     *
     * @return The skin template path as a String, or null to use the default template
     */
    default String getSkinExecutionDetailTemplatePath( )
    {
        return null;
    }

}
