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
package fr.paris.lutece.plugins.platform.service.dataset;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.client.ClientHome;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJob;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentJobHome;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.dataset.dto.DocumentJobView;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;
import fr.paris.lutece.plugins.platform.test.RbacTestSupport;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.business.user.AdminUserHome;

/**
 * Authorization contract of {@link DatasetService#getDatasetJobs}: the ingestion jobs of a dataset (and the document names they leak) are only served to a user
 * holding the view permission on that dataset. A user without the permission — typically a user of ANOTHER client — must be refused, even when authenticated.
 */
public class DatasetJobsAuthorizationTest extends AbstractPlatformDbTest
{
    /**
     * An authenticated user without view permission on the dataset is refused the job list (cross-client data leak otherwise).
     */
    @Test
    public void testJobsRefusedWithoutViewPermission( )
    {
        Dataset dataset = createDataset( );
        createJob( dataset, "secret-document.pdf" );
        AdminUser stranger = createUser( );

        assertThrows( AccessDeniedException.class, ( ) -> DatasetService.getDatasetJobs( dataset.getId( ), stranger ),
                "an authenticated user without view permission must not read the jobs of someone else's dataset" );
    }

    /**
     * The owner of the dataset gets the job list enriched with document names.
     */
    @Test
    public void testJobsServedToOwnerWithDocumentNames( )
    {
        Dataset dataset = createDataset( );
        createJob( dataset, "my-document.pdf" );
        AdminUser owner = createUser( );
        RbacTestSupport.grantOwnerPermissions( owner, dataset );

        List<DocumentJobView> jobs = DatasetService.getDatasetJobs( dataset.getId( ), owner );

        assertEquals( 1, jobs.size( ) );
        assertEquals( "my-document.pdf", jobs.get( 0 ).documentName( ) );
        assertEquals( DatasetDocumentJob.STATUS_PENDING, jobs.get( 0 ).status( ) );
    }

    /**
     * An unknown dataset id yields a typed not-found error, not an empty list.
     */
    @Test
    public void testJobsUnknownDataset( )
    {
        AdminUser user = createUser( );

        assertThrows( ResourceNotFoundException.class, ( ) -> DatasetService.getDatasetJobs( 999999, user ) );
    }

    /**
     * Persists a minimal admin user with a unique access code.
     *
     * @return the persisted admin user
     */
    private AdminUser createUser( )
    {
        AdminUser user = new AdminUser( );
        user.setAccessCode( "jobs-" + UUID.randomUUID( ) );
        user.setLastName( "Test" );
        user.setFirstName( "User" );
        user.setEmail( UUID.randomUUID( ) + "@example.com" );
        user.setStatus( 0 );
        user.setLocale( Locale.FRENCH );
        user.setUserLevel( 0 );
        AdminUserHome.create( user );
        return user;
    }

    /**
     * Creates and persists the minimal Client / Provider / Dataset graph.
     *
     * @return the persisted dataset
     */
    private Dataset createDataset( )
    {
        Client client = new Client( );
        client.setName( "Client jobs" );
        client.setCode( "CLIENT_JOBS_" + UUID.randomUUID( ) );
        client.setActive( true );
        ClientHome.create( client );

        Provider provider = new Provider( );
        provider.setProviderName( "Jobs Provider" );
        provider.setProviderType( "llm" );
        provider.setDeploymentModelName( "model" );
        provider.setDeploymentApiKey( "key" );
        ProviderHome.create( provider );

        Dataset dataset = new Dataset( );
        dataset.setDatasetName( "Jobs Dataset" );
        dataset.setDatasetDescription( "Jobs test dataset" );
        dataset.setEmbedProviderId( provider.getId( ) );
        dataset.setLlmProviderId( provider.getId( ) );
        dataset.setClientId( client.getId( ) );
        DatasetHome.create( dataset );
        return dataset;
    }

    /**
     * Creates a document and a pending ingestion job for it in the given dataset.
     *
     * @param dataset
     *            the owning dataset
     * @param strDocumentName
     *            the document name
     */
    private void createJob( Dataset dataset, String strDocumentName )
    {
        DatasetDocument document = new DatasetDocument( );
        document.setName( strDocumentName );
        document.setDescription( "job test" );
        document.setFileKey( "file-key-jobs" );
        document.setDatasetId( dataset.getId( ) );
        document.setFolderId( null );
        document.setChunkSize( 1000 );
        document.setChunkOverlap( 200 );
        DatasetDocumentHome.create( document );

        DatasetDocumentJob job = new DatasetDocumentJob( );
        job.setDocumentId( document.getId( ) );
        job.setDatasetId( dataset.getId( ) );
        job.setStatus( DatasetDocumentJob.STATUS_PENDING );
        DatasetDocumentJobHome.create( job );
    }
}
