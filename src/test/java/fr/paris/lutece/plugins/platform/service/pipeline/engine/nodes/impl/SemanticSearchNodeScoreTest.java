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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.test.AbstractPlatformDbTest;

/**
 * Score contract of {@link SemanticSearchNode#toResults}: the score exposed to downstream nodes (thresholds, ranking, RRF fusion) must be the REAL similarity
 * score computed by the embedding store and carried in {@link ContentMetadata#SCORE} — never a value fabricated from the result position. No Elasticsearch
 * involved; the container is only needed because the node caches CDI lookups in static fields.
 */
public class SemanticSearchNodeScoreTest extends AbstractPlatformDbTest
{
    /**
     * The real retriever score is restituted as-is, regardless of result position.
     */
    @Test
    public void testRealRetrieverScoreIsRestituted( )
    {
        Dataset dataset = dataset( );
        List<Content> contents = List.of( content( "first chunk", 0.42 ), content( "second chunk", 0.91 ) );

        List<Map<String, Object>> results = SemanticSearchNode.toResults( contents, dataset );

        assertEquals( 0.42, (double) results.get( 0 ).get( "score" ), 1e-9, "the first result must carry the retriever's real score, not a positional one" );
        assertEquals( 0.91, (double) results.get( 1 ).get( "score" ), 1e-9, "the second result must carry the retriever's real score, not a positional one" );
    }

    /**
     * A content without score metadata falls back to 0.0 instead of failing or inventing a value.
     */
    @Test
    public void testMissingScoreFallsBackToZero( )
    {
        Dataset dataset = dataset( );
        List<Content> contents = List.of( Content.from( TextSegment.from( "no score" ) ) );

        List<Map<String, Object>> results = SemanticSearchNode.toResults( contents, dataset );

        assertEquals( 0.0, (double) results.get( 0 ).get( "score" ), 1e-9 );
    }

    /**
     * Dataset id and name are injected into each result's metadata.
     */
    @Test
    public void testDatasetMetadataInjected( )
    {
        Dataset dataset = dataset( );
        List<Map<String, Object>> results = SemanticSearchNode.toResults( List.of( content( "chunk", 0.5 ) ), dataset );

        @SuppressWarnings( "unchecked" )
        Map<String, Object> metadata = (Map<String, Object>) results.get( 0 ).get( "metadata" );
        assertEquals( 7, metadata.get( "dataset_id" ) );
        assertEquals( "Score DS", metadata.get( "dataset_name" ) );
    }

    /**
     * Builds a content carrying a retriever score, as {@code EmbeddingStoreContentRetriever} does.
     *
     * @param strText
     *            the segment text
     * @param dScore
     *            the similarity score
     * @return the content
     */
    private Content content( String strText, double dScore )
    {
        return Content.from( TextSegment.from( strText ), Map.of( ContentMetadata.SCORE, dScore ) );
    }

    /**
     * Builds an in-memory dataset.
     *
     * @return the dataset
     */
    private Dataset dataset( )
    {
        Dataset dataset = new Dataset( );
        dataset.setId( 7 );
        dataset.setDatasetName( "Score DS" );
        return dataset;
    }
}
