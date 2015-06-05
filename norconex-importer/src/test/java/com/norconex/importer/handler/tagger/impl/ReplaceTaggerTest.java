/* Copyright 2010-2015 Norconex Inc.
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
import com.norconex.importer.handler.tagger.impl.ReplaceTagger.Replacement;

public class ReplaceTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();
        
        r = new Replacement();
        r.setFromValue("fromValue1");
        r.setToValue("toValue1");
        r.setFromField("fromName1");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue2");
        r.setToValue("toValue2");
        r.setFromField("fromName1");
        r.setRegex(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue1");
        r.setToValue("toValue1");
        r.setFromField("fromName2");
        r.setToField("toName2");
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue3");
        r.setToValue("toValue3");
        r.setFromField("fromName3");
        r.setToField("toName3");
        r.setRegex(true);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);
        
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
        meta.addString("caseField", "Value Of Mixed Case"); 
        
        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();
        
        r = new Replacement();
        r.setFromValue("full value match");
        r.setToValue("replaced");
        r.setFromField("fullMatchField");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("bad if you see me");
        r.setToValue("not replaced");
        r.setFromField("partialNoMatchField");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("match to new field");
        r.setToValue("replaced to new field");
        r.setFromField("matchOldField");
        r.setToField("matchNewField");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("bad if you see me");
        r.setToValue("not replaced");
        r.setFromField("nomatchOldField");
        r.setToField("nomatchNewField");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("value Of mixed case");
        r.setToValue("REPLACED");
        r.setFromField("caseField");
        r.setCaseSensitive(false);
        tagger.addReplacement(r);
        
        
        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals("replaced", meta.getString("fullMatchField"));
        Assert.assertEquals(
                "partial value nomatch", meta.getString("partialNoMatchField"));
        Assert.assertEquals("replaced to new field", 
                meta.getString("matchNewField"));
        Assert.assertEquals("no match to new field", 
                meta.getString("nomatchOldField"));
        Assert.assertNull(meta.getString("nomatchNewField"));
        Assert.assertEquals("REPLACED", meta.getString("caseField"));
    }
    
    @Test
    public void testRegexReplace() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("path1", "/this/is/a/path/file.doc"); 
        meta.addString("path2", "/that/is/a/path/file.doc"); 
        meta.addString("path3", "/That/Is/A/Path/File.doc"); 
        
        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();
        
        r = new Replacement();
        r.setFromValue("(.*)/.*");
        r.setToValue("$1");
        r.setFromField("path1");
        r.setRegex(true);
        tagger.addReplacement(r);
        
        r = new Replacement();
        r.setFromValue("(.*)/.*");
        r.setToValue("$1");
        r.setFromField("path2");
        r.setToField("folder");
        r.setRegex(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("file");
        r.setToValue("something");
        r.setFromField("path3");
        r.setRegex(true);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);
        
        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals("/this/is/a/path", meta.getString("path1"));
        Assert.assertEquals("/that/is/a/path", meta.getString("folder"));
        Assert.assertEquals(
                "/that/is/a/path/file.doc", meta.getString("path2"));
        Assert.assertEquals(
                "/That/Is/A/Path/something.doc", meta.getString("path3"));
    }

}
