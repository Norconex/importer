/* Copyright 2014-2018 Norconex Inc.
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
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class DateFormatTaggerTest {

    @Test
    public void testMultiFromFormatTagger() throws ImporterHandlerException {
        String dateISOFormat = "yyyy-MM-dd'T'HH:mm:ss";
        String dateEPOCHFormat = "EPOCH";
        String dateHTTPFormat = "EEE, dd MMM yyyy HH:mm:ss";


        String dateISO = "2001-10-10T11:32:21";
        String dateEPOCH = "1002727941000";
        String dateHTTP = "Wed, 10 Oct 2001 11:32:21";

        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("dateISO", dateISO);
        meta.addString("dateEPOCH", dateEPOCH);
        meta.addString("dateHTTP",  dateHTTP);

        DateFormatTagger t = new DateFormatTagger();
        t.setToField("date");
        t.setOverwrite(true);
        t.setKeepBadDates(false);

        // Test ISO to EPOCH
        t.setFromField("dateISO");
        t.setToFormat(dateEPOCHFormat);
        t.setFromFormats(dateHTTPFormat, dateEPOCHFormat, dateISOFormat);
        t.tagDocument("n/a", null, meta, true);
        Assert.assertEquals(dateEPOCH, meta.getString("date"));

        // Test EPOCH to ISO
        meta.remove("date");
        t.setFromField("dateEPOCH");
        t.setToFormat(dateISOFormat);
        t.setFromFormats(dateHTTPFormat, dateISOFormat, dateEPOCHFormat);
        t.tagDocument("n/a", null, meta, true);
        Assert.assertEquals(dateISO, meta.getString("date"));

        // Test HTTP to ISO
        meta.remove("date");
        t.setFromField("dateHTTP");
        t.setToFormat(dateISOFormat);
        t.setFromFormats(dateISOFormat, dateEPOCHFormat, dateHTTPFormat);
        t.tagDocument("n/a", null, meta, true);
        Assert.assertEquals(dateISO, meta.getString("date"));

        // Test No match
        meta.remove("date");
        t.setFromField("dateHTTP");
        t.setToFormat(dateISOFormat);
        t.setFromFormats(dateISOFormat, dateEPOCHFormat);
        t.tagDocument("n/a", null, meta, true);
        Assert.assertEquals(null, meta.getString("date"));

    }

    @Test
    public void testDateFormat() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("datefield1", "2001-10-10T11:32:21");
        meta.addString("datefield2", "1002727941000");

        DateFormatTagger tagger;

        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("datefield1");
        tagger.setToField("tofield1");
        tagger.setFromFormats("yyyy-MM-dd'T'HH:mm:ss");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("1002727941000", meta.getString("tofield1"));

        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("datefield1");
        tagger.setToField("tofield2");
        tagger.setFromFormats("yyyy-MM-dd'T'HH:mm:ss");
        tagger.setToFormat("yyyy/MM/dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001/10/10", meta.getString("tofield2"));

        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("datefield2");
        tagger.setToField("tofield3");
        tagger.setFromFormats((String) null);
        tagger.setToFormat("yyyy/MM/dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001/10/10", meta.getString("tofield3"));
    }

    @Test
    public void testLocalizedDateFormatting() throws ImporterHandlerException {
        ImporterMetadata meta;
        DateFormatTagger tagger;

        meta = new ImporterMetadata();
        meta.addString("sourceField", "2001-04-10T11:32:21");
        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("sourceField");
        tagger.setToField("targetField");
        tagger.setFromFormats("yyyy-MM-dd'T'HH:mm:ss");
        tagger.setToFormat("EEE, dd MMM yyyy");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("Tue, 10 Apr 2001", meta.getString("targetField"));

        meta = new ImporterMetadata();
        meta.addString("sourceField", "2001-04-10T11:32:21");
        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("sourceField");
        tagger.setToField("targetField");
        tagger.setFromFormats("yyyy-MM-dd'T'HH:mm:ss");
        tagger.setToFormat("EEE, dd MMM yyyy");
        tagger.setToLocale(Locale.CANADA_FRENCH);
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("mar., 10 avr. 2001", meta.getString("targetField"));
    }

    @Test
    public void testLocalizedDateParsing() throws ImporterHandlerException {
        ImporterMetadata meta;
        DateFormatTagger tagger;

        meta = new ImporterMetadata();
        meta.addString("sourceField", "Tue, 10 Apr 2001");
        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("sourceField");
        tagger.setToField("targetField");
        tagger.setFromFormats("EEE, dd MMM yyyy");
        tagger.setToFormat("yyyy-MM-dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001-04-10", meta.getString("targetField"));

        meta = new ImporterMetadata();
        meta.addString("sourceField", "mar., 10 avr. 2001");
        tagger = new DateFormatTagger();
        tagger.setOverwrite(true);
        tagger.setFromField("sourceField");
        tagger.setFromLocale(Locale.CANADA_FRENCH);
        tagger.setToField("targetField");
        tagger.setFromFormats("EEE, dd MMM yyyy");
        tagger.setToFormat("yyyy-MM-dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001-04-10", meta.getString("targetField"));
    }
    @Test
    public void testWriteRead() throws IOException {
        DateFormatTagger tagger = new DateFormatTagger();
        tagger.setFromField("fromField1");
        tagger.setToField("toField1");
        tagger.setFromFormats("yyyy-MM-dd", "anotherOne", "aThirdOne");
        tagger.setToFormat("yyyy-MM");
        tagger.setKeepBadDates(true);
        tagger.setOverwrite(true);
        XML.assertWriteRead(tagger, "handler");
    }
}
