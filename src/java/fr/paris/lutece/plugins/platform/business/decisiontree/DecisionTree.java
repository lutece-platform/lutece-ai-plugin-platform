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
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;

/**
 * DecisionTree entity representing a decision tree with its properties
 */
public class DecisionTree implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "DECISION_TREE";

    private int _nId;

    @NotEmpty( message = "#i18n{platform.agent.validation.decisionTree.TreeName.notEmpty}" )
    private String _strTreeName;

    private String _strTreeDescription;
    private int _nClientId;
    private String _strWelcomeMessage;
    private String _strEndMessage;
    private String _strLogoBase64;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    private transient List<DecisionNode> _listNodes;

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    /**
     * Returns the decision tree primary key.
     *
     * @return the tree identifier
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the decision tree primary key.
     *
     * @param nId
     *            the tree identifier
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the display name of the tree.
     *
     * @return the tree name
     */
    public String getTreeName( )
    {
        return _strTreeName;
    }

    /**
     * Sets the display name of the tree.
     *
     * @param strTreeName
     *            the tree name
     */
    public void setTreeName( String strTreeName )
    {
        _strTreeName = strTreeName;
    }

    /**
     * Returns the description of the tree.
     *
     * @return the tree description
     */
    public String getTreeDescription( )
    {
        return _strTreeDescription;
    }

    /**
     * Sets the description of the tree.
     *
     * @param strTreeDescription
     *            the tree description
     */
    public void setTreeDescription( String strTreeDescription )
    {
        _strTreeDescription = strTreeDescription;
    }

    /**
     * Returns the client identifier that owns this tree.
     *
     * @return the client identifier
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Sets the client identifier that owns this tree.
     *
     * @param nClientId
     *            the client identifier
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Returns the welcome message displayed at the start of the tree.
     *
     * @return the welcome message
     */
    public String getWelcomeMessage( )
    {
        return _strWelcomeMessage;
    }

    /**
     * Sets the welcome message displayed at the start of the tree.
     *
     * @param strWelcomeMessage
     *            the welcome message
     */
    public void setWelcomeMessage( String strWelcomeMessage )
    {
        _strWelcomeMessage = strWelcomeMessage;
    }

    /**
     * Returns the message displayed when the user reaches the end of the tree.
     *
     * @return the end message
     */
    public String getEndMessage( )
    {
        return _strEndMessage;
    }

    /**
     * Sets the message displayed when the user reaches the end of the tree.
     *
     * @param strEndMessage
     *            the end message
     */
    public void setEndMessage( String strEndMessage )
    {
        _strEndMessage = strEndMessage;
    }

    /**
     * Returns the logo image encoded in Base64.
     *
     * @return the Base64-encoded logo, or {@code null} if not set
     */
    public String getLogoBase64( )
    {
        return _strLogoBase64;
    }

    /**
     * Sets the logo image encoded in Base64.
     *
     * @param strLogoBase64
     *            the Base64-encoded logo
     */
    public void setLogoBase64( String strLogoBase64 )
    {
        _strLogoBase64 = strLogoBase64;
    }

    /**
     * Returns the timestamp when this tree was created.
     *
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the timestamp when this tree was created.
     *
     * @param timestampCreatedAt
     *            the creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the timestamp of the last update to this tree.
     *
     * @return the last update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the timestamp of the last update to this tree.
     *
     * @param timestampUpdatedAt
     *            the last update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Returns the list of nodes belonging to this tree (transient, not persisted directly).
     *
     * @return the list of nodes, possibly {@code null} if not loaded
     */
    public List<DecisionNode> getListNodes( )
    {
        return _listNodes;
    }

    /**
     * Sets the list of nodes belonging to this tree.
     *
     * @param listNodes
     *            the list of nodes
     */
    public void setListNodes( List<DecisionNode> listNodes )
    {
        _listNodes = listNodes;
    }

    /**
     * Returns whether the current user has view permission on this tree.
     *
     * @return {@code true} if the user can view
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets whether the current user has view permission on this tree.
     *
     * @param userCanView
     *            {@code true} to grant view permission
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Returns whether the current user has modify permission on this tree.
     *
     * @return {@code true} if the user can modify
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets whether the current user has modify permission on this tree.
     *
     * @param userCanModify
     *            {@code true} to grant modify permission
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Returns whether the current user has delete permission on this tree.
     *
     * @return {@code true} if the user can delete
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets whether the current user has delete permission on this tree.
     *
     * @param userCanDelete
     *            {@code true} to grant delete permission
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceId( )
    {
        return String.valueOf( _nId );
    }

}
