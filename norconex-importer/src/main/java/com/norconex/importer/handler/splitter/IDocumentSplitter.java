/* Copyright 2014 Norconex Inc.
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
package com.norconex.importer.handler.splitter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.xmlbeans.impl.xb.xsdschema.ImportDocument;

import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.IDocumentFilter;

/**
 * Responsible for splitting a single document into several ones.  The 
 * {@link ImporterDocument} instances returned by implementations will be
 * added as children of a parent {@link ImportDocument}.  Each children
 * will then be passed to this interface again for further splitting if
 * necessary.  Each document returned will also go through the same
 * pre-handler/parse/post-handler cycle as defined in the importer 
 * configuration.
 * <p/>
 * To blank values form a parent, you do not write to the output stream
 * and blank metadata values as desired.  The parent document will still get
 * processed as usual.  To prevent the parent from being processed
 * further, make sure to filter it out using an {@link IDocumentFilter}
 * implementation.
 * <p/>
 * If using the default importer parser, keep it mind you can configure it
 * to split most files with embedded content in them (zip, 
 * word processor document with embedded documents, etc).  A typical usage for 
 * this interface is 
 * to break a records-type document into a single document per record. For
 * example, to break some entities from XML data files into separate documents.
 * 
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public interface IDocumentSplitter extends IImporterHandler {

    List<ImporterDocument> splitDocument(String reference, InputStream input, 
            OutputStream output, ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException;
}
