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
package fr.paris.lutece.plugins.platform.business.pipeline;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * DAO implementation for PipelineUserRateLimit
 */
@ApplicationScoped
@Named( "platform.pipelineUserRateLimitDAO" )
public class PipelineUserRateLimitDAO implements IPipelineUserRateLimitDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_pipeline_user_rate_limit (user_id, pipeline_id, execution_count, date_first_execution) "
            + "VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_pipeline_user_rate_limit SET user_id = ?, pipeline_id = ?, execution_count = ?, date_first_execution = ? "
            + "WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, user_id, pipeline_id, execution_count, date_first_execution "
            + "FROM platform_pipeline_user_rate_limit WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_USER_PIPELINE = "SELECT id, user_id, pipeline_id, execution_count, date_first_execution "
            + "FROM platform_pipeline_user_rate_limit WHERE user_id = ? AND pipeline_id = ?";
    private static final String SQL_QUERY_INCREMENT_IF_BELOW = "UPDATE platform_pipeline_user_rate_limit SET execution_count = execution_count + 1 "
            + "WHERE id = ? AND execution_count < ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_pipeline_user_rate_limit WHERE id = ?";
    private static final String SQL_QUERY_DELETE_BY_DATE_BEFORE = "DELETE FROM platform_pipeline_user_rate_limit WHERE date_first_execution < ?";
    private static final String SQL_QUERY_SELECT_EXPIRED = "SELECT id, user_id, pipeline_id, execution_count, date_first_execution "
            + "FROM platform_pipeline_user_rate_limit WHERE date_first_execution < ?";
    private static final String SQL_QUERY_SELECT_BY_USER_AND_PIPELINE_IDS_PREFIX = "SELECT id, user_id, pipeline_id, execution_count, date_first_execution FROM platform_pipeline_user_rate_limit WHERE user_id = ? AND pipeline_id IN (";

    private static final int PARAM_INDEX_START = 1;
    private static final int GENERATED_KEY_INDEX = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert( PipelineUserRateLimit rateLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            setInsertParameters( daoUtil, rateLimit );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                rateLimit.setId( daoUtil.getGeneratedKeyInt( GENERATED_KEY_INDEX ) );
            }
            return rateLimit.getId( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( PipelineUserRateLimit rateLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            setUpdateParameters( daoUtil, rateLimit );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean incrementIfBelow( int nId, int nLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INCREMENT_IF_BELOW, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.setInt( 2, nLimit );
            daoUtil.executeUpdate( );
            return daoUtil.getReturnedRowCount( ) == 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PipelineUserRateLimit> load( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( PARAM_INDEX_START, nId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil ) );
            }
            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PipelineUserRateLimit> findByUserIdAndPipelineId( String userId, int pipelineId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_USER_PIPELINE, plugin ) )
        {
            daoUtil.setString( PARAM_INDEX_START, userId );
            daoUtil.setInt( PARAM_INDEX_START + 1, pipelineId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                return Optional.of( loadFromDaoUtil( daoUtil ) );
            }
            return Optional.empty( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( PARAM_INDEX_START, nId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByDateBefore( Timestamp date, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_DATE_BEFORE, plugin ) )
        {
            daoUtil.setTimestamp( PARAM_INDEX_START, date );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineUserRateLimit> selectExpiredEntries( Timestamp date, Plugin plugin )
    {
        List<PipelineUserRateLimit> rateLimitList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_EXPIRED, plugin ) )
        {
            daoUtil.setTimestamp( PARAM_INDEX_START, date );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                rateLimitList.add( loadFromDaoUtil( daoUtil ) );
            }
            return rateLimitList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineUserRateLimit> findByUserIdAndPipelineIds( String userId, List<Integer> pipelineIds, Plugin plugin )
    {
        if ( pipelineIds == null || pipelineIds.isEmpty( ) )
        {
            return new ArrayList<>( );
        }

        StringBuilder queryBuilder = new StringBuilder( SQL_QUERY_SELECT_BY_USER_AND_PIPELINE_IDS_PREFIX );
        for ( int i = 0; i < pipelineIds.size( ); i++ )
        {
            if ( i > 0 )
            {
                queryBuilder.append( ", " );
            }
            queryBuilder.append( "?" );
        }
        queryBuilder.append( ")" );

        List<PipelineUserRateLimit> rateLimitList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( queryBuilder.toString( ), plugin ) )
        {
            int nIndex = PARAM_INDEX_START;
            daoUtil.setString( nIndex++, userId );
            for ( Integer pipelineId : pipelineIds )
            {
                daoUtil.setInt( nIndex++, pipelineId );
            }
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                rateLimitList.add( loadFromDaoUtil( daoUtil ) );
            }
            return rateLimitList;
        }
    }

    /**
     * Sets parameters for insert operation
     *
     * @param daoUtil
     *            the DAO util
     * @param rateLimit
     *            the rate limit entity
     */
    private void setInsertParameters( DAOUtil daoUtil, PipelineUserRateLimit rateLimit )
    {
        int nIndex = PARAM_INDEX_START;
        daoUtil.setString( nIndex++, rateLimit.getUserId( ) );
        daoUtil.setInt( nIndex++, rateLimit.getPipelineId( ) );
        daoUtil.setInt( nIndex++, rateLimit.getExecutionCount( ) );
        daoUtil.setTimestamp( nIndex, rateLimit.getDateFirstExecution( ) );
    }

    /**
     * Sets parameters for update operation
     *
     * @param daoUtil
     *            the DAO util
     * @param rateLimit
     *            the rate limit entity
     */
    private void setUpdateParameters( DAOUtil daoUtil, PipelineUserRateLimit rateLimit )
    {
        int nIndex = PARAM_INDEX_START;
        daoUtil.setString( nIndex++, rateLimit.getUserId( ) );
        daoUtil.setInt( nIndex++, rateLimit.getPipelineId( ) );
        daoUtil.setInt( nIndex++, rateLimit.getExecutionCount( ) );
        daoUtil.setTimestamp( nIndex++, rateLimit.getDateFirstExecution( ) );
        daoUtil.setInt( nIndex, rateLimit.getId( ) );
    }

    /**
     * Loads a PipelineUserRateLimit from DAOUtil
     *
     * @param daoUtil
     *            the DAO util
     * @return the loaded PipelineUserRateLimit
     */
    private PipelineUserRateLimit loadFromDaoUtil( DAOUtil daoUtil )
    {
        PipelineUserRateLimit rateLimit = new PipelineUserRateLimit( );
        int nIndex = PARAM_INDEX_START;

        rateLimit.setId( daoUtil.getInt( nIndex++ ) );
        rateLimit.setUserId( daoUtil.getString( nIndex++ ) );
        rateLimit.setPipelineId( daoUtil.getInt( nIndex++ ) );
        rateLimit.setExecutionCount( daoUtil.getInt( nIndex++ ) );
        rateLimit.setDateFirstExecution( daoUtil.getTimestamp( nIndex ) );

        return rateLimit;
    }
}
