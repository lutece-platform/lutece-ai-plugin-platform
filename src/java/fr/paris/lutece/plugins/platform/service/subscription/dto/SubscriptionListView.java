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
package fr.paris.lutece.plugins.platform.service.subscription.dto;

import java.util.List;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.client.Client;

/**
 * Typed view object returned by the subscription service when resolving the manage-subscriptions list. Carries the subscriptions to display together with the
 * optional client filter that was resolved (null when no valid client filter applied).
 */
public class SubscriptionListView
{
    private final Client _client;
    private final List<Subscription> _subscriptions;

    /**
     * Builds the view object.
     *
     * @param client
     *            the resolved client filter, or null when no client filter applied
     * @param subscriptions
     *            the subscriptions to display
     */
    public SubscriptionListView( Client client, List<Subscription> subscriptions )
    {
        _client = client;
        _subscriptions = subscriptions;
    }

    /**
     * Gets the resolved client filter.
     *
     * @return the client, or null when the full list is returned
     */
    public Client getClient( )
    {
        return _client;
    }

    /**
     * Gets the subscriptions to display.
     *
     * @return the subscription list
     */
    public List<Subscription> getSubscriptions( )
    {
        return _subscriptions;
    }
}
