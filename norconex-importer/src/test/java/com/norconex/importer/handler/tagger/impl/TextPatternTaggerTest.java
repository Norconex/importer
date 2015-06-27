/* Copyright 2015 Norconex Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

/**
 * @author Pascal Essiembre
 * @since 2.3.0
 */
public class TextPatternTaggerTest {

    @Test
    public void testTagTextDocument() 
            throws IOException, ImporterHandlerException {
        TextPatternTagger t = new TextPatternTagger();
        t.addPattern("headings", "<h2>(.*?)</h2>" , 1);
        t.addPattern("country", "\\w+\\sZealand");
        t.setCaseSensitive(false);
        File htmlFile = TestUtil.getAliceHtmlFile();
        FileInputStream is = new FileInputStream(htmlFile);

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);

        is.close();

        List<String> headings = metadata.getStrings("headings");
        List<String> countries = metadata.getStrings("country");
        
        Assert.assertEquals("Wrong <h2> count.", 2, headings.size());
        Assert.assertEquals("Did not extract first heading", 
                "CHAPTER I", headings.get(0));
        Assert.assertEquals("Did not extract second heading", 
                "Down the Rabbit-Hole", headings.get(1));
        
        Assert.assertEquals("Wrong country count.", 1, countries.size());
        Assert.assertEquals("Did not extract country", 
                "New Zealand", countries.get(0));
    }

    @Test
    public void testExtractFirst100ContentChars() 
            throws IOException, ImporterHandlerException {
        TextPatternTagger t = new TextPatternTagger();
        t.addPattern("mytitle", "^.{0,100}");
        t.setCaseSensitive(false);
        File htmlFile = TestUtil.getAliceHtmlFile();
        FileInputStream is = new FileInputStream(htmlFile);

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);

        is.close();

        String myTitle = metadata.getString("mytitle");
        Assert.assertEquals(100, myTitle.length());
    }
    
    @Test
    public void testWriteRead() throws IOException {
        TextPatternTagger tagger = new TextPatternTagger();
        tagger.addPattern("field1", "123.*890");
        tagger.addPattern("field2", "abc.*xyz", 3);
        tagger.setMaxReadSize(512);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
