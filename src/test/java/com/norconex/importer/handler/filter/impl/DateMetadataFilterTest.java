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
package com.norconex.importer.handler.filter.impl;

import static com.norconex.importer.parser.ParseState.PRE;
import static org.apache.commons.lang3.time.DateUtils.truncate;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter.Condition;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter.Operator;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter.TimeUnit;

class DateMetadataFilterTest {

    @Test
    void testAcceptDocument()
            throws ImporterHandlerException, ParseException {

        Properties meta = new Properties();
        meta.add("field1", "1980-12-21T12:22:01.123");

        DateMetadataFilter filter = null;

        filter = new DateMetadataFilter();
        filter.setFieldMatcher(TextMatcher.basic("field1"));
        filter.setFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        filter.addCondition(Operator.LOWER_EQUAL,
                DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(
                        "1980-12-21"));
        Assertions.assertFalse(TestUtil.filter(filter, "n/a", null, meta, PRE));


        filter = new DateMetadataFilter();
        filter.setFieldMatcher(TextMatcher.basic("field1"));
        filter.setFormat("yyyy-MM-dd");
        filter.addCondition(Operator.LOWER_EQUAL,
                DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(
                        "1980-12-21"));
        Assertions.assertTrue(TestUtil.filter(filter, "n/a", null, meta, PRE));


        filter = new DateMetadataFilter();
        filter.setFieldMatcher(TextMatcher.basic("field1"));
        filter.setFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        filter.addCondition(Operator.LOWER_EQUAL,
                DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(
                        "1980-12-22"));
        Assertions.assertTrue(TestUtil.filter(filter, "n/a", null, meta, PRE));


        Calendar now = Calendar.getInstance();
        meta.add("field2",
                DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(now));

        filter = new DateMetadataFilter();
        filter.setFieldMatcher(TextMatcher.basic("field2"));
        filter.setFormat("yyyy-MM-dd'T'HH:mm:ss");
        filter.addConditionFromNow(
                Operator.GREATER_THAN, TimeUnit.MINUTE, -1, true);
        filter.addConditionFromNow(
                Operator.LOWER_THAN, TimeUnit.MINUTE, +1, true);
        Assertions.assertTrue(TestUtil.filter(filter, "n/a", null, meta, PRE));

    }

    @Test
    void testWriteRead() {
        DateMetadataFilter filter = new DateMetadataFilter();
        filter.setFieldMatcher(TextMatcher.basic("field1"));
        filter.setFormat("yyyy-MM-dd");
        filter.setOnMatch(OnMatch.EXCLUDE);
        filter.addCondition(Operator.GREATER_EQUAL, new Date());
        filter.addCondition(Operator.LOWER_THAN,
                new Date(System.currentTimeMillis() + 1000 * 10));
        // Cannot test equality when condition is fixed since the initialization
        // time will vary. So test with last argument false.
        filter.addConditionFromNow(Operator.EQUALS, TimeUnit.YEAR, -2, false);
        XML.assertWriteRead(filter, "handler");
    }

    @Test
    void testOperatorDateParsing() {

        XML xml = XML.of(
                  "<handler\n"
                + "    class=\"DateMetadataFilter\""
                + "    format=\"yyyy-MM-dd'T'HH:mm:ss'Z'\""
                + "    onMatch=\"exclude\">"
                + "  <fieldMatcher>scan_timestamp</fieldMatcher>"
                + "  <condition operator=\"lt\" date=\"TODAY\"/>"
                + "  <condition operator=\"lt\" date=\"2020-09-27T12:34:56\"/>"
                + "  <condition operator=\"lt\" date=\"2020-09-27\"/>"
                + "</handler>").create();
        DateMetadataFilter f = new DateMetadataFilter();
        f.loadFromXML(xml);

        List<Condition> conds = f.getConditions();

        // Assert valid date strings
        Assertions.assertEquals("TODAY", conds.get(0).getDateString());
        Assertions.assertEquals(
                "2020-09-27T12:34:56.000", conds.get(1).getDateString());
        Assertions.assertEquals(
                "2020-09-27T00:00:00.000", conds.get(2).getDateString());

        // Assert valid EPOCH
        Calendar cal = Calendar.getInstance();
        long today = truncate(cal, Calendar.DAY_OF_MONTH).getTimeInMillis();

        cal.set(2020, 9-1, 27, 12, 34, 56);
        long dateTime = truncate(cal, Calendar.SECOND).getTimeInMillis();
        long date = truncate(cal, Calendar.DAY_OF_MONTH).getTimeInMillis();

        Assertions.assertEquals(today, conds.get(0).getEpochDate());
        Assertions.assertEquals(dateTime, conds.get(1).getEpochDate());
        Assertions.assertEquals(date, conds.get(2).getEpochDate());
    }
}
