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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Item;

@Dependent
@PipelineNodeType( value = "MINIO_LOG_ANALYZER", name = "Analyseur de logs Minio", description = "Analyse un ou plusieurs fichiers de log stockés dans Minio et retourne des statistiques sur les erreurs et messages de debug" )
public class MinioLogAnalyzerNode extends AbstractPipelineNode
{
    private static final String PATH_NAME = "path";
    private static final String LOG_PATTERN_NAME = "log_pattern";
    private static final String TOP_ERRORS_LIMIT_NAME = "top_errors_limit";

    private static final String BUCKET_TITLE = "Bucket Minio";
    private static final String PATH_TITLE = "Chemin du fichier (supporte les wildcards *)";
    private static final String LOG_PATTERN_TITLE = "Pattern de log";
    private static final String ENDPOINT_TITLE = "Endpoint Minio";
    private static final String LOGIN_TITLE = "Login Minio";
    private static final String PASSWORD_TITLE = "Mot de passe Minio";
    private static final String TOP_ERRORS_LIMIT_TITLE = "Limite du top des erreurs";

    private static final String BUCKET_DESCRIPTION = "Nom du bucket Minio contenant le fichier de log";
    private static final String PATH_DESCRIPTION = "Chemin du fichier de log à analyser dans le bucket. Supporte les wildcards (* pour plusieurs fichiers)";
    private static final String LOG_PATTERN_DESCRIPTION = "Expression régulière pour parser les lignes de log. Doit contenir 2 groupes : niveau (ERROR/DEBUG) et message";
    private static final String ENDPOINT_DESCRIPTION = "URL du serveur Minio (ex: http://localhost:9000)";
    private static final String LOGIN_DESCRIPTION = "Nom d'utilisateur pour l'authentification Minio";
    private static final String PASSWORD_DESCRIPTION = "Mot de passe pour l'authentification Minio";
    private static final String TOP_ERRORS_LIMIT_DESCRIPTION = "Nombre maximum d'erreurs distinctes à retourner dans le top (par défaut: 30)";

    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String DEFAULT_OUTPUT_PORT_DESCRIPTION = "Sortie par défaut";

    private static final String ANALYSIS_RESULTS_KEY = "analysis_results";
    private static final String TOTAL_FILES_KEY = "total_files_analyzed";
    private static final String GLOBAL_ERROR_COUNT_KEY = "global_error_count";
    private static final String GLOBAL_DEBUG_COUNT_KEY = "global_debug_count";
    private static final String GLOBAL_TOTAL_LINES_KEY = "global_total_lines";
    private static final String ERROR_COUNT_KEY = "error_count";
    private static final String DEBUG_COUNT_KEY = "debug_count";
    private static final String TOP_ERRORS_KEY = "top_errors";
    private static final String TOTAL_LINES_KEY = "total_lines";
    private static final String FILE_PATH_KEY = "analyzed_file_path";
    private static final String SUCCESSFUL_ANALYSES_KEY = "successful_analyses";
    private static final String FAILED_ANALYSES_KEY = "failed_analyses";
    private static final String ERROR_KEY = "error";
    private static final String ERROR_MESSAGE_KEY = "error_message";
    private static final String ERROR_COUNT_MAP_KEY = "count";
    private static final String MESSAGE_KEY = "message";

    private static final String ANALYSIS_RESULTS_DESCRIPTION = "Liste des analyses de fichiers";
    private static final String TOTAL_FILES_DESCRIPTION = "Nombre total de fichiers analysés";
    private static final String GLOBAL_ERROR_COUNT_DESCRIPTION = "Nombre total d'erreurs sur tous les fichiers";
    private static final String GLOBAL_DEBUG_COUNT_DESCRIPTION = "Nombre total de messages de debug sur tous les fichiers";
    private static final String GLOBAL_TOTAL_LINES_DESCRIPTION = "Nombre total de lignes sur tous les fichiers";
    private static final String SUCCESSFUL_ANALYSES_DESCRIPTION = "Nombre de fichiers analysés avec succès";
    private static final String FAILED_ANALYSES_DESCRIPTION = "Nombre de fichiers en échec";

