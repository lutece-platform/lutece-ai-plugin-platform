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
package fr.paris.lutece.plugins.platform.service.rag.router;

import static java.util.Arrays.stream;
import java.util.Collection;
import static java.util.Collections.emptyList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.service.event.domain.DatasetsRoutedEvent;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Router for dataset queries that extends LanguageModelQueryRouter. Analyzes user queries to determine which datasets are needed for responses.
 */
public class DatasetQueryRouter extends LanguageModelQueryRouter
{

    private static final String PROMPT_ANALYZE_QUERY = "Analyse la requête utilisateur et détermine quelles sources de données sont nécessaires pour y répondre.\n";
    private static final String PROMPT_AVAILABLE_SOURCES = "Sources de données disponibles :\n{{options}}\n\n";
    private static final String PROMPT_INSTRUCTIONS = "Instructions :\n"
            + "- Si la requête est une simple formule de politesse (bonjour, merci, au revoir, etc.) ou ne nécessite pas de données spécifiques, réponds \"NONE\"\n"
            + "- Si aucune source de donnée ne semble convenir à la demande, réponds \"NONE\"\n"
            + "- Si la requête nécessite des informations spécialisées, sélectionne les sources pertinentes par leur numéro\n"
            + "- Tu peux sélectionner plusieurs sources en les séparant par des virgules\n"
            + "- Réponds UNIQUEMENT avec \"NONE\" ou les numéros des sources (ex: \"1\" ou \"1,3\" ou \"NONE\")\n\n";
    private static final String PROMPT_USER_QUERY = "Requête utilisateur: {{query}}";

    private static final PromptTemplate CUSTOM_PROMPT_TEMPLATE = PromptTemplate
            .from( PROMPT_ANALYZE_QUERY + PROMPT_AVAILABLE_SOURCES + PROMPT_INSTRUCTIONS + PROMPT_USER_QUERY );

    private static final String DATASET_ID_KEY = "id";
    private static final String DATASET_NAME_KEY = "name";
    private static final String DATASET_DESCRIPTION_KEY = "description";
    private static final String DATASET_ROUTING_RULES_KEY = "routing_rules";
    private static final String RESPONSE_NONE = "NONE";
    private static final String COMMA_SEPARATOR = ",";

    private static final String ERROR_MESSAGE_DISPATCH_FAILED = "Failed to dispatch dataset routing event for operationId: ";
    private static final String ERROR_MESSAGE_ROUTE_FAILED = "Failed to route query '{}': {}";
    private static final String ERROR_MESSAGE_PARSE_FAILED = "Failed to parse router response: '{}'. Falling back to no datasets.";

    private final int botId;
    private final String executionId;
    private final String operationId;
    private final Map<ContentRetriever, Dataset> retrieverToDataset;

    /**
     * Constructs a DatasetQueryRouter with specified parameters.
     *
     * @param chatLanguageModel
     *            the chat language model for routing decisions
     * @param retrieverToDescription
     *            mapping of content retrievers to their descriptions
     * @param availableDatasets
     *            list of available datasets
     * @param botId
     *            the bot identifier
     * @param userId
     *            the user identifier
     * @param executionId
     *            the execution identifier
     * @param operationId
     *            the operation identifier
     * @param retrieverToDataset
     *            mapping of content retrievers to datasets
     */
    public DatasetQueryRouter( ChatModel chatLanguageModel, Map<ContentRetriever, String> retrieverToDescription, List<Dataset> availableDatasets, int botId,
            String userId, String executionId, String operationId, Map<ContentRetriever, Dataset> retrieverToDataset )
    {
        super( chatLanguageModel, retrieverToDescription, CUSTOM_PROMPT_TEMPLATE, FallbackStrategy.DO_NOT_ROUTE );
        this.botId = botId;
        this.executionId = executionId;
        this.operationId = operationId;
        this.retrieverToDataset = retrieverToDataset;
    }

