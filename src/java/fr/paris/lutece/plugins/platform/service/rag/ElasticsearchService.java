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
package fr.paris.lutece.plugins.platform.service.rag;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import jakarta.annotation.PreDestroy;
import fr.paris.lutece.portal.service.util.AppLogService;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Named( "platform.elasticsearchService" )
public class ElasticsearchService
{

    private static final String SERVER_URL_FORMAT = "%s://%s:%d";
    private static final String INDEX_NAME_FORMAT = "dataset_%d";

    private static final String LOG_ERROR_EXCEPTION = "Error during %s operation: %s";
    private static final String LOG_CLIENT_CLOSED = "Elasticsearch clients closed successfully";
    private static final String LOG_ERROR_CLOSING = "Error closing Elasticsearch clients: %s";

    private static final String EXCEPTION_OPERATION_FAILED = "Elasticsearch operation failed";

    private static final String OPERATION_DELETE_EMBEDDINGS = "deleted embeddings for document %d from index";
    private static final String OPERATION_DELETE_INDEX = "deleted index";

    @Inject
    @ConfigProperty( name = "platform.agent.elasticsearch.host" )
    private String esHost;

    @Inject
    @ConfigProperty( name = "platform.agent.elasticsearch.port", defaultValue = "9200" )
    private int esPort;

    @Inject
    @ConfigProperty( name = "platform.agent.elasticsearch.protocol" )
    private String esProtocol;

    @Inject
    @ConfigProperty( name = "platform.agent.elasticsearch.username" )
    private Optional<String> esUser;

    @Inject
    @ConfigProperty( name = "platform.agent.elasticsearch.password" )
    private Optional<String> esPassword;

    private RestClient restClient;
    private RestClientTransport transport;
    private ElasticsearchClient client;

    /**
     * Initializes the Elasticsearch client once configuration values are injected.
     */
    @PostConstruct
    void init( )
    {
        this.restClient = createRestClient( );
        this.transport = new RestClientTransport( restClient, new JacksonJsonpMapper( ) );
        this.client = new ElasticsearchClient( transport );
    }

    /**
     * Creates and configures the RestClient instance with authentication if needed
     *
     * @return configured RestClient instance
     */
    private RestClient createRestClient( )
    {
        String serverUrl = String.format( SERVER_URL_FORMAT, esProtocol, esHost, esPort );
        AppLogService.info( "Elasticsearch Url {}", serverUrl );
        RestClientBuilder builder = RestClient.builder( HttpHost.create( serverUrl ) );

        if ( esUser.isPresent( ) && !esUser.get( ).isEmpty( ) )
        {
            CredentialsProvider creds = new BasicCredentialsProvider( );
            creds.setCredentials( AuthScope.ANY, new UsernamePasswordCredentials( esUser.get( ), esPassword.orElse( "" ) ) );
            builder.setHttpClientConfigCallback( h -> h.setDefaultCredentialsProvider( creds ) );
        }

        return builder.build( );
    }

    /**
     * Generates index name for a specific dataset
     *
     * @param datasetId
     *            the dataset identifier
     * @return the formatted index name
     */
    private String getIndexName( int datasetId )
    {
        return String.format( INDEX_NAME_FORMAT, datasetId );
    }

    /**
     * Creates an embedding store for the specified dataset Uses the modern Elasticsearch Java Client
     *
     * @param datasetId
     *            the dataset identifier
     * @return the configured embedding store
     */
    @SuppressWarnings( "removal" )
    public EmbeddingStore<TextSegment> createEmbeddingStore( int datasetId )
    {
        String indexName = getIndexName( datasetId );
        return ElasticsearchEmbeddingStore.builder( ).restClient( restClient ).indexName( indexName ).build( );
    }

    /**
     * Deletes all embeddings for a specific document from the dataset index Uses modern Elasticsearch Java Client API
     *
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     */
    public void deleteDocumentEmbeddings( int datasetId, int documentId )
    {
        String indexName = getIndexName( datasetId );

        try
        {
            Query query = TermQuery.of( t -> t.field( "metadata.document_id" ).value( String.valueOf( documentId ) ) )._toQuery( );

            DeleteByQueryRequest request = DeleteByQueryRequest.of( d -> d.index( indexName ).query( query ) );

            client.deleteByQuery( request );

        }
        catch( ElasticsearchException e )
        {
            if ( e.status( ) == 404 )
            {
                AppLogService.info( "Index {} does not exist, skipping delete for document {}", indexName, documentId );
            }
            else
            {
                String errorMsg = String.format( LOG_ERROR_EXCEPTION, String.format( OPERATION_DELETE_EMBEDDINGS, documentId ), e.getMessage( ) );
                AppLogService.error( errorMsg, e );
                throw new RuntimeException( EXCEPTION_OPERATION_FAILED, e );
            }
        }
        catch( Exception e )
        {
            String errorMsg = String.format( LOG_ERROR_EXCEPTION, String.format( OPERATION_DELETE_EMBEDDINGS, documentId ), e.getMessage( ) );
            AppLogService.error( errorMsg, e );
            throw new RuntimeException( EXCEPTION_OPERATION_FAILED, e );
        }
    }

