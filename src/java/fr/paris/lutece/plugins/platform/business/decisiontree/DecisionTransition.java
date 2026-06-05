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

/**
 * DecisionTransition entity representing a transition between two decision nodes
 */
public class DecisionTransition implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nSourceNodeId;
    private int _nTargetNodeId;
    private String _strLabel;
    private int _nSortOrder;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    /**
     * Returns the Id
     *
     * @return The Id
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the Id
     *
     * @param nId
     *            The Id
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the SourceNodeId
     *
     * @return The SourceNodeId
     */
    public int getSourceNodeId( )
    {
        return _nSourceNodeId;
    }

    /**
     * Sets the SourceNodeId
     *
     * @param nSourceNodeId
     *            The SourceNodeId
     */
    public void setSourceNodeId( int nSourceNodeId )
    {
        _nSourceNodeId = nSourceNodeId;
    }

    /**
     * Returns the TargetNodeId
     *
     * @return The TargetNodeId
     */
    public int getTargetNodeId( )
    {
        return _nTargetNodeId;
    }

    /**
     * Sets the TargetNodeId
     *
     * @param nTargetNodeId
     *            The TargetNodeId
     */
    public void setTargetNodeId( int nTargetNodeId )
    {
        _nTargetNodeId = nTargetNodeId;
    }

    /**
     * Returns the Label
     *
     * @return The Label
     */
    public String getLabel( )
    {
        return _strLabel;
    }

    /**
     * Sets the Label
     *
     * @param strLabel
     *            The Label
     */
    public void setLabel( String strLabel )
    {
        _strLabel = strLabel;
    }

    /**
     * Returns the SortOrder
     *
     * @return The SortOrder
     */
    public int getSortOrder( )
    {
        return _nSortOrder;
    }

    /**
     * Sets the SortOrder
     *
     * @param nSortOrder
     *            The SortOrder
     */
    public void setSortOrder( int nSortOrder )
    {
        _nSortOrder = nSortOrder;
    }

    /**
     * Returns the CreatedAt
     *
     * @return The CreatedAt
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the CreatedAt
     *
     * @param timestampCreatedAt
     *            The CreatedAt
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the UpdatedAt
     *
     * @return The UpdatedAt
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the UpdatedAt
     *
     * @param timestampUpdatedAt
     *            The UpdatedAt
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }
}
