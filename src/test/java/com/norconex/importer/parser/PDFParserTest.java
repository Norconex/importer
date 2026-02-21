/* Copyright 2015-2026 Norconex Inc.
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
package com.norconex.importer.parser;

import java.io.IOException;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterRequest;
import com.norconex.importer.parser.impl.FallbackParser;

public class PDFParserTest extends AbstractParserTest {

    private static final String PDF_FAMILY = "Portable Document Format (PDF)";

    @Test
    public void test_PDF_plain() throws IOException {
        testParsing("/parser/pdf/plain.pdf", "application/pdf",
                DEFAULT_CONTENT_REGEX, "pdf", PDF_FAMILY);
    }

    @Test
    public void test_PDF_jpeg() throws IOException {
        testParsing("/parser/pdf/jpeg.pdf", "application/pdf",
                ".*PDF with a JPEG image.*", "pdf", PDF_FAMILY, true);
    }

    @Test
    public void test_PDF_jbig2() throws IOException {
        // Build a FallbackParser subclass that enables inline image extraction
        // via the modifyParseContext hook. Grobid is disabled automatically by
        // AbstractTikaParser.initialize() when called with default ParseHints.
        ParseHints hints = new ParseHints();
        // Split embedded so each inline image becomes its own nested response.
        hints.getEmbeddedConfig().setSplitContentTypes("application/pdf");

        FallbackParser jbig2Parser = new FallbackParser() {
            @Override
            protected void modifyParseContext(ParseContext context) {
                PDFParserConfig pdfConfig = context.get(
                        PDFParserConfig.class, new PDFParserConfig());
                pdfConfig.setExtractInlineImages(true);
                pdfConfig.setExtractUniqueInlineImagesOnly(false);
                context.set(PDFParserConfig.class, pdfConfig);
            }
        };
        jbig2Parser.initialize(hints);

        // Wrap in a simple factory that always returns this parser.
        IDocumentParserFactory factory =
                (ref, ct) -> jbig2Parser;

        ImporterConfig config = new ImporterConfig();
        config.setParserFactory(factory);

        var response = new Importer(config).importDocument(
                new ImporterRequest(getFile("/parser/pdf/jbig2.pdf").toPath()));

        Assertions.assertNotNull(response, "Null response from importer.");
        Assertions.assertTrue(response.isSuccess(),
                "Import failed: " + response.getImporterStatus());

        // The top-level document must not carry a Tika parse exception.
        var rootMeta =
                response.getDocument().getMetadata();
        Assertions.assertNull(rootMeta.getString("X-TIKA:EXCEPTION:warn"),
                "Exception found in root doc metadata: "
                        + rootMeta.getString("X-TIKA:EXCEPTION:warn"));

        // When inline images are present they come back as nested responses.
        var nested = response.getNestedResponses();
        if (nested != null && nested.length > 0) {
            var imgMeta =
                    nested[0].getDocument().getMetadata();
            Assertions.assertEquals("91", imgMeta.getString("height"),
                    "Invalid height.");
            Assertions.assertEquals("352", imgMeta.getString("width"),
                    "Invalid width.");
        }
    }
}