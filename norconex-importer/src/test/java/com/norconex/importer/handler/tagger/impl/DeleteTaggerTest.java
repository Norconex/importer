/* Copyright 2010-2020 Norconex Inc.
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

import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class DeleteTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        DeleteTagger tagger = new DeleteTagger();
        tagger.getFieldMatcher().setPattern("(potato|carrot|document\\.*)")
                .setMethod(Method.REGEX);
        XML.assertWriteRead(tagger, "handler");
    }

    @Test
    public void testDeleteField() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("field1", "delete me");
        meta.add("field1", "delete me too");
        meta.set("field2", "delete also");
        meta.set("field3", "keep this one");
        meta.set("field4", "one last to delete");

        DeleteTagger tagger = new DeleteTagger();
        tagger.getFieldMatcher().setPattern("(field1|field2|field4\\.*)")
                .setMethod(Method.REGEX);

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals(1, meta.size(), "Invalid field count");
        Assertions.assertEquals(
                "keep this one", meta.getString("field3"),
                "Value wrongfully deleted or modified");
    }

    @Test
    public void testDeleteFieldsViaXMLConfig()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("content-type", "blah");
        meta.add("x-access-level", "blah");
        meta.add("X-CONTENT-TYPE-OPTIONS", "blah");
        meta.add("X-FRAME-OPTIONS", "blah");
        meta.add("X-PARSED-BY", "blah");
        meta.add("date", "blah");
        meta.add("X-RATE-LIMIT-LIMIT", "blah");
        meta.add("source", "blah");

        deleteBasic(meta, "X-ACCESS-LEVEL");
        deleteBasic(meta, "X-content-type-options");
        deleteBasic(meta, "X-FRAME-OPTIONS");
        deleteBasic(meta, "X-PARSED-BY");
        deleteBasic(meta, "X-RATE-LIMIT-LIMIT");

        Assertions.assertEquals(3, meta.size(), "Invalid field count");
    }

    private void deleteBasic(ImporterMetadata meta, String field)
            throws ImporterHandlerException {
        DeleteTagger tagger = new DeleteTagger();
        tagger.loadFromXML(new XML(
                "<tagger>"
              + "<fieldMatcher ignoreCase=\"true\">" + field + "</fieldMatcher>"
              + "</tagger>"));
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
    }



    @Test
    public void testDeleteFieldsRegexViaXMLConfig()
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.add("content-type", "blah");
        meta.add("x-access-level", "blah");
        meta.add("X-CONTENT-TYPE-OPTIONS", "blah");
        meta.add("X-FRAME-OPTIONS", "blah");
        meta.add("X-PARSED-BY", "blah");
        meta.add("date", "blah");
        meta.add("X-RATE-LIMIT-LIMIT", "blah");
        meta.add("source", "blah");

        DeleteTagger tagger = new DeleteTagger();

        tagger.loadFromXML(new XML(
                "<tagger><fieldMatcher method=\"regex\">"
              + "^[Xx]-.*</fieldMatcher></tagger>"));

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assertions.assertEquals(3, meta.size(), "Invalid field count");
    }
}
