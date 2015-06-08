/* Copyright 2010-2015 Norconex Inc.
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

import java.util.Map;

import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.EnhancedPDFParser;


/**
 * Parser using auto-detection of document content-type to figure out
 * which specific parser to invoke to best parse a document.  
 * Use this class only when you
 * do not know the content-type of a document to be imported.  
 * @author Pascal Essiembre
 */
public class FallbackParser extends AbstractTikaParser {

    /**
     * Creates a new parser.
     */
    public FallbackParser() {
        super(createAutoDetectParser());
    }

    private static AutoDetectParser createAutoDetectParser() {
        AutoDetectParser parser = new AutoDetectParser();
        Map<MediaType, Parser> parsers = parser.getParsers();
        
        EnhancedPDFParser pdfParser = new EnhancedPDFParser();
        pdfParser.setPreserveMemory(true);
        parsers.put(MediaType.application("pdf"), pdfParser);
        //Tika returns a defensive copy so we have to reset the whole thing
        parser.setParsers(parsers);
        return parser;
    }
}
