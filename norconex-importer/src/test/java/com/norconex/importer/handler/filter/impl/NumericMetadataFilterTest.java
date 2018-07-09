/* Copyright 2015-2018 Norconex Inc.
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
import com.norconex.importer.handler.filter.impl.NumericMetadataFilter.Operator;

public class NumericMetadataFilterTest {

    @Test
    public void testAcceptDocument()
            throws IOException, ImporterHandlerException {

        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("lowerthan", "-4.25");
        meta.addString("inrange", "25");
        meta.addString("greaterthan", "55");
        meta.addString("multivalInrange", "0");
        meta.addString("multivalInrange", "20");
        meta.addString("multivalInrange", "66");
        meta.addString("multivalOutrange", "0");
        meta.addString("multivalOutrange", "11");
        meta.addString("multivalOutrange", "66");
        meta.addString("equal", "6.5");

        NumericMetadataFilter filter = new NumericMetadataFilter();
        filter.addCondition(Operator.GREATER_EQUAL, 20);
        filter.addCondition(Operator.LOWER_THAN, 30);


        filter.setField("lowerthan");
        Assert.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("inrange");
        Assert.assertTrue(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("greaterthan");
        Assert.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("multivalInrange");
        Assert.assertTrue(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setField("multivalOutrange");
        Assert.assertFalse(
                filter.acceptDocument("n/a", null, meta, false));

        filter.setConditions(new NumericMetadataFilter.Condition(
                Operator.EQUALS, 6.5));
        filter.setField("equal");
        Assert.assertTrue(
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
