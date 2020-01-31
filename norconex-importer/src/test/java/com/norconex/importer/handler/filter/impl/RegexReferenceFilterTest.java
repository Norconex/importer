/* Copyright 2017-2020 Norconex Inc.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

/**
 * @deprecated
 */
@Deprecated
public class RegexReferenceFilterTest {

    @Test
    public void testAcceptDocument()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        RegexReferenceFilter filter = new RegexReferenceFilter();

        filter.setRegex(".*/login.*");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertFalse(filter.acceptDocument(
                "http://www.example.com/login", null, meta, false),
                "URL not filtered properly.");

        Assertions.assertTrue(filter.acceptDocument(
                "http://www.example.com/blah", null, meta, false),
                "URL not filtered properly.");
    }

    @Test
    public void testWriteRead() throws IOException {
        RegexReferenceFilter filter = new RegexReferenceFilter();
        filter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("author"),
                TextMatcher.regex("Pascal.*").setIgnoreCase(true)));
        filter.setRegex("blah");
        filter.setOnMatch(OnMatch.INCLUDE);
        XML.assertWriteRead(filter, "handler");
    }
}
