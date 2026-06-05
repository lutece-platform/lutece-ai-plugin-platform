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

/**
 * BotPipeline class representing the association between a bot and a pipeline
 */
public class BotPipeline implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int _nId;
    private int _nBotId;
    private int _nPipelineId;
    private String _strToolDescription;

    /**
     * Returns the ID
     *
     * @return The ID
     */
    public int getId( )
    {
        return _nId;
    }

    /**
     * Sets the ID
     *
     * @param nId
     *            The ID
     */
    public void setId( int nId )
    {
        _nId = nId;
    }

    /**
     * Returns the bot ID
     *
     * @return The bot ID
     */
    public int getBotId( )
    {
        return _nBotId;
    }

    /**
     * Sets the bot ID
     *
     * @param nBotId
     *            The bot ID
     */
    public void setBotId( int nBotId )
    {
        _nBotId = nBotId;
    }

    /**
     * Returns the pipeline ID
     *
     * @return The pipeline ID
     */
    public int getPipelineId( )
    {
        return _nPipelineId;
    }

    /**
     * Sets the pipeline ID
     *
     * @param nPipelineId
     *            The pipeline ID
     */
    public void setPipelineId( int nPipelineId )
    {
        _nPipelineId = nPipelineId;
    }

    /**
     * Returns the tool description
     *
     * @return The tool description
     */
    public String getToolDescription( )
    {
        return _strToolDescription;
    }

    /**
     * Sets the tool description
     *
     * @param strToolDescription
     *            The tool description
     */
    public void setToolDescription( String strToolDescription )
    {
        _strToolDescription = strToolDescription;
    }

}
