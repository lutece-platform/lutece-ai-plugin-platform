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
package fr.paris.lutece.plugins.platform.service.pipeline;

/**
 * Constants for pipeline event types and payload keys used in pipeline processing. This class provides standardized event type identifiers and payload key
 * names for pipeline lifecycle events, node operations, and job management.
 *
 * @since 1.0
 */
public final class PipelineEventTypes
{

    private static final String PIPELINE_PREFIX = "PIPELINE_";
    private static final String NODE_PREFIX = "PIPELINE_NODE_";
    private static final String JOB_PREFIX = "PIPELINE_JOB_";

    public static final String PIPELINE_START = PIPELINE_PREFIX + "START";
    public static final String PIPELINE_COMPLETE = PIPELINE_PREFIX + "COMPLETE";
    public static final String PIPELINE_FAILED = PIPELINE_PREFIX + "FAILED";
    public static final String NODE_START = NODE_PREFIX + "START";
    public static final String NODE_SUCCESS = NODE_PREFIX + "SUCCESS";
    public static final String NODE_FAILED = NODE_PREFIX + "FAILED";
    public static final String NODE_STREAM = NODE_PREFIX + "STREAM";
    public static final String PORT_CHOSEN = PIPELINE_PREFIX + "PORT_CHOSEN";
    public static final String JOB_QUEUED = JOB_PREFIX + "QUEUED";
    public static final String JOB_ACTIVATED = JOB_PREFIX + "ACTIVATED";
    public static final String JOB_POSITION_CHANGED = JOB_PREFIX + "POSITION_CHANGED";
    public static final String TOKEN_USAGE_RECORDED = PIPELINE_PREFIX + "TOKEN_USAGE_RECORDED";

    /**
     * Constants for payload keys used in pipeline events. These keys are used to access specific data within event payloads.
     */
    public static final class PayloadKeys
    {

        private static final String ID_SUFFIX = "Id";
        private static final String TOKENS_SUFFIX = "Tokens";

        public static final String JOB_ID = "job" + ID_SUFFIX;
        public static final String PIPELINE_ID = "pipeline" + ID_SUFFIX;
        public static final String NODE_ID = "node" + ID_SUFFIX;
        public static final String INPUT_TOKENS = "input" + TOKENS_SUFFIX;
        public static final String OUTPUT_TOKENS = "output" + TOKENS_SUFFIX;
        public static final String PROVIDER_ID = "provider" + ID_SUFFIX;

        /**
         * Private constructor to prevent instantiation of utility class.
         */
        private PayloadKeys( )
        {
        }
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PipelineEventTypes( )
    {
    }
}
