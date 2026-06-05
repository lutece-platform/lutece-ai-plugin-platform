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

/**
 * Interface for DecisionNode DAO
 */
public interface IDecisionNodeDAO
{
    /**
     * Insert a new decision node in the database
     *
     * @param node
     *            The decision node object to insert
     * @param plugin
     *            The plugin
     */
    void insert( DecisionNode node, Plugin plugin );

    /**
     * Store (update) a decision node in the database
     *
     * @param node
     *            The decision node object to store
     * @param plugin
     *            The plugin
     */
    void store( DecisionNode node, Plugin plugin );

    /**
     * Delete a decision node from the database
     *
     * @param nKey
     *            The decision node ID to delete
     * @param plugin
     *            The plugin
     */
    void delete( int nKey, Plugin plugin );

    /**
     * Load a decision node from the database
     *
     * @param nKey
     *            The decision node ID to load
     * @param plugin
     *            The plugin
     * @return An Optional containing the decision node if found
     */
    Optional<DecisionNode> load( int nKey, Plugin plugin );

    /**
     * Select all decision nodes for a given tree ID
     *
     * @param nTreeId
     *            The tree ID
     * @param plugin
     *            The plugin
     * @return The list of decision nodes for the specified tree
     */
    List<DecisionNode> selectNodesByTreeId( int nTreeId, Plugin plugin );

}
