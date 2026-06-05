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
package fr.paris.lutece.plugins.platform.business.bot;

import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import fr.paris.lutece.plugins.platform.business.rbac.AgentPermissionResource;
import fr.paris.lutece.plugins.platform.business.dataset.Dataset;

/**
 * Bot class representing a conversational agent with its properties and configurations
 */
public class Bot implements Serializable, AgentPermissionResource
{
    private static final long serialVersionUID = 1L;

    public static final String RESOURCE_TYPE = "BOT";

    private int _nId;

    @NotEmpty( message = "#i18n{platform.agent.validation.bot.BotName.notEmpty}" )
    private String _strBotName;

    private String _strBotDescription;
    private String _strBotSystemPrompt;
    private String _strLogoBase64;
    private String _strWelcomeMessage;
    private int _nClientId;
    private int _nLlmProviderId;
    private int _nEmbedProviderId;
    private Timestamp _timestampCreatedAt;
    private Timestamp _timestampUpdatedAt;
    private List<Dataset> _listBotDatasets;
    private int _nMaxTokens;
    private double _dTemperature = 0.7;
    private int _nRateLimitByUserByDay = 0;
    private boolean _bEnableContentAggregation = false;
    private int _nAggregationMaxResults = 5;
    private String _strAggregationStrategy = "DEFAULT";
    private String _strQueryEnhancementMode = "NONE";
    private int _nQueryExpansionVariants = 3;
    private int _nHydeMaxTokens = 500;
    private String _strHydePromptTemplate = "Générez une réponse détaillée et complète pour la question suivante : {query}";
    private int _nSemanticSearchMaxResults = 15;
    private double _dSemanticSearchMinScore = 0.7;
    private boolean _bEnableBuiltinTools = false;

    /**
     * Returns the bot ID
     *
     * @return The bot ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the bot ID
     *
     * @param nId
     *            The bot ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the bot name
     *
     * @return The bot name
     */
    public String getBotName( )
    {
        return _strBotName;
    }

    /**
     * Sets the bot name
     *
     * @param strBotName
     *            The bot name
     */
    public void setBotName( String strBotName )
    {
        _strBotName = strBotName;
    }

    /**
     * Returns the bot description
     *
     * @return The bot description
     */
    public String getBotDescription( )
    {
        return _strBotDescription;
    }

    /**
     * Sets the bot description
     *
     * @param strBotDescription
     *            The bot description
     */
    public void setBotDescription( String strBotDescription )
    {
        _strBotDescription = strBotDescription;
    }

    /**
     * Returns the bot system prompt
     *
     * @return The bot system prompt
     */
    public String getBotSystemPrompt( )
    {
        return _strBotSystemPrompt;
    }

    /**
     * Sets the bot system prompt
     *
     * @param strBotSystemPrompt
     *            The bot system prompt
     */
    public void setBotSystemPrompt( String strBotSystemPrompt )
    {
        _strBotSystemPrompt = strBotSystemPrompt;
    }

    /**
     * Returns the client ID
     *
     * @return The client ID
     */
    public int getClientId( )
    {
        return _nClientId;
    }

    /**
     * Sets the client ID
     *
     * @param nClientId
     *            The client ID
     */
    public void setClientId( int nClientId )
    {
        _nClientId = nClientId;
    }

    /**
     * Returns the LLM provider ID
     *
     * @return The LLM provider ID
     */
    public int getLlmProviderId( )
    {
        return _nLlmProviderId;
    }

    /**
     * Sets the LLM provider ID
     *
     * @param nLlmProviderId
     *            The LLM provider ID
     */
    public void setLlmProviderId( int nLlmProviderId )
    {
        _nLlmProviderId = nLlmProviderId;
    }

    /**
     * Returns the embedding provider ID
     *
     * @return The embedding provider ID
     */
    public int getEmbedProviderId( )
    {
        return _nEmbedProviderId;
    }

    /**
     * Sets the embedding provider ID
     *
     * @param nEmbedProviderId
     *            The embedding provider ID
     */
    public void setEmbedProviderId( int nEmbedProviderId )
    {
        _nEmbedProviderId = nEmbedProviderId;
    }

    /**
     * Returns the creation timestamp
     *
     * @return The creation timestamp
     */
    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC" )
    public Timestamp getCreatedAt( )
    {
        return _timestampCreatedAt;
    }

    /**
     * Sets the creation timestamp
     *
     * @param timestampCreatedAt
     *            The creation timestamp
     */
    public void setCreatedAt( Timestamp timestampCreatedAt )
    {
        _timestampCreatedAt = timestampCreatedAt;
    }

