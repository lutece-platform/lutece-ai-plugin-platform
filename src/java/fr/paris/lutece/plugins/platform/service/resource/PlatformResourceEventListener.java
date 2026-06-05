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
package fr.paris.lutece.plugins.platform.service.resource;

import java.util.List;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.service.subscription.SubscriptionService;
import fr.paris.lutece.portal.business.event.ResourceEvent;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.event.EventAction;
import fr.paris.lutece.portal.service.event.Type;
import fr.paris.lutece.portal.service.util.AppLogService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Listener for platform resource events that manages subscription cleanup when resources are deleted.
 */
@ApplicationScoped
public class PlatformResourceEventListener
{
    @Inject
    private SubscriptionService _subscriptionService;

    /**
     * Handles resource deletion events by removing all associated subscriptions. Each unsubscription failure is logged and skipped so one failure does not
     * leave the remaining subscriptions orphaned.
     *
     * @param event
     *            The resource event containing deletion details
     */
    public void deletedResource( @Observes @Type( EventAction.REMOVE ) ResourceEvent event )
    {
        List<Subscription> subscriptions = _subscriptionService.getResourceSubscriptions( event.getTypeResource( ), event.getIdResource( ) );

        for ( Subscription subscription : subscriptions )
        {
            try
            {
                _subscriptionService.unsubscribe( subscription.getId( ) );
            }
            catch( AccessDeniedException e )
            {
                AppLogService.error( "Error unsubscribing subscription {} during resource deletion", subscription.getId( ), e );
            }
        }
    }
}
