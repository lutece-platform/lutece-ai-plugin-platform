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

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineUserRateLimit;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineUserRateLimitHome;
import fr.paris.lutece.plugins.platform.service.security.AbstractUserRateLimitService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Per-user daily rate limiting of pipeline executions. The check/reset/atomic-increment algorithm lives in {@link AbstractUserRateLimitService}; this class
 * binds it to the pipeline storage.
 */
@ApplicationScoped
@Named( "platform.pipelineRateLimitService" )
public class PipelineRateLimitService extends AbstractUserRateLimitService<PipelineUserRateLimit>
{
    private static final String PIPELINE_NOT_FOUND_MESSAGE = "Pipeline not found";
    private static final String RATE_LIMIT_ERROR_MESSAGE = "Vous avez atteint la limite de %d exécutions par jour pour ce pipeline. Votre quota sera réinitialisé le %s";
    private static final int INITIAL_EXECUTION_COUNT = 1;

    @Inject
    private SubscriptionService _subscriptionService;

    /**
     * Default constructor for CDI.
     */
    PipelineRateLimitService( )
    {
    }

    /**
     * Retrieves the list of user rate limits for a specific user, restricted to the pipelines the client is subscribed to.
     *
     * @param userId
     *            the user identifier
     * @param client
     *            the client context
     * @return List of PipelineUserRateLimit for the specified user
     */
    public List<PipelineUserRateLimit> getUserRateLimits( String userId, Client client )
    {
        try
        {
            List<Pipeline> pipelines = PipelineHome.findByClientId( client.getId( ) );
            if ( pipelines.isEmpty( ) )
            {
                return new ArrayList<>( );
            }
            List<Integer> authorizedPipelineIds = pipelines.stream( ).filter(
                    pipeline -> _subscriptionService.hasActiveSubscription( client.getId( ), Pipeline.RESOURCE_TYPE, String.valueOf( pipeline.getId( ) ) ) )
                    .map( Pipeline::getId ).toList( );

            if ( authorizedPipelineIds.isEmpty( ) )
            {
                return new ArrayList<>( );
            }

            return PipelineUserRateLimitHome.findByUserIdAndPipelineIds( userId, authorizedPipelineIds );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error while retrieving user rate limits", e );
            return new ArrayList<>( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OptionalInt findDailyLimit( int resourceId )
    {
        return PipelineHome.findByPrimaryKey( resourceId ).map( pipeline -> OptionalInt.of( pipeline.getRateLimitByUserByDay( ) ) )
                .orElse( OptionalInt.empty( ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<PipelineUserRateLimit> findEntry( String userId, int resourceId )
    {
        return PipelineUserRateLimitHome.findByUserIdAndPipelineId( userId, resourceId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEntry( String userId, int resourceId, Timestamp now )
    {
        PipelineUserRateLimit rateLimit = new PipelineUserRateLimit( );
        rateLimit.setUserId( userId );
        rateLimit.setPipelineId( resourceId );
        rateLimit.setExecutionCount( INITIAL_EXECUTION_COUNT );
        rateLimit.setDateFirstExecution( now );
        PipelineUserRateLimitHome.create( rateLimit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Timestamp windowStart( PipelineUserRateLimit entry )
    {
        return entry.getDateFirstExecution( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetEntry( PipelineUserRateLimit entry, Timestamp now )
    {
        entry.setExecutionCount( INITIAL_EXECUTION_COUNT );
        entry.setDateFirstExecution( now );
        PipelineUserRateLimitHome.update( entry );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean incrementIfBelow( PipelineUserRateLimit entry, int dailyLimit )
    {
        return PipelineUserRateLimitHome.incrementIfBelow( entry.getId( ), dailyLimit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void removeEntriesBefore( Timestamp cutoff )
    {
        PipelineUserRateLimitHome.removeExpiredEntries( cutoff );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String resourceNotFoundMessage( )
    {
        return PIPELINE_NOT_FOUND_MESSAGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String exceededMessageTemplate( )
    {
        return RATE_LIMIT_ERROR_MESSAGE;
    }
}
