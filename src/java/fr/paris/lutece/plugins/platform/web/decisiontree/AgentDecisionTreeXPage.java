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
package fr.paris.lutece.plugins.platform.web.decisiontree;

import jakarta.inject.Inject;
import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNodeHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransition;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeService;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationDetail;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeConversationsView;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeExport;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeOverview;
import fr.paris.lutece.plugins.platform.service.exception.DecisionTreeImportException;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.util.url.UrlItem;

/**
 * XPage handling DecisionTree, DecisionNode and DecisionTransition views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_decisiontree" )
@Controller( xpageName = "agent_decisiontree", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentDecisionTreeXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    private static final String XPAGE_NAME = "agent_decisiontree";
    protected static final String TEMPLATE_CREATE_DECISION_TREE = "/skin/plugins/platform/create_decision_tree.html";
    protected static final String TEMPLATE_API_DECISION_TREE = "/skin/plugins/platform/api_decision_tree.html";
    protected static final String TEMPLATE_MODIFY_DECISION_TREE = "/skin/plugins/platform/modify_decision_tree.html";
    protected static final String TEMPLATE_MODIFY_DECISION_NODE = "/skin/plugins/platform/modify_decision_node.html";
    protected static final String TEMPLATE_PREVIEW_DECISION_TREE = "/skin/plugins/platform/preview_decision_tree.html";
    protected static final String TEMPLATE_DECISION_TREE_CONVERSATIONS = "/skin/plugins/platform/decision_tree_conversations.html";
    protected static final String TEMPLATE_DECISION_TREE_CONVERSATION_DETAIL = "/skin/plugins/platform/decision_tree_conversation_detail.html";

    protected static final String PARAMETER_TREE_ID = "tree_id";
    protected static final String PARAMETER_NODE_ID = "node_id";
    protected static final String PARAMETER_TRANSITION_ID = "transition_id";
    protected static final String PARAMETER_NODE_TITLE = "node_title";
    protected static final String PARAMETER_SHOW_BACK_BUTTON = "show_back_button";
    protected static final String PARAMETER_PARENT_NODE_ID = "parent_node_id";
    protected static final String PARAMETER_TRANSITION_LABEL = "transition_label";
    protected static final String PARAMETER_CONTENT_TEXT = "content_text";
    protected static final String PARAMETER_LABEL = "label";
    protected static final String PARAMETER_TARGET_NODE_ID = "target_node_id";
    protected static final String PARAMETER_BACK_TARGET_NODE_ID = "back_target_node_id";
    protected static final String PARAMETER_LOGO_BASE64 = "logo_base64";
    protected static final String PARAMETER_TREE_NAME = "tree_name";
    protected static final String PARAMETER_TREE_DESCRIPTION = "tree_description";
    protected static final String PARAMETER_WELCOME_MESSAGE = "welcome_message";
    protected static final String PARAMETER_END_MESSAGE = "end_message";
    protected static final String PARAMETER_CONVERSATION_ID = "conversation_id";

    protected static final String MARK_DECISION_TREE = "decision_tree";
    protected static final String MARK_DECISION_NODE = "decision_node";
    protected static final String MARK_DECISION_NODES = "decision_nodes";
    protected static final String MARK_DECISION_TRANSITIONS = "decision_transitions";
    protected static final String MARK_NODE_MAP = "node_map";
    protected static final String MARK_ORPHAN_NODES = "orphan_nodes";
    protected static final String MARK_ROOT_NODES = "root_nodes";
    protected static final String MARK_ALL_NODES = "all_nodes";
    protected static final String MARK_CONVERSATIONS = "conversations";
    protected static final String MARK_CONVERSATION = "conversation";
    protected static final String MARK_CONVERSATION_STEPS = "conversation_steps";
    protected static final String MARK_AVG_STEPS = "avg_steps";
    protected static final String MARK_AVERAGE_DURATION_SECONDS = "average_duration_seconds";
    protected static final String MARK_STEP_COUNTS = "step_counts";
    protected static final String MARK_CONVERSATION_COUNT = "conversation_count";
    protected static final String MARK_LAST_ACTIVITY = "last_activity";
    protected static final String MARK_PAGINATOR = "paginator";
    private static final String PARAMETER_PAGE_INDEX = "page_index";
    private static final int ITEMS_PER_PAGE = 20;

    protected static final String VIEW_CREATE_DECISION_TREE = "createDecisionTree";
    protected static final String VIEW_MODIFY_DECISION_TREE = "modifyDecisionTree";
    protected static final String VIEW_MODIFY_DECISION_NODE = "modifyDecisionNode";
    protected static final String VIEW_PREVIEW_DECISION_TREE = "previewDecisionTree";
    protected static final String VIEW_DECISION_TREE_CONVERSATIONS = "decisionTreeConversations";
    protected static final String VIEW_DECISION_TREE_CONVERSATION_DETAIL = "decisionTreeConversationDetail";
    protected static final String VIEW_VIEW_DECISION_TREE = "viewDecisionTree";
    protected static final String VIEW_API_DECISION_TREE = "apiDecisionTree";

    protected static final String TEMPLATE_VIEW_DECISION_TREE = "/skin/plugins/platform/view_decision_tree.html";

    protected static final String ACTION_CREATE_DECISION_TREE = "doCreateDecisionTree";
    protected static final String ACTION_MODIFY_DECISION_TREE = "doModifyDecisionTree";
    protected static final String ACTION_CONFIRM_REMOVE_DECISION_TREE = "confirmRemoveDecisionTree";
    protected static final String ACTION_REMOVE_DECISION_TREE = "doRemoveDecisionTree";
    protected static final String ACTION_CREATE_DECISION_NODE = "doCreateDecisionNode";
    protected static final String ACTION_MODIFY_DECISION_NODE = "doModifyDecisionNode";
    protected static final String ACTION_CONFIRM_REMOVE_DECISION_NODE = "confirmRemoveDecisionNode";
    protected static final String ACTION_REMOVE_DECISION_NODE = "doRemoveDecisionNode";
    protected static final String ACTION_CONFIRM_REMOVE_ORPHAN_NODES = "confirmRemoveOrphanNodes";
    protected static final String ACTION_REMOVE_ORPHAN_NODES = "doRemoveOrphanNodes";
    protected static final String ACTION_CREATE_DECISION_TRANSITION = "doCreateDecisionTransition";
    protected static final String ACTION_MODIFY_DECISION_TRANSITION = "doModifyDecisionTransition";
    protected static final String ACTION_REMOVE_DECISION_TRANSITION = "doRemoveDecisionTransition";
    protected static final String ACTION_MOVE_TRANSITION_UP = "doMoveTransitionUp";
    protected static final String ACTION_MOVE_TRANSITION_DOWN = "doMoveTransitionDown";
    protected static final String ACTION_EXPORT_DECISION_TREE = "doExportDecisionTree";
    protected static final String ACTION_IMPORT_DECISION_TREE = "doImportDecisionTree";

    protected static final String MESSAGE_DECISION_TREE_NOT_FOUND = "platform.agent.message.decisionTreeNotFound";
    protected static final String MESSAGE_CONFIRM_REMOVE_DECISION_TREE = "platform.agent.message.confirmRemoveDecisionTree";
    protected static final String MESSAGE_CONFIRM_REMOVE_DECISION_NODE = "platform.agent.decision_tree.message.confirmRemoveNode";
    protected static final String MESSAGE_CONFIRM_REMOVE_ORPHAN_NODES = "platform.agent.decision_tree.message.confirmRemoveOrphanNodes";

    protected static final String INFO_DECISION_TREE_CREATED = "platform.agent.info.decisionTree.created";
    protected static final String INFO_DECISION_TREE_UPDATED = "platform.agent.info.decisionTree.updated";
    protected static final String INFO_DECISION_TREE_REMOVED = "platform.agent.info.decisionTree.removed";
    protected static final String INFO_DECISION_NODE_CREATED = "platform.agent.info.decisionNode.created";
    protected static final String INFO_DECISION_NODE_UPDATED = "platform.agent.info.decisionNode.updated";
    protected static final String INFO_DECISION_NODE_REMOVED = "platform.agent.info.decisionNode.removed";
    protected static final String INFO_ORPHAN_NODES_REMOVED = "platform.agent.info.orphanNodes.removed";
    protected static final String INFO_DECISION_TRANSITION_CREATED = "platform.agent.info.decisionTransition.created";
    protected static final String INFO_DECISION_TRANSITION_UPDATED = "platform.agent.info.decisionTransition.updated";
    protected static final String INFO_DECISION_TRANSITION_REMOVED = "platform.agent.info.decisionTransition.removed";
    protected static final String INFO_DECISION_TREE_IMPORTED = "platform.agent.info.decisionTree.imported";
    protected static final String ERROR_DECISION_TREE_IMPORT = "platform.agent.error.decisionTree.import";
    protected static final String PARAMETER_IMPORT_JSON = "import_json";
    protected static final String PARAMETER_DATE_FROM = "date_from";
    protected static final String PARAMETER_DATE_TO = "date_to";
    protected static final String MARK_NODE_VISIT_COUNTS = "node_visit_counts";
    protected static final String MARK_DATE_FROM = "date_from";
    protected static final String MARK_DATE_TO = "date_to";

    /**
     * Returns the decision tree read-only view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_VIEW_DECISION_TREE )
    public XPage viewDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanView );

        DecisionTreeOverview overview = DecisionTreeService.getTreeOverview( nTreeId );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Models model = _models;
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_DECISION_NODES, overview.getNodes( ) );
        model.put( MARK_NODE_MAP, overview.getNodeMap( ) );
        model.put( MARK_ROOT_NODES, overview.getRootNodes( ) );
        model.put( MARK_CLIENT_ID, strClientId != null ? strClientId : String.valueOf( tree.getClientId( ) ) );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_VIEW_DECISION_TREE );
        model.put( MARK_CONVERSATION_COUNT, overview.getConversationCount( ) );
        if ( overview.getLastActivity( ) != null )
        {
            model.put( MARK_LAST_ACTIVITY, overview.getLastActivity( ) );
        }
        model.put( MARK_AVG_STEPS, overview.getAverageSteps( ) );
        model.put( MARK_AVERAGE_DURATION_SECONDS, overview.getAverageDurationSeconds( ) );

        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_VIEW_DECISION_TREE, locale, model );
    }

    /**
     * Returns the decision tree API documentation view.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_API_DECISION_TREE )
    public XPage apiDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanView );

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID ) != null ? request.getParameter( PARAMETER_CLIENT_ID )
                : String.valueOf( tree.getClientId( ) );
        Models model = _models;
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_CLIENT_ID, strClientId );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_API_DECISION_TREE );
        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_API_DECISION_TREE, locale, model );
    }

    /**
     * Displays the create decision tree view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( value = VIEW_CREATE_DECISION_TREE, defaultView = true )
    public XPage createDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDecisionTree( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_CLIENT_ID, strClientId );

        return getXPage( TEMPLATE_CREATE_DECISION_TREE, locale, model );
    }

    /**
     * Handles the create decision tree action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_DECISION_TREE )
    public XPage doCreateDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDecisionTree( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        DecisionTree tree = new DecisionTree( );
        tree.setTreeName( request.getParameter( PARAMETER_TREE_NAME ) );
        tree.setTreeDescription( request.getParameter( PARAMETER_TREE_DESCRIPTION ) );
        tree.setWelcomeMessage( request.getParameter( PARAMETER_WELCOME_MESSAGE ) );
        tree.setEndMessage( request.getParameter( PARAMETER_END_MESSAGE ) );

        String strLogoBase64 = request.getParameter( PARAMETER_LOGO_BASE64 );
        if ( strLogoBase64 != null && !strLogoBase64.trim( ).isEmpty( ) )
        {
            tree.setLogoBase64( strLogoBase64 );
        }

        if ( strClientId != null && !strClientId.isEmpty( ) )
        {
            tree.setClientId( Integer.parseInt( strClientId ) );
        }

        DecisionTreeService.createTree( tree );

        addInfo( INFO_DECISION_TREE_CREATED, locale );
        return redirectToTreeModify( request, tree.getId( ) );
    }

    /**
     * Displays the modify decision tree view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_DECISION_TREE )
    public XPage modifyDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeOverview overview = DecisionTreeService.getTreeOverview( nTreeId );

        Models model = _models;
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_DECISION_NODES, overview.getNodes( ) );
        model.put( MARK_NODE_MAP, overview.getNodeMap( ) );
        model.put( MARK_ORPHAN_NODES, DecisionTreeService.getOrphanNodes( overview.getNodes( ) ) );
        model.put( MARK_ROOT_NODES, overview.getRootNodes( ) );
        model.put( MARK_CLIENT_ID,
                request.getParameter( PARAMETER_CLIENT_ID ) != null ? request.getParameter( PARAMETER_CLIENT_ID ) : String.valueOf( tree.getClientId( ) ) );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_MODIFY_DECISION_TREE );
        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_MODIFY_DECISION_TREE, locale, model );
    }

    /**
     * Handles the modify decision tree action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_DECISION_TREE )
    public XPage doModifyDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        tree.setTreeName( request.getParameter( PARAMETER_TREE_NAME ) );
        tree.setTreeDescription( request.getParameter( PARAMETER_TREE_DESCRIPTION ) );
        tree.setWelcomeMessage( request.getParameter( PARAMETER_WELCOME_MESSAGE ) );
        tree.setEndMessage( request.getParameter( PARAMETER_END_MESSAGE ) );

        String strLogoBase64 = request.getParameter( PARAMETER_LOGO_BASE64 );
        if ( strLogoBase64 != null )
        {
            tree.setLogoBase64( strLogoBase64.trim( ).isEmpty( ) ? null : strLogoBase64 );
        }

        DecisionTreeService.updateTree( tree );

        addInfo( INFO_DECISION_TREE_UPDATED, locale );
        return redirectToTreeModify( request, nTreeId );
    }

    /**
     * Handles the confirm remove decision tree action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_DECISION_TREE )
    public XPage confirmRemoveDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanDelete );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_DECISION_TREE, MESSAGE_CONFIRM_REMOVE_DECISION_TREE, PARAMETER_TREE_ID, PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove decision tree action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_DECISION_TREE )
    public XPage doRemoveDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanDelete );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        int nClientId = tree.getClientId( );
        DecisionTreeService.removeTree( nTreeId );

        addInfo( INFO_DECISION_TREE_REMOVED, locale );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Displays the modify decision node view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_MODIFY_DECISION_NODE )
    public XPage modifyDecisionNode( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nNodeId = requireIntParam( request, PARAMETER_NODE_ID );
        Optional<DecisionNode> optNode = DecisionTreeService.getNodeWithDetails( nNodeId );

        if ( optNode.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DECISION_TREE_NOT_FOUND );
        }

        DecisionNode node = optNode.get( );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( node.getTreeId( ) ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        Models model = _models;
        model.put( MARK_DECISION_NODE, node );
        model.put( MARK_DECISION_TRANSITIONS, node.getTransitions( ) );
        model.put( MARK_ALL_NODES, DecisionTreeService.getNodesByTreeId( node.getTreeId( ) ) );
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        model.put( PARAMETER_TREE_ID, String.valueOf( node.getTreeId( ) ) );

        return getXPage( TEMPLATE_MODIFY_DECISION_NODE, locale, model );
    }

    /**
     * Handles the create decision node action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_DECISION_NODE )
    public XPage doCreateDecisionNode( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionNode node = new DecisionNode( );
        node.setTreeId( nTreeId );
        node.setNodeTitle( request.getParameter( PARAMETER_NODE_TITLE ) );

        String strParentNodeId = request.getParameter( PARAMETER_PARENT_NODE_ID );
        Integer nParentNodeId = strParentNodeId != null && !strParentNodeId.isEmpty( ) ? Integer.valueOf( strParentNodeId ) : null;
        DecisionTreeService.createNodeWithParent( node, nParentNodeId, request.getParameter( PARAMETER_TRANSITION_LABEL ) );

        addInfo( INFO_DECISION_NODE_CREATED, locale );
        return redirectToTreeModify( request, nTreeId );
    }

    /**
     * Handles the modify decision node action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_DECISION_NODE )
    public XPage doModifyDecisionNode( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nNodeId = requireIntParam( request, PARAMETER_NODE_ID );
        Optional<DecisionNode> optNode = DecisionNodeHome.findByPrimaryKey( nNodeId );

        if ( optNode.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DECISION_TREE_NOT_FOUND );
        }

        DecisionNode node = optNode.get( );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( node.getTreeId( ) ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        node.setNodeTitle( request.getParameter( PARAMETER_NODE_TITLE ) );
        boolean bShowBack = "on".equals( request.getParameter( PARAMETER_SHOW_BACK_BUTTON ) );
        node.setShowBackButton( bShowBack );
        String strBackTarget = request.getParameter( PARAMETER_BACK_TARGET_NODE_ID );
        node.setBackTargetNodeId( bShowBack && strBackTarget != null && !strBackTarget.isEmpty( ) ? Integer.parseInt( strBackTarget ) : null );
        node.setContent( request.getParameter( PARAMETER_CONTENT_TEXT ) );

        DecisionTreeService.updateNode( node );

        addInfo( INFO_DECISION_NODE_UPDATED, locale );
        return redirectToTreeModify( request, tree.getId( ) );
    }

    /**
     * Handles the confirm remove decision node action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_DECISION_NODE )
    public XPage confirmRemoveDecisionNode( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_DECISION_NODE, MESSAGE_CONFIRM_REMOVE_DECISION_NODE, PARAMETER_NODE_ID, PARAMETER_TREE_ID,
                PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove decision node action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_DECISION_NODE )
    public XPage doRemoveDecisionNode( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nNodeId = requireIntParam( request, PARAMETER_NODE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByNodeId( nNodeId ), DecisionTree::isUserCanDelete );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeService.removeNode( nNodeId );

        addInfo( INFO_DECISION_NODE_REMOVED, locale );
        return redirectToTreeModify( request, tree.getId( ) );
    }

    /**
     * Handles the confirm remove orphan nodes action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_ORPHAN_NODES )
    public XPage confirmRemoveOrphanNodes( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_ORPHAN_NODES, MESSAGE_CONFIRM_REMOVE_ORPHAN_NODES, PARAMETER_TREE_ID, PARAMETER_CLIENT_ID );
    }

    /**
     * Handles the remove orphan nodes action (deletes all nodes not reachable from any root node).
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_ORPHAN_NODES )
    public XPage doRemoveOrphanNodes( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeService.removeOrphanNodes( nTreeId );

        addInfo( INFO_ORPHAN_NODES_REMOVED, locale );
        return redirectToTreeModify( request, nTreeId );
    }

    /**
     * Handles the create decision transition action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_CREATE_DECISION_TRANSITION )
    public XPage doCreateDecisionTransition( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nNodeId = requireIntParam( request, PARAMETER_NODE_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByNodeId( nNodeId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTransition transition = new DecisionTransition( );
        transition.setSourceNodeId( nNodeId );
        transition.setTargetNodeId( requireIntParam( request, PARAMETER_TARGET_NODE_ID ) );
        transition.setLabel( request.getParameter( PARAMETER_LABEL ) );
        transition.setSortOrder( DecisionTreeService.getNextTransitionSortOrder( nNodeId ) );

        DecisionTreeService.createTransition( transition );

        addInfo( INFO_DECISION_TRANSITION_CREATED, locale );
        return redirectToNodeModify( request, nNodeId );
    }

    /**
     * Handles the modify decision transition action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MODIFY_DECISION_TRANSITION )
    public XPage doModifyDecisionTransition( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTransitionId = requireIntParam( request, PARAMETER_TRANSITION_ID );
        int nNodeId = requireIntParam( request, PARAMETER_NODE_ID );
        Optional<DecisionTransition> optTransition = DecisionTreeService.findTransitionById( nTransitionId );

        if ( optTransition.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DECISION_TREE_NOT_FOUND );
        }

        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByTransitionId( nTransitionId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTransition transition = optTransition.get( );
        transition.setLabel( request.getParameter( PARAMETER_LABEL ) );
        transition.setTargetNodeId( requireIntParam( request, PARAMETER_TARGET_NODE_ID ) );

        DecisionTreeService.updateTransition( transition );
        addInfo( INFO_DECISION_TRANSITION_UPDATED, locale );

        return redirectToNodeModify( request, nNodeId );
    }

    /**
     * Handles the remove decision transition action.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_REMOVE_DECISION_TRANSITION )
    public XPage doRemoveDecisionTransition( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTransitionId = requireIntParam( request, PARAMETER_TRANSITION_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByTransitionId( nTransitionId ), DecisionTree::isUserCanDelete );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeService.removeTransition( nTransitionId );

        addInfo( INFO_DECISION_TRANSITION_REMOVED, locale );
        return redirectToNodeModify( request, requireIntParam( request, PARAMETER_NODE_ID ) );
    }

    /**
     * Handles the move transition up action (decreases sort order of the transition).
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MOVE_TRANSITION_UP )
    public XPage doMoveTransitionUp( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nTransitionId = requireIntParam( request, PARAMETER_TRANSITION_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByTransitionId( nTransitionId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeService.moveTransitionUp( nTransitionId );
        return redirectToNodeModify( request, requireIntParam( request, PARAMETER_NODE_ID ) );
    }

    /**
     * Handles the move transition down action (increases sort order of the transition).
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_MOVE_TRANSITION_DOWN )
    public XPage doMoveTransitionDown( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nTransitionId = requireIntParam( request, PARAMETER_TRANSITION_ID );
        DecisionTree tree = resolveTreeForAction( request, DecisionTreeService.findTreeByTransitionId( nTransitionId ), DecisionTree::isUserCanModify );
        if ( tree == null )
        {
            return _accessRedirect;
        }

        DecisionTreeService.moveTransitionDown( nTransitionId );
        return redirectToNodeModify( request, requireIntParam( request, PARAMETER_NODE_ID ) );
    }

    /**
     * Displays the preview decision tree view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_PREVIEW_DECISION_TREE )
    public XPage previewDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.getFullTree( nTreeId ), DecisionTree::isUserCanView );

        Models model = _models;
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_PREVIEW_DECISION_TREE );
        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_PREVIEW_DECISION_TREE, locale, model );
    }

    /**
     * Displays the decision tree conversations view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_DECISION_TREE_CONVERSATIONS )
    public XPage getDecisionTreeConversations( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanView );

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        String strDateFrom = request.getParameter( PARAMETER_DATE_FROM );
        String strDateTo = request.getParameter( PARAMETER_DATE_TO );

        DecisionTreeConversationsView conversationsView = DecisionTreeService.getConversationsView( nTreeId, strDateFrom, strDateTo );

        UrlItem url = new UrlItem( URL_PORTAL );
        url.addParameter( "page", XPAGE_NAME );
        url.addParameter( "view", VIEW_DECISION_TREE_CONVERSATIONS );
        url.addParameter( PARAMETER_TREE_ID, nTreeId );
        url.addParameter( PARAMETER_CLIENT_ID, strClientId );
        if ( strDateFrom != null && !strDateFrom.isEmpty( ) && strDateTo != null && !strDateTo.isEmpty( ) )
        {
            url.addParameter( PARAMETER_DATE_FROM, strDateFrom );
            url.addParameter( PARAMETER_DATE_TO, strDateTo );
        }

        LocalizedPaginator<DecisionTreeConversation> paginator = new LocalizedPaginator<>( conversationsView.getConversations( ), ITEMS_PER_PAGE, url.getUrl( ),
                PARAMETER_PAGE_INDEX, request.getParameter( PARAMETER_PAGE_INDEX ), locale );

        Models model = _models;
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_CONVERSATIONS, paginator.getPageItems( ) );
        model.put( MARK_PAGINATOR, paginator );
        model.put( MARK_CLIENT_ID, strClientId );
        if ( conversationsView.getLastActivity( ) != null )
        {
            model.put( MARK_LAST_ACTIVITY, conversationsView.getLastActivity( ) );
        }
        model.put( MARK_AVG_STEPS, conversationsView.getAverageSteps( ) );
        model.put( MARK_AVERAGE_DURATION_SECONDS, conversationsView.getAverageDurationSeconds( ) );
        model.put( MARK_STEP_COUNTS, conversationsView.getStepCounts( ) );
        model.put( MARK_DECISION_NODES, conversationsView.getNodes( ) );
        model.put( MARK_NODE_MAP, conversationsView.getNodeMap( ) );
        model.put( MARK_ROOT_NODES, conversationsView.getRootNodes( ) );
        model.put( MARK_NODE_VISIT_COUNTS, conversationsView.getNodeVisitCounts( ) );
        model.put( MARK_DATE_FROM, strDateFrom != null ? strDateFrom : "" );
        model.put( MARK_DATE_TO, strDateTo != null ? strDateTo : "" );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_DECISION_TREE_CONVERSATIONS );
        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_DECISION_TREE_CONVERSATIONS, locale, model );
    }

    /**
     * Displays the decision tree conversation detail view page.
     *
     * @param request
     *            The HTTP request
     * @return The XPage
     */
    @View( VIEW_DECISION_TREE_CONVERSATION_DETAIL )
    public XPage getDecisionTreeConversationDetail( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        String strConversationId = request.getParameter( PARAMETER_CONVERSATION_ID );

        Optional<DecisionTreeConversationDetail> optDetail = DecisionTreeService.getConversationDetail( strConversationId );
        if ( optDetail.isEmpty( ) )
        {
            return redirect( request, URL_PORTAL );
        }

        DecisionTreeConversationDetail detail = optDetail.get( );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.findTree( detail.getTreeId( ) ), DecisionTree::isUserCanView );

        Models model = _models;
        model.put( MARK_CONVERSATION, detail.getConversation( ) );
        model.put( MARK_CONVERSATION_STEPS, detail.getSteps( ) );
        model.put( MARK_DECISION_TREE, tree );
        model.put( MARK_NODE_MAP, detail.getNodeMap( ) );
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        model.put( MARK_HEADER_CURRENT_VIEW, VIEW_DECISION_TREE_CONVERSATIONS );
        addPublicationStatusToModel( model, DecisionTree.RESOURCE_TYPE, tree.getId( ) );

        return getXPage( TEMPLATE_DECISION_TREE_CONVERSATION_DETAIL, locale, model );
    }

    /**
     * Exports a decision tree as a JSON file download.
     *
     * @param request
     *            The HTTP request
     * @return The file download XPage
     */
    @Action( ACTION_EXPORT_DECISION_TREE )
    public XPage doExportDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nTreeId = requireIntParam( request, PARAMETER_TREE_ID );
        DecisionTree tree = resolveTreeForView( request, DecisionTreeService.findTree( nTreeId ), DecisionTree::isUserCanView );

        DecisionTreeExport export = DecisionTreeService.exportTree( tree );

        return download( export.getJson( ), export.getFileName( ), "application/json" );
    }

    /**
     * Imports a decision tree from a JSON payload submitted via a hidden form field.
     *
     * @param request
     *            The HTTP request
     * @return The next XPage
     */
    @Action( ACTION_IMPORT_DECISION_TREE )
    public XPage doImportDecisionTree( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDecisionTree( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        try
        {
            int nClientId = strClientId != null && !strClientId.isEmpty( ) ? Integer.parseInt( strClientId ) : 0;
            DecisionTree tree = DecisionTreeService.importTreeFromJson( request.getParameter( PARAMETER_IMPORT_JSON ), nClientId );

            addInfo( INFO_DECISION_TREE_IMPORTED, locale );
            return redirectToTreeModify( request, tree.getId( ) );
        }
        catch( DecisionTreeImportException e )
        {
            addError( ERROR_DECISION_TREE_IMPORT, locale );
            Map<String, String> params = new HashMap<>( );
            params.put( PARAMETER_CLIENT_ID, strClientId );
            return redirect( request, VIEW_CREATE_DECISION_TREE, params );
        }
    }

    /**
     * Holds the access-denied redirect produced by {@link #resolveTreeForAction} when the user lacks permission, so the caller can return it without
     * re-deriving it. Set on the request-scoped bean for the duration of a single handler.
     */
    private transient XPage _accessRedirect;

    /**
     * Resolves an authorized tree for a read-only view: ensures the tree exists, enriches it with the current user's permissions and verifies the supplied
     * permission. When the tree is missing or the user is unauthorized, a portal site message is set and a {@link SiteMessageException} is thrown by the
     * underlying helper, so this method never returns to the caller in that case.
     *
     * @param request
     *            the HTTP request
     * @param optTree
     *            the looked-up tree
     * @param permission
     *            the permission predicate to satisfy (e.g. {@link DecisionTree#isUserCanView})
     * @return the authorized tree
     * @throws UserNotSignedException
     *             if no user is signed in
     * @throws SiteMessageException
     *             if the tree is missing or the user is unauthorized
     */
    private DecisionTree resolveTreeForView( HttpServletRequest request, Optional<DecisionTree> optTree, Predicate<DecisionTree> permission )
            throws UserNotSignedException, SiteMessageException
    {
        if ( optTree.isEmpty( ) )
        {
            redirectToPortalWithMessage( request, MESSAGE_DECISION_TREE_NOT_FOUND );
        }
        DecisionTree tree = optTree.get( );
        AgentRBACService.enrichWithPermissions( tree, requireUser( request ) );
        if ( !permission.test( tree ) )
        {
            redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }
        return tree;
    }

    /**
     * Resolves an authorized tree for an action or edit form: ensures the tree exists (portal message + exception when missing), enriches it with the current
     * user's permissions and verifies the supplied permission. When the user lacks permission, an error is flashed and {@link #_accessRedirect} is set to the
     * resource view redirect; the method then returns null so the caller can return {@code _accessRedirect}.
     *
     * @param request
     *            the HTTP request
     * @param optTree
     *            the looked-up tree
     * @param permission
     *            the permission predicate to satisfy (e.g. {@link DecisionTree#isUserCanModify})
     * @return the authorized tree, or null when the user is unauthorized (caller returns {@link #_accessRedirect})
     * @throws UserNotSignedException
     *             if no user is signed in
     * @throws SiteMessageException
     *             if the tree is missing
     */
    private DecisionTree resolveTreeForAction( HttpServletRequest request, Optional<DecisionTree> optTree, Predicate<DecisionTree> permission )
            throws UserNotSignedException, SiteMessageException
    {
        _accessRedirect = null;
        if ( optTree.isEmpty( ) )
        {
            return resolveMissing( request );
        }
        DecisionTree tree = optTree.get( );
        AgentRBACService.enrichWithPermissions( tree, requireUser( request ) );
        if ( !permission.test( tree ) )
        {
            addError( MESSAGE_ACCESS_DENIED, getLocale( request ) );
            _accessRedirect = redirectToResourceView( request, VIEW_VIEW_DECISION_TREE, PARAMETER_TREE_ID, tree.getId( ) );
            return null;
        }
        return tree;
    }

    /**
     * Sets the portal not-found site message and throws, never returning. Extracted so the not-found branch is shared.
     *
     * @param request
     *            the HTTP request
     * @return never returns
     * @throws UserNotSignedException
     *             if no user is signed in
     * @throws SiteMessageException
     *             always
     */
    private DecisionTree resolveMissing( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        redirectToPortalWithMessage( request, MESSAGE_DECISION_TREE_NOT_FOUND );
        return null;
    }

    /**
     * Redirects to the modify-tree view, preserving the client_id parameter.
     *
     * @param request
     *            the HTTP request
     * @param nTreeId
     *            the tree identifier
     * @return the redirect XPage
     */
    private XPage redirectToTreeModify( HttpServletRequest request, int nTreeId ) throws UserNotSignedException
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_TREE_ID, String.valueOf( nTreeId ) );
        params.put( PARAMETER_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        return redirect( request, VIEW_MODIFY_DECISION_TREE, params );
    }

    /**
     * Redirects to the modify-node view, preserving the client_id parameter.
     *
     * @param request
     *            the HTTP request
     * @param nNodeId
     *            the node identifier
     * @return the redirect XPage
     */
    private XPage redirectToNodeModify( HttpServletRequest request, int nNodeId ) throws UserNotSignedException
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_NODE_ID, String.valueOf( nNodeId ) );
        params.put( PARAMETER_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        return redirect( request, VIEW_MODIFY_DECISION_NODE, params );
    }
}
