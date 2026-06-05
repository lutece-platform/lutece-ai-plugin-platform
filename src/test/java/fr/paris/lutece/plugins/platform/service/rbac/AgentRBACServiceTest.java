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
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.business.user.AdminUserHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.plugins.platform.test.RbacTestSupport;

/**
 * Per-resource security tests for {@link AgentRBACService}, the central RBAC gate used by the XPages and resource types. Expectations follow the access-control
 * contract rather than the implementation. Two properties matter and are checked for every one of the six resource types (bot, dataset, pipeline, vision,
 * model, decision tree):
 * <ul>
 * <li><b>deny by default</b>: a user holding no role is refused create, view, modify and delete on every resource (RBAC is empty in the test container, so any
 * required role is absent);</li>
 * <li><b>grant and isolation</b>: granting owner permissions on one specific instance authorises view/modify/delete on <em>that</em> instance only — never on
 * another instance of the same type, and never on a different resource type that happens to share the same numeric id. This is the adversarial part: it would
 * catch a resource type or id that leaks across the typed permission space.</li>
 * </ul>
 */
public class AgentRBACServiceTest extends AbstractPlatformDbTest
{
    /**
     * Persists a minimal admin user with a unique access code.
     *
     * @return the persisted admin user
     */
    private AdminUser createUser( )
    {
        AdminUser user = new AdminUser( );
        user.setAccessCode( "rbac-" + UUID.randomUUID( ) );
        user.setLastName( "Test" );
        user.setFirstName( "User" );
        user.setEmail( UUID.randomUUID( ) + "@example.com" );
        user.setStatus( 0 );
        user.setLocale( Locale.FRENCH );
        user.setUserLevel( 0 );
        AdminUserHome.create( user );
        return user;
    }

    /**
     * Builds an in-memory bot with the given id (RBAC reads only the type code and id).
     *
     * @param nId
     *            the resource id
     * @return the bot
     */
    private Bot bot( int nId )
    {
        Bot bot = new Bot( );
        bot.setId( nId );
        return bot;
    }

    /**
     * Builds an in-memory dataset with the given id.
     *
     * @param nId
     *            the resource id
     * @return the dataset
     */
    private Dataset dataset( int nId )
    {
        Dataset dataset = new Dataset( );
        dataset.setId( nId );
        return dataset;
    }

    /**
     * Builds an in-memory pipeline with the given id.
     *
     * @param nId
     *            the resource id
     * @return the pipeline
     */
    private Pipeline pipeline( int nId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setId( nId );
        return pipeline;
    }

    /**
     * Builds an in-memory vision with the given id.
     *
     * @param nId
     *            the resource id
     * @return the vision
     */
    private Vision vision( int nId )
    {
        Vision vision = new Vision( );
        vision.setId( nId );
        return vision;
    }

    /**
     * Builds an in-memory model with the given id.
     *
     * @param nId
     *            the resource id
     * @return the model
     */
    private Model model( int nId )
    {
        Model model = new Model( );
        model.setId( nId );
        return model;
    }

    /**
     * Builds an in-memory decision tree with the given id.
     *
     * @param nId
     *            the resource id
     * @return the decision tree
     */
    private DecisionTree tree( int nId )
    {
        DecisionTree tree = new DecisionTree( );
        tree.setId( nId );
        return tree;
    }

