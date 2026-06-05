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
package fr.paris.lutece.plugins.platform.business.observability;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import java.sql.Timestamp;
import java.util.List;

/**
 * Home class for PlatformResourceNodeTrace objects
 */
public final class PlatformResourceNodeTraceHome
{
    private static final IPlatformResourceNodeTraceDAO _dao = CDI.current( ).select( IPlatformResourceNodeTraceDAO.class ).get( );
    private static final Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private PlatformResourceNodeTraceHome( )
    {
    }

    /**
     * Creates a new platform resource node trace
     *
     * @param trace
     *            the trace to create
     */
    public static void create( PlatformResourceNodeTrace trace )
    {
        if ( trace.getTimestamp( ) == null )
        {
            trace.setTimestamp( new Timestamp( System.currentTimeMillis( ) ) );
        }
        _dao.insert( trace, _plugin );
    }

    /**
     * Returns the list of traces for a given node execution
     *
     * @param idNodeExecution
     *            the node execution ID
     * @return the list of traces
     */
    public static List<PlatformResourceNodeTrace> getNodeTraces( int idNodeExecution )
    {
        return _dao.getNodeTraces( idNodeExecution, _plugin );
    }

    /**
     * Removes traces by node execution ID
     *
     * @param idNodeExecution
     *            the node execution ID
     */
    public static void removeByNodeExecutionId( int idNodeExecution )
    {
        _dao.deleteByNodeExecutionId( idNodeExecution, _plugin );
    }
}
