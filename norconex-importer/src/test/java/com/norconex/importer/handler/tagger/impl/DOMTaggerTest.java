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
 * @since 2.4.0
 */
public class DOMTaggerTest {

    @Test
    public void testExtractFromDOM() 
            throws IOException, ImporterHandlerException {
        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails("h2", "headings", false);
        t.addDOMExtractDetails("a[href]", "links", true, "html");
        
        File htmlFile = TestUtil.getAliceHtmlFile();
        FileInputStream is = new FileInputStream(htmlFile);

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);
        is.close();

        List<String> headings = metadata.getStrings("headings");
        List<String> links = metadata.getStrings("links");
        
        Assert.assertEquals("Wrong <h2> count.", 2, headings.size());
        Assert.assertEquals("Wrong <img src=\"...\"> count.", 4, links.size());
        Assert.assertEquals("Did not extract first heading", 
                "CHAPTER I", headings.get(0));
        Assert.assertEquals("Did not extract second heading", 
                "Down the Rabbit-Hole", headings.get(1));
    }

    @Test
    public void testExtractionTypes() 
            throws IOException, ImporterHandlerException {
        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails("head", "fhtml", false, "html");
        t.addDOMExtractDetails("head", "fouter", false, "outerhtml");
        t.addDOMExtractDetails("head", "ftext", false, "text");

        File htmlFile = TestUtil.getAliceHtmlFile();
        FileInputStream is = new FileInputStream(htmlFile);

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);
        is.close();

        String expectedText = "Alice's Adventures in Wonderland -- Chapter I";
        String expectedHtml = "<meta http-equiv=\"content-type\" "
                + "content=\"text/html; charset=ISO-8859-1\">"
                + "<title>" + expectedText + "</title>";
        String expectedOuter = "<head>" + expectedHtml + "</head>";

        Assert.assertEquals(expectedText, metadata.getString("ftext"));
        Assert.assertEquals(expectedHtml, 
                cleanHTML(metadata.getString("fhtml")));
        Assert.assertEquals(expectedOuter, 
                cleanHTML(metadata.getString("fouter")));
    }
    private String cleanHTML(String html) {
        String clean = html;
        clean = clean.replaceAll("[\\r\\n]", "");
        clean = clean.replaceAll(">\\s+<", "><");
        return clean;
    }
    
    
    
    @Test
    public void testWriteRead() throws IOException {
        DOMTagger tagger = new DOMTagger();
        tagger.addDOMExtractDetails("p.blah > a", "myField", true);
        tagger.addDOMExtractDetails("div.blah > a", "myOtherField", true);
        tagger.addRestriction("afield", "aregex", true);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
