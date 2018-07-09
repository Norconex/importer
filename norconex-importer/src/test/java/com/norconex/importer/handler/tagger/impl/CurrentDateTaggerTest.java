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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.Sleeper;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CurrentDateTaggerTest {


    @Test
    public void testCurrentDateTagger() throws ImporterHandlerException {
        long now = System.currentTimeMillis();
        Sleeper.sleepMillis(10);// to make sure time has passed

        ImporterMetadata meta = null;
        CurrentDateTagger tagger = null;

        meta = new ImporterMetadata();
        tagger = new CurrentDateTagger();
        tagger.setFormat("yyyy-MM-dd'T'HH:mm:ss");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertTrue("Returned date format does not match",
                meta.getString(ImporterMetadata.DOC_IMPORTED_DATE).matches(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));

        meta = new ImporterMetadata();
        tagger = new CurrentDateTagger();
        tagger.setFormat("EEEE");
        tagger.setLocale(Locale.CANADA_FRENCH);
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertTrue("Returned date format does not match",
                ArrayUtils.contains(new String[]{
                        "lundi", "mardi", "mercredi", "jeudi", "vendredi",
                        "samedi", "dimanche"},
                        meta.getString(ImporterMetadata.DOC_IMPORTED_DATE)));

        meta = new ImporterMetadata();
        meta.addString("existingField", "1002727941000");
        tagger = new CurrentDateTagger();
        tagger.setOverwrite(true);
        tagger.setField("existingField");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("Invalid overwritten number of date values",
                1, meta.getLongs("existingField").size());
        Assert.assertTrue("Invalid overwritten date created",
                meta.getLong("existingField") > now);

        meta = new ImporterMetadata();
        meta.addString("existingField", "1002727941000");
        tagger = new CurrentDateTagger();
        tagger.setOverwrite(false);
        tagger.setField("existingField");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("Invalid added number of date values",
                2, meta.getLongs("existingField").size());
        List<Long> longs = meta.getLongs("existingField");
        for (Long dateLong : longs) {
            if (dateLong == 1002727941000L) {
                continue;
            } else {
                Assert.assertTrue("Invalid added date created", dateLong > now);
            }
        }
    }

    @Test
    public void testWriteRead() throws IOException {
        CurrentDateTagger tagger = new CurrentDateTagger();
        tagger.setField("field1");
        tagger.setFormat("yyyy-MM-dd");
        tagger.setOverwrite(true);
        XML.assertWriteRead(tagger, "handler");
    }
}
