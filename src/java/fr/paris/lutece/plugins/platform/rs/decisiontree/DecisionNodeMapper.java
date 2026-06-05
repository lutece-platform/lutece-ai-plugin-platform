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
package fr.paris.lutece.plugins.platform.rs.decisiontree;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNodeDTO;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNodeResponseDTO;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransition;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTransitionDTO;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTree;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeDTO;

/**
 * Maps decision tree entities to the API DTOs shared by the client and admin REST endpoints.
 */
public final class DecisionNodeMapper
{
    /**
     * Private constructor to prevent instantiation.
     */
    private DecisionNodeMapper( )
    {
    }

    /**
     * Build the DTO response for a node with its tree metadata
     *
     * @param node
     *            The decision node with details
     * @param tree
     *            The decision tree
     * @return A DecisionNodeResponseDTO ready for JSON serialization
     */
    public static DecisionNodeResponseDTO toResponseDTO( DecisionNode node, DecisionTree tree )
    {
        DecisionNodeResponseDTO response = new DecisionNodeResponseDTO( );
        response.setNode( toNodeDTO( node ) );
        response.setTree( toTreeDTO( tree ) );
        return response;
    }

    /**
     * Converts a DecisionTree entity to its DTO representation
     *
     * @param tree
     *            The decision tree entity
     * @return The decision tree DTO
     */
    public static DecisionTreeDTO toTreeDTO( DecisionTree tree )
    {
        DecisionTreeDTO dto = new DecisionTreeDTO( );
        dto.setId( tree.getId( ) );
        dto.setTreeName( tree.getTreeName( ) );
        dto.setTreeDescription( tree.getTreeDescription( ) );
        dto.setWelcomeMessage( tree.getWelcomeMessage( ) );
        dto.setEndMessage( tree.getEndMessage( ) );
        dto.setLogoBase64( tree.getLogoBase64( ) );
        return dto;
    }

    /**
     * Converts a DecisionNode entity to its DTO representation
     *
     * @param node
     *            The decision node entity
     * @return The decision node DTO with transitions
     */
    private static DecisionNodeDTO toNodeDTO( DecisionNode node )
    {
        DecisionNodeDTO dto = new DecisionNodeDTO( );
        dto.setId( node.getId( ) );
        dto.setTitle( node.getNodeTitle( ) );
        dto.setContent( node.getContent( ) );
        dto.setShowBackButton( node.getShowBackButton( ) );
        dto.setBackTargetNodeId( node.getBackTargetNodeId( ) );
        dto.setTransitions( node.getTransitions( ).stream( ).map( DecisionNodeMapper::toTransitionDTO ).toList( ) );
        return dto;
    }

    /**
     * Converts a DecisionTransition entity to its DTO representation
     *
     * @param transition
     *            The decision transition entity
     * @return The decision transition DTO
     */
    private static DecisionTransitionDTO toTransitionDTO( DecisionTransition transition )
    {
        DecisionTransitionDTO dto = new DecisionTransitionDTO( );
        dto.setId( transition.getId( ) );
        dto.setLabel( transition.getLabel( ) );
        dto.setTargetNodeId( transition.getTargetNodeId( ) );
        return dto;
    }
}
