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

import java.sql.Date;

public class DailyResourceStats
{
    private Date _dateDay;
    private int _nExecutionCount;
    private double _dTotalCost;
    private double _dAvgDurationSeconds;
    private int _nErrorCount;

    /**
     * Returns the day
     *
     * @return the day
     */
    public Date getDay( )
    {
        return _dateDay;
    }

    /**
     * Sets the day
     *
     * @param dateDay
     *            the day
     */
    public void setDay( Date dateDay )
    {
        _dateDay = dateDay;
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
     * Returns the average duration in seconds
     *
     * @return the average duration in seconds
     */
    public double getAvgDurationSeconds( )
    {
        return _dAvgDurationSeconds;
    }

    /**
     * Sets the average duration in seconds
     *
     * @param dAvgDurationSeconds
     *            the average duration in seconds
     */
    public void setAvgDurationSeconds( double dAvgDurationSeconds )
    {
        _dAvgDurationSeconds = dAvgDurationSeconds;
    }

    /**
     * Returns the error count
     *
     * @return the error count
     */
    public int getErrorCount( )
    {
        return _nErrorCount;
    }

    /**
     * Sets the error count
     *
     * @param nErrorCount
     *            the error count
     */
    public void setErrorCount( int nErrorCount )
    {
        _nErrorCount = nErrorCount;
    }
}
