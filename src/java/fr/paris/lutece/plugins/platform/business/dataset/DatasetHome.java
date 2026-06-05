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
package fr.paris.lutece.plugins.platform.business.dataset;

import java.util.List;
import java.util.Optional;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import jakarta.enterprise.inject.spi.CDI;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.plugins.platform.service.cache.DatasetCacheService;
import fr.paris.lutece.plugins.platform.service.cache.PlatformCacheEvents;
import fr.paris.lutece.plugins.platform.service.cache.PlatformResource;
import fr.paris.lutece.plugins.platform.service.cache.EntityChangedEvent.Action;

/**
 * This class provides instances management methods for Dataset objects
 */
public final class DatasetHome
{
    private static IDatasetDAO _dao = CDI.current( ).select( IDatasetDAO.class ).get( );
    private static final DatasetCacheService _cache = CDI.current( ).select( DatasetCacheService.class ).get( );
    private static Plugin _plugin = PluginService.getPlugin( "platform" );

    /**
     * Private constructor
     */
    private DatasetHome( )
    {
    }

    /**
     * Create a new dataset record in the database
     *
     * @param dataset
     *            The dataset object
     * @return The dataset object with its ID updated
     */
    public static Dataset create( Dataset dataset )
    {
        _dao.insert( dataset, _plugin );
        PlatformCacheEvents.fire( PlatformResource.DATASET, dataset.getId( ), Action.CREATED );
        return dataset;
    }

    /**
     * Update a dataset record in the database
     *
     * @param dataset
     *            The dataset object
     * @return The updated dataset object
     */
    public static Dataset update( Dataset dataset )
    {
        _dao.store( dataset, _plugin );
        PlatformCacheEvents.fire( PlatformResource.DATASET, dataset.getId( ), Action.UPDATED );
        return dataset;
    }

    /**
     * Remove a dataset from the database
     *
     * @param nKey
     *            The dataset ID
     */
    public static void remove( int nKey )
    {
        _dao.delete( nKey, _plugin );
        PlatformCacheEvents.fire( PlatformResource.DATASET, nKey, Action.REMOVED );
    }

    /**
     * Find a dataset by its primary key
     *
     * @param nKey
     *            The dataset ID
     * @return An Optional containing the dataset if found
     */
    public static Optional<Dataset> findByPrimaryKey( int nKey )
    {
        Object cached = _cache.get( DatasetCacheService.getKey( nKey ) );

        if ( cached != null )
        {
            return Optional.of( (Dataset) cached );
        }

        Optional<Dataset> result = _dao.load( nKey, _plugin );
        result.ifPresent( dataset -> _cache.put( DatasetCacheService.getKey( nKey ), dataset ) );
        return result;
    }

    /**
     * Get the list of all datasets
     *
     * @return The list of all datasets
     */
    public static List<Dataset> getDatasetsList( )
    {
        return _dao.selectDatasetsList( _plugin );
    }

    /**
     * Get all datasets for a specified client
     *
     * @param nClientId
     *            The client ID
     * @return The list of datasets for the specified client
     */
    public static List<Dataset> getDatasetsByClientId( int nClientId )
    {
        return _dao.selectDatasetsByClientId( nClientId, _plugin );
    }

    /**
     * Returns a reference list of all Datasets (ID and name only) for RBAC purposes
     *
     * @return A ReferenceList containing dataset IDs and names
     */
    public static ReferenceList getDatasetsReferenceList( )
    {
        return _dao.selectDatasetsReferenceList( _plugin );
    }
}
