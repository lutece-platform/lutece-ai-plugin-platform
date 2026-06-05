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
import fr.paris.lutece.util.ReferenceList;

/**
 * This class provides instances management methods for DecisionTree objects
 */
public final class DecisionTreeHome
{
    private static IDecisionTreeDAO _dao = CDI.current( ).select( IDecisionTreeDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor - this class does not need to be instantiated
     */
    private DecisionTreeHome( )
    {
    }

    /**
     * Creates a new DecisionTree
     *
     * @param tree
     *            The DecisionTree object to create
     * @return The created DecisionTree
     */
    public static DecisionTree create( DecisionTree tree )
    {
        _dao.insert( tree, _plugin );
        return tree;
    }

    /**
     * Updates a DecisionTree
     *
     * @param tree
     *            The DecisionTree object to update
     * @return The updated DecisionTree
     */
    public static DecisionTree update( DecisionTree tree )
    {
        _dao.store( tree, _plugin );
        return tree;
    }

    /**
     * Removes a DecisionTree
     *
     * @param nKey
     *            The DecisionTree identifier
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
    }

    /**
     * Finds a DecisionTree by its primary key
     *
     * @param nKey
     *            The DecisionTree identifier
     * @return An Optional containing the DecisionTree if found, empty otherwise
     */
    public static Optional<DecisionTree> findByPrimaryKey( int nKey )
    {
        return _dao.load( nKey, _plugin );
    }

    /**
     * Returns the list of all DecisionTrees
     *
     * @return The list of DecisionTrees
     */
    public static List<DecisionTree> getDecisionTreesList( )
    {
        return _dao.selectDecisionTreesList( _plugin );
    }

    /**
     * Returns the list of DecisionTrees for a specific client
     *
     * @param clientId
     *            The client identifier
     * @return The list of DecisionTrees for the specified client
     */
    public static List<DecisionTree> getDecisionTreesListByClientId( int clientId )
    {
        return _dao.selectDecisionTreesListByClientId( clientId, _plugin );
    }

    /**
     * Returns a reference list of all DecisionTrees (ID and name only)
     *
     * @return A ReferenceList containing DecisionTree IDs and names
     */
    public static ReferenceList getDecisionTreesReferenceList( )
    {
        return _dao.selectDecisionTreesReferenceList( _plugin );
    }
}
