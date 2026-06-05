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

import java.io.InputStream;
import java.io.Serializable;
import jakarta.validation.constraints.NotEmpty;

/**
 * Class representing a document associated with a dataset
 */
public class DatasetDocument implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    @NotEmpty( message = "#i18n{platform.agent.validation.datasetDocument.Name.notEmpty}" )
    private String _strName;
    private String _strDescription;
    private String _strFileKey;
    private int _nDatasetId;
    private InputStream _file;
    private boolean _bUseDocumentIntelligence;
    private Integer _nDocumentIntelligenceProviderId;
    private int _nChunkSize = 1000;
    private int _nChunkOverlap = 200;
    private Integer _nFolderId;
    private String _strFullContent;

    /**
     * Returns the ID of this document
     *
     * @return The document ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the ID of this document
     *
     * @param nId
     *            The ID to set
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the name of this document
     *
     * @return The document name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the name of this document
     *
     * @param strName
     *            The name to set
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the description of this document
     *
     * @return The document description
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the description of this document
     *
     * @param strDescription
     *            The description to set
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the dataset ID that this document belongs to
     *
     * @return The dataset ID
     */
    public int getDatasetId( )
    {
        return _nDatasetId;
    }

    /**
     * Sets the dataset ID that this document belongs to
     *
     * @param nDatasetId
     *            The dataset ID to set
     */
    public void setDatasetId( int nDatasetId )
    {
        _nDatasetId = nDatasetId;
    }

    /**
     * Returns the file content as an input stream
     *
     * @return The file input stream
     */
    public InputStream getFile( )
    {
        return _file;
    }

    /**
     * Sets the file content as an input stream
     *
     * @param file
     *            The file input stream to set
     */
    public void setFile( InputStream file )
    {
        _file = file;
    }

    /**
     * Returns the file key used to identify the stored file
     *
     * @return The file key
     */
    public String getFileKey( )
    {
        return _strFileKey;
    }

    /**
     * Sets the file key used to identify the stored file
     *
     * @param strFileKey
     *            The file key to set
     */
    public void setFileKey( String strFileKey )
    {
        _strFileKey = strFileKey;
    }

    /**
     * Returns whether to use Document Intelligence for parsing
     *
     * @return true if Document Intelligence should be used
     */
    public boolean getUseDocumentIntelligence( )
    {
        return _bUseDocumentIntelligence;
    }

    /**
     * Sets whether to use Document Intelligence for parsing
     *
     * @param bUseDocumentIntelligence
     *            true if Document Intelligence should be used
     */
    public void setUseDocumentIntelligence( boolean bUseDocumentIntelligence )
    {
        _bUseDocumentIntelligence = bUseDocumentIntelligence;
    }

    /**
     * Returns the Document Intelligence provider ID
     *
     * @return The provider ID or null if not set
     */
    public Integer getDocumentIntelligenceProviderId( )
    {
        return _nDocumentIntelligenceProviderId;
    }

    /**
     * Sets the Document Intelligence provider ID
     *
     * @param nDocumentIntelligenceProviderId
     *            The provider ID to set
     */
    public void setDocumentIntelligenceProviderId( Integer nDocumentIntelligenceProviderId )
    {
        _nDocumentIntelligenceProviderId = nDocumentIntelligenceProviderId;
    }

    /**
     * Returns the chunk size for text segmentation
     *
     * @return The chunk size
     */
    public int getChunkSize( )
    {
        return _nChunkSize;
    }

    /**
     * Sets the chunk size for text segmentation
     *
     * @param nChunkSize
     *            The chunk size to set
     */
    public void setChunkSize( int nChunkSize )
    {
        _nChunkSize = nChunkSize;
    }

    /**
     * Returns the chunk overlap for text segmentation
     *
     * @return The chunk overlap
     */
    public int getChunkOverlap( )
    {
        return _nChunkOverlap;
    }

    /**
     * Sets the chunk overlap for text segmentation
     *
     * @param nChunkOverlap
     *            The chunk overlap to set
     */
    public void setChunkOverlap( int nChunkOverlap )
    {
        _nChunkOverlap = nChunkOverlap;
    }

    /**
     * Returns the folder this document belongs to (null = root of dataset)
     *
     * @return The folder ID, or null for root
     */
    public Integer getFolderId( )
    {
        return _nFolderId;
    }

    /**
     * Sets the folder this document belongs to (null = root of dataset)
     *
     * @param nFolderId
     *            The folder ID, or null for root
     */
    public void setFolderId( Integer nFolderId )
    {
        _nFolderId = nFolderId;
    }

    /**
     * Returns the full parsed text content of the document Used by built-in tools (read, grep) to access complete content without re-parsing
     *
     * @return The full content text or null if not yet parsed
     */
    public String getFullContent( )
    {
        return _strFullContent;
    }

    /**
     * Sets the full parsed text content of the document
     *
     * @param strFullContent
     *            The full content text to set
     */
    public void setFullContent( String strFullContent )
    {
        _strFullContent = strFullContent;
    }
}
