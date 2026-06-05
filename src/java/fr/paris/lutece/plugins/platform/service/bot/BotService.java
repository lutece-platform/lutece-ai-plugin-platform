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
package fr.paris.lutece.plugins.platform.service.bot;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationDTO;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationHistoryDTO;
import fr.paris.lutece.plugins.platform.business.dataset.DocumentFileDTO;
import fr.paris.lutece.plugins.platform.business.conversation.MessageDTO;
import fr.paris.lutece.plugins.platform.business.bot.QueryRequestDTO;
import fr.paris.lutece.plugins.platform.business.bot.*;
import fr.paris.lutece.plugins.platform.business.dataset.*;
import fr.paris.lutece.plugins.platform.business.mcp.BotMcpServerHome;
import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedback;
import fr.paris.lutece.plugins.platform.business.conversation.ConversationMessageFeedbackHome;
import fr.paris.lutece.plugins.platform.business.observability.ResourceStats;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.service.bot.dto.BotDetailDTO;
import fr.paris.lutece.plugins.platform.service.bot.dto.DatasetSelectionDTO;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.service.rag.DocumentService;
import fr.paris.lutece.plugins.platform.service.exception.*;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.api.user.User;

import java.io.InputStream;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service class for managing bot operations and client access control. Provides methods for bot querying, conversation management, and document access with
 * proper authorization and subscription validation.
 */
@ApplicationScoped
@Named( "platform.botService" )
public class BotService
{

    private static final String ERROR_USER_ID_REQUIRED = "User ID is required";
    private static final String ERROR_NO_MESSAGES_FOUND = "No messages found for conversation: ";
    private static final String ERROR_DOCUMENT_NOT_FOUND = "Document not found or access denied";
    private static final String ERROR_BOT_NOT_FOUND = "Bot not found";
    private static final String ERROR_NO_SUBSCRIPTION = "No active subscription to this bot";
    private static final String ERROR_DATASET_NOT_FOUND = "Dataset not found";
    private static final String ERROR_NO_DATASET_ACCESS = "No active subscription to access this dataset";
    private static final String ERROR_UNAUTHORIZED_CONVERSATION = "Unauthorized access to conversation";
    private static final String ERROR_NO_CONVERSATION_ACCESS = "No active subscription to access this conversation";
    private static final String ERROR_INVALID_QUERY_REQUEST = "Invalid query request";
    private static final String ERROR_INVALID_CONVERSATION_UUID = "Invalid conversation UUID";
    private static final String ERROR_ACCESS_DENIED_BOT = "Access denied to bot";
    private static final String ERROR_ACCESS_DENIED_CONVERSATION = "Access denied to conversation";

    private static final String BOT_RESOURCE_TYPE = Bot.RESOURCE_TYPE;
    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private BotQueryService _botQueryService;

    @Inject
    private BotMemoryService _botMemoryService;

    @Inject
    private ClientService _clientService;

    @Inject
    private ObservabilityService _observabilityService;

    /**
     * Default constructor for CDI.
     */
    BotService( )
    {
    }

    /**
     * Persists a new bot together with its dataset and MCP server associations. The bot must already be populated and validated by the caller.
     *
     * @param bot
     *            the bot to create
     * @param datasetIds
     *            the ids of the datasets to associate (never null)
     * @param mcpServerIds
     *            the ids of the MCP servers to associate (never null)
     * @return the persisted bot, carrying its generated identifier
     */
    public Bot createBotWithAssociations( Bot bot, List<Integer> datasetIds, List<Integer> mcpServerIds )
    {
        BotHome.create( bot );
        associateDatasets( bot.getId( ), datasetIds );
        BotMcpServerHome.associateMcpServers( bot.getId( ), mcpServerIds );
        return bot;
    }

