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
package fr.paris.lutece.plugins.platform.rs.decisiontree;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeImage;
import fr.paris.lutece.plugins.platform.service.decisiontree.DecisionTreeImageService;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;

/**
 * Admin-side REST endpoint exposing decision tree images for back-office preview. The client plugin {@code plugin-decisiontreeclient} exposes the same URL path
 * on client sites, so the same markdown renders correctly in both contexts.
 */
@ApplicationScoped
@Path( "decisiontree" )
public class DecisionTreeImageRest
{
    private static final String IMAGE_PATH = "images/{contentHash}";
    private static final String CACHE_CONTROL_VALUE = "private, max-age=3600";

    @Context
    private HttpServletRequest _request;

    @Inject
    private ClientService _clientService;

    /**
     * Streams the image bytes identified by the given sha256 content hash. Requires an authenticated LuteceUser whose authorized clients include the owner of
     * at least one tree that references this image.
     *
     * @param strContentHash
     *            the sha256 content hash
     * @return the image bytes, or 401/404 otherwise
     */
    @GET
    @Path( IMAGE_PATH )
    public Response getImage( @PathParam( "contentHash" ) String strContentHash )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            return Response.status( Response.Status.UNAUTHORIZED ).build( );
        }

        DecisionTreeImage mapping = findAuthorizedMapping( strContentHash, _clientService.getAuthorizedClientIds( user ) );
        if ( mapping == null )
        {
            return Response.status( Response.Status.NOT_FOUND ).build( );
        }

        File file = DecisionTreeImageService.getFile( mapping );
        PhysicalFile physical = file == null ? null : file.getPhysicalFile( );
        if ( physical == null || physical.getValue( ) == null )
        {
            return Response.status( Response.Status.NOT_FOUND ).build( );
        }

        return Response.ok( physical.getValue( ) ).type( mapping.getMimeType( ) ).header( HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE ).build( );
    }

    /**
     * Searches for a mapping of the given content hash whose owning tree belongs to one of the authorized clients. Returns the first match, or null if the user
     * has no access to any tree referencing this image.
     *
     * @param strContentHash
     *            the content hash
     * @param authorizedClientIds
     *            the client ids the user is authorized for
     * @return the matching mapping, or null
     */
    private DecisionTreeImage findAuthorizedMapping( String strContentHash, List<Integer> authorizedClientIds )
    {
        if ( authorizedClientIds.isEmpty( ) )
        {
            return null;
        }
        for ( DecisionTreeImage image : DecisionTreeImageService.findAnyMapping( strContentHash ) )
        {
            Optional<DecisionTree> optTree = DecisionTreeHome.findByPrimaryKey( image.getTreeId( ) );
            if ( optTree.isPresent( ) && authorizedClientIds.contains( optTree.get( ).getClientId( ) ) )
            {
                return image;
            }
        }
        return null;
    }
}
