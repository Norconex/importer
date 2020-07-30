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
package com.norconex.importer.handler.tagger.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

public class TitleGeneratorTaggerTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(TitleGeneratorTaggerTest.class);

    // Test for: https://github.com/Norconex/importer/issues/74
    @Test
    public void testNullFromField()
            throws ImporterHandlerException {

        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setFromField("nullField");
        t.setDetectHeading(true);

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/plain");
        TestUtil.tag(t, "test.txt", metadata, ParseState.POST);

        Assertions.assertNull(
                metadata.getString(DocMetadata.GENERATED_TITLE),
                "Title should be null");
    }

    @Test
    public void testSummarizeTitle()
            throws ImporterHandlerException, IOException {

        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setToField("mytitle");

        File file = TestUtil.getAliceTextFile();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/plain");
        t.tagDocument(TestUtil.toHandlerDoc(
                file.getAbsolutePath(), is, metadata), is, ParseState.POST);
        is.close();

        String title = metadata.getString("mytitle");

        LOG.debug("TITLE IS: " + title);
        Assertions.assertEquals(
                "that Alice had begun to think that very few things "
              + "indeed were really impossible.",  title,
              "Wrong title.");
    }

    @Test
    public void testHeadingTitle()
            throws ImporterHandlerException, IOException {
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setDetectHeading(true);
        t.setDetectHeadingMinLength(5);

        File file = TestUtil.getAliceTextFile();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/plain");
        t.tagDocument(TestUtil.toHandlerDoc(
                file.getAbsolutePath(), is, metadata), is, ParseState.POST);
        is.close();

        String title = metadata.getString(DocMetadata.GENERATED_TITLE);

        LOG.debug("TITLE IS: " + title);
        Assertions.assertEquals("Chapter I",  title, "Wrong title.");
    }

    @Test
    public void testFallbackTitle()
            throws ImporterHandlerException, IOException {
        TitleGeneratorTagger t = new TitleGeneratorTagger();

        InputStream is = new ByteArrayInputStream(
                "This is the first line. This is another line.".getBytes());

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/plain");

        t.tagDocument(TestUtil.toHandlerDoc(
                "test.txt", is, metadata), is, ParseState.POST);
        is.close();

        String title = metadata.getString(DocMetadata.GENERATED_TITLE);

        LOG.debug("TITLE IS: {}", title);
        Assertions.assertEquals(
                "This is the first line.",  title, "Wrong title.");
    }


    @Test
    public void testWriteRead() {
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setFromField("potato");
        t.setToField("banana");
        t.setOnSet(PropertySetter.APPEND);
        t.setTitleMaxLength(300);
        t.setDetectHeading(true);
        t.setDetectHeadingMaxLength(200);
        t.setDetectHeadingMinLength(20);
        XML.assertWriteRead(t, "handler");
    }
}
