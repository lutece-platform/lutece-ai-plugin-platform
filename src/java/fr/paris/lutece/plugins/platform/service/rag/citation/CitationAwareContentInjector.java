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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content injector that formats content with citation markers to enable tracking of which sources are used in the LLM response.
 */
public class CitationAwareContentInjector implements ContentInjector
{

    private static final String RAG_PROMPT_TEMPLATE_WITH_CITATIONS = "{{userMessage}}\n" + "\n" + "<rag_context>\n"
            + "Tu as accès aux sources suivantes pour répondre. CHAQUE source a un numéro entre crochets.\n" + "\n" + "{{contents}}\n" + "\n"
            + "RÈGLES OBLIGATOIRES POUR CITER:\n" + "1. Quand tu utilises une information d'une source, tu DOIS écrire [Source X] juste après\n"
            + "2. Exemple correct: \"Le Pass Navigo est remboursé à 75% [Source 1].\"\n"
            + "3. Si une même info est dans plusieurs sources, cite-les: [Source 1, Source 3]\n"
            + "4. NE JAMAIS inventer des numéros de source qui n'existent pas\n" + "5. NE JAMAIS utiliser une information sans citer sa source\n" + "\n"
            + "COMMENT RÉPONDRE:\n" + "- Utilise SEULEMENT les informations des sources ci-dessus\n"
            + "- Si tu ne trouves pas l'information, dis-le clairement\n" + "- Pour les salutations (Bonjour, Merci), réponds normalement sans citer\n"
            + "</rag_context>";

    private static final String SIMPLE_TEMPLATE_WITH_CITATION_INSTRUCTION = "{{userMessage}}\n\n" + "<rag_context>\n"
            + "Il n'y a aucune information pertinente disponible pour répondre à la question dans la base de connaissance.\n"
            + "Si la question est une formule de politesse (ex : Bonjour, Merci), réponds simplement de façon courtoise.\n"
            + "Si c'est une question qui nécessite des informations, réponds obligatoirement \"Je n'ai pas d'information disponible sur ce sujet dans ma base de connaissance.\"\n"
            + "Réponds de façon claire, concise et proactive en français.\n" + "</rag_context>";

    private final PromptTemplate promptTemplate;
    private final PromptTemplate emptyContentTemplate;

    /**
     * Creates a new CitationAwareContentInjector with default templates.
     */
    public CitationAwareContentInjector( )
    {
        this( PromptTemplate.from( RAG_PROMPT_TEMPLATE_WITH_CITATIONS ), PromptTemplate.from( SIMPLE_TEMPLATE_WITH_CITATION_INSTRUCTION ) );
    }

    /**
     * Creates a new CitationAwareContentInjector with custom templates.
     *
     * @param promptTemplate
     *            the template for when content is available
     * @param emptyContentTemplate
     *            the template for when no content is available
     */
    public CitationAwareContentInjector( PromptTemplate promptTemplate, PromptTemplate emptyContentTemplate )
    {
        this.promptTemplate = promptTemplate;
        this.emptyContentTemplate = emptyContentTemplate;
    }

    @Override
    public ChatMessage inject( List<Content> contents, ChatMessage chatMessage )
    {
        if ( contents == null || contents.isEmpty( ) )
        {
            return handleEmptyContent( chatMessage );
        }

        String formattedContents = formatContentsWithCitations( contents );
        Map<String, Object> variables = new HashMap<>( );
        variables.put( "userMessage", extractMessageText( chatMessage ) );
        variables.put( "contents", formattedContents );

        Prompt prompt = promptTemplate.apply( variables );

        if ( chatMessage instanceof UserMessage && ( (UserMessage) chatMessage ).name( ) != null )
        {
            return prompt.toUserMessage( ( (UserMessage) chatMessage ).name( ) );
        }

        return prompt.toUserMessage( );
    }

    /**
     * Formats contents with citation markers.
     *
     * @param contents
     *            the list of contents to format
     * @return formatted string with numbered sources
     */
    private String formatContentsWithCitations( List<Content> contents )
    {
        StringBuilder formatted = new StringBuilder( );

        for ( int i = 0; i < contents.size( ); i++ )
        {
            Content content = contents.get( i );
            String documentName = content.textSegment( ).metadata( ).getString( EmbeddingService.META_DOCUMENT_NAME );

            formatted.append( "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" );
            formatted.append( "[Source " ).append( i + 1 ).append( "]" );
            if ( documentName != null && !documentName.isEmpty( ) )
            {
                formatted.append( " 📄 " ).append( documentName );
            }
            formatted.append( "\n" );
            formatted.append( "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" );

            formatted.append( content.textSegment( ).text( ) );
            formatted.append( "\n" );

            String datasetName = content.textSegment( ).metadata( ).getString( EmbeddingService.META_DATASET_NAME );
            if ( datasetName != null && !datasetName.isEmpty( ) )
            {
                formatted.append( "\n📁 Dataset: " ).append( datasetName );
            }

            formatted.append( "\n\n" );
        }

        return formatted.toString( ).trim( );
    }

    /**
     * Handles the case when no content is available.
     *
     * @param chatMessage
     *            the original chat message
     * @return formatted message for empty content
     */
    private ChatMessage handleEmptyContent( ChatMessage chatMessage )
    {
        Map<String, Object> variables = new HashMap<>( );
        variables.put( "userMessage", extractMessageText( chatMessage ) );

        Prompt prompt = emptyContentTemplate.apply( variables );

        if ( chatMessage instanceof UserMessage && ( (UserMessage) chatMessage ).name( ) != null )
        {
            return prompt.toUserMessage( ( (UserMessage) chatMessage ).name( ) );
        }

        return prompt.toUserMessage( );
    }

    /**
     * Extracts text from a chat message.
     *
     * @param chatMessage
     *            the chat message
     * @return the message text
     */
    private String extractMessageText( ChatMessage chatMessage )
    {
        if ( chatMessage instanceof UserMessage )
        {
            return ( (UserMessage) chatMessage ).singleText( );
        }
        else if ( chatMessage instanceof AiMessage )
        {
            return ( (AiMessage) chatMessage ).text( );
        }
        else if ( chatMessage instanceof SystemMessage )
        {
            return ( (SystemMessage) chatMessage ).text( );
        }
        return "";
    }
}
