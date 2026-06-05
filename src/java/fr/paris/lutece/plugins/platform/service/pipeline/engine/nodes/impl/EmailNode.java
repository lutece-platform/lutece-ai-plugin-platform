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
package fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.service.pipeline.engine.PipelineContext;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.config.PipelineNodeConfig;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.AbstractPipelineNode;
import fr.paris.lutece.plugins.platform.service.pipeline.engine.nodes.PipelineNodeType;

import jakarta.enterprise.context.Dependent;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineConstants;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariable;
import fr.paris.lutece.plugins.platform.service.pipeline.util.PipelineVariableResolver;
import fr.paris.lutece.portal.service.mail.MailService;
import fr.paris.lutece.portal.service.util.AppLogService;

@Dependent
@PipelineNodeType( value = PipelineConstants.NODE_TYPE_EMAIL, name = "Email", description = "Envoie un email HTML à une liste de destinataires" )
public class EmailNode extends AbstractPipelineNode
{

    private static final String SUBJECT_KEY = "subject";
    private static final String SUBJECT_TITLE = "Objet";
    private static final String SUBJECT_DESCRIPTION = "Objet de l'email (supporte les variables {{nodeId.key}})";
    private static final String BODY_KEY = "body";
    private static final String BODY_TITLE = "Contenu";
    private static final String BODY_DESCRIPTION = "Corps de l'email en HTML (supporte les variables {{nodeId.key}})";
    private static final String RECIPIENTS_KEY = "recipients";
    private static final String RECIPIENTS_TITLE = "Destinataires";
    private static final String RECIPIENTS_DESCRIPTION = "Adresses email séparées par des ; (supporte les variables {{nodeId.key}})";
    private static final String SENDER_NAME = "Pipeline Agent";
    private static final String OUTPUT_STATUS_KEY = "status";
    private static final String OUTPUT_STATUS_DESCRIPTION = "Statut de l'envoi (success / error)";
    private static final String OUTPUT_COUNT_KEY = "recipients_count";
    private static final String OUTPUT_COUNT_DESCRIPTION = "Nombre de destinataires";
    private static final String OUTPUT_SUBJECT_KEY = "subject";
    private static final String OUTPUT_SUBJECT_DESCRIPTION = "Objet de l'email envoy\u00e9";
    private static final String OUTPUT_RECIPIENTS_KEY = "recipients";
    private static final String OUTPUT_RECIPIENTS_DESCRIPTION = "Destinataires de l'email";
    private static final String OUTPUT_BODY_KEY = "body_html";
    private static final String OUTPUT_BODY_DESCRIPTION = "Contenu HTML envoy\u00e9";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";
    private static final String RECIPIENTS_SEPARATOR = ";";
    private static final int BODY_ROWS = 8;
    private static final String LOG_SEND_ERROR = "Error while sending email in node %s: %s";

    public static final PipelineVariable SUBJECT = new PipelineVariable.Builder( SUBJECT_KEY ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( SUBJECT_TITLE ).description( SUBJECT_DESCRIPTION ).placeholder( "Objet du mail" ).build( );

    public static final PipelineVariable BODY = new PipelineVariable.Builder( BODY_KEY ).type( PipelineVariable.VariableType.STRING ).required( true )
            .title( BODY_TITLE ).description( BODY_DESCRIPTION ).textarea( true ).rows( BODY_ROWS ).build( );

    public static final PipelineVariable RECIPIENTS = new PipelineVariable.Builder( RECIPIENTS_KEY ).type( PipelineVariable.VariableType.STRING )
            .required( true ).title( RECIPIENTS_TITLE ).description( RECIPIENTS_DESCRIPTION ).textarea( true ).rows( 3 ).placeholder( "a@test.fr;b@test.fr" )
            .build( );

    /**
     * Constructor for EmailNode.
     */
    public EmailNode( )
    {
        super( PipelineConstants.NODE_TYPE_EMAIL );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipelineVariable> getConfigurableVariables( )
    {
        return Arrays.asList( SUBJECT, BODY, RECIPIENTS );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PipelineContext executeNode( PipelineContext context, PipelineNodeConfig config, Map<String, Object> resolvedData )
    {
        String subject = resolveTemplate( getParam( resolvedData, SUBJECT ), context );
        String body = resolveTemplate( getParam( resolvedData, BODY ), context );
        String recipients = resolveTemplate( getParam( resolvedData, RECIPIENTS ), context );
        String [ ] recipientArray = recipients.split( RECIPIENTS_SEPARATOR );

        Map<String, Object> outputData = new HashMap<>( );
        outputData.put( OUTPUT_COUNT_KEY, String.valueOf( recipientArray.length ) );
        outputData.put( OUTPUT_SUBJECT_KEY, subject );
        outputData.put( OUTPUT_RECIPIENTS_KEY, recipients );
        outputData.put( OUTPUT_BODY_KEY, body );

        try
        {
            String senderEmail = MailService.getNoReplyEmail( );
            MailService.sendMailHtml( recipients, SENDER_NAME, senderEmail, subject, body );
            outputData.put( OUTPUT_STATUS_KEY, STATUS_SUCCESS );
        }
        catch( Exception e )
        {
            AppLogService.error( String.format( LOG_SEND_ERROR, config.getId( ), e.getMessage( ) ), e );
            outputData.put( OUTPUT_STATUS_KEY, STATUS_ERROR );
        }

        prepareOutputData( context, config, outputData );
        return context;
    }

    /**
     * Resolves variable references in a template string.
     *
     * @param template
     *            the template string
     * @param context
     *            the pipeline context
     * @return the resolved string
     */
    private String resolveTemplate( String template, PipelineContext context )
    {
        return PipelineVariableResolver.resolve( template, context );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOutputKeys( )
    {
        Map<String, String> outputs = new LinkedHashMap<>( );
        outputs.put( OUTPUT_STATUS_KEY, OUTPUT_STATUS_DESCRIPTION );
        outputs.put( OUTPUT_COUNT_KEY, OUTPUT_COUNT_DESCRIPTION );
        outputs.put( OUTPUT_SUBJECT_KEY, OUTPUT_SUBJECT_DESCRIPTION );
        outputs.put( OUTPUT_RECIPIENTS_KEY, OUTPUT_RECIPIENTS_DESCRIPTION );
        outputs.put( OUTPUT_BODY_KEY, OUTPUT_BODY_DESCRIPTION );
        return outputs;
    }
}
