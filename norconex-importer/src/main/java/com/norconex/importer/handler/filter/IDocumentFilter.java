/* Copyright 2010-2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
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
