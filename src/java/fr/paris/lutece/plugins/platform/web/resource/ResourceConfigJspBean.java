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
package fr.paris.lutece.plugins.platform.web.resource;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.resource.PlatformResourceConfigItem;
import fr.paris.lutece.plugins.platform.service.resource.PlatformResourceService;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.api.user.User;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;

/**
 * This class provides the user interface to manage platform resource configuration
 */
@RequestScoped
@Named
@Controller( controllerJsp = "ManageResourceConfig.jsp", controllerPath = "jsp/admin/plugins/platform/", right = "PLATFORM_CONFIG_MANAGEMENT" )
public class ResourceConfigJspBean extends MVCAdminJspBean
{
    @Inject
    private Models _models;

    private static final String TEMPLATE_MANAGE_CONFIG_RESOURCES = "/admin/plugins/platform/manage_config_resources.html";
    private static final String PROPERTY_PAGE_TITLE_MANAGE_CONFIG_RESOURCES = "platform.manage_config_resources.pageTitle";
    private static final String MARK_RESOURCE_TYPE_LIST = "resource_type_list";
    private static final String MARK_CONFIG_LISTS_BY_TYPE = "config_lists_by_type";
    private static final String VIEW_MANAGE_CONFIG_RESOURCES = "manageConfigResources";

    @Inject
    private PlatformResourceService _platformResourceService;

    /**
     * Handles the request to view the resource configuration management page
     *
     * @param request
     *            The HTTP request
     * @return The HTML page content
     */
    @View( value = VIEW_MANAGE_CONFIG_RESOURCES, defaultView = true )
    public String getManageConfigResources( HttpServletRequest request )
    {
        User user = AdminUserService.getAdminUser( request );
        List<IPlatformResourceType> listResourceTypes = _platformResourceService.getAllResourceType( );
        Map<String, List<PlatformResourceConfigItem>> configListsByType = _platformResourceService.getConfigListsByType( user );

        Models model = _models;
        model.put( MARK_RESOURCE_TYPE_LIST, listResourceTypes );
        model.put( MARK_CONFIG_LISTS_BY_TYPE, configListsByType );

        return getPage( PROPERTY_PAGE_TITLE_MANAGE_CONFIG_RESOURCES, TEMPLATE_MANAGE_CONFIG_RESOURCES, model );
    }

}
