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

import java.io.IOException;

import org.apache.tika.io.NullInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class EmptyFilterTest {

    @Test
    public void testEmptyFields() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field1", "a string to match");
        meta.add("field2", "");

        EmptyFilter filter = new EmptyFilter();

        filter.setFieldMatcher(TextMatcher.basic("field1"));
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertTrue(filter.acceptDocument("n/a", null, meta, false),
                "field1 not filtered properly.");

        filter.setFieldMatcher(TextMatcher.basic("field2"));
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false),
                "field2 not filtered properly.");

        filter.setFieldMatcher(TextMatcher.basic("field3"));
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false),
                "field3 not filtered properly.");
    }

    @Test
    public void testEmptyContent()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();

        EmptyFilter filter = new EmptyFilter();
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertTrue(filter.acceptDocument(
                "n/a", new NullInputStream(5, true, false), meta, false),
                "Non-empty stream should be accepted.");
        Assertions.assertFalse(filter.acceptDocument(
                "n/a", new NullInputStream(0, true, false), meta, false),
                "Empty stream should be rejected.");
        Assertions.assertFalse(filter.acceptDocument("n/a", null, meta, false),
                "Null stream should be rejected.");
    }

    @Test
    public void testWriteRead() throws IOException {
        EmptyFilter filter = new EmptyFilter();
        filter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("author"), TextMatcher.regex("Pascal.*")));
        filter.setFieldMatcher(TextMatcher.regex("(field1|field2|field3)"));
        filter.setOnMatch(OnMatch.INCLUDE);
        XML.assertWriteRead(filter, "handler");
    }
}
