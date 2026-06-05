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
package fr.paris.lutece.plugins.platform.service.pipeline.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.IPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Registry that discovers and instantiates pipeline node implementations annotated with {@code @PipelineNodeType}. Auto-scans the default package on
 * initialization and caches node instances for reuse.
 */
@ApplicationScoped
@Named( "platform.pipelineNodeRegistry" )
public class PipelineNodeRegistry
{

    private static final String LOG_DUPLICATE_NODE = "Duplicate node type found: ";
    private static final String LOG_NODE_REGISTERED = "Node type registered: ";
    private static final String LOG_NODE_ARROW = " -> ";
    private static final String LOG_VARIABLES_COUNT = " variables";
    private static final String LOG_NAME_PREFIX = ", name: ";
    private static final String LOG_DESCRIPTION_PREFIX = ", description: ";
    private static final String LOG_NO_ANNOTATION = "Node class %s has no @PipelineNodeType annotation";
    private static final String LOG_REGISTRY_INITIALIZED = "Node registry initialized with %d node types";

    private static final String KEY_INPUT_PORTS = "inputPorts";
    private static final String KEY_OUTPUT_PORTS = "outputPorts";
    private static final String KEY_OUTPUT_KEYS = "outputKeys";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_VARIABLES = "variables";
    private static final String KEY_TYPE = "type";
    private static final String KEY_REQUIRED = "required";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DEFAULT_VALUE = "defaultValue";
    private static final String KEY_RANGE = "range";
    private static final String KEY_MIN = "min";
    private static final String KEY_MAX = "max";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_VALUE = "value";
    private static final String KEY_LABEL = "label";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_PLACEHOLDER = "placeholder";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_ITEM_SCHEMA = "itemSchema";
    private static final String KEY_FIELDS = "fields";
    private static final String KEY_IS_TEXTAREA = "isTextarea";
    private static final String KEY_ROWS = "rows";
    private static final String KEY_MULTIPLE = "multiple";
    private static final String KEY_ACCEPTED_CONTENT_TYPES = "acceptedContentTypes";

    private final Map<String, IPipelineNode> _nodeInstances = new ConcurrentHashMap<>( );
    private final Map<String, NodeTypeInfo> _nodeTypeInfoCache = new ConcurrentHashMap<>( );

    @Inject
    private Instance<IPipelineNode> _discoveredNodes;

    public static class NodeTypeInfo
    {
        private final String type;
        private final String name;
        private final String description;

        /**
         * Builds a node type information holder.
         *
         * @param type
         *            the node type identifier
         * @param name
         *            the display name
         * @param description
         *            the description
         */
        public NodeTypeInfo( String type, String name, String description )
        {
            this.type = type;
            this.name = name;
            this.description = description;
        }

        /**
         * Returns the type
         *
         * @return the type
         */
        public String getType( )
        {
            return type;
        }

        /**
         * Returns the display name, falling back to the type when the name is empty
         *
         * @return the display name
         */
        public String getName( )
        {
            return name.isEmpty( ) ? type : name;
        }

        /**
         * Returns the description
         *
         * @return the description
         */
        public String getDescription( )
        {
            return description;
        }
    }

    /**
     * Registers every {@link IPipelineNode} bean discovered by CDI. Each implementation must carry {@link PipelineNodeType} — duplicates are rejected.
     */
    @PostConstruct
    void init( )
    {
        int count = 0;
        for ( IPipelineNode node : _discoveredNodes )
        {
            PipelineNodeType annotation = node.getClass( ).getAnnotation( PipelineNodeType.class );
            if ( annotation == null )
            {
                AppLogService.error( String.format( LOG_NO_ANNOTATION, node.getClass( ).getName( ) ) );
                continue;
            }
            count += registerNode( node, annotation );
        }
        AppLogService.info( String.format( LOG_REGISTRY_INITIALIZED, count ) );
    }