    /**
     * Updates an existing bot together with its dataset, pipeline and MCP server associations. The bot must already be populated and validated by the caller.
     * Dataset associations are fully replaced (removed then re-created).
     *
     * @param bot
     *            the bot to update
     * @param datasetIds
     *            the ids of the datasets to associate (never null)
     * @param pipelineIds
     *            the ids of the pipelines to associate (never null)
     * @param mcpServerIds
     *            the ids of the MCP servers to associate (never null)
     */
    public void updateBotWithAssociations( Bot bot, List<Integer> datasetIds, List<Integer> pipelineIds, List<Integer> mcpServerIds )
    {
        BotHome.update( bot );
        BotDatasetHome.removeByBotId( bot.getId( ) );
        associateDatasets( bot.getId( ), datasetIds );
        BotMcpServerHome.associateMcpServers( bot.getId( ), mcpServerIds );
        BotPipelineHome.associatePipelines( bot.getId( ), pipelineIds );
    }

    /**
     * Associates the supplied dataset ids with a bot.
     *
     * @param botId
     *            the bot identifier
     * @param datasetIds
     *            the ids of the datasets to associate (never null)
     */
    private void associateDatasets( int botId, List<Integer> datasetIds )
    {
        for ( int datasetId : datasetIds )
        {
            BotDatasetHome.associate( new BotDataset( botId, datasetId ) );
        }
    }

    /**
     * Aggregates the detail data of a bot for its detail view: associated datasets, attached pipelines and MCP servers, the bot's resource statistics and the
     * number of pending feedbacks.
     *
     * @param botId
     *            the bot identifier
     * @param clientId
     *            the client identifier used to resolve the bot's statistics
     * @return the aggregated bot detail
     */
    public BotDetailDTO getBotDetail( int botId, int clientId )
    {
        List<Dataset> associatedDatasets = BotDatasetHome.getDatasetIdsByBotId( botId ).stream( ).map( DatasetHome::findByPrimaryKey )
                .filter( Optional::isPresent ).map( Optional::get ).toList( );
        List<Pipeline> attachedPipelines = BotPipelineHome.getPipelineIdsByBotId( botId ).stream( ).map( PipelineHome::findByPrimaryKey )
                .filter( Optional::isPresent ).map( Optional::get ).toList( );
        List<McpServer> attachedMcpServers = BotMcpServerHome.getMcpServerIdsByBotId( botId ).stream( ).map( McpServerHome::findByPrimaryKey )
                .filter( Optional::isPresent ).map( Optional::get ).toList( );
        Optional<ResourceStats> stats = findStatsForBot( clientId, botId );
        int pendingFeedbackCount = ConversationMessageFeedbackHome.getConversationMessageFeedbacksList( botId, ConversationMessageFeedback.Status.PENDING )
                .size( );
        return new BotDetailDTO( associatedDatasets, attachedPipelines, attachedMcpServers, stats, pendingFeedbackCount );
    }

    /**
     * Finds the resource statistics matching a given bot among the statistics of its client.
     *
     * @param clientId
     *            the client identifier
     * @param botId
     *            the bot identifier
     * @return the bot's statistics, empty when none match
     */
    private Optional<ResourceStats> findStatsForBot( int clientId, int botId )
    {
        String strBotId = String.valueOf( botId );
        return _observabilityService.getStatsByResource( clientId ).stream( )
                .filter( stats -> BOT_RESOURCE_TYPE.equals( stats.getResourceType( ) ) && strBotId.equals( stats.getResourceId( ) ) ).findFirst( );
    }

    /**
     * Builds the dataset selection for a bot create/modify form: all datasets partitioned into local (owned by the given client) and external (owned by another
     * client, enriched with the owning client name), plus the ids currently selected by the bot.
     *
     * @param botId
     *            the bot identifier, 0 when creating
     * @param clientId
     *            the client identifier used to separate local from external datasets
     * @return the dataset selection
     */
    public DatasetSelectionDTO getDatasetSelectionForBot( int botId, int clientId )
    {
        List<Integer> selectedDatasetIds = ( botId > 0 ) ? BotDatasetHome.getDatasetIdsByBotId( botId ) : new ArrayList<>( );
        List<Dataset> localDatasets = new ArrayList<>( );
        List<Dataset> externalDatasets = new ArrayList<>( );

        for ( Dataset dataset : DatasetHome.getDatasetsList( ) )
        {
            if ( clientId > 0 && dataset.getClientId( ) == clientId )
            {
                localDatasets.add( dataset );
            }
            else
            {
                ClientHome.findByPrimaryKey( dataset.getClientId( ) ).ifPresent( client -> dataset.setClientName( client.getName( ) ) );
                externalDatasets.add( dataset );
            }
        }
        return new DatasetSelectionDTO( localDatasets, externalDatasets, selectedDatasetIds );
    }

