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

import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;

/**
 * Per-resource observability summary over a given date range. Carries the executions used for the listing plus the aggregated KPI block (total executions,
 * total cost, success rate, total errors, average duration) that the resource observability view used to compute inline in the controller.
 */
public class ResourceObservabilitySummary
{
    private List<PlatformResourceExecution> _executions;
    private int _nTotalExecutions;
    private double _dTotalCost;
    private double _dSuccessRate;
    private int _nTotalErrors;
    private double _dAvgDurationSeconds;

    /**
     * Gets the executions matching the resource and date range.
     *
     * @return the executions
     */
    public List<PlatformResourceExecution> getExecutions( )
    {
        return _executions;
    }

    /**
     * Sets the executions matching the resource and date range.
     *
     * @param executions
     *            the executions
     */
    public void setExecutions( List<PlatformResourceExecution> executions )
    {
        _executions = executions;
    }

    /**
     * Gets the total number of executions.
     *
     * @return the total executions
     */
    public int getTotalExecutions( )
    {
        return _nTotalExecutions;
    }

    /**
     * Sets the total number of executions.
     *
     * @param nTotalExecutions
     *            the total executions
     */
    public void setTotalExecutions( int nTotalExecutions )
    {
        _nTotalExecutions = nTotalExecutions;
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
     * Gets the success rate (0..100) over the period.
     *
     * @return the success rate
     */
    public double getSuccessRate( )
    {
        return _dSuccessRate;
    }

    /**
     * Sets the success rate (0..100) over the period.
     *
     * @param dSuccessRate
     *            the success rate
     */
    public void setSuccessRate( double dSuccessRate )
    {
        _dSuccessRate = dSuccessRate;
    }

    /**
     * Gets the number of failed executions over the period.
     *
     * @return the total errors
     */
    public int getTotalErrors( )
    {
        return _nTotalErrors;
    }

    /**
     * Sets the number of failed executions over the period.
     *
     * @param nTotalErrors
     *            the total errors
     */
    public void setTotalErrors( int nTotalErrors )
    {
        _nTotalErrors = nTotalErrors;
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
}
