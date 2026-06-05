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
import fr.paris.lutece.util.ReferenceList;

import java.util.List;
import java.util.Optional;

/**
 * Pipeline DAO Home class
 */
public final class PipelineHome
{
    private static IPipelineDAO _dao = CDI.current( ).select( IPipelineDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PipelineHome( )
    {
    }

    /**
     * Create a new pipeline
     *
     * @param pipeline
     *            The pipeline
     * @return The pipeline ID
     */
    public static int create( Pipeline pipeline )
    {
        return _dao.insert( pipeline, _plugin );
    }

    /**
     * Update a pipeline
     *
     * @param pipeline
     *            The pipeline
     * @return The pipeline
     */
    public static Pipeline update( Pipeline pipeline )
    {
        _dao.store( pipeline, _plugin );
        return pipeline;
    }

    /**
     * Delete a pipeline
     *
     * @param nPipelineId
     *            The pipeline ID
     */
    public static void remove( int nPipelineId )
    {
        _dao.delete( nPipelineId, _plugin );
    }

    /**
     * Find a pipeline by ID
     *
     * @param nPipelineId
     *            The pipeline ID
     * @return The pipeline if found, empty otherwise
     */
    public static Optional<Pipeline> findByPrimaryKey( int nPipelineId )
    {
        return _dao.load( nPipelineId, _plugin );
    }

    /**
     * Find all pipelines
     *
     * @return The list of pipelines
     */
    public static List<Pipeline> findAll( )
    {
        return _dao.selectAll( _plugin );
    }

    /**
     * Find pipelines by client ID
     *
     * @param nClientId
     *            The client ID
     * @return The list of pipelines
     */
    public static List<Pipeline> findByClientId( int nClientId )
    {
        return _dao.selectByClientId( nClientId, _plugin );
    }

    /**
     * Get reference list of pipelines
     *
     * @return A reference list of pipelines
     */
    public static ReferenceList getReferenceList( )
    {
        return _dao.selectReferenceList( _plugin );
    }

}
