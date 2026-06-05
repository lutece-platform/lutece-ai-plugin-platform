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
package fr.paris.lutece.plugins.platform.web.mcp;

import fr.paris.lutece.portal.web.cdi.mvc.Models;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import fr.paris.lutece.plugins.platform.business.mcp.McpServer;
import fr.paris.lutece.plugins.platform.business.mcp.McpServerHome;
import fr.paris.lutece.plugins.platform.service.exception.InvalidRequestException;
import fr.paris.lutece.plugins.platform.service.exception.ResourceNotFoundException;
import fr.paris.lutece.plugins.platform.service.mcp.McpServerService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.util.mvc.admin.MVCAdminJspBean;
import fr.paris.lutece.portal.util.mvc.admin.annotations.Controller;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.util.url.UrlItem;

/**
 * JSP Bean for managing MCP server administration
 */
@RequestScoped
@Named
@Controller( controllerJsp = "ManageAgentMcpServers.jsp", controllerPath = "jsp/admin/plugins/platform/", right = "PLATFORM_CONFIG_MANAGEMENT" )
public class McpServerJspBean extends MVCAdminJspBean
{
    @Inject
    private Models _models;

    private static final long serialVersionUID = 1L;

    private static final String TEMPLATE_MANAGE_MCP_SERVERS = "/admin/plugins/platform/manage_mcp_servers.html";
    private static final String TEMPLATE_CREATE_MCP_SERVER = "/admin/plugins/platform/create_mcp_server.html";
    private static final String TEMPLATE_MODIFY_MCP_SERVER = "/admin/plugins/platform/modify_mcp_server.html";

    private static final String PARAMETER_MCP_SERVER_ID = "mcp_server_id";

    private static final String MARK_MCP_SERVER = "mcp_server";
    private static final String MARK_MCP_SERVER_LIST = "mcp_server_list";

    private static final String VIEW_MANAGE_MCP_SERVERS = "manageMcpServers";
    private static final String VIEW_CREATE_MCP_SERVER = "createMcpServer";
    private static final String VIEW_MODIFY_MCP_SERVER = "modifyMcpServer";

    private static final String ACTION_CREATE_MCP_SERVER = "createMcpServer";
    private static final String ACTION_MODIFY_MCP_SERVER = "modifyMcpServer";
    private static final String ACTION_CONFIRM_REMOVE_MCP_SERVER = "confirmRemoveMcpServer";
    private static final String ACTION_REMOVE_MCP_SERVER = "removeMcpServer";

    private static final String MESSAGE_MCP_SERVER_CREATED = "platform.agent.info.mcpServer.created";
    private static final String MESSAGE_MCP_SERVER_UPDATED = "platform.agent.info.mcpServer.updated";
    private static final String MESSAGE_MCP_SERVER_REMOVED = "platform.agent.info.mcpServer.removed";
    private static final String MESSAGE_CONFIRM_REMOVE_MCP_SERVER = "platform.agent.message.confirmRemoveMcpServer";
    private static final String MESSAGE_MCP_SERVER_NOT_FOUND = "platform.agent.error.mcpServer.not.found";
    private static final String MESSAGE_MCP_SERVER_VALIDATION_ERROR = "platform.agent.error.mcpServer.validation";
    private static final String MESSAGE_MANAGE_MCP_SERVERS_PAGE_TITLE = "platform.agent.manage_mcp_servers.pageTitle";
    private static final String MESSAGE_CREATE_MCP_SERVER_PAGE_TITLE = "platform.agent.create_mcp_server.pageTitle";
    private static final String MESSAGE_MODIFY_MCP_SERVER_PAGE_TITLE = "platform.agent.modify_mcp_server.pageTitle";

    private static final String VALIDATION_ATTRIBUTES_PREFIX = "platform.agent";

    private static final String PARAMETER_NAME = "mcp_server_name";
    private static final String PARAMETER_DESCRIPTION = "mcp_server_description";
    private static final String PARAMETER_TRANSPORT_TYPE = "transport_type";
    private static final String PARAMETER_URL = "url";
    private static final String PARAMETER_HEADERS_JSON = "headers_json";
    private static final String PARAMETER_TIMEOUT_MS = "timeout_ms";
    private static final String PARAMETER_ENABLED = "enabled";
    private static final String PARAMETER_TOOL_FILTER = "tool_filter";
    private static final String PARAMETER_LOG_REQUESTS = "log_requests";

    private static final int DEFAULT_TIMEOUT_MS = 60000;

    @Inject
    private McpServerService _mcpServerService;

