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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.ImporterHandlerException;

public class ReduceConsecutivesTransformerTest {

    private final String xml =
              "<handler ignoreCase=\"true\"><reduce>\\stext</reduce>"
            + "<reduce>\\t</reduce><reduce>\\n\\r</reduce>"
            + "<reduce>\\s</reduce><reduce>.</reduce></handler>";

    @Test
    public void testTransformTextDocument()
            throws ImporterHandlerException, IOException {
        String text = "\t\tThis is the text TeXt I want to modify...\n\r\n\r"
                + "     Too much space.";

        ReduceConsecutivesTransformer t = new ReduceConsecutivesTransformer();

        try (Reader reader = new InputStreamReader(
                IOUtils.toInputStream(xml, StandardCharsets.UTF_8))) {
            t.loadFromXML(new XML(reader));
        }

        try (InputStream is = IOUtils.toInputStream(
                text, StandardCharsets.UTF_8);
                ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            t.transformDocument(
                    "dummyRef", is, os, new Properties(), true);
            String response = os.toString();
//            System.out.println(response);
            Assertions.assertEquals(
                    "\tthis is the text i want to modify.\n\r too much space.",
                    response.toLowerCase());
        }
    }


    @Test
    public void testWriteRead() throws IOException {
        ReduceConsecutivesTransformer t = new ReduceConsecutivesTransformer();
        Reader reader = new InputStreamReader(
                IOUtils.toInputStream(xml, StandardCharsets.UTF_8));
        t.loadFromXML(new XML(reader));
        reader.close();
        XML.assertWriteRead(t, "handler");
    }
}
