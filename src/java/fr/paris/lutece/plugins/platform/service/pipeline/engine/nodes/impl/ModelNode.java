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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

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
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_MODEL, name = "Modèle", description = "Génère du contenu en utilisant un modèle de langage via un fournisseur configuré" )
public class ModelNode extends AbstractPipelineNode
{

    private static final String PROVIDER_NAME = "provider";
    private static final String PROMPT_NAME = "prompt";
    private static final String RESPONSE_FORMAT_NAME = "response_format";
    private static final String JSON_SCHEMA_NAME = "json_schema";
    private static final String PROVIDER_TITLE = "Fournisseur";
    private static final String PROVIDER_DESCRIPTION = "Sélectionnez le fournisseur de modèle à utiliser";
    private static final String PROMPT_TITLE = "Prompt";
    private static final String PROMPT_DESCRIPTION = "Le prompt à envoyer au modèle de langage";
    private static final String PROMPT_PLACEHOLDER = "Expliquez-moi ce concept en termes simples";
    private static final String RESPONSE_FORMAT_TITLE = "Format de réponse";
    private static final String RESPONSE_FORMAT_DESCRIPTION = "Format attendu pour la réponse du modèle";
    private static final String RESPONSE_FORMAT_TEXT = "text";
    private static final String RESPONSE_FORMAT_JSON = "json";
    private static final String JSON_SCHEMA_TITLE = "Schéma JSON";
    private static final String JSON_SCHEMA_DESCRIPTION = "Schéma JSON attendu en sortie (optionnel, utilisé uniquement si le format est JSON)";
    private static final String JSON_SCHEMA_PLACEHOLDER = "{ nom: string, prenom: string, ... }";
    private static final String JSON_SCHEMA_SYSTEM_PREFIX = "Tu dois retourner UNIQUEMENT un objet JSON valide conforme au schéma suivant, sans aucun autre texte :\n";
    private static final String ERROR_INVALID_JSON_SCHEMA = "The provided JSON schema is not valid JSON: %s";

    private static final ObjectMapper _objectMapper = new ObjectMapper( );
    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String DEFAULT_OUTPUT_PORT_DESCRIPTION = "Sortie par défaut";
    private static final String MODEL_RESPONSE_KEY = "model_response";
    private static final String INPUT_PROMPT_KEY = "input_prompt";
    private static final String PROVIDER_INFO_KEY = "provider_info";
    private static final String MODEL_RESPONSE_DESCRIPTION = "Réponse générée par le modèle";
    private static final String INPUT_PROMPT_DESCRIPTION = "Prompt utilisé pour la génération";
    private static final String PROVIDER_INFO_DESCRIPTION = "Informations sur le fournisseur utilisé";
    private static final String ERROR_PROVIDER_NOT_FOUND = "Provider with ID %s not found";
    private static final String ERROR_PROVIDER_INVALID = "Invalid provider: %s";
    private static final String ERROR_PROMPT_EMPTY = "The prompt cannot be empty";
    private static final String ERROR_MODEL_GENERATION = "Error during model generation for node %s";
    private static final String ERROR_MODEL_EXCEPTION = "Exception while calling the model for node %s";
    private static final String SUCCESS_GENERATION = "Generation succeeded for node %s with provider %s";
    private static final String DEFAULT_PROVIDER_OPTION_VALUE = "";
    private static final String DEFAULT_PROVIDER_OPTION_LABEL = "Sélectionner un fournisseur";
    private static final String DEFAULT_PROVIDER_OPTION_DESC = "Veuillez sélectionner un fournisseur configuré";
    private static final String PROVIDER_DESC_DEFAULT = "Fournisseur ";
    private static final String PROVIDER_INFO_ID_KEY = "id";
    private static final String PROVIDER_INFO_NAME_KEY = "name";
    private static final String PROVIDER_INFO_MODEL_KEY = "model";

