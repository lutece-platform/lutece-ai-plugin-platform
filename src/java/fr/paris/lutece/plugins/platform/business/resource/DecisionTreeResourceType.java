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
import java.util.Collections;
import java.util.List;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeHome;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.api.user.User;

@ApplicationScoped
@Named( "platform.decisionTreeResourceType" )
public class DecisionTreeResourceType implements IPlatformResourceType
{
    public static final String PROPERTY_RESOURCE_NAME = "platform.agent.resource.decisiontree.name";
    public static final String PROPERTY_RESOURCE_DESCRIPTION = "platform.agent.resource.decisiontree.description";

    private static final String JSP_FO_BASE_URL = "jsp/site/Portal.jsp";
    private static final String URL_SEPARATOR = "?";
    private static final String VIEW_MODIFY_TREE = "page=agent_decisiontree&view=viewDecisionTree&tree_id=";
    private static final String VIEW_CREATE_TREE_WITH_CLIENT = "page=agent_decisiontree&view=createDecisionTree&client_id=";
    private static final String VIEW_DECISION_TREE_CONVERSATIONS = "page=agent_decisiontree&view=decisionTreeConversations";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceType( )
    {
        return DecisionTree.RESOURCE_TYPE;
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
        return "binary-tree";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( )
    {
        List<DecisionTree> listTrees = DecisionTreeHome.getDecisionTreesList( );
        return listTrees.stream( ).map( this::convertToResource ).filter( r -> r != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlatformResourceItem getResource( int nIdResource, int nClientId, User user )
    {
        DecisionTree tree = DecisionTreeHome.findByPrimaryKey( nIdResource ).orElse( null );
        if ( tree != null && AgentRBACService.canViewDecisionTree( tree, user ) )
        {
            return convertToResource( tree, user );
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceItem> getResourceList( int clientId, User user )
    {
        List<DecisionTree> listTrees = DecisionTreeHome.getDecisionTreesListByClientId( clientId );
        return listTrees.stream( ).filter( tree -> AgentRBACService.canViewDecisionTree( tree, user ) ).map( tree -> convertToResource( tree, user ) )
                .filter( r -> r != null ).collect( ArrayList::new, List::add, List::addAll );
    }

    /**
     * Converts a decision tree into a platform resource item
     *
     * @param tree
     *            the decision tree to convert
     * @return the platform resource item, or null if the tree is null
     */
    private PlatformResourceItem convertToResource( DecisionTree tree )
    {
        if ( tree == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( tree.getId( ) );
        resource.setName( tree.getTreeName( ) );
        resource.setDescription( tree.getTreeDescription( ) );
        resource.setType( DecisionTree.RESOURCE_TYPE );
        resource.setClientId( tree.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_TREE + tree.getId( ) );
        resource.setIcon( tree.getLogoBase64( ) );

        return resource;
    }

    /**
     * Converts a decision tree into a platform resource item with per-user permission flags
     *
     * @param tree
     *            the decision tree to convert
     * @param user
     *            the user whose view/modify/delete rights are resolved
     * @return the platform resource item, or null if the tree is null
     */
    private PlatformResourceItem convertToResource( DecisionTree tree, User user )
    {
        if ( tree == null )
        {
            return null;
        }

        PlatformResourceItem resource = new PlatformResourceItem( );
        resource.setId( tree.getId( ) );
        resource.setName( tree.getTreeName( ) );
        resource.setDescription( tree.getTreeDescription( ) );
        resource.setType( DecisionTree.RESOURCE_TYPE );
        resource.setClientId( tree.getClientId( ) );
        resource.setSubscribable( true );
        resource.setViewUrl( JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_MODIFY_TREE + tree.getId( ) );
        resource.setIcon( tree.getLogoBase64( ) );

        resource.setUserCanView( AgentRBACService.canViewDecisionTree( tree, user ) );
        resource.setUserCanModify( AgentRBACService.canModifyDecisionTree( tree, user ) );
        resource.setUserCanDelete( AgentRBACService.canDeleteDecisionTree( tree, user ) );

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCreateUrl( String idClient )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_CREATE_TREE_WITH_CLIENT + idClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PlatformResourceConfigItem> getConfigList( User user )
    {
        return Collections.emptyList( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getObservabilityListUrl( int nClientId, String strResourceId )
    {
        return JSP_FO_BASE_URL + URL_SEPARATOR + VIEW_DECISION_TREE_CONVERSATIONS + "&tree_id=" + strResourceId + "&client_id=" + nClientId;
    }
}
