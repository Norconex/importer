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

import java.io.IOException;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.EqualsUtil;
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
        tagger.addFieldCase("field1", "upper", "value");
        tagger.addFieldCase("field2", "lower", "both");

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
        tagger.addFieldCase("fieldMustBeUpper", "upper", "field");
        tagger.addFieldCase("fieldMustBeLower", "lower", "both");
        tagger.addFieldCase("fieldMustBeCapitalized", "words", "field");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        String[] fields = meta.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        for (String field : fields) {
            Assertions.assertTrue(EqualsUtil.equalsAny(
                    field, "FIELDMUSTBEUPPER", "fieldmustbelower",
                    "Fieldmustbecapitalized"));
        }
    }

    @Test
    public void testSwapCase()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("fieldMustBeSwapped", "ValUe Swap. \n  OK.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("fieldMustBeSwapped", "swap", "value");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("vALuE sWAP. \n  ok.",
                meta.getString("fieldMustBeSwapped"));
    }

    @Test
    public void testCapitalizeString()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("string1", "normal string.");
        meta.add("string2", " string starting with a space.");
        meta.add("string3", "1 string starting with a number.");

        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("string1", "string", "value");
        tagger.addFieldCase("string2", "string", "value");
        tagger.addFieldCase("string3", "string", "value");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals("Normal string.", meta.getString("string1"));
        Assertions.assertEquals(" String starting with a space.",
                meta.getString("string2"));
        Assertions.assertEquals("1 string starting with a number.",
                meta.getString("string3"));
    }


    @Test
    public void testWriteRead() throws IOException {
        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("fld1", "upper");
        tagger.addFieldCase("fld2", "lower");
        XML.assertWriteRead(tagger, "handler");
    }
}
