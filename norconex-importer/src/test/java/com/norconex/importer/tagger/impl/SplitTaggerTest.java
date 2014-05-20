/* Copyright 2010-2013 Norconex Inc.
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
package com.norconex.importer.tagger.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.map.Properties;

public class SplitTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        SplitTagger tagger = new SplitTagger();
        tagger.addSplit("fromName1", "toName1", "sep1", false);
        tagger.addSplit("fromName2", "toName2", "sep2", true);
        tagger.addSplit("fromName3", "sep3", true);
        tagger.addSplit("fromName4", "sep4", false);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

    
    @Test
    public void testRegularSplit() throws IOException {
        Properties meta = new Properties();
        meta.addString("metaToSplitSameField", "Joe, Jack, William, Avrel"); 
        meta.addString("metaNoSplitSameField", "Joe Jack William Avrel"); 
        meta.addString("metaToSplitNewField", "Joe, Jack, William, Avrel"); 
        meta.addString("metaNoSplitNewField", "Joe Jack William Avrel"); 
        meta.addString("metaMultiSameField", 
                "Joe, Jack", "William, Avrel"); 
        meta.addString("metaMultiNewField", 
                "Joe, Jack", "William, Avrel"); 
        
        
        SplitTagger tagger = new SplitTagger();
        tagger.addSplit("metaToSplitSameField", ", ", false);
        tagger.addSplit("metaNoSplitSameField", ", ", false);
        tagger.addSplit("metaToSplitNewField", "toSplitNewField", ", ", false);
        tagger.addSplit("metaNoSplitNewField", "noSplitNewField", ", ", false);
        tagger.addSplit("metaMultiSameField", ", ", false);
        tagger.addSplit("metaMultiNewField", "multiNewField", ", ", false);

        tagger.tagDocument("n/a", null, meta, true);

        List<String> toSplitExpect = Arrays.asList(
                "Joe", "Jack", "William", "Avrel"); 
        List<String> noSplitExpect = Arrays.asList(
                "Joe Jack William Avrel"); 
        
        Assert.assertEquals(
                toSplitExpect, meta.getStrings("metaToSplitSameField"));
        Assert.assertEquals(
                noSplitExpect, meta.getStrings("metaNoSplitSameField"));
        Assert.assertEquals(
                toSplitExpect, meta.getStrings("toSplitNewField"));
        Assert.assertEquals(
                noSplitExpect, meta.getStrings("metaNoSplitNewField"));
        Assert.assertEquals(
                toSplitExpect, meta.getStrings("metaMultiSameField"));
        Assert.assertEquals(
                toSplitExpect, meta.getStrings("multiNewField"));

        Assert.assertEquals(
                noSplitExpect, meta.getStrings("metaNoSplitSameField"));
    }
    
    @Test
    public void testRegexSplit() throws IOException {
        Properties meta = new Properties();
        meta.addString("path1", "/a/path/file.doc"); 
        meta.addString("path2", "a, b,c d;e, f"); 
        
        SplitTagger tagger = new SplitTagger();
        tagger.addSplit("path1", "/", true);
        tagger.addSplit("path2", "file2", "[, ;]+", true);
        
        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals(
                Arrays.asList("a", "path", "file.doc"), 
                meta.getStrings("path1"));
        Assert.assertEquals(
                Arrays.asList("a", "b", "c", "d", "e", "f"), 
                meta.getStrings("file2"));
    }

}
