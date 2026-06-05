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
 * Mapping between an image blob stored in the Lutece FileStoreService and the decision tree it is referenced from. The {@code contentHash} is the sha256 of the
 * image bytes — it is deterministic, opaque, and used as the identifier inside the markdown ({@code lutece-image:<hash>}), so the identifier is stable across
 * instances (same bytes → same hash everywhere). The composite PK {@code (contentHash, treeId)} allows the same image to be referenced from several trees;
 * cascade on {@code treeId} cleans up references automatically when a tree is removed.
 */
public class DecisionTreeImage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String _strContentHash;
    private int _nTreeId;
    private String _strFileStoreKey;
    private String _strMimeType;
    private Timestamp _dateCreation;

    /**
     * Returns the sha256 hash of the image bytes — the stable, opaque identifier used in the markdown.
     *
     * @return the content hash
     */
    public String getContentHash( )
    {
        return _strContentHash;
    }

    /**
     * Sets the sha256 hash of the image bytes.
     *
     * @param strContentHash
     *            the content hash
     */
    public void setContentHash( String strContentHash )
    {
        _strContentHash = strContentHash;
    }

    /**
     * Returns the identifier of the decision tree that references this image.
     *
     * @return the tree id
     */
    public int getTreeId( )
    {
        return _nTreeId;
    }

    /**
     * Sets the identifier of the decision tree.
     *
     * @param nTreeId
     *            the tree id
     */
    public void setTreeId( int nTreeId )
    {
        _nTreeId = nTreeId;
    }

    /**
     * Returns the internal file store key — the identifier returned by {@link fr.paris.lutece.portal.service.file.IFileStoreServiceProvider#storeBytes(byte[])}
     * and used to retrieve the raw bytes. Never exposed in URLs or markdown.
     *
     * @return the file store key
     */
    public String getFileStoreKey( )
    {
        return _strFileStoreKey;
    }

    /**
     * Sets the internal file store key.
     *
     * @param strFileStoreKey
     *            the file store key
     */
    public void setFileStoreKey( String strFileStoreKey )
    {
        _strFileStoreKey = strFileStoreKey;
    }

    /**
     * Returns the MIME type of the image.
     *
     * @return the MIME type
     */
    public String getMimeType( )
    {
        return _strMimeType;
    }

    /**
     * Sets the MIME type of the image.
     *
     * @param strMimeType
     *            the MIME type
     */
    public void setMimeType( String strMimeType )
    {
        _strMimeType = strMimeType;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Timestamp getDateCreation( )
    {
        return _dateCreation;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param dateCreation
     *            the creation timestamp
     */
    public void setDateCreation( Timestamp dateCreation )
    {
        _dateCreation = dateCreation;
    }
}
