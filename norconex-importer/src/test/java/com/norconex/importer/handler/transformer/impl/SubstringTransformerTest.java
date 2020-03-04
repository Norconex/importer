/* Copyright 2017-2020 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.ImporterException;
import com.norconex.importer.TestUtil;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

public class SubstringTransformerTest {

    @Test
    public void testTransformTextDocument()
            throws ImporterHandlerException {
        String content = "1234567890";

        Assertions.assertEquals("", substring(0, 0, content));
        Assertions.assertEquals(content, substring(-1, -1, content));
        Assertions.assertEquals("123", substring(0, 3, content));
        Assertions.assertEquals("456", substring(3, 6, content));
        Assertions.assertEquals("890", substring(7, 42, content));
        Assertions.assertEquals("1234", substring(-1, 4, content));
        Assertions.assertEquals("7890", substring(6, -1, content));
        try {
            substring(4, 1, content);
            Assertions.fail("Should have triggered an exception.");
        } catch (ImporterException e) {
        }
    }

    private String substring(long begin, long end, String content)
            throws ImporterHandlerException {
        InputStream input = new ByteArrayInputStream(content.getBytes());
        SubstringTransformer t = new SubstringTransformer();
        t.setBegin(begin);
        t.setEnd(end);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        t.transformDocument(
                TestUtil.toHandlerDoc("N/A", input, new Properties()),
                input, output, ParseState.PRE);
        return new String(output.toByteArray());
    }

    @Test
    public void testWriteRead() {
        SubstringTransformer t = new SubstringTransformer();
        t.setBegin(1000);
        t.setEnd(5000);
        XML.assertWriteRead(t, "handler");
    }
}
