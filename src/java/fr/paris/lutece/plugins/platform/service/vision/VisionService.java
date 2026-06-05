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
package fr.paris.lutece.plugins.platform.service.vision;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import fr.paris.lutece.plugins.platform.business.client.Client;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.business.vision.VisionResponseDTO;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.business.vision.Vision;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractor;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorField;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorFieldHome;
import fr.paris.lutece.plugins.platform.business.vision.VisionExtractorHome;
import fr.paris.lutece.plugins.platform.business.vision.VisionHome;
import fr.paris.lutece.plugins.platform.business.vision.VisionFieldType;
import fr.paris.lutece.plugins.platform.service.exception.AccessDeniedException;
import fr.paris.lutece.plugins.platform.service.exception.InvalidFieldTypeException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.model.ModelService;
import fr.paris.lutece.plugins.platform.service.model.OcrService;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import fr.paris.lutece.plugins.platform.service.security.ClientService;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Service for processing vision-based operations including OCR and data extraction from images. Provides functionality to analyze images, extract text through
 * OCR, and extract structured data based on configured extractors.
 */
@ApplicationScoped
@Named( "platform.visionService" )
public class VisionService
{
    private static final String ERROR_VISION_NOT_FOUND = "Vision resource not found: ";
    private static final String ERROR_PROVIDER_NOT_FOUND = "Provider not found for vision: ";
    private static final String ERROR_IMAGE_PROCESSING = "Error processing image: ";
    private static final String ERROR_INVALID_IMAGE = "Invalid or unsupported image format";
    private static final String ERROR_EXTRACTION_FAILED = "Field extraction failed: ";
    private static final String OBS_OCR_OPERATION = "OCR Operation";
    private static final String OBS_EXTRACTION_OPERATION = "Extraction Operation";
    private static final String OBS_PROCESSING_IMAGE = "Processing image with OCR";
    private static final String OBS_EXTRACTING_DATA = "Extracting data from text";
    private static final String OBS_OCR_RESULT = "OCR Result";
    private static final String OBS_EXTRACTION_RESULT = "Extraction Result";
    private static final String OBS_OCR_COMPLETE = "OCR completed successfully";
    private static final String OBS_EXTRACTION_COMPLETE = "Extraction completed successfully";
    private static final String NODE_ID_PREFIX_OCR = "ocr-vision-";
    private static final String NODE_ID_PREFIX_EXTRACT = "extract-vision-";
    private static final String VISION_PROCESSING_FAILED = "Vision processing failed: ";
    private static final String OCR_FAILED_MESSAGE = "OCR failed: ";
    private static final String EXTRACTION_FAILED_MESSAGE = "Extraction failed: ";
    private static final String IMAGE_TOO_SMALL_MESSAGE = "Image too small or low quality for OCR processing";
    private static final String OCR_ENABLED_BUT_FAILED = "OCR was enabled but failed or produced no text.";
    private static final String VISION_NOT_FOUND_MESSAGE = "Vision not found: ";
    private static final String ACCESS_DENIED_MESSAGE = "Access denied to vision resource ";
    private static final String EXTRACTED_PREFIX = "Extracted ";
    private static final String CHARACTERS_SUFFIX = " characters";
    private static final String CHARS_SUFFIX = " chars";
    private static final String DATA_EXTRACTED_BY = "Data extracted by ";
    private static final String FIELD_PREFIX = "Field ";
    private static final String EXPERT_DATA_EXTRACTOR_PROMPT = "You are an expert data extractor. Extract information from the provided text according to the following JSON schema. Only output a valid JSON object that strictly adheres to this schema. The JSON schema is: \n";
    private static final String ERROR_BUILDING_JSON_SCHEMA = "Error building JSON schema for extractors";
    private static final String EMPTY_JSON = "{}";
    private static final String JSON_TYPE_OBJECT = "object";
    private static final String JSON_TYPE_STRING = "string";
    private static final String JSON_TYPE_NUMBER = "number";
    private static final String JSON_TYPE_BOOLEAN = "boolean";
    private static final String JSON_FIELD_TYPE = "type";
    private static final String JSON_FIELD_FORMAT = "format";
    private static final String JSON_FIELD_DESCRIPTION = "description";
    private static final String JSON_FIELD_PROPERTIES = "properties";
    private static final String FORMAT_DATE = "date";
    private static final String FORMAT_EMAIL = "email";
    private static final String EXTRACTED_JSON_PREFIX = "Extracted JSON: ";

