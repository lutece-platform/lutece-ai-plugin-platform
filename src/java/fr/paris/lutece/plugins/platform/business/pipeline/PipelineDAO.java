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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * This class provides Data Access methods for Pipeline objects
 */
@ApplicationScoped
@Named( "platform.pipelineDAO" )
public class PipelineDAO implements IPipelineDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_pipeline (name, description, max_concurrent_workers, rate_limit_by_user_by_day, id_client) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_pipeline WHERE id_pipeline = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_pipeline SET name = ?, description = ?, max_concurrent_workers = ?, rate_limit_by_user_by_day = ?, id_client = ? WHERE id_pipeline = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT id_pipeline, name, description, max_concurrent_workers, rate_limit_by_user_by_day, id_client FROM platform_pipeline";
    private static final String SQL_QUERY_SELECTALL_ID = "SELECT id_pipeline FROM platform_pipeline";
    private static final String SQL_QUERY_SELECTALL_BY_IDS = SQL_QUERY_SELECTALL + " WHERE id_pipeline IN (";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_pipeline = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE id_client = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT id_pipeline, name FROM platform_pipeline ORDER BY name";

    @Override
    public int insert( Pipeline pipeline, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, pipeline.getName( ) );
            daoUtil.setString( nIndex++, pipeline.getDescription( ) );
            daoUtil.setInt( nIndex++, pipeline.getMaxConcurrentWorkers( ) );
            daoUtil.setInt( nIndex++, pipeline.getRateLimitByUserByDay( ) );
            daoUtil.setInt( nIndex++, pipeline.getIdClient( ) );
            daoUtil.executeUpdate( );

            int generatedKey = 0;
            if ( daoUtil.nextGeneratedKey( ) )
            {
                generatedKey = daoUtil.getGeneratedKeyInt( 1 );
                pipeline.setId( generatedKey );
            }
            return generatedKey;
        }
    }

    @Override
    public Optional<Pipeline> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );

            Pipeline pipeline = null;
            if ( daoUtil.next( ) )
            {
                pipeline = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( pipeline );
        }
    }

    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void store( Pipeline pipeline, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, pipeline.getName( ) );
            daoUtil.setString( nIndex++, pipeline.getDescription( ) );
            daoUtil.setInt( nIndex++, pipeline.getMaxConcurrentWorkers( ) );
            daoUtil.setInt( nIndex++, pipeline.getRateLimitByUserByDay( ) );
            daoUtil.setInt( nIndex++, pipeline.getIdClient( ) );
            daoUtil.setInt( nIndex, pipeline.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<Pipeline> selectAll( Plugin plugin )
    {
        List<Pipeline> pipelineList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                pipelineList.add( loadFromDaoUtil( daoUtil ) );
            }
            return pipelineList;
        }
    }

    @Override
    public List<Pipeline> selectByClientId( int nClientId, Plugin plugin )
    {
        List<Pipeline> pipelineList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nClientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                pipelineList.add( loadFromDaoUtil( daoUtil ) );
            }
            return pipelineList;
        }
    }

    @Override
    public ReferenceList selectReferenceList( Plugin plugin )
    {
        ReferenceList pipelineList = new ReferenceList( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_REFERENCE_LIST, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                pipelineList.addItem( daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
            return pipelineList;
        }
    }

    /**
     * Returns the list of all pipeline IDs
     *
     * @param plugin
     *            The plugin
     * @return The list of pipeline IDs
     */
    public List<Integer> selectIdPipelinesList( Plugin plugin )
    {
        List<Integer> pipelineList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL_ID, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                pipelineList.add( daoUtil.getInt( 1 ) );
            }
            return pipelineList;
        }
    }

    /**
     * Returns the list of pipelines matching the given IDs
     *
     * @param plugin
     *            The plugin
     * @param listIds
     *            The list of pipeline IDs to load
     * @return The list of pipelines matching the given IDs
     */
    public List<Pipeline> selectPipelinesListByIds( Plugin plugin, List<Integer> listIds )
    {
        List<Pipeline> pipelineList = new ArrayList<>( );
        StringBuilder builder = new StringBuilder( );
        if ( !listIds.isEmpty( ) )
        {
            for ( int i = 0; i < listIds.size( ); i++ )
            {
                builder.append( "?," );
            }
            String placeHolders = builder.deleteCharAt( builder.length( ) - 1 ).toString( );
            String stmt = SQL_QUERY_SELECTALL_BY_IDS + placeHolders + ")";
            try ( DAOUtil daoUtil = new DAOUtil( stmt, plugin ) )
            {
                int index = 1;
                for ( Integer n : listIds )
                {
                    daoUtil.setInt( index++, n );
                }
                daoUtil.executeQuery( );
                while ( daoUtil.next( ) )
                {
                    pipelineList.add( loadFromDaoUtil( daoUtil ) );
                }
            }
        }
        return pipelineList;
    }

    /**
     * Builds a pipeline from the current row of the given DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil positioned on the current row
     * @return The pipeline built from the current row
     */
    private Pipeline loadFromDaoUtil( DAOUtil daoUtil )
    {
        Pipeline pipeline = new Pipeline( );
        int nIndex = 1;
        pipeline.setId( daoUtil.getInt( nIndex++ ) );
        pipeline.setName( daoUtil.getString( nIndex++ ) );
        pipeline.setDescription( daoUtil.getString( nIndex++ ) );
        pipeline.setMaxConcurrentWorkers( daoUtil.getInt( nIndex++ ) );
        pipeline.setRateLimitByUserByDay( daoUtil.getInt( nIndex++ ) );
        pipeline.setIdClient( daoUtil.getInt( nIndex++ ) );
        return pipeline;
    }
}
