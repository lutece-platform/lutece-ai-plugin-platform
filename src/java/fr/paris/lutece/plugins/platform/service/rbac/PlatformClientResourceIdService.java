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
package fr.paris.lutece.plugins.platform.service.rbac;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.service.PlatformPlugin;
import fr.paris.lutece.portal.service.rbac.Permission;
import fr.paris.lutece.portal.service.rbac.ResourceIdService;
import fr.paris.lutece.portal.service.rbac.ResourceType;
import fr.paris.lutece.portal.service.rbac.ResourceTypeManager;
import fr.paris.lutece.util.ReferenceList;

/**
 * Resource ID service for Platform Clients. This service manages RBAC (Role-Based Access Control) permissions for platform clients. It handles resource
 * identification, permission management, and resource type registration.
 */
public final class PlatformClientResourceIdService extends ResourceIdService
{
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_CREATE = "CREATE";
    public static final String PERMISSION_MODIFY = "MODIFY";
    public static final String PERMISSION_DELETE = "DELETE";

    public static final String RESOURCE_TYPE = "PLATFORM_CLIENT";

    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "platform.rbac.client.resourceType";
    private static final String PROPERTY_LABEL_VIEW = "platform.rbac.permission.label.view";
    private static final String PROPERTY_LABEL_CREATE = "platform.rbac.permission.label.create";
    private static final String PROPERTY_LABEL_MODIFY = "platform.rbac.permission.label.modify";
    private static final String PROPERTY_LABEL_DELETE = "platform.rbac.permission.label.delete";

    /**
     * Constructs the resource ID service and registers it under the platform plugin name
     */
    public PlatformClientResourceIdService( )
    {
        setPluginName( PlatformPlugin.PLUGIN_NAME );
    }

    /**
     * Retrieves a list of all platform clients as a ReferenceList. Each client is represented by its ID and name in the returned list.
     *
     * @param locale
     *            The current locale for internationalization
     * @return ReferenceList containing client IDs and names
     */
    @Override
    public ReferenceList getResourceIdList( Locale locale )
    {
        ReferenceList referenceList = new ReferenceList( );
        List<Client> listClients = ClientHome.getClientsList( );

        for ( Client client : listClients )
        {
            referenceList.addItem( String.valueOf( client.getId( ) ), client.getName( ) );
        }

        return referenceList;
    }

    /**
     * Retrieves the title (name) of a specific client by its ID. Returns an empty string if the client ID is invalid or the client is not found.
     *
     * @param strId
     *            The string representation of the client ID
     * @param locale
     *            The current locale for internationalization
     * @return The name of the client or an empty string if not found
     */
    @Override
    public String getTitle( String strId, Locale locale )
    {
        try
        {
            int nIdClient = Integer.parseInt( strId );
            Client client = ClientHome.findByPrimaryKey( nIdClient ).orElse( null );
            return ( client == null ) ? StringUtils.EMPTY : client.getName( );
        }
        catch( NumberFormatException ne )
        {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Registers the platform client resource type and its associated permissions. This method sets up all RBAC permissions (VIEW, CREATE, MODIFY, DELETE) for
     * the platform client resource type in the system.
     */
    @Override
    public void register( )
    {
        ResourceType resourceType = new ResourceType( );
        resourceType.setResourceIdServiceClass( PlatformClientResourceIdService.class.getName( ) );
        resourceType.setPluginName( "platform" );
        resourceType.setResourceTypeKey( RESOURCE_TYPE );
        resourceType.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

        Permission permissionView = new Permission( );
        permissionView.setPermissionKey( PERMISSION_VIEW );
        permissionView.setPermissionTitleKey( PROPERTY_LABEL_VIEW );
        resourceType.registerPermission( permissionView );

        Permission permissionCreate = new Permission( );
        permissionCreate.setPermissionKey( PERMISSION_CREATE );
        permissionCreate.setPermissionTitleKey( PROPERTY_LABEL_CREATE );
        resourceType.registerPermission( permissionCreate );

        Permission permissionModify = new Permission( );
        permissionModify.setPermissionKey( PERMISSION_MODIFY );
        permissionModify.setPermissionTitleKey( PROPERTY_LABEL_MODIFY );
        resourceType.registerPermission( permissionModify );

        Permission permissionDelete = new Permission( );
        permissionDelete.setPermissionKey( PERMISSION_DELETE );
        permissionDelete.setPermissionTitleKey( PROPERTY_LABEL_DELETE );
        resourceType.registerPermission( permissionDelete );

        ResourceTypeManager.registerResourceType( resourceType );
    }
}
