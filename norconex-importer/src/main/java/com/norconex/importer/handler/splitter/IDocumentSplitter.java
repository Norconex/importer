/* Copyright 2014 Norconex Inc.
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
package com.norconex.importer.handler.splitter;

import java.io.OutputStream;
import java.util.List;

import org.apache.xmlbeans.impl.xb.xsdschema.ImportDocument;

import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.importer.doc.ImporterDocument;
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
 * <br><br>
 * To blank values form a parent, you do not write to the output stream
 * and blank metadata values as desired.  The parent document will still get
 * processed as usual.  To prevent the parent from being processed
 * further, make sure to filter it out using an {@link IDocumentFilter}
 * implementation.
 * <br><br>
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

    List<ImporterDocument> splitDocument(
            SplittableDocument doc, 
            OutputStream docOutput,
            CachedStreamFactory streamFactory, 
            boolean parsed)
                    throws ImporterHandlerException;
}
