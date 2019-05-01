/* Copyright 2010-2019 Norconex Inc.
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
package com.norconex.importer.handler.transformer.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class StripBetweenTransformerTest {

    @Test
    public void testTransformTextDocument()
            throws IOException, ImporterHandlerException {
        StripBetweenTransformer t = new StripBetweenTransformer();
        t.addStripEndpoints("<h2>", "</h2>");
        t.addStripEndpoints("<P>", "</P>");
        t.addStripEndpoints("<head>", "</hEad>");
        t.addStripEndpoints("<Pre>", "</prE>");
        t.setCaseSensitive(false);
        t.setInclusive(true);
        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.set(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.transformDocument(
                htmlFile.getAbsolutePath(),
                is, os, metadata, false);
//        System.out.println(os.toString());

        Assertions.assertEquals(458, os.toString().length(),
                "Length of doc content after transformation is incorrect.");

        is.close();
        os.close();
    }


    @Test
    public void testCollectorHttpIssue237()
            throws IOException, ImporterHandlerException {
        StripBetweenTransformer t = new StripBetweenTransformer();
        t.addStripEndpoints("<body>", "<\\!-- START -->");
        t.addStripEndpoints("<\\!-- END -->", "<\\!-- START -->");
        t.addStripEndpoints("<\\!-- END -->", "</body>");

        t.setCaseSensitive(false);
        t.setInclusive(true);

        String html = "<html><body>"
                + "ignore this text"
                + "<!-- START -->extract me 1<!-- END -->"
                + "ignore this text"
                + "<!-- START -->extract me 2<!-- END -->"
                + "ignore this text"
                + "</body></html>";

        ByteArrayInputStream is = new ByteArrayInputStream(html.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.set(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.transformDocument("fake.html", is, os, metadata, false);
        String output = os.toString();
        is.close();
        os.close();
        //System.out.println(output);
        Assertions.assertEquals(
                "<html>extract me 1extract me 2</html>", output);
    }


    @Test
    public void testWriteRead() throws IOException {
        StripBetweenTransformer t = new StripBetweenTransformer();
        t.setInclusive(true);
        t.addStripEndpoints("<!-- NO INDEX", "/NOINDEX -->");
        t.addStripEndpoints("<!-- HEADER START", "HEADER END -->");
        XML.assertWriteRead(t, "handler");
    }
}
