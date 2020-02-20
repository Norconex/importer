/* Copyright 2010-2020 Norconex Inc.
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

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

@Deprecated
public class EmptyMetadataFilterTest {

    @Test
    public void testAcceptDocument() throws IOException, ImporterHandlerException {
        Properties meta = new Properties();
        meta.add("field1", "a string to match");
        meta.add("field2", "");

        EmptyMetadataFilter filter = new EmptyMetadataFilter();

        filter.setFields("field1");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertTrue(filter.acceptDocument("n/a", null, meta, false),
                "field1 not filtered properly.");

        filter.setFields("field2");
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false),
                "field2 not filtered properly.");

        filter.setFields("field3");
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false),
                "field3 not filtered properly.");
    }

    @Test
    public void testWriteRead() throws IOException {
        EmptyMetadataFilter filter = new EmptyMetadataFilter();
        filter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("author"),
                TextMatcher.regex("Pascal.*")));
        filter.setFields("field1", "field2", "field3");
        filter.setOnMatch(OnMatch.INCLUDE);
        XML.assertWriteRead(filter, "handler");
    }
}
