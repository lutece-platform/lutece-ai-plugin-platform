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
 * Resource ID service for Platform Observability. This service manages RBAC permissions for observability features per client.
 */
public final class PlatformObservabilityResourceIdService extends ResourceIdService
{
    public static final String PERMISSION_VIEW_OBSERVABILITY = "VIEW_OBSERVABILITY";

    public static final String RESOURCE_TYPE = "PLATFORM_OBSERVABILITY";

    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "platform.rbac.observability.resourceType";
    private static final String PROPERTY_LABEL_VIEW_OBSERVABILITY = "platform.rbac.permission.label.viewObservability";

    /**
     * Builds the observability resource ID service and binds it to the platform plugin
     */
    public PlatformObservabilityResourceIdService( )
    {
        setPluginName( PlatformPlugin.PLUGIN_NAME );
    }

    /**
     * Retrieves a list of all platform clients as a ReferenceList for observability.
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
            referenceList.addItem( String.valueOf( client.getId( ) ), client.getName( ) + " - Observability" );
        }

        return referenceList;
    }

    /**
     * Retrieves the title (name) of a specific client observability by its ID.
     *
     * @param strId
     *            The string representation of the client ID
     * @param locale
     *            The current locale for internationalization
     * @return The name of the client with observability suffix or an empty string if not found
     */
    @Override
    public String getTitle( String strId, Locale locale )
    {
        try
        {
            int nIdClient = Integer.parseInt( strId );
            Client client = ClientHome.findByPrimaryKey( nIdClient ).orElse( null );
            return ( client == null ) ? StringUtils.EMPTY : client.getName( ) + " - Observability";
        }
        catch( NumberFormatException ne )
        {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Registers the platform observability resource type and its associated permissions.
     */
    @Override
    public void register( )
    {
        ResourceType resourceType = new ResourceType( );
        resourceType.setResourceIdServiceClass( PlatformObservabilityResourceIdService.class.getName( ) );
        resourceType.setPluginName( "platform" );
        resourceType.setResourceTypeKey( RESOURCE_TYPE );
        resourceType.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

        Permission permissionViewObservability = new Permission( );
        permissionViewObservability.setPermissionKey( PERMISSION_VIEW_OBSERVABILITY );
        permissionViewObservability.setPermissionTitleKey( PROPERTY_LABEL_VIEW_OBSERVABILITY );
        resourceType.registerPermission( permissionViewObservability );

        ResourceTypeManager.registerResourceType( resourceType );
    }
}
