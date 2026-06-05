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

public class DecisionTreeConversationStep
{
    private int _nId;
    private String _strConversationId;
    private int _nNodeId;
    private String _strNodeTitle;
    private int _nChosenTransitionId;
    private String _strChosenTransitionLabel;
    private Timestamp _stepTime;

    /** @return the auto-generated step identifier */
    public int getId( )
    {
        return _nId;
    }

    /**
     * @param nId
     *            the step identifier
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /** @return the identifier of the parent conversation */
    public String getConversationId( )
    {
        return _strConversationId;
    }

    /**
     * @param strConversationId
     *            the parent conversation identifier
     */
    public void setConversationId( String strConversationId )
    {
        _strConversationId = strConversationId;
    }

    /** @return the identifier of the decision node visited at this step */
    public int getNodeId( )
    {
        return _nNodeId;
    }

    /**
     * @param nNodeId
     *            the decision node identifier
     */
    public void setNodeId( int nNodeId )
    {
        _nNodeId = nNodeId;
    }

    /** @return the display title of the node at this step */
    public String getNodeTitle( )
    {
        return _strNodeTitle;
    }

    /**
     * @param strNodeTitle
     *            the node display title
     */
    public void setNodeTitle( String strNodeTitle )
    {
        _strNodeTitle = strNodeTitle;
    }

    /** @return the identifier of the transition chosen at this step, or {@code 0} if none */
    public int getChosenTransitionId( )
    {
        return _nChosenTransitionId;
    }

    /**
     * @param nChosenTransitionId
     *            the chosen transition identifier
     */
    public void setChosenTransitionId( int nChosenTransitionId )
    {
        _nChosenTransitionId = nChosenTransitionId;
    }

    /** @return the label of the transition chosen at this step */
    public String getChosenTransitionLabel( )
    {
        return _strChosenTransitionLabel;
    }

    /**
     * @param strChosenTransitionLabel
     *            the chosen transition label
     */
    public void setChosenTransitionLabel( String strChosenTransitionLabel )
    {
        _strChosenTransitionLabel = strChosenTransitionLabel;
    }

    /** @return the timestamp when this step was recorded */
    public Timestamp getStepTime( )
    {
        return _stepTime;
    }

    /**
     * @param stepTime
     *            the step timestamp
     */
    public void setStepTime( Timestamp stepTime )
    {
        _stepTime = stepTime;
    }
}
