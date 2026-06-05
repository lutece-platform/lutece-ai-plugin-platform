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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.util.httpaccess.MemoryFileItem;

import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Named;

/**
 * Service for storing and retrieving files associated with pipeline executions. Uses Lutece's FileService to persist files and replaces base64 content with
 * fileKeys so that pipeline inputs stored in the database remain lightweight.
 */
@ApplicationScoped
@Named( "platform.pipelineFileService" )
public class PipelineFileService
{

    public static final String FILE_NAME_KEY = "fileName";
    private static final String CONTENT_TYPE_KEY = "contentType";
    private static final String CONTENT_KEY = "content";
    public static final String FILE_KEY = "fileKey";

    private static final String ERROR_STORE = "Error while storing the pipeline file: ";
    private static final String ERROR_RETRIEVE = "Error while retrieving the pipeline file: ";
    private IFileStoreServiceProvider _fileService;

    /**
     * Private constructor.
     */
    PipelineFileService( )
    {
    }

    /**
     * Gets the file store service provider, lazily initialized.
     *
     * @return the file store service provider
     */
    private IFileStoreServiceProvider getFileService( )
    {
        if ( _fileService == null )
        {
            _fileService = CDI.current( ).select( IFileStoreServiceProvider.class ).get( );
        }
        return _fileService;
    }

    /**
     * Scans pipeline inputs for FILE-type values (objects or arrays of objects containing base64 content), stores each file via FileService, and replaces the
     * base64 content with a lightweight fileKey reference.
     *
     * A file object is detected by the presence of "fileName", "contentType" and "content" keys. After storage, the "content" key is replaced by "fileKey".
     *
     * @param inputs
     *            the mutable pipeline inputs map — modified in place
     */
    @SuppressWarnings( "unchecked" )
    public void storeFileInputs( Map<String, Object> inputs )
    {
        for ( Map.Entry<String, Object> entry : inputs.entrySet( ) )
        {
            Object value = entry.getValue( );

            if ( value instanceof List )
            {
                List<Object> list = (List<Object>) value;
                List<Object> processed = new ArrayList<>( );
                boolean hasFile = false;
                for ( Object item : list )
                {
                    if ( isFileObject( item ) )
                    {
                        hasFile = true;
                        processed.add( storeAndReplace( (Map<String, Object>) item ) );
                    }
                    else
                    {
                        processed.add( item );
                    }
                }
                if ( hasFile )
                {
                    entry.setValue( processed );
                }
            }
            else if ( isFileObject( value ) )
            {
                entry.setValue( storeAndReplace( (Map<String, Object>) value ) );
            }
        }
    }

    /**
     * Loads a file's content as an InputStream from its fileKey.
     *
     * @param fileKey
     *            the file storage key
     * @return the file content as InputStream, or null if retrieval fails
     */
    public InputStream getFileInputStream( String fileKey )
    {
        try
        {
            return getFileService( ).getInputStream( fileKey );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_RETRIEVE, fileKey, e );
            return null;
        }
    }

    /**
     * Loads a file's content as a byte array from its fileKey.
     *
     * @param fileKey
     *            the file storage key
     * @return the file content as byte array, or null if retrieval fails
     */
    public byte [ ] getFileBytes( String fileKey )
    {
        try ( InputStream is = getFileInputStream( fileKey ) )
        {
            if ( is == null )
            {
                return null;
            }
            return is.readAllBytes( );
        }
        catch( IOException e )
        {
            AppLogService.error( "{}{}", ERROR_RETRIEVE, fileKey, e );
            return null;
        }
    }

    /**
     * Checks if an object is a file object (Map with fileName, contentType and content keys).
     *
     * @param value
     *            the object to check
     * @return true if the object is a file object with base64 content
     */
    @SuppressWarnings( "unchecked" )
    private boolean isFileObject( Object value )
    {
        if ( !( value instanceof Map ) )
        {
            return false;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        return map.containsKey( FILE_NAME_KEY ) && map.containsKey( CONTENT_TYPE_KEY ) && map.containsKey( CONTENT_KEY );
    }

    /**
     * Stores the file content from a file object and returns a new map with fileKey instead of content.
     *
     * @param fileObject
     *            the file object containing fileName, contentType and base64 content
     * @return a new map with fileKey replacing content, or the original map if storage fails
     */
    private Map<String, Object> storeAndReplace( Map<String, Object> fileObject )
    {
        String fileName = (String) fileObject.get( FILE_NAME_KEY );
        String contentType = (String) fileObject.get( CONTENT_TYPE_KEY );
        String base64Content = (String) fileObject.get( CONTENT_KEY );

        try
        {
            byte [ ] bytes = Base64.getDecoder( ).decode( base64Content );
            MultipartItem fileItem = createFileItem( fileName, contentType, bytes );
            String fileKey = getFileService( ).storeFileItem( fileItem );

            Map<String, Object> result = new HashMap<>( );
            result.put( FILE_NAME_KEY, fileName );
            result.put( CONTENT_TYPE_KEY, contentType );
            result.put( FILE_KEY, fileKey );
            return result;
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_STORE, fileName, e );
            return fileObject;
        }
    }

    /**
     * Creates a FileItem from raw bytes for storage via FileService.
     *
     * @param fileName
     *            the file name
     * @param contentType
     *            the MIME content type
     * @param content
     *            the file content as bytes
     * @return the created FileItem
     */
    private MultipartItem createFileItem( String fileName, String contentType, byte [ ] content )
    {
        return new MemoryFileItem( content, fileName, content.length, contentType );
    }
}
