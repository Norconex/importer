/* Copyright 2020 Norconex Inc.
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
 */
public class XMLStreamSplitterTest {

    private final String sampleXML =
            " <animals>\n"
          + "   <species name=\"mouse\">\n"
          + "     <animal>\n"
          + "       <name>Itchy</name>\n"
          + "       <race>cartoon</race>\n"
          + "     </animal>\n"
          + "   </species>\n"
          + "   <species name=\"cat\">\n"
          + "     <animal>\n"
          + "       <name>Scratchy</name>\n"
          + "       <race>cartoon</race>\n"
          + "     </animal>\n"
          + "   </species>\n"
          + " </animals>";

    @Test
    public void testStreamSplit()
            throws ImporterHandlerException, IOException {

        XMLStreamSplitter splitter = new XMLStreamSplitter();
        splitter.setPath("/animals/species/animal");
        List<Doc> docs = split(sampleXML, splitter);

        Assertions.assertEquals(2, docs.size());
        String content = TestUtil.getContentAsString(docs.get(1));
        Assertions.assertTrue(content.contains("Scratchy"));
    }

    private List<Doc> split(String text, XMLStreamSplitter splitter)
            throws ImporterHandlerException {
        Properties metadata = new Properties();
        InputStream is = IOUtils.toInputStream(text, StandardCharsets.UTF_8);
        List<Doc> docs = splitter.splitApplicableDocument(
                TestUtil.toHandlerDoc("n/a", is, metadata),
                is, new NullOutputStream(), ParseState.PRE);
        return docs;
    }

    @Test
        public void testWriteRead() {
        XMLStreamSplitter splitter = new XMLStreamSplitter();
        splitter.setPath("blah");
        splitter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("key").partial(),
                TextMatcher.basic("value").partial().ignoreCase()));
        XML.assertWriteRead(splitter, "handler");
    }
}
