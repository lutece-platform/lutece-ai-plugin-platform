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

import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;

import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetHome;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import fr.paris.lutece.plugins.platform.service.rag.QueryEnhancementMode;
import fr.paris.lutece.plugins.platform.service.rag.transformer.HydeQueryTransformer;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Pipeline node that performs semantic search across configured datasets. This node retrieves relevant content segments from Elasticsearch based on embedding
 * similarity.
 */
@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_SEMANTIC_SEARCH, name = "Recherche sémantique", description = "Effectue une recherche sémantique dans les datasets configurés" )
public class SemanticSearchNode extends AbstractPipelineNode
{
    private final ModelService _modelService = CDI.current( ).select( ModelService.class ).get( );
    private final ElasticsearchService _elasticsearch = CDI.current( ).select( ElasticsearchService.class ).get( );

    private static final String DATASETS_NAME = "datasets";
    private static final String QUERY_NAME = "query";
    private static final String MAX_RESULTS_NAME = "max_results";
    private static final String MIN_SCORE_NAME = "min_score";
    private static final String QUERY_ENHANCEMENT_MODE_NAME = "query_enhancement_mode";
    private static final String EXPANSION_VARIANTS_NAME = "expansion_variants";
    private static final String HYDE_PROVIDER_NAME = "hyde_provider";
    private static final String ENABLE_AGGREGATION_NAME = "enable_aggregation";

    private static final String DATASETS_TITLE = "Datasets";
    private static final String DATASETS_DESCRIPTION = "Sélectionnez les datasets dans lesquels effectuer la recherche";
    private static final String QUERY_TITLE = "Requête";
    private static final String QUERY_DESCRIPTION = "La requête de recherche sémantique";
    private static final String QUERY_PLACEHOLDER = "Entrez votre requête de recherche";
    private static final String MAX_RESULTS_TITLE = "Nombre maximum de résultats";
    private static final String MAX_RESULTS_DESCRIPTION = "Nombre maximum de segments à retourner";
    private static final String MIN_SCORE_TITLE = "Score minimum";
    private static final String MIN_SCORE_DESCRIPTION = "Score de similarité minimum (entre 0 et 1)";
    private static final String QUERY_ENHANCEMENT_MODE_TITLE = "Mode d'amélioration";
    private static final String QUERY_ENHANCEMENT_MODE_DESCRIPTION = "Mode d'amélioration de la requête pour de meilleurs résultats";
    private static final String EXPANSION_VARIANTS_TITLE = "Variantes d'expansion";
    private static final String EXPANSION_VARIANTS_DESCRIPTION = "Nombre de variantes de requête à générer (si expansion activée)";
    private static final String HYDE_PROVIDER_TITLE = "Fournisseur HyDE";
    private static final String HYDE_PROVIDER_DESCRIPTION = "Fournisseur LLM pour générer le document hypothétique (si HyDE activé)";
    private static final String ENABLE_AGGREGATION_TITLE = "Fusion pondérée";
    private static final String ENABLE_AGGREGATION_DESCRIPTION = "Activer la fusion pondérée des résultats de plusieurs datasets";

    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String DEFAULT_OUTPUT_PORT_DESCRIPTION = "Sortie par défaut";

    private static final String RETRIEVED_CONTENTS_KEY = "retrieved_contents";
    private static final String SOURCE_COUNT_KEY = "source_count";
    private static final String RETRIEVED_CONTENTS_DESCRIPTION = "Liste des contenus récupérés avec métadonnées";
    private static final String SOURCE_COUNT_DESCRIPTION = "Nombre de sources trouvées";

    private static final String ERROR_NO_DATASETS = "No dataset selected";
    private static final String ERROR_QUERY_EMPTY = "The query cannot be empty";
    private static final String ERROR_DATASET_NOT_FOUND = "Dataset not found: %s";
    private static final String ERROR_EMBEDDING_PROVIDER_NOT_FOUND = "Embedding provider not found for dataset: %s";
    private static final String ERROR_RAG_SEARCH = "Error during RAG search for node %s";

