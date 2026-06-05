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

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Optional;

/**
 * This class provides instances management methods for PipelineVersion objects
 */
public final class PipelineVersionHome
{
    private static IPipelineVersionDAO _dao = CDI.current( ).select( IPipelineVersionDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PipelineVersionHome( )
    {
    }

    /**
     * Create a new version
     *
     * @param version
     *            The version object
     * @return The id of the newly created version
     */
    public static int create( PipelineVersion version )
    {
        return _dao.insert( version, _plugin );
    }

    /**
     * Update a version
     *
     * @param version
     *            The version object
     * @return The updated version
     */
    public static PipelineVersion update( PipelineVersion version )
    {
        _dao.store( version, _plugin );
        return version;
    }

    /**
     * Remove a version
     *
     * @param nVersionId
     *            The version id
     */
    public static void remove( int nVersionId )
    {
        _dao.delete( nVersionId, _plugin );
    }

    /**
     * Find a version by its primary key
     *
     * @param nVersionId
     *            The version id
     * @return The version object if found, empty otherwise
     */
    public static Optional<PipelineVersion> findByPrimaryKey( int nVersionId )
    {
        return _dao.load( nVersionId, _plugin );
    }

    /**
     * Find all versions
     *
     * @return The list of all versions
     */
    public static List<PipelineVersion> findAll( )
    {
        return _dao.selectAll( _plugin );
    }

    /**
     * Find all versions of a pipeline
     *
     * @param nPipelineId
     *            The pipeline id
     * @return The list of pipeline versions
     */
    public static List<PipelineVersion> findByPipelineId( int nPipelineId )
    {
        return _dao.selectByPipelineId( nPipelineId, _plugin );
    }

    /**
     * Find the current version of a pipeline
     *
     * @param nPipelineId
     *            The pipeline id
     * @return The current version if found, empty otherwise
     */
    public static Optional<PipelineVersion> findCurrentVersion( int nPipelineId )
    {
        return _dao.loadCurrentVersion( nPipelineId, _plugin );
    }

    /**
     * Set a version as the current one for a pipeline
     *
     * @param nVersionId
     *            The version id
     * @param nPipelineId
     *            The pipeline id
     */
    public static void setCurrentVersion( int nVersionId, int nPipelineId )
    {
        _dao.setCurrentVersion( nVersionId, nPipelineId, _plugin );
    }

    /**
     * Find a version by pipeline identifier and version name
     *
     * @param pipelineId
     *            The pipeline identifier
     * @param versionName
     *            The version name to search for
     * @return The matching version if found, empty otherwise
     */
    public static Optional<PipelineVersion> findByPipelineIdAndVersionName( int pipelineId, String versionName )
    {
        List<PipelineVersion> versions = findByPipelineId( pipelineId );
        return versions.stream( ).filter( v -> v.getVersionName( ).equals( versionName ) ).findFirst( );
    }
}
