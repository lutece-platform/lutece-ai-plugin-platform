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
package fr.paris.lutece.plugins.platform.service.model;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import fr.paris.lutece.plugins.platform.business.provider.Provider;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class OcrService
{

    private static final String OCR_SYSTEM_PROMPT = "Tu es un extracteur de texte. Tu retournes UNIQUEMENT le texte brut visible dans l'image, rien d'autre.\n"
            + "Règles strictes :\n" + "- Si l'image ne contient aucun texte, retourne une chaîne vide.\n"
            + "- Ne jamais ajouter de commentaire, explication ou interprétation.\n" + "- Préserver la structure (tableaux, listes, paragraphes).\n"
            + "- Pour les schémas/diagrammes, extraire uniquement les étiquettes et textes visibles.\n"
            + "- Ignorer les éléments décoratifs sans valeur informative.\n";

    private static final String OCR_USER_PROMPT = "Extrais tout le texte visible dans cette image. Retourne uniquement le texte, rien d'autre.";

    private static final String LOG_OCR_ERROR = "[AzureOcrService] OCR error for image ";

    private static final String ERROR_NULL_IMAGE = "Image cannot be null";
    private static final String ERROR_NULL_PROVIDER = "LLM provider cannot be null";
    private static final String ERROR_NULL_EXECUTION_NODE_ID = "Execution ID and node ID cannot be null";
    private static final String ERROR_OCR_PROCESSING = "OCR error: ";

    private static final String IMAGE_FORMAT_JPEG = "jpeg";
    private static final String DATA_IMAGE_JPEG_BASE64_PREFIX = "data:image/jpeg;base64,";
    private static final String IMAGE_MIME_TYPE_JPEG = "image/jpeg";

    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_PIXELS = 5000;
    private static final int MAX_DIMENSION = 2048;

    /**
     * Performs OCR on a BufferedImage using an LLM provider
     *
     * @param image
     *            The image to process
     * @param llmProvider
     *            The LLM provider to use
     * @param executionId
     *            The execution ID for tracking
     * @param nodeId
     *            The node ID for tracking
     * @return The extracted text from the image
     * @throws Exception
     *             if OCR processing fails
     */
    public String performOCR( BufferedImage image, Provider llmProvider, String executionId, String nodeId ) throws Exception
    {
        validateInputs( image, llmProvider, executionId, nodeId );

        try
        {
            String imageBase64 = imageToBase64( image );
            ChatModel chatModel = CDI.current( ).select( ModelService.class ).get( ).createTrackedChatModel( llmProvider, executionId, nodeId, 1 );
            String ocrResult = processImageOCR( chatModel, imageBase64 );

            if ( ocrResult == null || ocrResult.trim( ).isEmpty( ) )
            {
                return "";
            }

            return ocrResult.trim( );

        }
        catch( Exception e )
        {
            AppLogService.error( LOG_OCR_ERROR, e );
            throw new Exception( ERROR_OCR_PROCESSING + e.getMessage( ), e );
        }
    }

    /**
     * Processes OCR using chat model and base64 image
     *
     * @param model
     *            The chat language model
     * @param imageBase64
     *            The base64 encoded image
     * @return The extracted text
     * @throws Exception
     *             if processing fails
     */
    private String processImageOCR( ChatModel model, String imageBase64 ) throws Exception
    {
        if ( imageBase64.startsWith( DATA_IMAGE_JPEG_BASE64_PREFIX ) )
        {
            imageBase64 = imageBase64.substring( DATA_IMAGE_JPEG_BASE64_PREFIX.length( ) ).trim( );
        }

        ImageContent imageContent = ImageContent.from( imageBase64, IMAGE_MIME_TYPE_JPEG, ImageContent.DetailLevel.HIGH );
        List<ChatMessage> messages = List.of( new SystemMessage( OCR_SYSTEM_PROMPT ), new UserMessage( TextContent.from( OCR_USER_PROMPT ), imageContent ) );

        ChatResponse response = model.chat( messages );
        return response.aiMessage( ).text( );
    }

    /**
     * Converts BufferedImage to base64 string
     *
     * @param image
     *            The image to convert
     * @return Base64 encoded image string
     * @throws IOException
     *             if image conversion fails
     */
    private String imageToBase64( BufferedImage image ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream( ) )
        {
            BufferedImage rgbImage = convertToRGB( image );
            ImageIO.write( rgbImage, IMAGE_FORMAT_JPEG, baos );
            byte [ ] imageBytes = baos.toByteArray( );
            return Base64.getEncoder( ).encodeToString( imageBytes );
        }
    }

    /**
     * Converts an image to RGB format if it is not already in that format
     *
     * @param originalImage
     *            The original image
     * @return A new BufferedImage in RGB format
     */
    private BufferedImage convertToRGB( BufferedImage originalImage )
    {
        if ( originalImage.getType( ) == BufferedImage.TYPE_INT_RGB )
        {
            return originalImage;
        }

        BufferedImage rgbImage = new BufferedImage( originalImage.getWidth( ), originalImage.getHeight( ), BufferedImage.TYPE_INT_RGB );

        Graphics2D graphics = rgbImage.createGraphics( );
        try
        {
            graphics.setComposite( AlphaComposite.SrcOver );
            graphics.setColor( new java.awt.Color( 255, 255, 255 ) );
            graphics.fillRect( 0, 0, rgbImage.getWidth( ), rgbImage.getHeight( ) );
            graphics.drawImage( originalImage, 0, 0, null );
        }
        finally
        {
            graphics.dispose( );
        }

        return rgbImage;
    }

    /**
     * Checks if an image is worth processing for OCR based on minimum dimensions
     *
     * @param image
     *            The image to evaluate
     * @return true if image meets minimum requirements
     */
    public boolean isImageWorthProcessing( BufferedImage image )
    {
        if ( image == null )
        {
            return false;
        }

        return image.getWidth( ) >= MIN_WIDTH && image.getHeight( ) >= MIN_HEIGHT && ( image.getWidth( ) * image.getHeight( ) ) >= MIN_PIXELS;
    }

    /**
     * Optimizes image for OCR by resizing if necessary
     *
     * @param originalImage
     *            The original image
     * @return Optimized image for OCR processing
     */
    public BufferedImage optimizeImageForOCR( BufferedImage originalImage )
    {
        int width = originalImage.getWidth( );
        int height = originalImage.getHeight( );

        if ( width <= MAX_DIMENSION && height <= MAX_DIMENSION )
        {
            return originalImage;
        }

        double ratio = Math.min( (double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height );
        int newWidth = (int) ( width * ratio );
        int newHeight = (int) ( height * ratio );

        BufferedImage resizedImage = new BufferedImage( newWidth, newHeight, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics = resizedImage.createGraphics( );
        try
        {
            graphics.drawImage( originalImage.getScaledInstance( newWidth, newHeight, java.awt.Image.SCALE_SMOOTH ), 0, 0, null );
        }
        finally
        {
            graphics.dispose( );
        }

        return resizedImage;
    }

    /**
     * Validates input parameters for OCR processing
     *
     * @param image
     *            The image to validate
     * @param llmProvider
     *            The provider to validate
     * @param executionId
     *            The execution ID to validate
     * @param nodeId
     *            The node ID to validate
     * @throws IllegalArgumentException
     *             if any parameter is invalid
     */
    private void validateInputs( BufferedImage image, Provider llmProvider, String executionId, String nodeId )
    {
        if ( image == null )
        {
            throw new IllegalArgumentException( ERROR_NULL_IMAGE );
        }
        if ( llmProvider == null )
        {
            throw new IllegalArgumentException( ERROR_NULL_PROVIDER );
        }
        if ( executionId == null || nodeId == null )
        {
            throw new IllegalArgumentException( ERROR_NULL_EXECUTION_NODE_ID );
        }
    }
}
