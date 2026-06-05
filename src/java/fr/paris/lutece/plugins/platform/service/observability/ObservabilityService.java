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
package fr.paris.lutece.plugins.platform.service.observability;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import fr.paris.lutece.plugins.platform.business.bot.BotHome;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeHome;
import fr.paris.lutece.plugins.platform.business.model.Model;
import fr.paris.lutece.plugins.platform.business.model.ModelHome;
import fr.paris.lutece.plugins.platform.business.observability.DailyResourceStats;
import fr.paris.lutece.plugins.platform.business.observability.ResourceStats;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;
import fr.paris.lutece.plugins.platform.business.bot.Bot;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientChartSeries;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientObservabilityStats;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientObservabilityView;
import fr.paris.lutece.plugins.platform.service.observability.dto.ClientResourcesView;
import fr.paris.lutece.plugins.platform.service.observability.dto.DailyStatPoint;
import fr.paris.lutece.plugins.platform.service.observability.dto.DateRange;
import fr.paris.lutece.plugins.platform.service.observability.dto.GlobalObservabilityStats;
import fr.paris.lutece.plugins.platform.service.observability.dto.ResourceObservabilitySummary;
import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionStatus;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeExecution;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeExecutionHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTrace;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceHome;
import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.resource.PlatformResourceItem;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.plugins.platform.service.resource.PlatformResourceService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service class for managing observability of platform resource executions. Provides functionality for tracking, monitoring, and analyzing resource execution
 * lifecycle.
 */
@ApplicationScoped
public class ObservabilityService
{
    private static final String ERROR_STARTING_RESOURCE_EXECUTION = "Error starting resource execution";
    private static final String ERROR_COMPLETING_RESOURCE_EXECUTION = "Error completing resource execution";
    private static final String ERROR_COMPLETING_RESOURCE_EXECUTION_WITH_ERROR = "Error completing resource execution with error";
    private static final String ERROR_CANCELLING_RESOURCE_EXECUTION = "Error cancelling resource execution";
    private static final String ERROR_STARTING_NODE_EXECUTION = "Error starting node execution";
    private static final String ERROR_COMPLETING_NODE_EXECUTION = "Error completing node execution";
    private static final String ERROR_COMPLETING_NODE_EXECUTION_WITH_ERROR = "Error completing node execution with error";
    private static final String ERROR_ADDING_NODE_TRACE = "Error adding node trace";
    private static final String LOG_NODE_NOT_FOUND = "Node execution completion ignored: nodeId {} not found for execution {} (start event likely dropped)";
    private static final String ERROR_PARSING_RESOURCE_ID = "Error parsing resource ID: ";
    @Inject
    private PlatformResourceService _platformResourceService;

    @Inject
    private ObservabilityWriter _writer;

    @Inject
    private SubscriptionService _subscriptionService;

    /**
     * Default constructor for CDI.
     */
    ObservabilityService( )
    {
    }

    /**
     * Starts a new resource execution and records its initial state.
     *
     * @param resourceType
     *            the type of the resource being executed
     * @param resourceId
     *            the unique identifier of the resource
     * @param clientId
     *            the identifier of the client initiating the execution
     * @param inputData
     *            the input data for the execution
     * @return the generated execution identifier
     */
    public String startResourceExecution( String resourceType, String resourceId, int clientId, ObservabilityData inputData )
    {
        String executionId = UUID.randomUUID( ).toString( );

        _writer.submit( ( ) -> {
            try
            {
                PlatformResourceExecution execution = new PlatformResourceExecution( );
                execution.setExecutionId( executionId );
                execution.setResourceType( resourceType );
                execution.setResourceId( resourceId );
                execution.setClientId( clientId );
                execution.setStatus( PlatformResourceExecutionStatus.RUNNING.getValue( ) );
                execution.setStartTime( new Timestamp( System.currentTimeMillis( ) ) );
                execution.setInputData( inputData != null ? inputData.toJson( ) : null );
                execution.setTotalCost( BigDecimal.ZERO );

                PlatformResourceExecutionHome.create( execution );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_STARTING_RESOURCE_EXECUTION, e );
            }
        } );

