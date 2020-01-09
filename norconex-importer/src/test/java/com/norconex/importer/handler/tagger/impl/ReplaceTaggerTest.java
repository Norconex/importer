/* Copyright 2010-2019 Norconex Inc.
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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.ReplaceTagger.Replacement;

public class ReplaceTaggerTest {

    // Test for: https://github.com/Norconex/collector-http/issues/416
    @Test
    public void testNoValue()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("test", "a b c");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        // regex
        r = new Replacement();
        r.setFromField("test");
        r.setToField("regex");
        r.setRegex(true);
        r.setFromValue("\\s+b\\s+");
        r.setToValue("");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);
        tagger.tagDocument("n/a", null, meta, true);

        // normal
        r = new Replacement();
        r.setFromField("test");
        r.setToField("normal");
        r.setRegex(false);
        r.setFromValue("b");
        r.setToValue("");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals("ac", meta.getString("regex"));
        Assertions.assertEquals("a  c", meta.getString("normal"));

        // XML
        String xml = "<tagger>"
                + "<replace fromField=\"test\" toField=\"regexXML\" "
                + "regex=\"true\">"
                + "  <fromValue>(.{0,0})\\s+b\\s+</fromValue>"
                + "</replace>"
                + "<replace fromField=\"test\" toField=\"normalXML\">"
                + "  <fromValue>b</fromValue>"
                + "</replace>"
                + "</tagger>";
        tagger = new ReplaceTagger();
        tagger.loadFromXML(new XML(xml));
        r = new Replacement();
        tagger.addReplacement(r);
        tagger.tagDocument("n/a", null, meta, true);
        Assertions.assertEquals("ac", meta.getString("regexXML"));
        Assertions.assertEquals("a  c", meta.getString("normalXML"));
    }

    //This is a test for https://github.com/Norconex/importer/issues/29
    //where the replaced value is equal to the original (EXP_NAME1), it should
    //still store it (was a bug).
    @Test
    public void testMatchReturnSameValue()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("EXP_NAME+COUNTRY1", "LAZARUS ANDREW");
        meta.add("EXP_NAME+COUNTRY2", "LAZARUS ANDREW [US]");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        // Author 1
        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY1");
        r.setToField("EXP_NAME1");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$1");
        r.setDiscardUnchanged(false);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY1");
        r.setToField("EXP_COUNTRY1");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$3");
        r.setDiscardUnchanged(false);
        tagger.addReplacement(r);

        // Author 2
        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY2");
        r.setToField("EXP_NAME2");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$1");
        r.setDiscardUnchanged(false);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("EXP_NAME+COUNTRY2");
        r.setToField("EXP_COUNTRY2");
        r.setRegex(true);
        r.setFromValue("^(.+?)(.?\\[([A-Z]+)\\])?$");
        r.setToValue("$3");
        r.setDiscardUnchanged(false);
        tagger.addReplacement(r);

        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals("LAZARUS ANDREW", meta.getString("EXP_NAME1"));
        Assertions.assertEquals("", meta.getString("EXP_COUNTRY1"));
        Assertions.assertEquals("LAZARUS ANDREW", meta.getString("EXP_NAME2"));
        Assertions.assertEquals("US", meta.getString("EXP_COUNTRY2"));
    }

    @Test
    public void testWriteRead() throws IOException {
        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        r = new Replacement();
        r.setFromValue("fromValue1");
        r.setToValue("toValue1");
        r.setFromField("fromName1");
        r.setOnSet(PropertySetter.REPLACE);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue2");
        r.setToValue("toValue2");
        r.setFromField("fromName1");
        r.setRegex(true);
        r.setOnSet(PropertySetter.PREPEND);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue1");
        r.setToValue("toValue1");
        r.setFromField("fromName2");
        r.setToField("toName2");
        r.setCaseSensitive(true);
        r.setOnSet(PropertySetter.OPTIONAL);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("fromValue3");
        r.setToValue("toValue3");
        r.setFromField("fromName3");
        r.setToField("toName3");
        r.setRegex(true);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        XML.assertWriteRead(tagger, "handler");
    }


    @Test
    public void testRegularReplace() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("fullMatchField", "full value match");
        meta.add("partialNoMatchField", "partial value nomatch");
        meta.add("matchOldField", "match to new field");
        meta.add("nomatchOldField", "no match to new field");
        meta.add("caseField", "Value Of Mixed Case");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        r = new Replacement();
        r.setFromValue("full value match");
        r.setToValue("replaced");
        r.setFromField("fullMatchField");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("bad if you see me");
        r.setToValue("not replaced");
        r.setFromField("partialNoMatchField");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("match to new field");
        r.setToValue("replaced to new field");
        r.setFromField("matchOldField");
        r.setToField("matchNewField");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("bad if you see me");
        r.setToValue("not replaced");
        r.setFromField("nomatchOldField");
        r.setToField("nomatchNewField");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("value Of mixed case");
        r.setToValue("REPLACED");
        r.setFromField("caseField");
        r.setCaseSensitive(false);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals("replaced", meta.getString("fullMatchField"));
        Assertions.assertNull(meta.getString("partialNoMatchField"));
        Assertions.assertEquals("replaced to new field",
                meta.getString("matchNewField"));
        Assertions.assertEquals("no match to new field",
                meta.getString("nomatchOldField"));
        Assertions.assertNull(meta.getString("nomatchNewField"));
        Assertions.assertEquals("REPLACED", meta.getString("caseField"));
    }

    @Test
    public void testRegexReplace() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("path1", "/this/is/a/path/file.doc");
        meta.add("path2", "/that/is/a/path/file.doc");
        meta.add("path3", "/That/Is/A/Path/File.doc");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        r = new Replacement();
        r.setFromValue("(.*)/.*");
        r.setToValue("$1");
        r.setFromField("path1");
        r.setRegex(true);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("(.*)/.*");
        r.setToValue("$1");
        r.setFromField("path2");
        r.setToField("folder");
        r.setRegex(true);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("file");
        r.setToValue("something");
        r.setFromField("path3");
        r.setRegex(true);
        r.setCaseSensitive(false);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals("/this/is/a/path", meta.getString("path1"));
        Assertions.assertEquals("/that/is/a/path", meta.getString("folder"));
        Assertions.assertEquals(
                "/that/is/a/path/file.doc", meta.getString("path2"));
        Assertions.assertEquals(
                "/That/Is/A/Path/something.doc", meta.getString("path3"));
    }

    @Test
    public void testWholeAndPartialMatches()
             throws IOException, ImporterHandlerException {
        String originalValue = "One dog, two dogs, three dogs";


        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field", originalValue);

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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One DOG, two DOGS, three DOGS");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlInsensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(false);
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One DOG, two DOGS, three DOGS");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlSensitiveUnchanged2");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One dog, two dogs, three dogs");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeNrmlSensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(false);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One DOG.*");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeRgxInsensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(false);
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("One dog.*");
        r.setToValue("One cat, two cats, three cats");
        r.setFromField("field");
        r.setToField("wholeRgxSensitiveCats");
        r.setWholeMatch(true);
        r.setRegex(true);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partNrmlSensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(false);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("dog");
        r.setToValue("cat");
        r.setFromField("field");
        r.setToField("partRgxSensitive1Cat");
        r.setWholeMatch(false);
        r.setRegex(true);
        r.setCaseSensitive(true);
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        //--- Whole-match regular replace, case insensitive --------------------
        Assertions.assertNull(meta.getString("wholeNrmlInsensitiveUnchanged"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeNrmlInsensitiveCats"));

        //--- Whole-match regular replace, case sensitive ----------------------
        Assertions.assertNull(meta.getString("wholeNrmlSensitiveUnchanged1"));
        Assertions.assertNull(meta.getString("wholeNrmlSensitiveUnchanged2"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeNrmlSensitiveCats"));

        //--- Whole-match regex replace, case insensitive ----------------------
        Assertions.assertNull(meta.getString("wholeRgxInsensitiveUnchanged"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeRgxInsensitiveCats"));

        //--- Whole-match regex replace, case sensitive ------------------------
        Assertions.assertNull(meta.getString("wholeRgxSensitiveUnchanged"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("wholeRgxSensitiveCats"));

        //--- Partial-match regular replace, case insensitive ------------------
        Assertions.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partNrmlInsensitive1Cat"));

        //--- Partial-match regular replace, case sensitive --------------------
        Assertions.assertNull(meta.getString("partNrmlSensitiveUnchanged"));
        Assertions.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partNrmlSensitive1Cat"));

        //--- Partial-match regex replace, case insensitive --------------------
        Assertions.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partRgxInsensitive1Cat"));

        //--- Partial-match regex replace, case sensitive ----------------------
        Assertions.assertNull(meta.getString("partRgxSensitiveUnchanged"));
        Assertions.assertEquals("One cat, two dogs, three dogs",
                meta.getString("partRgxSensitive1Cat"));
    }

    @Test
    public void testReplaceAll()
             throws IOException, ImporterHandlerException {


        String originalValue = "One dog, two dogs, three dogs";
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field", originalValue);

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
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
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
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertNull(meta.getString("wholeNrmlUnchanged"));
        Assertions.assertNull(meta.getString("wholeRegexUnchanged"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("partialNrmlCats"));
        Assertions.assertEquals("One cat, two cats, three cats",
                meta.getString("partialRegexCats"));
    }


    @Test
    public void testDiscardUnchanged()
             throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("test1", "keep me");
        meta.add("test2", "throw me");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        r = new Replacement();
        r.setFromValue("nomatch");
        r.setToValue("isaidnomatch");
        r.setFromField("test1");
        r.setDiscardUnchanged(false);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromValue("nomatch");
        r.setToValue("isaidnomatch");
        r.setFromField("test2");
        r.setDiscardUnchanged(true);
        tagger.addReplacement(r);

        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals("keep me", meta.getString("test1"));
        Assertions.assertNull(meta.getString("test2"));
    }

    @Test
    public void testOnSet()
             throws IOException, ImporterHandlerException {

        // Test what happens when target field already has a value

        ImporterMetadata meta = new ImporterMetadata();
        meta.add("source1", "value 1");
        meta.add("target1", "target value 1");
        meta.add("source2", "value 2");
        meta.add("target2", "target value 2");
        meta.add("source3", "value 3");
        meta.add("target3", "target value 3");
        meta.add("source4", "value 4");
        meta.add("target4", "target value 4");

        Replacement r = null;
        ReplaceTagger tagger = new ReplaceTagger();

        r = new Replacement();
        r.setFromField("source1");
        r.setToField("target1");
        r.setFromValue("value");
        r.setToValue("source value");
        r.setOnSet(PropertySetter.APPEND);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("source2");
        r.setToField("target2");
        r.setFromValue("value");
        r.setToValue("source value");
        r.setOnSet(PropertySetter.PREPEND);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("source3");
        r.setToField("target3");
        r.setFromValue("value");
        r.setToValue("source value");
        r.setOnSet(PropertySetter.REPLACE);
        tagger.addReplacement(r);

        r = new Replacement();
        r.setFromField("source4");
        r.setToField("target4");
        r.setFromValue("value");
        r.setToValue("source value");
        r.setOnSet(PropertySetter.OPTIONAL);
        tagger.addReplacement(r);

        //=== Asserts ==========================================================
        tagger.tagDocument("n/a", null, meta, true);

        Assertions.assertEquals(
                "target value 1, source value 1",
                StringUtils.join(meta.getStrings("target1"), ", "));
        Assertions.assertEquals(
                "source value 2, target value 2",
                StringUtils.join(meta.getStrings("target2"), ", "));
        Assertions.assertEquals(
                "source value 3",
                StringUtils.join(meta.getStrings("target3"), ", "));
        Assertions.assertEquals(
                "target value 4",
                StringUtils.join(meta.getStrings("target4"), ", "));
    }
}
