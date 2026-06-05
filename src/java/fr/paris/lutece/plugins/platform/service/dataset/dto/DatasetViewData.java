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
package fr.paris.lutece.plugins.platform.service.dataset.dto;

import java.util.List;
import java.util.Map;

import fr.paris.lutece.plugins.platform.business.dataset.DatasetDocument;
import fr.paris.lutece.plugins.platform.business.dataset.DatasetFolder;

/**
 * Aggregated view data for the dataset detail page: the documents and subfolders of the currently browsed folder, the resolved current folder, the breadcrumb
 * trail and the per-document chunk counts and file sizes. Produced in one pass by the service so the controller only assembles the model.
 *
 * @param currentFolder
 *            The folder being browsed, or null at the dataset root
 * @param documents
 *            The documents directly under the current folder
 * @param subfolders
 *            The immediate subfolders of the current folder
 * @param breadcrumb
 *            The breadcrumb trail from root to the current folder
 * @param documentChunks
 *            The per-document chunk (segment) counts, keyed by document id as string
 * @param documentFileSizes
 *            The per-document file sizes in bytes, keyed by document id as string
 */
public record DatasetViewData(DatasetFolder currentFolder, List<DatasetDocument> documents, List<DatasetFolder> subfolders, List<BreadcrumbEntry> breadcrumb,
        Map<String, Long> documentChunks, Map<String, Integer> documentFileSizes) {
}
