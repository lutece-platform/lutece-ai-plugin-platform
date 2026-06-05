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
package fr.paris.lutece.plugins.platform.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import fr.paris.lutece.portal.service.database.AppConnectionService;
import fr.paris.lutece.test.LuteceTestCase;

/**
 * Base class for platform tests that touch the database. After each test, every {@code platform_*} table is wiped, so a failing assertion can never leak seeded
 * rows into the following tests. Tests keep their local Arrange seeding; their manual cleanup blocks become optional. The wipe relies on the HSQL
 * {@code SET REFERENTIAL_INTEGRITY} session command, which is fine because the suite only ever runs against the in-memory HSQL test database.
 */
public abstract class AbstractPlatformDbTest extends LuteceTestCase
{
    private static final String SQL_LIST_PLATFORM_TABLES = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_NAME LIKE 'PLATFORM\\_%' ESCAPE '\\'";
    private static final String SQL_DISABLE_REFERENTIAL_INTEGRITY = "SET DATABASE REFERENTIAL INTEGRITY FALSE";
    private static final String SQL_ENABLE_REFERENTIAL_INTEGRITY = "SET DATABASE REFERENTIAL INTEGRITY TRUE";
    private static final String SQL_DELETE_FROM = "DELETE FROM ";

    /**
     * Wipes all {@code platform_*} tables, keeping each test isolated even when an assertion fails before the test's own cleanup code runs.
     *
     * @throws Exception
     *             if the wipe fails
     */
    @AfterEach
    protected void wipePlatformTables( ) throws Exception
    {
        Connection connection = AppConnectionService.getConnection( );
        try ( Statement statement = connection.createStatement( ) )
        {
            List<String> tables = new ArrayList<>( );
            try ( ResultSet resultSet = statement.executeQuery( SQL_LIST_PLATFORM_TABLES ) )
            {
                while ( resultSet.next( ) )
                {
                    tables.add( resultSet.getString( 1 ) );
                }
            }
            statement.execute( SQL_DISABLE_REFERENTIAL_INTEGRITY );
            for ( String table : tables )
            {
                statement.executeUpdate( SQL_DELETE_FROM + table );
            }
            statement.execute( SQL_ENABLE_REFERENTIAL_INTEGRITY );
        }
        finally
        {
            AppConnectionService.freeConnection( connection );
        }
    }
}
