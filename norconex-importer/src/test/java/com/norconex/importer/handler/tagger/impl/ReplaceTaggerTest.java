/* Copyright 2010-2014 Norconex Inc.
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
import com.norconex.importer.handler.tagger.impl.ReplaceTagger;

public class ReplaceTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        ReplaceTagger tagger = new ReplaceTagger();
        tagger.addReplacement("fromValue1", "toValue1", "fromName1");
        tagger.addReplacement("fromValue2", "toValue2", "fromName1");
        tagger.addReplacement("fromValue1", "toValue1", "fromName2", "toName2");
        tagger.addReplacement("fromValue3", "toValue3", "fromName3", "toName3");
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

    
    @Test
    public void testRegularReplace() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("fullMatchField", "full value match"); 
        meta.addString("partialNoMatchField", "partial value nomatch"); 
        meta.addString("matchOldField", "match to new field"); 
        meta.addString("nomatchOldField", "no match to new field"); 
        
        ReplaceTagger tagger = new ReplaceTagger();
        tagger.addReplacement("full value match", "replaced", 
                "fullMatchField");
        tagger.addReplacement("bad if you see me", "not replaced", 
                "partialNoMatchField");
        tagger.addReplacement("match to new field", "replaced to new field", 
                "matchOldField", "matchNewField");
        tagger.addReplacement("bad if you see me", "not replaced", 
                "nomatchOldField", "nomatchNewField");

        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals("replaced", meta.getString("fullMatchField"));
        Assert.assertEquals(
                "partial value nomatch", meta.getString("partialNoMatchField"));
        Assert.assertEquals("replaced to new field", 
                meta.getString("matchNewField"));
        Assert.assertEquals("no match to new field", 
                meta.getString("nomatchOldField"));
        Assert.assertNull(meta.getString("nomatchNewField"));
    }
    
    @Test
    public void testRegexReplace() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("path1", "/this/is/a/path/file.doc"); 
        meta.addString("path2", "/that/is/a/path/file.doc"); 
        
        ReplaceTagger tagger = new ReplaceTagger();
        tagger.addReplacement("(.*)/.*", "$1", "path1", true);
        tagger.addReplacement("(.*)/.*", "$1", "path2", "folder", true);
        
        
        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals("/this/is/a/path", meta.getString("path1"));
        Assert.assertEquals("/that/is/a/path", meta.getString("folder"));
        Assert.assertEquals(
                "/that/is/a/path/file.doc", meta.getString("path2"));
    }

}