    /**
     * Lists the pipelines available for a client.
     *
     * @param clientId
     *            the client identifier whose pipelines are listed
     * @return the pipelines of the client
     */
    public List<Pipeline> getPipelinesForClient( int clientId )
    {
        return PipelineHome.findByClientId( clientId );
    }

    /**
     * Returns the pipeline ids currently selected by a bot.
     *
     * @param botId
     *            the bot identifier, 0 when creating
     * @return the selected pipeline ids (never null)
     */
    public List<Integer> getSelectedPipelineIds( int botId )
    {
        return ( botId > 0 ) ? BotPipelineHome.getPipelineIdsByBotId( botId ) : new ArrayList<>( );
    }

    /**
     * Lists the MCP servers available.
     *
     * @return the available MCP servers
     */
    public List<McpServer> getAvailableMcpServers( )
    {
        return McpServerHome.getMcpServersList( );
    }

    /**
     * Returns the MCP server ids currently selected by a bot.
     *
     * @param botId
     *            the bot identifier, 0 when creating
     * @return the selected MCP server ids (never null)
     */
    public List<Integer> getSelectedMcpServerIds( int botId )
    {
        return ( botId > 0 ) ? BotMcpServerHome.getMcpServerIdsByBotId( botId ) : new ArrayList<>( );
    }

    /**
     * Retrieves all bots that the client has active subscriptions to.
     *
     * @param client
     *            the client requesting bot access
     * @return list of authorized bots with sanitized information
     */
    public List<Bot> getAuthorizedBotsForClient( Client client )
    {
        List<Bot> allBots = BotHome.getBotsList( );
        return allBots.stream( ).filter( bot -> hasSubscriptionToBot( client.getId( ), bot.getId( ) ) ).map( this::sanitizeBotForClient ).toList( );
    }

    /**
     * Checks if a client has an active subscription to a specific bot.
     *
     * @param clientId
     *            the client identifier
     * @param botId
     *            the bot identifier
     * @return true if client has active subscription, false otherwise
     */
    private boolean hasSubscriptionToBot( int clientId, int botId )
    {
        return subscriptionService.hasActiveSubscription( clientId, BOT_RESOURCE_TYPE, String.valueOf( botId ) );
    }