    /**
     * Returns the MCP servers management page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_MANAGE_MCP_SERVERS )
    public String getManageMcpServers( HttpServletRequest request )
    {
        Models model = _models;
        List<McpServer> mcpServerList = McpServerHome.getMcpServersList( );
        model.put( MARK_MCP_SERVER_LIST, mcpServerList );
        return getPage( MESSAGE_MANAGE_MCP_SERVERS_PAGE_TITLE, TEMPLATE_MANAGE_MCP_SERVERS, model );
    }

    /**
     * Returns the MCP server creation page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_CREATE_MCP_SERVER )
    public String getCreateMcpServer( HttpServletRequest request )
    {
        Models model = _models;
        return getPage( MESSAGE_CREATE_MCP_SERVER_PAGE_TITLE, TEMPLATE_CREATE_MCP_SERVER, model );
    }

    /**
     * Processes the creation form
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_CREATE_MCP_SERVER )
    public String doCreateMcpServer( HttpServletRequest request )
    {
        McpServer mcpServer = new McpServer( );
        populateFromRequest( mcpServer, request );

        if ( !validateBean( mcpServer, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            addError( MESSAGE_MCP_SERVER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_CREATE_MCP_SERVER );
        }

        try
        {
            _mcpServerService.createServer( mcpServer );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_MCP_SERVER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_CREATE_MCP_SERVER );
        }

        addInfo( MESSAGE_MCP_SERVER_CREATED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_MCP_SERVERS );
    }

    /**
     * Returns the MCP server modification page
     *
     * @param request
     *            The HTTP request
     * @return The page content
     */
    @View( VIEW_MODIFY_MCP_SERVER )
    public String getModifyMcpServer( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_MCP_SERVER_ID ) );
        return McpServerHome.findByPrimaryKey( nId ).map( server -> {
            Models model = _models;
            model.put( MARK_MCP_SERVER, server );
            return getPage( MESSAGE_MODIFY_MCP_SERVER_PAGE_TITLE, TEMPLATE_MODIFY_MCP_SERVER, model );
        } ).orElseGet( ( ) -> {
            addError( MESSAGE_MCP_SERVER_NOT_FOUND, getLocale( ) );
            return redirectView( request, VIEW_MANAGE_MCP_SERVERS );
        } );
    }

    /**
     * Processes the modification form
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_MODIFY_MCP_SERVER )
    public String doModifyMcpServer( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_MCP_SERVER_ID ) );

        McpServer mcpServer = new McpServer( );
        populateFromRequest( mcpServer, request );

        if ( !validateBean( mcpServer, VALIDATION_ATTRIBUTES_PREFIX ) )
        {
            addError( MESSAGE_MCP_SERVER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_MODIFY_MCP_SERVER );
        }

        try
        {
            _mcpServerService.updateServer( nId, mcpServer );
        }
        catch( ResourceNotFoundException e )
        {
            addError( MESSAGE_MCP_SERVER_NOT_FOUND, getLocale( ) );
            return redirectView( request, VIEW_MANAGE_MCP_SERVERS );
        }
        catch( InvalidRequestException e )
        {
            addError( MESSAGE_MCP_SERVER_VALIDATION_ERROR, getLocale( ) );
            return redirectView( request, VIEW_MODIFY_MCP_SERVER );
        }

        addInfo( MESSAGE_MCP_SERVER_UPDATED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_MCP_SERVERS );
    }

    /**
     * Confirms removal of an MCP server
     *
     * @param request
     *            The HTTP request
     * @return The confirmation page
     */
    @Action( ACTION_CONFIRM_REMOVE_MCP_SERVER )
    public String getConfirmRemoveMcpServer( HttpServletRequest request )
    {
        String strId = request.getParameter( PARAMETER_MCP_SERVER_ID );
        UrlItem url = new UrlItem( getActionUrl( ACTION_REMOVE_MCP_SERVER ) );
        url.addParameter( PARAMETER_MCP_SERVER_ID, strId );
        return redirect( request,
                AdminMessageService.getMessageUrl( request, MESSAGE_CONFIRM_REMOVE_MCP_SERVER, url.getUrl( ), AdminMessage.TYPE_CONFIRMATION ) );
    }

    /**
     * Removes an MCP server
     *
     * @param request
     *            The HTTP request
     * @return The next view
     */
    @Action( ACTION_REMOVE_MCP_SERVER )
    public String doRemoveMcpServer( HttpServletRequest request )
    {
        int nId = Integer.parseInt( request.getParameter( PARAMETER_MCP_SERVER_ID ) );
        _mcpServerService.deleteServer( nId );
        addInfo( MESSAGE_MCP_SERVER_REMOVED, getLocale( ) );
        return redirectView( request, VIEW_MANAGE_MCP_SERVERS );
    }

    /**
     * Maps form parameters onto the MCP server bean. Custom mapping (rather than {@link #populate}) because checkbox fields are absent when unchecked.
     *
     * @param mcpServer
     *            The bean to populate
     * @param request
     *            The HTTP request
     */
    private void populateFromRequest( McpServer mcpServer, HttpServletRequest request )
    {
        mcpServer.setName( StringUtils.trimToNull( request.getParameter( PARAMETER_NAME ) ) );
        mcpServer.setDescription( StringUtils.trimToNull( request.getParameter( PARAMETER_DESCRIPTION ) ) );
        mcpServer.setTransportType( StringUtils.trimToNull( request.getParameter( PARAMETER_TRANSPORT_TYPE ) ) );
        mcpServer.setUrl( StringUtils.trimToNull( request.getParameter( PARAMETER_URL ) ) );
        mcpServer.setHeadersJson( StringUtils.trimToNull( request.getParameter( PARAMETER_HEADERS_JSON ) ) );
        mcpServer.setTimeoutMs( NumberUtils.toInt( request.getParameter( PARAMETER_TIMEOUT_MS ), DEFAULT_TIMEOUT_MS ) );
        mcpServer.setEnabled( request.getParameter( PARAMETER_ENABLED ) != null );
        mcpServer.setToolFilter( StringUtils.trimToNull( request.getParameter( PARAMETER_TOOL_FILTER ) ) );
        mcpServer.setLogRequests( request.getParameter( PARAMETER_LOG_REQUESTS ) != null );
    }
}
