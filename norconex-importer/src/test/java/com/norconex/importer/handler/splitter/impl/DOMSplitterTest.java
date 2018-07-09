/* Copyright 2015-2018 Norconex Inc.
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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.SplittableDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class DOMSplitterTest {

    @Test
    public void testHtmlDOMSplit()
            throws IOException, ImporterHandlerException {
        String html = ResourceLoader.getHtmlString(getClass());
        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("div.person");
        List<ImporterDocument> docs = split(html, splitter);

        Assert.assertEquals(3, docs.size());
        String content = TestUtil.getContentAsString(docs.get(2));
        Assert.assertTrue(content.contains("Dalton"));
    }


    @Test
    public void testXmlDOMSplit()
            throws IOException, ImporterHandlerException {

        String xml = ResourceLoader.getXmlString(getClass());

        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("person");
        List<ImporterDocument> docs = split(xml, splitter);

        Assert.assertEquals(3, docs.size());

        String content = TestUtil.getContentAsString(docs.get(2));
        Assert.assertTrue(content.contains("Dalton"));
    }

    private List<ImporterDocument> split(String text, DOMSplitter splitter)
            throws IOException, ImporterHandlerException {
        ImporterMetadata metadata = new ImporterMetadata();
        SplittableDocument doc = new SplittableDocument("n/a",
                IOUtils.toInputStream(text, StandardCharsets.UTF_8), metadata);
        CachedStreamFactory factory = new CachedStreamFactory(
                100 * 1024,  100 * 1024);
        List<ImporterDocument> docs = splitter.splitApplicableDocument(
                doc, new NullOutputStream(), factory, false);
        return docs;
    }

    @Test
    public void testWriteRead() throws IOException {
        DOMSplitter splitter = new DOMSplitter();
        splitter.setSelector("blah");
        splitter.addRestriction("key", "value", true);
        XML.assertWriteRead(splitter, "handler");
    }
}
