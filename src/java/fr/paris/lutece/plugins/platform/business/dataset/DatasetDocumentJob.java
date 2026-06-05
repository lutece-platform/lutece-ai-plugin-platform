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
 * This class represents a document job in a dataset
 */
public class DatasetDocumentJob implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ERROR = "error";

    private int _nId;
    private int _nDocumentId;
    private int _nDatasetId;
    private String _strStatus;
    private String _strErrorMessage;
    private Timestamp _timestampCreated;
    private Timestamp _timestampUpdated;

    /**
     * Constructor
     */
    public DatasetDocumentJob( )
    {
        _strStatus = STATUS_PENDING;
    }

    /**
     * Get the job ID
     *
     * @return The job ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Set the job ID
     *
     * @param nId
     *            The job ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Get the document ID
     *
     * @return The document ID
     */
    public int getDocumentId( )
    {
        return _nDocumentId;
    }

    /**
     * Set the document ID
     *
     * @param nDocumentId
     *            The document ID
     */
    public void setDocumentId( int nDocumentId )
    {
        _nDocumentId = nDocumentId;
    }

    /**
     * Get the dataset ID
     *
     * @return The dataset ID
     */
    public int getDatasetId( )
    {
        return _nDatasetId;
    }

    /**
     * Set the dataset ID
     *
     * @param nDatasetId
     *            The dataset ID
     */
    public void setDatasetId( int nDatasetId )
    {
        _nDatasetId = nDatasetId;
    }

    /**
     * Get the job status
     *
     * @return The job status
     */
    public String getStatus( )
    {
        return _strStatus;
    }

    /**
     * Set the job status
     *
     * @param strStatus
     *            The job status
     */
    public void setStatus( String strStatus )
    {
        _strStatus = strStatus;
    }

    /**
     * Get the error message
     *
     * @return The error message
     */
    public String getErrorMessage( )
    {
        return _strErrorMessage;
    }

    /**
     * Set the error message
     *
     * @param strErrorMessage
     *            The error message
     */
    public void setErrorMessage( String strErrorMessage )
    {
        _strErrorMessage = strErrorMessage;
    }

    /**
     * Get the creation timestamp
     *
     * @return The creation timestamp
     */
    public Timestamp getCreated( )
    {
        return _timestampCreated;
    }

    /**
     * Set the creation timestamp
     *
     * @param timestampCreated
     *            The creation timestamp
     */
    public void setCreated( Timestamp timestampCreated )
    {
        _timestampCreated = timestampCreated;
    }

    /**
     * Get the update timestamp
     *
     * @return The update timestamp
     */
    public Timestamp getUpdated( )
    {
        return _timestampUpdated;
    }

    /**
     * Set the update timestamp
     *
     * @param timestampUpdated
     *            The update timestamp
     */
    public void setUpdated( Timestamp timestampUpdated )
    {
        _timestampUpdated = timestampUpdated;
    }
}
