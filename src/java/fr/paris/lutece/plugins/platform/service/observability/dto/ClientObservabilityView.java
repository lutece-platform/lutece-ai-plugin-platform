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
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.resource.PlatformResourceItem;

/**
 * Typed view model for a single client's observability dashboard. Carries the resource types, the stats-enriched resources grouped by type, the per-type
 * observability URLs, the aggregated KPI block (total executions, total cost, total errors, global success rate, average duration in seconds) and the typed
 * daily chart points. The whole nested aggregation that previously lived in the controller now lands here as a single typed result.
 */
public class ClientObservabilityView
{
    private final List<IPlatformResourceType> _resourceTypes;
    private final Map<String, List<PlatformResourceItem>> _resourcesByType;
    private final Map<String, Map<String, String>> _observabilityUrlsByType;
    private final int _nTotalExecutions;
    private final double _dTotalCost;
    private final int _nTotalErrors;
    private final double _dGlobalSuccessRate;
    private final double _dAvgDurationSeconds;
    private final List<DailyStatPoint> _dailyPoints;

    /**
     * Builds a client observability view.
     *
     * @param resourceTypes
     *            the available resource types
     * @param resourcesByType
     *            the stats-enriched resources grouped by resource type
     * @param observabilityUrlsByType
     *            the per-type observability URLs, by resource type then resource id
     * @param nTotalExecutions
     *            the total number of executions over the period
     * @param dTotalCost
     *            the total cost over the period
     * @param nTotalErrors
     *            the total number of failed executions over the period
     * @param dGlobalSuccessRate
     *            the global success rate (0..100) over the period
     * @param dAvgDurationSeconds
     *            the average execution duration in seconds
     * @param dailyPoints
     *            the typed daily chart points
     */
    public ClientObservabilityView( List<IPlatformResourceType> resourceTypes, Map<String, List<PlatformResourceItem>> resourcesByType,
            Map<String, Map<String, String>> observabilityUrlsByType, int nTotalExecutions, double dTotalCost, int nTotalErrors, double dGlobalSuccessRate,
            double dAvgDurationSeconds, List<DailyStatPoint> dailyPoints )
    {
        _resourceTypes = resourceTypes;
        _resourcesByType = resourcesByType;
        _observabilityUrlsByType = observabilityUrlsByType;
        _nTotalExecutions = nTotalExecutions;
        _dTotalCost = dTotalCost;
        _nTotalErrors = nTotalErrors;
        _dGlobalSuccessRate = dGlobalSuccessRate;
        _dAvgDurationSeconds = dAvgDurationSeconds;
        _dailyPoints = dailyPoints;
    }

    /**
     * Gets the available resource types.
     *
     * @return the resource types
     */
    public List<IPlatformResourceType> getResourceTypes( )
    {
        return _resourceTypes;
    }

    /**
     * Gets the stats-enriched resources grouped by resource type.
     *
     * @return the resources grouped by type
     */
    public Map<String, List<PlatformResourceItem>> getResourcesByType( )
    {
        return _resourcesByType;
    }

    /**
     * Gets the per-type observability URLs, by resource type then resource id.
     *
     * @return the observability URLs
     */
    public Map<String, Map<String, String>> getObservabilityUrlsByType( )
    {
        return _observabilityUrlsByType;
    }

    /**
     * Gets the total number of executions over the period.
     *
     * @return the total executions
     */
    public int getTotalExecutions( )
    {
        return _nTotalExecutions;
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
     * Gets the total number of failed executions over the period.
     *
     * @return the total errors
     */
    public int getTotalErrors( )
    {
        return _nTotalErrors;
    }

    /**
     * Gets the global success rate (0..100) over the period.
     *
     * @return the global success rate
     */
    public double getGlobalSuccessRate( )
    {
        return _dGlobalSuccessRate;
    }

    /**
     * Gets the average execution duration in seconds.
     *
     * @return the average duration in seconds
     */
    public double getAvgDurationSeconds( )
    {
        return _dAvgDurationSeconds;
    }

    /**
     * Gets the typed daily chart points.
     *
     * @return the daily points
     */
    public List<DailyStatPoint> getDailyPoints( )
    {
        return _dailyPoints;
    }
}
