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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import fr.paris.lutece.util.ReferenceList;

/**
 * Implementation of the IBotDAO interface
 */
@ApplicationScoped
@Named( "platform.botDAO" )
public class BotDAO implements IBotDAO
{
    private static final String SQL_QUERY_INSERT = "INSERT INTO platform_bot ( bot_name, bot_description, bot_system_prompt, logo_base64, "
            + "welcome_message, client_id, llm_provider_id, embed_provider_id, created_at, updated_at, max_tokens, temperature, rate_limit_by_user_by_day, "
            + "enable_content_aggregation, aggregation_max_results, aggregation_strategy, query_enhancement_mode, query_expansion_variants, "
            + "hyde_max_tokens, hyde_prompt_template, " + "semantic_search_max_results, semantic_search_min_score, enable_builtin_tools ) "
            + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM platform_bot WHERE bot_id = ?";
    private static final String SQL_QUERY_UPDATE = "UPDATE platform_bot SET bot_name = ?, bot_description = ?, bot_system_prompt = ?, logo_base64 = ?, "
            + "welcome_message = ?, client_id = ?, llm_provider_id = ?, embed_provider_id = ?, updated_at = CURRENT_TIMESTAMP, "
            + "max_tokens = ?, temperature = ?, rate_limit_by_user_by_day = ?, enable_content_aggregation = ?, "
            + "aggregation_max_results = ?, aggregation_strategy = ?, query_enhancement_mode = ?, query_expansion_variants = ?, "
            + "hyde_max_tokens = ?, hyde_prompt_template = ?, "
            + "semantic_search_max_results = ?, semantic_search_min_score = ?, enable_builtin_tools = ? WHERE bot_id = ?";
    private static final String SQL_QUERY_SELECTALL = "SELECT bot_id, bot_name, bot_description, bot_system_prompt, logo_base64, welcome_message, "
            + "client_id, llm_provider_id, embed_provider_id, created_at, updated_at, max_tokens, temperature, rate_limit_by_user_by_day, "
            + "enable_content_aggregation, aggregation_max_results, aggregation_strategy, query_enhancement_mode, query_expansion_variants, "
            + "hyde_max_tokens, hyde_prompt_template, " + "semantic_search_max_results, semantic_search_min_score, enable_builtin_tools " + "FROM platform_bot";
    private static final String SQL_QUERY_SELECT_BY_ID = SQL_QUERY_SELECTALL + " WHERE bot_id = ?";
    private static final String SQL_QUERY_SELECT_BY_CLIENT_ID = SQL_QUERY_SELECTALL + " WHERE client_id = ?";
    private static final String SQL_QUERY_SELECT_REFERENCE_LIST = "SELECT bot_id, bot_name FROM platform_bot ORDER BY bot_name";

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert( Bot bot, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, Statement.RETURN_GENERATED_KEYS, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, bot.getBotName( ) );
            daoUtil.setString( nIndex++, bot.getBotDescription( ) );
            daoUtil.setString( nIndex++, bot.getBotSystemPrompt( ) );
            daoUtil.setString( nIndex++, bot.getLogoBase64( ) );
            daoUtil.setString( nIndex++, bot.getWelcomeMessage( ) );
            daoUtil.setInt( nIndex++, bot.getClientId( ) );
            daoUtil.setInt( nIndex++, bot.getLlmProviderId( ) );
            daoUtil.setInt( nIndex++, bot.getEmbedProviderId( ) );
            daoUtil.setInt( nIndex++, bot.getMaxTokens( ) );
            daoUtil.setDouble( nIndex++, bot.getTemperature( ) );
            daoUtil.setInt( nIndex++, bot.getRateLimitByUserByDay( ) );
            daoUtil.setBoolean( nIndex++, bot.isEnableContentAggregation( ) );
            daoUtil.setInt( nIndex++, bot.getAggregationMaxResults( ) );
            daoUtil.setString( nIndex++, bot.getAggregationStrategy( ) );
            daoUtil.setString( nIndex++, bot.getQueryEnhancementMode( ) );
            daoUtil.setInt( nIndex++, bot.getQueryExpansionVariants( ) );
            daoUtil.setInt( nIndex++, bot.getHydeMaxTokens( ) );
            daoUtil.setString( nIndex++, bot.getHydePromptTemplate( ) );
            daoUtil.setInt( nIndex++, bot.getSemanticSearchMaxResults( ) );
            daoUtil.setDouble( nIndex++, bot.getSemanticSearchMinScore( ) );
            daoUtil.setBoolean( nIndex++, bot.isEnableBuiltinTools( ) );