    public static final PipelineVariable PROMPT = new PipelineVariable.Builder( PROMPT_NAME ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( PROMPT_TITLE ).description( PROMPT_DESCRIPTION ).minLength( 1 ).textarea( true ).rows( 8 ).placeholder( PROMPT_PLACEHOLDER ).build( );

    public static final PipelineVariable RESPONSE_FORMAT = new PipelineVariable.Builder( RESPONSE_FORMAT_NAME ).type( PipelineVariable.VariableType.ENUM )
            .required( false ).title( RESPONSE_FORMAT_TITLE ).description( RESPONSE_FORMAT_DESCRIPTION ).defaultValue( RESPONSE_FORMAT_TEXT )
            .options( new PipelineVariable.EnumOption( RESPONSE_FORMAT_TEXT, "Texte libre", "Le modèle répond en texte libre" ),
                    new PipelineVariable.EnumOption( RESPONSE_FORMAT_JSON, "JSON", "Le modèle répond en JSON structuré" ) )
            .build( );

    public static final PipelineVariable JSON_SCHEMA = new PipelineVariable.Builder( JSON_SCHEMA_NAME ).type( PipelineVariable.VariableType.STRING )
            .required( false ).title( JSON_SCHEMA_TITLE ).description( JSON_SCHEMA_DESCRIPTION ).textarea( true ).rows( 6 )
            .placeholder( JSON_SCHEMA_PLACEHOLDER ).build( );

    private final ModelService modelService;

    /**
     * Constructor. Initializes the node with the model node type and resolves the model service from CDI.
     */
    public ModelNode( )
    {
        super( PipelineConstants.NODE_TYPE_MODEL );
        this.modelService = CDI.current( ).select( ModelService.class ).get( );
    }

    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );

        PipelineVariable providerVariable = createProviderVariable( );
        variables.add( providerVariable );
        variables.add( PROMPT );
        variables.add( RESPONSE_FORMAT );
        variables.add( JSON_SCHEMA );

