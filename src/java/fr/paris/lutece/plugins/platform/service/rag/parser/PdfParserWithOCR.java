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
package fr.paris.lutece.plugins.platform.service.rag.parser;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.plugins.platform.service.model.OcrService;
import fr.paris.lutece.portal.service.util.AppLogService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfParserWithOCR implements DocumentParser
{

    private static final String LOG_OCR_ERROR = "[AzurePdfParserWithOCR] Erreur OCR pour image ";
    private static final String LOG_IMAGE_EXTRACTION_ERROR = "[AzurePdfParserWithOCR] Impossible d'extraire l'image ";

    private static final String ERROR_PDF_PARSING = "Erreur lors du parsing du PDF avec OCR";

    private static final String METADATA_PDF_PAGES = "pdf_pages";
    private static final String METADATA_EXTRACTED_IMAGES_COUNT = "extracted_images_count";
    private static final String METADATA_HAS_OCR_CONTENT = "has_ocr_content";
    private static final String METADATA_OCR_ENABLED = "ocr_enabled";
    private static final String METADATA_TEXT_LENGTH = "text_length";
    private static final String METADATA_OCR_TEXT_LENGTH = "ocr_text_length";

    private static final String IMAGE_NODE_ID_FORMAT = "%s-image-%d";

    private final Provider llmProvider;
    private final boolean enableOCR;
    private final String executionId;
    private final String baseNodeId;
    private final OcrService ocrService;

    /**
     * Creates a PDF parser with OCR capabilities
     *
     * @param llmProvider
     *            The LLM provider for OCR
     * @param enableOCR
     *            Whether to enable OCR processing
     * @param executionId
     *            The execution ID for tracking
     * @param baseNodeId
     *            The base node ID for tracking
     */
    public PdfParserWithOCR( Provider llmProvider, boolean enableOCR, String executionId, String baseNodeId )
    {
        this.llmProvider = llmProvider;
        this.enableOCR = enableOCR;
        this.executionId = executionId;
        this.baseNodeId = baseNodeId;
        this.ocrService = new OcrService( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document parse( InputStream inputStream )
    {
        try ( PDDocument pdfDocument = PDDocument.load( inputStream ) )
        {
            StringBuilder fullText = new StringBuilder( );
            List<String> imageTexts = new ArrayList<>( );

            String textContent = extractTextFromPdf( pdfDocument );
            fullText.append( textContent );

            int textLength = textContent != null ? textContent.length( ) : 0;

            List<BufferedImage> images = extractImagesFromPdf( pdfDocument );
            processImagesWithOCR( images, fullText, imageTexts );

            Metadata metadata = createMetadata( pdfDocument, imageTexts, textLength );
            return Document.from( fullText.toString( ), metadata );

        }
        catch( IOException e )
        {
            throw new RuntimeException( ERROR_PDF_PARSING, e );
        }
    }

    /**
     * Extracts text content from PDF document
     *
     * @param pdfDocument
     *            The PDF document
     * @return Extracted text content
     * @throws IOException
     *             if text extraction fails
     */
    private String extractTextFromPdf( PDDocument pdfDocument ) throws IOException
    {
        PDFTextStripper stripper = new PDFTextStripper( );
        return stripper.getText( pdfDocument );
    }

    /**
     * Processes images with OCR and adds results to text collections
     *
     * @param images
     *            List of images to process
     * @param fullText
     *            StringBuilder to append OCR results
     * @param imageTexts
     *            List to collect OCR text results
     * @return Number of successfully processed images
     */
    private int processImagesWithOCR( List<BufferedImage> images, StringBuilder fullText, List<String> imageTexts )
    {
        int processedImages = 0;

        for ( int i = 0; i < images.size( ); i++ )
        {
            BufferedImage image = images.get( i );
            if ( !ocrService.isImageWorthProcessing( image ) )
            {
                continue;
            }

            try
            {
                BufferedImage optimizedImage = ocrService.optimizeImageForOCR( image );
                String imageNodeId = generateImageNodeId( i );
                String ocrText = ocrService.performOCR( optimizedImage, llmProvider, executionId, imageNodeId );

                if ( ocrText != null && !ocrText.trim( ).isEmpty( ) )
                {
                    fullText.append( ocrText );
                    imageTexts.add( ocrText );
                    processedImages++;
                }
            }
            catch( Exception e )
            {
                AppLogService.error( "{}{}: {}", LOG_OCR_ERROR, i, e.getMessage( ), e );
            }
        }

        return processedImages;
    }

    /**
     * Generates image node ID for tracking
     *
     * @param imageIndex
     *            The index of the image
     * @return Generated node ID or null if base node ID is not set
     */
    private String generateImageNodeId( int imageIndex )
    {
        return baseNodeId != null ? String.format( IMAGE_NODE_ID_FORMAT, baseNodeId, imageIndex ) : null;
    }

    /**
     * Creates metadata for the parsed document
     *
     * @param pdfDocument
     *            The PDF document
     * @param imageTexts
     *            List of OCR text results
     * @param textLength
     *            Length of extracted text
     * @return Document metadata
     */
    private Metadata createMetadata( PDDocument pdfDocument, List<String> imageTexts, int textLength )
    {
        Metadata metadata = new Metadata( );
        metadata.put( METADATA_PDF_PAGES, String.valueOf( pdfDocument.getNumberOfPages( ) ) );
        metadata.put( METADATA_EXTRACTED_IMAGES_COUNT, String.valueOf( imageTexts.size( ) ) );
        metadata.put( METADATA_HAS_OCR_CONTENT, String.valueOf( !imageTexts.isEmpty( ) ) );
        metadata.put( METADATA_OCR_ENABLED, String.valueOf( enableOCR ) );
        metadata.put( METADATA_TEXT_LENGTH, String.valueOf( textLength ) );

        if ( !imageTexts.isEmpty( ) )
        {
            int totalOcrChars = imageTexts.stream( ).mapToInt( String::length ).sum( );
            metadata.put( METADATA_OCR_TEXT_LENGTH, String.valueOf( totalOcrChars ) );
        }

        return metadata;
    }

    /**
     * Extracts images from PDF document
     *
     * @param document
     *            The PDF document
     * @return List of extracted images
     * @throws IOException
     *             if image extraction fails
     */
    private List<BufferedImage> extractImagesFromPdf( PDDocument document ) throws IOException
    {
        List<BufferedImage> images = new ArrayList<>( );

        for ( PDPage page : document.getPages( ) )
        {
            PDResources resources = page.getResources( );
            if ( resources == null )
            {
                continue;
            }

            for ( org.apache.pdfbox.cos.COSName name : resources.getXObjectNames( ) )
            {
                PDXObject xObject = resources.getXObject( name );
                if ( xObject instanceof PDImageXObject )
                {
                    try
                    {
                        PDImageXObject imageXObject = (PDImageXObject) xObject;
                        BufferedImage image = imageXObject.getImage( );
                        if ( image != null )
                        {
                            images.add( image );
                        }
                    }
                    catch( Exception e )
                    {
                        AppLogService.error( "{}{}: {}", LOG_IMAGE_EXTRACTION_ERROR, name.getName( ), e.getMessage( ), e );
                    }
                }
            }
        }

        return images;
    }
}
