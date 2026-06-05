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

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * PipelineTrigger Home class
 */
public final class PipelineTriggerHome
{
    private static IPipelineTriggerDAO _dao = CDI.current( ).select( IPipelineTriggerDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private PipelineTriggerHome( )
    {
    }

    /**
     * Create a new trigger
     *
     * @param trigger
     *            The trigger
     * @return The generated id
     */
    public static int create( PipelineTrigger trigger )
    {
        return _dao.insert( trigger, _plugin );
    }

    /**
     * Update a trigger
     *
     * @param trigger
     *            The trigger
     */
    public static void update( PipelineTrigger trigger )
    {
        _dao.store( trigger, _plugin );
    }

    /**
     * Delete a trigger
     *
     * @param nTriggerId
     *            The trigger id
     */
    public static void remove( int nTriggerId )
    {
        _dao.delete( nTriggerId, _plugin );
    }

    /**
     * Find a trigger by id
     *
     * @param nTriggerId
     *            The trigger id
     * @return The trigger if found
     */
    public static Optional<PipelineTrigger> findByPrimaryKey( int nTriggerId )
    {
        return _dao.load( nTriggerId, _plugin );
    }

    /**
     * Find all triggers for a pipeline
     *
     * @param nPipelineId
     *            The pipeline id
     * @return The list of triggers
     */
    public static List<PipelineTrigger> findByPipelineId( int nPipelineId )
    {
        return _dao.selectByPipelineId( nPipelineId, _plugin );
    }

    /**
     * Find all active triggers
     *
     * @return The list of active triggers
     */
    public static List<PipelineTrigger> findAllActive( )
    {
        return _dao.selectAllActive( _plugin );
    }

    /**
     * Update the last triggered timestamp
     *
     * @param nTriggerId
     *            The trigger id
     * @param timestamp
     *            The timestamp
     */
    public static void updateLastTriggeredAt( int nTriggerId, Timestamp timestamp )
    {
        _dao.updateLastTriggeredAt( nTriggerId, timestamp, _plugin );
    }
}