    private static final String ERROR_BUCKET_REQUIRED = "The 'bucket' parameter is required";
    private static final String ERROR_PATH_REQUIRED = "The 'path' parameter is required";
    private static final String ERROR_PATTERN_REQUIRED = "The 'log_pattern' parameter is required";
    private static final String ERROR_ENDPOINT_REQUIRED = "The 'endpoint' parameter is required";
    private static final String ERROR_LOGIN_REQUIRED = "The 'login' parameter is required";
    private static final String ERROR_PASSWORD_REQUIRED = "The 'password' parameter is required";
    private static final String ERROR_MINIO_CONNECTION = "Error while connecting to Minio";
    private static final String ERROR_FILE_ANALYSIS = "Error while analyzing the file";
    private static final String ERROR_PATTERN_COMPILATION = "Error while compiling the regex pattern";
    private static final String ERROR_WILDCARD_SEARCH = "Error while searching files with wildcard: ";
    private static final String ERROR_FILE_SEARCH = "Error while searching files: ";
    private static final String ERROR_NO_FILES = "No file to analyze";
    private static final String ERROR_FILE_ANALYSIS_PREFIX = "Error while analyzing the file ";
    private static final String ERROR_THREAD_TIMEOUT = "Timeout or error while waiting for the result: ";
    private static final String ERROR_THREAD_FAILURE = "Timeout or thread error";

    private static final String DEFAULT_LOG_PATTERN = ".*?(ERROR|DEBUG|INFO|WARN)\\s+\\[.*?\\]\\s*(.*)";
    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";
    private static final String DEFAULT_LOGIN = "minio";
    private static final String DEFAULT_PASSWORD = "minio123";

    private static final String ERROR_LEVEL = "ERROR";
    private static final String DEBUG_LEVEL = "DEBUG";

    private static final int DEFAULT_TOP_ERRORS_LIMIT = 30;
    private static final int BUFFER_SIZE = 8192;
    private static final int THREAD_TIMEOUT_MINUTES = 2;

    private static final String WILDCARD_DOT_ESCAPE = "\\.";
    private static final String WILDCARD_STAR_REPLACEMENT = ".*";
    private static final String WILDCARD_QUESTION_REPLACEMENT = ".";
    private static final String REGEX_START_ANCHOR = "^";
    private static final String REGEX_END_ANCHOR = "$";
    private static final String PATH_SEPARATOR = "/";
    private static final String NEWLINE_SEPARATOR = "\n";

