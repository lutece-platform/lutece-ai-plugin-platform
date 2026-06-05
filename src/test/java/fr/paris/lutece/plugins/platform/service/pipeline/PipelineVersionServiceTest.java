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
package fr.paris.lutece.plugins.platform.service.pipeline;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.pipeline.Pipeline;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineHome;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersion;
import fr.paris.lutece.plugins.platform.business.pipeline.PipelineVersionHome;
import fr.paris.lutece.plugins.platform.service.exception.CannotRemoveCurrentVersionException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Behavioural tests for {@link PipelineVersionService}, a static facade over {@link PipelineHome} and {@link PipelineVersionHome}. These exercise the version
 * numbering algorithm (max + 0.1, locale-comma tolerance, default), the orchestrated pipeline-with-initial-version creation, cloning a new non-current version
 * from the current one, and the invariant that the current version cannot be removed. All assertions run against HSQL through the real Home layer.
 */
public class PipelineVersionServiceTest extends AbstractPlatformDbTest
{
    /**
     * Creates and persists a Client to satisfy the id_client foreign key of the pipeline.
     *
     * @param code
     *            the unique client code
     * @return the persisted client with its generated id set
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
     * Creates and persists a pipeline owned by the given client, with its id set.
     *
     * @param name
     *            the pipeline name
     * @param clientId
     *            the owning client identifier
     * @return the persisted pipeline
     */
    private Pipeline createPipeline( String name, int clientId )
    {
        Pipeline pipeline = new Pipeline( );
        pipeline.setName( name );
        pipeline.setDescription( "desc " + name );
        pipeline.setMaxConcurrentWorkers( 2 );
        pipeline.setRateLimitByUserByDay( 10 );
        pipeline.setIdClient( clientId );
        int id = PipelineHome.create( pipeline );
        pipeline.setId( id );
        return pipeline;
    }

    /**
     * Creates and persists a pipeline version with the given name and current flag.
     *
     * @param pipelineId
     *            the parent pipeline identifier
     * @param versionName
     *            the version name
     * @param current
     *            whether the version is the current one
     * @return the persisted version with its id set
     */
    private PipelineVersion createVersion( int pipelineId, String versionName, boolean current )
    {
        PipelineVersion version = new PipelineVersion( );
        version.setIdPipeline( pipelineId );
        version.setVersionName( versionName );
        version.setDescription( "v " + versionName );
        version.setFlow( "{}" );
        version.setCreationDate( new Timestamp( new Date( ).getTime( ) ) );
        version.setCurrent( current );
        int id = PipelineVersionHome.create( version );
        version.setId( id );
        return version;
    }

    /**
     * With no existing version, the next version name falls back to the default "1.0".
     */
    @Test
    public void testComputeNextVersionNameDefaultWhenNone( )
    {
        Client client = createClient( "pv-svc-default" );
        Pipeline pipeline = createPipeline( "pv-svc-default", client.getId( ) );

        assertEquals( "1.0", PipelineVersionService.computeNextVersionName( pipeline.getId( ) ), "Empty pipeline must yield the default version name" );

    }

    /**
     * The next version name increments the highest existing numeric version by 0.1, formatted with a US decimal point.
     */
    @Test
    public void testComputeNextVersionNameIncrementsHighest( )
    {
        Client client = createClient( "pv-svc-incr" );
        Pipeline pipeline = createPipeline( "pv-svc-incr", client.getId( ) );
        createVersion( pipeline.getId( ), "1.0", true );
        createVersion( pipeline.getId( ), "1.2", false );

        assertEquals( "1.3", PipelineVersionService.computeNextVersionName( pipeline.getId( ) ), "Next name must be highest (1.2) + 0.1" );

    }

    /**
     * A version name using a comma decimal separator is parsed as a number, so it still drives the increment.
     */
    @Test
    public void testComputeNextVersionNameToleratesComma( )
    {
        Client client = createClient( "pv-svc-comma" );
        Pipeline pipeline = createPipeline( "pv-svc-comma", client.getId( ) );
        createVersion( pipeline.getId( ), "2,5", true );

        assertEquals( "2.6", PipelineVersionService.computeNextVersionName( pipeline.getId( ) ), "Comma decimal must be parsed and incremented" );

    }

