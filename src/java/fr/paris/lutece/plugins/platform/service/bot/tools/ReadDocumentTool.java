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
package fr.paris.lutece.plugins.platform.service.bot.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocumentHome;
import fr.paris.lutece.plugins.platform.service.rag.ElasticsearchService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Tool that returns a slice of a document's text content by line range, so the LLM can page through large documents without overflowing its context. Reads from
 * the persisted full_content column when available; falls back to concatenating Elasticsearch segments otherwise.
 */
public class ReadDocumentTool extends AbstractDatasetTool
{
    private static final int DEFAULT_MAX_LINES = 200;
    private static final int HARD_MAX_LINES = 500;

    /**
     * Constructor
     *
     * @param accessibleDatasets
     *            The datasets the bot has access to
     */
    public ReadDocumentTool( List<Dataset> accessibleDatasets )
    {
        super( accessibleDatasets );
    }

    /**
     * Reads a line range of a document by its name within a dataset.
     *
     * @param datasetName
     *            The dataset name
     * @param documentName
     *            The document name (must match exactly)
     * @param startLine
     *            1-based start line (default 1)
     * @param maxLines
     *            Number of lines to return (default 200, max 500)
     * @return The document slice with metadata header or an explanatory error message
     */
    @Tool( "Read a line range of a document inside a dataset. Returns the requested lines plus a header with the document name, folder, and total line count. For large documents, page through by calling again with startLine=previousEnd+1." )
    public String readDocument( @P( "Name of the dataset" ) String datasetName, @P( "Exact name of the document to read" ) String documentName,
            @P( "1-based start line. Default 1." ) Integer startLine, @P( "Number of lines to return. Default 200, max 500." ) Integer maxLines )
    {
        Optional<Dataset> datasetOpt = findDatasetByName( datasetName );
        if ( datasetOpt.isEmpty( ) )
        {
            return datasetNotFoundMessage( datasetName );
        }
        Dataset dataset = datasetOpt.get( );

        Optional<DatasetDocument> docOpt = DatasetDocumentHome.findByNameAndDatasetId( documentName, dataset.getId( ) );
        if ( docOpt.isEmpty( ) )
        {
            return "Document '" + documentName + "' not found in dataset '" + dataset.getDatasetName( ) + "'.";
        }
        DatasetDocument doc = DatasetDocumentHome.findByPrimaryKey( docOpt.get( ).getId( ) ).orElse( docOpt.get( ) );
        String content = doc.getFullContent( );
        if ( content == null || content.isEmpty( ) )
        {
            content = reconstructFromSegments( dataset.getId( ), doc.getId( ) );
        }
        if ( content == null || content.isEmpty( ) )
        {
            return "Document '" + documentName + "' has no available text content yet (still being indexed?).";
        }

        String [ ] lines = content.split( "\n", -1 );
        int totalLines = lines.length;
        int resolvedStart = resolveStartLine( startLine, totalLines );
        int resolvedMax = resolveMaxLines( maxLines );
        int endLineExclusive = Math.min( totalLines, resolvedStart - 1 + resolvedMax );

        StringBuilder sb = new StringBuilder( );
        sb.append( "## " ).append( doc.getName( ) );
        if ( doc.getFolderId( ) != null )
        {
            String path = getResolver( dataset.getId( ) ).pathOf( doc.getFolderId( ) );
            sb.append( " (" ).append( path ).append( ")" );
        }
        sb.append( "\n\n" );
        if ( doc.getDescription( ) != null && !doc.getDescription( ).isEmpty( ) )
        {
            sb.append( "_" ).append( doc.getDescription( ) ).append( "_\n\n" );
        }
        sb.append( "Lines " ).append( resolvedStart ).append( "–" ).append( endLineExclusive ).append( " of " ).append( totalLines ).append( ":\n\n" );

        for ( int i = resolvedStart - 1; i < endLineExclusive; i++ )
        {
            sb.append( lines [i] ).append( "\n" );
        }

        if ( endLineExclusive < totalLines )
        {
            int remaining = totalLines - endLineExclusive;
            sb.append( "\n… " ).append( remaining ).append( " more line(s). " ).append( "Call readDocument again with startLine=" )
                    .append( endLineExclusive + 1 ).append( " to continue.\n" );
        }
        return sb.toString( );
    }

    /**
     * Clamps the requested start line to a valid 1-based index.
     *
     * @param requested
     *            The user-requested start line, possibly null or out of range
     * @param totalLines
     *            The document's total line count
     * @return A valid start line (1-based), never beyond totalLines
     */
    private int resolveStartLine( Integer requested, int totalLines )
    {
        if ( requested == null || requested < 1 )
        {
            return 1;
        }
        return Math.min( requested, totalLines );
    }

    /**
     * Clamps the requested maxLines to sane bounds.
     *
     * @param requested
     *            The user-requested max lines, possibly null or out of range
     * @return A valid max lines value
     */
    private int resolveMaxLines( Integer requested )
    {
        if ( requested == null || requested <= 0 )
        {
            return DEFAULT_MAX_LINES;
        }
        return Math.min( requested, HARD_MAX_LINES );
    }

    /**
     * Falls back to reading text segments from Elasticsearch and concatenating them in order
     *
     * @param datasetId
     *            The dataset id
     * @param documentId
     *            The document id
     * @return The reconstructed text or null
     */
    private String reconstructFromSegments( int datasetId, int documentId )
    {
        List<Map<String, Object>> segments = CDI.current( ).select( ElasticsearchService.class ).get( ).getDocumentSegments( datasetId, documentId );
        if ( segments == null || segments.isEmpty( ) )
        {
            return null;
        }
        StringBuilder sb = new StringBuilder( );
        for ( Map<String, Object> seg : segments )
        {
            Object text = seg.get( "text" );
            if ( text != null )
            {
                sb.append( text.toString( ) ).append( "\n" );
            }
        }
        return sb.toString( );
    }
}
