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
package fr.paris.lutece.plugins.platform.service.observability.dto;

import java.util.List;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.observability.DailyResourceStats;

/**
 * Aggregated observability statistics for a single client over a given date range. Carries everything the global observability view needs about one client.
 */
public class ClientObservabilityStats
{
    private Client _client;
    private int _nExecutionCount;
    private double _dTotalCost;
    private int _nErrorCount;
    private double _dSuccessRate;
    private double _dAvgDurationSeconds;
    private List<DailyResourceStats> _dailySeries;

    /**
     * Gets the underlying client.
     *
     * @return the client
     */
    public Client getClient( )
    {
        return _client;
    }

    /**
     * Sets the underlying client.
     *
     * @param client
     *            the client
     */
    public void setClient( Client client )
    {
        _client = client;
    }

    /**
     * Gets the total number of executions over the period.
     *
     * @return the execution count
     */
    public int getExecutionCount( )
    {
        return _nExecutionCount;
    }

    /**
     * Sets the total number of executions over the period.
     *
     * @param nExecutionCount
     *            the execution count
     */
    public void setExecutionCount( int nExecutionCount )
    {
        _nExecutionCount = nExecutionCount;
    }

    /**
     * Gets the total cost over the period.
     *
     * @return the total cost
     */
    public double getTotalCost( )
    {
        return _dTotalCost;
    }

    /**
     * Sets the total cost over the period.
     *
     * @param dTotalCost
     *            the total cost
     */
    public void setTotalCost( double dTotalCost )
    {
        _dTotalCost = dTotalCost;
    }

    /**
     * Gets the number of failed executions over the period.
     *
     * @return the error count
     */
    public int getErrorCount( )
    {
        return _nErrorCount;
    }

    /**
     * Sets the number of failed executions over the period.
     *
     * @param nErrorCount
     *            the error count
     */
    public void setErrorCount( int nErrorCount )
    {
        _nErrorCount = nErrorCount;
    }

    /**
     * Gets the success rate (0..100) over the period.
     *
     * @return the success rate as a percentage
     */
    public double getSuccessRate( )
    {
        return _dSuccessRate;
    }

    /**
     * Sets the success rate (0..100) over the period.
     *
     * @param dSuccessRate
     *            the success rate as a percentage
     */
    public void setSuccessRate( double dSuccessRate )
    {
        _dSuccessRate = dSuccessRate;
    }

    /**
     * Gets the average execution duration in seconds.
     *
     * @return the average duration
     */
    public double getAvgDurationSeconds( )
    {
        return _dAvgDurationSeconds;
    }

    /**
     * Sets the average execution duration in seconds.
     *
     * @param dAvgDurationSeconds
     *            the average duration
     */
    public void setAvgDurationSeconds( double dAvgDurationSeconds )
    {
        _dAvgDurationSeconds = dAvgDurationSeconds;
    }

    /**
     * Gets the daily breakdown for this client.
     *
     * @return the daily resource statistics
     */
    public List<DailyResourceStats> getDailySeries( )
    {
        return _dailySeries;
    }

    /**
     * Sets the daily breakdown for this client.
     *
     * @param dailySeries
     *            the daily resource statistics
     */
    public void setDailySeries( List<DailyResourceStats> dailySeries )
    {
        _dailySeries = dailySeries;
    }
}
