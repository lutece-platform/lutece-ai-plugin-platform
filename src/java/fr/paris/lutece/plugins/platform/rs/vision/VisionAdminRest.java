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
package fr.paris.lutece.plugins.platform.rs.vision;

import fr.paris.lutece.plugins.platform.business.vision.VisionResponseDTO;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.service.vision.VisionService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import fr.paris.lutece.plugins.platform.rs.util.RequestBodyReader;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import java.io.InputStream;
import java.util.Optional;

/**
 * REST service for vision-related administrative operations. Provides endpoints for processing images through vision services with authentication.
 */
@ApplicationScoped
@Path( "platform/agent/admin/vision" )
public class VisionAdminRest
{

    private static final String PROPERTY_MAX_IMAGE_BYTES = "platform.vision.maxImageBytes";
    private static final int DEFAULT_MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String UNEXPECTED_ERROR_LOG = "Unexpected error: ";
    private static final String VISION_NOT_FOUND_MESSAGE = "Vision not found with ID: ";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String USER_NOT_AUTHENTICATED_MESSAGE = "User not authenticated";

    @Context
    private HttpServletRequest _request;

    @Inject
    private VisionService visionService;

    /**
     * Processes an image through the specified vision Authenticates the admin user and applies vision processing including OCR and data extraction.
     *
     * @param visionId
     *            the ID of the vision configuration to use
     * @param imageData
     *            the image data as InputStream
     * @return Response containing vision processing results or error information
     */
    @POST
    @Path( VisionRestConstants.PROCESS_IMAGE_PATH )
    @Consumes( MediaType.APPLICATION_OCTET_STREAM )
    @Produces( MediaType.APPLICATION_JSON )
    public Response processImage( @PathParam( "visionId" ) int visionId, InputStream imageData )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            return Response.status( Response.Status.UNAUTHORIZED ).entity( VisionResponseDTO.error( USER_NOT_AUTHENTICATED_MESSAGE ) ).build( );
        }

        try
        {
            Optional<Vision> visionOpt = VisionHome.findByPrimaryKey( visionId );

            if ( !visionOpt.isPresent( ) )
            {
                VisionResponseDTO errorResponse = VisionResponseDTO.error( VISION_NOT_FOUND_MESSAGE + visionId );
                return Response.status( Response.Status.NOT_FOUND ).entity( errorResponse ).build( );
            }

            byte [ ] imageBytes = RequestBodyReader.readBounded( imageData,
                    AppPropertiesService.getPropertyInt( PROPERTY_MAX_IMAGE_BYTES, DEFAULT_MAX_IMAGE_BYTES ) );
            VisionResponseDTO visionResponse = visionService.processImage( visionId, imageBytes, user );

            Response.Status httpStatus = visionResponse.isSuccess( ) ? Response.Status.OK : Response.Status.INTERNAL_SERVER_ERROR;

            return Response.status( httpStatus ).entity( visionResponse ).build( );

        }
        catch( AgentServiceException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", UNEXPECTED_ERROR_LOG, e.getMessage( ), e );
            VisionResponseDTO errorResponse = VisionResponseDTO.error( INTERNAL_ERROR_CODE, INTERNAL_SERVER_ERROR_MESSAGE );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( errorResponse ).build( );
        }
    }
}