    public static final PipelineVariable BUCKET = new PipelineVariable.Builder( MinioNodeKeys.BUCKET ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( BUCKET_TITLE ).description( BUCKET_DESCRIPTION ).build( );

    public static final PipelineVariable PATH = new PipelineVariable.Builder( PATH_NAME ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( PATH_TITLE ).description( PATH_DESCRIPTION ).build( );

    public static final PipelineVariable LOG_PATTERN = new PipelineVariable.Builder( LOG_PATTERN_NAME ).type( PipelineVariable.VariableType.STRING )
            .required( false ).title( LOG_PATTERN_TITLE ).description( LOG_PATTERN_DESCRIPTION ).defaultValue( DEFAULT_LOG_PATTERN ).build( );

    public static final PipelineVariable ENDPOINT = new PipelineVariable.Builder( MinioNodeKeys.ENDPOINT ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( ENDPOINT_TITLE ).description( ENDPOINT_DESCRIPTION ).placeholder( DEFAULT_ENDPOINT ).build( );

    public static final PipelineVariable LOGIN = new PipelineVariable.Builder( MinioNodeKeys.LOGIN ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( LOGIN_TITLE ).description( LOGIN_DESCRIPTION ).placeholder( DEFAULT_LOGIN ).build( );

    public static final PipelineVariable PASSWORD = new PipelineVariable.Builder( MinioNodeKeys.PASSWORD ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( PASSWORD_TITLE ).description( PASSWORD_DESCRIPTION ).placeholder( DEFAULT_PASSWORD ).build( );

    public static final PipelineVariable TOP_ERRORS_LIMIT = new PipelineVariable.Builder( TOP_ERRORS_LIMIT_NAME ).type( PipelineVariable.VariableType.NUMBER )
            .required( false ).title( TOP_ERRORS_LIMIT_TITLE ).description( TOP_ERRORS_LIMIT_DESCRIPTION )
            .defaultValue( String.valueOf( DEFAULT_TOP_ERRORS_LIMIT ) ).build( );

    private static class ErrorCount
    {
        private final String errorMessage;
        private int count;

        /**
         * Constructs an ErrorCount for the given error message with an initial count of one
         *
         * @param errorMessage
         *            the error message being counted
         */
        public ErrorCount( String errorMessage )
        {
            this.errorMessage = errorMessage;
            this.count = 1;
        }

        /**
         * Increments the occurrence count of this error
         */
        public void increment( )
        {
            count++;
        }

        /**
         * Returns the count
         *
         * @return the count
         */
        public int getCount( )
        {
            return count;
        }

        /**
         * Builds a map representation of this error count with its message and count
         *
         * @return a map containing the error message and its occurrence count
         */
        public Map<String, Object> toMap( )
        {
            Map<String, Object> map = new HashMap<>( );
            map.put( MESSAGE_KEY, errorMessage );
            map.put( ERROR_COUNT_MAP_KEY, count );
            return map;
        }
    }

    /**
     * Constructor for MinioLogAnalyzerNode. Initializes the node with the MINIO_LOG_ANALYZER type.
     */
    public MinioLogAnalyzerNode( )
    {
        super( "MINIO_LOG_ANALYZER" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( BUCKET );
        variables.add( PATH );
        variables.add( ENDPOINT );
        variables.add( LOGIN );
        variables.add( PASSWORD );
        variables.add( LOG_PATTERN );
        variables.add( TOP_ERRORS_LIMIT );
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, DEFAULT_INPUT_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> initializeOutputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( OUTPUT_PORT_KEY, DEFAULT_OUTPUT_PORT_DESCRIPTION );
        return ports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        String bucket = getParam( resolvedData, BUCKET );
        String path = getParam( resolvedData, PATH );
        String endpoint = getParam( resolvedData, ENDPOINT );
        String login = getParam( resolvedData, LOGIN );
        String password = getParam( resolvedData, PASSWORD );
        String logPatternStr = getParamWithDefault( resolvedData, LOG_PATTERN, DEFAULT_LOG_PATTERN );
        int topErrorsLimit = Integer.parseInt( getParamWithDefault( resolvedData, TOP_ERRORS_LIMIT, String.valueOf( DEFAULT_TOP_ERRORS_LIMIT ) ) );

        executeAnalysis( context, config, bucket, path, endpoint, login, password, logPatternStr, topErrorsLimit );
        return context;
    }

    /**
     * Executes the complete log analysis process.
     *
     * @param context
     *            the pipeline context
     * @param config
     *            the node configuration
     * @param bucket
     *            the Minio bucket name
     * @param path
     *            the file path (supports wildcards)
     * @param endpoint
     *            the Minio endpoint URL
     * @param login
     *            the Minio login
     * @param password
     *            the Minio password
     * @param logPatternStr
     *            the log pattern regex
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     */
    private void executeAnalysis( PipelineContext context, PipelineNodeConfig config, String bucket, String path, String endpoint, String login,
            String password, String logPatternStr, int topErrorsLimit )
    {
        validateParameters( bucket, path, endpoint, login, password, logPatternStr );
        Pattern logPattern = compilePattern( logPatternStr );
        MinioClient minioClient = createMinioClient( endpoint, login, password );
        List<String> filesToAnalyze = resolveFilePaths( minioClient, bucket, path );
        Map<String, Object> globalResult = analyzeMultipleFiles( minioClient, bucket, filesToAnalyze, logPattern, topErrorsLimit );
        prepareOutputData( context, config, globalResult );
    }

    /**
     * Validates the required parameters for the analysis.
     *
     * @param bucket
     *            the Minio bucket name
     * @param path
     *            the file path
     * @param endpoint
     *            the Minio endpoint
     * @param login
     *            the Minio login
     * @param password
     *            the Minio password
     * @param logPattern
     *            the log pattern
     * @throws IllegalArgumentException
     *             if any required parameter is missing or empty
     */
    private void validateParameters( String bucket, String path, String endpoint, String login, String password, String logPattern )
    {
        if ( bucket == null || bucket.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_BUCKET_REQUIRED );
        }
        if ( path == null || path.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_PATH_REQUIRED );
        }
        if ( endpoint == null || endpoint.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_ENDPOINT_REQUIRED );
        }
        if ( login == null || login.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_LOGIN_REQUIRED );
        }
        if ( password == null || password.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_PASSWORD_REQUIRED );
        }
        if ( logPattern == null || logPattern.trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( ERROR_PATTERN_REQUIRED );
        }
    }

    /**
     * Compiles the regex pattern from the given string.
     *
     * @param patternStr
     *            the pattern string to compile
     * @return the compiled Pattern object
     * @throws RuntimeException
     *             if the pattern compilation fails
     */
    private Pattern compilePattern( String patternStr )
    {
        try
        {
            return Pattern.compile( patternStr );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}: {}", ERROR_PATTERN_COMPILATION, e.getMessage( ), e );
            throw new RuntimeException( ERROR_PATTERN_COMPILATION + ": " + e.getMessage( ), e );
        }
    }

