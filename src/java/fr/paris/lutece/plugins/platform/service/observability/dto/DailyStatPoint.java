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

import java.sql.Date;

/**
 * Typed chart point for the resource observability daily series. Defines the contract of one point in the chart payload (day, count, cost, average duration,
 * errors) so the controller only serializes this DTO with Jackson instead of hand-projecting a list of maps.
 */
public class DailyStatPoint
{
    private final Date day;
    private final int count;
    private final double cost;
    private final double avgDuration;
    private final int errors;

    /**
     * Builds a daily stat point.
     *
     * @param day
     *            the day
     * @param count
     *            the execution count for that day
     * @param cost
     *            the total cost for that day
     * @param avgDuration
     *            the average duration in seconds for that day
     * @param errors
     *            the error count for that day
     */
    public DailyStatPoint( Date day, int count, double cost, double avgDuration, int errors )
    {
        this.day = day;
        this.count = count;
        this.cost = cost;
        this.avgDuration = avgDuration;
        this.errors = errors;
    }

    /**
     * Gets the day.
     *
     * @return the day
     */
    public Date getDay( )
    {
        return day;
    }

    /**
     * Gets the execution count for that day.
     *
     * @return the count
     */
    public int getCount( )
    {
        return count;
    }

    /**
     * Gets the total cost for that day.
     *
     * @return the cost
     */
    public double getCost( )
    {
        return cost;
    }

    /**
     * Gets the average duration in seconds for that day.
     *
     * @return the average duration
     */
    public double getAvgDuration( )
    {
        return avgDuration;
    }

    /**
     * Gets the error count for that day.
     *
     * @return the errors
     */
    public int getErrors( )
    {
        return errors;
    }
}
