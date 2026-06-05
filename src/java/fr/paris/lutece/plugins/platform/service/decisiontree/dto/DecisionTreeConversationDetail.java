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
package fr.paris.lutece.plugins.platform.service.decisiontree.dto;

import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationStep;

/**
 * Typed view model for a single decision tree conversation detail. Carries the conversation, its ordered steps and the map of the nodes visited during the
 * conversation (keyed by string id) so the controller no longer has to issue one node lookup per step.
 */
public class DecisionTreeConversationDetail
{
    private final DecisionTreeConversation _conversation;
    private final List<DecisionTreeConversationStep> _steps;
    private final Map<String, DecisionNode> _nodeMap;

    /**
     * Builds a conversation detail.
     *
     * @param conversation
     *            the conversation
     * @param steps
     *            the ordered conversation steps
     * @param nodeMap
     *            the visited nodes keyed by string id
     */
    public DecisionTreeConversationDetail( DecisionTreeConversation conversation, List<DecisionTreeConversationStep> steps, Map<String, DecisionNode> nodeMap )
    {
        _conversation = conversation;
        _steps = steps;
        _nodeMap = nodeMap;
    }

    /**
     * Gets the conversation.
     *
     * @return the conversation
     */
    public DecisionTreeConversation getConversation( )
    {
        return _conversation;
    }

    /**
     * Gets the ordered conversation steps.
     *
     * @return the steps
     */
    public List<DecisionTreeConversationStep> getSteps( )
    {
        return _steps;
    }

    /**
     * Gets the visited nodes keyed by string id.
     *
     * @return the node map
     */
    public Map<String, DecisionNode> getNodeMap( )
    {
        return _nodeMap;
    }

    /**
     * Gets the tree identifier owning the conversation.
     *
     * @return the tree identifier
     */
    public int getTreeId( )
    {
        return _conversation.getTreeId( );
    }
}
