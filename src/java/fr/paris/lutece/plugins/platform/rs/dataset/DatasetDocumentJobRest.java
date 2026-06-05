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
package fr.paris.lutece.plugins.platform.rs.dataset;

import fr.paris.lutece.plugins.platform.service.dataset.DatasetService;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentJobView;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API for Dataset Document Jobs
 */
@ApplicationScoped
@Path( "platform/agent/dataset" )
public class DatasetDocumentJobRest
{

    private static final String USER_NOT_AUTHENTICATED_MESSAGE = "User not authenticated";

    @Context
    private HttpServletRequest _request;

    /**
     * Gets all ingestion jobs for a dataset the authenticated user is allowed to view. Authorization (view permission on the dataset) and aggregation are
     * enforced by {@link DatasetService#getDatasetJobs}; typed exceptions are translated to 403/404 by the central exception mapper.
     *
     * @param datasetId
     *            The ID of the dataset
     * @return The response containing dataset jobs information
     */
    @GET
    @Path( "jobs/{dataset_id}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getDatasetJobs( @PathParam( "dataset_id" ) int datasetId )
    {
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( _request );
        if ( user == null )
        {
            return Response.status( Response.Status.UNAUTHORIZED )
                    .entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( "UNAUTHORIZED", USER_NOT_AUTHENTICATED_MESSAGE ) ) ).build( );
        }

        List<DocumentJobView> jobs = DatasetService.getDatasetJobs( datasetId, user );

        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( jobs ) ) ).build( );
    }
}
