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
package fr.paris.lutece.plugins.platform.business.observability;

import java.sql.Timestamp;

public class ResourceStats
{
    private String _strResourceType;
    private String _strResourceId;
    private double _dTotalCost;
    private int _nExecutionCount;
    private double _dSuccessRate;
    private Timestamp _tLastExecution;

    /**
     * Returns the resource type
     *
     * @return the resource type
     */
    public String getResourceType( )
    {
        return _strResourceType;
    }

    /**
     * Sets the resource type
     *
     * @param strResourceType
     *            the resource type
     */
    public void setResourceType( String strResourceType )
    {
        _strResourceType = strResourceType;
    }

    /**
     * Returns the resource id
     *
     * @return the resource id
     */
    public String getResourceId( )
    {
        return _strResourceId;
    }

    /**
     * Sets the resource id
     *
     * @param strResourceId
     *            the resource id
     */
    public void setResourceId( String strResourceId )
    {
        _strResourceId = strResourceId;
    }

    /**
     * Returns the total cost
     *
     * @return the total cost
     */
    public double getTotalCost( )
    {
        return _dTotalCost;
    }

    /**
     * Sets the total cost
     *
     * @param dTotalCost
     *            the total cost
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
     * Returns the last execution
     *
     * @return the last execution
     */
    public Timestamp getLastExecution( )
    {
        return _tLastExecution;
    }

    /**
     * Sets the last execution
     *
     * @param tLastExecution
     *            the last execution
     */
    public void setLastExecution( Timestamp tLastExecution )
    {
        _tLastExecution = tLastExecution;
    }
}
