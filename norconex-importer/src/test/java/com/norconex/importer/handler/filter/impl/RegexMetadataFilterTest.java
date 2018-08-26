/* Copyright 2010-2018 Norconex Inc.
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

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class RegexMetadataFilterTest {

    @Test
    public void testAcceptDocument()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field1", "a string to match");
        meta.add("field2", "something we want");

        RegexMetadataFilter filter = new RegexMetadataFilter();

        filter.setField("field1");
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assert.assertFalse("field1 not filtered properly.",
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("field2");
        Assert.assertTrue("field2 not filtered properly.",
                filter.acceptDocument("n/a", null, meta, false));

    }

    @Test
    public void testWriteRead() throws IOException {
        RegexMetadataFilter filter = new RegexMetadataFilter();
        filter.addRestriction("author", "Pascal.*", false);
        filter.setField("field1");
        filter.setRegex("blah");
        filter.setOnMatch(OnMatch.INCLUDE);
        XML.assertWriteRead(filter, "handler");
    }
}
