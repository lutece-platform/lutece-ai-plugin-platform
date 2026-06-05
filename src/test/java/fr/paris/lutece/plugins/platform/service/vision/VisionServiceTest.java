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
package fr.paris.lutece.plugins.platform.service.vision;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractor;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorField;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorFieldHome;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorHome;
import fr.paris.lutece.plugins.platform.business.vision.VisionFieldType;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.service.exception.InvalidFieldTypeException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Behavioural tests for {@link VisionService} against HSQL. These exercise the Home-backed CRUD facades the service exposes — vision/extractor/field creation,
 * client assignment on create, update with not-found handling, the full extractor/field graph reload, field-type enum parsing with its typed
 * {@link InvalidFieldTypeException}, and the not-found contract on remove. The HTTP-bound {@code processImage} / {@code executeVisionProcessing} paths are out
 * of scope (they require an outbound LLM provider). A parent {@link Client} and {@link Provider} satisfy the NOT NULL foreign keys of {@code platform_vision}.
 */
public class VisionServiceTest extends AbstractPlatformDbTest
{
    /**
     * Resolves the CDI-managed vision service.
     *
     * @return the vision service instance
     */
    private VisionService service( )
    {
        return CDI.current( ).select( VisionService.class ).get( );
    }

    /**
     * Creates and persists an active client with the given code.
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
     * Creates and persists a minimal LLM provider.
     *
     * @param name
     *            the provider name
     * @return the persisted provider
     */
    private Provider createProvider( String name )
    {
        Provider provider = new Provider( );
        provider.setProviderName( name );
        provider.setProviderType( "LLM" );
        provider.setDeploymentModelName( "test-model" );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Builds an unpersisted vision wired to the given client and provider.
     *
     * @param title
     *            the vision title
     * @param clientId
     *            the owning client id
     * @param providerId
     *            the provider id
     * @return the unpersisted vision
     */
    private Vision newVision( String title, int clientId, int providerId )
    {
        Vision vision = new Vision( );
        vision.setVisionTitle( title );
        vision.setVisionDescription( "desc " + title );
        vision.setClientId( clientId );
        vision.setProviderId( providerId );
        return vision;
    }

    /**
     * createVision assigns the supplied client id then persists, returning a vision carrying a generated identifier.
     */
    @Test
    public void testCreateVisionAssignsClientId( )
    {
        Client client = createClient( "vis-svc-create" );
        Provider provider = createProvider( "prov-create" );

        Vision vision = newVision( "Created", 0, provider.getId( ) );
        Vision created = service( ).createVision( vision, client.getId( ) );

        assertTrue( created.getId( ) > 0, "create should assign a generated id" );
        assertEquals( client.getId( ), created.getClientId( ), "create should assign the supplied client id" );
        assertEquals( client.getId( ), VisionHome.findByPrimaryKey( created.getId( ) ).get( ).getClientId( ), "client id must be persisted" );
    }

    /**
     * A null client id leaves the vision client untouched on create.
     */
    @Test
    public void testCreateVisionWithNullClientIdKeepsValue( )
    {
        Client client = createClient( "vis-svc-nullclient" );
        Provider provider = createProvider( "prov-nullclient" );

        Vision vision = newVision( "Preassigned", client.getId( ), provider.getId( ) );
        Vision created = service( ).createVision( vision, null );

        assertEquals( client.getId( ), created.getClientId( ), "a null client id must not overwrite the preset value" );
    }

    /**
     * updateVision persists changes made to an existing vision.
     */
    @Test
    public void testUpdateVision( )
    {
        Client client = createClient( "vis-svc-update" );
        Provider provider = createProvider( "prov-update" );
        Vision created = service( ).createVision( newVision( "Before", client.getId( ), provider.getId( ) ), client.getId( ) );

        created.setVisionTitle( "After" );
        service( ).updateVision( created );

        assertEquals( "After", VisionHome.findByPrimaryKey( created.getId( ) ).get( ).getVisionTitle( ), "update must persist the new title" );
    }

    /**
     * loadVisionWithExtractorsAndFields rebuilds the full graph: the vision carries its extractors and each extractor carries its own persisted fields.
     */
    @Test
    public void testLoadVisionWithExtractorsAndFields( ) throws InvalidFieldTypeException
    {
        Client client = createClient( "vis-svc-graph" );
        Provider provider = createProvider( "prov-graph" );
        Vision vision = service( ).createVision( newVision( "Graph", client.getId( ), provider.getId( ) ), client.getId( ) );

        VisionExtractor e1 = service( ).addExtractor( vision.getId( ), "invoice", "invoice fields" );
        VisionExtractor e2 = service( ).addExtractor( vision.getId( ), "client", "client fields" );
        service( ).addExtractorField( e1.getId( ), "total", "total amount", "NUMBER" );
        service( ).addExtractorField( e1.getId( ), "date", "invoice date", "DATE" );
        service( ).addExtractorField( e2.getId( ), "email", "client email", "EMAIL" );

        Optional<Vision> loaded = service( ).loadVisionWithExtractorsAndFields( vision.getId( ) );
        assertTrue( loaded.isPresent( ), "vision must be found" );
        List<VisionExtractor> extractors = loaded.get( ).getExtractors( );
        assertEquals( 2, extractors.size( ), "both extractors must be loaded" );

        int totalFields = extractors.stream( ).mapToInt( e -> e.getFields( ).size( ) ).sum( );
        assertEquals( 3, totalFields, "every extractor must carry its own persisted fields" );
    }

    /**
     * loadVisionWithExtractorsAndFields returns an empty Optional when no vision matches the identifier.
     */
    @Test
    public void testLoadVisionNotFound( )
    {
        assertTrue( service( ).loadVisionWithExtractorsAndFields( 987654 ).isEmpty( ), "unknown vision id must yield an empty Optional" );
    }

    /**
     * removeVision deletes the vision and returns the identifier of its owning client; a subsequent lookup fails.
     */
    @Test
    public void testRemoveVisionReturnsClientId( )
    {
        Client client = createClient( "vis-svc-remove" );
        Provider provider = createProvider( "prov-remove" );
        Vision vision = service( ).createVision( newVision( "ToRemove", client.getId( ), provider.getId( ) ), client.getId( ) );

        int returnedClientId = service( ).removeVision( vision.getId( ) );
        assertEquals( client.getId( ), returnedClientId, "removeVision must return the owning client id" );
        assertTrue( VisionHome.findByPrimaryKey( vision.getId( ) ).isEmpty( ), "vision must be gone after removal" );
    }

    /**
     * removeVision raises the typed ResourceNotFoundException when the identifier matches no vision.
     */
    @Test
    public void testRemoveVisionNotFoundThrows( )
    {
        assertThrows( ResourceNotFoundException.class, ( ) -> service( ).removeVision( 876543 ),
                "removing an unknown vision must raise ResourceNotFoundException" );
    }

    /**
     * addExtractor persists a new extractor attached to its vision with a generated identifier.
     */
    @Test
    public void testAddExtractor( )
    {
        Client client = createClient( "vis-svc-addext" );
        Provider provider = createProvider( "prov-addext" );
        Vision vision = service( ).createVision( newVision( "WithExtractor", client.getId( ), provider.getId( ) ), client.getId( ) );

        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "header", "header block" );
        assertTrue( extractor.getId( ) > 0, "extractor must get a generated id" );
        assertEquals( vision.getId( ), extractor.getVisionId( ), "extractor must be attached to its vision" );
        assertEquals( 1, VisionExtractorHome.findByVisionId( vision.getId( ) ).size( ), "extractor must be persisted under the vision" );
    }

