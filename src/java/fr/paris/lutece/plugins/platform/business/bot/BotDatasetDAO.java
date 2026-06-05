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

import java.util.ArrayList;
import java.util.List;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of the IBotDatasetDAO interface
 */
@ApplicationScoped
@Named( "platform.botDatasetDAO" )
public class BotDatasetDAO implements IBotDatasetDAO
{
    private static final String SQL_QUERY_ASSOCIATE = "INSERT INTO platform_bot_dataset (bot_id, dataset_id) VALUES (?, ?)";
    private static final String SQL_QUERY_SELECT_DATASETS_BY_BOT = "SELECT dataset_id FROM platform_bot_dataset WHERE bot_id = ?";
    private static final String SQL_QUERY_SELECT_BOTS_BY_DATASET = "SELECT bot_id FROM platform_bot_dataset WHERE dataset_id = ?";
    private static final String SQL_QUERY_DELETE_BY_BOT = "DELETE FROM platform_bot_dataset WHERE bot_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void associate( BotDataset botDataset, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_ASSOCIATE, plugin ) )
        {
            daoUtil.setInt( 1, botDataset.getBotId( ) );
            daoUtil.setInt( 2, botDataset.getDatasetId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> selectDatasetIdsByBotId( int nBotId, Plugin plugin )
    {
        List<Integer> datasetIdList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_DATASETS_BY_BOT, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                datasetIdList.add( daoUtil.getInt( 1 ) );
            }
            return datasetIdList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> selectBotIdsByDatasetId( int nDatasetId, Plugin plugin )
    {
        List<Integer> botIdList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BOTS_BY_DATASET, plugin ) )
        {
            daoUtil.setInt( 1, nDatasetId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                botIdList.add( daoUtil.getInt( 1 ) );
            }
            return botIdList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteByBotId( int nBotId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_BOT, plugin ) )
        {
            daoUtil.setInt( 1, nBotId );
            daoUtil.executeUpdate( );
        }
    }

}