    private static final ObjectMapper _objectMapper = new ObjectMapper( );

    @Inject
    private ObservabilityService _observabilityService;

    @Inject
    private ModelService _modelService;

    @Inject
    private ClientService _clientService;

    @Inject
    private SubscriptionService _subscriptionService;

    VisionService( )
    {
    }

    /**
     * Processes an image through OCR and/or data extraction based on vision configuration for a specific client.
     *
     * @param visionId
     *            the ID of the vision configuration to use
     * @param imageBytes
     *            the image data as byte array
     * @param client
     *            the client requesting the operation
     * @return VisionResponseDTO containing OCR text and/or extracted data
     * @throws Exception
     *             if processing fails
     */
    public VisionResponseDTO processImageForClient( int visionId, byte [ ] imageBytes, Client client ) throws Exception
    {
        validateVisionAccessForClient( visionId, client );

        Vision vision = VisionHome.findByPrimaryKey( visionId ).orElse( null );
        if ( vision == null )
        {
            return VisionResponseDTO.error( ERROR_VISION_NOT_FOUND + visionId );
        }

        List<VisionExtractor> extractors = VisionExtractorHome.findByVisionId( visionId );

        for ( VisionExtractor extractor : extractors )
        {
            List<VisionExtractorField> fields = VisionExtractorFieldHome.findByExtractorId( extractor.getId( ) );
            extractor.setFields( fields );
        }
        vision.setExtractors( extractors );

        Provider provider = ProviderHome.findByPrimaryKey( vision.getProviderId( ) ).orElse( null );
        if ( provider == null )
        {
            return VisionResponseDTO.error( ERROR_PROVIDER_NOT_FOUND + vision.getVisionTitle( ) );
        }

        return executeVisionProcessing( vision, imageBytes, provider, visionId );
    }

    /**
     * Processes an image through OCR and/or data extraction based on vision configuration.
     *
     * @param visionId
     *            the ID of the vision configuration to use
     * @param imageBytes
     *            the image data as byte array
     * @param adminUser
     *            the admin user performing the operation
     * @return VisionResponseDTO containing OCR text and/or extracted data
     * @throws Exception
     *             if processing fails
     */
    public VisionResponseDTO processImage( int visionId, byte [ ] imageBytes, LuteceUser user ) throws Exception
    {
        validateVisionAccess( visionId, user );

        Vision vision = VisionHome.findByPrimaryKey( visionId ).orElse( null );
        if ( vision == null )
        {
            return VisionResponseDTO.error( ERROR_VISION_NOT_FOUND + visionId );
        }

        List<VisionExtractor> extractors = VisionExtractorHome.findByVisionId( visionId );

        for ( VisionExtractor extractor : extractors )
        {
            List<VisionExtractorField> fields = VisionExtractorFieldHome.findByExtractorId( extractor.getId( ) );
            extractor.setFields( fields );
        }
        vision.setExtractors( extractors );

        Provider provider = ProviderHome.findByPrimaryKey( vision.getProviderId( ) ).orElse( null );
        if ( provider == null )
        {
            return VisionResponseDTO.error( ERROR_PROVIDER_NOT_FOUND + vision.getVisionTitle( ) );
        }

        return executeVisionProcessing( vision, imageBytes, provider, visionId );
    }

