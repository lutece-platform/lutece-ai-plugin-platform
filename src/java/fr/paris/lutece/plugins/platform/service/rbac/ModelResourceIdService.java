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

import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.portal.service.rbac.Permission;
import fr.paris.lutece.portal.service.rbac.ResourceIdService;
import fr.paris.lutece.portal.service.rbac.ResourceType;
import fr.paris.lutece.portal.service.rbac.ResourceTypeManager;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.ReferenceList;

public class ModelResourceIdService extends ResourceIdService
{
    private static final String PROPERTY_LABEL_RESOURCE_TYPE = "platform.agent.rbac.model.resourceType";
    private static final String PROPERTY_LABEL_VIEW = "platform.agent.rbac.model.permission.view";
    private static final String PROPERTY_LABEL_CREATE = "platform.agent.rbac.model.permission.create";
    private static final String PROPERTY_LABEL_MODIFY = "platform.agent.rbac.model.permission.modify";
    private static final String PROPERTY_LABEL_DELETE = "platform.agent.rbac.model.permission.delete";

    /**
     * Creates a new ModelResourceIdService and binds it to the platform plugin.
     */
    public ModelResourceIdService( )
    {
        setPluginName( "platform" );
    }

    @Override
    public void register( )
    {
        try
        {
            ResourceType rt = new ResourceType( );
            rt.setResourceIdServiceClass( ModelResourceIdService.class.getName( ) );
            rt.setPluginName( "platform" );
            rt.setResourceTypeKey( Model.RESOURCE_TYPE );
            rt.setResourceTypeLabelKey( PROPERTY_LABEL_RESOURCE_TYPE );

            Permission p;

            p = new Permission( );
            p.setPermissionKey( Model.PERMISSION_VIEW );
            p.setPermissionTitleKey( PROPERTY_LABEL_VIEW );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Model.PERMISSION_CREATE );
            p.setPermissionTitleKey( PROPERTY_LABEL_CREATE );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Model.PERMISSION_MODIFY );
            p.setPermissionTitleKey( PROPERTY_LABEL_MODIFY );
            rt.registerPermission( p );

            p = new Permission( );
            p.setPermissionKey( Model.PERMISSION_DELETE );
            p.setPermissionTitleKey( PROPERTY_LABEL_DELETE );
            rt.registerPermission( p );

            ResourceTypeManager.registerResourceType( rt );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error registering Model RBAC permissions: {}", e.getMessage( ), e );
        }
    }

    @Override
    public ReferenceList getResourceIdList( Locale locale )
    {
        return ModelHome.getModelsReferenceList( );
    }

    @Override
    public String getTitle( String strId, Locale locale )
    {
        try
        {
            int nId = Integer.parseInt( strId );
            Model model = ModelHome.findByPrimaryKey( nId ).orElse( null );
            if ( model == null )
            {
                return "Model #" + strId;
            }
            Provider provider = ProviderHome.findByPrimaryKey( model.getProviderId( ) ).orElse( null );
            return provider != null ? provider.getProviderName( ) : "Model #" + strId;
        }
        catch( NumberFormatException e )
        {
            return "Model invalide";
        }
    }
}
