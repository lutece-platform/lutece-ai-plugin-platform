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

import fr.paris.lutece.portal.service.plugin.Plugin;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO contract for decision tree conversations and their steps, including the aggregate statistics queries backing the decision tree analytics views.
 */
public interface IDecisionTreeConversationDAO
{
    /**
     * Inserts a new conversation record.
     *
     * @param conversation
     *            the conversation to insert
     * @param plugin
     *            the plugin
     */
    void insertConversation( DecisionTreeConversation conversation, Plugin plugin );

    /**
     * Inserts a new conversation step record.
     *
     * @param step
     *            the step to insert
     * @param plugin
     *            the plugin
     */
    void insertStep( DecisionTreeConversationStep step, Plugin plugin );

    /**
     * Updates the last step timestamp of a conversation to the current time.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     */
    void updateLastStepTime( String strConversationId, Plugin plugin );

    /**
     * Finds a conversation by its identifier.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     * @return the conversation, or an empty Optional if not found
     */
    Optional<DecisionTreeConversation> findConversationById( String strConversationId, Plugin plugin );

    /**
     * Finds all conversations attached to a decision tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param plugin
     *            the plugin
     * @return the list of conversations
     */
    List<DecisionTreeConversation> findConversationsByTreeId( int nTreeId, Plugin plugin );

    /**
     * Finds the steps of a conversation in chronological order.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     * @return the list of steps
     */
    List<DecisionTreeConversationStep> findStepsByConversationId( String strConversationId, Plugin plugin );

    /**
     * Computes the average number of steps per conversation for a decision tree over a date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the range start, or null to ignore the range
     * @param dateTo
     *            the range end, or null to ignore the range
     * @param plugin
     *            the plugin
     * @return the average step count, or 0 if no conversation matches
     */
    double getAverageStepsPerConversation( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin );

    /**
     * Computes the average conversation duration in seconds for a decision tree over a date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the range start, or null to ignore the range
     * @param dateTo
     *            the range end, or null to ignore the range
     * @param plugin
     *            the plugin
     * @return the average duration in seconds, or 0 if no conversation matches
     */
    double getAverageDurationSeconds( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin );

    /**
     * Counts the steps of each conversation of a decision tree, optionally restricted to a date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the range start, or null to ignore the range
     * @param dateTo
     *            the range end, or null to ignore the range
     * @param plugin
     *            the plugin
     * @return a map of conversation identifier to step count
     */
    Map<String, Integer> getStepCountsByTreeId( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin );

    /**
     * Finds the conversations of a decision tree within a date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the range start, or null to ignore the range
     * @param dateTo
     *            the range end, or null to ignore the range
     * @param plugin
     *            the plugin
     * @return the list of conversations
     */
    List<DecisionTreeConversation> findConversationsByTreeIdAndDateRange( int nTreeId, Timestamp dateFrom, Timestamp dateTo, Plugin plugin );

    /**
     * Counts the visits of each node of a decision tree within a date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the range start, or null to ignore the range
     * @param dateTo
     *            the range end, or null to ignore the range
     * @param plugin
     *            the plugin
     * @return a map of node identifier to visit count
     */
    Map<Integer, Integer> getNodeVisitCounts( int nTreeId, Timestamp dateFrom, Timestamp dateTo, Plugin plugin );
}