    /**
     * Executes the complete vision processing workflow including OCR and extraction.
     *
     * @param vision
     *            the vision configuration
     * @param imageBytes
     *            the image data
     * @param provider
     *            the AI provider to use
     * @param visionId
     *            the vision ID for observability
     * @return VisionResponseDTO with results
     */
    private VisionResponseDTO executeVisionProcessing( Vision vision, byte [ ] imageBytes, Provider provider, int visionId )
    {
        VisionResponseDTO response = VisionResponseDTO.success( );
        String executionId = _observabilityService.startResourceExecution( Vision.RESOURCE_TYPE, String.valueOf( visionId ), vision.getClientId( ),
                ObservabilityData.of( "VISION_PROCESSING", "visionId", visionId, "title", vision.getVisionTitle( ) ) );

        try
        {
            String ocrResult = processOCR( imageBytes, provider, executionId, visionId );
            response.withOcrText( ocrResult );

            if ( vision.getExtractors( ) != null && !vision.getExtractors( ).isEmpty( ) )
            {
                JsonNode extractedData = processExtraction( vision, response.getOcrText( ), provider, executionId, visionId );
                if ( extractedData != null )
                {
                    response.withExtractedData( extractedData );
                }
            }

            _observabilityService.completeResourceExecutionSuccess( executionId, ObservabilityData.of( "VISION_RESULT", "success", true, "ocrText",
                    response.getOcrText( ), "extractedData", response.getExtractedData( ) != null ? response.getExtractedData( ).toString( ) : null ) );
        }
        catch( Exception e )
        {
            String errorMsg = VISION_PROCESSING_FAILED + e.getMessage( );
            AppLogService.error( errorMsg, e );
            _observabilityService.completeResourceExecutionError( executionId, errorMsg, null );
            return VisionResponseDTO.error( errorMsg );
        }

        return response;
    }

