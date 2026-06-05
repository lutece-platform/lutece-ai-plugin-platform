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
package fr.paris.lutece.plugins.platform.business.vision;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;

public class Vision implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "VISION";

    private int nId;

    @NotEmpty( message = "#i18n{platform.agent.validation.vision.VisionTitle.notEmpty}" )
    private String strVisionTitle;

    private String strVisionDescription;
    private int nProviderId;
    private int nClientId;
    private Timestamp timestampCreatedAt;
    private Timestamp timestampUpdatedAt;
    private List<VisionExtractor> extractors = new ArrayList<>( );

    /**
     * Gets the vision identifier
     *
     * @return the vision identifier
     */
    public int getId( )
    {
        return nId;
    }

    /**
     * Sets the vision identifier
     *
     * @param nId
     *            the vision identifier to set
     */
    public void setId( int nId )
    {
        this.nId = nId;
    }

    /**
     * Gets the vision title
     *
     * @return the vision title
     */
    public String getVisionTitle( )
    {
        return strVisionTitle;
    }

    /**
     * Sets the vision title
     *
     * @param strVisionTitle
     *            the vision title to set
     */
    public void setVisionTitle( String strVisionTitle )
    {
        this.strVisionTitle = strVisionTitle;
    }

    /**
     * Gets the vision description
     *
     * @return the vision description
     */
    public String getVisionDescription( )
    {
        return strVisionDescription;
    }

    /**
     * Sets the vision description
     *
     * @param strVisionDescription
     *            the vision description to set
     */
    public void setVisionDescription( String strVisionDescription )
    {
        this.strVisionDescription = strVisionDescription;
    }

    /**
     * Gets the provider identifier
     *
     * @return the provider identifier
     */
    public int getProviderId( )
    {
        return nProviderId;
    }

    /**
     * Sets the provider identifier
     *
     * @param nProviderId
     *            the provider identifier to set
     */
    public void setProviderId( int nProviderId )
    {
        this.nProviderId = nProviderId;
    }

    /**
     * Gets the client identifier
     *
     * @return the client identifier
     */
    public int getClientId( )
    {
        return nClientId;
    }

    /**
     * Sets the client identifier
     *
     * @param nClientId
     *            the client identifier to set
     */
    public void setClientId( int nClientId )
    {
        this.nClientId = nClientId;
    }

    /**
     * Gets the creation timestamp
     *
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param timestampCreatedAt
     *            the creation timestamp to set
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        this.timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Gets the update timestamp
     *
     * @return the update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return timestampUpdatedAt;
    }

    /**
     * Sets the update timestamp
     *
     * @param timestampUpdatedAt
     *            the update timestamp to set
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        this.timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Gets the list of vision extractors
     *
     * @return the list of vision extractors
     */
    public List<VisionExtractor> getExtractors( )
    {
        return extractors;
    }

    /**
     * Sets the list of vision extractors
     *
     * @param extractors
     *            the list of vision extractors to set
     */
    public void setExtractors( List<VisionExtractor> extractors )
    {
        this.extractors = extractors;
    }

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    /**
     * Checks if the current user can view this vision
     *
     * @return true if user can view this vision
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this vision
     *
     * @param userCanView
     *            true if user can view this vision
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this vision
     *
     * @return true if user can modify this vision
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this vision
     *
     * @param userCanModify
     *            true if user can modify this vision
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this vision
     *
     * @return true if user can delete this vision
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this vision
     *
     * @param userCanDelete
     *            true if user can delete this vision
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
        return String.valueOf( nId );
    }
}
