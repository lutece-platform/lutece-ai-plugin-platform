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
package fr.paris.lutece.plugins.platform.business.decisiontree;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of the IDecisionTransitionDAO interface
 */
@ApplicationScoped
@Named( "platform.decisionTransitionDAO" )
public class DecisionTransitionDAO implements IDecisionTransitionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_decision_transition ( source_node_id, target_node_id, label, sort_order, created_at, updated_at ) VALUES ( ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_decision_transition WHERE id_decision_transition = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_decision_transition SET source_node_id = ?, target_node_id = ?, label = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id_decision_transition = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT id_decision_transition, source_node_id, target_node_id, label, sort_order, created_at, updated_at FROM platform_decision_transition";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_decision_transition = ?";
    private static final String SQL_QUERY_SELECT_BY_SOURCE_NODE_ID = SQL_QUERY_SELECTALL + " WHERE source_node_id = ? ORDER BY sort_order ASC";
    private static final String SQL_QUERY_DELETE_BY_SOURCE_NODE_ID = "DELETE FROM platform_decision_transition WHERE source_node_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DecisionTransition transition, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, transition.getSourceNodeId( ) );
            daoUtil.setInt( nIndex++, transition.getTargetNodeId( ) );
            daoUtil.setString( nIndex++, transition.getLabel( ) );
            daoUtil.setInt( nIndex, transition.getSortOrder( ) );

            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                transition.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DecisionTransition> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            DecisionTransition transition = null;
            if ( daoUtil.next( ) )
            {
                transition = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( transition );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( DecisionTransition transition, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, transition.getSourceNodeId( ) );
            daoUtil.setInt( nIndex++, transition.getTargetNodeId( ) );
            daoUtil.setString( nIndex++, transition.getLabel( ) );
            daoUtil.setInt( nIndex++, transition.getSortOrder( ) );
            daoUtil.setInt( nIndex, transition.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionTransition> selectTransitionsBySourceNodeId( int nSourceNodeId, Plugin plugin )
    {
        List<DecisionTransition> transitionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_SOURCE_NODE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nSourceNodeId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                transitionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return transitionList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBySourceNodeId( int nSourceNodeId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_SOURCE_NODE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nSourceNodeId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Loads a DecisionTransition object from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object
     * @return The DecisionTransition object
     */
    private DecisionTransition loadFromDaoUtil( DAOUtil daoUtil )
    {
        DecisionTransition transition = new DecisionTransition( );
        int nIndex = 1;

        transition.setId( daoUtil.getInt( nIndex++ ) );
        transition.setSourceNodeId( daoUtil.getInt( nIndex++ ) );
        transition.setTargetNodeId( daoUtil.getInt( nIndex++ ) );
        transition.setLabel( daoUtil.getString( nIndex++ ) );
        transition.setSortOrder( daoUtil.getInt( nIndex++ ) );
        transition.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        transition.setUpdatedAt( daoUtil.getTimestamp( nIndex++ ) );

        return transition;
    }
}
