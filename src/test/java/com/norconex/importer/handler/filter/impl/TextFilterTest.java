/* Copyright 2020 Norconex Inc.
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
package com.norconex.importer.handler.filter.impl;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterRequest;
import com.norconex.importer.TestUtil;
import com.norconex.importer.handler.HandlerConsumer;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.parser.ParseState;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.importer.response.ImporterStatus.Status;

public class TextFilterTest {

    @Test
    public void testRegexContentMatchesExclude()
            throws ImporterHandlerException {
        TextFilter filter = newRegexTextFilter();
        filter.getValueMatcher().setPattern(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);
        Assertions.assertFalse(TestUtil.filter(filter, "n/a",
                IOUtils.toInputStream("a string that matches",
                        StandardCharsets.UTF_8), null, ParseState.PRE),
                "Should have been rejected.");
    }
    @Test
    public void testRegexContentMatchesInclude()
            throws ImporterHandlerException {
        TextFilter filter = newRegexTextFilter();
        filter.getValueMatcher().setPattern(".*string.*");
        filter.setOnMatch(OnMatch.INCLUDE);
        Assertions.assertTrue(TestUtil.filter(filter, "n/a",
                IOUtils.toInputStream(
                        "a string that matches", StandardCharsets.UTF_8),
                        null, ParseState.PRE),
                "Should have been accepted.");
    }
    @Test
    public void testRegexContentNoMatchesExclude()
            throws ImporterHandlerException {
        TextFilter filter = newRegexTextFilter();
        filter.getValueMatcher().setPattern(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);
        Assertions.assertTrue(
                TestUtil.filter(filter, "n/a", IOUtils.toInputStream(
                        "a text that does not match", StandardCharsets.UTF_8),
                        null, ParseState.PRE),
                "Should have been accepted.");
    }
    @Test
    public void testRegexContentNoMatchesUniqueInclude()
            throws ImporterHandlerException {

        TextFilter filter = newRegexTextFilter();
        filter.getValueMatcher().setPattern(".*string.*");
        filter.setOnMatch(OnMatch.INCLUDE);
        Assertions.assertFalse(
                TestUtil.filter(filter, "n/a", IOUtils.toInputStream(
                        "a text that does not match", StandardCharsets.UTF_8),
                        null, ParseState.PRE),
                "Should have been rejected.");
    }

    @Test
    public void testRegexContentMatchesOneOfManyIncludes() {
        TextFilter filter1 = newRegexTextFilter();
        filter1.getValueMatcher().setPattern(".*string.*");
        filter1.setOnMatch(OnMatch.INCLUDE);

        TextFilter filter2 = newRegexTextFilter();
        filter2.getValueMatcher().setPattern(".*asdf.*");
        filter2.setOnMatch(OnMatch.INCLUDE);

        TextFilter filter3 = newRegexTextFilter();
        filter3.getValueMatcher().setPattern(".*qwer.*");
        filter3.setOnMatch(OnMatch.INCLUDE);

        ImporterConfig config = new ImporterConfig();
        config.setPreParseConsumer(
                HandlerConsumer.fromHandlers(filter1, filter2, filter3));

        ImporterResponse response = new Importer(config).importDocument(
                new ImporterRequest(new ReaderInputStream(
                        new StringReader("a string that matches"),
                        StandardCharsets.UTF_8))
                .setReference("N/A"));
        Assertions.assertEquals(
                Status.SUCCESS, response.getImporterStatus().getStatus(),
                "Status should have been SUCCESS");
    }

    @Test
    public void testRegexContentNoMatchesOfManyIncludes() {

        TextFilter filter1 = newRegexTextFilter();
        filter1.getValueMatcher().setPattern(".*zxcv.*");
        filter1.setOnMatch(OnMatch.INCLUDE);

        TextFilter filter2 = newRegexTextFilter();
        filter2.getValueMatcher().setPattern(".*asdf.*");
        filter2.setOnMatch(OnMatch.INCLUDE);

        TextFilter filter3 = newRegexTextFilter();
        filter3.getValueMatcher().setPattern(".*qwer.*");
        filter3.setOnMatch(OnMatch.INCLUDE);

        ImporterConfig config = new ImporterConfig();
        config.setPostParseConsumer(
                HandlerConsumer.fromHandlers(filter1, filter2, filter3));

        ImporterResponse response = new Importer(config).importDocument(
                new ImporterRequest(new ReaderInputStream(
                        new StringReader("no matches"), StandardCharsets.UTF_8))
                .setReference("N/A"));
        Assertions.assertEquals(
                Status.REJECTED, response.getImporterStatus().getStatus(),
                "Status should have been REJECTED");
    }

    @Test
    public void testRegexFieldDocument()
            throws ImporterHandlerException {
        Properties meta = new Properties();
        meta.add("field1", "a string to match");
        meta.add("field2", "something we want");

        TextFilter filter = newRegexTextFilter();

        filter.getFieldMatcher().setPattern("field1");
        filter.getValueMatcher().setPattern(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertFalse(
                TestUtil.filter(filter, "n/a", null, meta, ParseState.PRE),
                "field1 not filtered properly.");

        filter.getFieldMatcher().setPattern("field2");
        Assertions.assertTrue(
                TestUtil.filter(filter, "n/a", null, meta, ParseState.PRE),
                "field2 not filtered properly.");
    }

    @Test
        public void testWriteRead() {
        TextFilter filter = new TextFilter();
        filter.setFieldMatcher(new TextMatcher()
                .setMethod(Method.REGEX)
                .setPartial(true));
        filter.setValueMatcher(new TextMatcher()
                .setMethod(Method.REGEX)
                .setPartial(true)
                .setPattern("blah"));
        XML.assertWriteRead(filter, "handler");
    }

    private TextFilter newRegexTextFilter() {
        return new TextFilter(newRegexMatcher(), newRegexMatcher());
    }
    private TextMatcher newRegexMatcher() {
        return new TextMatcher().setMethod(Method.REGEX);
    }
}
