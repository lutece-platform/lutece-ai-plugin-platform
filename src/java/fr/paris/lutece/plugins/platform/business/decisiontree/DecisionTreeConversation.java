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

import java.sql.Timestamp;
import java.util.List;

public class DecisionTreeConversation
{
    private String _strConversationId;
    private int _nTreeId;
    private int _nClientId;
    private Timestamp _startTime;
    private Timestamp _lastStepTime;
    private List<DecisionTreeConversationStep> _steps;

    /** @return the unique conversation identifier */
    public String getConversationId( )
    {
        return _strConversationId;
    }

    /**
     * @param strConversationId
     *            the unique conversation identifier
     */
    public void setConversationId( String strConversationId )
    {
        _strConversationId = strConversationId;
    }

    /** @return the decision tree identifier this conversation belongs to */
    public int getTreeId( )
    {
        return _nTreeId;
    }

    /**
     * @param nTreeId
     *            the decision tree identifier
     */
    public void setTreeId( int nTreeId )
    {
        _nTreeId = nTreeId;
    }

    /** @return the client identifier associated with this conversation */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * @param nClientId
     *            the client identifier
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /** @return the timestamp when the conversation started */
    public Timestamp getStartTime( )
    {
        return _startTime;
    }

    /**
     * @param startTime
     *            the conversation start timestamp
     */
    public void setStartTime( Timestamp startTime )
    {
        _startTime = startTime;
    }

    /** @return the timestamp of the most recent step in this conversation */
    public Timestamp getLastStepTime( )
    {
        return _lastStepTime;
    }

    /**
     * @param lastStepTime
     *            the last step timestamp
     */
    public void setLastStepTime( Timestamp lastStepTime )
    {
        _lastStepTime = lastStepTime;
    }

    /** @return the ordered list of steps in this conversation */
    public List<DecisionTreeConversationStep> getSteps( )
    {
        return _steps;
    }

    /**
     * @param steps
     *            the ordered list of steps
     */
    public void setSteps( List<DecisionTreeConversationStep> steps )
    {
        _steps = steps;
    }
}
