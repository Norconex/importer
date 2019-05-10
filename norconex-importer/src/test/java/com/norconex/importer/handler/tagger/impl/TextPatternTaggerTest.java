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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.util.regex.RegexFieldExtractor;

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
        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

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
        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);

        is.close();

        String myTitle = metadata.getString("mytitle");
        Assert.assertEquals(100, myTitle.length());
    }

//    @Test
//    public void testFieldGroupValueGroup() {
//        TextPatternTagger t = new TextPatternTagger();
//        t.addPattern(new RegexFieldExtractor("(.*?):(.*?)(,\\s*|$)", 1, 2).);
//
//        ImporterMetadata metadata = new ImporterMetadata();
//        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
//        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);
//
//    }

//    <tagger class="com.norconex.importer.handler.tagger.impl.TextPatternTagger" >
//    <pattern field="emails">
//        [A-Za-z0-9+_.-]+?@[a-zA-Z0-9.-]+
//    </pattern>
//    <pattern fieldGroup="1" valueGroup="2"><![CDATA[
//      <tr><td class="label">(.*?)</td><td class="value">(.*?)</td></tr>
//    ]]></pattern>
//</tagger>

    @Test
    public void testWriteRead() throws IOException {
        TextPatternTagger tagger = new TextPatternTagger();
        tagger.addPattern("field1", "123.*890");
        tagger.addPattern("field2", "abc.*xyz", 3);
        tagger.addPattern(new RegexFieldExtractor("blah")
                .setCaseSensitive(true)
                .setField("field3")
                .setFieldGroup(3)
                .setValueGroup(6));
        tagger.setMaxReadSize(512);
        System.out.println("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }

}
