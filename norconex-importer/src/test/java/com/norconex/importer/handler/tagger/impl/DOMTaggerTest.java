/* Copyright 2015-2016 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    // This is a test for: https://github.com/Norconex/importer/issues/21 
    public void testNotAllSelectorsMatching()
            throws IOException, ImporterHandlerException { 

        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails("div.class1", "match1", false);
        t.addDOMExtractDetails("div.classNoMatch", "match2", false);
        t.addDOMExtractDetails("div.class3", "match3", false);

        String html = "<html><body>"
                + "<div class=\"class1\">text1</div>"
                + "<div class=\"class2\">text2</div>"
                + "<div class=\"class3\">text3</div>"
                + "</body></html>";
        
        ImporterMetadata metadata = new ImporterMetadata();
        InputStream is = new ByteArrayInputStream(html.getBytes());
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument("n/a", is, metadata, false);
        is.close();

        String match1 = metadata.getString("match1");
        String match2 = metadata.getString("match2");
        String match3 = metadata.getString("match3");
        
        Assert.assertEquals("text1", match1);
        Assert.assertEquals(null, match2);
        Assert.assertEquals("text3", match3);
    }
    
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
    public void testAllExtractionTypes() 
            throws IOException, ImporterHandlerException {
        
        
        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails("div.parent", "text", false, "text");
        t.addDOMExtractDetails("span.child1", "html", false, "html");
        t.addDOMExtractDetails("span.child1", "outerHtml", false, "outerHtml");
        t.addDOMExtractDetails("script", "data", false, "data");
        t.addDOMExtractDetails("div.parent", "id", false, "id");
        t.addDOMExtractDetails("div.parent", "ownText", false, "ownText");
        t.addDOMExtractDetails("div.parent", "tagName", false, "tagName");
        t.addDOMExtractDetails(".formElement", "val", false, "val");
        t.addDOMExtractDetails("textarea", "className", false, "className");
        t.addDOMExtractDetails(".child2", "cssSelector", false, "cssSelector");
        t.addDOMExtractDetails("textarea", "attr", false, "attr(title)");

        String content = "<html><body>"
                + "<script>This is data, not HTML.</script>"
                + "<div id=\"content\" class=\"parent\">Parent text."
                + "<span class=\"child1\">Child text <b>1</b>.</span>"
                + "<span class=\"child2\">Child text <b>2</b>.</span>"
                + "</div>"
                + "<textarea class=\"formElement\" title=\"Some Title\">"
                + "textarea value.</textarea>"
                + "</body></html>";
        
        ImporterMetadata metadata = new ImporterMetadata();
        InputStream is = new ByteArrayInputStream(content.getBytes());
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument("n/a", is, metadata, false);
        is.close();

        String text = metadata.getString("text");
        String html = metadata.getString("html");
        String outerHtml = metadata.getString("outerHtml");
        String data = metadata.getString("data");
        String id = metadata.getString("id");
        String ownText = metadata.getString("ownText");
        String tagName = metadata.getString("tagName");
        String val = metadata.getString("val");
        String className = metadata.getString("className");
        String cssSelector = metadata.getString("cssSelector");
        String attr = metadata.getString("attr");
        
        Assert.assertEquals("Parent text.Child text 1.Child text 2.", text);
        Assert.assertEquals("Child text <b>1</b>.", html);
        Assert.assertEquals(
                "<span class=\"child1\">Child text <b>1</b>.</span>", 
                outerHtml);
        Assert.assertEquals("This is data, not HTML.", data);
        Assert.assertEquals("content", id);
        Assert.assertEquals("Parent text.", ownText);
        Assert.assertEquals("div", tagName);
        Assert.assertEquals("textarea value.", val);
        Assert.assertEquals("formElement", className);
        Assert.assertEquals("#content > span.child2", cssSelector);
        Assert.assertEquals("Some Title", attr);
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
