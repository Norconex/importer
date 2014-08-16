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

import org.apache.commons.io.input.NullInputStream;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CharacterCaseTaggerTest {

    @Test
    public void testUpperLower() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "Doit être upper");
        meta.addString("field1", "Must be upper");
        meta.setString("field2", "DOIT ÊTRE LOWER");

        CharacterCaseTagger tagger = new CharacterCaseTagger();
        tagger.addFieldCase("field1", "upper");
        tagger.addFieldCase("field2", "lower");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        Assert.assertEquals("DOIT ÊTRE UPPER", 
                meta.getStrings("field1").get(0));
        Assert.assertEquals("MUST BE UPPER", meta.getStrings("field1").get(1));
        Assert.assertEquals("doit être lower", meta.getString("field2"));
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
