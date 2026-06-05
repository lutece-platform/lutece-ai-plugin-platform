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
package fr.paris.lutece.plugins.platform.business.vision;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import fr.paris.lutece.util.ReferenceList;

@ApplicationScoped
@Named( "platform.visionDAO" )
public class VisionDAO implements IVisionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_vision ( vision_title, vision_description, provider_id, client_id ) VALUES ( ?, ?, ?, ? )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_vision WHERE vision_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_vision SET vision_title = ?, vision_description = ?, provider_id = ?, client_id = ?, updated_at = CURRENT_TIMESTAMP WHERE vision_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT vision_id, vision_title, vision_description, provider_id, client_id, created_at, updated_at FROM platform_vision";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE vision_id = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE client_id = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT vision_id, vision_title FROM platform_vision ORDER BY vision_title";

    @Override
    public void insert( Vision vision, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, vision.getVisionTitle( ) );
            daoUtil.setString( nIndex++, vision.getVisionDescription( ) );
            daoUtil.setInt( nIndex++, vision.getProviderId( ) );
            daoUtil.setInt( nIndex, vision.getClientId( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                vision.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public Optional<Vision> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Vision vision = null;
            if ( daoUtil.next( ) )
            {
                vision = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( vision );
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
    public void store( Vision vision, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, vision.getVisionTitle( ) );
            daoUtil.setString( nIndex++, vision.getVisionDescription( ) );
            daoUtil.setInt( nIndex++, vision.getProviderId( ) );
            daoUtil.setInt( nIndex++, vision.getClientId( ) );
            daoUtil.setInt( nIndex, vision.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public List<Vision> selectVisionsList( Plugin plugin )
    {
        List<Vision> visionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                Vision vision = loadFromDaoUtil( daoUtil );
                visionList.add( vision );
            }
            return visionList;
        }
    }

    @Override
    public List<Vision> selectVisionsListByClientId( int clientId, Plugin plugin )
    {
        List<Vision> visionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                Vision vision = loadFromDaoUtil( daoUtil );
                visionList.add( vision );
            }
            return visionList;
        }
    }

    @Override
    public ReferenceList selectVisionsReferenceList( Plugin plugin )
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

    /**
     * Builds a Vision instance from the current row of the given DAOUtil.
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to read
     * @return the populated Vision instance
     */
    private Vision loadFromDaoUtil( DAOUtil daoUtil )
    {
        Vision vision = new Vision( );
        int nIndex = 1;
        vision.setId( daoUtil.getInt( nIndex++ ) );
        vision.setVisionTitle( daoUtil.getString( nIndex++ ) );
        vision.setVisionDescription( daoUtil.getString( nIndex++ ) );
        vision.setProviderId( daoUtil.getInt( nIndex++ ) );
        vision.setClientId( daoUtil.getInt( nIndex++ ) );
        vision.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        vision.setUpdatedAt( daoUtil.getTimestamp( nIndex ) );
        return vision;
    }
}
