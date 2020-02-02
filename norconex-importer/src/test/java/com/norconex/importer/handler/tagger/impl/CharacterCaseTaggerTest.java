/* Copyright 2014-2019 Norconex Inc.
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

import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.APPLY_BOTH;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.APPLY_FIELD;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.APPLY_VALUE;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_LOWER;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_SENTENCES;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_SENTENCES_FULLY;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_STRING;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_STRING_FULLY;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_SWAP;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_UPPER;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_WORDS;
import static com.norconex.importer.handler.tagger.impl.CharacterCaseTagger.CASE_WORDS_FULLY;

import java.io.IOException;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CharacterCaseTaggerTest {

    @Test
    public void testUpperLowerValues()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field1", "Doit être upper");
        meta.add("field1", "Must be upper");
        meta.set("field2", "DOIT ÊTRE LOWER");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("field1"));
        tagger.setCaseType(CASE_UPPER);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("field2"));
        tagger.setCaseType(CASE_LOWER);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals(
                "DOIT ÊTRE UPPER", meta.getStrings("field1").get(0));
        Assertions.assertEquals(
                "MUST BE UPPER", meta.getStrings("field1").get(1));
        Assertions.assertEquals("doit être lower", meta.getString("field2"));
    }

    @Test
    public void testUpperLowerField()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("fieldMustBeUpper", "value 1");
        meta.add("fieldMustBeLower", "value 2");
        meta.set("fieldMustBeCapitalized", "value 3");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("fieldMustBeUpper"));
        tagger.setCaseType(CASE_UPPER);
        tagger.setApplyTo(APPLY_FIELD);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("fieldMustBeLower"));
        tagger.setCaseType(CASE_LOWER);
        tagger.setApplyTo(APPLY_BOTH);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("fieldMustBeCapitalized"));
        tagger.setCaseType(CASE_WORDS);
        tagger.setApplyTo(APPLY_FIELD);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        String[] fields = meta.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        for (String field : fields) {
            Assertions.assertTrue(EqualsUtil.equalsAny(
                    field, "FIELDMUSTBEUPPER", "fieldmustbelower",
                    "FieldMustBeCapitalized"));
        }
    }

    @Test
    public void testSwapCase()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("fieldMustBeSwapped", "ValUe Swap. \n  OK.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();


        tagger.setFieldMatcher(TextMatcher.basic("fieldMustBeSwapped"));
        tagger.setCaseType(CASE_SWAP);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("vALuE sWAP. \n  ok.",
                meta.getString("fieldMustBeSwapped"));
    }

    @Test
    public void testCapitalizeString()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starting with a Space.");
        meta.add("string3", "1 string starting with a Number.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_STRING);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_STRING);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_STRING);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal String. another One.",
                meta.getString("string1"));
        Assertions.assertEquals(" String starting with a Space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 string starting with a Number.",
                meta.getString("string3"));
    }

    @Test
    public void testCapitalizeStringFully()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starting with a Space.");
        meta.add("string3", "1 string starting with a Number.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_STRING_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_STRING_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_STRING_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal string. another one.",
                meta.getString("string1"));
        Assertions.assertEquals(" String starting with a space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 string starting with a number.",
                meta.getString("string3"));
    }

    @Test
    public void testCapitalizeWords()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starTing with a Space.");
        meta.add("string3", "1 string starTing with a Number.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_WORDS);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_WORDS);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_WORDS);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal String. Another One.",
                meta.getString("string1"));
        Assertions.assertEquals(" String StarTing With A Space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 String StarTing With A Number.",
                meta.getString("string3"));
    }

    @Test
    public void testCapitalizeWordsFully()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starTing with a Space.");
        meta.add("string3", "1 string starTing with a Number.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_WORDS_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_WORDS_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_WORDS_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal String. Another One.",
                meta.getString("string1"));
        Assertions.assertEquals(" String Starting With A Space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 String Starting With A Number.",
                meta.getString("string3"));
    }

    @Test
    public void testCapitalizeSentences()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starTing with a Space.");
        meta.add("string3", "1 string starTing with a Number. pLUS this");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_SENTENCES);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_SENTENCES);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_SENTENCES);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal String. Another One.",
                meta.getString("string1"));
        Assertions.assertEquals(" String starTing with a Space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 string starTing with a Number. PLUS this",
                meta.getString("string3"));
    }

    @Test
    public void testCapitalizeSentencesFully()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal String. another One.");
        meta.add("string2", " string starTing with a Space.");
        meta.add("string3", "1 string starTing with a Number. pLUS this");

        CharacterCaseTagger tagger = new CharacterCaseTagger();

        tagger.setFieldMatcher(TextMatcher.basic("string1"));
        tagger.setCaseType(CASE_SENTENCES_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string2"));
        tagger.setCaseType(CASE_SENTENCES_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        tagger.setFieldMatcher(TextMatcher.basic("string3"));
        tagger.setCaseType(CASE_SENTENCES_FULLY);
        tagger.setApplyTo(APPLY_VALUE);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal string. Another one.",
                meta.getString("string1"));
        Assertions.assertEquals(" String starting with a space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 string starting with a number. Plus this",
                meta.getString("string3"));
    }

    @Test
    public void testWriteRead() throws IOException {
        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.setCaseType(CASE_UPPER);
        tagger.setApplyTo(APPLY_BOTH);
        tagger.setFieldMatcher(TextMatcher.regex(".*"));
        XML.assertWriteRead(tagger, "handler");
    }
}
