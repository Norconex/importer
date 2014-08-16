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
import com.norconex.importer.handler.tagger.impl.HierarchyTagger;

public class HierarchyTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        HierarchyTagger tagger = new HierarchyTagger();
        tagger.addHierarcyDetails(
                "fromField1", "toField1", "fromSep1", "toSep1", false);
        tagger.addHierarcyDetails(
                "fromField2", null, "fromSep2", null, false);
        tagger.addHierarcyDetails(
                null, "toField3", null, "toSep3", false);
        tagger.addHierarcyDetails(
                "fromField4", "toField4", "fromSep4", "toSep4", true);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

    public void testTagDocument() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.setString("path", "/vegetable/potato/sweet");
        HierarchyTagger tagger = new HierarchyTagger();
        tagger.addHierarcyDetails(
                "path", "tree", "/", "~~~", false);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        String[] expected = new String[] {
                "~~~vegetable",
                "~~~vegetable~~~potato",
                "~~~vegetable~~~potato~~~sweet"
        }; 
        Assert.assertArrayEquals(expected, 
                meta.getStrings("tree").toArray());
    }

}
