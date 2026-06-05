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

import fr.paris.lutece.portal.service.plugin.Plugin;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Vision Extractor DAO operations
 */
public interface IVisionExtractorDAO
{

    /**
     * Insert a new vision extractor
     *
     * @param extractor
     *            the vision extractor to insert
     * @param plugin
     *            the plugin instance
     */
    void insert( VisionExtractor extractor, Plugin plugin );

    /**
     * Store an existing vision extractor
     *
     * @param extractor
     *            the vision extractor to store
     * @param plugin
     *            the plugin instance
     */
    void store( VisionExtractor extractor, Plugin plugin );

    /**
     * Delete a vision extractor by its ID
     *
     * @param nExtractorId
     *            the extractor ID
     * @param plugin
     *            the plugin instance
     */
    void delete( int nExtractorId, Plugin plugin );

    /**
     * Delete all vision extractors by vision ID
     *
     * @param nVisionId
     *            the vision ID
     * @param plugin
     *            the plugin instance
     */
    void deleteByVisionId( int nVisionId, Plugin plugin );

    /**
     * Load a vision extractor by its key
     *
     * @param nKey
     *            the extractor key
     * @param plugin
     *            the plugin instance
     * @return an Optional containing the vision extractor if found
     */
    Optional<VisionExtractor> load( int nKey, Plugin plugin );

    /**
     * Select all vision extractors by vision ID
     *
     * @param nVisionId
     *            the vision ID
     * @param plugin
     *            the plugin instance
     * @return a list of vision extractors
     */
    List<VisionExtractor> selectByVisionId( int nVisionId, Plugin plugin );
}
