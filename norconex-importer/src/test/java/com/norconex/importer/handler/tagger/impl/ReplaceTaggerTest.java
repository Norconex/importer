/* Copyright 2010-2014 Norconex Inc.
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
