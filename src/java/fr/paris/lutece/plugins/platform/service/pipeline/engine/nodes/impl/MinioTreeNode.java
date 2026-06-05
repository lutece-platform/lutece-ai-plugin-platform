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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.Duration;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;

@PipelineNodeType( value = "MINIO_TREE", name = "Minio Tree Structure", description = "Génère une structure d'arbre JSON des buckets Minio affichant les dossiers et le nombre de fichiers. Supporte les patterns dans les noms de buckets (*, ?, regex)" )
public class MinioTreeNode extends AbstractPipelineNode
{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper( );
    private static final String VAR_PREFIX = "prefix";
    private static final String VAR_MAX_DEPTH = "maxDepth";
    private static final String VAR_INCLUDE_FILES = "includeFiles";
    private static final String VAR_PATTERN_TYPE = "patternType";

    private final List<PipelineVariable> configurableVariables;

    /**
     * Builds a MinIO tree node and declares its configurable variables (endpoint, credentials, bucket pattern, prefix, depth, options)
     */
    public MinioTreeNode( )
    {
        super( "MINIO_TREE" );
        configurableVariables = Arrays.asList(
                new PipelineVariable.Builder( MinioNodeKeys.ENDPOINT ).type( PipelineVariable.VariableType.STRING ).required( true ).title( "Minio Endpoint" )
                        .description( "URL du serveur Minio" ).build( ),
                new PipelineVariable.Builder( MinioNodeKeys.LOGIN ).type( PipelineVariable.VariableType.STRING ).required( true ).title( "Login" )
                        .description( "Nom d'utilisateur Minio" ).build( ),
                new PipelineVariable.Builder( MinioNodeKeys.PASSWORD ).type( PipelineVariable.VariableType.STRING ).required( true ).title( "Password" )
                        .description( "Mot de passe Minio" ).textarea( false ).build( ),
                new PipelineVariable.Builder( MinioNodeKeys.BUCKET ).type( PipelineVariable.VariableType.STRING ).required( true ).title( "Bucket Pattern" )
                        .description( "Nom du bucket ou pattern (* pour tous, bucket* pour préfixe, regex possible)" ).build( ),
                new PipelineVariable.Builder( VAR_PATTERN_TYPE ).type( PipelineVariable.VariableType.STRING ).required( false ).title( "Pattern Type" )
                        .description( "Type de pattern : 'wildcard' (*, ?) ou 'regex'" ).defaultValue( "wildcard" ).build( ),
                new PipelineVariable.Builder( VAR_PREFIX ).type( PipelineVariable.VariableType.STRING ).required( false ).title( "Prefix" )
                        .description( "Préfixe pour filtrer les objets dans chaque bucket" ).defaultValue( "" ).build( ),
                new PipelineVariable.Builder( VAR_MAX_DEPTH ).type( PipelineVariable.VariableType.NUMBER ).required( false ).title( "Max Depth" )
                        .description( "Profondeur maximale de l'arbre" ).defaultValue( 10 ).build( ),
                new PipelineVariable.Builder( VAR_INCLUDE_FILES ).type( PipelineVariable.VariableType.BOOLEAN ).required( false ).title( "Include Files" )
                        .description( "Inclure la liste des fichiers dans la sortie" ).defaultValue( false ).build( ) );
    }

    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        return configurableVariables;
    }

    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( "input", "Port d'entrée par défaut" );
        return Collections.unmodifiableMap( ports );
    }

    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( "output", "Structure d'arbre JSON des buckets Minio correspondant au pattern" );
        return Collections.unmodifiableMap( ports );
    }

    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( "tree_json", "Structure d'arbre JSON complète" );
        outputs.put( "matched_buckets", "Liste des buckets qui correspondent au pattern" );
        outputs.put( "bucket_count", "Nombre de buckets scannés" );
        outputs.put( "total_folders", "Nombre total de dossiers" );
        outputs.put( "total_files", "Nombre total de fichiers" );
        outputs.put( "buckets_summary", "Résumé par bucket" );
        return outputs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        try
        {
            String endpoint = getParam( resolvedData, getVariable( MinioNodeKeys.ENDPOINT ) );
            String login = getParam( resolvedData, getVariable( MinioNodeKeys.LOGIN ) );
            String password = getParam( resolvedData, getVariable( MinioNodeKeys.PASSWORD ) );
            String bucketPattern = getParam( resolvedData, getVariable( MinioNodeKeys.BUCKET ) );
            String patternType = getParamWithDefault( resolvedData, getVariable( VAR_PATTERN_TYPE ), "wildcard" );
            String prefix = getParamWithDefault( resolvedData, getVariable( VAR_PREFIX ), "" );
            int maxDepth = getParamAsInteger( resolvedData, getVariable( VAR_MAX_DEPTH ) );
            boolean includeFiles = getParamAsBoolean( resolvedData, getVariable( VAR_INCLUDE_FILES ) );

            MinioClient minioClient = createMinioClient( endpoint, login, password );

            List<String> allBuckets = minioClient.listBuckets( ).stream( ).map( Bucket::name ).toList( );

            List<String> matchingBuckets = filterBucketsByPattern( allBuckets, bucketPattern, patternType );

            if ( matchingBuckets.isEmpty( ) )
            {
                throw new RuntimeException( "No bucket matches the pattern: " + bucketPattern );
            }

            ObjectMapper mapper = OBJECT_MAPPER;
            ObjectNode result = mapper.createObjectNode( );
            ArrayNode bucketsArray = mapper.createArrayNode( );

            int totalFolders = 0;
            int totalFiles = 0;
            List<BucketSummary> bucketsSummary = new ArrayList<>( );

            for ( String bucketName : matchingBuckets )
            {
                TreeNode bucketTree = buildTree( minioClient, bucketName, prefix, maxDepth, includeFiles );

                ObjectNode bucketNode = mapper.createObjectNode( );
                bucketNode.put( "bucket", bucketName );
                bucketNode.put( "prefix", prefix );
                bucketNode.set( "structure", convertTreeToJson( bucketTree, mapper ) );

                int bucketFolders = countFolders( bucketTree );
                int bucketFiles = bucketTree.totalFiles;

                bucketNode.put( "folder_count", bucketFolders );
                bucketNode.put( "file_count", bucketFiles );

                ArrayNode pathsArray = mapper.createArrayNode( );
                for ( String path : getPathsWithFiles( bucketTree ) )
                {
                    pathsArray.add( path );
                }
                bucketNode.set( "paths_with_files", pathsArray );

                bucketsArray.add( bucketNode );

                totalFolders += bucketFolders;
                totalFiles += bucketFiles;

                bucketsSummary.add( new BucketSummary( bucketName, bucketFolders, bucketFiles ) );
            }

            result.put( "pattern", bucketPattern );
            result.put( "pattern_type", patternType );
            result.put( "bucket_count", matchingBuckets.size( ) );
            result.put( "total_folders", totalFolders );
            result.put( "total_files", totalFiles );
            result.set( "buckets", bucketsArray );

            ArrayNode matchedBucketsArray = mapper.createArrayNode( );
            matchingBuckets.forEach( matchedBucketsArray::add );
            result.set( "matched_buckets", matchedBucketsArray );

            ArrayNode summaryArray = mapper.createArrayNode( );
            for ( BucketSummary summary : bucketsSummary )
            {
                ObjectNode summaryNode = mapper.createObjectNode( );
                summaryNode.put( "bucket", summary.name );
                summaryNode.put( "folders", summary.folders );
                summaryNode.put( "files", summary.files );
                summaryArray.add( summaryNode );
            }
            result.set( "buckets_summary", summaryArray );

            Map<String, Object> outputData = new HashMap<>( );
            outputData.put( "tree_json", mapper.writeValueAsString( result ) );
            outputData.put( "matched_buckets", matchingBuckets );
            outputData.put( "bucket_count", matchingBuckets.size( ) );
            outputData.put( "total_folders", totalFolders );
            outputData.put( "total_files", totalFiles );
            outputData.put( "buckets_summary", bucketsSummary );

            prepareOutputData( context, config, outputData );

        }
        catch( Exception e )
        {
            throw new RuntimeException( "Error while executing the Minio Tree node: " + e.getMessage( ), e );
        }

        return context;
    }

    /**
     * Returns the configurable variable definition matching the given name
     *
     * @param name
     *            the variable name to look up
     * @return the matching pipeline variable
     */
    private PipelineVariable getVariable( String name )
    {
        return configurableVariables.stream( ).filter( v -> v.getName( ).equals( name ) ).findFirst( )
                .orElseThrow( ( ) -> new RuntimeException( "Variable not found: " + name ) );
    }

    /**
     * Filters the bucket names against the pattern, dispatching to regex or wildcard matching according to the pattern type
     *
     * @param buckets
     *            the bucket names to filter
     * @param pattern
     *            the pattern to match
     * @param patternType
     *            the pattern type ("regex" or "wildcard")
     * @return the list of matching bucket names
     */
    private List<String> filterBucketsByPattern( List<String> buckets, String pattern, String patternType )
    {
        if ( "regex".equalsIgnoreCase( patternType ) )
        {
            return filterByRegex( buckets, pattern );
        }
        else
        {
            return filterByWildcard( buckets, pattern );
        }
    }

    /**
     * Filters the bucket names against a wildcard pattern (translating * and ? to a case-insensitive regex)
     *
     * @param buckets
     *            the bucket names to filter
     * @param pattern
     *            the wildcard pattern
     * @return the list of matching bucket names
     */
    private List<String> filterByWildcard( List<String> buckets, String pattern )
    {
        String regex = pattern.replace( ".", "\\." ).replace( "*", ".*" ).replace( "?", "." );

        Pattern compiledPattern = Pattern.compile( "^" + regex + "$", Pattern.CASE_INSENSITIVE );

        return buckets.stream( ).filter( bucket -> compiledPattern.matcher( bucket ).matches( ) ).toList( );
    }

    /**
     * Filters the bucket names whose value matches the given case-insensitive regular expression
     *
     * @param buckets
     *            the bucket names to filter
     * @param regex
     *            the regular expression
     * @return the list of matching bucket names
     */
    private List<String> filterByRegex( List<String> buckets, String regex )
    {
        try
        {
            Pattern compiledPattern = Pattern.compile( regex, Pattern.CASE_INSENSITIVE );
            return buckets.stream( ).filter( bucket -> compiledPattern.matcher( bucket ).find( ) ).toList( );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Invalid regex pattern: " + regex, e );
        }
    }

    private static final long MINIO_TIMEOUT_MS = Duration.ofSeconds( 30 ).toMillis( );

    /**
     * Builds a MinIO client with a 30s connect/read/write timeout.
     *
     * @param endpoint
     *            the MinIO endpoint URL
     * @param accessKey
     *            the access key
     * @param secretKey
     *            the secret key
     * @return the configured MinIO client
     */
    private MinioClient createMinioClient( String endpoint, String accessKey, String secretKey )
    {
        MinioClient client = MinioClient.builder( ).endpoint( endpoint ).credentials( accessKey, secretKey ).build( );
        client.setTimeout( MINIO_TIMEOUT_MS, MINIO_TIMEOUT_MS, MINIO_TIMEOUT_MS );
        return client;
    }

    /**
     * Builds the folder tree of a bucket by listing its objects and aggregating file counts per node up to the maximum depth
     *
     * @param client
     *            the MinIO client
     * @param bucket
     *            the bucket name
     * @param prefix
     *            the object prefix to strip and filter on
     * @param maxDepth
     *            the maximum tree depth
     * @param includeFiles
     *            whether to keep file names in the tree
     * @return the root tree node of the bucket
     * @throws Exception
     *             if listing the bucket objects fails
     */
    private TreeNode buildTree( MinioClient client, String bucket, String prefix, int maxDepth, boolean includeFiles ) throws Exception
    {
        TreeNode root = new TreeNode( "" );
        Map<String, TreeNode> nodeMap = new HashMap<>( );
        nodeMap.put( "", root );

        Iterable<Result<Item>> results = client.listObjects( ListObjectsArgs.builder( ).bucket( bucket ).prefix( prefix ).recursive( true ).build( ) );

        for ( Result<Item> result : results )
        {
            Item item = result.get( );
            String objectName = item.objectName( );

            if ( !prefix.isEmpty( ) && objectName.startsWith( prefix ) )
            {
                objectName = objectName.substring( prefix.length( ) );
                if ( objectName.startsWith( "/" ) )
                {
                    objectName = objectName.substring( 1 );
                }
            }

            String [ ] parts = objectName.split( "/" );
            if ( parts.length > maxDepth )
            {
                continue;
            }

            TreeNode currentNode = root;
            StringBuilder pathBuilder = new StringBuilder( );

            for ( int i = 0; i < parts.length - 1; i++ )
            {
                if ( pathBuilder.length( ) > 0 )
                {
                    pathBuilder.append( "/" );
                }
                pathBuilder.append( parts [i] );
                String path = pathBuilder.toString( );

                TreeNode childNode = nodeMap.get( path );
                if ( childNode == null )
                {
                    childNode = new TreeNode( parts [i] );
                    currentNode.children.put( parts [i], childNode );
                    nodeMap.put( path, childNode );
                }
                currentNode = childNode;
            }

            if ( !item.isDir( ) )
            {
                currentNode.fileCount++;
                currentNode.totalFiles++;

                TreeNode parent;
                for ( int i = 0; i < parts.length - 1; i++ )
                {
                    String path = String.join( "/", Arrays.copyOfRange( parts, 0, i + 1 ) );
                    parent = nodeMap.get( path );
                    if ( parent != null )
                    {
                        parent.totalFiles++;
                    }
                }

                if ( includeFiles )
                {
                    currentNode.files.add( parts [parts.length - 1] );
                }
            }
        }

        return root;
    }

    /**
     * Converts a tree node and its descendants into a JSON object node
     *
     * @param node
     *            the tree node to convert
     * @param mapper
     *            the object mapper used to create JSON nodes
     * @return the JSON representation of the tree node
     */
    private ObjectNode convertTreeToJson( TreeNode node, ObjectMapper mapper )
    {
        ObjectNode jsonNode = mapper.createObjectNode( );
        jsonNode.put( "name", node.name );
        jsonNode.put( "fileCount", node.fileCount );
        jsonNode.put( "totalFiles", node.totalFiles );

        if ( !node.files.isEmpty( ) )
        {
            ArrayNode filesArray = mapper.createArrayNode( );
            node.files.forEach( filesArray::add );
            jsonNode.set( "files", filesArray );
        }

        if ( !node.children.isEmpty( ) )
        {
            ArrayNode childrenArray = mapper.createArrayNode( );
            for ( TreeNode child : node.children.values( ) )
            {
                childrenArray.add( convertTreeToJson( child, mapper ) );
            }
            jsonNode.set( "folders", childrenArray );
        }

        return jsonNode;
    }

    /**
     * Counts the total number of folders contained in the tree rooted at the given node
     *
     * @param node
     *            the root tree node
     * @return the total folder count
     */
    private int countFolders( TreeNode node )
    {
        int count = node.children.size( );
        for ( TreeNode child : node.children.values( ) )
        {
            count += countFolders( child );
        }
        return count;
    }

    /**
     * Returns the list of folder paths that directly contain files, with their file count
     *
     * @param node
     *            the root tree node
     * @return the list of paths holding files
     */
    private List<String> getPathsWithFiles( TreeNode node )
    {
        List<String> paths = new ArrayList<>( );
        collectPathsWithFiles( node, "", paths );
        return paths;
    }

    /**
     * Recursively collects the paths of nodes that directly contain files into the given accumulator
     *
     * @param node
     *            the current tree node
     * @param currentPath
     *            the path accumulated up to the parent node
     * @param paths
     *            the accumulator collecting the matching paths
     */
    private void collectPathsWithFiles( TreeNode node, String currentPath, List<String> paths )
    {
        String path = currentPath.isEmpty( ) ? node.name : ( currentPath + ( node.name.isEmpty( ) ? "" : "/" + node.name ) );

        if ( node.fileCount > 0 )
        {
            paths.add( path + " (" + node.fileCount + " fichiers)" );
        }

        for ( TreeNode child : node.children.values( ) )
        {
            collectPathsWithFiles( child, path, paths );
        }
    }

    private static class TreeNode
    {
        String name;
        Map<String, TreeNode> children = new TreeMap<>( );
        List<String> files = new ArrayList<>( );
        int fileCount = 0;
        int totalFiles = 0;

        TreeNode( String name )
        {
            this.name = name;
        }
    }

    private static class BucketSummary
    {
        String name;
        int folders;
        int files;

        BucketSummary( String name, int folders, int files )
        {
            this.name = name;
            this.folders = folders;
            this.files = files;
        }
    }
}
