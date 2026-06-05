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

/**
 * Aggregated observability statistics across all clients accessible to a user. Returned by ObservabilityService.getGlobalStats and consumed by views, REST
 * endpoints, batch reports, etc.
 */
public class GlobalObservabilityStats
{
    private int _nTotalExecutions;
    private double _dTotalCost;
    private int _nTotalErrors;
    private double _dGlobalSuccessRate;
    private double _dAvgDurationSeconds;
    private List<ClientObservabilityStats> _clientStats;

    /**
     * Gets the total executions across all clients.
     *
     * @return the total executions
     */
    public int getTotalExecutions( )
    {
        return _nTotalExecutions;
    }

    /**
     * Sets the total executions across all clients.
     *
     * @param nTotalExecutions
     *            the total executions
     */
    public void setTotalExecutions( int nTotalExecutions )
    {
        _nTotalExecutions = nTotalExecutions;
    }

    /**
     * Gets the total cost across all clients.
     *
     * @return the total cost
     */
    public double getTotalCost( )
    {
        return _dTotalCost;
    }

    /**
     * Sets the total cost across all clients.
     *
     * @param dTotalCost
     *            the total cost
     */
    public void setTotalCost( double dTotalCost )
    {
        _dTotalCost = dTotalCost;
    }

    /**
     * Gets the total error count across all clients.
     *
     * @return the total errors
     */
    public int getTotalErrors( )
    {
        return _nTotalErrors;
    }

    /**
     * Sets the total error count across all clients.
     *
     * @param nTotalErrors
     *            the total errors
     */
    public void setTotalErrors( int nTotalErrors )
    {
        _nTotalErrors = nTotalErrors;
    }

    /**
     * Gets the global success rate (0..100).
     *
     * @return the global success rate
     */
    public double getGlobalSuccessRate( )
    {
        return _dGlobalSuccessRate;
    }

    /**
     * Sets the global success rate (0..100).
     *
     * @param dGlobalSuccessRate
     *            the global success rate
     */
    public void setGlobalSuccessRate( double dGlobalSuccessRate )
    {
        _dGlobalSuccessRate = dGlobalSuccessRate;
    }

    /**
     * Gets the average execution duration in seconds across all clients.
     *
     * @return the average duration
     */
    public double getAvgDurationSeconds( )
    {
        return _dAvgDurationSeconds;
    }

    /**
     * Sets the average execution duration in seconds across all clients.
     *
     * @param dAvgDurationSeconds
     *            the average duration
     */
    public void setAvgDurationSeconds( double dAvgDurationSeconds )
    {
        _dAvgDurationSeconds = dAvgDurationSeconds;
    }

    /**
     * Gets the per-client breakdown.
     *
     * @return the client statistics list
     */
    public List<ClientObservabilityStats> getClientStats( )
    {
        return _clientStats;
    }

    /**
     * Sets the per-client breakdown.
     *
     * @param clientStats
     *            the client statistics list
     */
    public void setClientStats( List<ClientObservabilityStats> clientStats )
    {
        _clientStats = clientStats;
    }
}