    /**
     * A non-numeric version name is skipped, so the algorithm falls back to the default base of 1.0.
     */
    @Test
    public void testComputeNextVersionNameSkipsNonNumeric( )
    {
        Client client = createClient( "pv-svc-nonnum" );
        Pipeline pipeline = createPipeline( "pv-svc-nonnum", client.getId( ) );
        createVersion( pipeline.getId( ), "draft", true );

        assertEquals( "1.1", PipelineVersionService.computeNextVersionName( pipeline.getId( ) ), "Non-numeric names fall back to base 1.0 + 0.1" );

    }

    /**
     * Creating a pipeline with its initial version persists the pipeline and seeds exactly one current version named "1.0" with a non-empty default flow.
     */
    @Test
    public void testCreatePipelineWithInitialVersion( )
    {
        Client client = createClient( "pv-svc-init" );
        Pipeline pipeline = PipelineVersionService.createPipelineWithInitialVersion( "pv-svc-init", "desc", 3, 5, client.getId( ) );

        assertTrue( pipeline.getId( ) > 0, "Created pipeline must have a generated id" );
        assertTrue( PipelineHome.findByPrimaryKey( pipeline.getId( ) ).isPresent( ), "Pipeline must be persisted" );

        List<PipelineVersion> versions = PipelineVersionHome.findByPipelineId( pipeline.getId( ) );
        assertEquals( 1, versions.size( ), "Exactly one initial version must be seeded" );

        PipelineVersion initial = versions.get( 0 );
        assertEquals( "1.0", initial.getVersionName( ), "Initial version must be named 1.0" );
        assertTrue( initial.isCurrent( ), "Initial version must be current" );
        assertTrue( initial.getFlow( ) != null, "Initial version must carry the default flow" );
        assertTrue( PipelineVersionHome.findCurrentVersion( pipeline.getId( ) ).isPresent( ), "Current version must resolve" );

    }

    /**
     * Cloning a new version from the current one duplicates the current flow, names the clone by incrementing, and marks the clone non-current while leaving
     * the original current version untouched.
     */
    @Test
    public void testCreateVersionFromCurrent( )
    {
        Client client = createClient( "pv-svc-clone" );
        Pipeline pipeline = createPipeline( "pv-svc-clone", client.getId( ) );
        PipelineVersion current = createVersion( pipeline.getId( ), "1.0", true );
        current.setFlow( "{\"nodes\":{},\"edges\":[]}" );
        PipelineVersionHome.update( current );

        PipelineVersion clone = PipelineVersionService.createVersionFromCurrent( pipeline.getId( ) );

        assertTrue( clone.getId( ) > 0, "Clone must be persisted with a generated id" );
        assertEquals( "1.1", clone.getVersionName( ), "Clone must be named by incrementing the highest version" );
        assertFalse( clone.isCurrent( ), "Clone must not be current" );
        assertEquals( current.getFlow( ), clone.getFlow( ), "Clone must duplicate the current version flow" );

        assertTrue( PipelineVersionHome.findByPrimaryKey( current.getId( ) ).get( ).isCurrent( ), "Original current version must remain current" );
        assertEquals( 2, PipelineVersionHome.findByPipelineId( pipeline.getId( ) ).size( ), "Pipeline must now hold two versions" );

    }

    /**
     * When the pipeline has no current version, cloning falls back to the default flow and yields the default name on an otherwise empty pipeline.
     */
    @Test
    public void testCreateVersionFromCurrentFallsBackToDefaultFlow( )
    {
        Client client = createClient( "pv-svc-clone-empty" );
        Pipeline pipeline = createPipeline( "pv-svc-clone-empty", client.getId( ) );

        PipelineVersion clone = PipelineVersionService.createVersionFromCurrent( pipeline.getId( ) );

        assertEquals( "1.0", clone.getVersionName( ), "Empty pipeline clone takes the default version name" );
        assertTrue( clone.getFlow( ) != null, "Clone must fall back to the default flow" );
        assertFalse( clone.isCurrent( ), "Clone must not be current" );

    }

