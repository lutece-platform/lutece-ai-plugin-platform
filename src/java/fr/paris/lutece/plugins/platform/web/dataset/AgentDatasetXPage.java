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
package fr.paris.lutece.plugins.platform.web.dataset;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import fr.paris.lutece.plugins.platform.web.AbstractAgentXPage;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.service.dataset.DatasetService;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DatasetViewData;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentDownload;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentSegmentsView;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderDTO;
import fr.paris.lutece.plugins.platform.service.exception.AgentServiceException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.provider.ProviderTypeConstants;
import fr.paris.lutece.plugins.platform.service.rbac.AgentRBACService;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import org.apache.commons.lang3.StringUtils;

/**
 * XPage handling Dataset, Document and Segment views and actions.
 */
@RequestScoped
@Named( "platform.xpage.agent_dataset" )
@Controller( xpageName = "agent_dataset", pageTitleI18nKey = "platform.agent.xpage.pageTitle", pagePathI18nKey = "platform.agent.xpage.pagePathLabel" )
public class AgentDatasetXPage extends AbstractAgentXPage
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    @Inject
    private ObservabilityService _observabilityService;

    protected static final String TEMPLATE_VIEW_DATASET = "/skin/plugins/platform/view_dataset.html";
    protected static final String TEMPLATE_API_DATASET = "/skin/plugins/platform/api_dataset.html";
    protected static final String TEMPLATE_CREATE_DATASET = "/skin/plugins/platform/create_dataset.html";
    protected static final String TEMPLATE_CREATE_DOCUMENT = "/skin/plugins/platform/create_document.html";
    protected static final String TEMPLATE_VIEW_SEGMENTS = "skin/plugins/platform/view_segments.html";

    protected static final String PARAMETER_USE_DOCUMENT_INTELLIGENCE = "use_document_intelligence";
    protected static final String PARAMETER_DOCUMENT_INTELLIGENCE_PROVIDER_ID = "document_intelligence_provider_id";
    protected static final String PARAMETER_CHUNK_SIZE = "chunk_size";
    protected static final String PARAMETER_CHUNK_OVERLAP = "chunk_overlap";
    protected static final String PARAMETER_DOCUMENT_FILES = "document_files";
    protected static final String PARAMETER_DOCUMENT_PATHS = "document_paths";

    protected static final String MARK_DOCUMENTS_LIST = "documents_list";
    protected static final String MARK_DOCUMENT = "document";
    protected static final String MARK_SEGMENTS_LIST = "segments_list";
    protected static final String MARK_SEGMENT_COUNT = "segment_count";
    protected static final String MARK_DOCUMENT_CHUNKS = "document_chunks";
    protected static final String MARK_DOCUMENT_FILE_SIZES = "document_file_sizes";
    protected static final String MARK_DOCUMENT_INTELLIGENCE_PROVIDERS = "document_intelligence_providers";

    protected static final String VIEW_VIEW_DATASET = "viewDataset";
    protected static final String VIEW_CREATE_DATASET = "createDataset";
    protected static final String VIEW_CREATE_DOCUMENT = "createDocument";
    protected static final String VIEW_VIEW_SEGMENTS = "viewSegments";
    protected static final String VIEW_MODIFY_DATASET = "modifyDataset";
    protected static final String VIEW_API_DATASET = "apiDataset";
    protected static final String VIEW_CREATE_FOLDER = "createFolder";
    protected static final String VIEW_MODIFY_FOLDER = "modifyFolder";

    protected static final String TEMPLATE_MODIFY_DATASET = "/skin/plugins/platform/modify_dataset.html";
    protected static final String TEMPLATE_CREATE_FOLDER = "/skin/plugins/platform/create_folder.html";
    protected static final String TEMPLATE_MODIFY_FOLDER = "/skin/plugins/platform/modify_folder.html";

    protected static final String ACTION_CREATE_DATASET = "doCreateDataset";
    protected static final String ACTION_MODIFY_DATASET = "doModifyDataset";
    protected static final String ACTION_CONFIRM_REMOVE_DATASET = "confirmRemoveDataset";
    protected static final String ACTION_REMOVE_DATASET = "doRemoveDataset";
    protected static final String ACTION_CREATE_DOCUMENT = "doCreateDocument";
    protected static final String ACTION_CONFIRM_REMOVE_DOCUMENT = "confirmRemoveDocument";
    protected static final String ACTION_REMOVE_DOCUMENT = "doRemoveDocument";
    protected static final String ACTION_REINDEX_DATASET = "doReindexDataset";
    protected static final String ACTION_DOWNLOAD_DOCUMENT = "doDownloadDocument";
    protected static final String ACTION_CREATE_FOLDER = "doCreateFolder";
    protected static final String ACTION_MODIFY_FOLDER = "doModifyFolder";
    protected static final String ACTION_CONFIRM_REMOVE_FOLDER = "confirmRemoveFolder";
    protected static final String ACTION_REMOVE_FOLDER = "doRemoveFolder";

    protected static final String PARAMETER_FOLDER_ID = "folder_id";
    protected static final String PARAMETER_PARENT_FOLDER_ID = "parent_folder_id";
    protected static final String PARAMETER_FOLDER_NAME = "folder_name";
    protected static final String PARAMETER_FOLDER_DESCRIPTION = "folder_description";
    protected static final String MARK_CURRENT_FOLDER = "current_folder";
    protected static final String MARK_SUBFOLDERS = "subfolders";
    protected static final String MARK_BREADCRUMB = "breadcrumb";
    protected static final String MARK_ALL_FOLDERS = "all_folders";
    protected static final String MARK_FOLDER = "folder";
    protected static final String MARK_PARENT_PATH = "parent_path";

    private static final String XPAGE_NAME = "agent_dataset";

    protected static final String MESSAGE_DATASET_NOT_FOUND = "platform.agent.error.dataset.not.found";
    protected static final String MESSAGE_CONFIRM_REMOVE_DATASET = "platform.agent.message.confirmRemoveDataset";
    protected static final String MESSAGE_CONFIRM_REMOVE_DOCUMENT = "platform.agent.message.confirmRemoveDocument";
    protected static final String MESSAGE_CONFIRM_REMOVE_FOLDER = "platform.agent.message.confirmRemoveFolder";
    protected static final String MESSAGE_FOLDER_NOT_FOUND = "platform.agent.error.folder.not.found";
    protected static final String INFO_FOLDER_CREATED = "platform.agent.info.folder.created";
    protected static final String INFO_FOLDER_UPDATED = "platform.agent.info.folder.updated";
    protected static final String INFO_FOLDER_REMOVED = "platform.agent.info.folder.removed";
    protected static final String MESSAGE_DOCUMENT_NOT_FOUND = "platform.agent.error.document.not.found";
    protected static final String MESSAGE_DOCUMENT_FILE_EMPTY = "platform.agent.error.document.file.empty";
    protected static final String MESSAGE_DOCUMENTS_CREATION_FAILED = "platform.agent.error.documents.creation.failed";
    protected static final String MESSAGE_DOCUMENT_CREATED = "platform.agent.info.document.created";
    protected static final String MESSAGE_DOCUMENTS_CREATED = "platform.agent.info.documents.created";
    protected static final String MESSAGE_DOCUMENT_REMOVED = "platform.agent.info.document.removed";
    protected static final String MESSAGE_DATASET_REINDEXED = "platform.agent.info.dataset.reindexed";
    protected static final String INFO_DATASET_CREATED = "platform.agent.info.dataset.created";
    protected static final String INFO_DATASET_UPDATED = "platform.agent.info.dataset.updated";
    protected static final String INFO_DATASET_REMOVED = "platform.agent.info.dataset.removed";
    protected static final String ERROR_DATASET_REINDEX_FAILED = "platform.agent.error.dataset.reindex.failed";
    protected static final String INFO_SEGMENTS_NO_INDEX = "platform.agent.info.segments.no.index";
    protected static final String MESSAGE_FOLDER_SAVE_FAILED = "platform.agent.error.folder.save.failed";

    /**
     * Returns the dataset detail view with its list of documents, chunk counts and file sizes.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( value = VIEW_VIEW_DATASET, defaultView = true )
    public XPage viewDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanView );
        if ( dataset == null )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        int nDatasetId = dataset.getId( );
        Integer currentFolderId = parseOptionalInt( request.getParameter( PARAMETER_FOLDER_ID ) );

        DatasetViewData viewData = DatasetService.getDatasetViewData( nDatasetId, currentFolderId );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_DOCUMENTS_LIST, viewData.documents( ) );
        model.put( MARK_SUBFOLDERS, viewData.subfolders( ) );
        model.put( MARK_CURRENT_FOLDER, viewData.currentFolder( ) );
        model.put( MARK_BREADCRUMB, viewData.breadcrumb( ) );
        model.put( MARK_DOCUMENT_CHUNKS, viewData.documentChunks( ) );
        model.put( MARK_DOCUMENT_FILE_SIZES, viewData.documentFileSizes( ) );
        model.put( MARK_USER, user );
        addProvidersToModel( model );

        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        model.put( MARK_CLIENT_ID, strClientId != null ? strClientId : String.valueOf( dataset.getClientId( ) ) );

        int nClientIdForSidebar = ( strClientId != null ) ? Integer.parseInt( strClientId ) : dataset.getClientId( );
        _observabilityService.getStatsForResource( nClientIdForSidebar, Dataset.RESOURCE_TYPE, String.valueOf( nDatasetId ) )
                .ifPresent( stats -> model.put( MARK_STATS, stats ) );

        addDatasetSidebarToModel( model, dataset, VIEW_VIEW_DATASET );

        return getXPage( TEMPLATE_VIEW_DATASET, locale, model );
    }

    /**
     * Returns the dataset API documentation view.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_API_DATASET )
    public XPage apiDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanView );
        if ( dataset == null )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }
        Locale locale = getLocale( request );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_CLIENT_ID, dataset.getClientId( ) );
        model.put( MARK_APP_BASE_URL, AppPathService.getBaseUrl( request ) );

        addDatasetSidebarToModel( model, dataset, VIEW_API_DATASET );

        return getXPage( TEMPLATE_API_DATASET, locale, model );
    }

    /**
     * Returns the dataset modification form.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_MODIFY_DATASET )
    public XPage modifyDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_USER, user );
        model.put( MARK_CLIENT_ID, strClientId != null ? strClientId : String.valueOf( dataset.getClientId( ) ) );
        addProvidersToModel( model );

        addDatasetSidebarToModel( model, dataset, VIEW_MODIFY_DATASET );

        return getXPage( TEMPLATE_MODIFY_DATASET, locale, model );
    }

    /**
     * Returns the dataset creation form.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_CREATE_DATASET )
    public XPage createDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDataset( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Models model = _models;
        model.put( MARK_CLIENT_ID, request.getParameter( PARAMETER_CLIENT_ID ) );
        addProvidersToModel( model );
        return getXPage( TEMPLATE_CREATE_DATASET, locale, model );
    }

    /**
     * Processes the creation of a new dataset from the submitted form.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage, redirecting to the dataset view on success
     */
    @Action( ACTION_CREATE_DATASET )
    public XPage doCreateDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDataset( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        Dataset dataset = new Dataset( );
        populate( dataset, request );

        if ( !validateBean( dataset, request.getLocale( ) ) )
        {
            return redirect( request, URL_PORTAL );
        }

        DatasetHome.create( dataset );

        addInfo( INFO_DATASET_CREATED, locale );
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_DATASET_ID, String.valueOf( dataset.getId( ) ) );
        return redirect( request, VIEW_VIEW_DATASET, params );
    }

    /**
     * Processes the modification of an existing dataset.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage, redirecting to the dataset view on success
     */
    @Action( ACTION_MODIFY_DATASET )
    public XPage doModifyDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );

        populate( dataset, request );

        if ( !validateBean( dataset, request.getLocale( ) ) )
        {
            Map<String, String> params = new HashMap<>( );
            params.put( PARAMETER_DATASET_ID, String.valueOf( nDatasetId ) );
            return redirect( request, VIEW_MODIFY_DATASET, params );
        }

        DatasetHome.update( dataset );

        addInfo( INFO_DATASET_UPDATED, locale );
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_DATASET_ID, String.valueOf( nDatasetId ) );
        return redirect( request, VIEW_VIEW_DATASET, params );
    }

    /**
     * Shows a confirmation message before removing a dataset.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_DATASET )
    public XPage confirmRemoveDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        Optional<Dataset> optDataset = DatasetHome.findByPrimaryKey( nDatasetId );

        if ( optDataset.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DATASET_NOT_FOUND );
        }
        if ( !AgentRBACService.canDeleteDataset( optDataset.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_DATASET, MESSAGE_CONFIRM_REMOVE_DATASET, PARAMETER_DATASET_ID );
    }

    /**
     * Processes the removal of a dataset and all its associated data.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage, redirecting to the dataset creation view
     */
    @Action( ACTION_REMOVE_DATASET )
    public XPage doRemoveDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanDelete );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );

        int nClientId = dataset.getClientId( );
        DatasetHome.remove( dataset.getId( ) );

        addInfo( INFO_DATASET_REMOVED, locale );
        return redirectToClientView( request, nClientId );
    }

    /**
     * Returns the document creation form for a given dataset.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_CREATE_DOCUMENT )
    public XPage createDocument( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );
        Integer currentFolderId = parseOptionalInt( request.getParameter( PARAMETER_FOLDER_ID ) );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_CLIENT_ID, strClientId != null ? strClientId : "" );
        List<Provider> documentIntelligenceProviders = ProviderHome.getProvidersByType( ProviderTypeConstants.PROVIDER_TYPE_DOCUMENT_INTELLIGENCE );
        model.put( MARK_DOCUMENT_INTELLIGENCE_PROVIDERS, documentIntelligenceProviders );
        model.put( MARK_CURRENT_FOLDER, DatasetService.getDatasetViewData( nDatasetId, currentFolderId ).currentFolder( ) );
        model.put( MARK_ALL_FOLDERS, DatasetService.listFolderOptions( nDatasetId ) );

        addDatasetSidebarToModel( model, dataset, VIEW_CREATE_DOCUMENT );

        return getXPage( TEMPLATE_CREATE_DOCUMENT, locale, model );
    }

    /**
     * Processes the upload and creation of one or more documents for a dataset.
     *
     * @param request
     *            the HTTP request (must be a multipart request)
     * @return the next XPage, redirecting to the dataset view
     */
    @Action( ACTION_CREATE_DOCUMENT )
    public XPage doCreateDocument( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        LuteceUser user = requireUser( request );

        if ( !AgentRBACService.canCreateDataset( user ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        int nDatasetId = Integer.parseInt( multipartRequest.getParameter( PARAMETER_DATASET_ID ) );
        List<MultipartItem> fileItems = multipartRequest.getFileList( PARAMETER_DOCUMENT_FILES );
        Integer targetFolderId = parseOptionalInt( multipartRequest.getParameter( PARAMETER_FOLDER_ID ) );

        if ( fileItems == null || fileItems.isEmpty( ) || fileItems.stream( ).allMatch( item -> StringUtils.isEmpty( item.getName( ) ) ) )
        {
            addError( MESSAGE_DOCUMENT_FILE_EMPTY, locale );
            return redirect( request, VIEW_CREATE_DOCUMENT, datasetFolderParams( nDatasetId, targetFolderId ) );
        }

        boolean useDocumentIntelligence = "true".equals( multipartRequest.getParameter( PARAMETER_USE_DOCUMENT_INTELLIGENCE ) );
        Integer diProviderId = parseOptionalInt( multipartRequest.getParameter( PARAMETER_DOCUMENT_INTELLIGENCE_PROVIDER_ID ) );
        int chunkSize = parseIntOrDefault( multipartRequest.getParameter( PARAMETER_CHUNK_SIZE ), DatasetService.getDefaultChunkSize( ) );
        int chunkOverlap = parseIntOrDefault( multipartRequest.getParameter( PARAMETER_CHUNK_OVERLAP ), DatasetService.getDefaultChunkOverlap( ) );
        List<String> relativePaths = DatasetService.parseRelativePaths( multipartRequest.getParameter( PARAMETER_DOCUMENT_PATHS ) );

        var result = DatasetService.ingestMultipartDocuments( nDatasetId, targetFolderId, fileItems, relativePaths, chunkSize, chunkOverlap,
                useDocumentIntelligence, diProviderId );

        if ( result.successCount( ) == 1 )
        {
            addInfo( MESSAGE_DOCUMENT_CREATED, locale );
        }
        else if ( result.successCount( ) > 1 )
        {
            addInfo( I18nService.getLocalizedString( MESSAGE_DOCUMENTS_CREATED, new Object [ ] {
                    result.successCount( )
            }, locale ) );
        }
        if ( result.failureCount( ) > 0 )
        {
            addError( I18nService.getLocalizedString( MESSAGE_DOCUMENTS_CREATION_FAILED, new Object [ ] {
                    result.failureCount( )
            }, locale ) );
        }

        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, targetFolderId ) );
    }

    /**
     * Shows a confirmation message before removing a document.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage
     * @throws SiteMessageException
     *             if a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_DOCUMENT )
    public XPage confirmRemoveDocument( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        Optional<Dataset> optDataset = DatasetHome.findByPrimaryKey( nDatasetId );

        if ( optDataset.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DATASET_NOT_FOUND );
        }
        if ( !AgentRBACService.canModifyDataset( optDataset.get( ), requireUser( request ) ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_DOCUMENT, MESSAGE_CONFIRM_REMOVE_DOCUMENT, PARAMETER_DOCUMENT_ID, PARAMETER_DATASET_ID );
    }

    /**
     * Processes the removal of a document from a dataset.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage, redirecting to the dataset view
     */
    @Action( ACTION_REMOVE_DOCUMENT )
    public XPage doRemoveDocument( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanDelete );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        int nDocumentId = Integer.parseInt( request.getParameter( PARAMETER_DOCUMENT_ID ) );

        try
        {
            DatasetService.deleteDocument( nDatasetId, nDocumentId );
            addInfo( MESSAGE_DOCUMENT_REMOVED, locale );
        }
        catch( AgentServiceException e )
        {
            addError( MESSAGE_DOCUMENT_NOT_FOUND, locale );
        }

        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, null ) );
    }

    /**
     * Triggers a full reindexing of all documents belonging to a dataset. Deletes existing embeddings and schedules new embedding jobs for each document.
     *
     * @param request
     *            the HTTP request
     * @return the next XPage, redirecting to the dataset view
     */
    @Action( ACTION_REINDEX_DATASET )
    public XPage doReindexDataset( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );

        try
        {
            DatasetService.reindexDataset( nDatasetId );
            addInfo( MESSAGE_DATASET_REINDEXED, locale );
        }
        catch( AgentServiceException e )
        {
            addError( ERROR_DATASET_REINDEX_FAILED, locale );
        }

        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, null ) );
    }

    /**
     * Returns the segments view for a specific document within a dataset.
     *
     * @param request
     *            the HTTP request
     * @return the XPage
     */
    @View( VIEW_VIEW_SEGMENTS )
    public XPage viewSegments( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Locale locale = getLocale( request );
        int nDocumentId = Integer.parseInt( request.getParameter( PARAMETER_DOCUMENT_ID ) );
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        String strClientId = request.getParameter( PARAMETER_CLIENT_ID );

        Optional<DatasetDocument> optDocument = DatasetDocumentHome.findByPrimaryKey( nDocumentId );
        if ( optDocument.isEmpty( ) )
        {
            addError( MESSAGE_DOCUMENT_NOT_FOUND, locale );
            return redirect( request, VIEW_VIEW_DATASET, datasetClientParams( nDatasetId, strClientId ) );
        }

        Dataset dataset = requireDataset( request, Dataset::isUserCanView );
        if ( dataset == null )
        {
            return redirectToPortalWithMessage( request, MESSAGE_ACCESS_DENIED );
        }

        DocumentSegmentsView segmentsView = DatasetService.getDocumentSegments( nDatasetId, nDocumentId );
        if ( !segmentsView.indexExists( ) )
        {
            addWarning( INFO_SEGMENTS_NO_INDEX, locale );
        }

        Models model = _models;
        model.put( MARK_DOCUMENT, optDocument.get( ) );
        model.put( MARK_DATASET, dataset );
        model.put( MARK_CLIENT_ID, strClientId != null ? strClientId : "" );
        model.put( MARK_SEGMENTS_LIST, segmentsView.segments( ) );
        model.put( MARK_SEGMENT_COUNT, segmentsView.segmentCount( ) );

        addDatasetSidebarToModel( model, dataset, VIEW_VIEW_SEGMENTS );

        return getXPage( TEMPLATE_VIEW_SEGMENTS, locale, model );
    }

    /**
     * Downloads a document file.
     *
     * @param request
     *            the HTTP request
     * @return the XPage with binary content
     */
    @Action( ACTION_DOWNLOAD_DOCUMENT )
    public XPage doDownloadDocument( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        int nDocumentId = Integer.parseInt( request.getParameter( PARAMETER_DOCUMENT_ID ) );

        if ( DatasetHome.findByPrimaryKey( nDatasetId ).isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DATASET_NOT_FOUND );
        }

        Optional<DocumentDownload> optDownload;
        try
        {
            optDownload = DatasetService.getDocumentDownload( nDocumentId );
        }
        catch( AgentServiceException e )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DOCUMENT_NOT_FOUND );
        }
        if ( optDownload.isEmpty( ) )
        {
            return redirectToPortalWithMessage( request, MESSAGE_DOCUMENT_NOT_FOUND );
        }
        DocumentDownload dl = optDownload.get( );
        return download( dl.content( ), dl.fileName( ), dl.contentType( ) );
    }

    /**
     * Displays the create folder form for a dataset (optionally pre-selected parent folder).
     *
     * @param request
     *            The HTTP request
     * @return The create folder XPage
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @View( VIEW_CREATE_FOLDER )
    public XPage createFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        Integer parentFolderId = parseOptionalInt( request.getParameter( PARAMETER_PARENT_FOLDER_ID ) );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_PARENT_PATH, DatasetService.folderPath( nDatasetId, parentFolderId ) );
        model.put( MARK_CURRENT_FOLDER, DatasetService.getDatasetViewData( nDatasetId, parentFolderId ).currentFolder( ) );
        model.put( MARK_ALL_FOLDERS, DatasetService.listFolderOptions( nDatasetId ) );
        addDatasetSidebarToModel( model, dataset, VIEW_VIEW_DATASET );
        return getXPage( TEMPLATE_CREATE_FOLDER, locale, model );
    }

    /**
     * Creates a new folder under the given parent in a dataset.
     *
     * @param request
     *            The HTTP request
     * @return Redirect to the viewDataset scoped at the created folder's parent
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @Action( ACTION_CREATE_FOLDER )
    public XPage doCreateFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        Integer parentFolderId = parseOptionalInt( request.getParameter( PARAMETER_PARENT_FOLDER_ID ) );

        DatasetFolderDTO payload = new DatasetFolderDTO( );
        payload.setName( request.getParameter( PARAMETER_FOLDER_NAME ) );
        payload.setParentFolderId( parentFolderId );
        payload.setDescription( request.getParameter( PARAMETER_FOLDER_DESCRIPTION ) );

        try
        {
            DatasetService.createFolder( nDatasetId, payload );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_FOLDER_SAVE_FAILED, locale );
            Map<String, String> redirectParams = new HashMap<>( );
            redirectParams.put( PARAMETER_DATASET_ID, String.valueOf( nDatasetId ) );
            if ( parentFolderId != null )
            {
                redirectParams.put( PARAMETER_PARENT_FOLDER_ID, String.valueOf( parentFolderId ) );
            }
            return redirect( request, VIEW_CREATE_FOLDER, redirectParams );
        }

        addInfo( INFO_FOLDER_CREATED, locale );
        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, parentFolderId ) );
    }

    /**
     * Displays the modify folder form.
     *
     * @param request
     *            The HTTP request
     * @return The modify folder XPage
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @View( VIEW_MODIFY_FOLDER )
    public XPage modifyFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        DatasetFolder folder = requireFolderInDataset( request, nDatasetId );

        Models model = _models;
        model.put( MARK_DATASET, dataset );
        model.put( MARK_FOLDER, folder );
        model.put( MARK_PARENT_PATH, DatasetService.folderPath( nDatasetId, folder.getParentFolderId( ) ) );
        addDatasetSidebarToModel( model, dataset, VIEW_VIEW_DATASET );
        return getXPage( TEMPLATE_MODIFY_FOLDER, locale, model );
    }

    /**
     * Updates a folder's name and description.
     *
     * @param request
     *            The HTTP request
     * @return Redirect to the dataset view scoped at the folder's parent
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @Action( ACTION_MODIFY_FOLDER )
    public XPage doModifyFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        int nFolderId = Integer.parseInt( request.getParameter( PARAMETER_FOLDER_ID ) );
        String folderName = request.getParameter( PARAMETER_FOLDER_NAME );
        String folderDescription = request.getParameter( PARAMETER_FOLDER_DESCRIPTION );

        Integer parentId;
        try
        {
            DatasetFolderDTO updated = DatasetService.updateFolder( nDatasetId, nFolderId, folderName, folderDescription );
            parentId = updated.getParentFolderId( );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_FOLDER_SAVE_FAILED, locale );
            Map<String, String> redirectParams = new HashMap<>( );
            redirectParams.put( PARAMETER_DATASET_ID, String.valueOf( nDatasetId ) );
            redirectParams.put( PARAMETER_FOLDER_ID, String.valueOf( nFolderId ) );
            return redirect( request, VIEW_MODIFY_FOLDER, redirectParams );
        }

        addInfo( INFO_FOLDER_UPDATED, locale );
        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, parentId ) );
    }

    /**
     * Confirms removal of a folder. Reparents children (subfolders and documents) to the folder's parent before deletion.
     *
     * @param request
     *            The HTTP request
     * @return The confirm remove XPage
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @Action( ACTION_CONFIRM_REMOVE_FOLDER )
    public XPage confirmRemoveFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        requireFolderInDataset( request, dataset.getId( ) );
        return confirmRemove( request, XPAGE_NAME, ACTION_REMOVE_FOLDER, MESSAGE_CONFIRM_REMOVE_FOLDER, PARAMETER_FOLDER_ID, PARAMETER_DATASET_ID );
    }

    /**
     * Removes a folder. Direct child subfolders and documents are re-parented to the removed folder's parent (no cascade delete).
     *
     * @param request
     *            The HTTP request
     * @return Redirect to the dataset view scoped at the parent folder
     * @throws SiteMessageException
     *             If a site message must be displayed
     */
    @Action( ACTION_REMOVE_FOLDER )
    public XPage doRemoveFolder( HttpServletRequest request ) throws UserNotSignedException, SiteMessageException
    {
        Dataset dataset = requireDataset( request, Dataset::isUserCanModify );
        if ( dataset == null )
        {
            return datasetAccessDenied( request );
        }
        Locale locale = getLocale( request );
        int nDatasetId = dataset.getId( );
        int nFolderId = Integer.parseInt( request.getParameter( PARAMETER_FOLDER_ID ) );

        Integer parentId;
        try
        {
            parentId = DatasetService.removeFolderReparenting( nDatasetId, nFolderId );
        }
        catch( InvalidRequestException e )
        {
            return redirectToPortalWithMessage( request, MESSAGE_FOLDER_NOT_FOUND );
        }

        addInfo( INFO_FOLDER_REMOVED, locale );
        return redirect( request, VIEW_VIEW_DATASET, datasetFolderParams( nDatasetId, parentId ) );
    }

    /**
     * Parses a string to an Integer, returning null when missing or invalid.
     *
     * @param value
     *            The string to parse
     * @return The parsed Integer, or null when blank or unparsable
     */
    private static Integer parseOptionalInt( String value )
    {
        if ( value == null || value.trim( ).isEmpty( ) )
        {
            return null;
        }
        try
        {
            return Integer.valueOf( value.trim( ) );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }

    /**
     * Loads the dataset referenced by {@code dataset_id}, enriches it with the current user's permissions and checks the supplied permission predicate.
     * Redirects to the portal with a not-found message when the dataset does not exist; returns null (without redirecting) when the permission check fails so
     * the caller decides the denied response.
     *
     * @param request
     *            The HTTP request
     * @param permission
     *            The permission predicate to satisfy (e.g. {@code Dataset::isUserCanView})
     * @return The authorized dataset, or null when the permission check fails
     * @throws SiteMessageException
     *             When the dataset does not exist
     */
    private Dataset requireDataset( HttpServletRequest request, Predicate<Dataset> permission ) throws UserNotSignedException, SiteMessageException
    {
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        Optional<Dataset> optDataset = DatasetHome.findByPrimaryKey( nDatasetId );
        if ( optDataset.isEmpty( ) )
        {
            redirectToPortalWithMessage( request, MESSAGE_DATASET_NOT_FOUND );
        }
        Dataset dataset = optDataset.get( );
        AgentRBACService.enrichWithPermissions( dataset, requireUser( request ) );
        return permission.test( dataset ) ? dataset : null;
    }

    /**
     * Builds the "access denied" redirect back to the dataset view, flashing an error message. Used when {@link #requireDataset(HttpServletRequest, Predicate)}
     * returns null.
     *
     * @param request
     *            The HTTP request
     * @return The XPage redirect to the dataset view
     */
    private XPage datasetAccessDenied( HttpServletRequest request ) throws UserNotSignedException
    {
        int nDatasetId = Integer.parseInt( request.getParameter( PARAMETER_DATASET_ID ) );
        addError( MESSAGE_ACCESS_DENIED, getLocale( request ) );
        return redirectToResourceView( request, VIEW_VIEW_DATASET, PARAMETER_DATASET_ID, nDatasetId );
    }

    /**
     * Loads the folder referenced by {@code folder_id} and ensures it belongs to the supplied dataset.
     *
     * @param request
     *            The HTTP request
     * @param nDatasetId
     *            The expected owning dataset id
     * @return The matching folder
     * @throws SiteMessageException
     *             When the folder is not found or does not belong to {@code nDatasetId}
     */
    private DatasetFolder requireFolderInDataset( HttpServletRequest request, int nDatasetId ) throws UserNotSignedException, SiteMessageException
    {
        int nFolderId = Integer.parseInt( request.getParameter( PARAMETER_FOLDER_ID ) );
        Optional<DatasetFolder> optFolder = DatasetFolderHome.findByPrimaryKey( nFolderId );
        if ( optFolder.isEmpty( ) || optFolder.get( ).getDatasetId( ) != nDatasetId )
        {
            redirectToPortalWithMessage( request, MESSAGE_FOLDER_NOT_FOUND );
        }
        return optFolder.get( );
    }

    /**
     * Builds a redirect parameter map carrying the dataset id and, when present, a folder id.
     *
     * @param datasetId
     *            The dataset id
     * @param folderId
     *            The folder id, or null to omit it
     * @return The parameter map
     */
    private static Map<String, String> datasetFolderParams( int datasetId, Integer folderId )
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_DATASET_ID, String.valueOf( datasetId ) );
        if ( folderId != null )
        {
            params.put( PARAMETER_FOLDER_ID, String.valueOf( folderId ) );
        }
        return params;
    }

    /**
     * Builds a redirect parameter map carrying the dataset id and, when present, a client id.
     *
     * @param datasetId
     *            The dataset id
     * @param clientId
     *            The client id, or null to omit it
     * @return The parameter map
     */
    private static Map<String, String> datasetClientParams( int datasetId, String clientId )
    {
        Map<String, String> params = new HashMap<>( );
        params.put( PARAMETER_DATASET_ID, String.valueOf( datasetId ) );
        if ( clientId != null )
        {
            params.put( PARAMETER_CLIENT_ID, clientId );
        }
        return params;
    }

    /**
     * Parses an integer request parameter, returning the supplied default when the value is missing or not a number.
     *
     * @param value
     *            The raw parameter value, may be null
     * @param defaultValue
     *            The fallback value
     * @return The parsed integer, or {@code defaultValue}
     */
    private static int parseIntOrDefault( String value, int defaultValue )
    {
        if ( value == null )
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt( value.trim( ) );
        }
        catch( NumberFormatException e )
        {
            return defaultValue;
        }
    }

    /**
     * Adds the sidebar data for a dataset to the model, including the current view marker and the publication status
     *
     * @param model
     *            The template model
     * @param dataset
     *            The dataset whose sidebar is built
     * @param currentView
     *            The current view identifier highlighted in the sidebar
     */
    private void addDatasetSidebarToModel( Models model, Dataset dataset, String currentView )
    {
        model.put( MARK_HEADER_CURRENT_VIEW, currentView );
        addPublicationStatusToModel( model, Dataset.RESOURCE_TYPE, dataset.getId( ) );
    }
}
