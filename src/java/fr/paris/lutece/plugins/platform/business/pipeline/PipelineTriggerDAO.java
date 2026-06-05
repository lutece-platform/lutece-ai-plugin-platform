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
 * This class provides Data Access methods for PipelineTrigger objects
 */
@ApplicationScoped
@Named( "platform.pipelineTriggerDAO" )
public class PipelineTriggerDAO implements IPipelineTriggerDAO
{
    private static final String SQL_QUERY_SELECTALL = "SELECT id_trigger, id_pipeline, id_client, name, trigger_type, configuration, input_data, is_active, last_triggered_at, created_at FROM platform_pipeline_trigger";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_trigger = ?";
    private static final String SQL_QUERY_SELECT_BY_PIPELINE = SQL_QUERY_SELECTALL + " WHERE id_pipeline = ? ORDER BY created_at DESC";
    private static final String SQL_QUERY_SELECT_ACTIVE = SQL_QUERY_SELECTALL + " WHERE is_active = 1";
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_pipeline_trigger (id_pipeline, id_client, name, trigger_type, configuration, input_data, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_pipeline_trigger SET name = ?, trigger_type = ?, configuration = ?, input_data = ?, is_active = ? WHERE id_trigger = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_pipeline_trigger WHERE id_trigger = ?";
    private static final String SQL_QUERY_UPDATE_LAST_TRIGGERED = "UPDATE platform_pipeline_trigger SET last_triggered_at = ? WHERE id_trigger = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public int insert( PipelineTrigger trigger, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, trigger.getIdPipeline( ) );
            daoUtil.setInt( nIndex++, trigger.getIdClient( ) );
            daoUtil.setString( nIndex++, trigger.getName( ) );
            daoUtil.setString( nIndex++, trigger.getTriggerType( ) );
            daoUtil.setString( nIndex++, trigger.getConfiguration( ) );
            daoUtil.setString( nIndex++, trigger.getInputData( ) );
            daoUtil.setBoolean( nIndex++, trigger.isActive( ) );
            daoUtil.executeUpdate( );

            int generatedKey = 0;
            if ( daoUtil.nextGeneratedKey( ) )
            {
                generatedKey = daoUtil.getGeneratedKeyInt( 1 );
                trigger.setId( generatedKey );
            }
            return generatedKey;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( PipelineTrigger trigger, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, trigger.getName( ) );
            daoUtil.setString( nIndex++, trigger.getTriggerType( ) );
            daoUtil.setString( nIndex++, trigger.getConfiguration( ) );
            daoUtil.setString( nIndex++, trigger.getInputData( ) );
            daoUtil.setBoolean( nIndex++, trigger.isActive( ) );
            daoUtil.setInt( nIndex, trigger.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nTriggerId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nTriggerId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PipelineTrigger> load( int nTriggerId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nTriggerId );
            daoUtil.executeQuery( );

            PipelineTrigger trigger = null;
            if ( daoUtil.next( ) )
            {
                trigger = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( trigger );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineTrigger> selectByPipelineId( int nPipelineId, Plugin plugin )
    {
        List<PipelineTrigger> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_PIPELINE, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
            return list;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineTrigger> selectAllActive( Plugin plugin )
    {
        List<PipelineTrigger> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_ACTIVE, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( loadFromDaoUtil( daoUtil ) );
            }
            return list;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLastTriggeredAt( int nTriggerId, Timestamp timestamp, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE_LAST_TRIGGERED, plugin ) )
        {
            daoUtil.setTimestamp( 1, timestamp );
            daoUtil.setInt( 2, nTriggerId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Loads a PipelineTrigger from a DAOUtil result set.
     *
     * @param daoUtil
     *            The DAOUtil
     * @return The loaded trigger
     */
    private PipelineTrigger loadFromDaoUtil( DAOUtil daoUtil )
    {
        PipelineTrigger trigger = new PipelineTrigger( );
        int nIndex = 1;
        trigger.setId( daoUtil.getInt( nIndex++ ) );
        trigger.setIdPipeline( daoUtil.getInt( nIndex++ ) );
        trigger.setIdClient( daoUtil.getInt( nIndex++ ) );
        trigger.setName( daoUtil.getString( nIndex++ ) );
        trigger.setTriggerType( daoUtil.getString( nIndex++ ) );
        trigger.setConfiguration( daoUtil.getString( nIndex++ ) );
        trigger.setInputData( daoUtil.getString( nIndex++ ) );
        trigger.setActive( daoUtil.getBoolean( nIndex++ ) );
        trigger.setLastTriggeredAt( daoUtil.getTimestamp( nIndex++ ) );
        trigger.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        return trigger;
    }
}
