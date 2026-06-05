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
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of the IDecisionTreeDAO interface
 */
@ApplicationScoped
@Named( "platform.decisionTreeDAO" )
public class DecisionTreeDAO implements IDecisionTreeDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_decision_tree ( tree_name, tree_description, client_id, "
            + "welcome_message, end_message, logo_base64, created_at, updated_at ) " + "VALUES ( ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_decision_tree WHERE id_decision_tree = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_decision_tree SET tree_name = ?, tree_description = ?, client_id = ?, "
            + "welcome_message = ?, end_message = ?, logo_base64 = ?, updated_at = CURRENT_TIMESTAMP WHERE id_decision_tree = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT id_decision_tree, tree_name, tree_description, client_id, "
            + "welcome_message, end_message, logo_base64, created_at, updated_at FROM platform_decision_tree";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_decision_tree = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE client_id = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT id_decision_tree, tree_name FROM platform_decision_tree ORDER BY tree_name";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DecisionTree tree, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, tree.getTreeName( ) );
            daoUtil.setString( nIndex++, tree.getTreeDescription( ) );
            daoUtil.setInt( nIndex++, tree.getClientId( ) );
            daoUtil.setString( nIndex++, tree.getWelcomeMessage( ) );
            daoUtil.setString( nIndex++, tree.getEndMessage( ) );
            daoUtil.setString( nIndex++, tree.getLogoBase64( ) );

            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                tree.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DecisionTree> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            DecisionTree tree = null;
            if ( daoUtil.next( ) )
            {
                tree = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( tree );
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
    public void store( DecisionTree tree, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, tree.getTreeName( ) );
            daoUtil.setString( nIndex++, tree.getTreeDescription( ) );
            daoUtil.setInt( nIndex++, tree.getClientId( ) );
            daoUtil.setString( nIndex++, tree.getWelcomeMessage( ) );
            daoUtil.setString( nIndex++, tree.getEndMessage( ) );
            daoUtil.setString( nIndex++, tree.getLogoBase64( ) );
            daoUtil.setInt( nIndex, tree.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionTree> selectDecisionTreesList( Plugin plugin )
    {
        List<DecisionTree> treeList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                treeList.add( loadFromDaoUtil( daoUtil ) );
            }
            return treeList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionTree> selectDecisionTreesListByClientId( int clientId, Plugin plugin )
    {
        List<DecisionTree> treeList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                treeList.add( loadFromDaoUtil( daoUtil ) );
            }
            return treeList;
        }
    }

    /**
     * Loads a DecisionTree object from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object
     * @return The DecisionTree object
     */
    private DecisionTree loadFromDaoUtil( DAOUtil daoUtil )
    {
        DecisionTree tree = new DecisionTree( );
        int nIndex = 1;

        tree.setId( daoUtil.getInt( nIndex++ ) );
        tree.setTreeName( daoUtil.getString( nIndex++ ) );
        tree.setTreeDescription( daoUtil.getString( nIndex++ ) );
        tree.setClientId( daoUtil.getInt( nIndex++ ) );
        tree.setWelcomeMessage( daoUtil.getString( nIndex++ ) );
        tree.setEndMessage( daoUtil.getString( nIndex++ ) );
        tree.setLogoBase64( daoUtil.getString( nIndex++ ) );
        tree.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        tree.setUpdatedAt( daoUtil.getTimestamp( nIndex++ ) );

        return tree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList selectDecisionTreesReferenceList( Plugin plugin )
    {
        ReferenceList referenceList = new ReferenceList( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_REFERENCE_LIST, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                referenceList.addItem( daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
        }
        return referenceList;
    }
}