    /**
     * Validates a client streaming query (request shape, bot access, user id) before any SSE connection is committed. Returns the resolved bot so the boundary
     * can register the SSE sink, then call {@link #startStreamingQuery} with the same operation id.
     *
     * @param request
     *            the query request containing bot ID, query text, and conversation UUID
     * @param client
     *            the client making the request
     * @return the resolved bot
     * @throws InvalidRequestException
     *             if request is invalid or user ID is missing
     * @throws AccessDeniedException
     *             if client lacks access to the bot
     */
    public Bot validateStreamingQueryForClient( QueryRequestDTO request, Client client )
    {
        validateQueryRequest( request );
        Bot bot = validateBotAccessForClient( request.getBotId( ), client );
        String userId = request.getUserId( );
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }
        request.setUserId( userId );
        return bot;
    }

    /**
     * Starts the asynchronous generation for an already-validated streaming query, emitting events on the given operation id (the one the boundary registered
     * its SSE sink with).
     *
     * @param bot
     *            the resolved bot
     * @param request
     *            the validated query request
     * @param operationId
     *            the operation identifier the SSE sink is registered on
     */
    public void startStreamingQuery( Bot bot, QueryRequestDTO request, String operationId )
    {
        _botQueryService.processStreamingQuery( bot, request, operationId );
    }

    /**
     * Retrieves conversation history for a client with authorization validation.
     *
     * @param conversationUuid
     *            the unique conversation identifier
     * @param userId
     *            the user identifier
     * @param client
     *            the client requesting conversation history
     * @return conversation history with messages
     * @throws InvalidRequestException
     *             if conversation UUID or user ID is invalid
     * @throws ResourceNotFoundException
     *             if no messages found
     * @throws AccessDeniedException
     *             if client lacks access to the conversation
     */
    public ConversationHistoryDTO getConversationHistoryForClient( String conversationUuid, String userId, Client client )
    {
        validateConversationUuid( conversationUuid );
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }

        validateConversationAccessForClient( conversationUuid, userId, client );
        List<MessageDTO> messages = _botQueryService.getConversationHistory( conversationUuid, userId );
        if ( messages == null || messages.isEmpty( ) )
        {
            throw new ResourceNotFoundException( ERROR_NO_MESSAGES_FOUND + conversationUuid );
        }
        return new ConversationHistoryDTO( conversationUuid, messages );
    }

    /**
     * Retrieves all conversations for a user that the client has access to.
     *
     * @param userId
     *            the user identifier
     * @param client
     *            the client requesting user conversations
     * @return list of authorized conversations for the user
     * @throws InvalidRequestException
     *             if user ID is invalid
     */
    public List<ConversationDTO> getUserConversationsForClient( String userId, Client client )
    {
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }

        List<BotConversation> conversations = _botMemoryService.getConversationsByUserId( userId );
        return conversations.stream( ).filter( conversation -> isConversationAuthorizedForClient( conversation, client ) )
                .map( conversation -> createConversationDTO( conversation, userId ) ).toList( );
    }

    /**
     * Deletes a specific conversation for a client with proper authorization.
     *
     * @param conversationUuid
     *            the unique conversation identifier
     * @param userId
     *            the user identifier
     * @param client
     *            the client requesting conversation deletion
     * @throws InvalidRequestException
     *             if conversation UUID or user ID is invalid
     * @throws AccessDeniedException
     *             if client lacks access to the conversation
     */
    public void deleteConversationForClient( String conversationUuid, String userId, Client client )
    {
        validateConversationUuid( conversationUuid );
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }

        validateConversationAccessForClient( conversationUuid, userId, client );
        _botMemoryService.deleteConversation( conversationUuid, userId );
    }

    /**
     * Deletes all conversations for a user that the client has access to.
     *
     * @param userId
     *            the user identifier
     * @param client
     *            the client requesting conversation deletion
     * @throws InvalidRequestException
     *             if user ID is invalid
     */
    public void deleteAllUserConversationsForClient( String userId, Client client )
    {
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }

        List<BotConversation> conversations = _botMemoryService.getConversationsByUserId( userId );
        conversations.stream( ).filter( conversation -> isConversationAuthorizedForClient( conversation, client ) )
                .forEach( conversation -> _botMemoryService.deleteConversation( conversation.getConversationUuid( ), userId ) );
    }

    /**
     * Retrieves a document file with proper bot and dataset access validation.
     *
     * @param botId
     *            the bot identifier
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     * @param client
     *            the client requesting document access
     * @return document file with input stream and name
     * @throws AccessDeniedException
     *             if client lacks access to bot or dataset
     * @throws ResourceNotFoundException
     *             if document is not found
     */
    public DocumentFileDTO getDocumentFileForClient( int botId, int datasetId, int documentId, Client client )
    {
        validateBotAccessForClient( botId, client );
        validateDatasetAccessForClient( datasetId, client );

        InputStream inputStream = DocumentService.getDocumentFileWithValidation( botId, datasetId, documentId );
        if ( inputStream == null )
        {
            throw new ResourceNotFoundException( ERROR_DOCUMENT_NOT_FOUND );
        }

        String documentName = DocumentService.getDocumentName( documentId );
        return new DocumentFileDTO( inputStream, documentName );
    }

    /**
     * Validates bot access for a client and returns the bot if authorized.
     *
     * @param botId
     *            the bot identifier
     * @param client
     *            the client requesting access
     * @return the bot if access is granted
     * @throws AccessDeniedException
     *             if bot not found or client lacks subscription
     */
    private Bot validateBotAccessForClient( int botId, Client client )
    {
        Bot bot = BotHome.findByPrimaryKey( botId ).orElse( null );
        if ( bot == null )
        {
            throw new AccessDeniedException( ERROR_BOT_NOT_FOUND );
        }

        if ( !hasSubscriptionToBot( client.getId( ), botId ) )
        {
            throw new AccessDeniedException( ERROR_NO_SUBSCRIPTION );
        }
        return bot;
    }

    /**
     * Validates dataset access for a client through bot subscriptions.
     *
     * @param datasetId
     *            the dataset identifier
     * @param client
     *            the client requesting access
     * @return the dataset if access is granted
     * @throws AccessDeniedException
     *             if dataset not found or client lacks access
     */
    private Dataset validateDatasetAccessForClient( int datasetId, Client client )
    {
        Dataset dataset = DatasetHome.findByPrimaryKey( datasetId ).orElse( null );
        if ( dataset == null )
        {
            throw new AccessDeniedException( ERROR_DATASET_NOT_FOUND );
        }

        List<Integer> botIds = BotDatasetHome.getBotIdsByDatasetId( datasetId );
        boolean hasAccess = botIds.stream( ).anyMatch( botId -> hasSubscriptionToBot( client.getId( ), botId ) );

        if ( !hasAccess )
        {
            throw new AccessDeniedException( ERROR_NO_DATASET_ACCESS );
        }
        return dataset;
    }

    /**
     * Validates conversation access for a client.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user identifier
     * @param client
     *            the client requesting access
     * @return the conversation if access is granted
     * @throws AccessDeniedException
     *             if conversation not found or client lacks access
     */
    private BotConversation validateConversationAccessForClient( String conversationUuid, String userId, Client client )
    {
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation == null || !conversation.getUserId( ).equals( userId ) )
        {
            throw new AccessDeniedException( ERROR_UNAUTHORIZED_CONVERSATION );
        }

        if ( !hasSubscriptionToBot( client.getId( ), conversation.getBotId( ) ) )
        {
            throw new AccessDeniedException( ERROR_NO_CONVERSATION_ACCESS );
        }
        return conversation;
    }

    /**
     * Checks if a conversation is authorized for a client.
     *
     * @param conversation
     *            the bot conversation
     * @param client
     *            the client to check authorization for
     * @return true if conversation is authorized, false otherwise
     */
    private boolean isConversationAuthorizedForClient( BotConversation conversation, Client client )
    {
        return hasSubscriptionToBot( client.getId( ), conversation.getBotId( ) );
    }

    /**
     * Retrieves all bots that an admin user has access to.
     *
     * @param adminUser
     *            the admin user requesting bot access
     * @return list of authorized bots with sanitized information
     */
    public List<Bot> getAuthorizedBots( User user )
    {
        List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
        if ( authorizedClientIds.isEmpty( ) )
        {
            return new ArrayList<>( );
        }

        List<Bot> allBots = BotHome.getBotsList( );
        return allBots.stream( ).filter( bot -> authorizedClientIds.contains( bot.getClientId( ) ) ).map( this::sanitizeBotForClient ).toList( );
    }

    /**
     * Sanitizes bot information for client consumption by removing sensitive data. Mutates the given instance in place: callers must pass request-local bots
     * (fresh from the DAO, never a shared/long-lived reference).
     *
     * @param bot
     *            the bot to sanitize, mutated in place
     * @return sanitized bot with public information only
     */
    private Bot sanitizeBotForClient( Bot bot )
    {
        List<Integer> botDatasetIds = BotDatasetHome.getDatasetIdsByBotId( bot.getId( ) );
        List<Dataset> botDatasets = botDatasetIds.stream( ).map( DatasetHome::findByPrimaryKey ).filter( Optional::isPresent ).map( Optional::get ).toList( );

        bot.setClientId( 0 );
        bot.setLlmProviderId( 0 );
        bot.setEmbedProviderId( 0 );
        bot.setListBotDatasets( botDatasets );
        return bot;
    }

    /**
     * Validates an admin streaming query (request shape, bot access, user id) before any SSE connection is committed. Returns the resolved bot so the boundary
     * can register the SSE sink, then call {@link #startStreamingQuery} with the same operation id.
     *
     * @param request
     *            the query request containing bot ID, query text, and conversation UUID
     * @param user
     *            the admin user making the request
     * @return the resolved bot
     * @throws InvalidRequestException
     *             if request is invalid
     * @throws AccessDeniedException
     *             if admin user lacks access to the bot
     */
    public Bot validateStreamingQuery( QueryRequestDTO request, User user )
    {
        validateQueryRequest( request );
        Bot bot = validateBotAccess( request.getBotId( ), user );
        String userId = user.getAccessCode( );
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_USER_ID_REQUIRED );
        }
        request.setUserId( userId );
        return bot;
    }

    /**
     * Retrieves conversation history for an admin user.
     *
     * @param conversationUuid
     *            the unique conversation identifier
     * @param adminUser
     *            the admin user requesting conversation history
     * @return conversation history with messages
     * @throws InvalidRequestException
     *             if conversation UUID is invalid
     * @throws ResourceNotFoundException
     *             if no messages found
     * @throws AccessDeniedException
     *             if admin user lacks access to the conversation
     */
    public ConversationHistoryDTO getConversationHistory( String conversationUuid, User user )
    {
        validateConversationUuid( conversationUuid );
        String userId = user.getAccessCode( );
        validateConversationAccess( conversationUuid, userId, user );

        List<MessageDTO> messages = _botQueryService.getConversationHistory( conversationUuid, userId );
        if ( messages == null || messages.isEmpty( ) )
        {
            throw new ResourceNotFoundException( ERROR_NO_MESSAGES_FOUND + conversationUuid );
        }
        return new ConversationHistoryDTO( conversationUuid, messages );
    }

    /**
     * Retrieves all conversations for an admin user.
     *
     * @param adminUser
     *            the admin user requesting conversations
     * @return list of authorized conversations for the admin user
     */
    public List<ConversationDTO> getUserConversations( User user )
    {
        String userId = user.getAccessCode( );
        List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
        List<BotConversation> conversations = _botMemoryService.getConversationsByUserId( userId );

        return conversations.stream( ).filter( conversation -> isConversationAuthorized( conversation, authorizedClientIds ) )
                .map( conversation -> createConversationDTO( conversation, userId ) ).toList( );
    }

    /**
     * Deletes a specific conversation for an admin user.
     *
     * @param conversationUuid
     *            the unique conversation identifier
     * @param adminUser
     *            the admin user requesting conversation deletion
     * @throws InvalidRequestException
     *             if conversation UUID is invalid
     * @throws AccessDeniedException
     *             if admin user lacks access to the conversation
     */
    public void deleteConversation( String conversationUuid, User user )
    {
        validateConversationUuid( conversationUuid );
        String userId = user.getAccessCode( );
        validateConversationAccess( conversationUuid, userId, user );
        _botMemoryService.deleteConversation( conversationUuid, userId );
    }

    /**
     * Deletes all conversations for an admin user.
     *
     * @param adminUser
     *            the admin user requesting conversation deletion
     */
    public void deleteAllUserConversations( User user )
    {
        String userId = user.getAccessCode( );
        List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
        List<BotConversation> conversations = _botMemoryService.getConversationsByUserId( userId );

        conversations.stream( ).filter( conversation -> isConversationAuthorized( conversation, authorizedClientIds ) )
                .forEach( conversation -> _botMemoryService.deleteConversation( conversation.getConversationUuid( ), userId ) );
    }

    /**
     * Retrieves a document file for an admin user with bot access validation.
     *
     * @param botId
     *            the bot identifier
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     * @param adminUser
     *            the admin user requesting document access
     * @return document file with input stream and name
     * @throws AccessDeniedException
     *             if admin user lacks access to the bot
     * @throws ResourceNotFoundException
     *             if document is not found
     */
    public DocumentFileDTO getDocumentFile( int botId, int datasetId, int documentId, User user )
    {
        validateBotAccess( botId, user );

        InputStream inputStream = DocumentService.getDocumentFileWithValidation( botId, datasetId, documentId );
        if ( inputStream == null )
        {
            throw new ResourceNotFoundException( ERROR_DOCUMENT_NOT_FOUND );
        }

        String documentName = DocumentService.getDocumentName( documentId );
        return new DocumentFileDTO( inputStream, documentName );
    }

    /**
     * Validates a query request for required fields.
     *
     * @param request
     *            the query request to validate
     * @throws InvalidRequestException
     *             if request is null or missing required fields
     */
    private void validateQueryRequest( QueryRequestDTO request )
    {
        if ( request == null || request.getBotId( ) <= 0 || request.getQuery( ) == null || request.getQuery( ).isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_INVALID_QUERY_REQUEST );
        }
    }

    /**
     * Validates a conversation UUID.
     *
     * @param conversationUuid
     *            the conversation UUID to validate
     * @throws InvalidRequestException
     *             if UUID is null or empty
     */
    private void validateConversationUuid( String conversationUuid )
    {
        if ( conversationUuid == null || conversationUuid.isEmpty( ) )
        {
            throw new InvalidRequestException( ERROR_INVALID_CONVERSATION_UUID );
        }
    }

    /**
     * Validates bot access for an admin user.
     *
     * @param botId
     *            the bot identifier
     * @param adminUser
     *            the admin user requesting access
     * @return the bot if access is granted
     * @throws AccessDeniedException
     *             if bot not found or admin user lacks access
     */
    private Bot validateBotAccess( int botId, User user )
    {
        Bot bot = BotHome.findByPrimaryKey( botId ).orElse( null );
        if ( bot == null )
        {
            throw new AccessDeniedException( ERROR_BOT_NOT_FOUND );
        }

        List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
        if ( !authorizedClientIds.contains( bot.getClientId( ) ) )
        {
            throw new AccessDeniedException( ERROR_ACCESS_DENIED_BOT );
        }
        return bot;
    }

    /**
     * Validates conversation access for an admin user.
     *
     * @param conversationUuid
     *            the conversation UUID
     * @param userId
     *            the user identifier
     * @param adminUser
     *            the admin user requesting access
     * @throws AccessDeniedException
     *             if conversation not found or admin user lacks access
     */
    private void validateConversationAccess( String conversationUuid, String userId, User user )
    {
        BotConversation conversation = BotConversationHome.findByUuid( conversationUuid ).orElse( null );
        if ( conversation == null || !conversation.getUserId( ).equals( userId ) )
        {
            throw new AccessDeniedException( ERROR_UNAUTHORIZED_CONVERSATION );
        }

        Bot bot = BotHome.findByPrimaryKey( conversation.getBotId( ) ).orElse( null );
        if ( bot != null )
        {
            List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
            if ( !authorizedClientIds.contains( bot.getClientId( ) ) )
            {
                throw new AccessDeniedException( ERROR_ACCESS_DENIED_CONVERSATION );
            }
        }
    }

    /**
     * Checks if a conversation is authorized for given client IDs.
     *
     * @param conversation
     *            the bot conversation
     * @param authorizedClientIds
     *            list of authorized client IDs
     * @return true if conversation is authorized, false otherwise
     */
    private boolean isConversationAuthorized( BotConversation conversation, List<Integer> authorizedClientIds )
    {
        return BotHome.findByPrimaryKey( conversation.getBotId( ) ).map( bot -> authorizedClientIds.contains( bot.getClientId( ) ) ).orElse( false );
    }

    /**
     * Creates a conversation DTO from a bot conversation and user ID.
     *
     * @param conversation
     *            the bot conversation
     * @param userId
     *            the user identifier
     * @return conversation DTO with populated fields
     */
    private ConversationDTO createConversationDTO( BotConversation conversation, String userId )
    {
        ConversationDTO dto = new ConversationDTO( );
        dto.setConversationUuid( conversation.getConversationUuid( ) );
        dto.setBotId( conversation.getBotId( ) );

        BotHome.findByPrimaryKey( conversation.getBotId( ) ).ifPresent( bot -> dto.setBotName( bot.getBotName( ) ) );

        dto.setCreatedAt( conversation.getCreatedAt( ) );
        dto.setUpdatedAt( conversation.getUpdatedAt( ) );

        List<MessageDTO> messages = _botQueryService.getConversationHistory( conversation.getConversationUuid( ), userId );
        if ( !messages.isEmpty( ) )
        {
            dto.setLastMessage( messages.get( messages.size( ) - 1 ).getMessage( ) );
        }
        return dto;
    }
}
