/* Copyright 2015 Norconex Inc.
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
package com.norconex.importer.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;

/**
 * Wrapper class around an external program used to extract the text
 * from a file.
 * {@link org.apache.tika.parser.external.ExternalParser}.
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public class ExternalParser 
        extends org.apache.tika.parser.external.ExternalParser
        implements IDocumentParser {

    private static final long serialVersionUID = 3569996828422125700L;

    @Override
    public List<ImporterDocument> parseDocument(ImporterDocument doc,
            Writer output) throws DocumentParserException {
        Metadata tikaMetadata = new Metadata();
        if (doc.getContentType() == null) {
            throw new DocumentParserException(
                    "ImporterDocument must have a content-type.");
        }
        String contentType = doc.getContentType().toString();
        tikaMetadata.set(HttpHeaders.CONTENT_TYPE, contentType);
        tikaMetadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, 
                doc.getReference());
        tikaMetadata.set(Metadata.CONTENT_ENCODING, doc.getContentEncoding());

        ContentHandler handler = new BodyContentHandler(output);

        InputStream stream = doc.getContent();
        try {
            parse(stream, handler,  tikaMetadata, new ParseContext());
        } catch (IOException | SAXException | TikaException e) {
            throw new DocumentParserException(e);
        }
        return null;
    }
    
    @Override
    public void setSupportedTypes(Set<MediaType> supportedTypes) {
        throw new UnsupportedOperationException(
                "Cannot set supported types this way.");
    }
}
