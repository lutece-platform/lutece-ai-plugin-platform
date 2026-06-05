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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.business.provider.ProviderHome;
import fr.paris.lutece.plugins.platform.service.pipeline.PipelineFileService;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Abstract base for nodes that process files with a provider. Handles provider selection, file list resolution, iteration and output structure.
 */
public abstract class AbstractFileProcessingNode extends AbstractPipelineNode
{

    private static final String PROVIDER_NAME = "provider";
    private static final String FILES_NAME = "files";

    private static final String PROVIDER_TITLE = "Fournisseur";
    private static final String PROVIDER_DESCRIPTION = "Le fournisseur pour le traitement";
    private static final String FILES_TITLE = "Fichiers";
    private static final String FILES_DESCRIPTION = "Les fichiers à traiter";

    private static final String OUTPUT_EXTRACTIONS_KEY = "extractions";
    private static final String OUTPUT_EXTRACTIONS_DESCRIPTION = "Liste des extractions {fileName, extractedText}";
    private static final String OUTPUT_FILE_COUNT_KEY = "file_count";
    private static final String OUTPUT_FILE_COUNT_DESCRIPTION = "Nombre de fichiers traités";
    protected static final String EXTRACTED_TEXT_KEY = "extractedText";
    private static final String DEFAULT_INPUT_PORT_DESCRIPTION = "Entrée par défaut";
    private static final String DEFAULT_OUTPUT_PORT_DESCRIPTION = "Sortie par défaut";

    private static final String DEFAULT_PROVIDER_OPTION_VALUE = "";
    private static final String DEFAULT_PROVIDER_OPTION_LABEL = "Sélectionner un fournisseur";
    private static final String DEFAULT_PROVIDER_OPTION_DESC = "Veuillez sélectionner un fournisseur configuré";
    private static final String PROVIDER_DESC_PREFIX = "Fournisseur ";

    private static final String ERROR_PROVIDER_INVALID = "Invalid provider: %s";
    private static final String ERROR_PROVIDER_NOT_FOUND = "Provider with ID %s not found";
    private static final String ERROR_FILES_EMPTY = "No file provided for processing";
    private static final String ERROR_PROCESSING = "Error while processing file %s";
    private static final String SUCCESS_PROCESSING = "Processing succeeded for node %s: %d file(s) processed";

    /**
     * Constructor.
     *
     * @param nodeType
     *            the node type constant
     */
    protected AbstractFileProcessingNode( String nodeType )
    {
        super( nodeType );
    }

    /**
     * Processes a single file and returns the extracted text.
     *
     * @param bytes
     *            the file content
     * @param fileName
     *            the file name
     * @param provider
     *            the provider
     * @param executionId
     *            the execution ID for observability
     * @param nodeId
     *            the node ID for observability
     * @return the extracted text
     */
    protected abstract String processFile( byte [ ] bytes, String fileName, Provider provider, String executionId, String nodeId );

    /**
     * Returns the accepted content types for files.
     *
     * @return array of MIME types
     */
    protected abstract String [ ] getAcceptedContentTypes( );

    /**
     * Returns the provider type to filter available providers.
     *
     * @return the provider type constant from ProviderTypeConstants
     */
    protected abstract String getProviderType( );

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        List<PipelineVariable> variables = new ArrayList<>( );
        variables.add( createProviderVariable( ) );
        variables.add( createFilesVariable( ) );
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
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( OUTPUT_EXTRACTIONS_KEY, OUTPUT_EXTRACTIONS_DESCRIPTION );
        outputs.put( OUTPUT_FILE_COUNT_KEY, OUTPUT_FILE_COUNT_DESCRIPTION );
        return outputs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        Object providerValue = resolvedData.get( PROVIDER_NAME );
        if ( providerValue == null || providerValue.toString( ).trim( ).isEmpty( ) )
        {
            throw new IllegalArgumentException( "The provider is required" );
        }
        Provider provider = resolveProvider( providerValue.toString( ) );

        List<Map<String, Object>> fileList = resolveFileList( resolvedData );

        List<Map<String, String>> extractions = new ArrayList<>( );
        String executionId = context.getObservabilityId( );
        PipelineFileService fileService = CDI.current( ).select( PipelineFileService.class ).get( );

