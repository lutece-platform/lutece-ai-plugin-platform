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

import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.api.user.User;

@ApplicationScoped
@Named( "platform.modelResourceType" )
public class ModelResourceType implements IPlatformResourceType
{
    public static final String PROPERTY_RESOURCE_NAME = "platform.agent.model.resource.name";
    public static final String PROPERTY_RESOURCE_DESCRIPTION = "platform.agent.model.resource.description";

    private static final String JSP_FO_BASE_URL = "jsp/site/Portal.jsp";
    private static final String VIEW_MODIFY_MODEL = "page=agent_model&view=viewModel&model_id=";
    private static final String VIEW_CREATE_MODEL_WITH_CLIENT = "page=agent_model&view=createModel&client_id=";
    private static final String VIEW_RESOURCE_OBSERVABILITY = "page=agent_observability&view=resourceObservability";
    private static final String URL_SEPARATOR = "?";

    @Override
    public String getResourceType( )
    {
        return Model.RESOURCE_TYPE;
    }

    @Override
    public String getNameKey( )
    {
        return PROPERTY_RESOURCE_NAME;
    }

    @Override
    public String getDescriptionKey( )
    {
        return PROPERTY_RESOURCE_DESCRIPTION;
    }

    @Override
    public String getIcon( )
    {
        return "brain";
    }

    @Override
    public List<PlatformResourceItem> getResourceList( )
    {
        List<Model> listModels = ModelHome.getModelsList( );
        return listModels.stream( ).map( this::convertToResource ).filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    @Override
    public PlatformResourceItem getResource( int nIdResource, int nClientId, User user )
    {
        Model model = ModelHome.findByPrimaryKey( nIdResource ).orElse( null );
        if ( model != null && AgentRBACService.canViewModel( model, user ) )
        {
            return convertToResource( model, user );
        }
        return null;
    }

    @Override
    public List<PlatformResourceItem> getResourceList( int clientId, User user )
    {
        List<Model> listModels = ModelHome.getModelsListByClientId( clientId );
        return listModels.stream( ).filter( model -> AgentRBACService.canViewModel( model, user ) ).map( model -> convertToResource( model, user ) )
                .filter( resource -> resource != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * Converts a model into a platform resource item, resolving its provider for display.
     *
     * @param model
     *            the model to convert
     * @return the resource item, or null if the model or its provider is missing
     */
    private PlatformResourceItem convertToResource( Model model )
    {
        if ( model == null )
        {
            return null;
        }

        Provider provider = ProviderHome.findByPrimaryKey( model.getProviderId( ) ).orElse( null );
        if ( provider == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( model.getId( ) );
        resource.setName( provider.getProviderName( ) );
        resource.setDescription( provider.getProviderDescription( ) + " (" + provider.getProviderType( ) + ")" );
        resource.setType( Model.RESOURCE_TYPE );
        resource.setClientId( model.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_MODEL + model.getId( ) );
        resource.setIcon( null );

        return resource;
    }

    /**
     * Converts a model into a platform resource item populated with the user's view, modify and delete permissions.
     *
     * @param model
     *            the model to convert
     * @param user
     *            the user whose permissions are evaluated
     * @return the resource item, or null if the model or its provider is missing
     */
    private PlatformResourceItem convertToResource( Model model, User user )
    {
        if ( model == null )
        {
            return null;
        }

        Provider provider = ProviderHome.findByPrimaryKey( model.getProviderId( ) ).orElse( null );
        if ( provider == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( model.getId( ) );
        resource.setName( provider.getProviderName( ) );
        resource.setDescription( provider.getProviderDescription( ) + " (" + provider.getProviderType( ) + ")" );
        resource.setType( Model.RESOURCE_TYPE );
        resource.setClientId( model.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_MODEL + model.getId( ) );
        resource.setIcon( null );

        resource.setUserCanView( AgentRBACService.canViewModel( model, user ) );
        resource.setUserCanModify( AgentRBACService.canModifyModel( model, user ) );
        resource.setUserCanDelete( AgentRBACService.canDeleteModel( model, user ) );

        return resource;
    }

    @Override
    public String getCreateUrl( String idClient )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_CREATE_MODEL_WITH_CLIENT + idClient;
    }

    @Override
    public List<PlatformResourceConfigItem> getConfigList( User user )
    {
        return new ArrayList<>( );
    }

    @Override
    public String getObservabilityListUrl( int nClientId, String strResourceId )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_RESOURCE_OBSERVABILITY + "&client_id=" + nClientId + "&resource_type=" + Model.RESOURCE_TYPE
                + "&resource_id=" + strResourceId;
    }

    @Override
    public String getSkinExecutionDetailTemplatePath( )
    {
        return "/skin/plugins/platform/observability/model_execution_details.html";
    }
}
