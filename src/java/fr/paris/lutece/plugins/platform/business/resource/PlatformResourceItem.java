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
package fr.paris.lutece.plugins.platform.business.resource;

/**
 * RRepresents a resource entity in the platform.
 */
public class PlatformResourceItem
{
    private int _nId;
    private String _strType;
    private String _strName;
    private String _strDescription;
    private String _strViewUrl;
    private String _strIcon;
    private int _nClientId;
    private Boolean _bIsSubscribable;
    private double _dTotalCost;
    private int _nExecutionCount;
    private double _dSuccessRate;
    private java.sql.Timestamp _tLastExecutionDate;

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    /**
     * Gets the resource identifier
     *
     * @return The resource ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the resource identifier
     *
     * @param nId
     *            The resource ID to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Gets the resource type
     *
     * @return The resource type
     */
    public String getType( )
    {
        return _strType;
    }

    /**
     * Sets the resource type
     *
     * @param strType
     *            The resource type to set
     */
    public void setType( String strType )
    {
        _strType = strType;
    }

    /**
     * Gets the resource name
     *
     * @return The resource name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the resource name
     *
     * @param strName
     *            The resource name to set
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Gets the resource description
     *
     * @return The resource description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the resource description
     *
     * @param strDescription
     *            The resource description to set
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Gets the resource view URL
     *
     * @return The view URL
     */
    public String getViewUrl( )
    {
        return _strViewUrl;
    }

    /**
     * Sets the resource view URL
     *
     * @param strViewUrl
     *            The view URL to set
     */
    public void setViewUrl( String strViewUrl )
    {
        _strViewUrl = strViewUrl;
    }

    /**
     * Gets the resource icon
     *
     * @return The icon
     */
    public String getIcon( )
    {
        return _strIcon;
    }

    /**
     * Sets the resource icon
     *
     * @param strIcon
     *            The icon to set
     */
    public void setIcon( String strIcon )
    {
        _strIcon = strIcon;
    }

    /**
     * Gets the client identifier
     *
     * @return The client ID
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Sets the client identifier
     *
     * @param nClientId
     *            The client ID to set
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Checks if the resource is subscribable
     *
     * @return True if subscribable, false otherwise
     */
    public Boolean isSubscribable( )
    {
        return _bIsSubscribable;
    }

    /**
     * Sets the subscribable status of the resource
     *
     * @param bIsSubscribable
     *            The subscribable status to set
     */
    public void setSubscribable( Boolean bIsSubscribable )
    {
        _bIsSubscribable = bIsSubscribable;
    }

    /**
     * Checks if the current user can view this resource
     *
     * @return true if user can view this resource
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this resource
     *
     * @param userCanView
     *            true if user can view this resource
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this resource
     *
     * @return true if user can modify this resource
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this resource
     *
     * @param userCanModify
     *            true if user can modify this resource
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this resource
     *
     * @return true if user can delete this resource
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this resource
     *
     * @param userCanDelete
     *            true if user can delete this resource
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * Gets the total cost of this resource
     *
     * @return The total cost
     */
    public double getTotalCost( )
    {
        return _dTotalCost;
    }

    /**
     * Sets the total cost of this resource
     *
     * @param dTotalCost
     *            The total cost to set
     */
    public void setTotalCost( double dTotalCost )
    {
        _dTotalCost = dTotalCost;
    }

    /**
     * Returns the execution count
     *
     * @return the execution count
     */
    public int getExecutionCount( )
    {
        return _nExecutionCount;
    }

    /**
     * Sets the execution count
     *
     * @param nExecutionCount
     *            the execution count
     */
    public void setExecutionCount( int nExecutionCount )
    {
        _nExecutionCount = nExecutionCount;
    }

    /**
     * Returns the success rate
     *
     * @return the success rate
     */
    public double getSuccessRate( )
    {
        return _dSuccessRate;
    }

    /**
     * Sets the success rate
     *
     * @param dSuccessRate
     *            the success rate
     */
    public void setSuccessRate( double dSuccessRate )
    {
        _dSuccessRate = dSuccessRate;
    }

    /**
     * Returns the last execution date
     *
     * @return the last execution date
     */
    public java.sql.Timestamp getLastExecutionDate( )
    {
        return _tLastExecutionDate;
    }

    /**
     * Sets the last execution date
     *
     * @param tLastExecutionDate
     *            the last execution date
     */
    public void setLastExecutionDate( java.sql.Timestamp tLastExecutionDate )
    {
        _tLastExecutionDate = tLastExecutionDate;
    }
}
