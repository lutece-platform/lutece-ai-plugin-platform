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

public interface IVisionExtractorFieldDAO
{

    /**
     * Insert a new VisionExtractorField
     *
     * @param field
     *            the vision extractor field to insert
     * @param plugin
     *            the plugin
     */
    void insert( VisionExtractorField field, Plugin plugin );

    /**
     * Store a VisionExtractorField
     *
     * @param field
     *            the vision extractor field to store
     * @param plugin
     *            the plugin
     */
    void store( VisionExtractorField field, Plugin plugin );

    /**
     * Delete a VisionExtractorField by its id
     *
     * @param nFieldId
     *            the field id to delete
     * @param plugin
     *            the plugin
     */
    void delete( int nFieldId, Plugin plugin );

    /**
     * Delete VisionExtractorFields by extractor id
     *
     * @param nExtractorId
     *            the extractor id
     * @param plugin
     *            the plugin
     */
    void deleteByExtractorId( int nExtractorId, Plugin plugin );

    /**
     * Load a VisionExtractorField by its key
     *
     * @param nKey
     *            the key
     * @param plugin
     *            the plugin
     * @return an Optional containing the VisionExtractorField if found
     */
    Optional<VisionExtractorField> load( int nKey, Plugin plugin );

    /**
     * Select VisionExtractorFields by extractor id
     *
     * @param nExtractorId
     *            the extractor id
     * @param plugin
     *            the plugin
     * @return the list of VisionExtractorFields
     */
    List<VisionExtractorField> selectByExtractorId( int nExtractorId, Plugin plugin );
}
