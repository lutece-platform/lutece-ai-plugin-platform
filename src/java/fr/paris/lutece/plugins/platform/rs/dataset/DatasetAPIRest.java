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

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetResponseDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetUploadRequestDTO;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.service.authentication.AuthenticationService;
import fr.paris.lutece.plugins.platform.service.dataset.DatasetService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

/**
 * REST API for Dataset operations. Thin adapter that delegates to DatasetService and maps service exceptions to HTTP responses.
 */
@ApplicationScoped
@Path( "platform/agent/api/datasets" )
public class DatasetAPIRest
{
    private static final String DATASET_PATH = "/{dataset_id}";
    private static final String DOCUMENTS_PATH = "/{dataset_id}/documents";
    private static final String DOCUMENT_PATH = "/{dataset_id}/documents/{document_id}";
    private static final String DOCUMENT_UPLOAD_PATH = "/{dataset_id}/documents/upload";
    private static final String FOLDERS_PATH = "/{dataset_id}/folders";

    private static final String INVALID_DATASET_ID_MESSAGE = "Invalid dataset identifier.";
    private static final String INVALID_DOCUMENT_ID_MESSAGE = "Invalid document identifier.";
    private static final String DOCUMENT_DELETED_MESSAGE = "Document deleted successfully.";
    private static final String UPLOAD_SUCCESS_MESSAGE_SUFFIX = " document(s) processed successfully.";

    @Context
    private HttpServletRequest _request;

    @Inject
    private AuthenticationService _authenticationService;

    /**
     * Returns every dataset owned by the authenticated client.
     *
     * @return The list of datasets wrapped in a JSON response
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAllDatasets( )
    {
        Client authClient = _authenticationService.authenticateClient( _request, null, null );
        return success( DatasetService.listDatasetsForClient( authClient.getId( ) ) );
    }

    /**
     * Returns a single dataset by identifier.
     *
     * @param datasetId
     *            The dataset identifier (must be strictly positive)
     * @return The dataset wrapped in a JSON response
     */
    @GET
    @Path( DATASET_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getDataset( @PathParam( "dataset_id" ) int datasetId )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        authenticateForDataset( datasetId );
        return success( DatasetService.getDataset( datasetId ) );
    }

    /**
     * Returns all documents of a dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The list of documents wrapped in a JSON response
     */
    @GET
    @Path( DOCUMENTS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getDocuments( @PathParam( "dataset_id" ) int datasetId )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        authenticateForDataset( datasetId );
        return success( DatasetService.listDocuments( datasetId ) );
    }

    /**
     * Lists all folders of a dataset. Returns every folder (root and nested) so clients can rebuild the tree.
     *
     * @param datasetId
     *            The dataset identifier
     * @return The list of folders wrapped in a JSON response
     */
    @GET
    @Path( FOLDERS_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getFolders( @PathParam( "dataset_id" ) int datasetId )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        authenticateForDataset( datasetId );
        return success( DatasetService.listFolders( datasetId ) );
    }

    /**
     * Creates a folder inside a dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @param payload
     *            The folder payload (name required, parent_folder_id + description optional)
     * @return The created or pre-existing folder wrapped in a JSON response
     */
    @POST
    @Path( FOLDERS_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response createFolder( @PathParam( "dataset_id" ) int datasetId, @NotNull @Valid DatasetFolderDTO payload )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        authenticateForDataset( datasetId );
        return success( DatasetService.createFolder( datasetId, payload ) );
    }

    /**
     * Uploads one or more documents to a dataset using JSON with base64 encoded files.
     *
     * @param datasetId
     *            The dataset identifier
     * @param request
     *            The upload request
     * @return The upload summary wrapped in a JSON response
     */
    @POST
    @Path( DOCUMENT_UPLOAD_PATH )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Response uploadDocument( @PathParam( "dataset_id" ) int datasetId, @NotNull @Valid DatasetUploadRequestDTO request )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        authenticateForDataset( datasetId );
        List<Integer> createdIds = DatasetService.uploadDocuments( datasetId, request );
        DatasetResponseDTO dto = new DatasetResponseDTO( );
        dto.setDatasetId( datasetId );
        dto.setDocumentIds( createdIds );
        dto.setDocumentCount( createdIds.size( ) );
        dto.setMessage( createdIds.size( ) + UPLOAD_SUCCESS_MESSAGE_SUFFIX );
        return success( dto );
    }

    /**
     * Deletes a document from a dataset.
     *
     * @param datasetId
     *            The dataset identifier
     * @param documentId
     *            The document identifier
     * @return A confirmation message wrapped in a JSON response
     */
    @DELETE
    @Path( DOCUMENT_PATH )
    @Produces( MediaType.APPLICATION_JSON )
    public Response deleteDocument( @PathParam( "dataset_id" ) int datasetId, @PathParam( "document_id" ) int documentId )
    {
        Response invalid = validateDatasetId( datasetId );
        if ( invalid != null )
        {
            return invalid;
        }
        if ( documentId <= 0 )
        {
            return error( Response.Status.BAD_REQUEST, INVALID_DOCUMENT_ID_MESSAGE );
        }
        authenticateForDataset( datasetId );
        DatasetService.deleteDocument( datasetId, documentId );
        return success( DOCUMENT_DELETED_MESSAGE );
    }

    /**
     * Validates that a dataset identifier is strictly positive.
     *
     * @param datasetId
     *            The identifier to check
     * @return A 400 response when invalid, null when valid
     */
    private Response validateDatasetId( int datasetId )
    {
        return datasetId <= 0 ? error( Response.Status.BAD_REQUEST, INVALID_DATASET_ID_MESSAGE ) : null;
    }

    /**
     * Authenticates the current request against a dataset resource.
     *
     * @param datasetId
     *            The dataset identifier used for RBAC
     * @return The authentication result
     */
    private Client authenticateForDataset( int datasetId )
    {
        return _authenticationService.authenticateClient( _request, Dataset.RESOURCE_TYPE, String.valueOf( datasetId ) );
    }

    /**
     * Builds a 200 OK JSON response.
     *
     * @param data
     *            The payload
     * @return The HTTP response
     */
    private Response success( Object data )
    {
        return Response.status( Response.Status.OK ).entity( JsonUtil.buildJsonResponse( new JsonResponse( data ) ) ).build( );
    }

    /**
     * Builds a JSON error response.
     *
     * @param status
     *            The HTTP status
     * @param message
     *            The message shown to the caller
     * @return The HTTP response
     */
    private Response error( Response.Status status, String message )
    {
        return Response.status( status ).entity( JsonUtil.buildJsonResponse( new ErrorJsonResponse( status.name( ), message ) ) ).build( );
    }
}
