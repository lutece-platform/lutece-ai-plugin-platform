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

public final class VisionExtractorFieldHome
{
    private static IVisionExtractorFieldDAO _dao = CDI.current( ).select( IVisionExtractorFieldDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private VisionExtractorFieldHome( )
    {
    }

    /**
     * Creates a new VisionExtractorField
     *
     * @param field
     *            the VisionExtractorField to create
     * @return the created VisionExtractorField
     */
    public static VisionExtractorField create( VisionExtractorField field )
    {
        _dao.insert( field, _plugin );
        return field;
    }

    /**
     * Updates an existing VisionExtractorField
     *
     * @param field
     *            the VisionExtractorField to update
     * @return the updated VisionExtractorField
     */
    public static VisionExtractorField update( VisionExtractorField field )
    {
        _dao.store( field, _plugin );
        return field;
    }

    /**
     * Removes a VisionExtractorField by its primary key
     *
     * @param nKey
     *            the primary key of the VisionExtractorField to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Removes all VisionExtractorFields associated with an extractor
     *
     * @param nExtractorId
     *            the extractor ID
     */
    public static void removeByExtractorId( int nExtractorId )
    {
        _dao.deleteByExtractorId( nExtractorId, _plugin );
    }

    /**
     * Finds a VisionExtractorField by its primary key
     *
     * @param nKey
     *            the primary key
     * @return an Optional containing the VisionExtractorField if found
     */
    public static Optional<VisionExtractorField> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Finds all VisionExtractorFields associated with an extractor
     *
     * @param nExtractorId
     *            the extractor ID
     * @return a List of VisionExtractorFields
     */
    public static List<VisionExtractorField> findByExtractorId( int nExtractorId )
    {
        return _dao.selectByExtractorId( nExtractorId, _plugin );
    }
}
