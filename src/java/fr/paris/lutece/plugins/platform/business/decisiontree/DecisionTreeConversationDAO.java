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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Named( "platform.decisionTreeConversationDAO" )
public class DecisionTreeConversationDAO implements IDecisionTreeConversationDAO
{
    private static final String SQL_INSERT_CONVERSATION = "INSERT INTO platform_decision_tree_conversation "
            + "( conversation_id, tree_id, client_id, start_time, last_step_time ) " + "VALUES ( ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP )";

    private static final String SQL_INSERT_STEP = "INSERT INTO platform_decision_tree_conversation_step "
            + "( conversation_id, node_id, node_title, chosen_transition_id, step_time ) " + "VALUES ( ?, ?, ?, ?, CURRENT_TIMESTAMP )";

    private static final String SQL_UPDATE_LAST_STEP_TIME = "UPDATE platform_decision_tree_conversation "
            + "SET last_step_time = CURRENT_TIMESTAMP WHERE conversation_id = ?";

    private static final String SQL_SELECT_CONVERSATION = "SELECT conversation_id, tree_id, client_id, start_time, last_step_time "
            + "FROM platform_decision_tree_conversation WHERE conversation_id = ?";

    private static final String SQL_SELECT_BY_TREE_ID = "SELECT conversation_id, tree_id, client_id, start_time, last_step_time "
            + "FROM platform_decision_tree_conversation WHERE tree_id = ? ORDER BY start_time DESC";

    private static final String SQL_AVG_STEPS = "SELECT COALESCE(AVG(step_count), 0) FROM "
            + "( SELECT COUNT(*) as step_count FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id "
            + "WHERE c.tree_id = ? GROUP BY s.conversation_id ) as counts";

    private static final String SQL_AVG_STEPS_DATE_RANGE = "SELECT COALESCE(AVG(step_count), 0) FROM "
            + "( SELECT COUNT(*) as step_count FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id "
            + "WHERE c.tree_id = ? AND c.start_time >= ? AND c.start_time < ? GROUP BY s.conversation_id ) as counts";

    private static final String SQL_AVERAGE_DURATION = "SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, start_time, last_step_time)), 0) "
            + "FROM platform_decision_tree_conversation WHERE tree_id = ?";

    private static final String SQL_AVERAGE_DURATION_DATE_RANGE = "SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, start_time, last_step_time)), 0) "
            + "FROM platform_decision_tree_conversation WHERE tree_id = ? AND start_time >= ? AND start_time < ?";

    private static final String SQL_STEP_COUNTS_BY_TREE = "SELECT s.conversation_id, COUNT(*) " + "FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id " + "WHERE c.tree_id = ? GROUP BY s.conversation_id";

    private static final String SQL_STEP_COUNTS_BY_TREE_DATE_RANGE = "SELECT s.conversation_id, COUNT(*) " + "FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id "
            + "WHERE c.tree_id = ? AND c.start_time >= ? AND c.start_time < ? GROUP BY s.conversation_id";

    private static final String SQL_SELECT_STEPS = "SELECT s.id, s.conversation_id, s.node_id, s.node_title, " + "s.chosen_transition_id, t.label, s.step_time "
            + "FROM platform_decision_tree_conversation_step s "
            + "LEFT JOIN platform_decision_transition t ON t.id_decision_transition = s.chosen_transition_id "
            + "WHERE s.conversation_id = ? ORDER BY s.id ASC";

    private static final String SQL_SELECT_BY_TREE_ID_DATE_RANGE = "SELECT conversation_id, tree_id, client_id, start_time, last_step_time "
            + "FROM platform_decision_tree_conversation WHERE tree_id = ? AND start_time >= ? AND start_time < ? ORDER BY start_time DESC";

    private static final String SQL_NODE_VISIT_COUNTS = "SELECT s.node_id, COUNT(*) " + "FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id " + "WHERE c.tree_id = ? GROUP BY s.node_id";

    private static final String SQL_NODE_VISIT_COUNTS_DATE_RANGE = "SELECT s.node_id, COUNT(*) " + "FROM platform_decision_tree_conversation_step s "
            + "INNER JOIN platform_decision_tree_conversation c ON c.conversation_id = s.conversation_id "
            + "WHERE c.tree_id = ? AND c.start_time >= ? AND c.start_time < ? GROUP BY s.node_id";

