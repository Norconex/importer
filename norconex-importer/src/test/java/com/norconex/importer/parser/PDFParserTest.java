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
package com.norconex.importer.parser;

import java.io.IOException;

import org.junit.Test;

import com.norconex.importer.ImporterException;

public class PDFParserTest extends AbstractParserTest {

    private static final String PDF_FAMILY = "Portable Document Format (PDF)";

    @Test
    public void test_PDF_plain() throws IOException, ImporterException {
        testParsing("/parser/pdf/plain.pdf", "application/pdf", 
                DEFAULT_CONTENT_REGEX, "pdf", PDF_FAMILY);
    }

    @Test
    public void test_PDF_jpeg() throws IOException, ImporterException {
        testParsing("/parser/pdf/jpeg.pdf", "application/pdf", 
                ".*PDF with a JPEG image.*", "pdf", PDF_FAMILY, true);
    }

    @Test
    public void test_PDF_jbig2() 
            throws IOException, ImporterException {
        testParsing("/parser/pdf/jbig2.pdf", "application/pdf", 
                ".*test images compressed using JBIG2.*", "pdf", 
                        PDF_FAMILY, true);
    }
}
