/* Copyright 2014-2020 Norconex Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.SplittableDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class CsvSplitterTest {

    private InputStream input;

    @BeforeEach
    public void setup() throws IOException {
        input = CsvSplitterTest.class.getResourceAsStream(
                 CsvSplitterTest.class.getSimpleName() + ".csv");
    }

    @AfterEach
    public void tearDown() throws IOException {
        input.close();
    }

    @Test
    public void testReferenceColumnByName()
            throws IOException, ImporterHandlerException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        splitter.setReferenceColumn("clientPhone");
        List<Doc> docs = split(splitter);
        Assertions.assertEquals(
                "654-0987", docs.get(2).getMetadata().getString(
                        DocMetadata.EMBEDDED_REFERENCE),
                "Could not find embedded William Dalton phone reference.");
    }

    @Test
    public void testReferenceColumnByPosition()
            throws IOException, ImporterHandlerException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(false);
        splitter.setReferenceColumn("2");
        List<Doc> docs = split(splitter);
        Assertions.assertEquals("William Dalton",
                docs.get(3).getMetadata().getString(
                        DocMetadata.EMBEDDED_REFERENCE),
                "Could not find embedded William Dalton reference.");
    }


    @Test
    public void testContentColumn()
            throws ImporterHandlerException, IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        splitter.setContentColumns("clientName", "3");

        List<Doc> docs = split(splitter);
        Assertions.assertEquals("William Dalton 654-0987",
                IOUtils.toString(docs.get(2).getInputStream(),
                        StandardCharsets.UTF_8));
    }


    @Test
    public void testFirstRowHeader()
            throws ImporterHandlerException, IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        List<Doc> docs = split(splitter);

        Assertions.assertEquals(4, docs.size(),
                "Invalid number of docs returned.");

        Assertions.assertEquals("William Dalton",
                docs.get(2).getMetadata().getString("clientName"),
                "Could not find William Dalton by column name.");
    }

    private List<Doc> split(CsvSplitter splitter)
            throws IOException, ImporterHandlerException {
        Properties metadata = new Properties();
        SplittableDocument doc = new SplittableDocument("n/a", input, metadata);

        CachedStreamFactory factory = new CachedStreamFactory(
                100 * 1024,  100 * 1024);

        List<Doc> docs = splitter.splitApplicableDocument(
                doc, new NullOutputStream(), factory, false);
        return docs;

    }

    @Test
    public void testWriteRead() throws IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setEscapeCharacter('.');
        splitter.setLinesToSkip(10);
        splitter.setQuoteCharacter('!');
        splitter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("key").partial(),
                TextMatcher.basic("value").partial().ignoreCase()));
        splitter.setSeparatorCharacter('@');
        splitter.setUseFirstRowAsFields(true);
        XML.assertWriteRead(splitter, "handler");
    }
}
