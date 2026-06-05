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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DecisionNode entity representing a node in a decision tree
 */
public class DecisionNode implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nTreeId;
    private String _strNodeTitle;
    private boolean _bShowBackButton;
    private Integer _nBackTargetNodeId;
    private String _strContent;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    private transient List<DecisionTransition> _listTransitions = new ArrayList<>( );

    /**
     * Get the node ID
     *
     * @return The node ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Set the node ID
     *
     * @param nId
     *            The node ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Get the tree ID
     *
     * @return The tree ID
     */
    public int getTreeId( )
    {
        return _nTreeId;
    }

    /**
     * Set the tree ID
     *
     * @param nTreeId
     *            The tree ID
     */
    public void setTreeId( int nTreeId )
    {
        _nTreeId = nTreeId;
    }

    /**
     * Get the node title
     *
     * @return The node title
     */
    public String getNodeTitle( )
    {
        return _strNodeTitle;
    }

    /**
     * Set the node title
     *
     * @param strNodeTitle
     *            The node title
     */
    public void setNodeTitle( String strNodeTitle )
    {
        _strNodeTitle = strNodeTitle;
    }

    /**
     * Returns whether the back button should be shown for this node.
     *
     * @return {@code true} if the back button is visible, {@code false} otherwise
     */
    public boolean getShowBackButton( )
    {
        return _bShowBackButton;
    }

    /**
     * Sets whether the back button should be shown for this node.
     *
     * @param bShowBackButton
     *            {@code true} to show the back button, {@code false} to hide it
     */
    public void setShowBackButton( boolean bShowBackButton )
    {
        _bShowBackButton = bShowBackButton;
    }

    /**
     * Returns the target node ID for the back button, or {@code null} for previous-node behavior.
     *
     * @return the back target node ID, or {@code null}
     */
    public Integer getBackTargetNodeId( )
    {
        return _nBackTargetNodeId;
    }

    /**
     * Sets the target node ID for the back button.
     *
     * @param nBackTargetNodeId
     *            the target node ID, or {@code null} for previous-node behavior
     */
    public void setBackTargetNodeId( Integer nBackTargetNodeId )
    {
        _nBackTargetNodeId = nBackTargetNodeId;
    }

    /**
     * Returns the rich-text content of this node.
     *
     * @return the node content, or {@code null} if not set
     */
    public String getContent( )
    {
        return _strContent;
    }

    /**
     * Sets the rich-text content of this node.
     *
     * @param strContent
     *            the node content
     */
    public void setContent( String strContent )
    {
        _strContent = strContent;
    }

    /**
     * Get the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Set the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Get the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Set the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Get the list of transitions
     *
     * @return The list of transitions
     */
    public List<DecisionTransition> getTransitions( )
    {
        return _listTransitions;
    }

    /**
     * Set the list of transitions
     *
     * @param listTransitions
     *            The list of transitions
     */
    public void setTransitions( List<DecisionTransition> listTransitions )
    {
        _listTransitions = listTransitions;
    }
}
