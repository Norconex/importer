/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.importer.handler.tagger;

import java.io.InputStream;

import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * Tags a document with extra metadata information, or manipulate existing
 * metadata information.
 * @author Pascal Essiembre
 */
public interface IDocumentTagger extends IImporterHandler {

    /**
     * Tags a document with extra metadata information.
     * @param reference document reference (e.g. URL)
     * @param document document
     * @param metadata document metadata
     * @param parsed whether the document has been parsed already or not (a 
     *        parsed document should normally be text-based)
     * @throws ImporterHandlerException problem tagging the document
     */
    void tagDocument(String reference, InputStream document, 
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException;
}
