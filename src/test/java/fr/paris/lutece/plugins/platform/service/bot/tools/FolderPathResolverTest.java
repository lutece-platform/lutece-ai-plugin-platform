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
package fr.paris.lutece.plugins.platform.service.bot.tools;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolderHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Tests for {@link FolderPathResolver}, which loads a dataset folder tree once and answers path/id navigation queries in memory. The fixture builds the tree
 * /a, /a/b, /a/b/c and a sibling /d, then verifies absolute path rendering, path resolution (including the root and unknown-path cases), recursive descendant
 * scoping, child listing, by-id lookup, and isolation between datasets.
 */
public class FolderPathResolverTest extends AbstractPlatformDbTest
{
    private Client _client;
    private Provider _provider;
    private Dataset _dataset;
    private DatasetFolder _a;
    private DatasetFolder _b;
    private DatasetFolder _c;
    private DatasetFolder _d;

    /**
     * Builds the parent chain and the folder tree /a, /a/b, /a/b/c, /d shared by every test.
     */
    @BeforeEach
    public void setUp( )
    {
        _client = new Client( );
        _client.setName( "Client FolderResolver Test" );
        _client.setCode( "CLIENT_FOLDERRESOLVER_" + UUID.randomUUID( ) );
        _client.setActive( true );
        ClientHome.create( _client );

        _provider = createProvider( );
        _dataset = createDataset( _client, _provider );

        _a = createFolder( _dataset.getId( ), null, "a" );
        _b = createFolder( _dataset.getId( ), _a.getId( ), "b" );
        _c = createFolder( _dataset.getId( ), _b.getId( ), "c" );
        _d = createFolder( _dataset.getId( ), null, "d" );
    }

    /**
     * pathOf walks the parent chain to render the absolute path, and the dataset root (null id) renders as "/".
     */
    @Test
    public void testPathOf( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        assertEquals( "/a", resolver.pathOf( _a.getId( ) ) );
        assertEquals( "/a/b", resolver.pathOf( _b.getId( ) ) );
        assertEquals( "/a/b/c", resolver.pathOf( _c.getId( ) ) );
        assertEquals( "/d", resolver.pathOf( _d.getId( ) ) );
        assertEquals( "/", resolver.pathOf( null ) );
    }

    /**
     * resolvePath matches an absolute path to its folder; the root and unknown paths return null.
     */
    @Test
    public void testResolvePath( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        assertEquals( _c.getId( ), resolver.resolvePath( "/a/b/c" ).getId( ) );
        assertEquals( _b.getId( ), resolver.resolvePath( "/a/b" ).getId( ) );
        assertNull( resolver.resolvePath( "/" ) );
        assertNull( resolver.resolvePath( "/a/x" ) );
        assertNull( resolver.resolvePath( "/nope" ) );
    }

    /**
     * resolvePath tolerates a trailing slash.
     */
    @Test
    public void testResolvePathTrailingSlash( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        assertEquals( _b.getId( ), resolver.resolvePath( "/a/b/" ).getId( ) );
    }

    /**
     * getDescendantIds returns the folder and its whole subtree; a null id yields an empty (unrestricted) scope.
     */
    @Test
    public void testGetDescendantIds( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        List<Integer> subtree = resolver.getDescendantIds( _a.getId( ) );
        assertEquals( 3, subtree.size( ) );
        assertTrue( subtree.contains( _a.getId( ) ) );
        assertTrue( subtree.contains( _b.getId( ) ) );
        assertTrue( subtree.contains( _c.getId( ) ) );
        assertFalse( subtree.contains( _d.getId( ) ) );

        assertTrue( resolver.getDescendantIds( null ).isEmpty( ) );
        assertEquals( 1, resolver.getDescendantIds( _c.getId( ) ).size( ) );
    }

    /**
     * getChildren lists the immediate children of a folder, and the dataset roots when the id is null.
     */
    @Test
    public void testGetChildrenAndRoots( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        assertEquals( 2, resolver.getRoots( ).size( ) );
        assertEquals( 2, resolver.getChildren( null ).size( ) );
        assertEquals( 1, resolver.getChildren( _a.getId( ) ).size( ) );
        assertEquals( _b.getId( ), resolver.getChildren( _a.getId( ) ).get( 0 ).getId( ) );
        assertTrue( resolver.getChildren( _c.getId( ) ).isEmpty( ) );
    }

    /**
     * findById returns the in-memory folder, and a null or unknown id returns null.
     */
    @Test
    public void testFindById( )
    {
        FolderPathResolver resolver = FolderPathResolver.forDataset( _dataset.getId( ) );

        assertEquals( "b", resolver.findById( _b.getId( ) ).getName( ) );
        assertNull( resolver.findById( null ) );
        assertNull( resolver.findById( 999999 ) );
    }

    /**
     * The resolver loads only the folders of its own dataset; a second empty dataset sees none of the first dataset's tree.
     */
    @Test
    public void testDatasetIsolation( )
    {
        Dataset other = createDataset( _client, _provider );
        FolderPathResolver resolver = FolderPathResolver.forDataset( other.getId( ) );

        assertEquals( other.getId( ), resolver.getDatasetId( ) );
        assertTrue( resolver.getAllFolders( ).isEmpty( ) );
        assertNull( resolver.findById( _a.getId( ) ) );
    }

    /**
     * Creates a persisted provider used as the embed and LLM provider of the dataset.
     *
     * @return the persisted provider
     */
    private Provider createProvider( )
    {
        Provider provider = new Provider( );
        provider.setProviderName( "FolderResolver Provider" );
        provider.setProviderType( "llm" );
        provider.setDeploymentModelName( "model" );
        provider.setDeploymentApiKey( "test-key" );
        return ProviderHome.create( provider );
    }

    /**
     * Creates a persisted dataset under the given client and provider.
     *
     * @param client
     *            the parent client
     * @param provider
     *            the provider used for embed and LLM
     * @return the persisted dataset
     */
    private Dataset createDataset( Client client, Provider provider )
    {
        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "FolderResolver Dataset" );
        dataset.setDatasetDescription( "Dataset for folder resolver test" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Creates a persisted folder in the given dataset.
     *
     * @param nDatasetId
     *            the parent dataset identifier
     * @param parentFolderId
     *            the parent folder identifier, or null for a root folder
     * @param strName
     *            the folder name
     * @return the persisted folder
     */
    private DatasetFolder createFolder( int nDatasetId, Integer parentFolderId, String strName )
    {
        DatasetFolder folder = new DatasetFolder( );
        folder.setDatasetId( nDatasetId );
        folder.setParentFolderId( parentFolderId );
        folder.setName( strName );
        folder.setDescription( "Folder " + strName );
        return DatasetFolderHome.create( folder );
    }
}