        return variables;
    }

    /**
     * Builds the provider selection variable from the list of configured providers.
     *
     * @return the provider configuration variable
     */
    private PipelineVariable createProviderVariable( )
    {
        List<PipelineVariable.EnumOption> providerOptions = loadProviderOptions( );

        return new PipelineVariable.Builder( PROVIDER_NAME ).type( PipelineVariable.VariableType.ENUM ).required( true ).title( PROVIDER_TITLE )
                .description( PROVIDER_DESCRIPTION ).options( providerOptions.toArray( new PipelineVariable.EnumOption [ 0] ) )
                .defaultValue( providerOptions.isEmpty( ) ? DEFAULT_PROVIDER_OPTION_VALUE : providerOptions.get( 0 ).getValue( ) ).build( );
    }

    /**
     * Loads the available providers as enum options for the provider selection variable.
     *
     * @return the list of provider options, with a placeholder option when no provider is configured
     */
    private List<PipelineVariable.EnumOption> loadProviderOptions( )
    {
        List<PipelineVariable.EnumOption> options = new ArrayList<>( );

        try
        {
            List<Provider> providers = ProviderHome.getProvidersList( );

            if ( providers.isEmpty( ) )
            {
                options.add( new PipelineVariable.EnumOption( DEFAULT_PROVIDER_OPTION_VALUE, DEFAULT_PROVIDER_OPTION_LABEL, DEFAULT_PROVIDER_OPTION_DESC ) );
            }
            else
            {
                for ( Provider provider : providers )
                {
                    String value = String.valueOf( provider.getId( ) );
                    String label = provider.getProviderName( ) + " (" + provider.getDeploymentModelName( ) + ")";
                    String description = provider.getProviderDescription( ) != null ? provider.getProviderDescription( )
                            : PROVIDER_DESC_DEFAULT + provider.getProviderType( );

                    options.add( new PipelineVariable.EnumOption( value, label, description ) );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.info( "Unable to load providers, perhaps Spring is not yet initialized: {}", e.getMessage( ) );

            options.add( new PipelineVariable.EnumOption( DEFAULT_PROVIDER_OPTION_VALUE, DEFAULT_PROVIDER_OPTION_LABEL, DEFAULT_PROVIDER_OPTION_DESC ) );
        }

        return options;
    }

    @Override
    protected Map<String, String> initializeInputPorts( )
    {
        Map<String, String> ports = new LinkedHashMap<>( );
        ports.put( INPUT_PORT_KEY, DEFAULT_INPUT_PORT_DESCRIPTION );
        return ports;
    }

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
        Object providerValue = resolvedData.get( PROVIDER_NAME );
        String providerIdStr = ( providerValue != null ) ? providerValue.toString( ) : null;
        String prompt = getParam( resolvedData, PROMPT );
        String responseFormat = getParamWithDefault( resolvedData, RESPONSE_FORMAT, RESPONSE_FORMAT_TEXT );
        String jsonSchema = getParamWithDefault( resolvedData, JSON_SCHEMA, "" );

        validateParameters( providerIdStr, prompt );

        Integer providerId = parseProviderId( providerIdStr );
        Provider provider = getProvider( providerId );
        boolean useJsonMode = RESPONSE_FORMAT_JSON.equalsIgnoreCase( responseFormat );

        if ( useJsonMode && !jsonSchema.isEmpty( ) )
        {
            validateJsonSchema( jsonSchema );
            prompt = JSON_SCHEMA_SYSTEM_PREFIX + jsonSchema + "\n\n" + prompt;
        }

        generateModelResponse( context, config, provider, prompt, useJsonMode );

        return context;
    }

    /**
     * Validates that the provided string is valid JSON.
     *
     * @param jsonSchema
     *            the JSON string to validate
     */
    /**
     * Strips markdown code fences (```json ... ```) from LLM output.
     *
     * @param text
     *            the raw LLM response
     * @return the cleaned text
     */
    private String stripMarkdownFences( String text )
    {
        if ( text == null )
        {
            return text;
        }
        return text.replaceAll( "(?s)^\\s*```\\w*\\s*", "" ).replaceAll( "(?s)\\s*```\\s*$", "" ).trim( );
    }

    /**
     * Validates that the provided string is valid JSON, throwing a runtime exception otherwise.
     *
     * @param jsonSchema
     *            the JSON schema string to validate
     */
    private void validateJsonSchema( String jsonSchema )
    {
        try
        {
            _objectMapper.readTree( jsonSchema );
        }
        catch( Exception e )
        {
            String error = String.format( ERROR_INVALID_JSON_SCHEMA, e.getMessage( ) );
            AppLogService.error( error );
            throw new RuntimeException( error );
        }
    }

    /**
     * Validates that the provider identifier and prompt are present and not empty.
     *
     * @param providerIdStr
     *            the provider identifier string
     * @param prompt
     *            the prompt to send to the model
     */
    private void validateParameters( String providerIdStr, String prompt )
    {
        if ( providerIdStr == null || providerIdStr.trim( ).isEmpty( ) )
        {
            String error = String.format( ERROR_PROVIDER_INVALID, providerIdStr );
            AppLogService.error( error );
            throw new RuntimeException( error );
        }

        if ( prompt == null || prompt.trim( ).isEmpty( ) )
        {
            AppLogService.error( ERROR_PROMPT_EMPTY );
            throw new RuntimeException( ERROR_PROMPT_EMPTY );
        }
    }

    /**
     * Parses the provider identifier string into a positive integer, throwing a runtime exception on invalid input.
     *
     * @param providerIdStr
     *            the provider identifier string
     * @return the parsed provider identifier
     */
    private Integer parseProviderId( String providerIdStr )
    {
        Integer providerId;
        try
        {
            providerId = Integer.valueOf( providerIdStr );
        }
        catch( NumberFormatException e )
        {
            String error = String.format( ERROR_PROVIDER_INVALID, providerIdStr );
            AppLogService.error( error, e );
            throw new RuntimeException( error );
        }

        if ( providerId <= 0 )
        {
            String error = String.format( ERROR_PROVIDER_INVALID, providerIdStr );
            AppLogService.error( error );
            throw new RuntimeException( error );
        }

        return providerId;
    }

    /**
     * Retrieves the provider matching the given identifier, throwing a runtime exception when not found.
     *
     * @param providerId
     *            the provider identifier
     * @return the matching provider
     */
    private Provider getProvider( Integer providerId )
    {
        return ProviderHome.findByPrimaryKey( providerId ).orElseThrow( ( ) -> {
            String error = String.format( ERROR_PROVIDER_NOT_FOUND, providerId );
            AppLogService.error( error );
            return new RuntimeException( error );
        } );
    }

    /**
     * Generates the model response by invoking the chat model and writes the result into the node output data.
     *
     * @param context
     *            the pipeline execution context
     * @param config
     *            the node configuration
     * @param provider
     *            the provider whose model is invoked
     * @param prompt
     *            the prompt sent to the model
     * @param useJsonMode
     *            whether the model should answer in JSON mode
     */
    private void generateModelResponse( PipelineContext context, PipelineNodeConfig config, Provider provider, String prompt, boolean useJsonMode )
    {
        try
        {
            String executionId = context.getObservabilityId( );
            String nodeId = config.getId( );
            int nodeOrder = getNodeOrder( context );

            ChatModel chatModel = useJsonMode ? modelService.createTrackedJsonChatModel( provider, executionId, nodeId, nodeOrder )
                    : modelService.createTrackedChatModel( provider, executionId, nodeId, nodeOrder );
            UserMessage userMessage = UserMessage.from( prompt );
            ChatResponse response = chatModel.chat( userMessage );

            String generatedText = response.aiMessage( ).text( );

            if ( useJsonMode )
            {
                generatedText = stripMarkdownFences( generatedText );
            }

            Object modelResponse = generatedText;
            if ( useJsonMode )
            {
                try
                {
                    modelResponse = _objectMapper.readValue( generatedText, Map.class );
                }
                catch( Exception e )
                {
                    AppLogService.info( "JSON response not parseable as Map, kept as String: {}", e.getMessage( ) );
                }
            }

            Map<String, Object> outputData = createOutputData( provider, prompt, modelResponse );

            prepareOutputData( context, config, outputData );

            AppLogService.info( String.format( SUCCESS_GENERATION, config.getId( ), provider.getProviderName( ) ) );

        }
        catch( Exception e )
        {
            AppLogService.error( String.format( ERROR_MODEL_EXCEPTION, config.getId( ) ), e );
            throw new RuntimeException( String.format( ERROR_MODEL_GENERATION, config.getId( ) ), e );
        }
    }

    /**
     * Builds the node output data map from the generated content, the prompt and the provider information.
     *
     * @param provider
     *            the provider used for generation
     * @param prompt
     *            the prompt used for generation
     * @param generatedContent
     *            the content produced by the model
     * @return the output data map
     */
    private Map<String, Object> createOutputData( Provider provider, String prompt, Object generatedContent )
    {
        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( MODEL_RESPONSE_KEY, generatedContent );
        outputData.put( INPUT_PROMPT_KEY, prompt );

        Map<String, Object> providerInfo = new HashMap<>( );
        providerInfo.put( PROVIDER_INFO_ID_KEY, provider.getId( ) );
        providerInfo.put( PROVIDER_INFO_NAME_KEY, provider.getProviderName( ) );
        providerInfo.put( PROVIDER_INFO_MODEL_KEY, provider.getDeploymentModelName( ) );
        outputData.put( PROVIDER_INFO_KEY, providerInfo );

        return outputData;
    }

    /**
     * Computes the order of the current node based on the number of outputs already produced in the context.
     *
     * @param context
     *            the pipeline execution context
     * @return the node order
     */
    private int getNodeOrder( PipelineContext context )
    {
        return context.getAllOutputs( ).size( ) + 1;
    }

    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( MODEL_RESPONSE_KEY, MODEL_RESPONSE_DESCRIPTION );
        outputs.put( INPUT_PROMPT_KEY, INPUT_PROMPT_DESCRIPTION );
        outputs.put( PROVIDER_INFO_KEY, PROVIDER_INFO_DESCRIPTION );
        return outputs;
    }
}
