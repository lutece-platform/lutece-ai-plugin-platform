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
package fr.paris.lutece.plugins.platform.service.observability.dto;

import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.subscription.Subscription;
import fr.paris.lutece.plugins.platform.business.resource.IPlatformResourceType;
import fr.paris.lutece.plugins.platform.business.resource.PlatformResourceItem;

/**
 * Typed view model for a client's resource overview. Carries the resource types, the stats-enriched resources grouped by type, the client's subscriptions and
 * the subscriptions index (resource type to resource id to subscription) so the controller no longer has to fold and enrich these collections itself.
 */
public class ClientResourcesView
{
    private final List<IPlatformResourceType> _resourceTypes;
    private final Map<String, List<PlatformResourceItem>> _resourcesByType;
    private final List<Subscription> _subscriptions;
    private final Map<String, Map<String, Subscription>> _subscriptionsByTypeAndId;

    /**
     * Builds a client resources view.
     *
     * @param resourceTypes
     *            the available resource types
     * @param resourcesByType
     *            the stats-enriched resources grouped by resource type
     * @param subscriptions
     *            the client's subscriptions
     * @param subscriptionsByTypeAndId
     *            the subscriptions index, by resource type then resource id
     */
    public ClientResourcesView( List<IPlatformResourceType> resourceTypes, Map<String, List<PlatformResourceItem>> resourcesByType,
            List<Subscription> subscriptions, Map<String, Map<String, Subscription>> subscriptionsByTypeAndId )
    {
        _resourceTypes = resourceTypes;
        _resourcesByType = resourcesByType;
        _subscriptions = subscriptions;
        _subscriptionsByTypeAndId = subscriptionsByTypeAndId;
    }

    /**
     * Gets the available resource types.
     *
     * @return the resource types
     */
    public List<IPlatformResourceType> getResourceTypes( )
    {
        return _resourceTypes;
    }

    /**
     * Gets the stats-enriched resources grouped by resource type.
     *
     * @return the resources grouped by type
     */
    public Map<String, List<PlatformResourceItem>> getResourcesByType( )
    {
        return _resourcesByType;
    }

    /**
     * Gets the client's subscriptions.
     *
     * @return the subscriptions
     */
    public List<Subscription> getSubscriptions( )
    {
        return _subscriptions;
    }

    /**
     * Gets the subscriptions index, by resource type then resource id.
     *
     * @return the subscriptions index
     */
    public Map<String, Map<String, Subscription>> getSubscriptionsByTypeAndId( )
    {
        return _subscriptionsByTypeAndId;
    }
}