    /**
     * Processes OCR (Optical Character Recognition) on the provided image.
     *
     * @param vision
     *            the vision configuration
     * @param imageBytes
     *            the image data
     * @param provider
     *            the AI provider
     * @param executionId
     *            the execution ID for observability
     * @param visionId
     *            the vision ID
     * @return extracted text from the image
     * @throws Exception
     *             if OCR processing fails
     */
    private String processOCR( byte [ ] imageBytes, Provider provider, String executionId, int visionId ) throws Exception
    {
        String ocrNodeId = NODE_ID_PREFIX_OCR + visionId + "-" + UUID.randomUUID( ).toString( ).substring( 0, 8 );

        try
        {
            _observabilityService.startNodeExecution( executionId, ocrNodeId, OBS_OCR_OPERATION, 1,
                    ObservabilityData.input( OBS_OCR_OPERATION, OBS_PROCESSING_IMAGE ) );

            BufferedImage image = loadImage( imageBytes );
            OcrService ocrService = new OcrService( );

            if ( !ocrService.isImageWorthProcessing( image ) )
            {
                _observabilityService.completeNodeExecutionSuccess( executionId, ocrNodeId, ObservabilityData.output( IMAGE_TOO_SMALL_MESSAGE, null ) );
                return "";
            }

            BufferedImage optimizedImage = ocrService.optimizeImageForOCR( image );
            String ocrText = ocrService.performOCR( optimizedImage, provider, executionId, ocrNodeId );

            _observabilityService.addNodeTrace( executionId, ocrNodeId,
                    ObservabilityData.trace( OBS_OCR_RESULT, OBS_OCR_RESULT,
                            EXTRACTED_PREFIX + ( ocrText != null ? ocrText.length( ) : 0 ) + CHARACTERS_SUFFIX ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

            _observabilityService.completeNodeExecutionSuccess( executionId, ocrNodeId, ObservabilityData.output( OBS_OCR_COMPLETE, ocrText ) );
            return ocrText != null ? ocrText : "";
        }
        catch( Exception e )
        {
            String errorMsg = ERROR_IMAGE_PROCESSING + e.getMessage( );
            AppLogService.error( errorMsg, e );
            _observabilityService.completeNodeExecutionError( executionId, ocrNodeId, errorMsg, null );
            throw new Exception( OCR_FAILED_MESSAGE + e.getMessage( ), e );
        }
    }

    /**
     * Processes data extraction from OCR text using configured extractors.
     *
     * @param vision
     *            the vision configuration
     * @param ocrText
     *            the text extracted from OCR
     * @param provider
     *            the AI provider
     * @param executionId
     *            the execution ID for observability
     * @param visionId
     *            the vision ID
     * @return extracted data as JsonNode
     * @throws Exception
     *             if extraction fails
     */
    /**
     * Strips optional Markdown code fences (```json ... ```) that LLMs often wrap around JSON output despite instructions, so the payload can be parsed as raw
     * JSON.
     *
     * @param text
     *            the raw LLM output, possibly fenced
     * @return the unfenced JSON payload, or the trimmed input when no fence is present
     */
    private static String stripMarkdownFences( String text )
    {
        if ( text == null )
        {
            return null;
        }
        String trimmed = text.trim( );
        if ( trimmed.startsWith( "```" ) )
        {
            int firstNewline = trimmed.indexOf( '\n' );
            int lastFence = trimmed.lastIndexOf( "```" );
            if ( firstNewline >= 0 && lastFence > firstNewline )
            {
                return trimmed.substring( firstNewline + 1, lastFence ).trim( );
            }
        }
        return trimmed;
    }

    private JsonNode processExtraction( Vision vision, String ocrText, Provider provider, String executionId, int visionId ) throws Exception
    {
        validateExtractionPreconditions( vision, ocrText );

        String extractNodeId = NODE_ID_PREFIX_EXTRACT + visionId + "-" + UUID.randomUUID( ).toString( ).substring( 0, 8 );

        try
        {
            _observabilityService.startNodeExecution( executionId, extractNodeId, OBS_EXTRACTION_OPERATION, 2,
                    ObservabilityData.input( OBS_EXTRACTION_OPERATION, OBS_EXTRACTING_DATA ) );

            String jsonSchema = buildJsonSchemaForExtractors( vision.getExtractors( ) );
            String systemPrompt = EXPERT_DATA_EXTRACTOR_PROMPT + jsonSchema;

            ChatModel chatModel = _modelService.createTrackedJsonChatModel( provider, executionId, extractNodeId, 1 );
            List<ChatMessage> messages = List.of( new SystemMessage( systemPrompt ), new UserMessage( ocrText ) );

            dev.langchain4j.model.chat.response.ChatResponse response = chatModel.chat( messages );
            String extractedJsonString = stripMarkdownFences( response.aiMessage( ).text( ) );
            JsonNode extractedJsonNode = _objectMapper.readTree( extractedJsonString );

            _observabilityService.addNodeTrace( executionId, extractNodeId,
                    ObservabilityData.trace( OBS_EXTRACTION_RESULT, OBS_EXTRACTION_RESULT,
                            EXTRACTED_JSON_PREFIX + ( extractedJsonString != null ? extractedJsonString.length( ) : 0 ) + CHARS_SUFFIX ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

            _observabilityService.completeNodeExecutionSuccess( executionId, extractNodeId,
                    ObservabilityData.output( OBS_EXTRACTION_COMPLETE, extractedJsonString ) );
            return extractedJsonNode;
        }
        catch( IOException | RuntimeException e )
        {
            String errorMsg = ERROR_EXTRACTION_FAILED + e.getMessage( );
            AppLogService.error( errorMsg, e );
            _observabilityService.completeNodeExecutionError( executionId, extractNodeId, errorMsg, null );
            throw new Exception( EXTRACTION_FAILED_MESSAGE + e.getMessage( ), e );
        }
    }

    /**
     * Validates preconditions for data extraction process.
     *
     * @param vision
     *            the vision configuration
     * @param ocrText
     *            the OCR text to validate
     * @throws IllegalStateException
     *             if preconditions are not met
     */
    private void validateExtractionPreconditions( Vision vision, String ocrText )
    {
        if ( ocrText == null )
        {
            throw new IllegalStateException( OCR_ENABLED_BUT_FAILED );
        }
    }

    /**
     * Builds a JSON schema for the configured extractors.
     *
     * @param extractors
     *            the list of vision extractors
     * @return JSON schema as string
     */
    private String buildJsonSchemaForExtractors( List<VisionExtractor> extractors )
    {
        ObjectNode globalSchema = JsonNodeFactory.instance.objectNode( );
        globalSchema.put( JSON_FIELD_TYPE, JSON_TYPE_OBJECT );
        ObjectNode propertiesNode = JsonNodeFactory.instance.objectNode( );

        for ( VisionExtractor extractor : extractors )
        {
            ObjectNode extractorSchema = createExtractorSchema( extractor );
            propertiesNode.set( extractor.getExtractorName( ), extractorSchema );
        }

        globalSchema.set( JSON_FIELD_PROPERTIES, propertiesNode );

        try
        {
            return _objectMapper.writerWithDefaultPrettyPrinter( ).writeValueAsString( globalSchema );
        }
        catch( IOException e )
        {
            AppLogService.error( ERROR_BUILDING_JSON_SCHEMA, e );
            return EMPTY_JSON;
        }
    }

    /**
     * Creates a JSON schema for a single extractor.
     *
     * @param extractor
     *            the vision extractor
     * @return ObjectNode representing the extractor schema
     */
    private ObjectNode createExtractorSchema( VisionExtractor extractor )
    {
        ObjectNode extractorSchema = JsonNodeFactory.instance.objectNode( );
        extractorSchema.put( JSON_FIELD_TYPE, JSON_TYPE_OBJECT );
        extractorSchema.put( JSON_FIELD_DESCRIPTION,
                extractor.getExtractorDescription( ) != null ? extractor.getExtractorDescription( ) : DATA_EXTRACTED_BY + extractor.getExtractorName( ) );

        ObjectNode fieldProperties = JsonNodeFactory.instance.objectNode( );

        for ( VisionExtractorField field : extractor.getFields( ) )
        {
            ObjectNode fieldSchema = createFieldSchema( field );
            fieldProperties.set( field.getFieldName( ), fieldSchema );
        }

        extractorSchema.set( JSON_FIELD_PROPERTIES, fieldProperties );
        return extractorSchema;
    }

    /**
     * Creates a JSON schema for a single field.
     *
     * @param field
     *            the vision extractor field
     * @return ObjectNode representing the field schema
     */
    private ObjectNode createFieldSchema( VisionExtractorField field )
    {
        ObjectNode fieldSchema = JsonNodeFactory.instance.objectNode( );
        String jsonType = JSON_TYPE_STRING;
        String format = null;

        switch( field.getFieldType( ) )
        {
            case NUMBER:
                jsonType = JSON_TYPE_NUMBER;
                break;
            case BOOLEAN:
                jsonType = JSON_TYPE_BOOLEAN;
                break;
            case DATE:
                format = FORMAT_DATE;
                break;
            case EMAIL:
                format = FORMAT_EMAIL;
                break;
            case STRING:
            default:
                jsonType = JSON_TYPE_STRING;
                break;
        }

        fieldSchema.put( JSON_FIELD_TYPE, jsonType );
        if ( format != null )
        {
            fieldSchema.put( JSON_FIELD_FORMAT, format );
        }
        fieldSchema.put( JSON_FIELD_DESCRIPTION, field.getFieldDescription( ) != null ? field.getFieldDescription( ) : FIELD_PREFIX + field.getFieldName( ) );

        return fieldSchema;
    }

    /**
     * Loads a BufferedImage from byte array.
     *
     * @param imageBytes
     *            the image data as bytes
     * @return BufferedImage object
     * @throws IOException
     *             if image cannot be loaded
     */
    private BufferedImage loadImage( byte [ ] imageBytes ) throws IOException
    {
        try ( ByteArrayInputStream bais = new ByteArrayInputStream( imageBytes ) )
        {
            BufferedImage image = ImageIO.read( bais );
            if ( image == null )
            {
                throw new IOException( ERROR_INVALID_IMAGE );
            }
            return image;
        }
    }

    /**
     * Validates that the admin user has access to the specified vision resource.
     *
     * @param visionId
     *            the vision ID to validate access for
     * @param adminUser
     *            the admin user requesting access
     * @throws ResourceNotFoundException
     *             if vision is not found
     * @throws AccessDeniedException
     *             if user doesn't have access
     */
    private void validateVisionAccess( int visionId, LuteceUser user )
    {
        Vision vision = VisionHome.findByPrimaryKey( visionId ).orElseThrow( ( ) -> new ResourceNotFoundException( VISION_NOT_FOUND_MESSAGE + visionId ) );

        List<Integer> authorizedClientIds = _clientService.getAuthorizedClientIds( user );
        if ( !authorizedClientIds.contains( vision.getClientId( ) ) )
        {
            throw new AccessDeniedException( ACCESS_DENIED_MESSAGE + visionId );
        }
    }

    /**
     * Validates that the client has access to the specified vision resource.
     *
     * @param visionId
     *            the vision ID to validate access for
     * @param client
     *            the client requesting access
     * @throws ResourceNotFoundException
     *             if vision is not found
     * @throws AccessDeniedException
     *             if client doesn't have access
     */
    private void validateVisionAccessForClient( int visionId, Client client )
    {
        Vision vision = VisionHome.findByPrimaryKey( visionId ).orElseThrow( ( ) -> new ResourceNotFoundException( VISION_NOT_FOUND_MESSAGE + visionId ) );

        if ( client.getId( ) != vision.getClientId( ) )
        {
            throw new AccessDeniedException( ACCESS_DENIED_MESSAGE + visionId );
        }

        if ( !_subscriptionService.hasActiveSubscription( client.getId( ), Vision.RESOURCE_TYPE, String.valueOf( visionId ) ) )
        {
            throw new AccessDeniedException( "No active subscription to vision resource " + visionId );
        }
    }

    /**
     * Loads a vision by its identifier with its extractors fully populated, each extractor carrying its own fields.
     *
     * @param visionId
     *            the vision identifier
     * @return an Optional holding the vision with its extractor/field graph, empty if no vision matches
     */
    public Optional<Vision> loadVisionWithExtractorsAndFields( int visionId )
    {
        Optional<Vision> optVision = VisionHome.findByPrimaryKey( visionId );
        if ( optVision.isEmpty( ) )
        {
            return optVision;
        }

        Vision vision = optVision.get( );
        List<VisionExtractor> extractors = VisionExtractorHome.findByVisionId( vision.getId( ) );
        for ( VisionExtractor extractor : extractors )
        {
            extractor.setFields( VisionExtractorFieldHome.findByExtractorId( extractor.getId( ) ) );
        }
        vision.setExtractors( extractors );
        return optVision;
    }

    /**
     * Resolves the provider configured for the given vision.
     *
     * @param vision
     *            the vision whose provider is resolved
     * @return an Optional holding the provider, empty if none matches the vision provider identifier
     */
    public Optional<Provider> resolveProvider( Vision vision )
    {
        return ProviderHome.findByPrimaryKey( vision.getProviderId( ) );
    }

    /**
     * Persists a new vision, optionally assigning it to a client.
     *
     * @param vision
     *            the vision to create
     * @param clientId
     *            the owning client identifier, or null to leave it unset
     * @return the created vision with its generated identifier
     */
    public Vision createVision( Vision vision, Integer clientId )
    {
        if ( clientId != null )
        {
            vision.setClientId( clientId );
        }
        return VisionHome.create( vision );
    }

    /**
     * Persists changes made to an existing vision.
     *
     * @param vision
     *            the vision to update
     */
    public void updateVision( Vision vision )
    {
        VisionHome.update( vision );
    }

    /**
     * Removes a vision by its identifier and returns the identifier of its owning client.
     *
     * @param visionId
     *            the vision identifier
     * @return the owning client identifier
     * @throws ResourceNotFoundException
     *             if no vision matches the identifier
     */
    public int removeVision( int visionId )
    {
        Vision vision = VisionHome.findByPrimaryKey( visionId ).orElseThrow( ( ) -> new ResourceNotFoundException( VISION_NOT_FOUND_MESSAGE + visionId ) );
        int clientId = vision.getClientId( );
        VisionHome.remove( visionId );
        return clientId;
    }

    /**
     * Creates a new extractor attached to the given vision.
     *
     * @param visionId
     *            the owning vision identifier
     * @param name
     *            the extractor name
     * @param description
     *            the extractor description
     * @return the created extractor with its generated identifier
     */
    public VisionExtractor addExtractor( int visionId, String name, String description )
    {
        VisionExtractor extractor = new VisionExtractor( );
        extractor.setVisionId( visionId );
        extractor.setExtractorName( name );
        extractor.setExtractorDescription( description );
        return VisionExtractorHome.create( extractor );
    }

    /**
     * Updates the name and description of an existing extractor.
     *
     * @param extractorId
     *            the extractor identifier
     * @param name
     *            the new extractor name
     * @param description
     *            the new extractor description
     * @return true if the extractor existed and was updated, false otherwise
     */
    public boolean updateExtractor( int extractorId, String name, String description )
    {
        VisionExtractor extractor = VisionExtractorHome.findByPrimaryKey( extractorId ).orElse( null );
        if ( extractor == null )
        {
            return false;
        }
        extractor.setExtractorName( name );
        extractor.setExtractorDescription( description );
        VisionExtractorHome.update( extractor );
        return true;
    }

    /**
     * Removes an extractor by its identifier.
     *
     * @param extractorId
     *            the extractor identifier
     */
    public void removeExtractor( int extractorId )
    {
        VisionExtractorHome.remove( extractorId );
    }

    /**
     * Creates a new extractor field after parsing and validating its type.
     *
     * @param extractorId
     *            the owning extractor identifier
     * @param name
     *            the field name
     * @param description
     *            the field description
     * @param fieldType
     *            the textual field type, parsed against {@link VisionFieldType}
     * @return the created field with its generated identifier
     * @throws InvalidFieldTypeException
     *             if the field type cannot be parsed
     */
    public VisionExtractorField addExtractorField( int extractorId, String name, String description, String fieldType ) throws InvalidFieldTypeException
    {
        VisionExtractorField field = new VisionExtractorField( );
        field.setExtractorId( extractorId );
        field.setFieldName( name );
        field.setFieldDescription( description );
        field.setFieldType( parseFieldType( fieldType ) );
        return VisionExtractorFieldHome.create( field );
    }

    /**
     * Updates an existing extractor field after parsing and validating its type.
     *
     * @param fieldId
     *            the field identifier
     * @param name
     *            the new field name
     * @param description
     *            the new field description
     * @param fieldType
     *            the textual field type, parsed against {@link VisionFieldType}
     * @return true if the field existed and was updated, false otherwise
     * @throws InvalidFieldTypeException
     *             if the field type cannot be parsed
     */
    public boolean updateExtractorField( int fieldId, String name, String description, String fieldType ) throws InvalidFieldTypeException
    {
        VisionFieldType type = parseFieldType( fieldType );
        VisionExtractorField field = VisionExtractorFieldHome.findByPrimaryKey( fieldId ).orElse( null );
        if ( field == null )
        {
            return false;
        }
        field.setFieldName( name );
        field.setFieldDescription( description );
        field.setFieldType( type );
        VisionExtractorFieldHome.update( field );
        return true;
    }

    /**
     * Removes an extractor field by its identifier.
     *
     * @param fieldId
     *            the field identifier
     */
    public void removeExtractorField( int fieldId )
    {
        VisionExtractorFieldHome.remove( fieldId );
    }

    /**
     * Parses a textual field type into its enum value.
     *
     * @param fieldType
     *            the textual field type
     * @return the matching {@link VisionFieldType}
     * @throws InvalidFieldTypeException
     *             if the value is null or does not match any field type
     */
    private VisionFieldType parseFieldType( String fieldType ) throws InvalidFieldTypeException
    {
        VisionFieldType parsed = fieldType != null ? VisionFieldType.fromString( fieldType ) : null;
        if ( parsed == null )
        {
            throw new InvalidFieldTypeException( "Invalid vision field type: " + fieldType );
        }
        return parsed;
    }
}
