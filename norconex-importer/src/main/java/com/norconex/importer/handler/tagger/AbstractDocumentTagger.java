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

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.importer.handler.AbstractImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;

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
    public final void tagDocument(String reference, InputStream document,
            Properties metadata, boolean parsed)
                    throws ImporterHandlerException {
        if (!isApplicable(reference, metadata, parsed)) {
            return;
        }
        tagApplicableDocument(reference, document, metadata, parsed);
    }

    protected abstract void tagApplicableDocument(
            String reference, InputStream document,
            Properties metadata, boolean parsed)
                    throws ImporterHandlerException;
}