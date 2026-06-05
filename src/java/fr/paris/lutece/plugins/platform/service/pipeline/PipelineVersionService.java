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
package fr.paris.lutece.plugins.platform.service.pipeline;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecution;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineExecutionHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersionHome;
import fr.paris.lutece.plugins.platform.service.exception.CannotRemoveCurrentVersionException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Service encapsulating all non-runtime pipeline data operations: Pipeline and PipelineVersion lookups, version updates (including current-version switching),
 * subscription-filtered listings and execution history queries. The runtime side (execute, cancel, stats) stays in PipelineService / IPipelineService.
 */
public final class PipelineVersionService
{
    private static final String PIPELINE_NOT_FOUND_MESSAGE = "Pipeline not found";
    private static final String VERSION_NOT_FOUND_MESSAGE = "Version not found";
    private static final String NO_CURRENT_VERSION_MESSAGE = "No current version found";
    private static final String VERSION_REQUIRED_MESSAGE = "Version required";
    private static final String USER_ID_REQUIRED_MESSAGE = "The user identifier is required for this operation.";
    private static final String INPUT_SCHEMA_SERIALIZATION_ERROR = "Error while serializing the inputSchema";
    private static final String INVALID_FLOW_MESSAGE = "Invalid flow: the JSON does not match a pipeline definition";

