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
 * This class provides instances management methods for DecisionNode objects
 */
public final class DecisionNodeHome
{
    private static IDecisionNodeDAO _dao = CDI.current( ).select( IDecisionNodeDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private DecisionNodeHome( )
    {
    }

    /**
     * Create a new decision node record in the database
     *
     * @param node
     *            The decision node object
     * @return The decision node object with its ID updated
     */
    public static DecisionNode create( DecisionNode node )
    {
        _dao.insert( node, _plugin );
        return node;
    }

    /**
     * Update a decision node record in the database
     *
     * @param node
     *            The decision node object
     * @return The updated decision node object
     */
    public static DecisionNode update( DecisionNode node )
    {
        _dao.store( node, _plugin );
        return node;
    }

    /**
     * Remove a decision node from the database
     *
     * @param nKey
     *            The decision node ID
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Find a decision node by its primary key
     *
     * @param nKey
     *            The decision node ID
     * @return An Optional containing the decision node if found
     */
    public static Optional<DecisionNode> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Get all decision nodes for a given tree
     *
     * @param nTreeId
     *            The tree ID
     * @return The list of decision nodes ordered by sort_order
     */
    public static List<DecisionNode> getNodesByTreeId( int nTreeId )
    {
        return _dao.selectNodesByTreeId( nTreeId, _plugin );
    }

}
