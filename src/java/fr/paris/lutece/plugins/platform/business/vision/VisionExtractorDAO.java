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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Named( "platform.visionExtractorDAO" )
public class VisionExtractorDAO implements IVisionExtractorDAO
{

    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_vision_extractor (vision_id, extractor_name, extractor_description) VALUES (?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_vision_extractor SET extractor_name = ?, extractor_description = ? WHERE extractor_id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_vision_extractor WHERE extractor_id = ?";
    private static final String SQL_QUERY_DELETE_BY_VISION_ID = "DELETE FROM platform_vision_extractor WHERE vision_id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT extractor_id, vision_id, extractor_name, extractor_description FROM platform_vision_extractor WHERE extractor_id = ?";
    private static final String SQL_QUERY_SELECT_BY_VISION_ID = "SELECT extractor_id, vision_id, extractor_name, extractor_description FROM platform_vision_extractor WHERE vision_id = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( VisionExtractor extractor, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, extractor.getVisionId( ) );
            daoUtil.setString( nIndex++, extractor.getExtractorName( ) );
            daoUtil.setString( nIndex, extractor.getExtractorDescription( ) );
            daoUtil.executeUpdate( );

            if ( daoUtil.nextGeneratedKey( ) )
            {
                extractor.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( VisionExtractor extractor, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, extractor.getExtractorName( ) );
            daoUtil.setString( nIndex++, extractor.getExtractorDescription( ) );
            daoUtil.setInt( nIndex, extractor.getId( ) );
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
    public void deleteByVisionId( int nVisionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_VISION_ID, plugin ) )
        {
            daoUtil.setInt( 1, nVisionId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<VisionExtractor> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );

            VisionExtractor extractor = null;
            if ( daoUtil.next( ) )
            {
                extractor = dataToExtractor( daoUtil );
            }
            return Optional.ofNullable( extractor );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisionExtractor> selectByVisionId( int nVisionId, Plugin plugin )
    {
        List<VisionExtractor> extractorList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_VISION_ID, plugin ) )
        {
            daoUtil.setInt( 1, nVisionId );
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                VisionExtractor extractor = dataToExtractor( daoUtil );
                extractorList.add( extractor );
            }
        }
        return extractorList;
    }

    /**
     * Maps database data to VisionExtractor object
     *
     * @param daoUtil
     *            the DAO utility containing result set data
     * @return the mapped VisionExtractor object
     */
    private VisionExtractor dataToExtractor( DAOUtil daoUtil )
    {
        VisionExtractor extractor = new VisionExtractor( );
        int nIndex = 1;
        extractor.setId( daoUtil.getInt( nIndex++ ) );
        extractor.setVisionId( daoUtil.getInt( nIndex++ ) );
        extractor.setExtractorName( daoUtil.getString( nIndex++ ) );
        extractor.setExtractorDescription( daoUtil.getString( nIndex ) );
        return extractor;
    }
}
