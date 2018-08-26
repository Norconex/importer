/* Copyright 2010-2018 Norconex Inc.
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
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.ConstantTagger.OnConflict;

public class ConstantTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        ConstantTagger tagger = new ConstantTagger();
        tagger.addConstant("constant1", "value1");
        tagger.addConstant("constant1", "value2");
        tagger.addConstant("constant2", "valueA");
        tagger.addConstant("constant2", "valueA");
        tagger.addConstant("constant3", "valueZ");
        tagger.setOnConflict(OnConflict.REPLACE);
        XML.assertWriteRead(tagger, "handler");
    }

    @Test
    public void testOnConflict() throws IOException, ImporterHandlerException {
        ImporterMetadata m = new ImporterMetadata();
        m.add("test1", "1");
        m.add("test1", "2");
        m.add("test2", "1");
        m.add("test2", "2");
        m.add("test3", "1");
        m.add("test3", "2");

        ConstantTagger t = new ConstantTagger();

        // ADD
        t.setOnConflict(OnConflict.ADD);
        t.addConstant("test1", "3");
        t.addConstant("test1", "4");
        t.tagDocument("n/a", new NullInputStream(0), m, false);
        Assert.assertArrayEquals(new String[]{
                "1", "2", "3", "4"}, m.getStrings("test1").toArray());
        // REPLACE
        t.setOnConflict(OnConflict.REPLACE);
        t.addConstant("test2", "3");
        t.addConstant("test2", "4");
        t.tagDocument("n/a", new NullInputStream(0), m, false);
        Assert.assertArrayEquals(new String[]{
                "3", "4"}, m.getStrings("test2").toArray());
        // NOOP
        t.setOnConflict(OnConflict.NOOP);
        t.addConstant("test3", "3");
        t.addConstant("test3", "4");
        t.tagDocument("n/a", new NullInputStream(0), m, false);
        Assert.assertArrayEquals(new String[]{
                "1", "2"}, m.getStrings("test3").toArray());
    }
}