    /**
     * Inserts a decision tree conversation record, idempotently: if a conversation with the same identifier already exists, the call is a no-op. This portable
     * check-then-insert replaces a MariaDB-only {@code INSERT IGNORE}.
     *
     * @param conversation
     *            the conversation to insert
     * @param plugin
     *            the plugin
     */
    @Override
    public void insertConversation( DecisionTreeConversation conversation, Plugin plugin )
    {
        if ( findConversationById( conversation.getConversationId( ), plugin ).isPresent( ) )
        {
            return;
        }
        try ( DAOUtil daoUtil = new DAOUtil( SQL_INSERT_CONVERSATION, plugin ) )
        {
            daoUtil.setString( 1, conversation.getConversationId( ) );
            daoUtil.setInt( 2, conversation.getTreeId( ) );
            daoUtil.setInt( 3, conversation.getClientId( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Inserts a new conversation step and sets the generated identifier on the step object.
     *
     * @param step
     *            the step to insert
     * @param plugin
     *            the plugin
     */
    @Override
    public void insertStep( DecisionTreeConversationStep step, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_INSERT_STEP, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            daoUtil.setString( 1, step.getConversationId( ) );
            daoUtil.setInt( 2, step.getNodeId( ) );
            daoUtil.setString( 3, step.getNodeTitle( ) );
            if ( step.getChosenTransitionId( ) > 0 )
            {
                daoUtil.setInt( 4, step.getChosenTransitionId( ) );
            }
            else
            {
                daoUtil.setIntNull( 4 );
            }
            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                step.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * Updates the last step timestamp of the conversation identified by the given ID.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     */
    @Override
    public void updateLastStepTime( String strConversationId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_UPDATE_LAST_STEP_TIME, plugin ) )
        {
            daoUtil.setString( 1, strConversationId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Finds a conversation by its unique identifier.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     * @return an {@link Optional} containing the conversation, or empty if not found
     */
    @Override
    public Optional<DecisionTreeConversation> findConversationById( String strConversationId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_SELECT_CONVERSATION, plugin ) )
        {
            daoUtil.setString( 1, strConversationId );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return Optional.of( mapConversation( daoUtil ) );
            }
        }
        return Optional.empty( );
    }

    /**
     * Returns all conversations belonging to the specified decision tree.
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param plugin
     *            the plugin
     * @return the list of conversations, possibly empty
     */
    @Override
    public List<DecisionTreeConversation> findConversationsByTreeId( int nTreeId, Plugin plugin )
    {
        List<DecisionTreeConversation> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_SELECT_BY_TREE_ID, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( mapConversation( daoUtil ) );
            }
        }
        return list;
    }

    /**
     * Returns all steps of a conversation, ordered by insertion.
     *
     * @param strConversationId
     *            the conversation identifier
     * @param plugin
     *            the plugin
     * @return the ordered list of steps, possibly empty
     */
    @Override
    public List<DecisionTreeConversationStep> findStepsByConversationId( String strConversationId, Plugin plugin )
    {
        List<DecisionTreeConversationStep> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_SELECT_STEPS, plugin ) )
        {
            daoUtil.setString( 1, strConversationId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                DecisionTreeConversationStep step = new DecisionTreeConversationStep( );
                step.setId( daoUtil.getInt( 1 ) );
                step.setConversationId( daoUtil.getString( 2 ) );
                step.setNodeId( daoUtil.getInt( 3 ) );
                step.setNodeTitle( daoUtil.getString( 4 ) );
                step.setChosenTransitionId( daoUtil.getInt( 5 ) );
                step.setChosenTransitionLabel( daoUtil.getString( 6 ) );
                step.setStepTime( daoUtil.getTimestamp( 7 ) );
                list.add( step );
            }
        }
        return list;
    }

    /**
     * Returns the average number of steps per conversation for the given tree, optionally restricted to conversations started within [dateFrom, dateTo).
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the start bound (inclusive), or null for no filter
     * @param dateTo
     *            the end bound (exclusive), or null for no filter
     * @param plugin
     *            the plugin
     * @return the average step count, or {@code 0} if no data is available
     */
    @Override
    public double getAverageStepsPerConversation( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin )
    {
        boolean hasDateRange = dateFrom != null && dateTo != null;
        try ( DAOUtil daoUtil = new DAOUtil( hasDateRange ? SQL_AVG_STEPS_DATE_RANGE : SQL_AVG_STEPS, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            if ( hasDateRange )
            {
                daoUtil.setTimestamp( 2, dateFrom );
                daoUtil.setTimestamp( 3, dateTo );
            }
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return daoUtil.getDouble( 1 );
            }
        }
        return 0;
    }

    /**
     * Returns the average conversation duration in seconds for the given tree, optionally restricted to conversations started within [dateFrom, dateTo).
     *
     * @param nTreeId
     *            the decision tree identifier
     * @param dateFrom
     *            the start bound (inclusive), or null for no filter
     * @param dateTo
     *            the end bound (exclusive), or null for no filter
     * @param plugin
     *            the plugin
     * @return the average duration in seconds, or {@code 0} if no data is available
     */
    @Override
    public double getAverageDurationSeconds( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin )
    {
        boolean hasDateRange = dateFrom != null && dateTo != null;
        try ( DAOUtil daoUtil = new DAOUtil( hasDateRange ? SQL_AVERAGE_DURATION_DATE_RANGE : SQL_AVERAGE_DURATION, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            if ( hasDateRange )
            {
                daoUtil.setTimestamp( 2, dateFrom );
                daoUtil.setTimestamp( 3, dateTo );
            }
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                return daoUtil.getDouble( 1 );
            }
        }
        return 0;
    }

    @Override
    public Map<String, Integer> getStepCountsByTreeId( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin )
    {
        Map<String, Integer> map = new HashMap<>( );
        boolean hasDateRange = dateFrom != null && dateTo != null;
        try ( DAOUtil daoUtil = new DAOUtil( hasDateRange ? SQL_STEP_COUNTS_BY_TREE_DATE_RANGE : SQL_STEP_COUNTS_BY_TREE, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            if ( hasDateRange )
            {
                daoUtil.setTimestamp( 2, dateFrom );
                daoUtil.setTimestamp( 3, dateTo );
            }
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                map.put( daoUtil.getString( 1 ), daoUtil.getInt( 2 ) );
            }
        }
        return map;
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
     * @param plugin
     *            the plugin
     * @return the filtered list of conversations
     */
    @Override
    public List<DecisionTreeConversation> findConversationsByTreeIdAndDateRange( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo,
            Plugin plugin )
    {
        List<DecisionTreeConversation> list = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_SELECT_BY_TREE_ID_DATE_RANGE, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            daoUtil.setTimestamp( 2, dateFrom );
            daoUtil.setTimestamp( 3, dateTo );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                list.add( mapConversation( daoUtil ) );
            }
        }
        return list;
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
     * @param plugin
     *            the plugin
     * @return a map of nodeId to visit count
     */
    @Override
    public Map<Integer, Integer> getNodeVisitCounts( int nTreeId, java.sql.Timestamp dateFrom, java.sql.Timestamp dateTo, Plugin plugin )
    {
        Map<Integer, Integer> map = new HashMap<>( );
        boolean hasDateRange = dateFrom != null && dateTo != null;
        String sql = hasDateRange ? SQL_NODE_VISIT_COUNTS_DATE_RANGE : SQL_NODE_VISIT_COUNTS;
        try ( DAOUtil daoUtil = new DAOUtil( sql, plugin ) )
        {
            daoUtil.setInt( 1, nTreeId );
            if ( hasDateRange )
            {
                daoUtil.setTimestamp( 2, dateFrom );
                daoUtil.setTimestamp( 3, dateTo );
            }
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                map.put( daoUtil.getInt( 1 ), daoUtil.getInt( 2 ) );
            }
        }
        return map;
    }

    /**
     * Builds a decision tree conversation from the current row of the given DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil positioned on the current row
     * @return The decision tree conversation built from the current row
     */
    private DecisionTreeConversation mapConversation( DAOUtil daoUtil )
    {
        DecisionTreeConversation conv = new DecisionTreeConversation( );
        conv.setConversationId( daoUtil.getString( 1 ) );
        conv.setTreeId( daoUtil.getInt( 2 ) );
        conv.setClientId( daoUtil.getInt( 3 ) );
        conv.setStartTime( daoUtil.getTimestamp( 4 ) );
        conv.setLastStepTime( daoUtil.getTimestamp( 5 ) );
        return conv;
    }
}
