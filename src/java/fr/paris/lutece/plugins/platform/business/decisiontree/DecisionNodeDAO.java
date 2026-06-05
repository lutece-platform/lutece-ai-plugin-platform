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
 * Data Access Object implementation for DecisionNode objects
 */
@ApplicationScoped
@Named( "platform.decisionNodeDAO" )
public class DecisionNodeDAO implements IDecisionNodeDAO
{
    private static final String SQL_QUERY_SELECTALL = "SELECT id_decision_node, tree_id, node_title, show_back_button, back_target_node_id, content, created_at, updated_at FROM platform_decision_node";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_decision_node = ?";
    private static final String SQL_QUERY_SELECT_BY_TREE_ID = SQL_QUERY_SELECTALL + " WHERE tree_id = ? ORDER BY id_decision_node ASC";
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_decision_node ( tree_id, node_title, show_back_button, back_target_node_id, content ) VALUES ( ?, ?, ?, ?, ? )";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_decision_node SET tree_id = ?, node_title = ?, show_back_button = ?, back_target_node_id = ?, content = ?, updated_at = CURRENT_TIMESTAMP WHERE id_decision_node = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_decision_node WHERE id_decision_node = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DecisionNode node, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, node.getTreeId( ) );
            daoUtil.setString( nIndex++, node.getNodeTitle( ) );
            daoUtil.setBoolean( nIndex++, node.getShowBackButton( ) );
            setIntOrNull( daoUtil, nIndex++, node.getBackTargetNodeId( ) );
            daoUtil.setString( nIndex, node.getContent( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                node.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( DecisionNode node, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, node.getTreeId( ) );
            daoUtil.setString( nIndex++, node.getNodeTitle( ) );
            daoUtil.setBoolean( nIndex++, node.getShowBackButton( ) );
            setIntOrNull( daoUtil, nIndex++, node.getBackTargetNodeId( ) );
            daoUtil.setString( nIndex++, node.getContent( ) );
            daoUtil.setInt( nIndex, node.getId( ) );
            daoUtil.executeUpdate( );
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
    public Optional<DecisionNode> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            DecisionNode node = null;
            if ( daoUtil.next( ) )
            {
                node = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( node );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionNode> selectNodesByTreeId( int nTreeId, Plugin plugin )
    {
        List<DecisionNode> nodeList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_TREE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                nodeList.add( loadFromDaoUtil( daoUtil ) );
            }
            return nodeList;
        }
    }

    /**
     * Loads a decision node object from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object with the decision node data
     * @return The DecisionNode object
     */
    /**
     * Sets an integer parameter, or SQL NULL if the value is null.
     */
    private void setIntOrNull( DAOUtil daoUtil, int nIndex, Integer value )
    {
        if ( value != null )
        {
            daoUtil.setInt( nIndex, value );
        }
        else
        {
            daoUtil.setIntNull( nIndex );
        }
    }

    /**
     * Builds a {@link DecisionNode} from the current row of the given DAOUtil.
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to read
     * @return the decision node mapped from the current row
     */
    private DecisionNode loadFromDaoUtil( DAOUtil daoUtil )
    {
        DecisionNode node = new DecisionNode( );
        int nIndex = 1;
        node.setId( daoUtil.getInt( nIndex++ ) );
        node.setTreeId( daoUtil.getInt( nIndex++ ) );
        node.setNodeTitle( daoUtil.getString( nIndex++ ) );
        node.setShowBackButton( daoUtil.getBoolean( nIndex++ ) );
        int nBackTargetNodeId = daoUtil.getInt( nIndex++ );
        node.setBackTargetNodeId( nBackTargetNodeId == 0 ? null : nBackTargetNodeId );
        node.setContent( daoUtil.getString( nIndex++ ) );
        node.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        node.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return node;
    }
}
