/* Copyright 2015-2020 Norconex Inc.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.Sleeper;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.handler.ImporterHandlerException;

public class CurrentDateTaggerTest {


    @Test
    public void testCurrentDateTagger() throws ImporterHandlerException {
        long now = System.currentTimeMillis();
        Sleeper.sleepMillis(10);// to make sure time has passed

        Properties meta = null;
        CurrentDateTagger tagger = null;

        meta = new Properties();
        tagger = new CurrentDateTagger();
        tagger.setFormat("yyyy-MM-dd'T'HH:mm:ss");
        tagger.tagDocument("n/a", null, meta, true);
        Assertions.assertTrue(
                meta.getString(Doc.DOC_IMPORTED_DATE).matches(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
                "Returned date format does not match");

        meta = new Properties();
        tagger = new CurrentDateTagger();
        tagger.setFormat("EEEE");
        tagger.setLocale(Locale.CANADA_FRENCH);
        tagger.tagDocument("n/a", null, meta, true);
        Assertions.assertTrue(
                ArrayUtils.contains(new String[]{
                        "lundi", "mardi", "mercredi", "jeudi", "vendredi",
                        "samedi", "dimanche"},
                        meta.getString(Doc.DOC_IMPORTED_DATE)),
                "Returned date format does not match");

        meta = new Properties();
        meta.add("existingField", "1002727941000");
        tagger = new CurrentDateTagger();
        tagger.setOnSet(PropertySetter.REPLACE);
        tagger.setToField("existingField");
        tagger.tagDocument("n/a", null, meta, true);
        Assertions.assertEquals(
                1, meta.getLongs("existingField").size(),
                "Invalid overwritten number of date values");
        Assertions.assertTrue(
                meta.getLong("existingField") > now,
                "Invalid overwritten date created");

        meta = new Properties();
        meta.add("existingField", "1002727941000");
        tagger = new CurrentDateTagger();
        tagger.setOnSet(PropertySetter.APPEND);
        tagger.setToField("existingField");
        tagger.tagDocument("n/a", null, meta, true);
        Assertions.assertEquals(
                2, meta.getLongs("existingField").size(),
                "Invalid added number of date values");
        List<Long> longs = meta.getLongs("existingField");
        for (Long dateLong : longs) {
            if (dateLong == 1002727941000L) {
                continue;
            } else {
                Assertions.assertTrue(
                        dateLong > now, "Invalid added date created");
            }
        }
    }

    @Test
    public void testWriteRead() throws IOException {
        CurrentDateTagger tagger = new CurrentDateTagger();
        tagger.setToField("field1");
        tagger.setFormat("yyyy-MM-dd");
        tagger.setOnSet(PropertySetter.REPLACE);
        XML.assertWriteRead(tagger, "handler");
    }
}