    /**
     * Deletes the entire index for a dataset, removing all its embeddings Uses modern Elasticsearch Java Client API with existence check
     *
     * @param datasetId
     *            the dataset identifier
     */
    public void deleteDatasetEmbeddings( int datasetId )
    {
        String indexName = getIndexName( datasetId );

        try
        {
            if ( !indexExists( datasetId ) )
            {
                return;
            }

            DeleteIndexRequest request = DeleteIndexRequest.of( d -> d.index( indexName ) );
            DeleteIndexResponse response = client.indices( ).delete( request );

            if ( !response.acknowledged( ) )
            {
                AppLogService.error( "Delete index operation not acknowledged for %s".formatted( indexName ) );
            }

        }
        catch( Exception e )
        {
            String errorMsg = String.format( LOG_ERROR_EXCEPTION, OPERATION_DELETE_INDEX, e.getMessage( ) );
            AppLogService.error( errorMsg, e );
            throw new RuntimeException( EXCEPTION_OPERATION_FAILED, e );
        }
    }

    /**
     * Retrieves all segments/documents from Elasticsearch for a specific document
     *
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     * @return list of segments with their metadata
     */
    public List<Map<String, Object>> getDocumentSegments( int datasetId, int documentId )
    {
        List<Map<String, Object>> segments = new ArrayList<>( );
        String indexName = getIndexName( datasetId );

        try
        {
            Query query = TermQuery.of( t -> t.field( "metadata.document_id" ).value( String.valueOf( documentId ) ) )._toQuery( );

            SearchRequest searchRequest = SearchRequest.of( s -> s.index( indexName ).query( query ).sort(
                    sort -> sort.field( f -> f.field( "metadata.segment_index.keyword" ).order( co.elastic.clients.elasticsearch._types.SortOrder.Asc ) ) )
                    .size( 1000 ) );

            SearchResponse<ObjectNode> searchResponse = client.search( searchRequest, ObjectNode.class );
            segments.addAll( parseHits( searchResponse ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error retrieving segments for document %d from index %s: %s".formatted( documentId, indexName, e.getMessage( ) ), e );
        }

        return segments;
    }

    /**
     * Parses the hits of an Elasticsearch search response into the project's segment map shape: id, text, metadata.
     *
     * @param searchResponse
     *            The raw Elasticsearch response
     * @return List of segment maps (never null)
     */
    private List<Map<String, Object>> parseHits( SearchResponse<ObjectNode> searchResponse )
    {
        List<Map<String, Object>> parsed = new ArrayList<>( );
        for ( Hit<ObjectNode> hit : searchResponse.hits( ).hits( ) )
        {
            ObjectNode source = hit.source( );
            if ( source == null )
            {
                continue;
            }
            Map<String, Object> segment = new HashMap<>( );
            segment.put( "id", hit.id( ) );
            JsonNode textNode = source.get( "text" );
            if ( textNode != null )
            {
                segment.put( "text", textNode.asText( ) );
            }
            JsonNode metadataNode = source.get( "metadata" );
            Map<String, Object> metadataMap = new HashMap<>( );
            if ( metadataNode != null )
            {
                metadataNode.fields( ).forEachRemaining( entry -> metadataMap.put( entry.getKey( ), entry.getValue( ).asText( ) ) );
            }
            segment.put( "metadata", metadataMap );
            parsed.add( segment );
        }
        return parsed;
    }

    /**
     * Searches for an exact term across all documents of a dataset (grep-like). Optionally restricts to a set of folder IDs (typically the subtree resolved by
     * FolderPathResolver) and/or to a single document identifier.
     *
     * @param datasetId
     *            the dataset identifier
     * @param term
     *            the term or phrase to search for
     * @param folderIds
     *            optional folder ID filter (any match). null or empty for all folders
     * @param documentId
     *            optional single document identifier to restrict the search to one document. null for no restriction
     * @param maxResults
     *            maximum number of segments to return
     * @return list of matching segments with text and metadata
     */
    public List<Map<String, Object>> searchByTerm( int datasetId, String term, List<Integer> folderIds, Integer documentId, int maxResults )
    {
        List<Map<String, Object>> results = new ArrayList<>( );
        if ( term == null || term.isEmpty( ) )
        {
            return results;
        }
        String indexName = getIndexName( datasetId );

        try
        {
            co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder boolBuilder = new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder( );
            boolBuilder.must( m -> m.match( mt -> mt.field( "text" ).query( term ) ) );

            if ( folderIds != null && !folderIds.isEmpty( ) )
            {
                List<co.elastic.clients.elasticsearch._types.FieldValue> values = new ArrayList<>( );
                for ( Integer id : folderIds )
                {
                    values.add( co.elastic.clients.elasticsearch._types.FieldValue.of( String.valueOf( id ) ) );
                }
                boolBuilder
                        .filter( f -> f.terms( t -> t.field( "metadata." + EmbeddingService.META_FOLDER_ID + ".keyword" ).terms( tv -> tv.value( values ) ) ) );
            }

            if ( documentId != null )
            {
                boolBuilder.filter(
                        f -> f.term( t -> t.field( "metadata." + EmbeddingService.META_DOCUMENT_ID + ".keyword" ).value( String.valueOf( documentId ) ) ) );
            }

            int safeMax = maxResults > 0 ? maxResults : 20;
            SearchRequest searchRequest = SearchRequest
                    .of( s -> s.index( indexName ).query( Query.of( q -> q.bool( boolBuilder.build( ) ) ) ).size( safeMax ) );

            SearchResponse<ObjectNode> searchResponse = client.search( searchRequest, ObjectNode.class );
            results.addAll( parseHits( searchResponse ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error during grep search for term '%s' in dataset %d: %s".formatted( term, datasetId, e.getMessage( ) ), e );
        }

        return results;
    }

    /**
     * Counts segments grouped by document for every document in a dataset in a single Elasticsearch request. Avoids the N+1 pattern of calling
     * {@link #countDocumentSegments(int, int)} per document on list pages.
     *
     * @param datasetId
     *            the dataset identifier
     * @return a map keyed by document_id (as string) with the number of segments (never null)
     */
    public Map<String, Long> countSegmentsByDocument( int datasetId )
    {
        Map<String, Long> counts = new HashMap<>( );
        String indexName = getIndexName( datasetId );
        try
        {
            SearchRequest searchRequest = SearchRequest.of( s -> s.index( indexName ).size( 0 ).aggregations( "by_doc",
                    a -> a.terms( t -> t.field( "metadata." + EmbeddingService.META_DOCUMENT_ID + ".keyword" ).size( 10000 ) ) ) );
            SearchResponse<ObjectNode> response = client.search( searchRequest, ObjectNode.class );
            if ( response.aggregations( ) != null && response.aggregations( ).get( "by_doc" ) != null )
            {
                response.aggregations( ).get( "by_doc" ).sterms( ).buckets( ).array( )
                        .forEach( bucket -> counts.put( bucket.key( ).stringValue( ), bucket.docCount( ) ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error aggregating segment counts for dataset %d: %s".formatted( datasetId, e.getMessage( ) ), e );
        }
        return counts;
    }

    /**
     * Counts the number of segments for a specific document
     *
     * @param datasetId
     *            the dataset identifier
     * @param documentId
     *            the document identifier
     * @return number of segments found
     */
    public long countDocumentSegments( int datasetId, int documentId )
    {
        String indexName = getIndexName( datasetId );

        try
        {
            Query query = TermQuery.of( t -> t.field( "metadata.document_id" ).value( String.valueOf( documentId ) ) )._toQuery( );

            CountRequest countRequest = CountRequest.of( c -> c.index( indexName ).query( query ) );

            CountResponse countResponse = client.count( countRequest );
            return countResponse.count( );

        }
        catch( Exception e )
        {
            AppLogService.error( "Error counting segments for document %d: %s".formatted( documentId, e.getMessage( ) ), e );
            return 0;
        }
    }

    /**
     * Checks if an index exists for the specified dataset
     *
     * @param datasetId
     *            the dataset identifier
     * @return true if the index exists, false otherwise
     */
    public boolean indexExists( int datasetId )
    {
        String indexName = getIndexName( datasetId );

        try
        {
            ExistsRequest existsRequest = ExistsRequest.of( e -> e.index( indexName ) );
            return client.indices( ).exists( existsRequest ).value( );

        }
        catch( Exception e )
        {
            AppLogService.error( "Index %s does not exist or error checking: %s".formatted( indexName, e.getMessage( ) ) );
            return false;
        }
    }

    /**
     * Closes Elasticsearch transport and REST client on CDI shutdown.
     */
    @PreDestroy
    void shutdown( )
    {
        try
        {
            if ( client != null )
            {
                client.shutdown( );
            }
            if ( transport != null )
            {
                transport.close( );
            }
            if ( restClient != null )
            {
                restClient.close( );
            }
            AppLogService.info( LOG_CLIENT_CLOSED );
        }
        catch( Exception e )
        {
            AppLogService.error( LOG_ERROR_CLOSING.formatted( e.getMessage( ) ), e );
        }
    }
}
