/* Copyright 2016-2019 Norconex Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.CountMatchesTagger.MatchDetails;

public class CountMatchesTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        MatchDetails m = null;

        CountMatchesTagger tagger = new CountMatchesTagger();

        m = new MatchDetails();
        m.setFromField("fromFiel1");
        m.setToField("toField1");
        m.setValue("value1");
        m.setCaseSensitive(true);
        m.setRegex(true);
        tagger.addMatchDetails(m);

        m = new MatchDetails();
        m.setToField("toField2");
        m.setValue("value2");
        tagger.addMatchDetails(m);

        XML.assertWriteRead(tagger, "handler");
    }

    @Test
    public void testMatchesCount()
            throws ImporterHandlerException, IOException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("url", "http://domain/url/test");
        meta.add("fruits", "grapefruit, apple, orange, APPLE");
        String content = "potato carrot Potato";

        MatchDetails m = null;

        CountMatchesTagger tagger = new CountMatchesTagger();

        // Count slashes with substrings (4)
        m = new MatchDetails();
        m.setFromField("url");
        m.setToField("slashesCountNormal");
        m.setValue("/");
        tagger.addMatchDetails(m);
        // Count slashes with regex (4)
        m = new MatchDetails();
        m.setFromField("url");
        m.setToField("slashesCountRegex");
        m.setValue("/");
        m.setRegex(true);
        tagger.addMatchDetails(m);
        // Count URL segments (3)
        m = new MatchDetails();
        m.setFromField("url");
        m.setToField("segmentCountRegex");
        m.setValue("/[^/]+");
        m.setRegex(true);
        tagger.addMatchDetails(m);

        // Count fruits with substrings case-sensitive (1)
        m = new MatchDetails();
        m.setFromField("fruits");
        m.setToField("appleCountSensitiveNormal");
        m.setValue("apple");
        m.setCaseSensitive(true);
        tagger.addMatchDetails(m);
        // Count fruits with substrings case-insensitive (2)
        m = new MatchDetails();
        m.setFromField("fruits");
        m.setToField("appleCountInsensitiveNormal");
        m.setValue("apple");
        tagger.addMatchDetails(m);
        // Count fruits with regex case-sensitive (3)
        m = new MatchDetails();
        m.setFromField("fruits");
        m.setToField("fruitsCountSensitiveRegex");
        m.setValue("(apple|orange|grapefruit)");
        m.setRegex(true);
        m.setCaseSensitive(true);
        tagger.addMatchDetails(m);
        // Count fruits with regex case-insensitive (4)
        m = new MatchDetails();
        m.setFromField("fruits");
        m.setToField("fruitsCountInsensitiveRegex");
        m.setValue("(apple|orange|grapefruit)");
        m.setRegex(true);
        tagger.addMatchDetails(m);


        // Count vegetables with substrings case-sensitive (1)
        m = new MatchDetails();
        m.setToField("potatoCountSensitiveNormal");
        m.setValue("potato");
        m.setCaseSensitive(true);
        tagger.addMatchDetails(m);
        // Count vegetables  with substrings case-insensitive (2)
        m = new MatchDetails();
        m.setToField("potatoCountInsensitiveNormal");
        m.setValue("potato");
        tagger.addMatchDetails(m);
        // Count vegetables  with regex case-sensitive (2)
        m = new MatchDetails();
        m.setToField("vegetableCountSensitiveRegex");
        m.setValue("(potato|carrot)");
        m.setRegex(true);
        m.setCaseSensitive(true);
        tagger.addMatchDetails(m);
        // Count vegetables  with regex case-insensitive (3)
        m = new MatchDetails();
        m.setToField("vegetableCountInsensitiveRegex");
        m.setValue("(potato|carrot)");
        m.setRegex(true);
        tagger.addMatchDetails(m);

        tagger.tagDocument("n/a", IOUtils.toInputStream(
                content, StandardCharsets.UTF_8), meta, true);

        assertEquals(4, (int) meta.getInteger("slashesCountNormal"));
        assertEquals(4, (int) meta.getInteger("slashesCountRegex"));
        assertEquals(3, (int) meta.getInteger("segmentCountRegex"));

        assertEquals(1, (int) meta.getInteger("appleCountSensitiveNormal"));
        assertEquals(2, (int) meta.getInteger("appleCountInsensitiveNormal"));
        assertEquals(3, (int) meta.getInteger("fruitsCountSensitiveRegex"));
        assertEquals(4, (int) meta.getInteger("fruitsCountInsensitiveRegex"));

        assertEquals(1, (int) meta.getInteger("potatoCountSensitiveNormal"));
        assertEquals(2, (int) meta.getInteger("potatoCountInsensitiveNormal"));
        assertEquals(2, (int) meta.getInteger("vegetableCountSensitiveRegex"));
        assertEquals(3,
                (int) meta.getInteger("vegetableCountInsensitiveRegex"));
    }

    @Test
    public void testLargeContent()
            throws ImporterHandlerException, IOException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("fruits", "orange orange");
        String content = "potato whatever whatever whatever whatever"
                + "potato whatever whatever whatever whatever";

        CountMatchesTagger tagger = new CountMatchesTagger();
        tagger.setMaxReadSize(20);

        MatchDetails m = null;

        m = new MatchDetails();
        m.setToField("potatoCount");
        m.setValue("potato");
        tagger.addMatchDetails(m);

        m = new MatchDetails();
        m.setFromField("fruits");
        m.setToField("orangeCount");
        m.setValue("orange");
        tagger.addMatchDetails(m);

        tagger.tagDocument("n/a", IOUtils.toInputStream(
                content, StandardCharsets.UTF_8), meta, true);

        assertEquals(2, (int) meta.getInteger("potatoCount"));
        assertEquals(2, (int) meta.getInteger("orangeCount"));
    }

    @Test
    public void testAddToSameFieldAndNoMatch()
            throws ImporterHandlerException, IOException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("orange", "orange orange");
        meta.add("apple", "apple apple apple");
        meta.add("potato", "carrot");

        CountMatchesTagger tagger = new CountMatchesTagger();
        tagger.setMaxReadSize(20);

        MatchDetails m = null;

        m = new MatchDetails();
        m.setFromField("orange");
        m.setToField("fruitCount");
        m.setValue("orange");
        tagger.addMatchDetails(m);

        m = new MatchDetails();
        m.setFromField("apple");
        m.setToField("fruitCount");
        m.setValue("apple");
        tagger.addMatchDetails(m);

        m = new MatchDetails();
        m.setFromField("potato");
        m.setToField("potatoCount");
        m.setValue("potato");
        tagger.addMatchDetails(m);

        tagger.tagDocument("n/a", null, meta, true);

        // we should get the sum of both oranges and apples
        assertEquals(5, (int) meta.getInteger("fruitCount"));
        // we should get zero (use string to make sure).
        assertEquals("0", meta.getString("potatoCount"));
    }
}
