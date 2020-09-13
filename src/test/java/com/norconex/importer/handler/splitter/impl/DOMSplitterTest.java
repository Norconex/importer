/* Copyright 2015-2020 Norconex Inc.
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
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

/**
 * @author Pascal Essiembre
 *
 */
public class DOMSplitterTest {

    @Test
    public void testHtmlDOMSplit()
            throws ImporterHandlerException, IOException {
        String html = ResourceLoader.getHtmlString(getClass());
        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("div.person");
        List<Doc> docs = split(html, splitter);

        Assertions.assertEquals(3, docs.size());
        String content = TestUtil.getContentAsString(docs.get(2));
        Assertions.assertTrue(content.contains("Dalton"));
    }


    @Test
    public void testXmlDOMSplit()
            throws ImporterHandlerException, IOException {

        String xml = ResourceLoader.getXmlString(getClass());

        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("person");
        List<Doc> docs = split(xml, splitter);

        Assertions.assertEquals(3, docs.size());

        String content = TestUtil.getContentAsString(docs.get(2));
        Assertions.assertTrue(content.contains("Dalton"));
    }

    private List<Doc> split(String text, DOMSplitter splitter)
            throws ImporterHandlerException {
        Properties metadata = new Properties();
        InputStream is = IOUtils.toInputStream(text, StandardCharsets.UTF_8);
        List<Doc> docs = splitter.splitApplicableDocument(
                TestUtil.toHandlerDoc("n/a", is, metadata),
                is, NullOutputStream.NULL_OUTPUT_STREAM, ParseState.PRE);
        return docs;
    }

    @Test
        public void testWriteRead() {
        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("blah");
        splitter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("key").partial(),
                TextMatcher.basic("value").partial().ignoreCase()));
        XML.assertWriteRead(splitter, "handler");
    }
}
