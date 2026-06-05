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
package fr.paris.lutece.plugins.platform.service.model.dto;

import fr.paris.lutece.plugins.platform.business.model.Model;

/**
 * Typed outcome of a model creation attempt. Either carries the freshly created model ({@link ModelCreateStatus#CREATED}) or signals that a model already
 * exists for the same client and provider ({@link ModelCreateStatus#DUPLICATE}), so the controller can map the status to an i18n message without re-running the
 * uniqueness check itself.
 *
 * @param status
 *            the creation status
 * @param model
 *            the created model when status is CREATED, {@code null} otherwise
 */
public record ModelCreateResult(ModelCreateStatus status, Model model) {
    /**
     * Possible outcomes of a model creation attempt.
     */
    public enum ModelCreateStatus
    {
        /** The model was successfully created. */
        CREATED,
        /** A model already exists for the same client and provider. */
        DUPLICATE
    }

    /**
     * Builds a successful creation result.
     *
     * @param model
     *            the created model
     * @return a CREATED result wrapping the given model
     */
    public static ModelCreateResult created( Model model )
    {
        return new ModelCreateResult( ModelCreateStatus.CREATED, model );
    }

    /**
     * Builds a duplicate creation result.
     *
     * @return a DUPLICATE result with no model
     */
    public static ModelCreateResult duplicate( )
    {
        return new ModelCreateResult( ModelCreateStatus.DUPLICATE, null );
    }
}