    /**
     * Registers a single discovered node under the name carried by its annotation.
     *
     * @param node
     *            the node instance produced by CDI
     * @param annotation
     *            the type metadata annotation
     * @return 1 if registered, 0 if rejected as a duplicate
     */
    private int registerNode( IPipelineNode node, PipelineNodeType annotation )
    {
        String typeName = annotation.value( );
        if ( _nodeInstances.containsKey( typeName ) )
        {
            AppLogService.error( "{}{}", LOG_DUPLICATE_NODE, typeName );
            return 0;
        }

        _nodeInstances.put( typeName, node );
        _nodeTypeInfoCache.put( typeName, new NodeTypeInfo( typeName, annotation.name( ), annotation.description( ) ) );

        logNodeRegistration( typeName, node.getClass( ), node.getConfigurableVariables( ), annotation.name( ), annotation.description( ) );
        return 1;
    }

    /**
     * Log the registration of a node type
     *
     * @param typeName
     *            the type name
     * @param nodeClass
     *            the node class
     * @param variables
     *            the list of variables
     * @param displayName
     *            the display name
     * @param description
     *            the description
     */
    private void logNodeRegistration( String typeName, Class<? extends IPipelineNode> nodeClass, List<PipelineVariable> variables, String displayName,
            String description )
    {
        StringBuilder logMessage = new StringBuilder( LOG_NODE_REGISTERED ).append( typeName ).append( LOG_NODE_ARROW ).append( nodeClass.getName( ) )
                .append( " (" ).append( variables.size( ) ).append( LOG_VARIABLES_COUNT ).append( ")" );

        if ( !displayName.isEmpty( ) )
        {
            logMessage.append( LOG_NAME_PREFIX ).append( displayName );
        }
        if ( !description.isEmpty( ) )
        {
            logMessage.append( LOG_DESCRIPTION_PREFIX ).append( description );
        }

        AppLogService.info( logMessage.toString( ) );
    }

    /**
     * Get a node instance by type name
     *
     * @param typeName
     *            the type name
     * @return optional containing the node instance if found
     */
    public Optional<IPipelineNode> getNodeInstance( String typeName )
    {
        return Optional.ofNullable( _nodeInstances.get( typeName ) );
    }

    /**
     * Get node type information
     *
     * @param nodeType
     *            the node type
     * @return optional containing the node type info if found
     */
    public Optional<NodeTypeInfo> getNodeTypeInfo( String nodeType )
    {
        return Optional.ofNullable( _nodeTypeInfoCache.get( nodeType ) );
    }

    /**
     * Get information about all available nodes
     *
     * @return map containing node information
     */
    public Map<String, Object> getAvailableNodesInfo( )
    {
        Map<String, Object> result = new HashMap<>( );

        for ( String nodeType : _nodeInstances.keySet( ) )
        {
            Map<String, Object> nodeInfo = buildNodeInfo( nodeType );
            if ( !nodeInfo.isEmpty( ) )
            {
                result.put( nodeType, nodeInfo );
            }
        }

        return result;
    }

    /**
     * Build node information for a specific node type
     *
     * @param nodeType
     *            the node type
     * @return map containing node information
     */
    private Map<String, Object> buildNodeInfo( String nodeType )
    {
        Map<String, Object> nodeInfo = new HashMap<>( );
        IPipelineNode node = getNodeInstance( nodeType ).orElse( null );

        if ( node == null )
        {
            return nodeInfo;
        }

        addBasicNodeInfo( nodeInfo, node );
        addNodeTypeInfo( nodeInfo, nodeType );
        addVariablesInfo( nodeInfo, node );

        return nodeInfo;
    }

    /**
     * Add basic node information to the info map
     *
     * @param nodeInfo
     *            the info map to populate
     * @param node
     *            the node instance
     */
    private void addBasicNodeInfo( Map<String, Object> nodeInfo, IPipelineNode node )
    {
        nodeInfo.put( KEY_INPUT_PORTS, node.getInputPorts( ) );
        nodeInfo.put( KEY_OUTPUT_PORTS, node.getOutputPorts( ) );
        nodeInfo.put( KEY_OUTPUT_KEYS, node.getOutputKeys( ) );
    }

