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
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DecisionTreeConversationHome
{
    private static IDecisionTreeConversationDAO _dao = CDI.current( ).select( IDecisionTreeConversationDAO.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private DecisionTreeConversationHome( )
    {
    }

    /**
     * Creates a new conversation record in the data store.
     *
     * @param conversation
     *            the conversation to persist
     */
    public static void createConversation( DecisionTreeConversation conversation )
    {
        _dao.insertConversation( conversation, _plugin );
    }

    /**
     * Adds a step to an existing conversation and updates its last step timestamp.
     *
     * @param step
     *            the step to add
     */
    public static void addStep( DecisionTreeConversationStep step )
    {
        _dao.insertStep( step, _plugin );
        _dao.updateLastStepTime( step.getConversationId( ), _plugin );
    }

    /**
     * Finds a conversation by its unique identifier.
     *
     * @param strConversationId
     *            the conversation identifier
     * @return an {@link Optional} containing the conversation, or empty if not found
     */
    public static Optional<DecisionTreeConversation> findConversationById( String strConversationId )
    {
        return _dao.findConversationById( strConversationId, _plugin );
    }

    /**
     * Returns all conversations for the specified decision tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @return the list of conversations, possibly empty
     */
    public static List<DecisionTreeConversation> findConversationsByTreeId( int nTreeId )
    {
        return _dao.findConversationsByTreeId( nTreeId, _plugin );
    }

    /**
     * Returns all steps of the specified conversation.
     *
     * @param strConversationId
     *            the conversation identifier
     * @return the ordered list of steps, possibly empty
     */
    public static List<DecisionTreeConversationStep> findStepsByConversationId( String strConversationId )
    {
        return _dao.findStepsByConversationId( strConversationId, _plugin );
    }

    /**
     * Returns the average number of steps per conversation for the given tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @return the average step count, or {@code 0} if no data is available
     */
    public static double getAverageStepsPerConversation( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo )
    {
        return _dao.getAverageStepsPerConversation( nTreeId, dateFrom, dateTo, _plugin );
    }

    /**
     * Returns the average conversation duration in seconds for the given tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @return the average duration in seconds, or {@code 0} if no data is available
     */
    public static double getAverageDurationSeconds( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo )
    {
        return _dao.getAverageDurationSeconds( nTreeId, dateFrom, dateTo, _plugin );
    }

    /**
     * Returns the per-step conversation counts for the given decision tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @return a map of step key to the number of conversations that reached it
     */
    public static Map<String, Integer> getStepCountsByTreeId( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo )
    {
        return _dao.getStepCountsByTreeId( nTreeId, dateFrom, dateTo, _plugin );
    }

    /**
     * Returns conversations for the given tree filtered by date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the start date (inclusive)
     * @param dateTo
     *            the end date (exclusive)
     * @return the filtered list of conversations
     */
    public static List<DecisionTreeConversation> findConversationsByTreeIdAndDateRange( int nTreeId, Timestamp dateFrom, Timestamp dateTo )
    {
        return _dao.findConversationsByTreeIdAndDateRange( nTreeId, dateFrom, dateTo, _plugin );
    }

    /**
     * Returns node visit counts for the given tree, optionally filtered by date range.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the start date (inclusive), or null for no filter
     * @param dateTo
     *            the end date (exclusive), or null for no filter
     * @return a map of nodeId to visit count
     */
    public static Map<Integer, Integer> getNodeVisitCounts( int nTreeId, Timestamp dateFrom, Timestamp dateTo )
    {
        return _dao.getNodeVisitCounts( nTreeId, dateFrom, dateTo, _plugin );
    }
}
