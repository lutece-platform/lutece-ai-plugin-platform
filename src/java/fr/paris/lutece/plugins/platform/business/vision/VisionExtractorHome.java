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
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Optional;

public final class VisionExtractorHome
{

    private static final String PLUGIN_NAME = "platform";

    private static IVisionExtractorDAO _dao = CDI.current( ).select( IVisionExtractorDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( PLUGIN_NAME );

    /**
     * Private constructor
     */
    private VisionExtractorHome( )
    {
    }

    /**
     * Creates a new vision extractor
     *
     * @param extractor
     *            the vision extractor to create
     * @return the created vision extractor
     */
    public static VisionExtractor create( VisionExtractor extractor )
    {
        _dao.insert( extractor, _plugin );
        return extractor;
    }

    /**
     * Updates an existing vision extractor
     *
     * @param extractor
     *            the vision extractor to update
     * @return the updated vision extractor
     */
    public static VisionExtractor update( VisionExtractor extractor )
    {
        _dao.store( extractor, _plugin );
        return extractor;
    }

    /**
     * Removes a vision extractor by its primary key
     *
     * @param nKey
     *            the primary key of the vision extractor to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Removes all vision extractors associated with a specific vision ID
     *
     * @param nVisionId
     *            the vision ID to remove extractors for
     */
    public static void removeByVisionId( int nVisionId )
    {
        _dao.deleteByVisionId( nVisionId, _plugin );
    }

    /**
     * Finds a vision extractor by its primary key
     *
     * @param nKey
     *            the primary key to search for
     * @return an Optional containing the vision extractor if found, empty otherwise
     */
    public static Optional<VisionExtractor> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Finds all vision extractors associated with a specific vision ID
     *
     * @param nVisionId
     *            the vision ID to search for
     * @return a list of vision extractors matching the vision ID
     */
    public static List<VisionExtractor> findByVisionId( int nVisionId )
    {
        return _dao.selectByVisionId( nVisionId, _plugin );
    }
}
