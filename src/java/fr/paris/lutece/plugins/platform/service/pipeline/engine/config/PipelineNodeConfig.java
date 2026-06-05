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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents a node configuration in a pipeline. Contains node identification, type, positioning and associated data parameters.
 */
public class PipelineNodeConfig
{

    private static final String JSON_ID_PROPERTY = "id";
    private static final String JSON_TYPE_PROPERTY = "type";
    private static final String JSON_DATA_PROPERTY = "data";
    private static final String JSON_X_PROPERTY = "x";
    private static final String JSON_Y_PROPERTY = "y";

    @JsonProperty( JSON_ID_PROPERTY )
    private String id;

    @JsonProperty( JSON_TYPE_PROPERTY )
    private String type;

    @JsonProperty( JSON_DATA_PROPERTY )
    private Map<String, Object> data;

    @JsonProperty( JSON_X_PROPERTY )
    private int x;

    @JsonProperty( JSON_Y_PROPERTY )
    private int y;

    /**
     * Gets the node identifier.
     *
     * @return the node identifier
     */
    public String getId( )
    {
        return id;
    }

    /**
     * Sets the node identifier.
     *
     * @param id
     *            the node identifier to set
     */
    public void setId( String id )
    {
        this.id = id;
    }

    /**
     * Gets the node type.
     *
     * @return the node type
     */
    public String getType( )
    {
        return type;
    }

    /**
     * Sets the node type.
     *
     * @param type
     *            the node type to set
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * Gets the node data parameters.
     *
     * @return the node data parameters map
     */
    public Map<String, Object> getData( )
    {
        return data;
    }

    /**
     * Sets the node data parameters.
     *
     * @param data
     *            the node data parameters map to set
     */
    public void setData( Map<String, Object> data )
    {
        this.data = data;
    }

    /**
     * Gets the node X coordinate position.
     *
     * @return the X coordinate
     */
    public int getX( )
    {
        return x;
    }

    /**
     * Sets the node X coordinate position.
     *
     * @param x
     *            the X coordinate to set
     */
    public void setX( int x )
    {
        this.x = x;
    }

    /**
     * Gets the node Y coordinate position.
     *
     * @return the Y coordinate
     */
    public int getY( )
    {
        return y;
    }

    /**
     * Sets the node Y coordinate position.
     *
     * @param y
     *            the Y coordinate to set
     */
    public void setY( int y )
    {
        this.y = y;
    }
}
