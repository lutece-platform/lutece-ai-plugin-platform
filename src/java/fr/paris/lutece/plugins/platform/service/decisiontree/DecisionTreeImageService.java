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
package fr.paris.lutece.plugins.platform.service.decisiontree;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeImage;
import fr.paris.lutece.plugins.platform.business.decisiontree.DecisionTreeImageHome;
import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Service for managing images referenced from decision tree markdown content.
 *
 * Images are stored in the Lutece FileStoreService; a mapping table ties each {@code (contentHash, treeId)} pair to the internal file store key so we can (a)
 * enforce access control — a hash is only served to clients authorized on a tree that references it — and (b) clean up physical blobs when no tree references
 * them anymore.
 *
 * The identifier embedded in markdown is {@code lutece-image:<sha256>} — the content hash. Since it is derived from the bytes, the identifier is stable across
 * instances: the same image produces the same marker everywhere, which makes export/import trivial (no marker rewrite required).
 */
public final class DecisionTreeImageService
{
    /** Scheme marker embedded in markdown. The trailing component is the sha256 of the image bytes. */
    public static final String URL_SCHEME = "lutece-image:";

    /** Matches both the current scheme form and the legacy slash-prefixed URL form, for robustness when extracting hashes from markdown. */
    private static final Pattern URL_PATTERN = Pattern.compile( "(?:" + Pattern.quote( URL_SCHEME ) + "|/?rest/decisiontree/images/)([^)\\s'\"<>]+)" );

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final String ERROR_STORING_IMAGE = "Error storing decision tree image: ";
    private static final String ERROR_DELETING_IMAGE = "Error deleting decision tree image: ";
    private static final String ERROR_LOADING_IMAGE = "Error loading decision tree image: ";
    private static final String ERROR_HASH_ALGORITHM = "SHA-256 algorithm not available";

    /**
     * Utility class — no instance.
     */
    private DecisionTreeImageService( )
    {
    }

    /**
     * Stores an image for a given tree. Computes the sha256 of the bytes as the public identifier. If the (hash, tree) pair already exists, the call is a no-op
     * and returns the hash directly — making the operation idempotent. If the hash is new to the tree but another tree already has the same blob, this call
     * reuses the existing file store key, avoiding a duplicate upload in the FileStoreService.
     *
     * @param nTreeId
     *            the owning tree identifier
     * @param bytes
     *            the image bytes
     * @param strMimeType
     *            the image MIME type
     * @return the sha256 content hash, or null if storage failed
     */
    public static String upload( int nTreeId, byte [ ] bytes, String strMimeType )
    {
        if ( bytes == null || bytes.length == 0 )
        {
            return null;
        }
        String strHash = sha256Hex( bytes );
        if ( strHash == null )
        {
            return null;
        }

        if ( DecisionTreeImageHome.findByPrimaryKey( strHash, nTreeId ).isPresent( ) )
        {
            return strHash;
        }

        String strFileStoreKey = existingFileStoreKey( strHash );
        if ( strFileStoreKey == null )
        {
            try
            {
                strFileStoreKey = fileStore( ).storeBytes( bytes );
            }
            catch( FileServiceException e )
            {
                AppLogService.error( "{}{}", ERROR_STORING_IMAGE, e.getMessage( ), e );
                return null;
            }
        }

        DecisionTreeImage image = new DecisionTreeImage( );
        image.setContentHash( strHash );
        image.setTreeId( nTreeId );
        image.setFileStoreKey( strFileStoreKey );
        image.setMimeType( strMimeType );
        DecisionTreeImageHome.create( image );
        return strHash;
    }

    /**
     * Loads the mapping row for a given hash, scoped to a specific tree. Used for authorization checks (the caller must already know which tree it is
     * validating access against).
     *
     * @param strContentHash
     *            the content hash
     * @param nTreeId
     *            the tree id
     * @return an Optional containing the mapping if found, empty otherwise
     */
    public static Optional<DecisionTreeImage> findMapping( String strContentHash, int nTreeId )
    {
        return DecisionTreeImageHome.findByPrimaryKey( strContentHash, nTreeId );
    }

    /**
     * Returns every mapping entry (across all trees) that references the given content hash. Useful when the caller needs to find any tree that references an
     * image (for authorization, it will then check client ownership on each candidate tree).
     *
     * @param strContentHash
     *            the content hash
     * @return the list of entries (may be empty)
     */
    public static List<DecisionTreeImage> findAnyMapping( String strContentHash )
    {
        return DecisionTreeImageHome.findByContentHash( strContentHash );
    }

    /**
     * Loads the raw image bytes from the FileStoreService for a given mapping entry. The caller is expected to have already authorized the request.
     *
     * @param mapping
     *            the mapping entry
     * @return the stored File, or null if not found / error
     */
    public static File getFile( DecisionTreeImage mapping )
    {
        if ( mapping == null )
        {
            return null;
        }
        try
        {
            return fileStore( ).getFile( mapping.getFileStoreKey( ) );
        }
        catch( FileServiceException e )
        {
            AppLogService.error( "{}{}", ERROR_LOADING_IMAGE, e.getMessage( ), e );
            return null;
        }
    }

    /**
     * Removes the reference of a content hash for a specific tree. If no other tree still references the same blob, the physical file is deleted from the
     * FileStoreService as well.
     *
     * @param strContentHash
     *            the content hash
     * @param nTreeId
     *            the tree id
     */
    public static void delete( String strContentHash, int nTreeId )
    {
        DecisionTreeImage mapping = DecisionTreeImageHome.findByPrimaryKey( strContentHash, nTreeId ).orElse( null );
        if ( mapping == null )
        {
            return;
        }
        String strFileStoreKey = mapping.getFileStoreKey( );
        DecisionTreeImageHome.remove( strContentHash, nTreeId );
        if ( !isFileStoreKeyStillReferenced( strFileStoreKey, strContentHash ) )
        {
            try
            {
                fileStore( ).delete( strFileStoreKey );
            }
            catch( FileServiceException e )
            {
                AppLogService.error( "{}{}", ERROR_DELETING_IMAGE, e.getMessage( ), e );
            }
        }
    }

