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

public class DeleteTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        DeleteTagger tagger = new DeleteTagger();
        tagger.addField("potato");
        tagger.addField("potato");
        tagger.addField("carrot");
        tagger.setFieldsRegex("document\\.*");
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
        tagger.addField("field1");
        tagger.addField("field2");
        tagger.addField("field4");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 1, meta.size());
        Assert.assertEquals("Value wrongfully deleted or modified",
                "keep this one", meta.getString("field3"));
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

        DeleteTagger tagger = new DeleteTagger();

//        Reader r = new StringReader(
//                "<tagger><fields>X-ACCESS-LEVEL,X-content-type-options,"
//              + "X-FRAME-OPTIONS,X-PARSED-BY,X-RATE-LIMIT-LIMIT</fields>"
//              + "</tagger>");
        tagger.loadFromXML(new XML(
                  "<tagger><fields>X-ACCESS-LEVEL,X-content-type-options,"
                + "X-FRAME-OPTIONS,X-PARSED-BY,X-RATE-LIMIT-LIMIT</fields>"
                + "</tagger>"));

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 3, meta.size());
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

//        Reader r = new StringReader(
//                "<tagger><fieldsRegex>^[Xx]-.*</fieldsRegex></tagger>");
        tagger.loadFromXML(new XML(
                "<tagger><fieldsRegex>^[Xx]-.*</fieldsRegex></tagger>"));

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 3, meta.size());
    }


}
