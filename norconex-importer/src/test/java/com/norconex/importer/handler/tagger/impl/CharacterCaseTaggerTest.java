/* Copyright 2014-2015 Norconex Inc.
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
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CharacterCaseTaggerTest {

    @Test
    public void testUpperLowerValues() 
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "Doit être upper");
        meta.addString("field1", "Must be upper");
        meta.setString("field2", "DOIT ÊTRE LOWER");

        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("field1", "upper", "value");
        tagger.addFieldCase("field2", "lower", "both");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        Assert.assertEquals("DOIT ÊTRE UPPER", 
                meta.getStrings("field1").get(0));
        Assert.assertEquals("MUST BE UPPER", meta.getStrings("field1").get(1));
        Assert.assertEquals("doit être lower", meta.getString("field2"));
    }    

    @Test
    public void testUpperLowerField() 
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("fieldMustBeUpper", "value 1");
        meta.addString("fieldMustBeLower", "value 2");
        meta.setString("fieldMustBeCapitalized", "value 3");

        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("fieldMustBeUpper", "upper", "field");
        tagger.addFieldCase("fieldMustBeLower", "lower", "both");
        tagger.addFieldCase("fieldMustBeCapitalized", "words", "field");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        String[] fields = meta.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        for (String field : fields) {
            Assert.assertTrue(EqualsUtil.equalsAny(
                    field, "FIELDMUSTBEUPPER", "fieldmustbelower", 
                    "Fieldmustbecapitalized"));
        }
    }    
    
    @Test
    public void testWriteRead() throws IOException {
        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("fld1", "upper");
        tagger.addFieldCase("fld2", "lower");
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }
}
