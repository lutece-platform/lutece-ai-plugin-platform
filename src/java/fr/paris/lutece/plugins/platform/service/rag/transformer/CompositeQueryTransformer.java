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
package fr.paris.lutece.plugins.platform.service.rag.transformer;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import fr.paris.lutece.plugins.platform.business.observability.PlatformResourceNodeTraceStatus;
import fr.paris.lutece.plugins.platform.service.rag.QueryEnhancementMode;
import fr.paris.lutece.plugins.platform.service.observability.ObservabilityService;
import fr.paris.lutece.plugins.platform.service.observability.data.ObservabilityData;
import jakarta.enterprise.inject.spi.CDI;

/**
 * A composite query transformer that chains multiple query transformers based on the enhancement mode. It supports compression, expansion, and HyDE in various
 * combinations.
 */
public class CompositeQueryTransformer implements QueryTransformer
{

    private final QueryTransformer compressor;
    private final QueryTransformer expander;
    private final QueryTransformer hydeTransformer;
    private final QueryEnhancementMode enhancementMode;
    private final String executionId;
    private final int baseNodeOrder;
    private final ObservabilityService _observability = CDI.current( ).select( ObservabilityService.class ).get( );

    /**
     * Creates a new CompositeQueryTransformer with all transformers and enhancement mode.
     *
     * @param compressor
     *            the compression transformer
     * @param expander
     *            the expansion transformer (can be null)
     * @param hydeTransformer
     *            the HyDE transformer (can be null)
     * @param enhancementMode
     *            the query enhancement mode
     * @param executionId
     *            the execution ID for observability
     * @param baseNodeOrder
     *            the base node order for observability
     */
    public CompositeQueryTransformer( QueryTransformer compressor, QueryTransformer expander, QueryTransformer hydeTransformer,
            QueryEnhancementMode enhancementMode, String executionId, int baseNodeOrder )
    {
        if ( compressor == null )
        {
            throw new IllegalArgumentException( "Compressor cannot be null" );
        }
        this.compressor = compressor;
        this.expander = expander;
        this.hydeTransformer = hydeTransformer;
        this.enhancementMode = enhancementMode != null ? enhancementMode : QueryEnhancementMode.NONE;
        this.executionId = executionId;
        this.baseNodeOrder = baseNodeOrder;
    }

