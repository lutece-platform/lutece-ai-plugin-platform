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
package fr.paris.lutece.plugins.platform.web.observability;

/**
 * View contract shared by every controller rendering the observability templates (resource summary, execution details) : FreeMarker marks, request parameters
 * and view names.
 */
public final class ObservabilityViewMarks
{
    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_AVG_DURATION = "avg_duration";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_DAILY_STATS_JSON = "daily_stats_json";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_DATE_RANGE = "date_range";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_EXECUTION = "execution";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_METRICS = "metrics";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_RESOURCE_ID = "resource_id";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_RESOURCE_TYPE = "resource_type";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_TOTAL_COST = "total_cost";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_TOTAL_ERRORS = "total_errors";

    /**
     * Shared FreeMarker mark of the observability templates.
     */
    public static final String MARK_TOTAL_EXECUTIONS = "total_executions";

    /**
     * Shared request parameter of the observability templates.
     */
    public static final String PARAMETER_DATE_RANGE = "date_range";

    /**
     * Shared request parameter of the observability templates.
     */
    public static final String PARAMETER_EXECUTION_ID = "execution_id";

    /**
     * Shared request parameter of the observability templates.
     */
    public static final String PARAMETER_RESOURCE_ID = "resource_id";

    /**
     * Shared request parameter of the observability templates.
     */
    public static final String PARAMETER_RESOURCE_TYPE = "resource_type";

    /**
     * Shared view name of the observability templates.
     */
    public static final String VIEW_EXECUTION_DETAILS = "executionDetails";

    /**
     * Private constructor to prevent instantiation.
     */
    private ObservabilityViewMarks( )
    {
    }
}