    /**
     * Add node type information to the info map
     *
     * @param nodeInfo
     *            the info map to populate
     * @param nodeType
     *            the node type
     */
    private void addNodeTypeInfo( Map<String, Object> nodeInfo, String nodeType )
    {
        NodeTypeInfo typeInfo = _nodeTypeInfoCache.get( nodeType );
        if ( typeInfo != null )
        {
            if ( !typeInfo.getName( ).isEmpty( ) )
            {
                nodeInfo.put( KEY_NAME, typeInfo.getName( ) );
            }
            if ( !typeInfo.getDescription( ).isEmpty( ) )
            {
                nodeInfo.put( KEY_DESCRIPTION, typeInfo.getDescription( ) );
            }
        }
    }

    /**
     * Add variables information to the info map
     *
     * @param nodeInfo
     *            the info map to populate
     * @param node
     *            the node instance
     */
    private void addVariablesInfo( Map<String, Object> nodeInfo, IPipelineNode node )
    {
        Map<String, Object> variablesInfo = new LinkedHashMap<>( );
        List<PipelineVariable> variables = node.getConfigurableVariables( );

        for ( PipelineVariable var : variables )
        {
            variablesInfo.put( var.getName( ), buildVariableInfo( var ) );
        }

        nodeInfo.put( KEY_VARIABLES, variablesInfo );
    }

    /**
     * Build variable information for a specific variable
     *
     * @param var
     *            the pipeline variable
     * @return map containing variable information
     */
    private Map<String, Object> buildVariableInfo( PipelineVariable var )
    {
        Map<String, Object> varInfo = new HashMap<>( );

        addBasicVariableInfo( varInfo, var );
        addVariableRangeInfo( varInfo, var );
        addVariableOptionsInfo( varInfo, var );
        addVariableStringInfo( varInfo, var );
        addVariableFileInfo( varInfo, var );
        addVariableLengthInfo( varInfo, var );
        addVariableSchemaInfo( varInfo, var );

        return varInfo;
    }

    /**
     * Add basic variable information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addBasicVariableInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        varInfo.put( KEY_TYPE, var.getType( ).name( ) );
        varInfo.put( KEY_REQUIRED, var.isRequired( ) );
        varInfo.put( KEY_DESCRIPTION, var.getDescription( ) );
        varInfo.put( KEY_TITLE, var.getTitle( ) );

        if ( var.hasDefaultValue( ) )
        {
            varInfo.put( KEY_DEFAULT_VALUE, var.getDefaultValue( ) );
        }
        if ( var.getPattern( ) != null )
        {
            varInfo.put( KEY_PATTERN, var.getPattern( ) );
        }
        if ( var.getPlaceholder( ) != null )
        {
            varInfo.put( KEY_PLACEHOLDER, var.getPlaceholder( ) );
        }
    }

    /**
     * Add variable range information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableRangeInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( var.getMinValue( ) != null || var.getMaxValue( ) != null )
        {
            Map<String, Object> range = new HashMap<>( );
            if ( var.getMinValue( ) != null )
            {
                range.put( KEY_MIN, var.getMinValue( ) );
            }
            if ( var.getMaxValue( ) != null )
            {
                range.put( KEY_MAX, var.getMaxValue( ) );
            }
            varInfo.put( KEY_RANGE, range );
        }
    }

    /**
     * Add variable options information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableOptionsInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( !var.getEnumOptions( ).isEmpty( ) )
        {
            List<Map<String, String>> options = new ArrayList<>( );
            for ( PipelineVariable.EnumOption option : var.getEnumOptions( ) )
            {
                Map<String, String> optionInfo = new HashMap<>( );
                optionInfo.put( KEY_VALUE, option.getValue( ) );
                optionInfo.put( KEY_LABEL, option.getLabel( ) );
                if ( option.getDescription( ) != null )
                {
                    optionInfo.put( KEY_DESCRIPTION, option.getDescription( ) );
                }
                options.add( optionInfo );
            }
            varInfo.put( KEY_OPTIONS, options );
        }
    }

    /**
     * Add string-specific variable information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableStringInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( var.getType( ) == PipelineVariable.VariableType.STRING )
        {
            varInfo.put( KEY_IS_TEXTAREA, var.isTextarea( ) );
            if ( var.getRows( ) != null )
            {
                varInfo.put( KEY_ROWS, var.getRows( ) );
            }
        }
    }

    /**
     * Add file-specific variable information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableFileInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( var.getType( ) == PipelineVariable.VariableType.FILE )
        {
            varInfo.put( KEY_MULTIPLE, var.isMultiple( ) );
            if ( !var.getAcceptedContentTypes( ).isEmpty( ) )
            {
                varInfo.put( KEY_ACCEPTED_CONTENT_TYPES, var.getAcceptedContentTypes( ) );
            }
        }
    }

    /**
     * Add variable length information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableLengthInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( var.getMinLength( ) != null || var.getMaxLength( ) != null )
        {
            Map<String, Object> length = new HashMap<>( );
            if ( var.getMinLength( ) != null )
            {
                length.put( KEY_MIN, var.getMinLength( ) );
            }
            if ( var.getMaxLength( ) != null )
            {
                length.put( KEY_MAX, var.getMaxLength( ) );
            }
            varInfo.put( KEY_LENGTH, length );
        }
    }

    /**
     * Add variable schema information to the variable info map
     *
     * @param varInfo
     *            the variable info map to populate
     * @param var
     *            the pipeline variable
     */
    private void addVariableSchemaInfo( Map<String, Object> varInfo, PipelineVariable var )
    {
        if ( var.getType( ) == PipelineVariable.VariableType.ARRAY && var.getObjectItemSchema( ) != null )
        {
            PipelineVariable itemSchema = var.getObjectItemSchema( );
            Map<String, Object> itemSchemaInfo = buildItemSchemaInfo( itemSchema );
            varInfo.put( KEY_ITEM_SCHEMA, itemSchemaInfo );
        }
    }