    /**
     * Transforms a query based on the enhancement mode.
     *
     * @param query
     *            the query to transform
     * @return the transformed queries
     */
    @Override
    public Collection<Query> transform( Query query )
    {
        String compositeNodeId = "query-composite-transformer";
        _observability.startNodeExecution( executionId, compositeNodeId, "Transformation composite des requêtes", baseNodeOrder,
                ObservabilityData.input( "Transformation composite", "Mode: " + enhancementMode ) );

        try
        {
            _observability.addNodeTrace( executionId, compositeNodeId,
                    ObservabilityData.trace( "QUERY_TRACE", "Requête originale", "\"" + query.text( ) + "\"" ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

            Collection<Query> compressedQueries = compressor.transform( query );
            if ( compressedQueries.isEmpty( ) )
            {
                _observability.completeNodeExecutionSuccess( executionId, compositeNodeId,
                        ObservabilityData.output( "Compression terminée", "Aucune requête après compression" ) );
                return compressedQueries;
            }

            Query compressedQuery = compressedQueries.iterator( ).next( );

            Set<Query> allQueries = new HashSet<>( );

            switch( enhancementMode )
            {
                case NONE:
                    allQueries.add( compressedQuery );
                    _observability.addNodeTrace( executionId, compositeNodeId,
                            ObservabilityData.trace( "MODE_INFO", "Mode NONE", "Pas d'amélioration, requête compressée uniquement" ),
                            PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                    break;

                case EXPANSION_ONLY:
                    if ( expander != null )
                    {
                        Collection<Query> expandedQueries = expander.transform( compressedQuery );
                        allQueries.addAll( expandedQueries );

                        _observability.addNodeTrace( executionId, compositeNodeId,
                                ObservabilityData.trace( "EXPANSION_SUMMARY", "Expansion de requête", expandedQueries.size( ) + " variantes générées" ),
                                PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

                        int i = 1;
                        for ( Query expandedQuery : expandedQueries )
                        {
                            _observability.addNodeTrace( executionId, compositeNodeId,
                                    ObservabilityData.trace( "QUERY_VARIANT", "Variante " + i++, "\"" + expandedQuery.text( ) + "\"" ),
                                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                        }
                    }
                    else
                    {
                        allQueries.add( compressedQuery );
                    }
                    break;

                case HYDE_ONLY:
                    allQueries.add( compressedQuery );
                    if ( hydeTransformer != null )
                    {
                        Collection<Query> hydeQueries = hydeTransformer.transform( compressedQuery );
                        allQueries.addAll( hydeQueries );

                        _observability.addNodeTrace( executionId, compositeNodeId,
                                ObservabilityData.trace( "HYDE_SUMMARY", "HyDE uniquement", hydeQueries.size( ) + " document(s) hypothétique(s) généré(s)" ),
                                PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

                        for ( Query hydeQuery : hydeQueries )
                        {
                            _observability.addNodeTrace( executionId, compositeNodeId,
                                    ObservabilityData.trace( "HYDE_DOCUMENT", "Document HyDE", "\"" + hydeQuery.text( ) + "\"" ),
                                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                        }
                    }
                    break;

                case EXPANSION_THEN_HYDE:
                    if ( expander != null )
                    {
                        Collection<Query> expandedQueries = expander.transform( compressedQuery );
                        allQueries.addAll( expandedQueries );
                        int hydeCount = 0;

                        int varianteIndex = 1;
                        for ( Query expandedQuery : expandedQueries )
                        {
                            _observability.addNodeTrace( executionId, compositeNodeId,
                                    ObservabilityData.trace( "QUERY_VARIANT", "Variante " + varianteIndex, "\"" + expandedQuery.text( ) + "\"" ),
                                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

                            if ( hydeTransformer != null )
                            {
                                Collection<Query> hydeQueries = hydeTransformer.transform( expandedQuery );
                                allQueries.addAll( hydeQueries );
                                hydeCount += hydeQueries.size( );

                                for ( Query hydeQuery : hydeQueries )
                                {
                                    _observability.addNodeTrace( executionId, compositeNodeId,
                                            ObservabilityData.trace( "HYDE_VARIANT", "HyDE pour variante " + varianteIndex, "\"" + hydeQuery.text( ) + "\"" ),
                                            PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                                }
                            }
                            varianteIndex++;
                        }

                        _observability.addNodeTrace( executionId, compositeNodeId,
                                ObservabilityData.trace( "EXPANSION_HYDE_SUMMARY", "Expansion puis HyDE - Résumé",
                                        expandedQueries.size( ) + " variantes + " + hydeCount + " documents HyDE" ),
                                PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                    }
                    else
                    {
                        allQueries.add( compressedQuery );
                    }
                    break;

                case EXPANSION_PLUS_HYDE_GLOBAL:
                    int expansionCount = 0;
                    if ( expander != null )
                    {
                        Collection<Query> expandedQueries = expander.transform( compressedQuery );
                        allQueries.addAll( expandedQueries );
                        expansionCount = expandedQueries.size( );

                        int i = 1;
                        for ( Query expandedQuery : expandedQueries )
                        {
                            _observability.addNodeTrace( executionId, compositeNodeId,
                                    ObservabilityData.trace( "QUERY_VARIANT", "Variante " + i++, "\"" + expandedQuery.text( ) + "\"" ),
                                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                        }
                    }
                    else
                    {
                        allQueries.add( compressedQuery );
                    }

                    int hydeGlobalCount = 0;
                    if ( hydeTransformer != null )
                    {
                        Collection<Query> hydeQueries = hydeTransformer.transform( compressedQuery );
                        allQueries.addAll( hydeQueries );
                        hydeGlobalCount = hydeQueries.size( );

                        for ( Query hydeQuery : hydeQueries )
                        {
                            _observability.addNodeTrace( executionId, compositeNodeId,
                                    ObservabilityData.trace( "HYDE_GLOBAL", "HyDE global (sur requête compressée)", "\"" + hydeQuery.text( ) + "\"" ),
                                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                        }
                    }

                    _observability.addNodeTrace( executionId, compositeNodeId,
                            ObservabilityData.trace( "EXPANSION_HYDE_GLOBAL_SUMMARY", "Expansion + HyDE global - Résumé",
                                    expansionCount + " variantes + " + hydeGlobalCount + " document(s) HyDE global" ),
                            PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );
                    break;
            }

            String finalMessage = "Total: " + allQueries.size( ) + " requête(s)";

            _observability.addNodeTrace( executionId, compositeNodeId, ObservabilityData.trace( "FINAL_RESULT", "Résultat final", finalMessage ),
                    PlatformResourceNodeTraceStatus.INFO.getValue( ), BigDecimal.ZERO );

            _observability.completeNodeExecutionSuccess( executionId, compositeNodeId, ObservabilityData.output( "Transformation réussie", finalMessage ) );

            return allQueries;

        }
        catch( Exception e )
        {
            _observability.completeNodeExecutionError( executionId, compositeNodeId, "Erreur lors de la transformation: " + e.getMessage( ), null );
            throw e;
        }
    }

}
