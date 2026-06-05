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
package fr.paris.lutece.plugins.platform.service.decisiontree;

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversation;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeConversationHome;
import fr.paris.lutece.plugins.platform.service.decisiontree.dto.DecisionTreeOverview;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.util.sql.DAOUtil;

/**
 * Proves the lastActivity contract of the tree overview : it must reflect the most recently ACTIVE conversation, not the last step time of the most recently
 * STARTED one.
 */
public class DecisionTreeLastActivityTest extends AbstractPlatformDbTest
{
    private static final int TREE_ID = 90431;

    /**
     * Seeds two conversations : one started early but active late, one started later but inactive since. The overview lastActivity must be the late activity of
     * the early conversation.
     */
    @Test
    public void testLastActivityIsMostRecentActivity( )
    {
        String earlyStartedLateActive = seedConversation( "2024-01-01 10:00:00", "2024-01-01 12:00:00" );
        String lateStartedEarlyIdle = seedConversation( "2024-01-01 11:00:00", "2024-01-01 11:30:00" );

        DecisionTreeOverview overview = DecisionTreeService.getTreeOverview( TREE_ID );

        assertNotNull( earlyStartedLateActive );
        assertNotNull( lateStartedEarlyIdle );
        assertEquals( Timestamp.valueOf( "2024-01-01 12:00:00" ), overview.getLastActivity( ),
                "lastActivity must be the most recent last_step_time across conversations, not the one of the most recently started conversation" );
    }

    /**
     * Creates a conversation for the test tree and forces its start and last-step timestamps.
     *
     * @param strStartTime
     *            the start timestamp (yyyy-MM-dd HH:mm:ss)
     * @param strLastStepTime
     *            the last step timestamp (yyyy-MM-dd HH:mm:ss)
     * @return the conversation identifier
     */
    private String seedConversation( String strStartTime, String strLastStepTime )
    {
        String strConversationId = UUID.randomUUID( ).toString( );
        DecisionTreeConversation conversation = new DecisionTreeConversation( );
        conversation.setConversationId( strConversationId );
        conversation.setTreeId( TREE_ID );
        conversation.setClientId( 1 );
        DecisionTreeConversationHome.createConversation( conversation );

        try ( DAOUtil daoUtil = new DAOUtil( "UPDATE platform_decision_tree_conversation SET start_time = ?, last_step_time = ? WHERE conversation_id = ?",
                PluginService.getPlugin( "platform" ) ) )
        {
            daoUtil.setTimestamp( 1, Timestamp.valueOf( strStartTime ) );
            daoUtil.setTimestamp( 2, Timestamp.valueOf( strLastStepTime ) );
            daoUtil.setString( 3, strConversationId );
            daoUtil.executeUpdate( );
        }
        return strConversationId;
    }
}
