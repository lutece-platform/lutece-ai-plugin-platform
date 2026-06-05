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

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.util.ReferenceList;

public final class VisionHome
{
    private static IVisionDAO _dao = CDI.current( ).select( IVisionDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private VisionHome( )
    {
    }

    /**
     * Creates a new Vision object
     *
     * @param vision
     *            the Vision object to create
     * @return the created Vision object
     */
    public static Vision create( Vision vision )
    {
        _dao.insert( vision, _plugin );
        return vision;
    }

    /**
     * Updates an existing Vision object
     *
     * @param vision
     *            the Vision object to update
     * @return the updated Vision object
     */
    public static Vision update( Vision vision )
    {
        _dao.store( vision, _plugin );
        return vision;
    }

    /**
     * Removes a Vision object by its primary key
     *
     * @param nKey
     *            the primary key of the Vision to remove
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Finds a Vision object by its primary key
     *
     * @param nKey
     *            the primary key to search for
     * @return an Optional containing the Vision if found, empty otherwise
     */
    public static Optional<Vision> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Retrieves all Vision objects
     *
     * @return a List of all Vision objects
     */
    public static List<Vision> getVisionsList( )
    {
        return _dao.selectVisionsList( _plugin );
    }

    /**
     * Retrieves Vision objects filtered by client ID
     *
     * @param clientId
     *            the client ID to filter by
     * @return a List of Vision objects matching the client ID
     */
    public static List<Vision> getVisionsListByClientId( int clientId )
    {
        return _dao.selectVisionsListByClientId( clientId, _plugin );
    }

    /**
     * Returns a reference list of all Visions (ID and name only) for RBAC purposes
     *
     * @return A ReferenceList containing vision IDs and names
     */
    public static ReferenceList getVisionsReferenceList( )
    {
        return _dao.selectVisionsReferenceList( _plugin );
    }
}
