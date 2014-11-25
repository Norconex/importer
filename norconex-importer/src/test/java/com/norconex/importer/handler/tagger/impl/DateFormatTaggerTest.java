/* Copyright 2014 Norconex Inc.
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

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class DateFormatTaggerTest {

    
    @Test
    public void testDateFormatTagger() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("datefield1", "2001-10-10T11:32:21"); 
        meta.addString("datefield2", "1002727941000"); 
        
        DateFormatTagger tagger = new DateFormatTagger();

        tagger.setOverwrite(true);

        tagger.setFromField("datefield1");
        tagger.setToField("tofield1");
        tagger.setFromFormat("yyyy-MM-dd'T'HH:mm:ss");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("1002727941000", meta.getString("tofield1"));

        tagger.setFromField("datefield1");
        tagger.setToField("tofield2");
        tagger.setFromFormat("yyyy-MM-dd'T'HH:mm:ss");
        tagger.setToFormat("yyyy/MM/dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001/10/10", meta.getString("tofield2"));

        tagger.setFromField("datefield2");
        tagger.setToField("tofield3");
        tagger.setFromFormat(null);
        tagger.setToFormat("yyyy/MM/dd");
        tagger.tagDocument("n/a", null, meta, true);
        Assert.assertEquals("2001/10/10", meta.getString("tofield3"));
        
        
    }
    
    @Test
    public void testWriteRead() throws IOException {
        DateFormatTagger tagger = new DateFormatTagger();
        tagger.setFromField("fromField1");
        tagger.setToField("toField1");
        tagger.setFromFormat("yyyy-MM-dd");
        tagger.setToFormat("yyyy-MM");
        tagger.setKeepBadDates(true);
        tagger.setOverwrite(true);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
