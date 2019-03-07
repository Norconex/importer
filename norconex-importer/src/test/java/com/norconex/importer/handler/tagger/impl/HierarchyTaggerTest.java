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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.HierarchyTagger.HierarchyDetails;

public class HierarchyTaggerTest {

    private static final Logger LOG =
            LogManager.getLogger(HierarchyTaggerTest.class);
    @Test
    public void testWriteRead() throws IOException {
        HierarchyTagger tagger = new HierarchyTagger();
        tagger.addHierarcyDetails(new HierarchyDetails(
                "fromField1", "toField1", "fromSep1", "toSep1"));
        tagger.addHierarcyDetails(new HierarchyDetails(
                "fromField2", null, "fromSep2", null));
        tagger.addHierarcyDetails(new HierarchyDetails(
                null, "toField3", null, "toSep3"));
        HierarchyDetails d = new HierarchyDetails(
                "fromField4", "toField4", "fromSep4", "toSep4");
        d.setOverwrite(true);
        tagger.addHierarcyDetails(d);

        LOG.debug("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }

    @Test
    public void testDiffToAndFromSeparators() throws IOException, ImporterHandlerException {
        tagAndAssert("/",  "~~~",
                "~~~vegetable",
                "~~~vegetable~~~potato",
                "~~~vegetable~~~potato~~~sweet");
    }

    @Test
    public void testSameOrNoToSeparators() throws IOException, ImporterHandlerException {
        tagAndAssert("/",  "/",
                "/vegetable", "/vegetable/potato", "/vegetable/potato/sweet");
        tagAndAssert("/",  null,
                "/vegetable", "/vegetable/potato", "/vegetable/potato/sweet");
    }

    @Test
    public void testMultiCharSeparator() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "//vegetable//potato//sweet");
        HierarchyTagger tagger = createDefaultTagger("//", "!");
        Assert.assertArrayEquals(new String[] {
                "!vegetable",
                "!vegetable!potato",
                "!vegetable!potato!sweet"
        }, tag(tagger, meta));
    }

    @Test
    public void testEmptySegments() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "//vegetable/potato//sweet/fries//");
        HierarchyTagger tagger = createDefaultTagger("/", "!");

        // do not keep empty segments
        Assert.assertArrayEquals(new String[] {
                "!vegetable",
                "!vegetable!potato",
                "!vegetable!potato!sweet",
                "!vegetable!potato!sweet!fries"
        }, tag(tagger, meta));

        // keep empty segments
        meta.remove("targetField");
        tagger.getHierarchyDetails().get(0).setKeepEmptySegments(true);
        Assert.assertArrayEquals(new String[] {
                "!",
                "!!vegetable",
                "!!vegetable!potato",
                "!!vegetable!potato!",
                "!!vegetable!potato!!sweet",
                "!!vegetable!potato!!sweet!fries",
                "!!vegetable!potato!!sweet!fries!"
        }, tag(tagger, meta));
    }

    @Test
    public void testMultipleWithMiscSeparatorPlacement() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "/vegetable/potato/sweet",
                "vegetable/potato/sweet",
                "vegetable/potato/sweet/",
                "/vegetable/potato/sweet/");
        HierarchyTagger tagger = createDefaultTagger("/", "|");
        Assert.assertArrayEquals(new String[] {
                "|vegetable",
                "|vegetable|potato",
                "|vegetable|potato|sweet",

                "vegetable",
                "vegetable|potato",
                "vegetable|potato|sweet",

                "vegetable",
                "vegetable|potato",
                "vegetable|potato|sweet",

                "|vegetable",
                "|vegetable|potato",
                "|vegetable|potato|sweet"
        }, tag(tagger, meta));
    }


    @Test
    public void testRegexSeparatorWithToSep() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "/1/vegetable/2/potato/3//4/sweet");
        HierarchyTagger tagger = createDefaultTagger("/\\d/", "!");
        tagger.getHierarchyDetails().get(0).setRegex(true);
        Assert.assertArrayEquals(new String[] {
                "!vegetable",
                "!vegetable!potato",
                "!vegetable!potato!sweet"
        }, tag(tagger, meta));
    }

    @Test
    public void testRegexSeparatorWithToSepKeepEmpty() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "/1/vegetable/2/potato/3//4/sweet");
        HierarchyTagger tagger = createDefaultTagger("/\\d/", "!");
        tagger.getHierarchyDetails().get(0).setKeepEmptySegments(true);
        tagger.getHierarchyDetails().get(0).setRegex(true);
        Assert.assertArrayEquals(new String[] {
                "!vegetable",
                "!vegetable!potato",
                "!vegetable!potato!",
                "!vegetable!potato!!sweet"
        }, tag(tagger, meta));
    }

    @Test
    public void testRegexSeparatorWithoutToSep() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "/1/vegetable/2/potato/3//4/sweet");
        HierarchyTagger tagger = createDefaultTagger("/\\d/", null);
        tagger.getHierarchyDetails().get(0).setRegex(true);
        Assert.assertArrayEquals(new String[] {
                "/1/vegetable",
                "/1/vegetable/2/potato",
                "/1/vegetable/2/potato/4/sweet"
        }, tag(tagger, meta));
    }

    @Test
    public void testRegexSeparatorWithoutToSepKeepEmpty() {
        ImporterMetadata meta = createDefaultTestMetadata(
                "/1/vegetable/2/potato/3//4/sweet");
        HierarchyTagger tagger = createDefaultTagger("/\\d/", null);
        tagger.getHierarchyDetails().get(0).setKeepEmptySegments(true);
        tagger.getHierarchyDetails().get(0).setRegex(true);
        Assert.assertArrayEquals(new String[] {
                "/1/vegetable",
                "/1/vegetable/2/potato",
                "/1/vegetable/2/potato/3/",
                "/1/vegetable/2/potato/3//4/sweet"
        }, tag(tagger, meta));
    }


    private void tagAndAssert(
            String fromSep, String toSep, String... expected) {
        ImporterMetadata meta = createDefaultTestMetadata();
        HierarchyTagger tagger = createDefaultTagger(fromSep, toSep);
        Assert.assertArrayEquals(expected, tag(tagger, meta));
    }
    private String[] tag(HierarchyTagger tagger, ImporterMetadata meta) {
        try {
            tagger.tagDocument("blah", new NullInputStream(0), meta, false);
            LOG.debug(meta.getStrings("targetField"));
            return meta.getStrings("targetField").toArray(new String[] {});
        } catch (ImporterHandlerException e) {
            throw new RuntimeException(e);
        }
    }
    private HierarchyTagger createDefaultTagger(String fromSep, String toSep) {
        HierarchyTagger tagger = new HierarchyTagger();
        tagger.addHierarcyDetails(new HierarchyDetails(
                "sourceField", "targetField", fromSep, toSep));
        return tagger;
    }
    private ImporterMetadata createDefaultTestMetadata(String... testValues) {
        ImporterMetadata meta = new ImporterMetadata();
        if (ArrayUtils.isEmpty(testValues)) {
            meta.setString("sourceField", "/vegetable/potato/sweet");
        } else {
            for (String value : testValues) {
                meta.addString("sourceField", value);
            }
        }
        return meta;
    }

    //TODO test regex separator

}
