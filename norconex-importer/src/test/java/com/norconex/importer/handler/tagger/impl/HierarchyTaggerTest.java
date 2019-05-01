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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

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
        XML.assertWriteRead(tagger, "handler");
    }

    public void testTagDocument() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.set("path", "/vegetable/potato/sweet");
        HierarchyTagger tagger = new HierarchyTagger();
        tagger.addHierarcyDetails(
                "path", "tree", "/", "~~~", false);
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        String[] expected = new String[] {
                "~~~vegetable",
                "~~~vegetable~~~potato",
                "~~~vegetable~~~potato~~~sweet"
        };
        Assertions.assertArrayEquals(expected,
                meta.getStrings("tree").toArray());
    }
}
