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
@Named( "platform.visionExtractorFieldDAO" )
public class VisionExtractorFieldDAO implements IVisionExtractorFieldDAO
{

    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_vision_extractor_field (extractor_id, field_name, field_description, field_type) VALUES (?, ?, ?, ?)";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_vision_extractor_field SET field_name = ?, field_description = ?, field_type = ? WHERE field_id = ?";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_vision_extractor_field WHERE field_id = ?";
    private static final String SQL_QUERY_DELETE_BY_EXTRACTOR_ID = "DELETE FROM platform_vision_extractor_field WHERE extractor_id = ?";
    private static final String SQL_QUERY_SELECT = "SELECT field_id, extractor_id, field_name, field_description, field_type FROM platform_vision_extractor_field WHERE field_id = ?";
    private static final String SQL_QUERY_SELECT_BY_EXTRACTOR_ID = "SELECT field_id, extractor_id, field_name, field_description, field_type FROM platform_vision_extractor_field WHERE extractor_id = ?";

    @Override
    public void insert( VisionExtractorField field, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, field.getExtractorId( ) );
            daoUtil.setString( nIndex++, field.getFieldName( ) );
            daoUtil.setString( nIndex++, field.getFieldDescription( ) );
            daoUtil.setString( nIndex, field.getFieldType( ).name( ) );
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                field.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    @Override
    public void store( VisionExtractorField field, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, field.getFieldName( ) );
            daoUtil.setString( nIndex++, field.getFieldDescription( ) );
            daoUtil.setString( nIndex++, field.getFieldType( ).name( ) );
            daoUtil.setInt( nIndex, field.getId( ) );
            daoUtil.executeUpdate( );
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
    public void deleteByExtractorId( int nExtractorId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_BY_EXTRACTOR_ID, plugin ) )
        {
            daoUtil.setInt( 1, nExtractorId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public Optional<VisionExtractorField> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            VisionExtractorField field = null;
            if ( daoUtil.next( ) )
            {
                field = dataToField( daoUtil );
            }
            return Optional.ofNullable( field );
        }
    }

    @Override
    public List<VisionExtractorField> selectByExtractorId( int nExtractorId, Plugin plugin )
    {
        List<VisionExtractorField> fieldList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_EXTRACTOR_ID, plugin ) )
        {
            daoUtil.setInt( 1, nExtractorId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                fieldList.add( dataToField( daoUtil ) );
            }
        }
        return fieldList;
    }

    /**
     * Builds a VisionExtractorField from the current row of the given DAOUtil
     *
     * @param daoUtil
     *            the DAOUtil positioned on the row to read
     * @return the VisionExtractorField mapped from the current row
     */
    private VisionExtractorField dataToField( DAOUtil daoUtil )
    {
        VisionExtractorField field = new VisionExtractorField( );
        int nIndex = 1;
        field.setId( daoUtil.getInt( nIndex++ ) );
        field.setExtractorId( daoUtil.getInt( nIndex++ ) );
        field.setFieldName( daoUtil.getString( nIndex++ ) );
        field.setFieldDescription( daoUtil.getString( nIndex++ ) );
        field.setFieldType( VisionFieldType.fromString( daoUtil.getString( nIndex ) ) );
        return field;
    }
}
