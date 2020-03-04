/* Copyright 2010-2020 Norconex Inc.
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
package com.norconex.importer.handler.tagger;

import java.io.InputStream;

import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

/**
 * <p>
 * Base class for taggers.
 * </p>
 * <p>
 * Subclasses inherit this {@link IXMLConfigurable} configuration:
 * </p>
 *
 * {@nx.xml
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 * }
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
@SuppressWarnings("javadoc")
public abstract class AbstractDocumentTagger extends AbstractImporterHandler
            implements IDocumentTagger {

    @Override
    public final void tagDocument(HandlerDoc doc, InputStream input,
            ParseState parseState) throws ImporterHandlerException {
        if (!isApplicable(doc, parseState)) {
            return;
        }
        tagApplicableDocument(doc, input, parseState);
    }

    protected abstract void tagApplicableDocument(
            HandlerDoc doc, InputStream input,
            ParseState parseState) throws ImporterHandlerException;
}