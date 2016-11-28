/* Copyright 2010-2016 Norconex Inc.
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
import com.norconex.importer.handler.tagger.impl.ReplaceTagger.Replacement;

public class ReplaceTaggerTest {

    //This is a test for https://github.com/Norconex/importer/issues/29
    //where the replaced value is equal to the original (EXP_NAME1), it should 
    //still store it (was a bug).
    @Test
    public void testMatchReturnSameValue() 
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("EXP_NAME+COUNTRY1", "LAZARUS ANDREW"); 
        meta.addString("EXP_NAME+COUNTRY2", "LAZARUS ANDREW [US]"); 
        
        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        // Author 1
        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY1");
        r.setToField("EXP_NAME1");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$1");
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY1");
        r.setToField("EXP_COUNTRY1");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$3");
        tagger.addReplacement(r);

        // Author 2
        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY2");
        r.setToField("EXP_NAME2");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$1");
        tagger.addReplacement(r);
        
        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY2");
        r.setToField("EXP_COUNTRY2");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$3");
        tagger.addReplacement(r);

        tagger.tagDocument("n/a", null, meta, true);
        
        Assert.assertEquals("LAZARUS ANDREW", meta.getString("EXP_NAME1"));
        Assert.assertEquals("", meta.getString("EXP_COUNTRY1"));
        Assert.assertEquals("LAZARUS ANDREW", meta.getString("EXP_NAME2"));
        Assert.assertEquals("US", meta.getString("EXP_COUNTRY2"));
    }
    
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

    @Test
    public void testWholeAndPartialMatches() 
             throws IOException, ImporterHandlerException {
        String originalValue = "One dog, two dogs, three dogs";

        
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field", originalValue);

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        //--- Whole-match regular replace, case insensitive --------------------
        r = new Replacement();
        r.setFromValue("One dog");
        r.setToValue("One cat");
        r.setFromField("field");
        r.setToField("wholeNrmlInsensitiveUnchanged");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One DOG, two DOGS, three DOGS");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlInsensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        //--- Whole-match regular replace, case sensitive ----------------------
        r = new Replacement();
        r.setFromValue("One dog");
        r.setToValue("One cat");
        r.setFromField("field");
        r.setToField("wholeNrmlSensitiveUnchanged1");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);
        
        r = new Replacement();
        r.setFromValue("One DOG, two DOGS, three DOGS");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlSensitiveUnchanged2");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One dog, two dogs, three dogs");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlSensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        //--- Whole-match regex replace, case insensitive ----------------------
        r = new Replacement();
        r.setFromValue("One dog");
        r.setToValue("One cat");
        r.setFromField("field");
        r.setToField("wholeRgxInsensitiveUnchanged");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One DOG.*");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeRgxInsensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        //--- Whole-match regex replace, case sensitive ------------------------
        r = new Replacement();
        r.setFromValue("One DOG.*");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeRgxSensitiveUnchanged");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One dog.*");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeRgxSensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        //--- Partial-match regular replace, case insensitive ------------------
        r = new Replacement();
        r.setFromValue("DOG");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partNrmlInsensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(false);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        //--- Partial-match regular replace, case sensitive --------------------
        r = new Replacement();
        r.setFromValue("DOG");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partNrmlSensitiveUnchanged");
        r.setWholeMatch(false);
        r.setRegex(false);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partNrmlSensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(false);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        //--- Partial-match regex replace, case insensitive --------------------
        r = new Replacement();
        r.setFromValue("DOG");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partRgxInsensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(true);
        r.setCaseSensitive(false);
        tagger.addReplacement(r);

        //--- Partial-match regex replace, case sensitive ----------------------
        r = new Replacement();
        r.setFromValue("DOG");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partRgxSensitiveUnchanged");
        r.setWholeMatch(false);
        r.setRegex(true);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partRgxSensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(true);
        r.setCaseSensitive(true);
        tagger.addReplacement(r);
        
        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        //--- Whole-match regular replace, case insensitive --------------------
        Assert.assertNull(meta.getString("wholeNrmlInsensitiveUnchanged"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeNrmlInsensitiveCats"));

        //--- Whole-match regular replace, case sensitive ----------------------
        Assert.assertNull(meta.getString("wholeNrmlSensitiveUnchanged1"));
        Assert.assertNull(meta.getString("wholeNrmlSensitiveUnchanged2"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeNrmlSensitiveCats"));

        //--- Whole-match regex replace, case insensitive ----------------------
        Assert.assertNull(meta.getString("wholeRgxInsensitiveUnchanged"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeRgxInsensitiveCats"));

        //--- Whole-match regex replace, case sensitive ------------------------
        Assert.assertNull(meta.getString("wholeRgxSensitiveUnchanged"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeRgxSensitiveCats"));

        //--- Partial-match regular replace, case insensitive ------------------
        Assert.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partNrmlInsensitive1Cat"));

        //--- Partial-match regular replace, case sensitive --------------------
        Assert.assertNull(meta.getString("partNrmlSensitiveUnchanged"));
        Assert.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partNrmlSensitive1Cat"));

        //--- Partial-match regex replace, case insensitive --------------------
        Assert.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partRgxInsensitive1Cat"));

        //--- Partial-match regex replace, case sensitive ----------------------
        Assert.assertNull(meta.getString("partRgxSensitiveUnchanged"));
        Assert.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partRgxSensitive1Cat"));
    }

    @Test
    public void testReplaceAll() 
             throws IOException, ImporterHandlerException {

        
        String originalValue = "One dog, two dogs, three dogs";
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field", originalValue);

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        //--- Whole-match regular replace all ----------------------------------
        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("wholeNrmlUnchanged");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setReplaceAll(true);
        tagger.addReplacement(r);

        //--- Whole-match regex replace all ------------------------------------
        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("wholeRegexUnchanged");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setReplaceAll(true);
        tagger.addReplacement(r);
        
        //--- Partial-match regular replace all --------------------------------
        r = new Replacement();
        r.setFromValue("DOG");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partialNrmlCats");
        r.setWholeMatch(false);
        r.setRegex(false);
        r.setReplaceAll(true);
        tagger.addReplacement(r);
        
        //--- Partial-match regex replace all ----------------------------------
        r = new Replacement();
        r.setFromValue("D.G");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partialRegexCats");
        r.setWholeMatch(false);
        r.setRegex(true);
        r.setReplaceAll(true);
        tagger.addReplacement(r);
        
        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertNull(meta.getString("wholeNrmlUnchanged"));
        Assert.assertNull(meta.getString("wholeRegexUnchanged"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("partialNrmlCats"));
        Assert.assertEquals("One cat, two cats, three cats",
                meta.getString("partialRegexCats"));
    }

}
