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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * A query transformer that implements HyDE (Hypothetical Document Embeddings). It generates a hypothetical document that would answer the query, which can
 * improve semantic search results.
 */
public class HydeQueryTransformer implements QueryTransformer
{

    private final ChatModel chatModel;
    private final int maxTokens;
    private final String promptTemplate;

    private static final String DEFAULT_SYSTEM_PROMPT = "Vous êtes un assistant expert qui génère des documents hypothétiques pour améliorer la recherche sémantique. "
            + "Générez un document détaillé qui répondrait parfaitement à la question posée. "
            + "Le document doit être informatif, précis et contenir les mots-clés pertinents.";

    /**
     * Creates a new HydeQueryTransformer.
     *
     * @param chatModel
     *            the chat model to use for generation
     * @param maxTokens
     *            the maximum number of tokens for the generated document
     * @param promptTemplate
     *            the prompt template (use {query} as placeholder)
     */
    private HydeQueryTransformer( ChatModel chatModel, int maxTokens, String promptTemplate )
    {
        this.chatModel = chatModel;
        this.maxTokens = maxTokens;
        this.promptTemplate = ( promptTemplate != null && !promptTemplate.trim( ).isEmpty( ) ) ? promptTemplate
                : "Générez une réponse détaillée et complète pour la question suivante : {query}";
    }

    /**
     * Transforms a query by generating a hypothetical document.
     *
     * @param query
     *            the query to transform
     * @return a collection containing the original query and the HyDE-generated document as a new query
     */
    @Override
    public Collection<Query> transform( Query query )
    {
        try
        {
            SystemMessage systemMessage = SystemMessage.from( DEFAULT_SYSTEM_PROMPT );

            String userPrompt = promptTemplate.replace( "{query}", query.text( ) );
            UserMessage userMessage = UserMessage.from( userPrompt );

            ChatRequestParameters parameters = DefaultChatRequestParameters.builder( ).maxOutputTokens( maxTokens ).build( );

            ChatRequest chatRequest = ChatRequest.builder( ).messages( Arrays.asList( systemMessage, userMessage ) ).parameters( parameters ).build( );

            ChatResponse response = chatModel.chat( chatRequest );
            String hydeDocument = response.aiMessage( ).text( );

            Query hydeQuery = Query.from( hydeDocument );
            return Collections.singletonList( hydeQuery );

        }
        catch( Exception e )
        {
            AppLogService.error( "Error generating HyDE document for query: {}", query.text( ), e );
            return Collections.emptyList( );
        }
    }

    /**
     * Builder for HydeQueryTransformer.
     */
    public static class Builder
    {
        private ChatModel chatModel;
        private int maxTokens = 500;
        private String promptTemplate = "Générez une réponse détaillée et complète pour la question suivante : {query}";

        /**
         * Sets the chat model to use for generation.
         *
         * @param chatModel
         *            the chat model
         * @return this builder
         */
        public Builder chatModel( ChatModel chatModel )
        {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Sets the maximum number of tokens for the generated document.
         *
         * @param maxTokens
         *            the maximum number of tokens
         * @return this builder
         */
        public Builder maxTokens( int maxTokens )
        {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Sets the prompt template (use {query} as placeholder).
         *
         * @param promptTemplate
         *            the prompt template
         * @return this builder
         */
        public Builder promptTemplate( String promptTemplate )
        {
            this.promptTemplate = promptTemplate;
            return this;
        }

        /**
         * Builds the HydeQueryTransformer instance.
         *
         * @return the built HydeQueryTransformer
         * @throws IllegalArgumentException
         *             if no chat model has been set
         */
        public HydeQueryTransformer build( )
        {
            if ( chatModel == null )
            {
                throw new IllegalArgumentException( "ChatModel is required" );
            }
            return new HydeQueryTransformer( chatModel, maxTokens, promptTemplate );
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder( )
    {
        return new Builder( );
    }
}
