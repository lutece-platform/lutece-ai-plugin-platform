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

/**
 * Typed view model for the read-only decision tree overview. Carries the nodes with their transitions, the node lookup map, the root nodes and the conversation
 * statistics (count, last activity, average steps, average duration) so the controller no longer has to call several Home stat methods and aggregate them
 * itself.
 */
public class DecisionTreeOverview
{
    private final List<DecisionNode> _nodes;
    private final Map<String, DecisionNode> _nodeMap;
    private final List<DecisionNode> _rootNodes;
    private final int _conversationCount;
    private final Timestamp _lastActivity;
    private final double _averageSteps;
    private final double _averageDurationSeconds;

    /**
     * Builds a decision tree overview.
     *
     * @param nodes
     *            the nodes with their transitions populated
     * @param nodeMap
     *            the node lookup map keyed by string id
     * @param rootNodes
     *            the root nodes
     * @param conversationCount
     *            the number of conversations on the tree
     * @param lastActivity
     *            the timestamp of the most recent conversation activity, or null when there is none
     * @param averageSteps
     *            the average number of steps per conversation
     * @param averageDurationSeconds
     *            the average conversation duration in seconds
     */
    public DecisionTreeOverview( List<DecisionNode> nodes, Map<String, DecisionNode> nodeMap, List<DecisionNode> rootNodes, int conversationCount,
            Timestamp lastActivity, double averageSteps, double averageDurationSeconds )
    {
        _nodes = nodes;
        _nodeMap = nodeMap;
        _rootNodes = rootNodes;
        _conversationCount = conversationCount;
        _lastActivity = lastActivity;
        _averageSteps = averageSteps;
        _averageDurationSeconds = averageDurationSeconds;
    }

    /**
     * Gets the nodes with their transitions populated.
     *
     * @return the nodes
     */
    public List<DecisionNode> getNodes( )
    {
        return _nodes;
    }

    /**
     * Gets the node lookup map keyed by string id.
     *
     * @return the node map
     */
    public Map<String, DecisionNode> getNodeMap( )
    {
        return _nodeMap;
    }

    /**
     * Gets the root nodes.
     *
     * @return the root nodes
     */
    public List<DecisionNode> getRootNodes( )
    {
        return _rootNodes;
    }

    /**
     * Gets the number of conversations on the tree.
     *
     * @return the conversation count
     */
    public int getConversationCount( )
    {
        return _conversationCount;
    }

    /**
     * Gets the timestamp of the most recent conversation activity.
     *
     * @return the last activity timestamp, or null when there is none
     */
    public Timestamp getLastActivity( )
    {
        return _lastActivity;
    }

    /**
     * Gets the average number of steps per conversation.
     *
     * @return the average steps
     */
    public double getAverageSteps( )
    {
        return _averageSteps;
    }

    /**
     * Gets the average conversation duration in seconds.
     *
     * @return the average duration in seconds
     */
    public double getAverageDurationSeconds( )
    {
        return _averageDurationSeconds;
    }
}
