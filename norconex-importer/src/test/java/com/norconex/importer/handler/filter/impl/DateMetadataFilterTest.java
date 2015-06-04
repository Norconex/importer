/* Copyright 2015 Norconex Inc.
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
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter.Operator;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter.TimeUnit;

public class DateMetadataFilterTest {

    @Test
    public void testAcceptDocument() 
            throws IOException, ImporterHandlerException, ParseException {
        
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "1980-12-21T12:22:01.123");

        DateMetadataFilter filter = null;

        filter = new DateMetadataFilter();
        filter.setField("field1");
        filter.setFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

        filter.addCondition(Operator.LOWER_EQUAL, 
                DateFormatUtils.ISO_DATE_FORMAT.parse("1980-12-21"));
        Assert.assertFalse(filter.acceptDocument("n/a", null, meta, false));

        
        filter = new DateMetadataFilter();
        filter.setField("field1");
        filter.setFormat("yyyy-MM-dd");
        filter.addCondition(Operator.LOWER_EQUAL, 
                DateFormatUtils.ISO_DATE_FORMAT.parse("1980-12-21"));
        Assert.assertTrue(filter.acceptDocument("n/a", null, meta, false));

        
        filter = new DateMetadataFilter();
        filter.setField("field1");
        filter.setFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        filter.addCondition(Operator.LOWER_EQUAL, 
                DateFormatUtils.ISO_DATE_FORMAT.parse("1980-12-22"));
        Assert.assertTrue(filter.acceptDocument("n/a", null, meta, false));

        
        Calendar now = Calendar.getInstance();
        meta.addString("field2",  
                DateFormatUtils.ISO_DATETIME_FORMAT.format(now));

        filter = new DateMetadataFilter();
        filter.setField("field2");
        filter.setFormat("yyyy-MM-dd'T'hh:mm:ss");
        filter.addConditionFromNow(
                Operator.GREATER_THAN, TimeUnit.MINUTE, -1, true);
        filter.addConditionFromNow(
                Operator.LOWER_THAN, TimeUnit.MINUTE, +1, true);
        Assert.assertTrue(filter.acceptDocument("n/a", null, meta, false));

    }    
    
    @Test
    public void testWriteRead() throws IOException {
        DateMetadataFilter filter = new DateMetadataFilter();
        filter.setField("field1");
        filter.setFormat("yyyy-MM-dd");
        filter.setOnMatch(OnMatch.EXCLUDE);
        filter.addCondition(Operator.GREATER_EQUAL, new Date());
        filter.addCondition(Operator.LOWER_THAN, 
                new Date(System.currentTimeMillis() + 1000 * 10));
        // Cannot test equality when condition is fixed since the initialization
        // time will vary. So test with last argument false.
        filter.addConditionFromNow(Operator.EQUALS, TimeUnit.YEAR, -2, false);
        System.out.println("Writing/Reading this: " + filter);
        ConfigurationUtil.assertWriteRead(filter);
    }
}
