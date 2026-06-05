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
package fr.paris.lutece.plugins.platform.service.observability;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecution;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionHome;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceExecutionStatus;
import fr.paris.lutece.plugins.platform.business.observability.ResourceStats;
import fr.paris.lutece.plugins.platform.service.observability.dto.DateRange;
import fr.paris.lutece.plugins.platform.service.observability.dto.ResourceObservabilitySummary;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link ObservabilityService}. These exercise the pure {@link DateRange} bound resolution and the Home-backed aggregation paths
 * ({@link ObservabilityService#getStatsForResource} and {@link ObservabilityService#getResourceSummary}) against HSQL, seeding
 * {@link PlatformResourceExecution} rows directly via the Home to control status, cost and timing without relying on the asynchronous write-behind path.
 * Elasticsearch and chart-serialization paths are out of scope and not exercised here.
 */
public class ObservabilityServiceTest extends AbstractPlatformDbTest
{
    private static final String RESOURCE_TYPE = "BOT";

    /**
     * Resolves the CDI-managed observability service.
     *
     * @return the observability service instance
     */
    private ObservabilityService service( )
    {
        return CDI.current( ).select( ObservabilityService.class ).get( );
    }

    /**
     * Creates and persists an active parent client with the given code.
     *
     * @param code
     *            the unique client code
     * @return the persisted client
     */
    private Client createClient( String code )
    {
        Client client = new Client( );
        client.setName( "Client " + code );
        client.setCode( code );
        client.setActive( true );
        return ClientHome.create( client );
    }

    /**
     * Creates and persists a resource execution with explicit status, cost and timing, then returns its generated id.
     *
     * @param clientId
     *            the parent client id
     * @param resourceId
     *            the resource id
     * @param status
     *            the execution status
     * @param cost
     *            the total cost
     * @param startEpochMs
     *            the start time epoch in milliseconds
     * @param endEpochMs
     *            the end time epoch in milliseconds (use a negative value to leave the end time null)
     * @return the persisted execution id
     */
    private String createExecution( int clientId, String resourceId, PlatformResourceExecutionStatus status, BigDecimal cost, long startEpochMs,
            long endEpochMs )
    {
        PlatformResourceExecution execution = new PlatformResourceExecution( );
        execution.setResourceType( RESOURCE_TYPE );
        execution.setResourceId( resourceId );
        execution.setClientId( clientId );
        execution.setStatus( status );
        execution.setStartTime( new Timestamp( startEpochMs ) );
        if ( endEpochMs >= 0 )
        {
            execution.setEndTime( new Timestamp( endEpochMs ) );
        }
        execution.setTotalCost( cost );
        return PlatformResourceExecutionHome.create( execution );
    }

    /**
     * The all-time range resolves to a lower bound roughly ten years in the past and an end bound at the current instant, while the bounded ranges resolve to a
     * lower bound strictly before now and an end bound at now.
     */
    @Test
    public void testDateRangeResolveBounds( )
    {
        long now = System.currentTimeMillis( );

        Timestamp [ ] all = DateRange.ALL.resolveBounds( );
        assertTrue( all [0].before( all [1] ), "ALL start must precede end" );
        assertTrue( all [0].getTime( ) < now - 8L * 365 * 24 * 3600 * 1000, "ALL start must be several years in the past" );

        for ( DateRange range : new DateRange [ ] {
                DateRange.SEVEN_DAYS, DateRange.FIFTEEN_DAYS, DateRange.MONTH, DateRange.THREE_MONTHS
        } )
        {
            Timestamp [ ] bounds = range.resolveBounds( );
            assertTrue( bounds [0].before( bounds [1] ), range.getKey( ) + " start must precede end" );
            assertTrue( bounds [0].getTime( ) < now, range.getKey( ) + " start must be in the past" );
        }

        Timestamp [ ] seven = DateRange.SEVEN_DAYS.resolveBounds( );
        Timestamp [ ] fifteen = DateRange.FIFTEEN_DAYS.resolveBounds( );
        assertTrue( fifteen [0].before( seven [0] ), "15days lower bound must be earlier than 7days lower bound" );
    }

    /**
     * Each request/template key maps to the matching range, and an unknown, null or empty key falls back to ALL.
     */
    @Test
    public void testDateRangeFromKey( )
    {
        assertEquals( DateRange.SEVEN_DAYS, DateRange.fromKey( "7days" ) );
        assertEquals( DateRange.FIFTEEN_DAYS, DateRange.fromKey( "15days" ) );
        assertEquals( DateRange.MONTH, DateRange.fromKey( "month" ) );
        assertEquals( DateRange.THREE_MONTHS, DateRange.fromKey( "3months" ) );
        assertEquals( DateRange.ALL, DateRange.fromKey( "all" ) );
        assertEquals( DateRange.ALL, DateRange.fromKey( "unknown" ) );
        assertEquals( DateRange.ALL, DateRange.fromKey( null ) );
        assertEquals( DateRange.ALL, DateRange.fromKey( "" ) );
    }

    /**
     * The per-resource stats aggregate the seeded executions of one resource: count, summed cost, success rate (only COMPLETED counts as success) and the most
     * recent start time.
     */
    @Test
    public void testGetStatsForResourceAggregates( )
    {
        Client client = createClient( "obs-svc-stats-" + System.currentTimeMillis( ) );
        long base = System.currentTimeMillis( );

        createExecution( client.getId( ), "10", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "1.00" ), base, base + 1000 );
        createExecution( client.getId( ), "10", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "2.00" ), base + 5000, base + 7000 );
        createExecution( client.getId( ), "10", PlatformResourceExecutionStatus.ERROR, new BigDecimal( "0.50" ), base + 10000, base + 11000 );

        Optional<ResourceStats> opt = service( ).getStatsForResource( client.getId( ), RESOURCE_TYPE, "10" );
        assertTrue( opt.isPresent( ), "stats must be present for the seeded resource" );

        ResourceStats stats = opt.get( );
        assertEquals( 3, stats.getExecutionCount( ), "three executions seeded" );
        assertEquals( 3.5, stats.getTotalCost( ), 0.0001, "cost is the sum of the three execution costs" );
        assertEquals( 200.0 / 3.0, stats.getSuccessRate( ), 0.0001, "two of three executions completed successfully" );
        assertEquals( RESOURCE_TYPE, stats.getResourceType( ), "resource type echoed back" );
        assertEquals( "10", stats.getResourceId( ), "resource id echoed back" );
    }

    /**
     * A resource with no seeded execution yields no stats entry.
     */
    @Test
    public void testGetStatsForResourceEmptyWhenNoExecution( )
    {
        Client client = createClient( "obs-svc-empty-" + System.currentTimeMillis( ) );

        Optional<ResourceStats> opt = service( ).getStatsForResource( client.getId( ), RESOURCE_TYPE, "999" );
        assertTrue( opt.isEmpty( ), "no execution seeded so no stats expected" );
    }

    /**
     * The all-time resource summary aggregates the seeded executions of one resource into the KPI block: total count, summed cost, error count, success rate
     * and the duration-weighted average derived from start/end times.
     */
    @Test
    public void testGetResourceSummaryAggregates( )
    {
        Client client = createClient( "obs-svc-summary-" + System.currentTimeMillis( ) );
        long base = System.currentTimeMillis( );

        createExecution( client.getId( ), "20", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "1.50" ), base, base + 2000 );
        createExecution( client.getId( ), "20", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "0.50" ), base + 3000, base + 7000 );
        createExecution( client.getId( ), "20", PlatformResourceExecutionStatus.ERROR, new BigDecimal( "0.00" ), base + 8000, base + 9000 );
        createExecution( client.getId( ), "20", PlatformResourceExecutionStatus.RUNNING, BigDecimal.ZERO, base + 10000, -1 );

        ResourceObservabilitySummary summary = service( ).getResourceSummary( RESOURCE_TYPE, "20", client.getId( ), DateRange.ALL );

        assertEquals( 4, summary.getTotalExecutions( ), "four executions match the resource" );
        assertEquals( 2.0, summary.getTotalCost( ), 0.0001, "cost is the sum across the four executions" );
        assertEquals( 1, summary.getTotalErrors( ), "one execution is in ERROR" );
        assertEquals( 50.0, summary.getSuccessRate( ), 0.0001, "two of four executions completed" );
        assertEquals( 7.0 / 3.0, summary.getAvgDurationSeconds( ), 0.0001, "average duration over the three timed executions" );
        assertEquals( 4, summary.getExecutions( ).size( ), "the matching executions are carried in the summary" );
    }

    /**
     * The resource summary client filter scopes the aggregation to a single client: executions of another client on the same resource id are excluded.
     */
    @Test
    public void testGetResourceSummaryFiltersByClient( )
    {
        Client clientA = createClient( "obs-svc-filter-a-" + System.currentTimeMillis( ) );
        Client clientB = createClient( "obs-svc-filter-b-" + System.currentTimeMillis( ) );
        long base = System.currentTimeMillis( );

        List<String> ids = new ArrayList<>( );
        ids.add( createExecution( clientA.getId( ), "30", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "1.00" ), base, base + 1000 ) );
        ids.add( createExecution( clientA.getId( ), "30", PlatformResourceExecutionStatus.ERROR, new BigDecimal( "1.00" ), base + 2000, base + 3000 ) );
        ids.add( createExecution( clientB.getId( ), "30", PlatformResourceExecutionStatus.COMPLETED, new BigDecimal( "5.00" ), base + 4000, base + 5000 ) );

        ResourceObservabilitySummary summary = service( ).getResourceSummary( RESOURCE_TYPE, "30", clientA.getId( ), DateRange.ALL );
        assertEquals( 2, summary.getTotalExecutions( ), "only client A executions are counted" );
        assertEquals( 2.0, summary.getTotalCost( ), 0.0001, "client B cost is excluded" );
    }
}
