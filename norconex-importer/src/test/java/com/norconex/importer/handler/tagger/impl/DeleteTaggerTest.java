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
import java.io.Reader;
import java.io.StringReader;

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
        meta.addString("field1", "delete me");
        meta.addString("field1", "delete me too");
        meta.setString("field2", "delete also");
        meta.setString("field3", "keep this one");
        meta.setString("field4", "one last to delete");

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
        meta.addString("content-type", "blah");
        meta.addString("x-access-level", "blah");
        meta.addString("X-CONTENT-TYPE-OPTIONS", "blah");
        meta.addString("X-FRAME-OPTIONS", "blah");
        meta.addString("X-PARSED-BY", "blah");
        meta.addString("date", "blah");
        meta.addString("X-RATE-LIMIT-LIMIT", "blah");
        meta.addString("source", "blah");

        DeleteTagger tagger = new DeleteTagger();

        Reader r = new StringReader(
                "<tagger><fields>X-ACCESS-LEVEL,X-content-type-options,"
              + "X-FRAME-OPTIONS,X-PARSED-BY,X-RATE-LIMIT-LIMIT</fields>"
              + "</tagger>");
        tagger.loadFromXML(r);

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 3, meta.size());
    }

    @Test
    public void testDeleteFieldsRegexViaXMLConfig()
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

        DeleteTagger tagger = new DeleteTagger();

        Reader r = new StringReader(
                "<tagger><fieldsRegex>^[Xx]-.*</fieldsRegex></tagger>");
        tagger.loadFromXML(r);

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);

        Assert.assertEquals("Invalid field count", 3, meta.size());
    }


}