    /**
     * Removing a non-current version succeeds and the version disappears from persistence.
     */
    @Test
    public void testRemoveNonCurrentVersion( )
    {
        Client client = createClient( "pv-svc-rm-ok" );
        Pipeline pipeline = createPipeline( "pv-svc-rm-ok", client.getId( ) );
        createVersion( pipeline.getId( ), "1.0", true );
        PipelineVersion draft = createVersion( pipeline.getId( ), "1.1", false );

        PipelineVersionService.removeVersion( draft.getId( ) );

        assertFalse( PipelineVersionHome.findByPrimaryKey( draft.getId( ) ).isPresent( ), "Removed non-current version must be gone" );

    }

    /**
     * Removing the current version is rejected with the typed exception and the version remains persisted.
     */
    @Test
    public void testRemoveCurrentVersionIsRejected( )
    {
        Client client = createClient( "pv-svc-rm-guard" );
        Pipeline pipeline = createPipeline( "pv-svc-rm-guard", client.getId( ) );
        PipelineVersion current = createVersion( pipeline.getId( ), "1.0", true );

        assertThrows( CannotRemoveCurrentVersionException.class, ( ) -> PipelineVersionService.removeVersion( current.getId( ) ),
                "Removing the current version must throw the typed exception" );
        assertTrue( PipelineVersionHome.findByPrimaryKey( current.getId( ) ).isPresent( ), "Current version must still be persisted after a rejected removal" );

    }

    /**
     * A patch carrying a flow that is not parseable JSON is rejected with the typed exception and nothing is persisted.
     */
    @Test
    public void testUpdateVersionRejectsMalformedFlow( )
    {
        Client client = createClient( "pv-svc-badflow" );
        Pipeline pipeline = createPipeline( "pv-svc-badflow", client.getId( ) );
        PipelineVersion version = createVersion( pipeline.getId( ), "1.0", true );

        assertThrows( InvalidRequestException.class, ( ) -> PipelineVersionService.updateVersion( version.getId( ), Map.of( "flow", "{not-json" ) ),
                "A malformed flow must be rejected at save" );
        assertEquals( "{}", PipelineVersionHome.findByPrimaryKey( version.getId( ) ).orElseThrow( ).getFlow( ),
                "The stored flow must be untouched after a rejected patch" );
    }

    /**
     * Work-in-progress flows produced by the autosaving editor are accepted at save: an empty node map and even a transient cycle are legal persistence states
     * — executable validity is only enforced at execution time.
     */
    @Test
    public void testUpdateVersionAcceptsWorkInProgressFlows( )
    {
        Client client = createClient( "pv-svc-wipflow" );
        Pipeline pipeline = createPipeline( "pv-svc-wipflow", client.getId( ) );
        PipelineVersion version = createVersion( pipeline.getId( ), "1.0", true );

        String emptyFlow = "{\"nodes\":{},\"edges\":[],\"rootNodeId\":\"node-start\"}";
        PipelineVersionService.updateVersion( version.getId( ), Map.of( "flow", emptyFlow ) );
        assertEquals( emptyFlow, PipelineVersionHome.findByPrimaryKey( version.getId( ) ).orElseThrow( ).getFlow( ), "An empty editor flow must be persisted" );

        String cyclicFlow = "{\"nodes\":{\"a\":{\"id\":\"a\",\"type\":\"MODEL\"},\"b\":{\"id\":\"b\",\"type\":\"MODEL\"}},"
                + "\"edges\":[{\"source\":\"a\",\"target\":\"b\"},{\"source\":\"b\",\"target\":\"a\"}],\"rootNodeId\":\"a\"}";
        PipelineVersionService.updateVersion( version.getId( ), Map.of( "flow", cyclicFlow ) );
        assertEquals( cyclicFlow, PipelineVersionHome.findByPrimaryKey( version.getId( ) ).orElseThrow( ).getFlow( ),
                "A transient cycle must not block the autosave" );
    }
}