    /**
     * Gets the execution identifier of the current routing operation
     *
     * @return the execution identifier
     */
    public String getExecutionId( )
    {
        return executionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ContentRetriever> route( Query query )
    {
        Prompt prompt = createPrompt( query );
        try
        {
            String response = generateResponse( prompt );
            Collection<ContentRetriever> selectedRetrievers = parseCustomResponse( response );
            List<Dataset> selectedDatasets = getSelectedDatasets( selectedRetrievers );
            String queryText = query.text( );
            dispatchDatasetRoutingEvent( queryText, selectedDatasets );
            return selectedRetrievers;
        }
        catch( Exception e )
        {
            AppLogService.error( ERROR_MESSAGE_ROUTE_FAILED, query.text( ), e.getMessage( ), e );
            return fallback( query, e );
        }
    }

    /**
     * Generates response from the chat language model.
     *
     * @param prompt
     *            the prompt to send to the model
     * @return the trimmed response from the model
     */
    private String generateResponse( Prompt prompt )
    {
        return chatModel.chat( prompt.toUserMessage( ) ).aiMessage( ).text( ).trim( );
    }

    /**
     * Parses the custom response from the language model to extract content retrievers.
     *
     * @param response
     *            the response string from the language model
     * @return collection of selected content retrievers
     */
    protected Collection<ContentRetriever> parseCustomResponse( String response )
    {
        if ( RESPONSE_NONE.equalsIgnoreCase( response.trim( ) ) )
        {
            return emptyList( );
        }

        return parseNumericResponse( response );
    }

    /**
     * Parses numeric response to extract content retrievers.
     *
     * @param response
     *            the numeric response string
     * @return collection of content retrievers
     */
    private Collection<ContentRetriever> parseNumericResponse( String response )
    {
        try
        {
            return stream( response.split( COMMA_SEPARATOR ) ).map( String::trim ).filter( s -> !s.isEmpty( ) ).map( Integer::parseInt )
                    .map( idToRetriever::get ).filter( Objects::nonNull ).toList( );
        }
        catch( NumberFormatException e )
        {
            AppLogService.error( ERROR_MESSAGE_PARSE_FAILED, response );
            return emptyList( );
        }
    }

    /**
     * Gets selected datasets from content retrievers.
     *
     * @param selectedRetrievers
     *            collection of selected content retrievers
     * @return list of corresponding datasets
     */
    private List<Dataset> getSelectedDatasets( Collection<ContentRetriever> selectedRetrievers )
    {
        return selectedRetrievers.stream( ).map( retrieverToDataset::get ).filter( Objects::nonNull ).toList( );
    }

    /**
     * Dispatches dataset routing event with query and selected datasets information.
     *
     * @param query
     *            the user query
     * @param selectedDatasets
     *            list of selected datasets
     */
    private void dispatchDatasetRoutingEvent( String query, List<Dataset> selectedDatasets )
    {
        try
        {
            List<Object> datasetsInfo = buildDatasetsInfo( selectedDatasets );
            CDI.current( ).getBeanManager( ).getEvent( ).fire( DatasetsRoutedEvent.now( operationId, botId, datasetsInfo ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "{}{}", ERROR_MESSAGE_DISPATCH_FAILED, operationId, e );
        }
    }

    /**
     * Builds datasets information for event payload.
     *
     * @param selectedDatasets
     *            list of selected datasets
     * @return list of dataset information maps
     */
    private List<Object> buildDatasetsInfo( List<Dataset> selectedDatasets )
    {
        return selectedDatasets.stream( ).map( this::createDatasetInfo ).toList( );
    }

    /**
     * Creates dataset information map.
     *
     * @param dataset
     *            the dataset to create info for
     * @return map containing dataset information
     */
    private Object createDatasetInfo( Dataset dataset )
    {
        Map<String, Object> datasetInfo = new HashMap<>( );
        datasetInfo.put( DATASET_ID_KEY, dataset.getId( ) );
        datasetInfo.put( DATASET_NAME_KEY, dataset.getDatasetName( ) );
        datasetInfo.put( DATASET_DESCRIPTION_KEY, dataset.getDatasetDescription( ) );
        datasetInfo.put( DATASET_ROUTING_RULES_KEY, dataset.getDatasetRoutingRules( ) );
        return datasetInfo;
    }
}