    private static final String VERSION_NAME_KEY = "versionName";
    private static final String DESCRIPTION_KEY = "description";
    private static final String FLOW_KEY = "flow";
    private static final String INPUT_SCHEMA_KEY = "inputSchema";
    private static final String CURRENT_KEY = "current";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );

    private static final double VERSION_INCREMENT = 0.1;
    private static final double DEFAULT_VERSION_NUMBER = 1.0;
    private static final String DEFAULT_VERSION_NAME = "1.0";
    private static final String DEFAULT_VERSION_DESCRIPTION = "Initial version";
    private static final String NEW_VERSION_DESCRIPTION_PREFIX = "New version created on ";
    private static final String DEFAULT_INITIAL_FLOW = "{\"nodes\":{\"node-start\":{\"id\":\"node-start\",\"type\":\"START\",\"data\":{\"nodeId\":\"node-start\"},\"x\":449,\"y\":544}},\"edges\":[],\"rootNodeId\":\"node-start\"}";

    /**
     * Private constructor for utility class.
     */
    private PipelineVersionService( )
    {
    }

    /**
     * Creates a pipeline and seeds its initial current version with the default flow. The pipeline and its first version are persisted in one orchestration.
     *
     * @param name
     *            The pipeline name
     * @param description
     *            The pipeline description
     * @param maxConcurrentWorkers
     *            The maximum number of concurrent workers
     * @param rateLimitByUserByDay
     *            The per-user daily rate limit
     * @param clientId
     *            The owning client identifier
     * @return The created pipeline with its identifier set
     */
    public static Pipeline createPipelineWithInitialVersion( String name, String description, int maxConcurrentWorkers, int rateLimitByUserByDay, int clientId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( name );
        pipeline.setDescription( description );
        pipeline.setMaxConcurrentWorkers( maxConcurrentWorkers );
        pipeline.setRateLimitByUserByDay( rateLimitByUserByDay );
        pipeline.setIdClient( clientId );

        int nPipelineId = PipelineHome.create( pipeline );
        pipeline.setId( nPipelineId );

        PipelineVersion version = new PipelineVersion( );
        version.setIdPipeline( nPipelineId );
        version.setVersionName( DEFAULT_VERSION_NAME );
        version.setDescription( DEFAULT_VERSION_DESCRIPTION );
        version.setFlow( DEFAULT_INITIAL_FLOW );
        version.setCreationDate( new Timestamp( new Date( ).getTime( ) ) );
        version.setCurrent( true );
        PipelineVersionHome.create( version );

        return pipeline;
    }

    /**
     * Creates a new non-current version duplicating the flow of the pipeline's current version (or the default flow when none exists). The new version is named
     * by incrementing the highest existing version number.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The created version with its identifier set
     */
    public static PipelineVersion createVersionFromCurrent( int pipelineId )
    {
        String flowToDuplicate = PipelineVersionHome.findCurrentVersion( pipelineId ).map( PipelineVersion::getFlow ).orElse( DEFAULT_INITIAL_FLOW );

        PipelineVersion version = new PipelineVersion( );
        version.setIdPipeline( pipelineId );
        version.setVersionName( computeNextVersionName( pipelineId ) );
        version.setDescription( NEW_VERSION_DESCRIPTION_PREFIX + new Date( ) );
        version.setFlow( flowToDuplicate );
        version.setCreationDate( new Timestamp( new Date( ).getTime( ) ) );
        version.setCurrent( false );
        int nVersionId = PipelineVersionHome.create( version );
        version.setId( nVersionId );

        return version;
    }

    /**
     * Computes the next version name for a pipeline by incrementing the highest existing version number, or returns the default version name when none exist.
     * Version names that are not parsable as a number are skipped.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The next version name
     */
    public static String computeNextVersionName( int pipelineId )
    {
        List<PipelineVersion> versions = PipelineVersionHome.findByPipelineId( pipelineId );
        if ( versions.isEmpty( ) )
        {
            return DEFAULT_VERSION_NAME;
        }

        double highestVersion = DEFAULT_VERSION_NUMBER;
        for ( PipelineVersion version : versions )
        {
            highestVersion = Math.max( highestVersion, parseVersionNumber( version.getVersionName( ), DEFAULT_VERSION_NUMBER ) );
        }
        return String.format( Locale.US, "%.1f", highestVersion + VERSION_INCREMENT );
    }

    /**
     * Parses a version name as a locale-tolerant decimal number, returning the fallback when the name is not a valid number.
     *
     * @param versionName
     *            The version name to parse
     * @param fallback
     *            The value returned when parsing fails
     * @return The parsed version number, or the fallback
     */
    private static double parseVersionNumber( String versionName, double fallback )
    {
        if ( versionName == null )
        {
            return fallback;
        }
        try
        {
            return Double.parseDouble( versionName.replace( ",", "." ) );
        }
        catch( NumberFormatException e )
        {
            return fallback;
        }
    }

    /**
     * Removes a pipeline version, enforcing the invariant that the current version cannot be removed.
     *
     * @param versionId
     *            The version identifier
     * @throws CannotRemoveCurrentVersionException
     *             if the version is the current version
     */
    public static void removeVersion( int versionId )
    {
        if ( PipelineVersionHome.findByPrimaryKey( versionId ).filter( PipelineVersion::isCurrent ).isPresent( ) )
        {
            throw new CannotRemoveCurrentVersionException( );
        }
        PipelineVersionHome.remove( versionId );
    }

    /**
     * Loads a pipeline by primary key.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The pipeline entity
     * @throws ResourceNotFoundException
     *             if the pipeline does not exist
     */
    public static Pipeline findPipeline( int pipelineId )
    {
        return PipelineHome.findByPrimaryKey( pipelineId ).orElseThrow( ( ) -> new ResourceNotFoundException( PIPELINE_NOT_FOUND_MESSAGE ) );
    }

    /**
     * Loads a pipeline version by primary key.
     *
     * @param versionId
     *            The version identifier
     * @return The version entity
     * @throws ResourceNotFoundException
     *             if the version does not exist
     */
    public static PipelineVersion findVersion( int versionId )
    {
        return PipelineVersionHome.findByPrimaryKey( versionId ).orElseThrow( ( ) -> new ResourceNotFoundException( VERSION_NOT_FOUND_MESSAGE ) );
    }

    /**
     * Resolves a version by pipeline identifier and version name.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @param versionName
     *            The version name (must be non-empty)
     * @return The version entity
     * @throws InvalidRequestException
     *             if the version name is missing
     * @throws ResourceNotFoundException
     *             if the pipeline does not exist
     * @throws ResourceNotFoundException
     *             if the version does not exist
     */
    public static PipelineVersion findVersionByName( int pipelineId, String versionName )
    {
        if ( versionName == null || versionName.isEmpty( ) )
        {
            throw new InvalidRequestException( VERSION_REQUIRED_MESSAGE );
        }
        findPipeline( pipelineId );
        return PipelineVersionHome.findByPipelineIdAndVersionName( pipelineId, versionName )
                .orElseThrow( ( ) -> new ResourceNotFoundException( VERSION_NOT_FOUND_MESSAGE ) );
    }

    /**
     * Resolves the current version of a pipeline.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The current version
     * @throws ResourceNotFoundException
     *             if the pipeline does not exist
     * @throws ResourceNotFoundException
     *             if no current version is set
     */
    public static PipelineVersion findCurrentVersion( int pipelineId )
    {
        findPipeline( pipelineId );
        return PipelineVersionHome.findCurrentVersion( pipelineId ).orElseThrow( ( ) -> new ResourceNotFoundException( NO_CURRENT_VERSION_MESSAGE ) );
    }

    /**
     * Lists all versions of a pipeline after validating the pipeline exists.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The list of versions (possibly empty)
     * @throws ResourceNotFoundException
     *             if the pipeline does not exist
     */
    public static List<PipelineVersion> listVersionsOfPipeline( int pipelineId )
    {
        findPipeline( pipelineId );
        return PipelineVersionHome.findByPipelineId( pipelineId );
    }

    /**
     * Lists executions of a pipeline restricted to a single user.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @param userId
     *            The user identifier (must be non-empty)
     * @return The matching executions
     * @throws InvalidRequestException
     *             if the user identifier is missing
     */
    public static List<PipelineExecution> listExecutionsByUserAndPipeline( int pipelineId, String userId )
    {
        if ( userId == null || userId.trim( ).isEmpty( ) )
        {
            throw new InvalidRequestException( USER_ID_REQUIRED_MESSAGE );
        }
        return PipelineExecutionHome.findByUserIdAndPipelineId( userId, pipelineId );
    }

    /**
     * Lists pipelines a client has at least an active subscription to.
     *
     * @param clientId
     *            The client identifier
     * @return The accessible pipelines
     */
    public static List<Pipeline> listAccessiblePipelines( int clientId )
    {
        SubscriptionService subscriptionService = CDI.current( ).select( SubscriptionService.class ).get( );
        return PipelineHome.findByClientId( clientId ).stream( )
                .filter( pipeline -> subscriptionService.hasActiveSubscription( clientId, Pipeline.RESOURCE_TYPE, String.valueOf( pipeline.getId( ) ) ) )
                .toList( );
    }

    /**
     * Updates a pipeline version from a partial payload. When the payload flags the version as current, the current-version flag is propagated through the DAO
     * so only one version of the pipeline remains current.
     *
     * @param versionId
     *            The version identifier
     * @param versionData
     *            The partial payload (recognized keys: versionName, description, flow, inputSchema, current)
     * @return The updated version
     * @throws ResourceNotFoundException
     *             if the version does not exist
     * @throws InvalidRequestException
     *             if the payload carries a flow that is not structurally valid JSON
     */
    public static PipelineVersion updateVersion( int versionId, Map<String, Object> versionData )
    {
        PipelineVersion version = findVersion( versionId );
        applyVersionPatch( version, versionData );
        PipelineVersionHome.update( version );
        return version;
    }

    /**
     * Applies the non-null fields of a partial payload on top of an existing version entity.
     *
     * @param version
     *            The entity to mutate
     * @param versionData
     *            The partial payload
     */
    private static void applyVersionPatch( PipelineVersion version, Map<String, Object> versionData )
    {
        if ( versionData == null )
        {
            return;
        }
        if ( versionData.containsKey( VERSION_NAME_KEY ) )
        {
            version.setVersionName( (String) versionData.get( VERSION_NAME_KEY ) );
        }
        if ( versionData.containsKey( DESCRIPTION_KEY ) )
        {
            version.setDescription( (String) versionData.get( DESCRIPTION_KEY ) );
        }
        if ( versionData.containsKey( FLOW_KEY ) )
        {
            String flow = (String) versionData.get( FLOW_KEY );
            if ( !CDI.current( ).select( IPipelineService.class ).get( ).validatePipelineFlow( flow ) )
            {
                throw new InvalidRequestException( INVALID_FLOW_MESSAGE );
            }
            version.setFlow( flow );
        }
        if ( versionData.containsKey( INPUT_SCHEMA_KEY ) )
        {
            version.setInputSchema( serializeInputSchema( versionData.get( INPUT_SCHEMA_KEY ) ) );
        }
        if ( versionData.containsKey( CURRENT_KEY ) )
        {
            boolean isCurrent = (boolean) versionData.get( CURRENT_KEY );
            if ( isCurrent && !version.isCurrent( ) )
            {
                PipelineVersionHome.setCurrentVersion( version.getId( ), version.getIdPipeline( ) );
            }
            version.setCurrent( isCurrent );
        }
    }

    /**
     * Serializes the input schema as JSON, returning null when the payload is null or serialization fails.
     *
     * @param inputSchema
     *            The raw payload (typically a Map or List from JSON deserialization)
     * @return The serialized JSON, or null on failure
     */
    private static String serializeInputSchema( Object inputSchema )
    {
        if ( inputSchema == null )
        {
            return null;
        }
        try
        {
            return OBJECT_MAPPER.writeValueAsString( inputSchema );
        }
        catch( Exception e )
        {
            AppLogService.error( INPUT_SCHEMA_SERIALIZATION_ERROR, e );
            return null;
        }
    }

    /**
     * Finds a pipeline without throwing; provided for callers that want to short-circuit when the pipeline does not exist.
     *
     * @param pipelineId
     *            The pipeline identifier
     * @return The pipeline if present
     */
    public static Optional<Pipeline> findPipelineOptional( int pipelineId )
    {
        return PipelineHome.findByPrimaryKey( pipelineId );
    }
}
