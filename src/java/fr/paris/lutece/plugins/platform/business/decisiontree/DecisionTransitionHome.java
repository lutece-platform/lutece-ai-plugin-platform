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
package fr.paris.lutece.plugins.platform.business.decisiontree;

import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * This class provides instances management methods for DecisionTransition objects
 */
public final class DecisionTransitionHome
{
    private static IDecisionTransitionDAO _dao = CDI.current( ).select( IDecisionTransitionDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private DecisionTransitionHome( )
    {
    }

    /**
     * Creates a new DecisionTransition
     *
     * @param transition
     *            The DecisionTransition object to create
     * @return The created DecisionTransition
     */
    public static DecisionTransition create( DecisionTransition transition )
    {
        _dao.insert( transition, _plugin );
        return transition;
    }

    /**
     * Updates a DecisionTransition
     *
     * @param transition
     *            The DecisionTransition object to update
     * @return The updated DecisionTransition
     */
    public static DecisionTransition update( DecisionTransition transition )
    {
        _dao.store( transition, _plugin );
        return transition;
    }

    /**
     * Removes a DecisionTransition
     *
     * @param nKey
     *            The DecisionTransition identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Finds a DecisionTransition by its primary key
     *
     * @param nKey
     *            The DecisionTransition identifier
     * @return An Optional containing the DecisionTransition if found, empty otherwise
     */
    public static Optional<DecisionTransition> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of transitions for a given source node
     *
     * @param nSourceNodeId
     *            The source node identifier
     * @return The list of DecisionTransitions for the specified source node
     */
    public static List<DecisionTransition> getTransitionsBySourceNodeId( int nSourceNodeId )
    {
        return _dao.selectTransitionsBySourceNodeId( nSourceNodeId, _plugin );
    }

    /**
     * Removes all transitions for a given source node
     *
     * @param nSourceNodeId
     *            The source node identifier
     */
    public static void removeBySourceNodeId( int nSourceNodeId )
    {
        _dao.deleteBySourceNodeId( nSourceNodeId, _plugin );
    }
}
