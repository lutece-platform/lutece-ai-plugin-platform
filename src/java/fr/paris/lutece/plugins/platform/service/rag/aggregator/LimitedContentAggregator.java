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
package fr.paris.lutece.plugins.platform.service.rag.aggregator;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.query.Query;

/**
 * A content aggregator that extends DefaultContentAggregator to limit the number of results. This aggregator applies Reciprocal Rank Fusion (RRF) and then
 * limits the output to a maximum number of results.
 */
public class LimitedContentAggregator extends DefaultContentAggregator
{

    private final int maxResults;

    /**
     * Creates a new LimitedContentAggregator with the specified maximum number of results.
     *
     * @param maxResults
     *            the maximum number of content items to return after aggregation
     */
    public LimitedContentAggregator( int maxResults )
    {
        if ( maxResults <= 0 )
        {
            throw new IllegalArgumentException( "maxResults must be greater than 0" );
        }
        this.maxResults = maxResults;
    }

    /**
     * Aggregates content from multiple queries and retrievers, then limits the results.
     *
     * @param queryToContents
     *            a map from queries to their retrieved contents
     * @return a list of aggregated contents limited to maxResults
     */
    @Override
    public List<Content> aggregate( Map<Query, Collection<List<Content>>> queryToContents )
    {
        List<Content> aggregatedContents = super.aggregate( queryToContents );

        return aggregatedContents.stream( ).limit( maxResults ).toList( );
    }
}
