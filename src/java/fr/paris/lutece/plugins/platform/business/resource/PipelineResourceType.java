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

import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.api.user.User;

@ApplicationScoped
@Named( "platform.pipelineResourceType" )
public class PipelineResourceType implements IPlatformResourceType
{
    public static final String PROPERTY_RESOURCE_NAME = "platform.agent.pipeline.resource.name";
    public static final String PROPERTY_RESOURCE_DESCRIPTION = "platform.agent.pipeline.resource.description";

    private static final String JSP_FO_BASE_URL = "jsp/site/Portal.jsp";
    private static final String VIEW_VIEW_PIPELINE = "page=agent_pipeline&view=viewPipeline&id_pipeline=";
    private static final String VIEW_CREATE_PIPELINE_WITH_CLIENT = "page=agent_pipeline&view=createPipeline&client_id=";
    private static final String VIEW_RESOURCE_OBSERVABILITY = "page=agent_observability&view=resourceObservability";
    private static final String URL_SEPARATOR = "?";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceType( )
    {
        return Pipeline.RESOURCE_TYPE;
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
        return "topology-ring-3";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( )
    {
        List<Pipeline> listPipelines = PipelineHome.findAll( );
        return listPipelines.stream( ).map( pipeline -> convertToResourceSimple( pipeline ) ).filter( resource -> resource != null ).collect( ArrayList::new,
                List::add, List::addAll );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformResourceItem getResource( int nIdResource, int nClientId, User user )
    {
        Pipeline pipeline = PipelineHome.findByPrimaryKey( nIdResource ).orElse( null );
        if ( pipeline != null && AgentRBACService.canViewPipeline( pipeline, user ) )
        {
            return convertToResource( pipeline, user );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( int clientId, User user )
    {
        List<Pipeline> listPipelines = PipelineHome.findByClientId( clientId );
        return listPipelines.stream( ).filter( pipeline -> AgentRBACService.canViewPipeline( pipeline, user ) )
                .map( pipeline -> convertToResource( pipeline, user ) ).filter( resource -> resource != null )
                .collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * Converts a Pipeline object to a PlatformResourceItem (no RBAC)
     *
     * @param pipeline
     *            the pipeline to convert
     * @return the converted PlatformResourceItem or null if pipeline is null
     */
    private PlatformResourceItem convertToResourceSimple( Pipeline pipeline )
    {
        if ( pipeline == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( pipeline.getId( ) );
        resource.setName( pipeline.getName( ) );
        resource.setDescription( pipeline.getDescription( ) );
        resource.setType( Pipeline.RESOURCE_TYPE );
        resource.setClientId( pipeline.getIdClient( ) );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_VIEW_PIPELINE + pipeline.getId( ) );

        return resource;
    }

    /**
     * Converts a Pipeline object to a PlatformResourceItem with RBAC permissions
     *
     * @param pipeline
     *            the pipeline to convert
     * @param user
     *            the current user for RBAC permissions
     * @return the converted PlatformResourceItem with permissions or null if pipeline is null
     */
    private PlatformResourceItem convertToResource( Pipeline pipeline, User user )
    {
        if ( pipeline == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( pipeline.getId( ) );
        resource.setName( pipeline.getName( ) );
        resource.setDescription( pipeline.getDescription( ) );
        resource.setType( Pipeline.RESOURCE_TYPE );
        resource.setClientId( pipeline.getIdClient( ) );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_VIEW_PIPELINE + pipeline.getId( ) );

        resource.setUserCanView( AgentRBACService.canViewPipeline( pipeline, user ) );
        resource.setUserCanModify( AgentRBACService.canModifyPipeline( pipeline, user ) );
        resource.setUserCanDelete( AgentRBACService.canDeletePipeline( pipeline, user ) );

        resource.setSubscribable( true );

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreateUrl( String idClient )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_CREATE_PIPELINE_WITH_CLIENT + idClient;
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
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_RESOURCE_OBSERVABILITY + "&client_id=" + nClientId + "&resource_type=" + Pipeline.RESOURCE_TYPE
                + "&resource_id=" + strResourceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSkinExecutionDetailTemplatePath( )
    {
        return "/skin/plugins/platform/observability/pipeline_execution_details.html";
    }
}
