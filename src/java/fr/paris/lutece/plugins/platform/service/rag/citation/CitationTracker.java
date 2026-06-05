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
package fr.paris.lutece.plugins.platform.service.rag.citation;

import fr.paris.lutece.plugins.platform.service.rag.EmbeddingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.rag.content.Content;

/**
 * Service for tracking and extracting citations from LLM responses. This class identifies which sources were actually cited in the generated response.
 */
public class CitationTracker
{

    private static final Pattern CITATION_PATTERN = Pattern.compile( "\\[Source:?\\s*([^\\]]+)\\]" );
    private static final Pattern NUMBERED_CITATION_PATTERN = Pattern.compile( "\\[Source\\s+(\\d+)\\]" );

    /**
     * Extracts cited sources from an LLM response with their source numbers.
     *
     * @param response
     *            the LLM response containing citations
     * @param providedContents
     *            the list of contents that were provided to the LLM
     * @return map of source number to content that was cited
     */
    public Map<Integer, Content> extractCitedContentsWithNumbers( String response, List<Content> providedContents )
    {
        Set<Integer> citedIndices = extractCitedIndices( response );
        Map<Integer, Content> citedContentsMap = new LinkedHashMap<>( );

        for ( Integer index : citedIndices )
        {
            if ( index > 0 && index <= providedContents.size( ) )
            {
                citedContentsMap.put( index, providedContents.get( index - 1 ) );
            }
        }

        return citedContentsMap;
    }

    /**
     * Extracts cited sources from an LLM response.
     *
     * @param response
     *            the LLM response containing citations
     * @param providedContents
     *            the list of contents that were provided to the LLM
     * @return list of contents that were actually cited
     */
    public List<Content> extractCitedContents( String response, List<Content> providedContents )
    {
        Set<String> citedDocumentNames = extractCitedDocumentNames( response );
        Set<Integer> citedIndices = extractCitedIndices( response );

        Map<String, Content> nameToContent = new HashMap<>( );
        for ( int i = 0; i < providedContents.size( ); i++ )
        {
            Content content = providedContents.get( i );
            String docName = content.textSegment( ).metadata( ).getString( EmbeddingService.META_DOCUMENT_NAME );
            if ( docName != null )
            {
                nameToContent.put( docName, content );
            }
            nameToContent.put( String.valueOf( i + 1 ), content );
        }

        Set<Content> citedContentsSet = new HashSet<>( );

        for ( String docName : citedDocumentNames )
        {
            Content content = nameToContent.get( docName );
            if ( content != null )
            {
                citedContentsSet.add( content );
            }
        }

        for ( Integer index : citedIndices )
        {
            if ( index > 0 && index <= providedContents.size( ) )
            {
                citedContentsSet.add( providedContents.get( index - 1 ) );
            }
        }

        return new ArrayList<>( citedContentsSet );
    }

    /**
     * Extracts document names from citations in the response.
     *
     * @param response
     *            the LLM response
     * @return set of cited document names
     */
    private Set<String> extractCitedDocumentNames( String response )
    {
        Set<String> citedNames = new HashSet<>( );
        Matcher matcher = CITATION_PATTERN.matcher( response );

        while ( matcher.find( ) )
        {
            String citation = matcher.group( 1 ).trim( );
            if ( !citation.matches( "\\d+" ) )
            {
                citedNames.add( citation );
            }
        }

        return citedNames;
    }

    /**
     * Extracts source indices from numbered citations in the response.
     *
     * @param response
     *            the LLM response
     * @return set of cited source indices (1-based)
     */
    private Set<Integer> extractCitedIndices( String response )
    {
        Set<Integer> citedIndices = new HashSet<>( );

        Matcher matcher1 = NUMBERED_CITATION_PATTERN.matcher( response );
        while ( matcher1.find( ) )
        {
            try
            {
                citedIndices.add( Integer.valueOf( matcher1.group( 1 ) ) );
            }
            catch( NumberFormatException e )
            {
            }
        }

        Matcher matcher2 = CITATION_PATTERN.matcher( response );
        while ( matcher2.find( ) )
        {
            String citation = matcher2.group( 1 ).trim( );

            if ( citation.matches( "\\d+" ) )
            {
                try
                {
                    citedIndices.add( Integer.valueOf( citation ) );
                }
                catch( NumberFormatException e )
                {
                }
            }
            else if ( citation.contains( "," ) )
            {
                String [ ] parts = citation.split( "," );
                for ( String part : parts )
                {
                    part = part.trim( );
                    if ( part.startsWith( "Source" ) )
                    {
                        part = part.substring( 6 ).trim( );
                    }
                    if ( part.matches( "\\d+" ) )
                    {
                        try
                        {
                            citedIndices.add( Integer.valueOf( part ) );
                        }
                        catch( NumberFormatException e )
                        {
                        }
                    }
                }
            }
        }

        return citedIndices;
    }

}
