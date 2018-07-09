/* Copyright 2016-2018 Norconex Inc.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.MergeTagger.Merge;

public class MergeTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        MergeTagger tagger = new MergeTagger();

        Merge m = null;

        m = new Merge();
        m.setDeleteFromFields(true);
        m.setFromFields(Arrays.asList("1", "2"));
        m.setFromFieldsRegex("regex");
        m.setSingleValue(true);
        m.setSingleValueSeparator(",");
        m.setToField("toField");
        tagger.addMerge(m);

        m = new Merge();
        m.setDeleteFromFields(false);
        m.setFromFields(Arrays.asList("3", "4"));
        m.setFromFieldsRegex(null);
        m.setSingleValue(false);
        m.setSingleValueSeparator(null);
        m.setToField("toAnotherField");
        tagger.addMerge(m);

        XML.assertWriteRead(tagger, "handler");
    }


    @Test
    public void testMultiFieldsMerge() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "1.1", "1.2");
        meta.addString("field2", "2");
        meta.addString("field3", "3");
        meta.addString("fld4", "4");
        meta.addString("fld5", "5");
        meta.addString("fld6", "6");

        MergeTagger tagger = new MergeTagger();

        Merge m = null;

        m = new Merge();
        m.setDeleteFromFields(false);
        m.setFromFields(Arrays.asList("fld4", "fld6"));
        m.setFromFieldsRegex("field.*");
        m.setSingleValue(false);
        m.setToField("toField");
        tagger.addMerge(m);

        m = new Merge();
        m.setDeleteFromFields(true);
        m.setFromFields(Arrays.asList("fld4", "fld6"));
        m.setFromFieldsRegex("field.*");
        m.setSingleValue(true);
        m.setSingleValueSeparator("-");
        m.setToField("fld4");
        tagger.addMerge(m);

        tagger.tagDocument("n/a", null, meta, true);

        Set<String> expected = new TreeSet<>(Arrays.asList(
                "1.1", "1.2", "2", "3", "4", "6"));

        Assert.assertEquals(expected,
                new TreeSet<>(meta.getStrings("toField")));
        Assert.assertEquals(expected, new TreeSet<>(Arrays.asList(
                meta.getString("fld4").split("-"))));
    }

    @Test
    public void testSingleFieldMerge() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field", "1", "2");

        MergeTagger tagger = new MergeTagger();

        Merge m = new Merge();
        m.setDeleteFromFields(false);
        m.setFromFields(Arrays.asList("field"));
        m.setSingleValue(true);
        m.setToField("field");
        tagger.addMerge(m);

        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals("12", meta.getString("field"));
    }

}
