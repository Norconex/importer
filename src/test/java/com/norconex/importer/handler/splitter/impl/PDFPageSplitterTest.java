/* Copyright 2018-2020 Norconex Inc.
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
import java.util.List;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

/**
 * @author Pascal Essiembre
 *
 */
public class PDFPageSplitterTest {

    private InputStream input;

    @BeforeEach
    public void setup() {
        input = PDFPageSplitterTest.class.getResourceAsStream(
                PDFPageSplitterTest.class.getSimpleName() + ".pdf");
    }
    @AfterEach
    public void tearDown() throws IOException {
        input.close();
    }

    @Test
    public void testSplit() throws ImporterHandlerException {
        PDFPageSplitter s = new PDFPageSplitter();
        List<Doc> pages = split(s);

        Assertions.assertEquals(3, pages.size(), "Invalid number of pages.");
        Assertions.assertEquals(1, getPageNo(pages.get(0)));
        Assertions.assertEquals(2, getPageNo(pages.get(1)));
        Assertions.assertEquals(3, getPageNo(pages.get(2)));
    }

    private int getPageNo(Doc doc) {
        return doc.getMetadata().getInteger(PDFPageSplitter.DOC_PDF_PAGE_NO);
    }

    @Test
        public void testWriteRead() {
        PDFPageSplitter splitter = new PDFPageSplitter();
        splitter.setReferencePagePrefix("#page");
        XML.assertWriteRead(splitter, "handler");
    }

    private List<Doc> split(PDFPageSplitter splitter)
            throws ImporterHandlerException {
        Properties metadata = new Properties();
        List<Doc> docs = splitter.splitApplicableDocument(
                TestUtil.toHandlerDoc("n/a", input, metadata),
                input, NullOutputStream.NULL_OUTPUT_STREAM, ParseState.PRE);
        return docs;
    }
}
