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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of {@link IDecisionTreeImageDAO}.
 */
@ApplicationScoped
@Named( "platform.decisionTreeImageDAO" )
public class DecisionTreeImageDAO implements IDecisionTreeImageDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_decision_tree_image ( content_hash, tree_id, file_store_key, mime_type, created_at ) VALUES ( ?, ?, ?, ?, CURRENT_TIMESTAMP )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_decision_tree_image WHERE content_hash = ? AND tree_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT content_hash, tree_id, file_store_key, mime_type, created_at FROM platform_decision_tree_image";
    private static final String SQL_QUERY_SELECT_BY_PK = SQL_QUERY_SELECTALL + " WHERE content_hash = ? AND tree_id = ?";
    private static final String SQL_QUERY_SELECT_BY_HASH = SQL_QUERY_SELECTALL + " WHERE content_hash = ?";
    private static final String SQL_QUERY_SELECT_BY_TREE = SQL_QUERY_SELECTALL + " WHERE tree_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( DecisionTreeImage image, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, image.getContentHash( ) );
            daoUtil.setInt( nIndex++, image.getTreeId( ) );
            daoUtil.setString( nIndex++, image.getFileStoreKey( ) );
            daoUtil.setString( nIndex++, image.getMimeType( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( String strContentHash, int nTreeId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setString( 1, strContentHash );
            daoUtil.setInt( 2, nTreeId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DecisionTreeImage> load( String strContentHash, int nTreeId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_PK, plugin ) )
        {
            daoUtil.setString( 1, strContentHash );
            daoUtil.setInt( 2, nTreeId );
            daoUtil.executeQuery( );
            DecisionTreeImage image = null;
            if ( daoUtil.next( ) )
            {
                image = mapRow( daoUtil );
            }
            return Optional.ofNullable( image );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionTreeImage> selectByContentHash( String strContentHash, Plugin plugin )
    {
        List<DecisionTreeImage> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_HASH, plugin ) )
        {
            daoUtil.setString( 1, strContentHash );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( mapRow( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DecisionTreeImage> selectByTreeId( int nTreeId, Plugin plugin )
    {
        List<DecisionTreeImage> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_TREE, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( mapRow( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * Maps the current DAOUtil row to a {@link DecisionTreeImage}.
     *
     * @param daoUtil
     *            the DAOUtil positioned on a row
     * @return the mapped entity
     */
    private DecisionTreeImage mapRow( DAOUtil daoUtil )
    {
        DecisionTreeImage image = new DecisionTreeImage( );
        int nIndex = 1;
        image.setContentHash( daoUtil.getString( nIndex++ ) );
        image.setTreeId( daoUtil.getInt( nIndex++ ) );
        image.setFileStoreKey( daoUtil.getString( nIndex++ ) );
        image.setMimeType( daoUtil.getString( nIndex++ ) );
        image.setDateCreation( daoUtil.getTimestamp( nIndex++ ) );
        return image;
    }
}