            daoUtil.executeUpdate( );
            if ( daoUtil.nextGeneratedKey( ) )
            {
                bot.setId( daoUtil.getGeneratedKeyInt( 1 ) );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Bot> load( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_ID, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeQuery( );
            Bot bot = null;
            if ( daoUtil.next( ) )
            {
                bot = loadFromDaoUtil( daoUtil );
            }
            return Optional.ofNullable( bot );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nKey, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nKey );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( Bot bot, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setString( nIndex++, bot.getBotName( ) );
            daoUtil.setString( nIndex++, bot.getBotDescription( ) );
            daoUtil.setString( nIndex++, bot.getBotSystemPrompt( ) );
            daoUtil.setString( nIndex++, bot.getLogoBase64( ) );
            daoUtil.setString( nIndex++, bot.getWelcomeMessage( ) );
            daoUtil.setInt( nIndex++, bot.getClientId( ) );
            daoUtil.setInt( nIndex++, bot.getLlmProviderId( ) );
            daoUtil.setInt( nIndex++, bot.getEmbedProviderId( ) );
            daoUtil.setInt( nIndex++, bot.getMaxTokens( ) );
            daoUtil.setDouble( nIndex++, bot.getTemperature( ) );
            daoUtil.setInt( nIndex++, bot.getRateLimitByUserByDay( ) );
            daoUtil.setBoolean( nIndex++, bot.isEnableContentAggregation( ) );
            daoUtil.setInt( nIndex++, bot.getAggregationMaxResults( ) );
            daoUtil.setString( nIndex++, bot.getAggregationStrategy( ) );
            daoUtil.setString( nIndex++, bot.getQueryEnhancementMode( ) );
            daoUtil.setInt( nIndex++, bot.getQueryExpansionVariants( ) );
            daoUtil.setInt( nIndex++, bot.getHydeMaxTokens( ) );
            daoUtil.setString( nIndex++, bot.getHydePromptTemplate( ) );
            daoUtil.setInt( nIndex++, bot.getSemanticSearchMaxResults( ) );
            daoUtil.setDouble( nIndex++, bot.getSemanticSearchMinScore( ) );
            daoUtil.setBoolean( nIndex++, bot.isEnableBuiltinTools( ) );
            daoUtil.setInt( nIndex, bot.getId( ) );

            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bot> selectBotsList( Plugin plugin )
    {
        List<Bot> botList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECTALL, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                botList.add( loadFromDaoUtil( daoUtil ) );
            }
            return botList;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bot> selectBotsListByClientId( int clientId, Plugin plugin )
    {
        List<Bot> botList = new ArrayList<>( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_BY_CLIENT_ID, plugin ) )
        {
            daoUtil.setInt( 1, clientId );
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                botList.add( loadFromDaoUtil( daoUtil ) );
            }
            return botList;
        }
    }

    /**
     * Loads a Bot object from a DAOUtil
     *
     * @param daoUtil
     *            The DAOUtil object
     * @return The Bot object
     */
    private Bot loadFromDaoUtil( DAOUtil daoUtil )
    {
        Bot bot = new Bot( );
        int nIndex = 1;

        bot.setId( daoUtil.getInt( nIndex++ ) );
        bot.setBotName( daoUtil.getString( nIndex++ ) );
        bot.setBotDescription( daoUtil.getString( nIndex++ ) );
        bot.setBotSystemPrompt( daoUtil.getString( nIndex++ ) );
        bot.setLogoBase64( daoUtil.getString( nIndex++ ) );
        bot.setWelcomeMessage( daoUtil.getString( nIndex++ ) );
        bot.setClientId( daoUtil.getInt( nIndex++ ) );
        bot.setLlmProviderId( daoUtil.getInt( nIndex++ ) );
        bot.setEmbedProviderId( daoUtil.getInt( nIndex++ ) );
        bot.setCreatedAt( daoUtil.getTimestamp( nIndex++ ) );
        bot.setUpdatedAt( daoUtil.getTimestamp( nIndex++ ) );
        bot.setMaxTokens( daoUtil.getInt( nIndex++ ) );
        bot.setTemperature( daoUtil.getDouble( nIndex++ ) );
        bot.setRateLimitByUserByDay( daoUtil.getInt( nIndex++ ) );
        bot.setEnableContentAggregation( daoUtil.getBoolean( nIndex++ ) );
        bot.setAggregationMaxResults( daoUtil.getInt( nIndex++ ) );
        bot.setAggregationStrategy( daoUtil.getString( nIndex++ ) );
        bot.setQueryEnhancementMode( daoUtil.getString( nIndex++ ) );
        bot.setQueryExpansionVariants( daoUtil.getInt( nIndex++ ) );
        bot.setHydeMaxTokens( daoUtil.getInt( nIndex++ ) );
        bot.setHydePromptTemplate( daoUtil.getString( nIndex++ ) );
        bot.setSemanticSearchMaxResults( daoUtil.getInt( nIndex++ ) );
        bot.setSemanticSearchMinScore( daoUtil.getDouble( nIndex++ ) );
        bot.setEnableBuiltinTools( daoUtil.getBoolean( nIndex++ ) );

        return bot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList selectBotsReferenceList( Plugin plugin )
    {
        ReferenceList referenceList = new ReferenceList( );
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT_REFERENCE_LIST, plugin ) )
        {
            daoUtil.executeQuery( );
            while ( daoUtil.next( ) )
            {
                referenceList.addItem( daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
        }
        return referenceList;
    }
}
