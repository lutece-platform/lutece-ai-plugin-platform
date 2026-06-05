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
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.service.vision.VisionService;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import fr.paris.lutece.plugins.platform.rs.util.RequestBodyReader;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import java.io.InputStream;

/**
 * REST API service for vision processing operations. Provides client-authenticated endpoints for image processing through vision services.
 */
@ApplicationScoped
@Path( "platform/agent/api/vision" )
public class VisionAPIRest
{

    private static final String PROPERTY_MAX_IMAGE_BYTES = "platform.vision.maxImageBytes";
    private static final int DEFAULT_MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String UNEXPECTED_ERROR_LOG = "Unexpected error: ";
    private static final String INVALID_VISION_ID_MESSAGE = "Invalid request: visionId is required";

    @Context
    private HttpServletRequest _request;

    @Inject
    private VisionService visionService;
    @Inject
    private AuthenticationService authenticationService;

    /**
     * Processes an image through the specified vision configuration for API clients. Authenticates the client and applies vision processing including OCR and
     * data extraction.
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
        if ( visionId <= 0 )
        {
            return Response.status( Response.Status.BAD_REQUEST )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "BAD_REQUEST", INVALID_VISION_ID_MESSAGE ) ) ).build( );
        }

        Client client = authenticationService.authenticateClient( _request, Vision.RESOURCE_TYPE, String.valueOf( visionId ) );

        try
        {
            byte [ ] imageBytes = RequestBodyReader.readBounded( imageData,
                    AppPropertiesService.getPropertyInt( PROPERTY_MAX_IMAGE_BYTES, DEFAULT_MAX_IMAGE_BYTES ) );
            VisionResponseDTO visionResponse = visionService.processImageForClient( visionId, imageBytes, client );

            if ( visionResponse.isSuccess( ) )
            {
                return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( visionResponse ) ) ).build( );
            }
            else
            {
                return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                        .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "INTERNAL_ERROR", visionResponse.getErrorMessage( ) ) ) ).build( );
            }
        }
        catch( AgentServiceException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", UNEXPECTED_ERROR_LOG, e.getMessage( ), e );
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "INTERNAL_ERROR", INTERNAL_SERVER_ERROR_MESSAGE ) ) ).build( );
        }
    }
}
