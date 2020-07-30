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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertyMatcher;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.parser.ParseState;

/**
 * @deprecated
 */
@Deprecated
public class RegexMetadataFilterTest {

    @Test
    public void testAcceptDocument()
            throws ImporterHandlerException {
        Properties meta = new Properties();
        meta.add("field1", "a string to match");
        meta.add("field2", "something we want");

        RegexMetadataFilter filter = new RegexMetadataFilter();

        filter.setField("field1");
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assertions.assertFalse(
                TestUtil.filter(filter, "n/a", null, meta, ParseState.PRE),
                "field1 not filtered properly.");

        filter.setField("field2");
        Assertions.assertTrue(
                TestUtil.filter(filter, "n/a", null, meta, ParseState.PRE),
                "field2 not filtered properly.");

    }

    @Test
        public void testWriteRead() {
        RegexMetadataFilter filter = new RegexMetadataFilter();
        filter.addRestriction(new PropertyMatcher(
                TextMatcher.basic("author"),
                TextMatcher.regex("Pascal.*")));
        filter.setField("field1");
        filter.setRegex("blah");
        filter.setOnMatch(OnMatch.INCLUDE);
        XML.assertWriteRead(filter, "handler");
    }
}
