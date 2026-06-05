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
package fr.paris.lutece.plugins.platform.business.client;

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import fr.paris.lutece.plugins.platform.service.rbac.PlatformClientResourceIdService;
import fr.paris.lutece.portal.service.rbac.RBACResource;

/**
 * Represents a client entity in the platform.
 */
public class Client implements Serializable, RBACResource
{
    private static final long serialVersionUID = 1L;
    private int _nId;
    public static final String RESOURCE_TYPE = PlatformClientResourceIdService.RESOURCE_TYPE;

    @NotEmpty( message = "#i18n{platform.validation.client.name.notEmpty}" )
    @Size( max = 255, message = "#i18n{platform.validation.client.name.size}" )
    private String _strName;

    private String _strCode;

    private String _strDescription;
    private boolean _bActive;
    private Timestamp _dateCreatedAt;

    private double _dTotalCost;

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;
    private boolean _bUserCanViewObservability;

    /**
     * Gets the client identifier
     *
     * @return The client identifier
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the client identifier
     *
     * @param id
     *            The client identifier
     */
    public void setId( int id )
    {
        _nId = id;
    }

    /**
     * Gets the client name
     *
     * @return The client name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the client name
     *
     * @param name
     *            The client name
     */
    public void setName( String name )
    {
        _strName = name;
    }

    /**
     * Gets the client code
     *
     * @return The client code
     */
    public String getCode( )
    {
        return _strCode;
    }

    /**
     * Sets the client code
     *
     * @param code
     *            The client code
     */
    public void setCode( String code )
    {
        _strCode = code;
    }

    /**
     * Gets the client description
     *
     * @return The client description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the client description
     *
     * @param description
     *            The client description
     */
    public void setDescription( String description )
    {
        _strDescription = description;
    }

    /**
     * Checks if the client is active
     *
     * @return True if the client is active, false otherwise
     */
    public boolean isActive( )
    {
        return _bActive;
    }

    /**
     * Sets the client active status
     *
     * @param active
     *            The active status
     */
    public void setActive( boolean active )
    {
        _bActive = active;
    }

    /**
     * Gets the client creation date
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _dateCreatedAt;
    }

    /**
     * Sets the client creation date
     *
     * @param createdAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp createdAt )
    {
        _dateCreatedAt = createdAt;
    }

    /**
     * Gets the total cost for this client (transient, not persisted)
     *
     * @return The total cost
     */
    public double getTotalCost( )
    {
        return _dTotalCost;
    }

    /**
     * Sets the total cost for this client (transient, not persisted)
     *
     * @param dTotalCost
     *            The total cost
     */
    public void setTotalCost( double dTotalCost )
    {
        _dTotalCost = dTotalCost;
    }

    /**
     * Checks if the current user can view this client
     *
     * @return true if user can view this client
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this client
     *
     * @param userCanView
     *            true if user can view this client
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this client
     *
     * @return true if user can modify this client
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this client
     *
     * @param userCanModify
     *            true if user can modify this client
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this client
     *
     * @return true if user can delete this client
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this client
     *
     * @param userCanDelete
     *            true if user can delete this client
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * Checks if the current user can view observability for this client
     *
     * @return true if user can view observability for this client
     */
    public boolean isUserCanViewObservability( )
    {
        return _bUserCanViewObservability;
    }

    /**
     * Sets if the current user can view observability for this client
     *
     * @param userCanViewObservability
     *            true if user can view observability for this client
     */
    public void setUserCanViewObservability( boolean userCanViewObservability )
    {
        _bUserCanViewObservability = userCanViewObservability;
    }

    /**
     * Gets the resource identifier for RBAC
     *
     * @return The resource identifier as string
     */
    @Override
    public String getResourceId( )
    {
        return StringUtils.EMPTY + _nId;
    }

    /**
     * Gets the resource type code for RBAC
     *
     * @return The resource type code
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }
}
