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

import java.util.Locale;

import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.portal.service.rbac.Permission;
import fr.paris.lutece.portal.service.rbac.ResourceIdService;
import fr.paris.lutece.portal.service.rbac.ResourceType;
import fr.paris.lutece.portal.service.rbac.ResourceTypeManager;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.ReferenceList;

public class DatasetResourceIdService extends ResourceIdService
{
    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "platform.agent.rbac.dataset.resourceType";
    private static final String PROPERTY_LABEL_VIEW = "platform.agent.rbac.dataset.permission.view";
    private static final String PROPERTY_LABEL_CREATE = "platform.agent.rbac.dataset.permission.create";
    private static final String PROPERTY_LABEL_MODIFY = "platform.agent.rbac.dataset.permission.modify";
    private static final String PROPERTY_LABEL_DELETE = "platform.agent.rbac.dataset.permission.delete";

    /**
     * Creates a new DatasetResourceIdService and binds it to the platform plugin.
     */
    public DatasetResourceIdService( )
    {
        setPluginName( "platform" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register( )
    {
        try
        {
            ResourceType rt = new ResourceType( );
            rt.setResourceIdServiceClass( DatasetResourceIdService.class.getName( ) );
            rt.setPluginName( "platform" );
            rt.setResourceTypeKey( Dataset.RESOURCE_TYPE );
            rt.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

            Permission p;

            p = new Permission( );
            p.setPermissionKey( Dataset.PERMISSION_VIEW );
            p.setPermissionTitleKey( PROPERTY_LABEL_VIEW );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Dataset.PERMISSION_CREATE );
            p.setPermissionTitleKey( PROPERTY_LABEL_CREATE );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Dataset.PERMISSION_MODIFY );
            p.setPermissionTitleKey( PROPERTY_LABEL_MODIFY );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Dataset.PERMISSION_DELETE );
            p.setPermissionTitleKey( PROPERTY_LABEL_DELETE );
            rt.registerPermission( p );

            ResourceTypeManager.registerResourceType( rt );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error registering Dataset RBAC permissions: {}", e.getMessage( ), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList getResourceIdList( Locale locale )
    {
        return DatasetHome.getDatasetsReferenceList( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle( String strId, Locale locale )
    {
        try
        {
            int nId = Integer.parseInt( strId );
            Dataset dataset = DatasetHome.findByPrimaryKey( nId ).orElse( null );
            return dataset != null ? dataset.getDatasetName( ) : "Dataset #" + strId;
        }
        catch( NumberFormatException e )
        {
            return "Dataset invalide";
        }
    }
}
