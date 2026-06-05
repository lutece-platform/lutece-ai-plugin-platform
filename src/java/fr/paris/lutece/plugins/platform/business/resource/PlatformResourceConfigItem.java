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
package fr.paris.lutece.plugins.platform.business.resource;

/**
 * Class representing a admin configuration item for platform resources.
 */
public class PlatformResourceConfigItem
{
    private String _strName;
    private String _strDescription;
    private String _strUrl;
    private String _strIcon;

    /**
     * Returns the name of this configuration item.
     *
     * @return The name as a String
     */
    public String getName( )
    {
        return _strName;
    }

    /**
     * Sets the name of this configuration item.
     *
     * @param strName
     *            The name to set
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the description of this configuration item.
     *
     * @return The description as a String
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * Sets the description of this configuration item.
     *
     * @param strDescription
     *            The description to set
     */
    public void setDescription( String strDescription )
    {
        _strDescription = strDescription;
    }

    /**
     * Returns the URL of this configuration item.
     *
     * @return The URL as a String
     */
    public String getUrl( )
    {
        return _strUrl;
    }

    /**
     * Sets the URL of this configuration item.
     *
     * @param strUrl
     *            The URL to set
     */
    public void setUrl( String strUrl )
    {
        _strUrl = strUrl;
    }

    /**
     * Returns the icon of this configuration item.
     *
     * @return The icon as a String
     */
    public String getIcon( )
    {
        return _strIcon;
    }

    /**
     * Sets the icon of this configuration item.
     *
     * @param strIcon
     *            The icon to set
     */
    public void setIcon( String strIcon )
    {
        _strIcon = strIcon;
    }
}
