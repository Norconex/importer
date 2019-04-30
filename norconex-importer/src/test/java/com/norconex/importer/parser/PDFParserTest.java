/* Copyright 2015-2016 Norconex Inc.
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
import java.io.InputStream;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

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
            throws IOException, ImporterException, SAXException, TikaException {
        RecursiveParserWrapperHandler h = new RecursiveParserWrapperHandler(
                new BasicContentHandlerFactory(BasicContentHandlerFactory
                        .HANDLER_TYPE.IGNORE, -1));

        RecursiveParserWrapper p = new RecursiveParserWrapper(new AutoDetectParser());
        ParseContext context = new ParseContext();
        PDFParserConfig config = new PDFParserConfig();
        config.setExtractInlineImages(true);
        config.setExtractUniqueInlineImagesOnly(false);
        context.set(PDFParserConfig.class, config);
        context.set(Parser.class, p);

        try (InputStream stream = getInputStream("/parser/pdf/jbig2.pdf")) {
            p.parse(stream, h, new Metadata(), context);
        }
        List<Metadata> metadatas = h.getMetadataList();

        Assert.assertNull("Exception found: " + metadatas.get(0).get(
                "X-TIKA:EXCEPTION:warn"), metadatas.get(0).get(
                        "X-TIKA:EXCEPTION:warn"));
        Assert.assertEquals(
                "Invalid height.", "91", metadatas.get(1).get("height"));
        Assert.assertEquals(
                "Invalid width.", "352", metadatas.get(1).get("width"));

//        System.out.println("OUTPUT:" + output);
//        System.out.println("METADATA:" + metadatas.get(1));

    }
}
