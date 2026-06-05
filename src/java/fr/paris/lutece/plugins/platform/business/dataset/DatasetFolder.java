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
package fr.paris.lutece.plugins.platform.business.dataset;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DatasetFolder — a logical folder inside a dataset. Folders form a recursive tree via parent_folder_id (NULL = root of dataset). Each folder holds a name
 * (unique among siblings) and an optional description used by built-in tools to hint the AI about its content.
 */
public class DatasetFolder implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nDatasetId;
    private Integer _nParentFolderId;
    private String _strName;
    private String _strDescription;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;

    /**
     * Returns the folder ID
     *
     * @return The folder ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the folder ID
     *
     * @param nId
     *            The folder ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the dataset ID this folder belongs to
     *
     * @return The dataset ID
     */
    public int getDatasetId( )
    {
        return _nDatasetId;
    }

    /**
     * Sets the dataset ID this folder belongs to
     *
     * @param nDatasetId
     *            The dataset ID
     */
    public void setDatasetId( int nDatasetId )
    {
        _nDatasetId = nDatasetId;
    }

    /**
     * Returns the parent folder ID (null = root of dataset)
     *
     * @return The parent folder ID, or null for root folders
     */
    public Integer getParentFolderId( )
    {
        return _nParentFolderId;
    }

    /**
     * Sets the parent folder ID (null = root of dataset)
     *
     * @param nParentFolderId
     *            The parent folder ID, or null for root folders
     */
    public void setParentFolderId( Integer nParentFolderId )
    {
        _nParentFolderId = nParentFolderId;
    }

    /**
     * Returns the folder name (unique among siblings of same parent)
     *
     * @return The folder name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the folder name
     *
     * @param strName
     *            The folder name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the folder description used to inform the AI about this folder content
     *
     * @return The folder description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the folder description
     *
     * @param strDescription
     *            The folder description
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }
}
