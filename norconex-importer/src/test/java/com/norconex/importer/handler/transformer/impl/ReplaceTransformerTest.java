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
package com.norconex.importer.handler.transformer.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.ImporterHandlerException;

public class ReplaceTransformerTest {

    private final String restrictionTestConfig =
          "<handler>"
        + "<replace><valueMatcher ignoreCase=\"true\" partial=\"true\">"
        + "CAKES</valueMatcher>"
        + "<toValue>FRUITS</toValue></replace>"
        + "<replace><valueMatcher ignoreCase=\"true\" partial=\"true\">"
        + "candies</valueMatcher>"
        + "<toValue>vegetables</toValue></replace>"
        + "<restrictTo><fieldMatcher ignoreCase=\"true\">"
        + "document.reference</fieldMatcher>"
        + "<valueMatcher ignoreCase=\"true\" partial=\"true\" method=\"regex\">"
        + ".*test.*</valueMatcher>"
        + "</restrictTo>"
        + "</handler>";
    private final String restrictionTestContent =
            "I like to eat cakes and candies.";

    @Test
    public void testReplaceEolWithWhiteSpace()
            throws ImporterHandlerException, IOException {
        String input = "line1\r\nline2\rline3\nline4";
        String expectedOutput = "line1 line2 line3 line4";

        String preserveTestConfig =
                "<handler>"
                + "<replace><valueMatcher method=\"regex\" ignoreCase=\"true\" "
                + "partial=\"true\" replaceAll=\"true\">"
                + "[\\r\\n]+</valueMatcher>"
                + "<toValue> </toValue></replace>"
                + "</handler>";
        String response1 = transformTextDocument(
                preserveTestConfig, "N/A", input);
        Assertions.assertEquals(expectedOutput, response1);
    }

    @Test
    public void testTransformRestrictedTextDocument()
            throws IOException, ImporterHandlerException {
        String response = transformTextDocument(
                restrictionTestConfig, "rejectme.html", restrictionTestContent);
        Assertions.assertEquals(StringUtils.EMPTY, response.toLowerCase());
    }

    @Test
    public void testTransformUnrestrictedTextDocument()
            throws IOException, ImporterHandlerException {
        String response = transformTextDocument(
                restrictionTestConfig, "test.html", restrictionTestContent);
        Assertions.assertEquals(
                "i like to eat fruits and vegetables.",
                response.toLowerCase());
    }

    private String transformTextDocument(
            String config, String reference, String content)
            throws IOException, ImporterHandlerException {

        ReplaceTransformer t = new ReplaceTransformer();

        Reader reader = new InputStreamReader(
                IOUtils.toInputStream(config, StandardCharsets.UTF_8));
        t.loadFromXML(new XML(reader));
        reader.close();

        InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Properties metadata = new Properties();
        metadata.set("document.reference", reference);

        t.transformDocument(reference, is, os, metadata, true);

        String response = os.toString();
        is.close();
        os.close();
        return response;
    }


    @Test
    public void testWriteRead() throws IOException {
        ReplaceTransformer t = new ReplaceTransformer();
        t.setMaxReadSize(128);
        Reader reader = new InputStreamReader(IOUtils.toInputStream(
                restrictionTestConfig, StandardCharsets.UTF_8));
        t.loadFromXML(new XML(reader));
        reader.close();
        XML.assertWriteRead(t, "handler");
    }
}
