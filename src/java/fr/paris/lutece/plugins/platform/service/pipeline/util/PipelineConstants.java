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
package fr.paris.lutece.plugins.platform.service.pipeline.util;

public final class PipelineConstants
{
    public static final String NODE_TYPE_START = "START";
    public static final String NODE_TYPE_END = "END";
    public static final String NODE_TYPE_CONDITION = "CONDITION";
    public static final String NODE_TYPE_VARIABLE_CREATOR = "VARIABLE_CREATOR";
    public static final String NODE_TYPE_RETRY_MANAGER = "RETRY_MANAGER";
    public static final String NODE_TYPE_MODEL = "MODEL";
    public static final String NODE_TYPE_SEMANTIC_SEARCH = "SEMANTIC_SEARCH";
    public static final String NODE_TYPE_VISION = "VISION";
    public static final String NODE_TYPE_DOCUMENT_PARSER = "DOCUMENT_PARSER";
    public static final String NODE_TYPE_EMAIL = "EMAIL";

    /**
     * Key of the outputs collection in an execution result.
     */
    public static final String KEY_OUTPUTS = "outputs";

    /**
     * Field holding the name of a single pipeline output.
     */
    public static final String KEY_OUTPUT_KEY = "outputKey";

    /**
     * Field holding the value of a single pipeline output.
     */
    public static final String KEY_OUTPUT_VALUE = "outputValue";

    /**
     * Sentinel output key marking a dynamically named output.
     */
    public static final String DYNAMIC_OUTPUT_KEY = "__dynamic__";

    /**
     * Config key naming the variable holding the value to evaluate (condition and evaluation nodes).
     */
    public static final String KEY_INPUT_KEY = "input_key";

    /**
     * Config key holding the condition expression (condition and evaluation nodes).
     */
    public static final String KEY_CONDITION = "condition";

    /**
     * Config key holding the direct input value (condition and evaluation nodes).
     */
    public static final String KEY_INPUT_VALUE = "input_value";

    /**
     * Private constructor
     */
    private PipelineConstants( )
    {
    }
}
