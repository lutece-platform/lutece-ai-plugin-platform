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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.api.user.User;

@ApplicationScoped
@Named( "platform.visionResourceType" )
public class VisionResourceType implements IPlatformResourceType
{
    public static final String PROPERTY_RESOURCE_NAME = "platform.agent.vision.resource.name";
    public static final String PROPERTY_RESOURCE_DESCRIPTION = "platform.agent.vision.resource.description";

    private static final String JSP_FO_BASE_URL = "jsp/site/Portal.jsp";
    private static final String VIEW_MODIFY_VISION = "page=agent_vision&view=viewVision&vision_id=";
    private static final String VIEW_CREATE_VISION_WITH_CLIENT = "page=agent_vision&view=createVision&client_id=";
    private static final String VIEW_RESOURCE_OBSERVABILITY = "page=agent_observability&view=resourceObservability";
    private static final String URL_SEPARATOR = "?";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceType( )
    {
        return Vision.RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey( )
    {
        return PROPERTY_RESOURCE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptionKey( )
    {
        return PROPERTY_RESOURCE_DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIcon( )
    {
        return "eye";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( )
    {
        List<Vision> listVisions = VisionHome.getVisionsList( );
        return listVisions.stream( ).map( this::convertToResource ).filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformResourceItem getResource( int nIdResource, int nClientId, User user )
    {
        Vision vision = VisionHome.findByPrimaryKey( nIdResource ).orElse( null );
        if ( vision != null && AgentRBACService.canViewVision( vision, user ) )
        {
            return convertToResource( vision, user );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( int clientId, User user )
    {
        List<Vision> listVisions = VisionHome.getVisionsListByClientId( clientId );
        return listVisions.stream( ).filter( vision -> AgentRBACService.canViewVision( vision, user ) ).map( vision -> convertToResource( vision, user ) )
                .filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * Converts a Vision object to a PlatformResourceItem
     *
     * @param vision
     *            the vision to convert
     * @return the converted PlatformResourceItem or null if vision is null
     */
    private PlatformResourceItem convertToResource( Vision vision )
    {
        if ( vision == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( vision.getId( ) );
        resource.setName( vision.getVisionTitle( ) );
        resource.setDescription( vision.getVisionDescription( ) );
        resource.setType( Vision.RESOURCE_TYPE );
        resource.setClientId( vision.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_VISION + vision.getId( ) );

        return resource;
    }

    /**
     * Converts a Vision object to a PlatformResourceItem with RBAC permissions
     *
     * @param vision
     *            the vision to convert
     * @param user
     *            the current user for RBAC permissions
     * @return the converted PlatformResourceItem with permissions or null if vision is null
     */
    private PlatformResourceItem convertToResource( Vision vision, User user )
    {
        if ( vision == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( vision.getId( ) );
        resource.setName( vision.getVisionTitle( ) );
        resource.setDescription( vision.getVisionDescription( ) );
        resource.setType( Vision.RESOURCE_TYPE );
        resource.setClientId( vision.getClientId( ) );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_VISION + vision.getId( ) );
        resource.setSubscribable( true );

        resource.setUserCanView( AgentRBACService.canViewVision( vision, user ) );
        resource.setUserCanModify( AgentRBACService.canModifyVision( vision, user ) );
        resource.setUserCanDelete( AgentRBACService.canDeleteVision( vision, user ) );

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreateUrl( String idClient )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_CREATE_VISION_WITH_CLIENT + idClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceConfigItem> getConfigList( User user )
    {
        return new ArrayList<>( );
    }

    @Override
    public String getObservabilityListUrl( int nClientId, String strResourceId )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_RESOURCE_OBSERVABILITY + "&client_id=" + nClientId + "&resource_type=" + Vision.RESOURCE_TYPE
                + "&resource_id=" + strResourceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSkinExecutionDetailTemplatePath( )
    {
        return "/skin/plugins/platform/observability/vision_execution_details.html";
    }
}