    /**
     * Build item schema information
     *
     * @param itemSchema
     *            the item schema variable
     * @return map containing item schema information
     */
    private Map<String, Object> buildItemSchemaInfo( PipelineVariable itemSchema )
    {
        Map<String, Object> itemSchemaInfo = new HashMap<>( );
        itemSchemaInfo.put( KEY_TYPE, itemSchema.getType( ).name( ) );
        itemSchemaInfo.put( KEY_DESCRIPTION, itemSchema.getDescription( ) );
        itemSchemaInfo.put( KEY_REQUIRED, itemSchema.isRequired( ) );
        itemSchemaInfo.put( KEY_TITLE, itemSchema.getTitle( ) );

        addVariableOptionsInfo( itemSchemaInfo, itemSchema );
        addObjectFieldsInfo( itemSchemaInfo, itemSchema );

        return itemSchemaInfo;
    }

    /**
     * Add object fields information to the item schema info map
     *
     * @param itemSchemaInfo
     *            the item schema info map to populate
     * @param itemSchema
     *            the item schema variable
     */
    private void addObjectFieldsInfo( Map<String, Object> itemSchemaInfo, PipelineVariable itemSchema )
    {
        if ( itemSchema.getType( ) == PipelineVariable.VariableType.OBJECT && itemSchema.getObjectFields( ) != null
                && !itemSchema.getObjectFields( ).isEmpty( ) )
        {

            List<Map<String, Object>> fields = new ArrayList<>( );
            for ( Map.Entry<String, PipelineVariable> entry : itemSchema.getObjectFields( ).entrySet( ) )
            {
                PipelineVariable fieldVar = entry.getValue( );
                Map<String, Object> fieldInfo = buildFieldInfo( fieldVar );
                fields.add( fieldInfo );
            }
            itemSchemaInfo.put( KEY_FIELDS, fields );
        }
    }

    /**
     * Build field information for an object field
     *
     * @param fieldVar
     *            the field variable
     * @return map containing field information
     */
    private Map<String, Object> buildFieldInfo( PipelineVariable fieldVar )
    {
        Map<String, Object> fieldInfo = new HashMap<>( );
        fieldInfo.put( KEY_NAME, fieldVar.getName( ) );
        fieldInfo.put( KEY_TYPE, fieldVar.getType( ).name( ) );
        fieldInfo.put( KEY_DESCRIPTION, fieldVar.getDescription( ) );
        fieldInfo.put( KEY_REQUIRED, fieldVar.isRequired( ) );
        fieldInfo.put( KEY_TITLE, fieldVar.getTitle( ) );
        return fieldInfo;
    }

}
