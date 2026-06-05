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
package fr.paris.lutece.plugins.platform.business.bot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides Data Access methods for BotPipeline objects
 */
@ApplicationScoped
@Named( "platform.botPipelineDAO" )
public class BotPipelineDAO implements IBotPipelineDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_bot_pipeline ( id_bot, id_pipeline, tool_description ) VALUES ( ?, ?, ? ) ";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_bot_pipeline WHERE id = ? ";
    private static final String SQL_QUERY_DELETE_BY_BOT_ID = "DELETE FROM platform_bot_pipeline WHERE id_bot = ? ";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_bot_pipeline SET id_bot = ?, id_pipeline = ?, tool_description = ? WHERE id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT id, id_bot, id_pipeline, tool_description FROM platform_bot_pipeline WHERE id = ?";
    private static final String SQL_QUERY_SELECT_BY_BOT_ID = "SELECT id, id_bot, id_pipeline, tool_description FROM platform_bot_pipeline WHERE id_bot = ?";
    private static final String SQL_QUERY_SELECT_PIPELINE_IDS_BY_BOT_ID = "SELECT id_pipeline FROM platform_bot_pipeline WHERE id_bot = ?";

    /**
     * {@inheritDoc }
     */
    @Override
    public void insert( BotPipeline botPipeline, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, botPipeline.getBotId( ) );
            daoUtil.setInt( nIndex++, botPipeline.getPipelineId( ) );
            daoUtil.setString( nIndex++, botPipeline.getToolDescription( ) );

            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                botPipeline.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BotPipeline load( int nKey, Plugin plugin )
    {
        BotPipeline botPipeline = null;
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) )
            {
                botPipeline = new BotPipeline( );
                int nIndex = 1;

                botPipeline.setId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setBotId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setPipelineId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setToolDescription( daoUtil.getString( nIndex++ ) );
            }
        }
        return botPipeline;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void delete( int nId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deleteByBotId( int nBotId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_BOT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void store( BotPipeline botPipeline, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;

            daoUtil.setInt( nIndex++, botPipeline.getBotId( ) );
            daoUtil.setInt( nIndex++, botPipeline.getPipelineId( ) );
            daoUtil.setString( nIndex++, botPipeline.getToolDescription( ) );
            daoUtil.setInt( nIndex, botPipeline.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<BotPipeline> selectBotPipelinesByBotId( int nBotId, Plugin plugin )
    {
        List<BotPipeline> botPipelineList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_BOT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                BotPipeline botPipeline = new BotPipeline( );
                int nIndex = 1;

                botPipeline.setId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setBotId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setPipelineId( daoUtil.getInt( nIndex++ ) );
                botPipeline.setToolDescription( daoUtil.getString( nIndex++ ) );

                botPipelineList.add( botPipeline );
            }
        }
        return botPipelineList;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Integer> selectPipelineIdsByBotId( int nBotId, Plugin plugin )
    {
        List<Integer> pipelineIdList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_PIPELINE_IDS_BY_BOT_ID, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                pipelineIdList.add( daoUtil.getInt( 1 ) );
            }
        }
        return pipelineIdList;
    }
}
