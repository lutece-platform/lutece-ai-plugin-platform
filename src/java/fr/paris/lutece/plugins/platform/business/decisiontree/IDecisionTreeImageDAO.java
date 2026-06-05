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
 * Data access operations for the decision tree image mapping.
 */
public interface IDecisionTreeImageDAO
{
    /**
     * Inserts a new mapping entry.
     *
     * @param image
     *            the mapping to persist
     * @param plugin
     *            the plugin
     */
    void insert( DecisionTreeImage image, Plugin plugin );

    /**
     * Deletes the mapping entry for a given (contentHash, treeId) pair.
     *
     * @param strContentHash
     *            the content hash
     * @param nTreeId
     *            the tree id
     * @param plugin
     *            the plugin
     */
    void delete( String strContentHash, int nTreeId, Plugin plugin );

    /**
     * Loads the mapping entry for a (contentHash, treeId) pair.
     *
     * @param strContentHash
     *            the content hash
     * @param nTreeId
     *            the tree id
     * @param plugin
     *            the plugin
     * @return an Optional containing the entry if found, empty otherwise
     */
    Optional<DecisionTreeImage> load( String strContentHash, int nTreeId, Plugin plugin );

    /**
     * Returns all entries for a given content hash, across every tree that references the image.
     *
     * @param strContentHash
     *            the content hash
     * @param plugin
     *            the plugin
     * @return the list of entries (may be empty)
     */
    List<DecisionTreeImage> selectByContentHash( String strContentHash, Plugin plugin );

    /**
     * Returns all entries for a given tree.
     *
     * @param nTreeId
     *            the tree id
     * @param plugin
     *            the plugin
     * @return the list of entries (may be empty)
     */
    List<DecisionTreeImage> selectByTreeId( int nTreeId, Plugin plugin );
}
