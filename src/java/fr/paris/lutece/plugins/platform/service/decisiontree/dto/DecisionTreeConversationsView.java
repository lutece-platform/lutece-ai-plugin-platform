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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionNode;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;

/**
 * Typed view model for the decision tree conversations analytics view. Composes a {@link DecisionTreeOverview} for the shared tree structure and statistics
 * (nodes, node map, root nodes, last activity, average steps, average duration) and adds the conversation-specific data: the (optionally date-filtered)
 * conversations, the step-count distribution and the per-node visit counts already keyed by string id.
 */
public class DecisionTreeConversationsView
{
    private final DecisionTreeOverview _overview;
    private final List<DecisionTreeConversation> _conversations;
    private final Map<String, Integer> _stepCounts;
    private final Map<String, Integer> _nodeVisitCounts;

    /**
     * Builds a decision tree conversations view.
     *
     * @param overview
     *            the shared tree structure and statistics for the (filtered) conversations
     * @param conversations
     *            the conversations, possibly filtered by date range
     * @param stepCounts
     *            the step-count distribution keyed by step label
     * @param nodeVisitCounts
     *            the per-node visit counts keyed by node string id
     */
    public DecisionTreeConversationsView( DecisionTreeOverview overview, List<DecisionTreeConversation> conversations, Map<String, Integer> stepCounts,
            Map<String, Integer> nodeVisitCounts )
    {
        _overview = overview;
        _conversations = conversations;
        _stepCounts = stepCounts;
        _nodeVisitCounts = nodeVisitCounts;
    }

    /**
     * Gets the conversations.
     *
     * @return the conversations
     */
    public List<DecisionTreeConversation> getConversations( )
    {
        return _conversations;
    }

    /**
     * Gets the timestamp of the most recent conversation activity.
     *
     * @return the last activity timestamp, or null when there is none
     */
    public Timestamp getLastActivity( )
    {
        return _overview.getLastActivity( );
    }

    /**
     * Gets the average number of steps per conversation.
     *
     * @return the average steps
     */
    public double getAverageSteps( )
    {
        return _overview.getAverageSteps( );
    }

    /**
     * Gets the average conversation duration in seconds.
     *
     * @return the average duration in seconds
     */
    public double getAverageDurationSeconds( )
    {
        return _overview.getAverageDurationSeconds( );
    }

    /**
     * Gets the step-count distribution keyed by step label.
     *
     * @return the step counts
     */
    public Map<String, Integer> getStepCounts( )
    {
        return _stepCounts;
    }

    /**
     * Gets the nodes with their transitions populated.
     *
     * @return the nodes
     */
    public List<DecisionNode> getNodes( )
    {
        return _overview.getNodes( );
    }

    /**
     * Gets the node lookup map keyed by string id.
     *
     * @return the node map
     */
    public Map<String, DecisionNode> getNodeMap( )
    {
        return _overview.getNodeMap( );
    }

    /**
     * Gets the root nodes.
     *
     * @return the root nodes
     */
    public List<DecisionNode> getRootNodes( )
    {
        return _overview.getRootNodes( );
    }

    /**
     * Gets the per-node visit counts keyed by node string id.
     *
     * @return the node visit counts
     */
    public Map<String, Integer> getNodeVisitCounts( )
    {
        return _nodeVisitCounts;
    }
}
