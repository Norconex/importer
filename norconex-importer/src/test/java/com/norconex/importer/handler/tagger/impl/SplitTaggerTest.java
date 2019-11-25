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
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class SplitTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        SplitTagger tagger = new SplitTagger();
        tagger.addSplit("fromName1", "toName1", "sep1", false);
        tagger.addSplit("fromName2", "toName2", "sep2", true);
        tagger.addSplit("fromName3", "sep3", true);
        tagger.addSplit("fromName4", "sep4", false);
        System.out.println("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }


    @Test
    public void testRegularSplit() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
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
    public void testRegexSplit() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
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

    // Test for https://github.com/Norconex/importer/issues/97
    @Test
    public void testOverwritten() throws ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("test", "Joe, Jack, William, Avrel");

        SplitTagger tagger = new SplitTagger();
        tagger.addSplit("test", "\\s*,\\s*", true);

        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals(4, meta.getStrings("test").size());
    }
}
