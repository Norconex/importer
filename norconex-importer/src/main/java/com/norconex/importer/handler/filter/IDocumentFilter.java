/* Copyright 2010-2014 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.filter;

import java.io.InputStream;

import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Filters documents.  Rejected documents are no longer processed.
 * @author Pascal Essiembre
 */
public interface IDocumentFilter extends IImporterHandler {

    /**
     * Whether to accepts a document.
     * @param reference document reference
     * @param document the document to evaluate
     * @param metadata document metadata
     * @param parsed whether the document has been parsed already or not (a 
     *        parsed document should normally be text-based)
     * @return <code>true</code> if document is accepted
     * @throws ImporterHandlerException problem reading the document
     */
    boolean acceptDocument(String reference,
            InputStream document, ImporterMetadata metadata, boolean parsed)
        throws ImporterHandlerException;

   //TODO have a RejectionCause returned instead of boolean?

}