        for ( int i = 0; i < fileList.size( ); i++ )
        {
            Map<String, Object> file = fileList.get( i );
            String fileName = (String) file.get( PipelineFileService.FILE_NAME_KEY );
            String fileKey = (String) file.get( PipelineFileService.FILE_KEY );
            byte [ ] bytes = fileService.getFileBytes( fileKey );
            if ( bytes == null )
            {
                throw new RuntimeException( "Unable to load file: " + fileName + " (fileKey=" + fileKey + ")" );
            }

            String nodeId = config.getId( ) + "_file_" + i;

            try
            {
                String extractedText = processFile( bytes, fileName, provider, executionId, nodeId );

                Map<String, String> extraction = new HashMap<>( );
                extraction.put( PipelineFileService.FILE_NAME_KEY, fileName );
                extraction.put( EXTRACTED_TEXT_KEY, extractedText );
                extractions.add( extraction );
            }
            catch( Exception e )
            {
                AppLogService.error( String.format( ERROR_PROCESSING, fileName ), e );
                throw new RuntimeException( String.format( ERROR_PROCESSING, fileName ), e );
            }
        }

        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( OUTPUT_EXTRACTIONS_KEY, extractions );
        outputData.put( OUTPUT_FILE_COUNT_KEY, extractions.size( ) );
        prepareOutputData( context, config, outputData );

        AppLogService.info( String.format( SUCCESS_PROCESSING, config.getId( ), extractions.size( ) ) );

        return context;
    }

    /**
     * Resolves the file list from the resolved node data.
     *
     * @param resolvedData
     *            the resolved node data
     * @return the list of file objects
     */
    @SuppressWarnings( "unchecked" )
    private List<Map<String, Object>> resolveFileList( Map<String, Object> resolvedData )
    {
        Object filesValue = resolvedData.get( FILES_NAME );

        if ( filesValue == null )
        {
            throw new RuntimeException( ERROR_FILES_EMPTY );
        }

        List<Map<String, Object>> fileList = new ArrayList<>( );

        if ( filesValue instanceof List )
        {
            for ( Object item : (List<?>) filesValue )
            {
                if ( item instanceof Map )
                {
                    fileList.add( (Map<String, Object>) item );
                }
            }
        }
        else if ( filesValue instanceof Map )
        {
            fileList.add( (Map<String, Object>) filesValue );
        }

        if ( fileList.isEmpty( ) )
        {
            throw new RuntimeException( ERROR_FILES_EMPTY );
        }

        return fileList;
    }

    /**
     * Resolves a provider from a string ID.
     *
     * @param providerIdStr
     *            the provider ID as string
     * @return the Provider instance
     */
    private Provider resolveProvider( String providerIdStr )
    {
        if ( providerIdStr == null || providerIdStr.trim( ).isEmpty( ) )
        {
            throw new RuntimeException( String.format( ERROR_PROVIDER_INVALID, providerIdStr ) );
        }

        int providerId;
        try
        {
            providerId = Integer.parseInt( providerIdStr );
        }
        catch( NumberFormatException e )
        {
            throw new RuntimeException( String.format( ERROR_PROVIDER_INVALID, providerIdStr ) );
        }

        return ProviderHome.findByPrimaryKey( providerId ).orElseThrow( ( ) -> new RuntimeException( String.format( ERROR_PROVIDER_NOT_FOUND, providerId ) ) );
    }

    /**
     * Creates the provider variable with dynamically loaded options.
     *
     * @return the provider PipelineVariable
     */
    private PipelineVariable createProviderVariable( )
    {
        List<PipelineVariable.EnumOption> options = new ArrayList<>( );

        try
        {
            List<Provider> providers = ProviderHome.getProvidersByType( getProviderType( ) );
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
                            : PROVIDER_DESC_PREFIX + provider.getProviderType( );
                    options.add( new PipelineVariable.EnumOption( value, label, description ) );
                }
            }
        }
        catch( Exception e )
        {
            AppLogService.info( "Unable to load providers: {}", e.getMessage( ) );
            options.add( new PipelineVariable.EnumOption( DEFAULT_PROVIDER_OPTION_VALUE, DEFAULT_PROVIDER_OPTION_LABEL, DEFAULT_PROVIDER_OPTION_DESC ) );
        }

        return new PipelineVariable.Builder( PROVIDER_NAME ).type( PipelineVariable.VariableType.ENUM ).required( true ).title( PROVIDER_TITLE )
                .description( PROVIDER_DESCRIPTION ).options( options.toArray( new PipelineVariable.EnumOption [ 0] ) )
                .defaultValue( options.isEmpty( ) ? DEFAULT_PROVIDER_OPTION_VALUE : options.get( 0 ).getValue( ) ).build( );
    }

    /**
     * Creates the files variable with subclass-defined content types.
     *
     * @return the files PipelineVariable
     */
    private PipelineVariable createFilesVariable( )
    {
        return new PipelineVariable.Builder( FILES_NAME ).type( PipelineVariable.VariableType.FILE ).required( true ).multiple( true ).title( FILES_TITLE )
                .description( FILES_DESCRIPTION ).acceptedContentTypes( getAcceptedContentTypes( ) ).build( );
    }
}
