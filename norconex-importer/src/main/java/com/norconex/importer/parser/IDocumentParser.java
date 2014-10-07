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
package com.norconex.importer.parser;

import java.io.Writer;
import java.util.List;

import com.norconex.importer.doc.ImporterDocument;

/**
 * Implementations are responsible for parsing a document to 
 * extract its text and metadata, as well as any embedded documents.
 * @author Pascal Essiembre
 * @see IDocumentParserFactory
 */
@SuppressWarnings("nls")
public interface IDocumentParser {

    /**
     * Parses a document.
     * @param doc importer document to parse
     * @param output where to store extracted or modified content of the 
     *        supplied document
     * @return a list of first-level embedded documents, if any
     * @throws DocumentParserException
     */
    List<ImporterDocument> parseDocument(
            ImporterDocument doc, Writer output) throws DocumentParserException;
}