    /**
     * updateExtractor persists name/description changes and returns true when the extractor exists.
     */
    @Test
    public void testUpdateExtractorExisting( )
    {
        Client client = createClient( "vis-svc-updext" );
        Provider provider = createProvider( "prov-updext" );
        Vision vision = service( ).createVision( newVision( "UpdExt", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "old", "old desc" );

        boolean updated = service( ).updateExtractor( extractor.getId( ), "new", "new desc" );
        assertTrue( updated, "updating an existing extractor must return true" );
        VisionExtractor reloaded = VisionExtractorHome.findByPrimaryKey( extractor.getId( ) ).get( );
        assertEquals( "new", reloaded.getExtractorName( ), "name must be persisted" );
        assertEquals( "new desc", reloaded.getExtractorDescription( ), "description must be persisted" );
    }

    /**
     * updateExtractor returns false when no extractor matches the identifier.
     */
    @Test
    public void testUpdateExtractorNotFound( )
    {
        assertFalse( service( ).updateExtractor( 765432, "x", "y" ), "updating an unknown extractor must return false" );
    }

    /**
     * removeExtractor deletes the extractor from its vision.
     */
    @Test
    public void testRemoveExtractor( )
    {
        Client client = createClient( "vis-svc-rmext" );
        Provider provider = createProvider( "prov-rmext" );
        Vision vision = service( ).createVision( newVision( "RmExt", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "doomed", "to be removed" );

        service( ).removeExtractor( extractor.getId( ) );
        assertTrue( VisionExtractorHome.findByPrimaryKey( extractor.getId( ) ).isEmpty( ), "extractor must be gone after removal" );
    }

    /**
     * addExtractorField parses the textual type into its enum and persists the field under its extractor.
     */
    @Test
    public void testAddExtractorFieldParsesType( ) throws InvalidFieldTypeException
    {
        Client client = createClient( "vis-svc-addfld" );
        Provider provider = createProvider( "prov-addfld" );
        Vision vision = service( ).createVision( newVision( "AddField", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "ext", "ext desc" );

        VisionExtractorField field = service( ).addExtractorField( extractor.getId( ), "amount", "the amount", "number" );
        assertTrue( field.getId( ) > 0, "field must get a generated id" );
        assertEquals( VisionFieldType.NUMBER, field.getFieldType( ), "lowercase type must parse to the NUMBER enum" );
        assertEquals( 1, VisionExtractorFieldHome.findByExtractorId( extractor.getId( ) ).size( ), "field must be persisted under the extractor" );
    }

    /**
     * addExtractorField raises the typed InvalidFieldTypeException on an unknown field type, and the field is not persisted.
     */
    @Test
    public void testAddExtractorFieldInvalidType( )
    {
        Client client = createClient( "vis-svc-badfld" );
        Provider provider = createProvider( "prov-badfld" );
        Vision vision = service( ).createVision( newVision( "BadField", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "ext", "ext desc" );

        assertThrows( InvalidFieldTypeException.class, ( ) -> service( ).addExtractorField( extractor.getId( ), "f", "d", "NOT_A_TYPE" ),
                "an unknown field type must raise InvalidFieldTypeException" );
        assertTrue( VisionExtractorFieldHome.findByExtractorId( extractor.getId( ) ).isEmpty( ), "no field must be persisted on a bad type" );
    }

    /**
     * updateExtractorField parses the type, persists the changes and returns true when the field exists.
     */
    @Test
    public void testUpdateExtractorFieldExisting( ) throws InvalidFieldTypeException
    {
        Client client = createClient( "vis-svc-updfld" );
        Provider provider = createProvider( "prov-updfld" );
        Vision vision = service( ).createVision( newVision( "UpdField", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "ext", "ext desc" );
        VisionExtractorField field = service( ).addExtractorField( extractor.getId( ), "old", "old desc", "STRING" );

        boolean updated = service( ).updateExtractorField( field.getId( ), "new", "new desc", "BOOLEAN" );
        assertTrue( updated, "updating an existing field must return true" );
        VisionExtractorField reloaded = VisionExtractorFieldHome.findByPrimaryKey( field.getId( ) ).get( );
        assertEquals( "new", reloaded.getFieldName( ), "name must be persisted" );
        assertEquals( VisionFieldType.BOOLEAN, reloaded.getFieldType( ), "new type must be persisted" );
    }

    /**
     * updateExtractorField returns false when no field matches the identifier, even though the type parses.
     */
    @Test
    public void testUpdateExtractorFieldNotFound( ) throws InvalidFieldTypeException
    {
        assertFalse( service( ).updateExtractorField( 654321, "n", "d", "STRING" ), "updating an unknown field must return false" );
    }

    /**
     * updateExtractorField raises InvalidFieldTypeException before any lookup when the type cannot be parsed.
     */
    @Test
    public void testUpdateExtractorFieldInvalidType( )
    {
        assertThrows( InvalidFieldTypeException.class, ( ) -> service( ).updateExtractorField( 1, "n", "d", "BOGUS" ),
                "an unknown field type must raise InvalidFieldTypeException" );
    }

    /**
     * removeExtractorField deletes the field from its extractor.
     */
    @Test
    public void testRemoveExtractorField( ) throws InvalidFieldTypeException
    {
        Client client = createClient( "vis-svc-rmfld" );
        Provider provider = createProvider( "prov-rmfld" );
        Vision vision = service( ).createVision( newVision( "RmField", client.getId( ), provider.getId( ) ), client.getId( ) );
        VisionExtractor extractor = service( ).addExtractor( vision.getId( ), "ext", "ext desc" );
        VisionExtractorField field = service( ).addExtractorField( extractor.getId( ), "doomed", "d", "STRING" );

        service( ).removeExtractorField( field.getId( ) );
        assertTrue( VisionExtractorFieldHome.findByPrimaryKey( field.getId( ) ).isEmpty( ), "field must be gone after removal" );
    }
}
