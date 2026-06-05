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
package fr.paris.lutece.plugins.platform.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import fr.paris.lutece.portal.business.rbac.RBAC;
import fr.paris.lutece.portal.business.rbac.RBACHome;
import fr.paris.lutece.portal.business.rbac.RBACRole;
import fr.paris.lutece.portal.business.rbac.RBACRoleHome;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.business.user.AdminUserHome;
import fr.paris.lutece.portal.service.rbac.RBACResource;

/**
 * Test-only RBAC seeding: grants a user the three owner permissions (VIEW/MODIFY/DELETE) on one resource instance through a per-user role, and cleans the core
 * RBAC tables afterwards (they are not covered by the platform_* wipe of {@link AbstractPlatformDbTest}). This logic used to live in a production service that
 * no production code ever called; it only survives here because the RBAC tests need a way to create typed per-instance grants.
 */
public final class RbacTestSupport
{
    private static final String ROLE_PREFIX = "ROLE_LUTECE_AI_USER_";
    private static final String [ ] OWNER_PERMISSIONS = {
            "VIEW", "MODIFY", "DELETE"
    };

    /**
     * Private constructor — utility class.
     */
    private RbacTestSupport( )
    {
    }

    /**
     * Grants the owner permissions on a resource to a user, through a per-user RBAC role created and assigned on the fly.
     *
     * @param user
     *            the admin user receiving the permissions
     * @param resource
     *            the resource instance
     */
    public static void grantOwnerPermissions( AdminUser user, RBACResource resource )
    {
        String roleKey = ROLE_PREFIX + user.getUserId( );
        if ( RBACRoleHome.findByPrimaryKey( roleKey ) == null )
        {
            RBACRole role = new RBACRole( );
            role.setKey( roleKey );
            role.setDescription( "Test permissions for user " + user.getAccessCode( ) );
            RBACRoleHome.create( role );
        }
        ensureUserHasRole( user, roleKey );
        for ( String permission : OWNER_PERMISSIONS )
        {
            RBAC rbac = new RBAC( );
            rbac.setRoleKey( roleKey );
            rbac.setResourceTypeKey( resource.getResourceTypeCode( ) );
            rbac.setResourceId( resource.getResourceId( ) );
            rbac.setPermissionKey( permission );
            RBACHome.create( rbac );
        }
    }

    /**
     * Removes every RBAC rule targeting a resource and deletes the test roles left empty, so a test leaves the core RBAC tables as it found them.
     *
     * @param resource
     *            the resource instance to clean up
     */
    public static void revokeAllPermissionsOnResource( RBACResource resource )
    {
        Set<String> affectedRoles = new HashSet<>( );
        for ( RBAC rbac : RBACHome.findAll( ) )
        {
            if ( rbac.getResourceTypeKey( ).equals( resource.getResourceTypeCode( ) ) && rbac.getResourceId( ).equals( resource.getResourceId( ) ) )
            {
                if ( rbac.getRoleKey( ).startsWith( ROLE_PREFIX ) )
                {
                    affectedRoles.add( rbac.getRoleKey( ) );
                }
                RBACHome.remove( rbac.getRBACId( ) );
            }
        }
        for ( String roleKey : affectedRoles )
        {
            Collection<RBAC> remaining = RBACHome.findResourcesByCode( roleKey );
            if ( remaining.isEmpty( ) )
            {
                RBACRoleHome.remove( roleKey );
            }
        }
    }

    /**
     * Assigns the role to the user when it is not already held.
     *
     * @param user
     *            the admin user
     * @param roleKey
     *            the role key to assign
     */
    @SuppressWarnings( "deprecation" )
    private static void ensureUserHasRole( AdminUser user, String roleKey )
    {
        boolean hasRole = user.getRoles( ).values( ).stream( ).anyMatch( role -> role.getKey( ).equals( roleKey ) );
        if ( !hasRole )
        {
            AdminUserHome.createRoleForUser( user.getUserId( ), roleKey );
            user.setRoles( AdminUserHome.getRolesListForUser( user.getUserId( ) ) );
        }
    }
}
