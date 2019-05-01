/* Copyright 2015-2019 Norconex Inc.
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

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.NumericMetadataFilter.Operator;

public class NumericMetadataFilterTest {

    @Test
    public void testAcceptDocument()
            throws IOException, ImporterHandlerException {

        ImporterMetadata meta = new ImporterMetadata();
        meta.add("lowerthan", "-4.25");
        meta.add("inrange", "25");
        meta.add("greaterthan", "55");
        meta.add("multivalInrange", "0");
        meta.add("multivalInrange", "20");
        meta.add("multivalInrange", "66");
        meta.add("multivalOutrange", "0");
        meta.add("multivalOutrange", "11");
        meta.add("multivalOutrange", "66");
        meta.add("equal", "6.5");

        NumericMetadataFilter filter = new NumericMetadataFilter();
        filter.addCondition(Operator.GREATER_EQUAL, 20);
        filter.addCondition(Operator.LOWER_THAN, 30);


        filter.setField("lowerthan");
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("inrange");
        Assertions.assertTrue(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("greaterthan");
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("multivalInrange");
        Assertions.assertTrue(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("multivalOutrange");
        Assertions.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setConditions(new NumericMetadataFilter.Condition(
                Operator.EQUALS, 6.5));
        filter.setField("equal");
        Assertions.assertTrue(
                filter.acceptDocument("n/a", null, meta, false));
    }

    @Test
    public void testWriteRead() throws IOException {
        NumericMetadataFilter filter = new NumericMetadataFilter();
        filter.setField("field1");
        filter.setOnMatch(OnMatch.EXCLUDE);
        filter.addCondition(Operator.GREATER_EQUAL, 20);
        filter.addCondition(Operator.LOWER_THAN, 30);
        XML.assertWriteRead(filter, "handler");
    }
}
