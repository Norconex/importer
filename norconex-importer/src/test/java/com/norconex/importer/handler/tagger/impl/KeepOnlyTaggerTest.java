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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class KeepOnlyTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        KeepOnlyTagger tagger = new KeepOnlyTagger();
        tagger.addField("field1");
        tagger.addField("field2");
        tagger.addField("field3");
        XML.assertWriteRead(tagger, "handler");
    }

    @Test
    public void testKeepAllFields() throws Exception {

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.addString("key1", "value1");
        metadata.addString("key2", "value2");
        metadata.addString("key3", "value3");

        // Should only keep all keys
        KeepOnlyTagger tagger = new KeepOnlyTagger();
        tagger.addField("key1");
        tagger.addField("key2");
        tagger.addField("key3");
        tagger.tagDocument("reference", null, metadata, true);

        assertEquals(3, metadata.size());
    }

    @Test
    public void testKeepSingleField() throws Exception {

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.addString("key1", "value1");
        metadata.addString("key2", "value2");
        metadata.addString("key3", "value3");

        // Should only keep key1
        KeepOnlyTagger tagger = new KeepOnlyTagger();
        tagger.addField("key1");
        tagger.tagDocument("reference", null, metadata, true);

        assertEquals(1, metadata.size());
        assertTrue(metadata.containsKey("key1"));
    }

    @Test
    public void testDeleteAllMetadata() throws Exception {

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.addString("key1", "value1");
        metadata.addString("key2", "value2");
        metadata.addString("key3", "value3");

        // Because we are not adding any field to keep, all metadata should be
        // deleted
        KeepOnlyTagger tagger = new KeepOnlyTagger();
        tagger.tagDocument("reference", null, metadata, true);

        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testKeepFieldsRegexViaXMLConfig()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("content-type", "blah");
        meta.addString("x-access-level", "blah");
        meta.addString("X-CONTENT-TYPE-OPTIONS", "blah");
        meta.addString("X-FRAME-OPTIONS", "blah");
        meta.addString("X-PARSED-BY", "blah");
        meta.addString("date", "blah");
        meta.addString("X-RATE-LIMIT-LIMIT", "blah");
        meta.addString("source", "blah");

        KeepOnlyTagger tagger = new KeepOnlyTagger();

//        Reader r = new StringReader(
//                "<tagger><fieldsRegex>[Xx]-.*</fieldsRegex></tagger>");
        tagger.loadFromXML(new XML(
                "<tagger><fieldsRegex>[Xx]-.*</fieldsRegex></tagger>"));

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 5, meta.size());
    }
}
