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

import static com.norconex.commons.lang.map.PropertySetter.APPEND;
import static com.norconex.commons.lang.map.PropertySetter.REPLACE;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.DOMTagger.DOMExtractDetails;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.util.DOMUtil;

/**
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class DOMTaggerTest {

    @Test
    public void testDelete() throws ImporterHandlerException {
 
        String child1 = "<div id=\"childOneId\" class=\"childClass\">"
                + "<a href=\"http://example.org/doc.html\">"
                + "Child1 Link</a></div>";
        String child2 = "<div class=\"childClass\">Child2 text</div>";

        String full = "<div id=\"parentId\" class=\"parentClass\">"
                + child1 + child2 + "</div>";

        String fullMinusChild1 = "<div id=\"parentId\" class=\"parentClass\">"
                + child2 + "</div>";


        DOMTagger t = new DOMTagger();
        t.setParser(DOMUtil.PARSER_XML);
        t.setFromField("fromField1");

        Properties metadata = new Properties();
        metadata.set("fromField1", full);
        t.addDOMExtractDetails(new DOMExtractDetails()
                .setDelete(true)
                .setSelector("#childOneId")
                .setExtract("outerHtml")
                .setToField("toField1"));

        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        InputStream content = new NullInputStream(0);
        t.tagDocument(TestUtil.toHandlerDoc(
                "n/a", content, metadata), content, ParseState.PRE);

        String fromXml = metadata.getString("fromField1");
        String toXml = metadata.getString("toField1");

        Assertions.assertEquals(child1, cleanHTML(toXml));
        Assertions.assertEquals(fullMinusChild1, cleanHTML(fromXml));

    }

    @Test
    public void testNestedDelete() throws ImporterHandlerException {

        String child1 = "<div id=\"childOneId\" class=\"childClass\">"
                + "<a href=\"http://example.org/doc.html\">"
                + "Child1 Link</a></div>";
        String child2 = "<div class=\"childClass\">Child2 text</div>";

        String full = "<div id=\"parentId\" class=\"parentClass\">"
                + child1 + child2 + "</div>";

        DOMTagger t = new DOMTagger();
        t.setParser(DOMUtil.PARSER_XML);
        t.setFromField("fromField1");

        Properties metadata = new Properties();
        metadata.set("fromField1", full);
        t.addDOMExtractDetails(new DOMExtractDetails()
                .setDelete(true)
                .setSelector("div"));

        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        InputStream content = new NullInputStream(0);
        t.tagDocument(TestUtil.toHandlerDoc(
                "n/a", content, metadata), content, ParseState.PRE);

        String fromXml = metadata.getString("fromField1");

        Assertions.assertEquals("", cleanHTML(fromXml));

    }

    // This is a test for: https://github.com/Norconex/collector-http/issues/381
    @Test
    public void testXMLParser()
            throws ImporterHandlerException, IOException {

        DOMTagger t = new DOMTagger();

        t.setParser(DOMUtil.PARSER_XML);
        t.addDOMExtractDetails(new DOMExtractDetails(
                "tr ", "WHOLE", REPLACE, "outerHtml"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "tr td:nth-of-type(1) a", "TEST_URL", REPLACE, "attr(href)"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "tr td:nth-of-type(1) a", "TEST_TITLE", REPLACE, "ownText"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "tr td:nth-of-type(2)", "TEST_DESC", REPLACE, "ownText"));

        String xml = "<tr>"
                + "<td><a href=\"http://example.org/doc.html\">"
                + "Sample Title</a></td>"
                + "<td>This is a description.</td>"
                + "</tr>";

        Properties metadata = new Properties();
        performTagging(metadata, t, xml);

        String whole = metadata.getString("WHOLE");
        String title = metadata.getString("TEST_TITLE");
        String url = metadata.getString("TEST_URL");
        String desc = metadata.getString("TEST_DESC");

        Assertions.assertTrue(StringUtils.contains(whole, "</tr>"));
        Assertions.assertEquals("Sample Title", title);
        Assertions.assertEquals("http://example.org/doc.html", url);
        Assertions.assertEquals("This is a description.", desc);
    }

    // This is a test for: https://github.com/Norconex/importer/issues/39
    @Test
    public void testMatchBlanks()
            throws ImporterHandlerException, IOException {

        DOMTagger t = new DOMTagger();
        DOMExtractDetails d = null;

        d = new DOMExtractDetails("author name", "blanksON", APPEND);
        d.setMatchBlanks(true);
        t.addDOMExtractDetails(d);

        d = new DOMExtractDetails("author name", "blanksOFF", APPEND);
        d.setMatchBlanks(false);
        t.addDOMExtractDetails(d);

        d = new DOMExtractDetails("author name", "blanksONDefault", APPEND);
        d.setMatchBlanks(true);
        d.setDefaultValue("Joe");
        t.addDOMExtractDetails(d);

        d = new DOMExtractDetails("author name", "blanksOFFDefault", APPEND);
        d.setMatchBlanks(false);
        d.setDefaultValue("Joe");
        t.addDOMExtractDetails(d);

        String xml = "<test>"
                // present
                + "<author><name>John</name></author>"
                // empty
                + "<author><name></name></author>"
                // blank
                + "<author><name>   </name></author>"
                // missing/null
                + "<author></author>"
                + "</test>";


        Properties metadata = new Properties();
        performTagging(metadata, t, xml);

        String[] blanksON = getSortedArray(metadata, "blanksON");
        String[] blanksOFF = getSortedArray(metadata, "blanksOFF");
        String[] blanksONDefault = getSortedArray(metadata, "blanksONDefault");
        String[] blanksOFFDefault =
                getSortedArray(metadata, "blanksOFFDefault");

        Assertions.assertArrayEquals(new String[]{"", "", "John"}, blanksON);
        Assertions.assertArrayEquals(new String[]{"John"}, blanksOFF);

        Assertions.assertArrayEquals(new String[]{"", "", "John"}, blanksONDefault);
        Assertions.assertArrayEquals(
                new String[]{"Joe", "Joe", "John"}, blanksOFFDefault);
    }


    private String[] getSortedArray(Properties metadata, String key) {
        List<String> list = metadata.getStrings(key);
        Collections.sort(list);
        return list.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    // This is a test for: https://github.com/Norconex/importer/issues/39
    // where default value should not be trimmed.
    @Test
    public void testDefaultValue()
            throws ImporterHandlerException, IOException {

        String cfg = "<tagger class=\""
                + "com.norconex.importer.handler.tagger.impl.DOMTagger\">"
                + "<dom selector=\"author name\" toField=\"noDefault\"/>"
                + "<dom selector=\"author name\" toField=\"emptyDefault\" "
                + "defaultValue=\"\"/>"
                + "<dom selector=\"author name\" toField=\"spaceDefault\" "
                + "defaultValue=\"   \"/>"
                + "</tagger>";
        DOMTagger t = new DOMTagger();
        t.loadFromXML(new XML(cfg));

        String xml = "<test><author><name></name></author></test>";


        Properties metadata = new Properties();
        performTagging(metadata, t, xml);

        String noDefault = metadata.getString("noDefault");
        String emptyDefault = metadata.getString("emptyDefault");
        String spaceDefault = metadata.getString("spaceDefault");

        Assertions.assertEquals(null, noDefault);
        Assertions.assertEquals("", emptyDefault);
        Assertions.assertEquals("   ", spaceDefault);
    }


    // This is a test for "fromField" and "defaultValue" feature request:
    // https://github.com/Norconex/importer/issues/28
    @Test
    public void testFromField()
                throws ImporterHandlerException, IOException {

        String html = "<html><body>"
                + "<whatever/>"
                + "<div class=\"contact\">"
                + "  <div class=\"firstName\">JoeFirstOnly</div>"
                + "</div>"
                + "<whatever class=\"contact\"></whatever>"
                + "<div class=\"contact\">"
                + "  <div class=\"firstName\">John</div>"
                + "  <div class=\"lastName\">Smith</div>"
                + "</div>"
                + "<whatever/>"
                + "<div class=\"contact\">"
                + "  <div class=\"lastName\">JackLastOnly</div>"
                + "</div>"
                + "<whatever/>"
                + "</body></html>";

        Properties metadata = new Properties();

        DOMTagger parentTagger = new DOMTagger();
        parentTagger.addDOMExtractDetails(new DOMExtractDetails(
                "div.contact", "htmlContacts", APPEND, "html"));
        performTagging(metadata, parentTagger, html);

        DOMTagger childTagger = new DOMTagger();
        childTagger.setFromField("htmlContacts");

        DOMExtractDetails firstNameDetails = new DOMExtractDetails(
                "div.firstName", "firstName", APPEND);
        firstNameDetails.setDefaultValue("NoFirstName");
        childTagger.addDOMExtractDetails(firstNameDetails);

        DOMExtractDetails lastNameDetails = new DOMExtractDetails(
                "div.lastName", "lastName", APPEND, "html");
        lastNameDetails.setDefaultValue("NoLastName");
        childTagger.addDOMExtractDetails(lastNameDetails);

        performTagging(metadata, childTagger, html);


        List<String> firstNames = metadata.getStrings("firstName");
        List<String> lastNames = metadata.getStrings("lastName");

        Assertions.assertEquals(Arrays.asList(
                "JoeFirstOnly", "John", "NoFirstName"), firstNames);
        Assertions.assertEquals(Arrays.asList(
                "NoLastName", "Smith", "JackLastOnly"), lastNames);
    }

    // This is a test for: https://github.com/Norconex/importer/issues/21
    @Test
    public void testNotAllSelectorsMatching()
            throws ImporterHandlerException, IOException {

        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.class1", "match1", APPEND));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.classNoMatch", "match2", APPEND));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.class3", "match3", APPEND));

        String html = "<html><body>"
                + "<div class=\"class1\">text1</div>"
                + "<div class=\"class2\">text2</div>"
                + "<div class=\"class3\">text3</div>"
                + "</body></html>";

        Properties metadata = new Properties();
        performTagging(metadata, t, html);

        String match1 = metadata.getString("match1");
        String match2 = metadata.getString("match2");
        String match3 = metadata.getString("match3");

        Assertions.assertEquals("text1", match1);
        Assertions.assertEquals(null, match2);
        Assertions.assertEquals("text3", match3);
    }

    @Test
    public void testExtractFromDOM()
            throws ImporterHandlerException, IOException {
        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails(new DOMExtractDetails(
                "h2", "headings", APPEND));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "a[href]", "links", REPLACE, "html"));

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        t.tagDocument(TestUtil.toHandlerDoc(
                htmlFile.getAbsolutePath(), is, metadata), is, ParseState.PRE);
        is.close();

        List<String> headings = metadata.getStrings("headings");
        List<String> links = metadata.getStrings("links");

        Assertions.assertEquals(2, headings.size(), "Wrong <h2> count.");
        Assertions.assertEquals(4, links.size(),
                "Wrong <img src=\"...\"> count.");
        Assertions.assertEquals("CHAPTER I", headings.get(0),
                "Did not extract first heading");
        Assertions.assertEquals("Down the Rabbit-Hole", headings.get(1),
                "Did not extract second heading");
    }

    @Test
    public void testExtractionTypes()
            throws ImporterHandlerException, IOException {
        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails(new DOMExtractDetails(
                "head", "fhtml", APPEND, "html"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "head", "fouter", APPEND, "outerhtml"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "head", "ftext", APPEND, "text"));

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        t.tagDocument(TestUtil.toHandlerDoc(
                htmlFile.getAbsolutePath(), is, metadata), is, ParseState.PRE);
        is.close();

        String expectedText = "Alice's Adventures in Wonderland -- Chapter I";
        String expectedHtml = "<meta http-equiv=\"content-type\" "
                + "content=\"text/html; charset=ISO-8859-1\">"
                + "<title>" + expectedText + "</title>";
        String expectedOuter = "<head>" + expectedHtml + "</head>";

        Assertions.assertEquals(expectedText, metadata.getString("ftext"));
        Assertions.assertEquals(expectedHtml,
                cleanHTML(metadata.getString("fhtml")));
        Assertions.assertEquals(expectedOuter,
                cleanHTML(metadata.getString("fouter")));
    }

    private void performTagging(
            Properties metadata, DOMTagger tagger, String html)
            throws ImporterHandlerException, IOException {
        InputStream is = new ByteArrayInputStream(html.getBytes());
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");
        tagger.tagDocument(TestUtil.toHandlerDoc(
                "n/a", is, metadata), is, ParseState.PRE);
        is.close();
    }



    private String cleanHTML(String html) {
        String clean = html;
        clean = clean.replaceAll("[\\r\\n]", "");
        clean = clean.replaceAll(">\\s+", ">");
        clean = clean.replaceAll("\\s+<", "<");
        return clean;
    }


    @Test
    public void testAllExtractionTypes()
            throws ImporterHandlerException, IOException {

        DOMTagger t = new DOMTagger();
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.parent", "text", APPEND, "text"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "span.child1", "html", APPEND, "html"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "span.child1", "outerHtml", APPEND, "outerHtml"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "script", "data", APPEND, "data"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.parent", "id", APPEND, "id"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.parent", "ownText", APPEND, "ownText"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "div.parent", "tagName", APPEND, "tagName"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                ".formElement", "val", APPEND, "val"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "textarea", "className", APPEND, "className"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                ".child2", "cssSelector", APPEND, "cssSelector"));
        t.addDOMExtractDetails(new DOMExtractDetails(
                "textarea", "attr", APPEND, "attr(title)"));

        String content = "<html><body>"
                + "<script>This is data, not HTML.</script>"
                + "<div id=\"content\" class=\"parent\">Parent text."
                + "<span class=\"child1\">Child text <b>1</b>.</span>"
                + "<span class=\"child2\">Child text <b>2</b>.</span>"
                + "</div>"
                + "<textarea class=\"formElement\" title=\"Some Title\">"
                + "textarea value.</textarea>"
                + "</body></html>";

        Properties metadata = new Properties();
        InputStream is = new ByteArrayInputStream(content.getBytes());
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");

        t.tagDocument(TestUtil.toHandlerDoc(
                "n/a", is, metadata), is, ParseState.PRE);
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

        Assertions.assertEquals("Parent text.Child text 1.Child text 2.", text);
        Assertions.assertEquals("Child text <b>1</b>.", html);
        Assertions.assertEquals(
                "<span class=\"child1\">Child text <b>1</b>.</span>",
                outerHtml);
        Assertions.assertEquals("This is data, not HTML.", data);
        Assertions.assertEquals("content", id);
        Assertions.assertEquals("Parent text.", ownText);
        Assertions.assertEquals("div", tagName);
        Assertions.assertEquals("textarea value.", val);
        Assertions.assertEquals("formElement", className);
        Assertions.assertEquals("#content > span.child2", cssSelector);
        Assertions.assertEquals("Some Title", attr);
    }

    @Test
    public void testWriteRead() {
        DOMTagger tagger = new DOMTagger();
        tagger.addDOMExtractDetails(new DOMExtractDetails(
                "p.blah > a", "myField", REPLACE));

        DOMExtractDetails details = new DOMExtractDetails(
                "div.blah > a", "myOtherField", REPLACE, "html");
        details.setDefaultValue("myDefaultValue");
        details.setDelete(true);
        tagger.addDOMExtractDetails(details);
        tagger.addRestriction(new PropertyMatcher(
                TextMatcher.basic("afield"),
                TextMatcher.basic("aregex")));
        tagger.setFromField("myfromfield");
        XML.assertWriteRead(tagger, "handler");
    }
}
