/* Copyright 2015-2017 Norconex Inc.
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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class TitleGeneratorTest {

    private static final Logger LOG = 
            LogManager.getLogger(TitleGeneratorTest.class);
    
//    @Before
//    public void before() {
//        Logger logger = Logger.getRootLogger();
//        logger.setLevel(Level.INFO);
//        logger.setAdditivity(false);
//        logger.addAppender(new ConsoleAppender(
//                new PatternLayout("%-5p [%C{1}] %m%n"), 
//                ConsoleAppender.SYSTEM_OUT));
//    }
    
    @Test
    public void testSummarizeTitle() 
            throws IOException, ImporterHandlerException {
        
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setToField("mytitle");
        
        File file = TestUtil.getAliceTextFile();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/plain");
        t.tagDocument(file.getAbsolutePath(), is, metadata, true);

        is.close();

        String title = metadata.getString("mytitle");
        
        LOG.debug("TITLE IS: " + title);        
        Assert.assertEquals("Wrong title.", 
                "that Alice had begun to think that very few things "
              + "indeed were really impossible.",  title);
    }
    
    @Test
    public void testHeadingTitle() 
            throws IOException, ImporterHandlerException {
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setDetectHeading(true);
        t.setDetectHeadingMinLength(5);
        
        File file = TestUtil.getAliceTextFile();
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/plain");
        t.tagDocument(file.getAbsolutePath(), is, metadata, true);

        is.close();

        String title = metadata.getString(ImporterMetadata.DOC_GENERATED_TITLE);
        
        LOG.debug("TITLE IS: " + title);        
        Assert.assertEquals("Wrong title.", "Chapter I",  title);
    }

    @Test
    public void testFallbackTitle() 
            throws IOException, ImporterHandlerException {
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        
        InputStream is = new ByteArrayInputStream(
                "This is the first line. This is another line.".getBytes());

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/plain");
        t.tagDocument("test.txt", is, metadata, true);

        is.close();

        String title = metadata.getString(ImporterMetadata.DOC_GENERATED_TITLE);
        
        LOG.info("TITLE IS: " + title);        
        Assert.assertEquals("Wrong title.", "This is the first line.",  title);
    }

    
    @Test
    public void testWriteRead() throws IOException {
        TitleGeneratorTagger t = new TitleGeneratorTagger();
        t.setFromField("potato");
        t.setToField("banana");
        t.setOverwrite(true);
        t.setTitleMaxLength(300);
        t.setDetectHeading(true);
        t.setDetectHeadingMaxLength(200);
        t.setDetectHeadingMinLength(20);
        System.out.println("Writing/Reading this: " + t);
        XMLConfigurationUtil.assertWriteRead(t);
    }

}
