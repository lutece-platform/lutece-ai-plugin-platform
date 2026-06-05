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
import fr.paris.lutece.util.ReferenceList;
import java.util.List;
import java.util.Optional;

/**
 * IPipelineDAO Interface
 */
public interface IPipelineDAO
{
    /**
     * Insert a new record in the table.
     *
     * @param pipeline
     *            The instance of the Pipeline to insert
     * @param plugin
     *            The plugin
     * @return The id of the newly created pipeline
     */
    int insert( Pipeline pipeline, Plugin plugin );

    /**
     * Update the record in the table
     *
     * @param pipeline
     *            The instance of the Pipeline to update
     * @param plugin
     *            The plugin
     */
    void store( Pipeline pipeline, Plugin plugin );

    /**
     * Delete a record from the table
     *
     * @param nPipelineId
     *            The id of the Pipeline to delete
     * @param plugin
     *            The plugin
     */
    void delete( int nPipelineId, Plugin plugin );

    /**
     * Load the data from the table
     *
     * @param nPipelineId
     *            The id of the Pipeline
     * @param plugin
     *            The plugin
     * @return The instance of the Pipeline
     */
    Optional<Pipeline> load( int nPipelineId, Plugin plugin );

    /**
     * Load all the data of the table
     *
     * @param plugin
     *            The plugin
     * @return The list of Pipeline
     */
    List<Pipeline> selectAll( Plugin plugin );

    /**
     * Load all pipelines by client id
     *
     * @param nClientId
     *            The client id
     * @param plugin
     *            The plugin
     * @return The list of pipelines for the client
     */
    List<Pipeline> selectByClientId( int nClientId, Plugin plugin );

    /**
     * Load a reference list of pipelines
     *
     * @param plugin
     *            The plugin
     * @return The reference list
     */
    ReferenceList selectReferenceList( Plugin plugin );

}