    private static final String DEFAULT_DATASET_OPTION_VALUE = "";
    private static final String DEFAULT_DATASET_OPTION_LABEL = "Sélectionner un dataset";
    private static final String DEFAULT_DATASET_OPTION_DESC = "Veuillez sélectionner au moins un dataset";

    private static final int DEFAULT_MAX_RESULTS = 15;
    private static final double DEFAULT_MIN_SCORE = 0.7;
    private static final int DEFAULT_EXPANSION_VARIANTS = 3;
    private static final String DEFAULT_PROVIDER_OPTION_VALUE = "";
    private static final String DEFAULT_PROVIDER_OPTION_LABEL = "Sélectionner un fournisseur";
    private static final String DEFAULT_PROVIDER_OPTION_DESC = "Requis si HyDE activé";

    public static final PipelineVariable QUERY = new PipelineVariable.Builder( QUERY_NAME ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( QUERY_TITLE ).description( QUERY_DESCRIPTION ).minLength( 1 ).textarea( true ).rows( 3 ).placeholder( QUERY_PLACEHOLDER ).build( );

    public static final PipelineVariable MAX_RESULTS = new PipelineVariable.Builder( MAX_RESULTS_NAME ).type( PipelineVariable.VariableType.NUMBER )
            .required( false ).title( MAX_RESULTS_TITLE ).description( MAX_RESULTS_DESCRIPTION ).defaultValue( DEFAULT_MAX_RESULTS ).min( 1.0 ).max( 100.0 )
            .build( );

    public static final PipelineVariable MIN_SCORE = new PipelineVariable.Builder( MIN_SCORE_NAME ).type( PipelineVariable.VariableType.NUMBER )
            .required( false ).title( MIN_SCORE_TITLE ).description( MIN_SCORE_DESCRIPTION ).defaultValue( DEFAULT_MIN_SCORE ).min( 0.0 ).max( 1.0 ).build( );

    public static final PipelineVariable EXPANSION_VARIANTS = new PipelineVariable.Builder( EXPANSION_VARIANTS_NAME )
            .type( PipelineVariable.VariableType.NUMBER ).required( false ).title( EXPANSION_VARIANTS_TITLE ).description( EXPANSION_VARIANTS_DESCRIPTION )
            .defaultValue( DEFAULT_EXPANSION_VARIANTS ).min( 1.0 ).max( 10.0 ).build( );

    public static final PipelineVariable ENABLE_AGGREGATION = new PipelineVariable.Builder( ENABLE_AGGREGATION_NAME )
            .type( PipelineVariable.VariableType.BOOLEAN ).required( false ).title( ENABLE_AGGREGATION_TITLE ).description( ENABLE_AGGREGATION_DESCRIPTION )
            .defaultValue( false ).build( );

    /**
     * Constructor for SemanticSearchNode.
     */
    public SemanticSearchNode( )
    {
        super( PipelineConstants.NODE_TYPE_SEMANTIC_SEARCH );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( createDatasetsVariable( ) );
        variables.add( QUERY );
        variables.add( MAX_RESULTS );
        variables.add( MIN_SCORE );
        variables.add( createQueryEnhancementModeVariable( ) );
        variables.add( EXPANSION_VARIANTS );
        variables.add( createHydeProviderVariable( ) );
        variables.add( ENABLE_AGGREGATION );
        return variables;
    }

    /**
     * Creates the datasets variable with options loaded from database.
     *
     * @return the datasets pipeline variable
     */
    private PipelineVariable createDatasetsVariable( )
    {
        List<PipelineVariable.EnumOption> datasetOptions = loadDatasetOptions( );

        return new PipelineVariable.Builder( DATASETS_NAME ).type( PipelineVariable.VariableType.ARRAY ).required( true ).title( DATASETS_TITLE )
                .description( DATASETS_DESCRIPTION ).itemSchema( new PipelineVariable.Builder( "dataset_id" ).type( PipelineVariable.VariableType.ENUM )
                        .options( datasetOptions.toArray( new PipelineVariable.EnumOption [ 0] ) ).build( ) )
                .build( );
    }

    /**
     * Loads dataset options from database for the enum selection.
     *
     * @return list of dataset enum options
     */
    private List<PipelineVariable.EnumOption> loadDatasetOptions( )
    {
        List<PipelineVariable.EnumOption> options = new ArrayList<>( );

        try
        {
            List<Dataset> datasets = DatasetHome.getDatasetsList( );

            if ( datasets.isEmpty( ) )
            {
                options.add( new PipelineVariable.EnumOption( DEFAULT_DATASET_OPTION_VALUE, DEFAULT_DATASET_OPTION_LABEL, DEFAULT_DATASET_OPTION_DESC ) );
            }
            else
            {
                for ( Dataset dataset : datasets )
                {
                    String value = String.valueOf( dataset.getId( ) );
                    String label = dataset.getDatasetName( );
                    String description = dataset.getDatasetDescription( ) != null ? dataset.getDatasetDescription( ) : "";
                    options.add( new PipelineVariable.EnumOption( value, label, description ) );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.info( "Unable to load datasets: {}", e.getMessage( ) );
            options.add( new PipelineVariable.EnumOption( DEFAULT_DATASET_OPTION_VALUE, DEFAULT_DATASET_OPTION_LABEL, DEFAULT_DATASET_OPTION_DESC ) );
        }

        return options;
    }

    /**
     * Creates the query enhancement mode variable with available modes.
     *
     * @return the query enhancement mode pipeline variable
     */
    private PipelineVariable createQueryEnhancementModeVariable( )
    {
        List<PipelineVariable.EnumOption> options = new ArrayList<>( );
        options.add( new PipelineVariable.EnumOption( QueryEnhancementMode.NONE.getCode( ), "Aucun", "Recherche simple sans amélioration" ) );
        options.add(
                new PipelineVariable.EnumOption( QueryEnhancementMode.EXPANSION_ONLY.getCode( ), "Expansion", "Génère plusieurs variantes de la requête" ) );
        options.add( new PipelineVariable.EnumOption( QueryEnhancementMode.HYDE_ONLY.getCode( ), "HyDE", "Génère un document hypothétique" ) );
        options.add( new PipelineVariable.EnumOption( QueryEnhancementMode.EXPANSION_THEN_HYDE.getCode( ), "Expansion + HyDE",
                "Expansion puis HyDE sur chaque variante" ) );

        return new PipelineVariable.Builder( QUERY_ENHANCEMENT_MODE_NAME ).type( PipelineVariable.VariableType.ENUM ).required( false )
                .title( QUERY_ENHANCEMENT_MODE_TITLE ).description( QUERY_ENHANCEMENT_MODE_DESCRIPTION )
                .options( options.toArray( new PipelineVariable.EnumOption [ 0] ) ).defaultValue( QueryEnhancementMode.NONE.getCode( ) ).build( );
    }

    /**
     * Creates the HyDE provider variable with available LLM providers.
     *
     * @return the HyDE provider pipeline variable
     */
    private PipelineVariable createHydeProviderVariable( )
    {
        List<PipelineVariable.EnumOption> options = new ArrayList<>( );
        options.add( new PipelineVariable.EnumOption( DEFAULT_PROVIDER_OPTION_VALUE, DEFAULT_PROVIDER_OPTION_LABEL, DEFAULT_PROVIDER_OPTION_DESC ) );

        try
        {
            List<Provider> providers = ProviderHome.getProvidersList( );
            for ( Provider provider : providers )
            {
                String value = String.valueOf( provider.getId( ) );
                String label = provider.getProviderName( ) + " (" + provider.getDeploymentModelName( ) + ")";
                String description = provider.getProviderDescription( ) != null ? provider.getProviderDescription( ) : "";
                options.add( new PipelineVariable.EnumOption( value, label, description ) );
            }
        }
        catch( Exception e )
        {
            AppLogService.info( "Unable to load providers: {}", e.getMessage( ) );
        }

        return new PipelineVariable.Builder( HYDE_PROVIDER_NAME ).type( PipelineVariable.VariableType.ENUM ).required( false ).title( HYDE_PROVIDER_TITLE )
                .description( HYDE_PROVIDER_DESCRIPTION ).options( options.toArray( new PipelineVariable.EnumOption [ 0] ) )
                .defaultValue( DEFAULT_PROVIDER_OPTION_VALUE ).build( );
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
        List<String> datasetIds = extractDatasetIds( resolvedData );
        String query = getParam( resolvedData, QUERY );
        int maxResults = getParamAsInteger( resolvedData, MAX_RESULTS );
        double minScore = getParamAsDouble( resolvedData, MIN_SCORE );
        String enhancementModeCode = getParamWithDefault( resolvedData, QUERY_ENHANCEMENT_MODE_NAME, QueryEnhancementMode.NONE.getCode( ) );
        int expansionVariants = getParamAsInteger( resolvedData, EXPANSION_VARIANTS );
        String hydeProviderIdStr = getParamWithDefault( resolvedData, HYDE_PROVIDER_NAME, DEFAULT_PROVIDER_OPTION_VALUE );
        boolean enableAggregation = getParamAsBoolean( resolvedData, ENABLE_AGGREGATION );

        validateParameters( datasetIds, query );

        if ( maxResults <= 0 )
        {
            maxResults = DEFAULT_MAX_RESULTS;
        }
        if ( minScore <= 0 )
        {
            minScore = DEFAULT_MIN_SCORE;
        }
        if ( expansionVariants <= 0 )
        {
            expansionVariants = DEFAULT_EXPANSION_VARIANTS;
        }

        QueryEnhancementMode enhancementMode = QueryEnhancementMode.fromCode( enhancementModeCode );
        Provider hydeProvider = loadHydeProvider( hydeProviderIdStr, enhancementMode );

        List<String> queries = enhanceQuery( query, enhancementMode, expansionVariants, hydeProvider, context );
        List<Map<String, Object>> retrievedContents = performSearchWithQueries( datasetIds, queries, maxResults, minScore, enableAggregation, config.getId( ) );

        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( RETRIEVED_CONTENTS_KEY, retrievedContents );
        outputData.put( SOURCE_COUNT_KEY, retrievedContents.size( ) );

        prepareOutputData( context, config, outputData );

        return context;
    }

    /**
     * Loads the HyDE provider if HyDE mode is enabled.
     *
     * @param hydeProviderIdStr
     *            the provider ID as string
     * @param mode
     *            the enhancement mode
     * @return the provider or null if not needed/found
     */
    private Provider loadHydeProvider( String hydeProviderIdStr, QueryEnhancementMode mode )
    {
        if ( !mode.includesHyde( ) || hydeProviderIdStr == null || hydeProviderIdStr.isEmpty( ) )
        {
            return null;
        }

        try
        {
            int providerId = Integer.parseInt( hydeProviderIdStr );
            return ProviderHome.findByPrimaryKey( providerId ).orElse( null );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }

    /**
     * Enhances the query based on the enhancement mode.
     *
     * @param originalQuery
     *            the original query
     * @param mode
     *            the enhancement mode
     * @param expansionVariants
     *            number of variants to generate
     * @param hydeProvider
     *            the provider for HyDE
     * @param context
     *            the pipeline context
     * @return list of queries to execute
     */
    private List<String> enhanceQuery( String originalQuery, QueryEnhancementMode mode, int expansionVariants, Provider hydeProvider, PipelineContext context )
    {
        List<String> queries = new ArrayList<>( );

        if ( mode == QueryEnhancementMode.NONE )
        {
            queries.add( originalQuery );
            return queries;
        }

        if ( mode.includesExpansion( ) && hydeProvider != null )
        {
            queries.addAll( expandQuery( originalQuery, expansionVariants, hydeProvider, context ) );
        }
        else if ( mode.includesExpansion( ) )
        {
            queries.add( originalQuery );
        }
        else
        {
            queries.add( originalQuery );
        }

        if ( mode.includesHyde( ) && hydeProvider != null )
        {
            if ( mode.isHydePerVariant( ) )
            {
                List<String> hydeQueries = new ArrayList<>( );
                for ( String q : queries )
                {
                    String hydeDoc = generateHydeDocument( q, hydeProvider, context );
                    if ( hydeDoc != null && !hydeDoc.isEmpty( ) )
                    {
                        hydeQueries.add( hydeDoc );
                    }
                }
                queries.addAll( hydeQueries );
            }
            else
            {
                String hydeDoc = generateHydeDocument( originalQuery, hydeProvider, context );
                if ( hydeDoc != null && !hydeDoc.isEmpty( ) )
                {
                    queries.add( hydeDoc );
                }
            }
        }

        return queries.isEmpty( ) ? List.of( originalQuery ) : queries;
    }

    /**
     * Expands the query into multiple variants using a LLM.
     *
     * @param query
     *            the original query
     * @param variants
     *            number of variants to generate
     * @param provider
     *            the LLM provider
     * @param context
     *            the pipeline context
     * @return list of expanded queries
     */
    private List<String> expandQuery( String query, int variants, Provider provider, PipelineContext context )
    {
        List<String> expandedQueries = new ArrayList<>( );
        expandedQueries.add( query );

        try
        {
            ChatModel chatModel = _modelService.createTrackedChatModel( provider, context.getObservabilityId( ), "query-expander",
                    context.getAllOutputs( ).size( ) + 1 );

            ExpandingQueryTransformer expander = ExpandingQueryTransformer.builder( ).chatModel( chatModel ).n( variants ).build( );

            Collection<Query> expanded = expander.transform( Query.from( query ) );
            for ( Query q : expanded )
            {
                if ( !q.text( ).equals( query ) )
                {
                    expandedQueries.add( q.text( ) );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error during query expansion: {}", e.getMessage( ), e );
        }

        return expandedQueries;
    }

    /**
     * Generates a hypothetical document using HyDE technique.
     *
     * @param query
     *            the query to generate document for
     * @param provider
     *            the LLM provider
     * @param context
     *            the pipeline context
     * @return the generated hypothetical document
     */
    private String generateHydeDocument( String query, Provider provider, PipelineContext context )
    {
        try
        {
            ChatModel chatModel = _modelService.createTrackedChatModel( provider, context.getObservabilityId( ), "hyde-generator",
                    context.getAllOutputs( ).size( ) + 1 );

            HydeQueryTransformer hyde = HydeQueryTransformer.builder( ).chatModel( chatModel ).build( );

            Collection<Query> transformed = hyde.transform( Query.from( query ) );
            for ( Query q : transformed )
            {
                return q.text( );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error during HyDE generation: {}", e.getMessage( ), e );
        }

        return null;
    }

    /**
     * Performs search with multiple queries.
     *
     * @param datasetIds
     *            the dataset IDs
     * @param queries
     *            the queries to execute
     * @param maxResults
     *            maximum results per query
     * @param minScore
     *            minimum score
     * @param enableAggregation
     *            whether to enable reciprocal rank fusion aggregation
     * @param nodeId
     *            the node ID
     * @return aggregated search results
     */
    private List<Map<String, Object>> performSearchWithQueries( List<String> datasetIds, List<String> queries, int maxResults, double minScore,
            boolean enableAggregation, String nodeId )
    {
        List<Map<String, Object>> allResults = new ArrayList<>( );

        for ( String query : queries )
        {
            List<Map<String, Object>> queryResults = performSearch( datasetIds, query, maxResults, minScore, nodeId );
            allResults.addAll( queryResults );
        }

        if ( enableAggregation && queries.size( ) > 1 )
        {
            return aggregateResults( allResults, maxResults );
        }

        allResults.sort( ( a, b ) -> Double.compare( (Double) b.get( "score" ), (Double) a.get( "score" ) ) );
        if ( allResults.size( ) > maxResults )
        {
            return allResults.subList( 0, maxResults );
        }
        return allResults;
    }

    /**
     * Aggregates results from multiple queries using reciprocal rank fusion.
     *
     * @param allResults
     *            all results to aggregate
     * @param maxResults
     *            maximum results to return
     * @return aggregated results
     */
    private List<Map<String, Object>> aggregateResults( List<Map<String, Object>> allResults, int maxResults )
    {
        Map<String, Map<String, Object>> uniqueResults = new LinkedHashMap<>( );
        Map<String, Double> scores = new HashMap<>( );

        int rank = 1;
        for ( Map<String, Object> result : allResults )
        {
            String text = (String) result.get( "text" );
            double rrfScore = 1.0 / ( 60 + rank );

            if ( uniqueResults.containsKey( text ) )
            {
                scores.put( text, scores.get( text ) + rrfScore );
            }
            else
            {
                uniqueResults.put( text, result );
                scores.put( text, rrfScore );
            }
            rank++;
        }

        for ( Map.Entry<String, Map<String, Object>> entry : uniqueResults.entrySet( ) )
        {
            entry.getValue( ).put( "score", scores.get( entry.getKey( ) ) );
        }

        List<Map<String, Object>> aggregated = new ArrayList<>( uniqueResults.values( ) );
        aggregated.sort( ( a, b ) -> Double.compare( (Double) b.get( "score" ), (Double) a.get( "score" ) ) );

        if ( aggregated.size( ) > maxResults )
        {
            return aggregated.subList( 0, maxResults );
        }
        return aggregated;
    }

    /**
     * Extracts dataset IDs from the resolved data.
     *
     * @param resolvedData
     *            the resolved node data
     * @return list of dataset IDs as strings
     */
    private List<String> extractDatasetIds( Map<String, Object> resolvedData )
    {
        List<String> datasetIds = new ArrayList<>( );
        Object datasetsObj = resolvedData.get( DATASETS_NAME );

        if ( datasetsObj instanceof List<?> list )
        {
            for ( Object item : list )
            {
                switch( item )
                {
                    case String s -> datasetIds.add( s );
                    case Map<?, ?> map -> {
                        Object idValue = map.get( "dataset_id" );
                        if ( idValue != null )
                        {
                            datasetIds.add( idValue.toString( ) );
                        }
                    }
                    case null -> {
                    }
                    default -> datasetIds.add( item.toString( ) );
                }
            }
        }
        else if ( datasetsObj instanceof String datasetsStr && !datasetsStr.isEmpty( ) )
        {
            for ( String id : datasetsStr.split( "," ) )
            {
                String trimmed = id.trim( );
                if ( !trimmed.isEmpty( ) )
                {
                    datasetIds.add( trimmed );
                }
            }
        }

        return datasetIds;
    }

    /**
     * Validates the search parameters.
     *
     * @param datasetIds
     *            the list of dataset IDs
     * @param query
     *            the search query
     * @throws RuntimeException
     *             if validation fails
     */
    private void validateParameters( List<String> datasetIds, String query )
    {
        if ( datasetIds.isEmpty( ) )
        {
            AppLogService.error( ERROR_NO_DATASETS );
            throw new RuntimeException( ERROR_NO_DATASETS );
        }

        if ( query == null || query.trim( ).isEmpty( ) )
        {
            AppLogService.error( ERROR_QUERY_EMPTY );
            throw new RuntimeException( ERROR_QUERY_EMPTY );
        }
    }

    /**
     * Performs semantic search across all specified datasets.
     *
     * @param datasetIds
     *            the list of dataset IDs to search
     * @param query
     *            the search query
     * @param maxResults
     *            maximum number of results to return
     * @param minScore
     *            minimum similarity score threshold
     * @param nodeId
     *            the node ID for logging
     * @return list of search results with text, metadata, and score
     */
    private List<Map<String, Object>> performSearch( List<String> datasetIds, String query, int maxResults, double minScore, String nodeId )
    {
        List<Map<String, Object>> allResults = new ArrayList<>( );

        for ( String datasetIdStr : datasetIds )
        {
            try
            {
                int datasetId = Integer.parseInt( datasetIdStr );
                Optional<Dataset> optDataset = DatasetHome.findByPrimaryKey( datasetId );

                if ( !optDataset.isPresent( ) )
                {
                    AppLogService.error( String.format( ERROR_DATASET_NOT_FOUND, datasetIdStr ) );
                    continue;
                }

                Dataset dataset = optDataset.get( );
                List<Map<String, Object>> datasetResults = searchInDataset( dataset, query, maxResults, minScore );
                allResults.addAll( datasetResults );
            }
            catch( NumberFormatException e )
            {
                AppLogService.error( String.format( ERROR_DATASET_NOT_FOUND, datasetIdStr ) );
            }
            catch( Exception e )
            {
                AppLogService.error( String.format( ERROR_RAG_SEARCH, nodeId ), e );
            }
        }

        allResults.sort( ( a, b ) -> Double.compare( (Double) b.get( "score" ), (Double) a.get( "score" ) ) );

        if ( allResults.size( ) > maxResults )
        {
            return allResults.subList( 0, maxResults );
        }

        return allResults;
    }

    /**
     * Searches for relevant content within a single dataset.
     *
     * @param dataset
     *            the dataset to search in
     * @param query
     *            the search query
     * @param maxResults
     *            maximum number of results
     * @param minScore
     *            minimum similarity score
     * @return list of search results from this dataset
     * @throws Exception
     *             if embedding provider not found or search fails
     */
    private List<Map<String, Object>> searchInDataset( Dataset dataset, String query, int maxResults, double minScore ) throws Exception
    {
        Provider embedProvider = ProviderHome.findByPrimaryKey( dataset.getEmbedProviderId( ) )
                .orElseThrow( ( ) -> new Exception( String.format( ERROR_EMBEDDING_PROVIDER_NOT_FOUND, dataset.getDatasetName( ) ) ) );
        EmbeddingModel embeddingModel = _modelService.createEmbeddingModel( embedProvider );
        EmbeddingStore<TextSegment> embeddingStore = _elasticsearch.createEmbeddingStore( dataset.getId( ) );

        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder( ).embeddingStore( embeddingStore ).embeddingModel( embeddingModel )
                .maxResults( maxResults ).minScore( minScore ).build( );

        List<Content> contents = retriever.retrieve( Query.from( query ) );

        return toResults( contents, dataset );
    }

    /**
     * Maps retrieved contents to result maps, carrying the real similarity score computed by the embedding store ({@link ContentMetadata#SCORE}) so that
     * downstream thresholds and ranking operate on actual relevance. Package-private for unit testing.
     *
     * @param contents
     *            the contents returned by the retriever
     * @param dataset
     *            the dataset they were retrieved from
     * @return the result maps (text, metadata, score)
     */
    static List<Map<String, Object>> toResults( List<Content> contents, Dataset dataset )
    {
        List<Map<String, Object>> results = new ArrayList<>( );
        for ( Content content : contents )
        {
            Map<String, Object> result = new HashMap<>( );
            result.put( "text", content.textSegment( ).text( ) );

            Map<String, Object> metadata = new HashMap<>( );
            content.textSegment( ).metadata( ).toMap( ).forEach( ( key, value ) -> metadata.put( key, value ) );
            metadata.put( EmbeddingService.META_DATASET_ID, dataset.getId( ) );
            metadata.put( EmbeddingService.META_DATASET_NAME, dataset.getDatasetName( ) );
            result.put( "metadata", metadata );

            Object score = content.metadata( ).get( ContentMetadata.SCORE );
            result.put( "score", score instanceof Number number ? number.doubleValue( ) : 0.0 );

            results.add( result );
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( RETRIEVED_CONTENTS_KEY, RETRIEVED_CONTENTS_DESCRIPTION );
        outputs.put( SOURCE_COUNT_KEY, SOURCE_COUNT_DESCRIPTION );
        return outputs;
    }
}
