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
 * Typed per-client chart series for the global observability chart. One series carries the client id, the client name and its daily points. The controller
 * serializes a list of these with Jackson instead of hand-building a list of maps.
 */
public class ClientChartSeries
{
    private final int _nId;
    private final String _strName;
    private final List<DailyStatPoint> _daily;

    /**
     * Builds a client chart series.
     *
     * @param nId
     *            the client id
     * @param strName
     *            the client name
     * @param daily
     *            the daily chart points
     */
    public ClientChartSeries( int nId, String strName, List<DailyStatPoint> daily )
    {
        _nId = nId;
        _strName = strName;
        _daily = daily;
    }

    /**
     * Gets the client id.
     *
     * @return the id
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Gets the client name.
     *
     * @return the name
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Gets the daily chart points.
     *
     * @return the daily points
     */
    public List<DailyStatPoint> getDaily( )
    {
        return _daily;
    }
}
