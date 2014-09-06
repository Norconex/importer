/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
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