    /**
     * Returns the update timestamp
     *
     * @return The update timestamp
     */
    @JsonFormat( shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC" )
    public Timestamp getUpdatedAt( )
    {
        return _timestampUpdatedAt;
    }

    /**
     * Sets the update timestamp
     *
     * @param timestampUpdatedAt
     *            The update timestamp
     */
    public void setUpdatedAt( Timestamp timestampUpdatedAt )
    {
        _timestampUpdatedAt = timestampUpdatedAt;
    }

    /**
     * Returns the list of datasets associated with the bot
     *
     * @return The list of datasets
     */
    public List<Dataset> getListBotDatasets( )
    {
        return _listBotDatasets;
    }

    /**
     * Sets the list of datasets associated with the bot
     *
     * @param listBotDatasets
     *            The list of datasets
     */
    public void setListBotDatasets( List<Dataset> listBotDatasets )
    {
        _listBotDatasets = listBotDatasets;
    }

    /**
     * Returns the logo in Base64 format
     *
     * @return The logo in Base64 format
     */
    public String getLogoBase64( )
    {
        return _strLogoBase64;
    }

    /**
     * Sets the logo in Base64 format
     *
     * @param strLogoBase64
     *            The logo in Base64 format
     */
    public void setLogoBase64( String strLogoBase64 )
    {
        _strLogoBase64 = strLogoBase64;
    }

    /**
     * Returns the welcome message
     *
     * @return The welcome message
     */
    public String getWelcomeMessage( )
    {
        return _strWelcomeMessage;
    }

    /**
     * Sets the welcome message
     *
     * @param strWelcomeMessage
     *            The welcome message
     */
    public void setWelcomeMessage( String strWelcomeMessage )
    {
        _strWelcomeMessage = strWelcomeMessage;
    }

    /**
     * Returns the maximum number of tokens
     *
     * @return The maximum number of tokens
     */
    public int getMaxTokens( )
    {
        return _nMaxTokens;
    }

    /**
     * Sets the maximum number of tokens
     *
     * @param nMaxTokens
     *            The maximum number of tokens
     */
    public void setMaxTokens( int nMaxTokens )
    {
        _nMaxTokens = nMaxTokens;
    }

    /**
     * Returns the temperature parameter for LLM
     *
     * @return The temperature (0.0 to 1.0)
     */
    public double getTemperature( )
    {
        return _dTemperature;
    }

    /**
     * Sets the temperature parameter for LLM
     *
     * @param dTemperature
     *            The temperature (0.0 to 1.0)
     */
    public void setTemperature( double dTemperature )
    {
        _dTemperature = dTemperature;
    }

    /**
     * Returns the rate limit by user by day
     *
     * @return the rate limit by user by day
     */
    public int getRateLimitByUserByDay( )
    {
        return _nRateLimitByUserByDay;
    }

    /**
     * Sets the rate limit by user by day
     *
     * @param nRateLimitByUserByDay
     *            the rate limit by user by day
     */
    public void setRateLimitByUserByDay( int nRateLimitByUserByDay )
    {
        _nRateLimitByUserByDay = nRateLimitByUserByDay;
    }

    /**
     * Returns whether content aggregation is enabled
     *
     * @return true if content aggregation is enabled
     */
    public boolean isEnableContentAggregation( )
    {
        return _bEnableContentAggregation;
    }

    /**
     * Sets whether content aggregation is enabled
     *
     * @param bEnableContentAggregation
     *            true to enable content aggregation
     */
    public void setEnableContentAggregation( boolean bEnableContentAggregation )
    {
        _bEnableContentAggregation = bEnableContentAggregation;
    }

    /**
     * Returns the maximum number of results after aggregation
     *
     * @return The maximum number of results
     */
    public int getAggregationMaxResults( )
    {
        return _nAggregationMaxResults;
    }

    /**
     * Sets the maximum number of results after aggregation
     *
     * @param nAggregationMaxResults
     *            The maximum number of results
     */
    public void setAggregationMaxResults( int nAggregationMaxResults )
    {
        _nAggregationMaxResults = nAggregationMaxResults;
    }

    /**
     * Returns the aggregation strategy
     *
     * @return The aggregation strategy
     */
    public String getAggregationStrategy( )
    {
        return _strAggregationStrategy;
    }

    /**
     * Sets the aggregation strategy
     *
     * @param strAggregationStrategy
     *            The aggregation strategy
     */
    public void setAggregationStrategy( String strAggregationStrategy )
    {
        _strAggregationStrategy = strAggregationStrategy;
    }

    /**
     * Returns the query enhancement mode
     *
     * @return The query enhancement mode
     */
    public String getQueryEnhancementMode( )
    {
        return _strQueryEnhancementMode;
    }

    /**
     * Sets the query enhancement mode
     *
     * @param strQueryEnhancementMode
     *            The query enhancement mode
     */
    public void setQueryEnhancementMode( String strQueryEnhancementMode )
    {
        _strQueryEnhancementMode = strQueryEnhancementMode;
    }

    /**
     * Returns the number of query expansion variants
     *
     * @return The number of variants
     */
    public int getQueryExpansionVariants( )
    {
        return _nQueryExpansionVariants;
    }

    /**
     * Sets the number of query expansion variants
     *
     * @param nQueryExpansionVariants
     *            The number of variants
     */
    public void setQueryExpansionVariants( int nQueryExpansionVariants )
    {
        _nQueryExpansionVariants = nQueryExpansionVariants;
    }

    /**
     * Returns the maximum number of tokens for HyDE documents
     *
     * @return The maximum number of tokens
     */
    public int getHydeMaxTokens( )
    {
        return _nHydeMaxTokens;
    }

    /**
     * Sets the maximum number of tokens for HyDE documents
     *
     * @param nHydeMaxTokens
     *            The maximum number of tokens
     */
    public void setHydeMaxTokens( int nHydeMaxTokens )
    {
        _nHydeMaxTokens = nHydeMaxTokens;
    }

    /**
     * Returns the HyDE prompt template
     *
     * @return The HyDE prompt template
     */
    public String getHydePromptTemplate( )
    {
        return _strHydePromptTemplate;
    }

    /**
     * Sets the HyDE prompt template
     *
     * @param strHydePromptTemplate
     *            The HyDE prompt template
     */
    public void setHydePromptTemplate( String strHydePromptTemplate )
    {
        _strHydePromptTemplate = strHydePromptTemplate;
    }

    /**
     * Returns the maximum number of semantic search results
     *
     * @return The maximum number of results
     */
    public int getSemanticSearchMaxResults( )
    {
        return _nSemanticSearchMaxResults;
    }

    /**
     * Sets the maximum number of semantic search results
     *
     * @param nSemanticSearchMaxResults
     *            The maximum number of results
     */
    public void setSemanticSearchMaxResults( int nSemanticSearchMaxResults )
    {
        _nSemanticSearchMaxResults = nSemanticSearchMaxResults;
    }

    /**
     * Returns the minimum score for semantic search results
     *
     * @return The minimum score (0.0 to 1.0)
     */
    public double getSemanticSearchMinScore( )
    {
        return _dSemanticSearchMinScore;
    }

    /**
     * Sets the minimum score for semantic search results
     *
     * @param dSemanticSearchMinScore
     *            The minimum score (0.0 to 1.0)
     */
    public void setSemanticSearchMinScore( double dSemanticSearchMinScore )
    {
        _dSemanticSearchMinScore = dSemanticSearchMinScore;
    }

    /**
     * Returns whether built-in dataset tools are enabled for this bot
     *
     * @return true if built-in tools are enabled
     */
    public boolean isEnableBuiltinTools( )
    {
        return _bEnableBuiltinTools;
    }

    /**
     * Sets whether built-in dataset tools are enabled for this bot
     *
     * @param bEnableBuiltinTools
     *            true to enable built-in tools (list folders, read document, grep, search)
     */
    public void setEnableBuiltinTools( boolean bEnableBuiltinTools )
    {
        _bEnableBuiltinTools = bEnableBuiltinTools;
    }

    private boolean _bUserCanView;
    private boolean _bUserCanModify;
    private boolean _bUserCanDelete;

    /**
     * Checks if the current user can view this bot
     *
     * @return true if user can view this bot
     */
    public boolean isUserCanView( )
    {
        return _bUserCanView;
    }

    /**
     * Sets if the current user can view this bot
     *
     * @param userCanView
     *            true if user can view this bot
     */
    public void setUserCanView( boolean userCanView )
    {
        _bUserCanView = userCanView;
    }

    /**
     * Checks if the current user can modify this bot
     *
     * @return true if user can modify this bot
     */
    public boolean isUserCanModify( )
    {
        return _bUserCanModify;
    }

    /**
     * Sets if the current user can modify this bot
     *
     * @param userCanModify
     *            true if user can modify this bot
     */
    public void setUserCanModify( boolean userCanModify )
    {
        _bUserCanModify = userCanModify;
    }

    /**
     * Checks if the current user can delete this bot
     *
     * @return true if user can delete this bot
     */
    public boolean isUserCanDelete( )
    {
        return _bUserCanDelete;
    }

    /**
     * Sets if the current user can delete this bot
     *
     * @param userCanDelete
     *            true if user can delete this bot
     */
    public void setUserCanDelete( boolean userCanDelete )
    {
        _bUserCanDelete = userCanDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceId( )
    {
        return String.valueOf( _nId );
    }

}