    /**
     * Deletes all images associated with a tree. Called before removing the tree itself so the FK cascade only wipes mapping rows while this method handles
     * physical blobs (deleting those no longer referenced by any remaining tree).
     *
     * @param nTreeId
     *            the tree identifier
     */
    public static void deleteByTreeId( int nTreeId )
    {
        for ( DecisionTreeImage image : DecisionTreeImageHome.findByTreeId( nTreeId ) )
        {
            delete( image.getContentHash( ), nTreeId );
        }
    }

    /**
     * Deletes every image referenced in the given markdown content, scoped to a specific tree. Intended when the owning node or textarea is removed.
     *
     * @param strMarkdown
     *            the markdown whose referenced images must be removed
     * @param nTreeId
     *            the owning tree id
     */
    public static void deleteFromMarkdown( String strMarkdown, int nTreeId )
    {
        for ( String strHash : extractContentHashes( strMarkdown ) )
        {
            delete( strHash, nTreeId );
        }
    }

    /**
     * Deletes image hashes that were present in {@code strOldMarkdown} but are no longer referenced in {@code strNewMarkdown}. Intended for save operations so
     * that images removed from a textarea see their underlying files purged.
     *
     * @param strOldMarkdown
     *            the previous markdown (may be null)
     * @param strNewMarkdown
     *            the new markdown (may be null)
     * @param nTreeId
     *            the owning tree id
     */
    public static void cleanupDiff( String strOldMarkdown, String strNewMarkdown, int nTreeId )
    {
        Set<String> oldHashes = extractContentHashes( strOldMarkdown );
        Set<String> newHashes = extractContentHashes( strNewMarkdown );
        oldHashes.removeAll( newHashes );
        for ( String strHash : oldHashes )
        {
            delete( strHash, nTreeId );
        }
    }

    /**
     * Extracts all decision-tree image content hashes from a markdown text by matching the well-known scheme / URL pattern.
     *
     * @param strMarkdown
     *            the markdown to scan (nullable)
     * @return a set of hashes referenced in the markdown (empty if none)
     */
    public static Set<String> extractContentHashes( String strMarkdown )
    {
        Set<String> keys = new HashSet<>( );
        if ( strMarkdown == null || strMarkdown.isEmpty( ) )
        {
            return keys;
        }
        Matcher matcher = URL_PATTERN.matcher( strMarkdown );
        while ( matcher.find( ) )
        {
            keys.add( matcher.group( 1 ) );
        }
        return keys;
    }

    /**
     * Builds the scheme marker to embed in markdown for a given content hash. Stable across instances — the same bytes always produce the same marker.
     *
     * @param strContentHash
     *            the content hash
     * @return the scheme marker
     */
    public static String buildMarker( String strContentHash )
    {
        return URL_SCHEME + strContentHash;
    }

    /**
     * Computes the lowercase hex sha256 of the given bytes.
     *
     * @param bytes
     *            the bytes to hash
     * @return the 64-char lowercase hex string, or null on algorithm failure
     */
    public static String sha256Hex( byte [ ] bytes )
    {
        try
        {
            byte [ ] digest = MessageDigest.getInstance( HASH_ALGORITHM ).digest( bytes );
            StringBuilder sb = new StringBuilder( digest.length * 2 );
            for ( byte b : digest )
            {
                sb.append( HEX_CHARS.charAt( ( b >> 4 ) & 0x0F ) ).append( HEX_CHARS.charAt( b & 0x0F ) );
            }
            return sb.toString( );
        }
        catch( NoSuchAlgorithmException e )
        {
            AppLogService.error( ERROR_HASH_ALGORITHM, e );
            return null;
        }
    }

    /**
     * Returns a file store key already used for this content hash by any tree, or null if this hash is unknown to the mapping table.
     *
     * @param strContentHash
     *            the content hash
     * @return an existing file store key, or null
     */
    private static String existingFileStoreKey( String strContentHash )
    {
        List<DecisionTreeImage> existing = DecisionTreeImageHome.findByContentHash( strContentHash );
        return existing.isEmpty( ) ? null : existing.get( 0 ).getFileStoreKey( );
    }

    /**
     * Returns whether the given file store key is still referenced by at least one row of the mapping table. Used to decide whether the physical blob must be
     * deleted from the FileStoreService when a tree's reference goes away.
     *
     * Implementation note: since different content hashes cannot share a file store key (the key is only reused when the same hash comes from another tree), we
     * can simply check whether any row still exists for the same hash. The {@code strHashJustRemoved} parameter is the hash whose row we just deleted, passed
     * so we can filter out the removed row in case a racy re-query would be inconsistent — defensive.
     *
     * @param strFileStoreKey
     *            the file store key
     * @param strHashJustRemoved
     *            the hash whose mapping row has just been deleted
     * @return {@code true} if another mapping row still references the blob
     */
    private static boolean isFileStoreKeyStillReferenced( String strFileStoreKey, String strHashJustRemoved )
    {
        for ( DecisionTreeImage image : DecisionTreeImageHome.findByContentHash( strHashJustRemoved ) )
        {
            if ( strFileStoreKey.equals( image.getFileStoreKey( ) ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the default FileStoreService provider.
     *
     * @return the default file store provider
     */
    private static IFileStoreServiceProvider fileStore( )
    {
        return CDI.current( ).select( IFileStoreServiceProvider.class ).get( );
    }

}