    /**
     * Creates a MinioClient instance with the provided credentials.
     *
     * @param endpoint
     *            the Minio server endpoint
     * @param login
     *            the login username
     * @param password
     *            the login password
     * @return the configured MinioClient
     * @throws RuntimeException
     *             if the client creation fails
     */
    private MinioClient createMinioClient( String endpoint, String login, String password )
    {
        try
        {
            return MinioClient.builder( ).endpoint( endpoint ).credentials( login, password ).build( );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}: {}", ERROR_MINIO_CONNECTION, e.getMessage( ), e );
            throw new RuntimeException( ERROR_MINIO_CONNECTION + ": " + e.getMessage( ), e );
        }
    }

    /**
     * Resolves file paths, handling wildcard patterns.
     *
     * @param minioClient
     *            the Minio client
     * @param bucket
     *            the bucket name
     * @param path
     *            the file path (may contain wildcards)
     * @return list of resolved file paths
     */
    private List<String> resolveFilePaths( MinioClient minioClient, String bucket, String path )
    {
        List<String> filePaths = new ArrayList<>( );

        if ( path.contains( "*" ) )
        {
            resolveWildcardPaths( minioClient, bucket, path, filePaths );
        }
        else
        {
            filePaths.add( path );
        }

        return filePaths;
    }

    /**
     * Resolves wildcard file paths by listing objects in Minio.
     *
     * @param minioClient
     *            the Minio client
     * @param bucket
     *            the bucket name
     * @param path
     *            the wildcard path
     * @param filePaths
     *            the list to populate with resolved paths
     */
    private void resolveWildcardPaths( MinioClient minioClient, String bucket, String path, List<String> filePaths )
    {
        String directory = path.substring( 0, path.lastIndexOf( PATH_SEPARATOR ) );
        String filePattern = path.substring( path.lastIndexOf( PATH_SEPARATOR ) + 1 );

        try
        {
            ListObjectsArgs listArgs = ListObjectsArgs.builder( ).bucket( bucket ).prefix( directory + PATH_SEPARATOR ).build( );

            Iterable<Result<Item>> objects = minioClient.listObjects( listArgs );
            Pattern pattern = createWildcardPattern( filePattern );

            for ( Result<Item> result : objects )
            {
                Item item = result.get( );
                String objectName = item.objectName( );
                String fileName = objectName.substring( objectName.lastIndexOf( PATH_SEPARATOR ) + 1 );

                if ( pattern.matcher( fileName ).matches( ) )
                {
                    filePaths.add( objectName );
                }
            }
        }
        catch( ErrorResponseException | InsufficientDataException | InternalException | InvalidResponseException | ServerException | XmlParserException
                | IOException | IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException e )
        {
            AppLogService.error( "{}{}", ERROR_WILDCARD_SEARCH, e.getMessage( ), e );
            throw new RuntimeException( ERROR_FILE_SEARCH + e.getMessage( ), e );
        }
    }

    /**
     * Creates a regex pattern from a wildcard string.
     *
     * @param wildcard
     *            the wildcard pattern
     * @return the compiled regex Pattern
     */
    private Pattern createWildcardPattern( String wildcard )
    {
        String regex = wildcard.replace( ".", WILDCARD_DOT_ESCAPE ).replace( "*", WILDCARD_STAR_REPLACEMENT ).replace( "?", WILDCARD_QUESTION_REPLACEMENT );
        return Pattern.compile( REGEX_START_ANCHOR + regex + REGEX_END_ANCHOR );
    }

    /**
     * Analyzes multiple log files in parallel.
     *
     * @param minioClient
     *            the Minio client
     * @param bucket
     *            the bucket name
     * @param filePaths
     *            the list of file paths to analyze
     * @param logPattern
     *            the log pattern for parsing
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     * @return aggregated analysis results
     */
    private Map<String, Object> analyzeMultipleFiles( MinioClient minioClient, String bucket, List<String> filePaths, Pattern logPattern, int topErrorsLimit )
    {
        if ( filePaths.isEmpty( ) )
        {
            AppLogService.error( ERROR_NO_FILES );
            return createEmptyGlobalResult( );
        }

        List<CompletableFuture<Map<String, Object>>> futures = createAnalysisTasks( minioClient, bucket, filePaths, logPattern, topErrorsLimit );

        return collectAnalysisResults( futures );
    }

    /**
     * Creates asynchronous analysis tasks for each file, submitted to the platform Virtual Thread executor.
     *
     * @param minioClient
     *            the Minio client
     * @param bucket
     *            the bucket name
     * @param filePaths
     *            the file paths to analyze
     * @param logPattern
     *            the log pattern
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     * @return list of CompletableFuture tasks
     */
    private List<CompletableFuture<Map<String, Object>>> createAnalysisTasks( MinioClient minioClient, String bucket, List<String> filePaths,
            Pattern logPattern, int topErrorsLimit )
    {
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>( );

        for ( String filePath : filePaths )
        {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync( ( ) -> {
                try
                {
                    return analyzeLogFile( minioClient, bucket, filePath, logPattern, topErrorsLimit );
                }
                catch( Exception e )
                {
                    AppLogService.error( "{}{}: {}", ERROR_FILE_ANALYSIS_PREFIX, filePath, e.getMessage( ), e );
                    return createErrorResult( filePath, e.getMessage( ) );
                }
            }, BLOCKING_EXECUTOR );
            futures.add( future );
        }

        return futures;
    }

    /**
     * Collects and aggregates results from analysis tasks. No executor shutdown — the container owns the executor lifecycle.
     *
     * @param futures
     *            the list of analysis tasks
     * @return aggregated global results
     */
    private Map<String, Object> collectAnalysisResults( List<CompletableFuture<Map<String, Object>>> futures )
    {
        List<Map<String, Object>> analysisResults = new ArrayList<>( );
        int globalErrorCount = 0;
        int globalDebugCount = 0;
        int globalTotalLines = 0;
        int successfulAnalyses = 0;

        for ( CompletableFuture<Map<String, Object>> future : futures )
        {
            try
            {
                Map<String, Object> fileResult = future.get( THREAD_TIMEOUT_MINUTES, TimeUnit.MINUTES );
                analysisResults.add( fileResult );

                if ( !fileResult.containsKey( ERROR_KEY ) )
                {
                    globalErrorCount += (Integer) fileResult.get( ERROR_COUNT_KEY );
                    globalDebugCount += (Integer) fileResult.get( DEBUG_COUNT_KEY );
                    globalTotalLines += (Integer) fileResult.get( TOTAL_LINES_KEY );
                    successfulAnalyses++;
                }
            }
            catch( InterruptedException | ExecutionException | TimeoutException e )
            {
                AppLogService.error( "{}{}", ERROR_THREAD_TIMEOUT, e.getMessage( ), e );
                analysisResults.add( createErrorResult( "unknown", ERROR_THREAD_FAILURE ) );
                if ( e instanceof InterruptedException )
                {
                    Thread.currentThread( ).interrupt( );
                }
            }
        }

        return createGlobalResult( analysisResults, successfulAnalyses, globalErrorCount, globalDebugCount, globalTotalLines );
    }

    /**
     * Creates the global result map with aggregated statistics.
     *
     * @param analysisResults
     *            the individual file analysis results
     * @param successfulAnalyses
     *            the number of successful analyses
     * @param globalErrorCount
     *            the total error count
     * @param globalDebugCount
     *            the total debug count
     * @param globalTotalLines
     *            the total line count
     * @return the global result map
     */
    private Map<String, Object> createGlobalResult( List<Map<String, Object>> analysisResults, int successfulAnalyses, int globalErrorCount,
            int globalDebugCount, int globalTotalLines )
    {
        Map<String, Object> globalResult = new HashMap<>( );
        globalResult.put( ANALYSIS_RESULTS_KEY, analysisResults );
        globalResult.put( TOTAL_FILES_KEY, analysisResults.size( ) );
        globalResult.put( SUCCESSFUL_ANALYSES_KEY, successfulAnalyses );
        globalResult.put( FAILED_ANALYSES_KEY, analysisResults.size( ) - successfulAnalyses );
        globalResult.put( GLOBAL_ERROR_COUNT_KEY, globalErrorCount );
        globalResult.put( GLOBAL_DEBUG_COUNT_KEY, globalDebugCount );
        globalResult.put( GLOBAL_TOTAL_LINES_KEY, globalTotalLines );
        return globalResult;
    }

    /**
     * Creates an empty global result when no files are found.
     *
     * @return empty result map with zero values
     */
    private Map<String, Object> createEmptyGlobalResult( )
    {
        Map<String, Object> result = new HashMap<>( );
        result.put( ANALYSIS_RESULTS_KEY, new ArrayList<>( ) );
        result.put( TOTAL_FILES_KEY, 0 );
        result.put( SUCCESSFUL_ANALYSES_KEY, 0 );
        result.put( FAILED_ANALYSES_KEY, 0 );
        result.put( GLOBAL_ERROR_COUNT_KEY, 0 );
        result.put( GLOBAL_DEBUG_COUNT_KEY, 0 );
        result.put( GLOBAL_TOTAL_LINES_KEY, 0 );
        return result;
    }

    /**
     * Creates an error result for a failed file analysis.
     *
     * @param filePath
     *            the path of the file that failed
     * @param errorMessage
     *            the error message
     * @return error result map
     */
    private Map<String, Object> createErrorResult( String filePath, String errorMessage )
    {
        Map<String, Object> result = new HashMap<>( );
        result.put( FILE_PATH_KEY, filePath );
        result.put( ERROR_KEY, true );
        result.put( ERROR_MESSAGE_KEY, errorMessage );
        result.put( ERROR_COUNT_KEY, 0 );
        result.put( DEBUG_COUNT_KEY, 0 );
        result.put( TOTAL_LINES_KEY, 0 );
        result.put( TOP_ERRORS_KEY, new ArrayList<>( ) );
        return result;
    }

    /**
     * Analyzes a single log file from Minio.
     *
     * @param minioClient
     *            the Minio client
     * @param bucket
     *            the bucket name
     * @param path
     *            the file path
     * @param logPattern
     *            the log pattern for parsing
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     * @return analysis result map
     */
    private Map<String, Object> analyzeLogFile( MinioClient minioClient, String bucket, String path, Pattern logPattern, int topErrorsLimit )
    {
        try
        {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder( ).bucket( bucket ).object( path ).build( );

            try ( InputStream stream = minioClient.getObject( getObjectArgs ) )
            {
                return processLogFileOptimized( stream, path, logPattern, topErrorsLimit );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "{} ({}): {}", ERROR_FILE_ANALYSIS, path, e.getMessage( ), e );
            throw new RuntimeException( ERROR_FILE_ANALYSIS + " (" + path + "): " + e.getMessage( ), e );
        }
    }

    /**
     * Processes a log file stream with optimized buffering.
     *
     * @param stream
     *            the input stream
     * @param filePath
     *            the file path for logging
     * @param logPattern
     *            the log pattern
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     * @return processing result map
     * @throws Exception
     *             if processing fails
     */
    private Map<String, Object> processLogFileOptimized( InputStream stream, String filePath, Pattern logPattern, int topErrorsLimit ) throws Exception
    {
        int errorCount = 0;
        int debugCount = 0;
        int totalLines = 0;
        Map<String, ErrorCount> errorMessages = new HashMap<>( );

        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( stream, StandardCharsets.UTF_8 ), BUFFER_SIZE ) )
        {
            StringBuilder buffer = new StringBuilder( );
            char [ ] charBuffer = new char [ BUFFER_SIZE];
            int charsRead;

            while ( ( charsRead = reader.read( charBuffer ) ) != -1 )
            {
                buffer.append( charBuffer, 0, charsRead );

                ProcessingResult result = processBufferContent( buffer, logPattern, errorMessages, totalLines, errorCount, debugCount );
                totalLines = result.getTotalLines( );
                errorCount = result.getErrorCount( );
                debugCount = result.getDebugCount( );
            }

            ProcessingResult finalResult = processRemainingBuffer( buffer, logPattern, errorMessages, totalLines, errorCount, debugCount );
            totalLines = finalResult.getTotalLines( );
            errorCount = finalResult.getErrorCount( );
            debugCount = finalResult.getDebugCount( );
        }

        return createAnalysisResult( errorCount, debugCount, totalLines, errorMessages, filePath, topErrorsLimit );
    }

    /**
     * Processes the content in the buffer and extracts complete lines.
     *
     * @param buffer
     *            the string buffer
     * @param logPattern
     *            the log pattern
     * @param errorMessages
     *            the error message map
     * @param totalLines
     *            current total line count
     * @param errorCount
     *            current error count
     * @param debugCount
     *            current debug count
     * @return processing result with updated counts
     */
    private ProcessingResult processBufferContent( StringBuilder buffer, Pattern logPattern, Map<String, ErrorCount> errorMessages, int totalLines,
            int errorCount, int debugCount )
    {
        int lastNewlineIndex;
        while ( ( lastNewlineIndex = buffer.lastIndexOf( NEWLINE_SEPARATOR ) ) != -1 )
        {
            String content = buffer.substring( 0, lastNewlineIndex );
            buffer.delete( 0, lastNewlineIndex + 1 );

            String [ ] lines = content.split( NEWLINE_SEPARATOR );
            ProcessingResult result = processLines( lines, logPattern, errorMessages, totalLines, errorCount, debugCount );
            totalLines = result.getTotalLines( );
            errorCount = result.getErrorCount( );
            debugCount = result.getDebugCount( );
        }

        return new ProcessingResult( totalLines, errorCount, debugCount );
    }

    /**
     * Processes remaining content in buffer after main processing.
     *
     * @param buffer
     *            the string buffer
     * @param logPattern
     *            the log pattern
     * @param errorMessages
     *            the error message map
     * @param totalLines
     *            current total line count
     * @param errorCount
     *            current error count
     * @param debugCount
     *            current debug count
     * @return final processing result
     */
    private ProcessingResult processRemainingBuffer( StringBuilder buffer, Pattern logPattern, Map<String, ErrorCount> errorMessages, int totalLines,
            int errorCount, int debugCount )
    {
        if ( buffer.length( ) > 0 )
        {
            String [ ] remainingLines = buffer.toString( ).split( NEWLINE_SEPARATOR );
            return processLines( remainingLines, logPattern, errorMessages, totalLines, errorCount, debugCount );
        }

        return new ProcessingResult( totalLines, errorCount, debugCount );
    }

    /**
     * Processes an array of log lines.
     *
     * @param lines
     *            the log lines to process
     * @param logPattern
     *            the log pattern
     * @param errorMessages
     *            the error message map
     * @param totalLines
     *            current total line count
     * @param errorCount
     *            current error count
     * @param debugCount
     *            current debug count
     * @return processing result with updated counts
     */
    private ProcessingResult processLines( String [ ] lines, Pattern logPattern, Map<String, ErrorCount> errorMessages, int totalLines, int errorCount,
            int debugCount )
    {
        for ( String line : lines )
        {
            if ( !line.trim( ).isEmpty( ) )
            {
                totalLines++;

                Matcher matcher = logPattern.matcher( line );
                if ( matcher.find( ) )
                {
                    String level = matcher.group( 1 );
                    String message = matcher.group( 2 );

                    switch( level.toUpperCase( ) )
                    {
                        case ERROR_LEVEL:
                            errorCount++;
                            errorMessages.computeIfAbsent( message, ErrorCount::new ).increment( );
                            break;
                        case DEBUG_LEVEL:
                            debugCount++;
                            break;
                    }
                }
            }
        }

        return new ProcessingResult( totalLines, errorCount, debugCount );
    }

    /**
     * Creates the final analysis result map.
     *
     * @param errorCount
     *            the total error count
     * @param debugCount
     *            the total debug count
     * @param totalLines
     *            the total line count
     * @param errorMessages
     *            the error message map
     * @param filePath
     *            the analyzed file path
     * @param topErrorsLimit
     *            the maximum number of top errors to return
     * @return analysis result map
     */
    private Map<String, Object> createAnalysisResult( int errorCount, int debugCount, int totalLines, Map<String, ErrorCount> errorMessages, String filePath,
            int topErrorsLimit )
    {
        Map<String, Object> result = new HashMap<>( );
        result.put( ERROR_COUNT_KEY, errorCount );
        result.put( DEBUG_COUNT_KEY, debugCount );
        result.put( TOTAL_LINES_KEY, totalLines );
        result.put( FILE_PATH_KEY, filePath );

        List<Map<String, Object>> topErrors = errorMessages.values( ).stream( ).sorted( ( e1, e2 ) -> Integer.compare( e2.getCount( ), e1.getCount( ) ) )
                .limit( topErrorsLimit ).map( ErrorCount::toMap ).toList( );

        result.put( TOP_ERRORS_KEY, topErrors );
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( ANALYSIS_RESULTS_KEY, ANALYSIS_RESULTS_DESCRIPTION );
        outputs.put( TOTAL_FILES_KEY, TOTAL_FILES_DESCRIPTION );
        outputs.put( SUCCESSFUL_ANALYSES_KEY, SUCCESSFUL_ANALYSES_DESCRIPTION );
        outputs.put( FAILED_ANALYSES_KEY, FAILED_ANALYSES_DESCRIPTION );
        outputs.put( GLOBAL_ERROR_COUNT_KEY, GLOBAL_ERROR_COUNT_DESCRIPTION );
        outputs.put( GLOBAL_DEBUG_COUNT_KEY, GLOBAL_DEBUG_COUNT_DESCRIPTION );
        outputs.put( GLOBAL_TOTAL_LINES_KEY, GLOBAL_TOTAL_LINES_DESCRIPTION );
        return outputs;
    }

    /**
     * Helper class to hold processing results.
     */
    private static class ProcessingResult
    {
        private final int totalLines;
        private final int errorCount;
        private final int debugCount;

        /**
         * Constructs a ProcessingResult with the given line counts
         *
         * @param totalLines
         *            the total number of lines processed
         * @param errorCount
         *            the number of error lines
         * @param debugCount
         *            the number of debug lines
         */
        public ProcessingResult( int totalLines, int errorCount, int debugCount )
        {
            this.totalLines = totalLines;
            this.errorCount = errorCount;
            this.debugCount = debugCount;
        }

        /**
         * Returns the total lines
         *
         * @return the total lines
         */
        public int getTotalLines( )
        {
            return totalLines;
        }

        /**
         * Returns the error count
         *
         * @return the error count
         */
        public int getErrorCount( )
        {
            return errorCount;
        }

        /**
         * Returns the debug count
         *
         * @return the debug count
         */
        public int getDebugCount( )
        {
            return debugCount;
        }
    }
}
