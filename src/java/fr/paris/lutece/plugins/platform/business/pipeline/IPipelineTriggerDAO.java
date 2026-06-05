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

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * IPipelineTriggerDAO Interface
 */
public interface IPipelineTriggerDAO
{
    /**
     * Insert a new trigger record
     *
     * @param trigger
     *            The trigger to insert
     * @param plugin
     *            The plugin
     * @return The generated id
     */
    int insert( PipelineTrigger trigger, Plugin plugin );

    /**
     * Update a trigger record
     *
     * @param trigger
     *            The trigger to update
     * @param plugin
     *            The plugin
     */
    void store( PipelineTrigger trigger, Plugin plugin );

    /**
     * Delete a trigger record
     *
     * @param nTriggerId
     *            The trigger id
     * @param plugin
     *            The plugin
     */
    void delete( int nTriggerId, Plugin plugin );

    /**
     * Load a trigger by id
     *
     * @param nTriggerId
     *            The trigger id
     * @param plugin
     *            The plugin
     * @return The trigger if found
     */
    Optional<PipelineTrigger> load( int nTriggerId, Plugin plugin );

    /**
     * Select all triggers for a pipeline
     *
     * @param nPipelineId
     *            The pipeline id
     * @param plugin
     *            The plugin
     * @return The list of triggers
     */
    List<PipelineTrigger> selectByPipelineId( int nPipelineId, Plugin plugin );

    /**
     * Select all active triggers
     *
     * @param plugin
     *            The plugin
     * @return The list of active triggers
     */
    List<PipelineTrigger> selectAllActive( Plugin plugin );

    /**
     * Update the last triggered timestamp
     *
     * @param nTriggerId
     *            The trigger id
     * @param timestamp
     *            The timestamp
     * @param plugin
     *            The plugin
     */
    void updateLastTriggeredAt( int nTriggerId, Timestamp timestamp, Plugin plugin );
}
