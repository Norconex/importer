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
package com.norconex.importer.parser.impl.wordperfect;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.IDocumentParser;

/**
 * <p>Parser for Corel WordPerfect documents.</p>
 * @author Pascal Essiembre 
 * @since 2.1.0
 */
public class WordPerfectParser implements IDocumentParser {

    @Override
    public List<ImporterDocument> parseDocument(
            ImporterDocument doc, Writer output) 
                    throws DocumentParserException {
        
        WP6TextExtractor ex = new WP6TextExtractor();
        try {
            ex.extract(doc.getContent(), output);
        } catch (IOException e) {
            throw new DocumentParserException(
                    "Coult not parse WordPerfect document: "
                            + doc.getReference(), e);
        }
        return null;
    }
}