    /**
     * A user with no role is refused every permission (create, view, modify, delete) on every resource type.
     */
    @Test
    public void testRolelessUserDeniedOnEveryResource( )
    {
        AdminUser user = new AdminUser( );

        assertFalse( AgentRBACService.canCreateBot( user ) );
        assertFalse( AgentRBACService.canViewBot( bot( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyBot( bot( 1 ), user ) );
        assertFalse( AgentRBACService.canDeleteBot( bot( 1 ), user ) );

        assertFalse( AgentRBACService.canCreateDataset( user ) );
        assertFalse( AgentRBACService.canViewDataset( dataset( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyDataset( dataset( 1 ), user ) );
        assertFalse( AgentRBACService.canDeleteDataset( dataset( 1 ), user ) );

        assertFalse( AgentRBACService.canCreatePipeline( user ) );
        assertFalse( AgentRBACService.canViewPipeline( pipeline( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyPipeline( pipeline( 1 ), user ) );
        assertFalse( AgentRBACService.canDeletePipeline( pipeline( 1 ), user ) );

        assertFalse( AgentRBACService.canCreateVision( user ) );
        assertFalse( AgentRBACService.canViewVision( vision( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyVision( vision( 1 ), user ) );
        assertFalse( AgentRBACService.canDeleteVision( vision( 1 ), user ) );

        assertFalse( AgentRBACService.canCreateModel( user ) );
        assertFalse( AgentRBACService.canViewModel( model( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyModel( model( 1 ), user ) );
        assertFalse( AgentRBACService.canDeleteModel( model( 1 ), user ) );

        assertFalse( AgentRBACService.canCreateDecisionTree( user ) );
        assertFalse( AgentRBACService.canViewDecisionTree( tree( 1 ), user ) );
        assertFalse( AgentRBACService.canModifyDecisionTree( tree( 1 ), user ) );
        assertFalse( AgentRBACService.canDeleteDecisionTree( tree( 1 ), user ) );
    }

    /**
     * Granting owner permissions on a bot authorises view/modify/delete on that bot only, leaking neither to another bot nor to a same-id dataset.
     */
    @Test
    public void testBotOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        Bot granted = bot( 101 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewBot( granted, user ) );
        assertTrue( AgentRBACService.canModifyBot( granted, user ) );
        assertTrue( AgentRBACService.canDeleteBot( granted, user ) );

        assertFalse( AgentRBACService.canViewBot( bot( 102 ), user ) );
        assertFalse( AgentRBACService.canViewDataset( dataset( 101 ), user ) );
        assertFalse( AgentRBACService.canCreateBot( user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }

    /**
     * Granting owner permissions on a dataset authorises view/modify/delete on that dataset only, leaking neither to another dataset nor to a same-id bot.
     */
    @Test
    public void testDatasetOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        Dataset granted = dataset( 201 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewDataset( granted, user ) );
        assertTrue( AgentRBACService.canModifyDataset( granted, user ) );
        assertTrue( AgentRBACService.canDeleteDataset( granted, user ) );

        assertFalse( AgentRBACService.canViewDataset( dataset( 202 ), user ) );
        assertFalse( AgentRBACService.canViewBot( bot( 201 ), user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }

    /**
     * Granting owner permissions on a pipeline authorises view/modify/delete on that pipeline only, leaking neither to another pipeline nor to a same-id
     * vision.
     */
    @Test
    public void testPipelineOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        Pipeline granted = pipeline( 301 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewPipeline( granted, user ) );
        assertTrue( AgentRBACService.canModifyPipeline( granted, user ) );
        assertTrue( AgentRBACService.canDeletePipeline( granted, user ) );

        assertFalse( AgentRBACService.canViewPipeline( pipeline( 302 ), user ) );
        assertFalse( AgentRBACService.canViewVision( vision( 301 ), user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }

    /**
     * Granting owner permissions on a vision authorises view/modify/delete on that vision only, leaking neither to another vision nor to a same-id model.
     */
    @Test
    public void testVisionOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        Vision granted = vision( 401 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewVision( granted, user ) );
        assertTrue( AgentRBACService.canModifyVision( granted, user ) );
        assertTrue( AgentRBACService.canDeleteVision( granted, user ) );

        assertFalse( AgentRBACService.canViewVision( vision( 402 ), user ) );
        assertFalse( AgentRBACService.canViewModel( model( 401 ), user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }

    /**
     * Granting owner permissions on a model authorises view/modify/delete on that model only, leaking neither to another model nor to a same-id pipeline.
     */
    @Test
    public void testModelOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        Model granted = model( 501 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewModel( granted, user ) );
        assertTrue( AgentRBACService.canModifyModel( granted, user ) );
        assertTrue( AgentRBACService.canDeleteModel( granted, user ) );

        assertFalse( AgentRBACService.canViewModel( model( 502 ), user ) );
        assertFalse( AgentRBACService.canViewPipeline( pipeline( 501 ), user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }

    /**
     * Granting owner permissions on a decision tree authorises view/modify/delete on that tree only, leaking neither to another tree nor to a same-id bot.
     */
    @Test
    public void testDecisionTreeOwnerGrantAndIsolation( )
    {
        AdminUser user = createUser( );
        DecisionTree granted = tree( 601 );
        RbacTestSupport.grantOwnerPermissions( user, granted );

        assertTrue( AgentRBACService.canViewDecisionTree( granted, user ) );
        assertTrue( AgentRBACService.canModifyDecisionTree( granted, user ) );
        assertTrue( AgentRBACService.canDeleteDecisionTree( granted, user ) );

        assertFalse( AgentRBACService.canViewDecisionTree( tree( 602 ), user ) );
        assertFalse( AgentRBACService.canViewBot( bot( 601 ), user ) );

        RbacTestSupport.revokeAllPermissionsOnResource( granted );
        AdminUserHome.remove( user.getUserId( ) );
    }
}