        return executionId;
    }

    /**
     * Marks a resource execution as successfully completed.
     *
     * @param executionId
     *            the unique identifier of the execution to complete
     * @param outputData
     *            the output data produced by the execution
     */
    public void completeResourceExecutionSuccess( String executionId, ObservabilityData outputData )
    {
        _writer.submit( ( ) -> {
            try
            {
                PlatformResourceExecutionHome.findByPrimaryKey( executionId ).ifPresent( execution -> {
                    execution.setStatus( PlatformResourceExecutionStatus.COMPLETED.getValue( ) );
                    execution.setEndTime( new Timestamp( System.currentTimeMillis( ) ) );
                    execution.setOutputData( outputData != null ? outputData.toJson( ) : null );

                    BigDecimal totalCost = calculateTotalCostFromNodes( executionId );
                    execution.setTotalCost( totalCost );

                    PlatformResourceExecutionHome.update( execution );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_COMPLETING_RESOURCE_EXECUTION, e );
            }
        } );
    }

    /**
     * Marks a resource execution as completed with an error.
     *
     * @param executionId
     *            the unique identifier of the execution to complete
     * @param errorMessage
     *            the error message describing the failure
     * @param outputData
     *            the output data produced before the error occurred
     */
    public void completeResourceExecutionError( String executionId, String errorMessage, ObservabilityData outputData )
    {
        _writer.submit( ( ) -> {
            try
            {
                PlatformResourceExecutionHome.findByPrimaryKey( executionId ).ifPresent( execution -> {
                    execution.setStatus( PlatformResourceExecutionStatus.ERROR.getValue( ) );
                    execution.setEndTime( new Timestamp( System.currentTimeMillis( ) ) );
                    execution.setErrorMessage( errorMessage );
                    execution.setOutputData( outputData != null ? outputData.toJson( ) : null );

                    BigDecimal totalCost = calculateTotalCostFromNodes( executionId );
                    execution.setTotalCost( totalCost );

                    PlatformResourceExecutionHome.update( execution );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_COMPLETING_RESOURCE_EXECUTION_WITH_ERROR, e );
            }
        } );
    }

    /**
     * Cancels an ongoing resource execution.
     *
     * @param executionId
     *            the unique identifier of the execution to cancel
     */
    public void cancelResourceExecution( String executionId )
    {
        _writer.submit( ( ) -> {
            try
            {
                PlatformResourceExecutionHome.findByPrimaryKey( executionId ).ifPresent( execution -> {
                    execution.setStatus( PlatformResourceExecutionStatus.CANCELLED.getValue( ) );
                    execution.setEndTime( new Timestamp( System.currentTimeMillis( ) ) );

                    BigDecimal totalCost = calculateTotalCostFromNodes( executionId );
                    execution.setTotalCost( totalCost );

                    PlatformResourceExecutionHome.update( execution );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_CANCELLING_RESOURCE_EXECUTION, e );
            }
        } );
    }

    /**
     * Retrieves all executions for a specific resource, optionally filtered by client.
     *
     * @param resourceType
     *            the type of the resource
     * @param resourceId
     *            the unique identifier of the resource
     * @param clientId
     *            the identifier of the client (use 0 or negative to skip filtering)
     * @return a list of resource executions matching the criteria
     */
    public List<PlatformResourceExecution> getResourceExecutions( String resourceType, String resourceId, int clientId )
    {
        List<PlatformResourceExecution> executions = PlatformResourceExecutionHome.getPlatformResourceExecutions( resourceType, resourceId );
        enrichExecutionsWithNames( executions );

        if ( clientId > 0 )
        {
            executions = executions.stream( ).filter( e -> clientId == e.getClientId( ) ).toList( );
        }

        return executions;
    }

    /**
     * Retrieves executions for a specific resource within a date range, optionally filtered by client.
     *
     * @param resourceType
     *            the type of the resource
     * @param resourceId
     *            the unique identifier of the resource
     * @param clientId
     *            the identifier of the client (use 0 or negative to skip filtering)
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return a list of resource executions matching the criteria
     */
    public List<PlatformResourceExecution> getResourceExecutionsByDateRange( String resourceType, String resourceId, int clientId, Timestamp startDate,
            Timestamp endDate )
    {
        List<PlatformResourceExecution> executions = PlatformResourceExecutionHome.getPlatformResourceExecutionsByDateRange( resourceType, resourceId,
                startDate, endDate );
        enrichExecutionsWithNames( executions );

        if ( clientId > 0 )
        {
            executions = executions.stream( ).filter( e -> clientId == e.getClientId( ) ).toList( );
        }

        return executions;
    }

    /**
     * Retrieves a specific execution by its identifier.
     *
     * @param executionId
     *            the unique identifier of the execution
     * @return an Optional containing the execution if found, empty otherwise
     */
    public Optional<PlatformResourceExecution> getExecution( String executionId )
    {
        Optional<PlatformResourceExecution> optExecution = PlatformResourceExecutionHome.findByPrimaryKey( executionId );

        optExecution.ifPresent( this::enrichExecutionWithNames );

        return optExecution;
    }

    /**
     * Starts the execution of a node within a resource execution.
     *
     * @param executionId
     *            the unique identifier of the parent execution
     * @param nodeId
     *            the unique identifier of the node
     * @param nodeName
     *            the name of the node
     * @param executionOrder
     *            the order of execution for this node
     * @param inputData
     *            the input data for the node execution
     */
    public void startNodeExecution( String executionId, String nodeId, String nodeName, int executionOrder, ObservabilityData inputData )
    {
        _writer.submit( ( ) -> {
            try
            {
                PlatformResourceNodeExecution nodeExecution = new PlatformResourceNodeExecution( );
                nodeExecution.setExecutionId( executionId );
                nodeExecution.setNodeId( nodeId );
                nodeExecution.setNodeName( nodeName );
                nodeExecution.setStatus( PlatformResourceExecutionStatus.RUNNING.getValue( ) );
                nodeExecution.setStartTime( new Timestamp( System.currentTimeMillis( ) ) );
                nodeExecution.setInputData( inputData != null ? inputData.toJson( ) : null );
                nodeExecution.setExecutionOrder( executionOrder );
                nodeExecution.setTotalCost( BigDecimal.ZERO );

                PlatformResourceNodeExecutionHome.create( nodeExecution );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_STARTING_NODE_EXECUTION, e );
            }
        } );
    }

    /**
     * Marks a node execution as successfully completed.
     *
     * @param executionId
     *            the unique identifier of the parent execution
     * @param nodeId
     *            the unique identifier of the node
     * @param outputData
     *            the output data produced by the node execution
     */
    public void completeNodeExecutionSuccess( String executionId, String nodeId, ObservabilityData outputData )
    {
        _writer.submit( ( ) -> {
            try
            {
                findNodeExecution( executionId, nodeId ).ifPresent( nodeExecution -> {
                    nodeExecution.setStatus( PlatformResourceExecutionStatus.COMPLETED.getValue( ) );
                    nodeExecution.setEndTime( new Timestamp( System.currentTimeMillis( ) ) );
                    nodeExecution.setOutputData( outputData != null ? outputData.toJson( ) : null );
                    nodeExecution.setTotalCost( calculateNodeTotalCost( nodeExecution.getId( ) ) );
                    PlatformResourceNodeExecutionHome.update( nodeExecution );
                    updateResourceExecutionCost( executionId );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_COMPLETING_NODE_EXECUTION, e );
            }
        } );
    }

    /**
     * Finds a node execution of an execution by its node identifier, logging when no node matches.
     *
     * @param executionId
     *            the unique identifier of the parent execution
     * @param nodeId
     *            the unique identifier of the node
     * @return the matching node execution, or empty when the node is unknown for this execution
     */
    private Optional<PlatformResourceNodeExecution> findNodeExecution( String executionId, String nodeId )
    {
        Optional<PlatformResourceNodeExecution> match = PlatformResourceNodeExecutionHome.getNodeExecutions( executionId ).stream( )
                .filter( nodeExecution -> nodeExecution.getNodeId( ).equals( nodeId ) ).findFirst( );
        if ( match.isEmpty( ) )
        {
            AppLogService.error( LOG_NODE_NOT_FOUND, nodeId, executionId );
        }
        return match;
    }

    /**
     * Marks a node execution as completed with an error.
     *
     * @param executionId
     *            the unique identifier of the parent execution
     * @param nodeId
     *            the unique identifier of the node
     * @param errorMessage
     *            the error message describing the failure
     * @param outputData
     *            the output data produced before the error occurred
     */
    public void completeNodeExecutionError( String executionId, String nodeId, String errorMessage, ObservabilityData outputData )
    {
        _writer.submit( ( ) -> {
            try
            {
                findNodeExecution( executionId, nodeId ).ifPresent( nodeExecution -> {
                    nodeExecution.setStatus( PlatformResourceExecutionStatus.ERROR.getValue( ) );
                    nodeExecution.setEndTime( new Timestamp( System.currentTimeMillis( ) ) );
                    nodeExecution.setErrorMessage( errorMessage );
                    nodeExecution.setOutputData( outputData != null ? outputData.toJson( ) : null );
                    nodeExecution.setTotalCost( calculateNodeTotalCost( nodeExecution.getId( ) ) );
                    PlatformResourceNodeExecutionHome.update( nodeExecution );
                    updateResourceExecutionCost( executionId );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_COMPLETING_NODE_EXECUTION_WITH_ERROR, e );
            }
        } );
    }

    /**
     * Adds a trace entry to a node execution for detailed monitoring.
     *
     * @param executionId
     *            the unique identifier of the parent execution
     * @param nodeId
     *            the unique identifier of the node
     * @param traceData
     *            the trace data to record
     * @param status
     *            the status of the trace
     * @param cost
     *            the cost associated with this trace entry
     */
    public void addNodeTrace( String executionId, String nodeId, ObservabilityData traceData, String status, BigDecimal cost )
    {
        _writer.submit( ( ) -> {
            try
            {
                findNodeExecution( executionId, nodeId ).ifPresent( nodeExecution -> {
                    PlatformResourceNodeTrace trace = new PlatformResourceNodeTrace( );
                    trace.setIdNodeExecution( nodeExecution.getId( ) );
                    trace.setMessage( traceData != null ? traceData.getType( ) : null );
                    trace.setData( traceData != null ? traceData.toJson( ) : null );
                    trace.setTimestamp( new Timestamp( System.currentTimeMillis( ) ) );
                    trace.setStatus( status );
                    trace.setCost( cost != null ? cost : BigDecimal.ZERO );
                    PlatformResourceNodeTraceHome.create( trace );
                    updateNodeTotalCost( nodeExecution );
                    updateResourceExecutionCost( executionId );
                } );
            }
            catch( Exception e )
            {
                AppLogService.error( ERROR_ADDING_NODE_TRACE, e );
            }
        } );
    }

    /**
     * Enriches a list of executions with human-readable resource names.
     *
     * @param executions
     *            the list of executions to enrich
     */
    public void enrichExecutionsWithNames( List<PlatformResourceExecution> executions )
    {
        Map<String, IPlatformResourceType> resourceTypesMap = new HashMap<>( );
        for ( IPlatformResourceType type : _platformResourceService.getAllResourceType( ) )
        {
            resourceTypesMap.put( type.getResourceType( ), type );
        }

        Map<String, Map<String, PlatformResourceItem>> resourcesByType = new HashMap<>( );

        for ( PlatformResourceExecution execution : executions )
        {
            enrichExecutionWithNames( execution, resourceTypesMap, resourcesByType );
        }
    }

    /**
     * Enriches a single execution with human-readable resource names.
     *
     * @param execution
     *            the execution to enrich
     */
    private void enrichExecutionWithNames( PlatformResourceExecution execution )
    {
        Map<String, IPlatformResourceType> resourceTypesMap = new HashMap<>( );

        for ( IPlatformResourceType type : _platformResourceService.getAllResourceType( ) )
        {
            resourceTypesMap.put( type.getResourceType( ), type );
        }

        enrichExecutionWithNames( execution, resourceTypesMap, new HashMap<>( ) );
    }

    /**
     * Retrieves an execution with its full details (node executions and their traces).
     *
     * @param executionId
     *            the unique identifier of the execution
     * @return an Optional containing the execution with details if found, empty otherwise
     */
    public Optional<PlatformResourceExecution> getExecutionWithDetails( String executionId )
    {
        Optional<PlatformResourceExecution> optExecution = getExecution( executionId );

        optExecution.ifPresent( execution -> {
            List<PlatformResourceNodeExecution> nodeExecutions = PlatformResourceNodeExecutionHome.getNodeExecutions( executionId );

            for ( PlatformResourceNodeExecution nodeExecution : nodeExecutions )
            {
                nodeExecution.setTraces( PlatformResourceNodeTraceHome.getNodeTraces( nodeExecution.getId( ) ) );
            }

            execution.setNodeExecutions( nodeExecutions );
        } );

        return optExecution;
    }

    /**
     * Builds a resource items map for a list of executions, indexed by resource type and resource id.
     *
     * @param executions
     *            the list of executions
     * @return a map of resource type to resource id to PlatformResourceItem
     */
    public Map<String, Map<String, PlatformResourceItem>> getResourceItemsForExecutions( List<PlatformResourceExecution> executions )
    {
        Map<String, IPlatformResourceType> resourceTypeMap = _platformResourceService.getAllResourceType( ).stream( )
                .collect( Collectors.toMap( IPlatformResourceType::getResourceType, rt -> rt ) );

        Map<String, Map<String, PlatformResourceItem>> resourceItems = new HashMap<>( );

        for ( PlatformResourceExecution execution : executions )
        {
            String resType = execution.getResourceType( );
            String resId = execution.getResourceId( );

            if ( !resourceItems.containsKey( resType ) )
            {
                resourceItems.put( resType, new HashMap<>( ) );
            }

            if ( !resourceItems.get( resType ).containsKey( resId ) )
            {
                IPlatformResourceType platformResourceType = resourceTypeMap.get( resType );
                if ( platformResourceType != null )
                {
                    List<PlatformResourceItem> allItems = platformResourceType.getResourceList( );
                    if ( allItems != null )
                    {
                        for ( PlatformResourceItem item : allItems )
                        {
                            if ( String.valueOf( item.getId( ) ).equals( resId ) )
                            {
                                resourceItems.get( resType ).put( resId, item );
                                break;
                            }
                        }
                    }
                }
            }
        }

        return resourceItems;
    }

    /**
     * Retrieves the total costs grouped by resource for a specific client.
     *
     * @param clientId
     *            the identifier of the client
     * @return a map of resource types to resource IDs to their total costs
     */
    public Map<String, Map<String, BigDecimal>> getTotalCostsByResource( int clientId )
    {
        return PlatformResourceExecutionHome.getTotalCostsByResource( clientId );
    }

    /**
     * Retrieves execution statistics grouped by resource for a specific client.
     *
     * @param clientId
     *            the identifier of the client
     * @return a map of resource types to resource IDs to their statistics
     */
    public List<ResourceStats> getStatsByResource( int clientId )
    {
        return PlatformResourceExecutionHome.getStatsByResource( clientId );
    }

    /**
     * Retrieves the execution statistics of a single resource for a client.
     *
     * @param clientId
     *            the identifier of the client
     * @param resourceType
     *            the resource type
     * @param resourceId
     *            the resource identifier
     * @return the matching statistics if present
     */
    public Optional<ResourceStats> getStatsForResource( int clientId, String resourceType, String resourceId )
    {
        return getStatsByResource( clientId ).stream( )
                .filter( stats -> resourceType.equals( stats.getResourceType( ) ) && resourceId.equals( stats.getResourceId( ) ) ).findFirst( );
    }

    /**
     * Retrieves execution statistics grouped by resource for a specific client within a date range.
     *
     * @param clientId
     *            the identifier of the client
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return the list of resource statistics
     */
    public List<ResourceStats> getStatsByResourceAndDateRange( int clientId, Timestamp startDate, Timestamp endDate )
    {
        return PlatformResourceExecutionHome.getStatsByResourceAndDateRange( clientId, startDate, endDate );
    }

    /**
     * Retrieves daily execution statistics for a specific resource within a date range.
     *
     * @param resourceType
     *            the resource type
     * @param resourceId
     *            the resource identifier
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return the list of daily resource statistics
     */
    public List<DailyResourceStats> getDailyStatsByResource( String resourceType, String resourceId, Timestamp startDate, Timestamp endDate )
    {
        return PlatformResourceExecutionHome.getDailyStatsByResource( resourceType, resourceId, startDate, endDate );
    }

    /**
     * Retrieves daily execution statistics for a specific client within a date range.
     *
     * @param clientId
     *            the identifier of the client
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return the list of daily resource statistics
     */
    public List<DailyResourceStats> getDailyStatsByClient( int clientId, Timestamp startDate, Timestamp endDate )
    {
        return PlatformResourceExecutionHome.getDailyStatsByClient( clientId, startDate, endDate );
    }

    /**
     * Builds the typed resource overview of a client: the available resource types, the resources of each type enriched in place with their aggregated stats,
     * the client's subscriptions and the subscriptions index. Replaces the stats-folding and entity-enrichment loops that previously lived in the controller.
     *
     * @param clientId
     *            the client identifier
     * @param user
     *            the user performing the lookup (used to scope the resource lists)
     * @return the typed client resources view
     */
    public ClientResourcesView getClientResourcesView( int clientId, User user )
    {
        List<Subscription> subscriptions = _subscriptionService.getSubscriptionsByClientId( clientId );

        Map<String, Map<String, Subscription>> subscriptionsByTypeAndId = new HashMap<>( );
        for ( Subscription subscription : subscriptions )
        {
            subscriptionsByTypeAndId.computeIfAbsent( subscription.getResourceType( ), k -> new HashMap<>( ) ).put( subscription.getResourceId( ),
                    subscription );
        }

        Map<String, Map<String, ResourceStats>> statsByResource = indexStatsByResource( getStatsByResource( clientId ) );
        List<IPlatformResourceType> resourceTypes = _platformResourceService.getAllResourceType( );
        Map<String, List<PlatformResourceItem>> resourcesByType = new HashMap<>( );

        for ( IPlatformResourceType resourceType : resourceTypes )
        {
            List<PlatformResourceItem> resources = resourceType.getResourceList( clientId, user );
            if ( resources != null && !resources.isEmpty( ) )
            {
                applyStats( resources, statsByResource.get( resourceType.getResourceType( ) ) );
                resourcesByType.put( resourceType.getResourceType( ), resources );
            }
        }

        return new ClientResourcesView( resourceTypes, resourcesByType, subscriptions, subscriptionsByTypeAndId );
    }

    /**
     * Aggregates a single client's observability dashboard over the given date range: the stats-enriched resources grouped by type, the per-type observability
     * URLs and the aggregated KPI block, plus the typed daily chart points. The average duration is derived from the daily-stats weighted sum (no full
     * execution scan). Replaces the largest aggregation block that previously lived in the controller.
     *
     * @param clientId
     *            the client identifier
     * @param user
     *            the user performing the lookup (used to scope the resource lists)
     * @param range
     *            the date range window
     * @return the typed client observability view
     */
    public ClientObservabilityView getClientObservabilityView( int clientId, User user, DateRange range )
    {
        List<ResourceStats> statsList = range == DateRange.ALL ? getStatsByResource( clientId )
                : getStatsByResourceAndDateRange( clientId, range.resolveBounds( ) [0], range.resolveBounds( ) [1] );

        Map<String, Map<String, ResourceStats>> statsByResource = indexStatsByResource( statsList );

        List<IPlatformResourceType> resourceTypes = _platformResourceService.getAllResourceType( );
        Map<String, List<PlatformResourceItem>> resourcesByType = new HashMap<>( );
        Map<String, Map<String, String>> observabilityUrlsByType = new HashMap<>( );

        int totalExecutions = 0;
        double totalCost = 0;
        int totalCompleted = 0;

        for ( IPlatformResourceType resourceType : resourceTypes )
        {
            Map<String, ResourceStats> typeStats = statsByResource.get( resourceType.getResourceType( ) );
            if ( typeStats == null || typeStats.isEmpty( ) )
            {
                continue;
            }

            List<PlatformResourceItem> resources = resourceType.getResourceList( clientId, user );
            if ( resources == null || resources.isEmpty( ) )
            {
                continue;
            }

            List<PlatformResourceItem> enrichedResources = new ArrayList<>( );
            Map<String, String> typeObsUrls = new HashMap<>( );

            for ( PlatformResourceItem resource : resources )
            {
                ResourceStats stats = typeStats.get( String.valueOf( resource.getId( ) ) );
                if ( stats != null )
                {
                    applyStats( resource, stats );
                    totalExecutions += stats.getExecutionCount( );
                    totalCost += stats.getTotalCost( );
                    totalCompleted += (int) Math.round( stats.getExecutionCount( ) * stats.getSuccessRate( ) / 100.0 );

                    String obsUrl = resourceType.getObservabilityListUrl( clientId, String.valueOf( resource.getId( ) ) );
                    if ( obsUrl != null )
                    {
                        typeObsUrls.put( String.valueOf( resource.getId( ) ), obsUrl );
                    }
                    enrichedResources.add( resource );
                }
            }

            if ( !enrichedResources.isEmpty( ) )
            {
                resourcesByType.put( resourceType.getResourceType( ), enrichedResources );
                observabilityUrlsByType.put( resourceType.getResourceType( ), typeObsUrls );
            }
        }

        int totalErrors = totalExecutions - totalCompleted;
        double globalSuccessRate = totalExecutions > 0 ? ( totalCompleted * 100.0 / totalExecutions ) : 0;

        Timestamp [ ] bounds = range == DateRange.ALL ? null : range.resolveBounds( );
        List<DailyResourceStats> dailyStats = getDailyStatsByClient( clientId, bounds != null ? bounds [0] : null, bounds != null ? bounds [1] : null );
        double avgDuration = weightedAvgDuration( dailyStats );

        return new ClientObservabilityView( resourceTypes, resourcesByType, observabilityUrlsByType, totalExecutions, totalCost, totalErrors, globalSuccessRate,
                avgDuration, toDailyStatPoints( dailyStats ) );
    }

    /**
     * Indexes a flat list of resource stats by resource type then resource id.
     *
     * @param statsList
     *            the flat stats list
     * @return the nested index
     */
    private Map<String, Map<String, ResourceStats>> indexStatsByResource( List<ResourceStats> statsList )
    {
        Map<String, Map<String, ResourceStats>> statsByResource = new HashMap<>( );
        for ( ResourceStats stats : statsList )
        {
            statsByResource.computeIfAbsent( stats.getResourceType( ), k -> new HashMap<>( ) ).put( stats.getResourceId( ), stats );
        }
        return statsByResource;
    }

    /**
     * Copies aggregated stats onto each resource of a list, matching by resource id.
     *
     * @param resources
     *            the resources to enrich in place
     * @param typeStats
     *            the stats of the matching resource type, indexed by resource id (may be null)
     */
    private void applyStats( List<PlatformResourceItem> resources, Map<String, ResourceStats> typeStats )
    {
        if ( typeStats == null )
        {
            return;
        }
        for ( PlatformResourceItem resource : resources )
        {
            ResourceStats stats = typeStats.get( String.valueOf( resource.getId( ) ) );
            if ( stats != null )
            {
                applyStats( resource, stats );
            }
        }
    }

    /**
     * Copies aggregated stats onto a single resource.
     *
     * @param resource
     *            the resource to enrich in place
     * @param stats
     *            the stats to copy
     */
    private void applyStats( PlatformResourceItem resource, ResourceStats stats )
    {
        resource.setTotalCost( stats.getTotalCost( ) );
        resource.setExecutionCount( stats.getExecutionCount( ) );
        resource.setSuccessRate( stats.getSuccessRate( ) );
        resource.setLastExecutionDate( stats.getLastExecution( ) );
    }

    /**
     * Computes the execution-count-weighted average duration from a daily-stats series.
     *
     * @param dailyStats
     *            the daily stats
     * @return the weighted average duration in seconds, or 0 when no execution carries a duration
     */
    private double weightedAvgDuration( List<DailyResourceStats> dailyStats )
    {
        double weightedSum = 0;
        int count = 0;
        for ( DailyResourceStats ds : dailyStats )
        {
            if ( ds.getAvgDurationSeconds( ) > 0 && ds.getExecutionCount( ) > 0 )
            {
                weightedSum += ds.getAvgDurationSeconds( ) * ds.getExecutionCount( );
                count += ds.getExecutionCount( );
            }
        }
        return count > 0 ? weightedSum / count : 0;
    }

    /**
     * Projects a daily-stats series to typed chart points (day, count, cost, average duration, errors). Owns the chart-payload contract so the controller only
     * serializes the result.
     *
     * @param dailyStats
     *            the daily stats
     * @return the typed daily chart points
     */
    public List<DailyStatPoint> toDailyStatPoints( List<DailyResourceStats> dailyStats )
    {
        return dailyStats.stream( )
                .map( s -> new DailyStatPoint( s.getDay( ), s.getExecutionCount( ), s.getTotalCost( ), s.getAvgDurationSeconds( ), s.getErrorCount( ) ) )
                .toList( );
    }

    /**
     * Builds the typed per-client chart series for the global observability chart, projecting each client's daily series to typed chart points. Owns the
     * chart-payload contract so the controller only serializes the result.
     *
     * @param clientStats
     *            the per-client stats from {@link #getGlobalStats}
     * @return the typed per-client chart series
     */
    public List<ClientChartSeries> buildChartSeries( List<ClientObservabilityStats> clientStats )
    {
        List<ClientChartSeries> series = new ArrayList<>( clientStats.size( ) );
        for ( ClientObservabilityStats cs : clientStats )
        {
            List<DailyStatPoint> daily = cs.getDailySeries( ) != null ? toDailyStatPoints( cs.getDailySeries( ) ) : new ArrayList<>( );
            series.add( new ClientChartSeries( cs.getClient( ).getId( ), cs.getClient( ).getName( ), daily ) );
        }
        return series;
    }

    /**
     * Pads each client's daily series with zeroed entries for any day present in at least one other client. Aligns all series on the same time axis so
     * consumers can stack/zip them safely.
     *
     * @param clientStatsList
     *            the per-client stats whose dailySeries will be aligned in place
     */
    private void padDailySeriesForAlignment( List<ClientObservabilityStats> clientStatsList )
    {
        TreeSet<Date> allDays = new TreeSet<>( );
        for ( ClientObservabilityStats cs : clientStatsList )
        {
            if ( cs.getDailySeries( ) != null )
            {
                for ( DailyResourceStats ds : cs.getDailySeries( ) )
                {
                    allDays.add( ds.getDay( ) );
                }
            }
        }
        if ( allDays.isEmpty( ) )
        {
            return;
        }
        for ( ClientObservabilityStats cs : clientStatsList )
        {
            Map<Date, DailyResourceStats> existing = new HashMap<>( );
            if ( cs.getDailySeries( ) != null )
            {
                for ( DailyResourceStats ds : cs.getDailySeries( ) )
                {
                    existing.put( ds.getDay( ), ds );
                }
            }
            List<DailyResourceStats> aligned = new ArrayList<>( allDays.size( ) );
            for ( Date day : allDays )
            {
                DailyResourceStats ds = existing.get( day );
                if ( ds == null )
                {
                    ds = new DailyResourceStats( );
                    ds.setDay( day );
                }
                aligned.add( ds );
            }
            cs.setDailySeries( aligned );
        }
    }

    /**
     * Aggregates per-client and global observability statistics for the given clients over a date range. Returns a typed DTO consumable by any presentation
     * layer (XPage, REST endpoint, batch report…). For each client: one stats SQL query + one daily-stats SQL query (no full execution scan).
     *
     * @param clients
     *            the clients to aggregate over
     * @param startDate
     *            the start of the date range (null = all-time)
     * @param endDate
     *            the end of the date range (null = all-time)
     * @return aggregated statistics for the whole user scope
     */
    public GlobalObservabilityStats getGlobalStats( Collection<Client> clients, Timestamp startDate, Timestamp endDate )
    {
        List<ClientObservabilityStats> clientStatsList = new ArrayList<>( );

        int totalExecutions = 0;
        double totalCost = 0;
        int totalCompleted = 0;
        double totalDurationWeightedSum = 0;
        int totalDurationCount = 0;

        for ( Client client : clients )
        {
            List<ResourceStats> statsList = startDate != null ? getStatsByResourceAndDateRange( client.getId( ), startDate, endDate )
                    : getStatsByResource( client.getId( ) );

            int clientExecs = 0;
            double clientCost = 0;
            int clientCompleted = 0;

            for ( ResourceStats stats : statsList )
            {
                clientExecs += stats.getExecutionCount( );
                clientCost += stats.getTotalCost( );
                clientCompleted += (int) Math.round( stats.getExecutionCount( ) * stats.getSuccessRate( ) / 100.0 );
            }

            int clientErrors = clientExecs - clientCompleted;
            double clientSuccessRate = clientExecs > 0 ? ( clientCompleted * 100.0 / clientExecs ) : 0;

            List<DailyResourceStats> dailyStats = getDailyStatsByClient( client.getId( ), startDate, endDate );

            double clientDurationWeightedSum = 0;
            int clientDurationCount = 0;
            for ( DailyResourceStats ds : dailyStats )
            {
                if ( ds.getAvgDurationSeconds( ) > 0 && ds.getExecutionCount( ) > 0 )
                {
                    clientDurationWeightedSum += ds.getAvgDurationSeconds( ) * ds.getExecutionCount( );
                    clientDurationCount += ds.getExecutionCount( );
                }
            }
            double clientAvgDuration = clientDurationCount > 0 ? clientDurationWeightedSum / clientDurationCount : 0;

            totalExecutions += clientExecs;
            totalCost += clientCost;
            totalCompleted += clientCompleted;
            totalDurationWeightedSum += clientDurationWeightedSum;
            totalDurationCount += clientDurationCount;

            ClientObservabilityStats cStats = new ClientObservabilityStats( );
            cStats.setClient( client );
            cStats.setExecutionCount( clientExecs );
            cStats.setTotalCost( clientCost );
            cStats.setErrorCount( clientErrors );
            cStats.setSuccessRate( clientSuccessRate );
            cStats.setAvgDurationSeconds( clientAvgDuration );
            cStats.setDailySeries( dailyStats );
            clientStatsList.add( cStats );
        }

        padDailySeriesForAlignment( clientStatsList );

        int totalErrors = totalExecutions - totalCompleted;
        double globalSuccessRate = totalExecutions > 0 ? ( totalCompleted * 100.0 / totalExecutions ) : 0;
        double globalAvgDuration = totalDurationCount > 0 ? totalDurationWeightedSum / totalDurationCount : 0;

        GlobalObservabilityStats result = new GlobalObservabilityStats( );
        result.setTotalExecutions( totalExecutions );
        result.setTotalCost( totalCost );
        result.setTotalErrors( totalErrors );
        result.setGlobalSuccessRate( globalSuccessRate );
        result.setAvgDurationSeconds( globalAvgDuration );
        result.setClientStats( clientStatsList );
        return result;
    }

    /**
     * Builds the per-resource observability summary over the given date range: the matching executions plus the aggregated KPI block (total executions, total
     * cost, success rate, total errors, average duration in seconds). Replaces the inline accumulator loop that used to live in the controller.
     *
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource identifier
     * @param clientId
     *            the client identifier (use 0 or negative to skip filtering)
     * @param range
     *            the date range window
     * @return the typed per-resource summary
     */
    public ResourceObservabilitySummary getResourceSummary( String resourceType, String resourceId, int clientId, DateRange range )
    {
        List<PlatformResourceExecution> executions = getResourceExecutions( resourceType, resourceId, clientId, range );

        int totalExecutions = executions.size( );
        int totalErrors = 0;
        int totalCompleted = 0;
        double totalCost = 0.0;
        long totalDurationMs = 0;
        int durationCount = 0;

        for ( PlatformResourceExecution exec : executions )
        {
            if ( exec.getTotalCost( ) != null )
            {
                totalCost += exec.getTotalCost( ).doubleValue( );
            }
            if ( PlatformResourceExecutionStatus.ERROR.equals( exec.getStatus( ) ) )
            {
                totalErrors++;
            }
            if ( PlatformResourceExecutionStatus.COMPLETED.equals( exec.getStatus( ) ) )
            {
                totalCompleted++;
            }
            if ( exec.getStartTime( ) != null && exec.getEndTime( ) != null )
            {
                totalDurationMs += ( exec.getEndTime( ).getTime( ) - exec.getStartTime( ).getTime( ) );
                durationCount++;
            }
        }

        ResourceObservabilitySummary summary = new ResourceObservabilitySummary( );
        summary.setExecutions( executions );
        summary.setTotalExecutions( totalExecutions );
        summary.setTotalCost( totalCost );
        summary.setTotalErrors( totalErrors );
        summary.setSuccessRate( totalExecutions > 0 ? ( totalCompleted * 100.0 / totalExecutions ) : 0.0 );
        summary.setAvgDurationSeconds( durationCount > 0 ? ( totalDurationMs / 1000.0 / durationCount ) : 0.0 );
        return summary;
    }

    /**
     * Retrieves the executions of a resource over the given date range, dispatching to the all-time query for the ALL range and to the date-ranged query
     * otherwise. Resolves the date window once via {@link DateRange#resolveBounds}.
     *
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource identifier
     * @param clientId
     *            the client identifier (use 0 or negative to skip filtering)
     * @param range
     *            the date range window
     * @return the matching resource executions
     */
    public List<PlatformResourceExecution> getResourceExecutions( String resourceType, String resourceId, int clientId, DateRange range )
    {
        if ( range == DateRange.ALL )
        {
            return getResourceExecutions( resourceType, resourceId, clientId );
        }
        Timestamp [ ] bounds = range.resolveBounds( );
        return getResourceExecutionsByDateRange( resourceType, resourceId, clientId, bounds [0], bounds [1] );
    }

    /**
     * Builds the typed daily chart series for a resource over the given date range. Resolves the bounds from the range and projects each
     * {@link DailyResourceStats} to a {@link DailyStatPoint}, owning the chart-payload contract so the controller only serializes the result.
     *
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource identifier
     * @param range
     *            the date range window
     * @return the typed daily chart points
     */
    public List<DailyStatPoint> getDailyStatPoints( String resourceType, String resourceId, DateRange range )
    {
        Timestamp [ ] bounds = range.resolveBounds( );
        List<DailyResourceStats> dailyStats = getDailyStatsByResource( resourceType, resourceId, bounds [0], bounds [1] );

        return dailyStats.stream( )
                .map( s -> new DailyStatPoint( s.getDay( ), s.getExecutionCount( ), s.getTotalCost( ), s.getAvgDurationSeconds( ), s.getErrorCount( ) ) )
                .toList( );
    }

    /**
     * Resolves the resource entity backing an observability header by dispatching on the resource type to the matching Home. For models, back-fills the
     * provider association when missing. Returns the typed permission resource so the web layer can enrich it with RBAC flags and resolve its publication
     * status.
     *
     * @param resourceType
     *            the resource type identifier
     * @param resourceId
     *            the resource identifier
     * @return the resolved permission resource, or empty when the type is unknown or the entity does not exist
     */
    public Optional<AgentPermissionResource> resolveResourceEntity( String resourceType, int resourceId )
    {
        if ( resourceType == null )
        {
            return Optional.empty( );
        }
        switch( resourceType )
        {
            case Bot.RESOURCE_TYPE:
                return BotHome.findByPrimaryKey( resourceId ).map( b -> (AgentPermissionResource) b );
            case Pipeline.RESOURCE_TYPE:
                return PipelineHome.findByPrimaryKey( resourceId ).map( p -> (AgentPermissionResource) p );
            case Dataset.RESOURCE_TYPE:
                return DatasetHome.findByPrimaryKey( resourceId ).map( d -> (AgentPermissionResource) d );
            case Vision.RESOURCE_TYPE:
                return VisionHome.findByPrimaryKey( resourceId ).map( v -> (AgentPermissionResource) v );
            case Model.RESOURCE_TYPE:
                return ModelHome.findByPrimaryKey( resourceId ).map( this::hydrateModelProvider );
            case DecisionTree.RESOURCE_TYPE:
                return DecisionTreeHome.findByPrimaryKey( resourceId ).map( t -> (AgentPermissionResource) t );
            default:
                return Optional.empty( );
        }
    }

    /**
     * Back-fills the provider association of a model when it has not been loaded yet.
     *
     * @param model
     *            the model to hydrate
     * @return the same model, with its provider populated when available
     */
    private AgentPermissionResource hydrateModelProvider( Model model )
    {
        if ( model.getProvider( ) == null )
        {
            ProviderHome.findByPrimaryKey( model.getProviderId( ) ).ifPresent( model::setProvider );
        }
        return model;
    }

    /**
     * Calculates the total cost for a specific node execution by summing all trace costs.
     *
     * @param nodeExecutionId
     *            the identifier of the node execution
     * @return the total cost of all traces for the node execution
     */
    private BigDecimal calculateNodeTotalCost( int nodeExecutionId )
    {
        List<PlatformResourceNodeTrace> traces = PlatformResourceNodeTraceHome.getNodeTraces( nodeExecutionId );
        BigDecimal totalCost = BigDecimal.ZERO;

        for ( PlatformResourceNodeTrace trace : traces )
        {
            if ( trace.getCost( ) != null )
            {
                totalCost = totalCost.add( trace.getCost( ) );
            }
        }

        return totalCost;
    }

    /**
     * Updates the total cost of a node execution based on its traces. Writes only the cost column so it cannot race with a concurrent completion call that sets
     * status / end_time on the same node.
     *
     * @param nodeExecution
     *            the node execution to update
     */
    private void updateNodeTotalCost( PlatformResourceNodeExecution nodeExecution )
    {
        BigDecimal totalCost = calculateNodeTotalCost( nodeExecution.getId( ) );
        nodeExecution.setTotalCost( totalCost );
        PlatformResourceNodeExecutionHome.updateCost( nodeExecution.getId( ), totalCost );
    }

    /**
     * Calculates the total cost from all node executions within a resource execution.
     *
     * @param executionId
     *            the unique identifier of the resource execution
     * @return the total cost of all node executions
     */
    private BigDecimal calculateTotalCostFromNodes( String executionId )
    {
        List<PlatformResourceNodeExecution> nodeExecutions = PlatformResourceNodeExecutionHome.getNodeExecutions( executionId );
        BigDecimal totalCost = BigDecimal.ZERO;

        for ( PlatformResourceNodeExecution nodeExecution : nodeExecutions )
        {
            if ( nodeExecution.getTotalCost( ) != null )
            {
                totalCost = totalCost.add( nodeExecution.getTotalCost( ) );
            }
        }

        return totalCost;
    }

    /**
     * Updates the total cost of a resource execution based on its node executions. Writes only the cost column to avoid racing with concurrent completion calls
     * that set status / end_time — a full-row update would reload the RUNNING snapshot and silently revert a just-committed COMPLETED state, leaving executions
     * stuck in RUNNING forever.
     *
     * @param executionId
     *            the unique identifier of the resource execution
     */
    private void updateResourceExecutionCost( String executionId )
    {
        BigDecimal totalCost = calculateTotalCostFromNodes( executionId );
        PlatformResourceExecutionHome.updateCost( executionId, totalCost );
    }

    /**
     * Enriches an execution with human-readable names using provided caches.
     *
     * @param execution
     *            the execution to enrich
     * @param resourceTypesMap
     *            a map of resource type identifiers to their type objects
     * @param resourcesByType
     *            a cache map of resources organized by type
     */
    private void enrichExecutionWithNames( PlatformResourceExecution execution, Map<String, IPlatformResourceType> resourceTypesMap,
            Map<String, Map<String, PlatformResourceItem>> resourcesByType )
    {
        String resourceType = execution.getResourceType( );
        String resourceId = execution.getResourceId( );

        if ( resourceType != null )
        {
            IPlatformResourceType type = resourceTypesMap.get( resourceType );
            execution
                    .setResourceTypeName( type != null ? I18nService.getLocalizedString( type.getNameKey( ), I18nService.getDefaultLocale( ) ) : resourceType );

            if ( resourceId != null && type != null )
            {
                execution.setResourceName( getResourceName( type, resourceId, resourcesByType ) );
            }
        }
    }

    /**
     * Retrieves the human-readable name of a resource from the cache or fetches it.
     *
     * @param resourceType
     *            the type of the resource
     * @param resourceId
     *            the unique identifier of the resource
     * @param resourcesByType
     *            a cache map of resources organized by type
     * @return the resource name, or the resource ID if not found
     */
    private String getResourceName( IPlatformResourceType resourceType, String resourceId, Map<String, Map<String, PlatformResourceItem>> resourcesByType )
    {
        try
        {
            Map<String, PlatformResourceItem> resourcesMap = resourcesByType.get( resourceType.getResourceType( ) );

            if ( resourcesMap == null )
            {
                List<PlatformResourceItem> resources = resourceType.getResourceList( );
                resourcesMap = new HashMap<>( );

                for ( PlatformResourceItem resource : resources )
                {
                    resourcesMap.put( String.valueOf( resource.getId( ) ), resource );
                }

                resourcesByType.put( resourceType.getResourceType( ), resourcesMap );
            }

            PlatformResourceItem resource = resourcesMap.get( resourceId );

            if ( resource != null )
            {
                return resource.getName( );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_PARSING_RESOURCE_ID, resourceId, e );
        }

        return resourceId;
    }
}
