/* Copyright 2010-2014 Norconex Inc.
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

import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.tagger.impl.KeepOnlyTagger;

public class KeepOnlyTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        KeepOnlyTagger tagger = new KeepOnlyTagger();
        tagger.addField("field1");
        tagger.addField("field2");
        tagger.addField("field3");
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

    @Test
    public void test_keep_all_fields() throws Exception {

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
    public void test_keep_single_field() throws Exception {

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
    public void test_delete_all_metadata() throws Exception {

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

}
