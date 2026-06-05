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

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.service.rbac.RBACService;
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;

public final class AgentRBACService
{
    private static final String WILDCARD_RESOURCE_ID = RBAC.WILDCARD_RESOURCES_ID;

    /**
     * Private constructor
     */
    private AgentRBACService( )
    {
    }

    /**
     * Checks if the user has permission to create a bot.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreateBot( User user )
    {
        return RBACService.isAuthorized( Bot.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, Bot.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to modify a bot.
     *
     * @param bot
     *            the bot to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyBot( Bot bot, User user )
    {
        return RBACService.isAuthorized( bot, Bot.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a bot.
     *
     * @param bot
     *            the bot to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeleteBot( Bot bot, User user )
    {
        return RBACService.isAuthorized( bot, Bot.PERMISSION_DELETE, user );
    }

    /**
     * Checks if the user has permission to view a bot.
     *
     * @param bot
     *            the bot to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewBot( Bot bot, User user )
    {
        return RBACService.isAuthorized( bot, Bot.PERMISSION_VIEW, user );
    }

    /**
     * Checks if the user has permission to create a dataset.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreateDataset( User user )
    {
        return RBACService.isAuthorized( Dataset.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, Dataset.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to modify a dataset.
     *
     * @param dataset
     *            the dataset to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyDataset( Dataset dataset, User user )
    {
        return RBACService.isAuthorized( dataset, Dataset.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a dataset.
     *
     * @param dataset
     *            the dataset to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeleteDataset( Dataset dataset, User user )
    {
        return RBACService.isAuthorized( dataset, Dataset.PERMISSION_DELETE, user );
    }

    /**
     * Checks if the user has permission to view a dataset.
     *
     * @param dataset
     *            the dataset to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewDataset( Dataset dataset, User user )
    {
        return RBACService.isAuthorized( dataset, Dataset.PERMISSION_VIEW, user );
    }

    /**
     * Checks if the user has permission to create a pipeline.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreatePipeline( User user )
    {
        return RBACService.isAuthorized( Pipeline.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, Pipeline.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to modify a pipeline.
     *
     * @param pipeline
     *            the pipeline to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyPipeline( Pipeline pipeline, User user )
    {
        return RBACService.isAuthorized( pipeline, Pipeline.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a pipeline.
     *
     * @param pipeline
     *            the pipeline to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeletePipeline( Pipeline pipeline, User user )
    {
        return RBACService.isAuthorized( pipeline, Pipeline.PERMISSION_DELETE, user );
    }

    /**
     * Checks if the user has permission to view a pipeline.
     *
     * @param pipeline
     *            the pipeline to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewPipeline( Pipeline pipeline, User user )
    {
        return RBACService.isAuthorized( pipeline, Pipeline.PERMISSION_VIEW, user );
    }

    /**
     * Checks if the user has permission to create a vision.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreateVision( User user )
    {
        return RBACService.isAuthorized( Vision.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, Vision.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to modify a vision.
     *
     * @param vision
     *            the vision to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyVision( Vision vision, User user )
    {
        return RBACService.isAuthorized( vision, Vision.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a vision.
     *
     * @param vision
     *            the vision to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeleteVision( Vision vision, User user )
    {
        return RBACService.isAuthorized( vision, Vision.PERMISSION_DELETE, user );
    }

    /**
     * Checks if the user has permission to view a vision.
     *
     * @param vision
     *            the vision to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewVision( Vision vision, User user )
    {
        return RBACService.isAuthorized( vision, Vision.PERMISSION_VIEW, user );
    }

    /**
     * Checks if the user has permission to create a model.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreateModel( User user )
    {
        return RBACService.isAuthorized( Model.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, Model.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to modify a model.
     *
     * @param model
     *            the model to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyModel( Model model, User user )
    {
        return RBACService.isAuthorized( model, Model.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a model.
     *
     * @param model
     *            the model to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeleteModel( Model model, User user )
    {
        return RBACService.isAuthorized( model, Model.PERMISSION_DELETE, user );
    }

    /**
     * Checks if the user has permission to view a model.
     *
     * @param model
     *            the model to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewModel( Model model, User user )
    {
        return RBACService.isAuthorized( model, Model.PERMISSION_VIEW, user );
    }

    /**
     * Enriches any agent resource with RBAC permissions for the current user.
     *
     * @param resource
     *            The resource to enrich
     * @param user
     *            The current user
     */
    public static void enrichWithPermissions( AgentPermissionResource resource, User user )
    {
        resource.setUserCanView( RBACService.isAuthorized( resource, AgentPermissionResource.PERMISSION_VIEW, user ) );
        resource.setUserCanModify( RBACService.isAuthorized( resource, AgentPermissionResource.PERMISSION_MODIFY, user ) );
        resource.setUserCanDelete( RBACService.isAuthorized( resource, AgentPermissionResource.PERMISSION_DELETE, user ) );
    }

    /**
     * Checks if the user has permission to create a decision tree.
     *
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canCreateDecisionTree( User user )
    {
        return RBACService.isAuthorized( DecisionTree.RESOURCE_TYPE, WILDCARD_RESOURCE_ID, DecisionTree.PERMISSION_CREATE, user );
    }

    /**
     * Checks if the user has permission to view a decision tree.
     *
     * @param tree
     *            the decision tree to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canViewDecisionTree( DecisionTree tree, User user )
    {
        return RBACService.isAuthorized( tree, DecisionTree.PERMISSION_VIEW, user );
    }

    /**
     * Checks if the user has permission to modify a decision tree.
     *
     * @param tree
     *            the decision tree to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canModifyDecisionTree( DecisionTree tree, User user )
    {
        return RBACService.isAuthorized( tree, DecisionTree.PERMISSION_MODIFY, user );
    }

    /**
     * Checks if the user has permission to delete a decision tree.
     *
     * @param tree
     *            the decision tree to check
     * @param user
     *            the current user
     * @return true if authorized
     */
    public static boolean canDeleteDecisionTree( DecisionTree tree, User user )
    {
        return RBACService.isAuthorized( tree, DecisionTree.PERMISSION_DELETE, user );
    }

}
