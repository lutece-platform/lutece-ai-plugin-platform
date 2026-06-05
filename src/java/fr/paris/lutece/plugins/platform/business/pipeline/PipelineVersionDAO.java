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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Implementation of IPipelineVersionDAO
 */
@ApplicationScoped
@Named( "platform.pipelineVersionDAO" )
public class PipelineVersionDAO implements IPipelineVersionDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_pipeline_version (id_pipeline, flow, version_name, description, date_creation, is_current, input_schema) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_pipeline_version WHERE id_version = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_pipeline_version SET id_pipeline = ?, flow = ?, version_name = ?, description = ?, date_creation = ?, is_current = ?, input_schema = ? WHERE id_version = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT id_version, id_pipeline, flow, version_name, description, date_creation, is_current, input_schema FROM platform_pipeline_version";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE id_version = ?";
    private static final String SQL_QUERY_SELECT_BY_PIPELINE_ID = SQL_QUERY_SELECTALL + " WHERE id_pipeline = ? ORDER BY date_creation DESC";
    private static final String SQL_QUERY_SELECT_CURRENT_VERSION = SQL_QUERY_SELECTALL + " WHERE id_pipeline = ? AND is_current = 1";
    private static final String SQL_QUERY_RESET_CURRENT_VERSION = "UPDATE platform_pipeline_version SET is_current = 0 WHERE id_pipeline = ?";
    private static final String SQL_QUERY_SET_CURRENT_VERSION = "UPDATE platform_pipeline_version SET is_current = 1 WHERE id_version = ?";

    @Override
    public int insert( PipelineVersion version, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, version.getIdPipeline( ) );
            daoUtil.setString( nIndex++, version.getFlow( ) );
            daoUtil.setString( nIndex++, version.getVersionName( ) );
            daoUtil.setString( nIndex++, version.getDescription( ) );
            daoUtil.setTimestamp( nIndex++, version.getCreationDate( ) );
            daoUtil.setBoolean( nIndex++, version.isCurrent( ) );
            daoUtil.setString( nIndex++, version.getInputSchema( ) );
            daoUtil.executeUpdate( );

            int generatedKey = 0;
            if ( daoUtil.nextGeneratedKey( ) )
            {
                generatedKey = daoUtil.getGeneratedKeyInt( 1 );
                version.setId( generatedKey );
            }
            return generatedKey;
        }
    }

    @Override
    public void store( PipelineVersion version, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, version.getIdPipeline( ) );
            daoUtil.setString( nIndex++, version.getFlow( ) );
            daoUtil.setString( nIndex++, version.getVersionName( ) );
            daoUtil.setString( nIndex++, version.getDescription( ) );
            daoUtil.setTimestamp( nIndex++, version.getCreationDate( ) );
            daoUtil.setBoolean( nIndex++, version.isCurrent( ) );
            daoUtil.setString( nIndex++, version.getInputSchema( ) );
            daoUtil.setInt( nIndex, version.getId( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void delete( int nVersionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nVersionId );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public Optional<PipelineVersion> load( int nVersionId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nVersionId );
            daoUtil.executeQuery( );
            PipelineVersion version = null;
            if ( daoUtil.next( ) )
            {
                version = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( version );
        }
    }

    @Override
    public List<PipelineVersion> selectAll( Plugin plugin )
    {
        List<PipelineVersion> versionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                versionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return versionList;
        }
    }

    @Override
    public List<PipelineVersion> selectByPipelineId( int nPipelineId, Plugin plugin )
    {
        List<PipelineVersion> versionList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_PIPELINE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                versionList.add( loadFromDaoUtil( daoUtil ) );
            }
            return versionList;
        }
    }

    @Override
    public Optional<PipelineVersion> loadCurrentVersion( int nPipelineId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_CURRENT_VERSION, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.executeQuery( );
            PipelineVersion version = null;
            if ( daoUtil.next( ) )
            {
                version = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( version );
        }
    }

    @Override
    public void setCurrentVersion( int nVersionId, int nPipelineId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_RESET_CURRENT_VERSION, plugin ) )
        {
            daoUtil.setInt( 1, nPipelineId );
            daoUtil.executeUpdate( );
        }

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SET_CURRENT_VERSION, plugin ) )
        {
            daoUtil.setInt( 1, nVersionId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Loads a PipelineVersion from DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil
     * @return The PipelineVersion object
     */
    private PipelineVersion loadFromDaoUtil( DAOUtil daoUtil )
    {
        PipelineVersion version = new PipelineVersion( );
        int nIndex = 1;
        version.setId( daoUtil.getInt( nIndex++ ) );
        version.setIdPipeline( daoUtil.getInt( nIndex++ ) );
        version.setFlow( daoUtil.getString( nIndex++ ) );
        version.setVersionName( daoUtil.getString( nIndex++ ) );
        version.setDescription( daoUtil.getString( nIndex++ ) );
        version.setCreationDate( daoUtil.getTimestamp( nIndex++ ) );
        version.setCurrent( daoUtil.getBoolean( nIndex++ ) );
        version.setInputSchema( daoUtil.getString( nIndex ) );
        return version;
    }
}
