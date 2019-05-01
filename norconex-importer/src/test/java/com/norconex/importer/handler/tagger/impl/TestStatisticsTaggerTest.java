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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class TestStatisticsTaggerTest {

    @Test
    public void testTagTextDocument()
            throws IOException, ImporterHandlerException {

        String txt =
            "White Rabbit checking watch"
          + "\n\n"
          + "  In another moment down went Alice after it, never once "
          + "considering how in the world she was to get out again."
          + "\n\n"
          + "  The rabbit-hole went straight on like a tunnel for some way, "
          + "and then dipped suddenly down, so suddenly that Alice had not a "
          + "moment to think about stopping herself before she found herself "
          + "falling down a very deep well."
          + "\n\n"
          + "`Well!' thought Alice to herself, `after such a fall as this, I "
          + "shall think nothing of tumbling down stairs!  How brave they'll "
          + "all think me at home!  Why, I wouldn't say anything about it, "
          + "even if I fell off the top of the house!' (Which was very likely "
          + "true.)";

        TextStatisticsTagger t = new TextStatisticsTagger();
        InputStream is = IOUtils.toInputStream(txt, StandardCharsets.UTF_8);

        ImporterMetadata meta = new ImporterMetadata();
        meta.set(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument("n/a", is, meta, false);

        is.close();

        Assertions.assertEquals(616,
                (int) meta.getInteger("document.stat.characterCount"));
        Assertions.assertEquals(115,
                (int) meta.getInteger("document.stat.wordCount"));
        Assertions.assertEquals(8,
                (int) meta.getInteger("document.stat.sentenceCount"));
        Assertions.assertEquals(4,
                (int) meta.getInteger("document.stat.paragraphCount"));
        Assertions.assertEquals("4.2",
                meta.getString("document.stat.averageWordCharacterCount"));
        Assertions.assertEquals("77.0",
                meta.getString("document.stat.averageSentenceCharacterCount"));
        Assertions.assertEquals("14.4",
                meta.getString("document.stat.averageSentenceWordCount"));
        Assertions.assertEquals("154.0",
                meta.getString("document.stat.averageParagraphCharacterCount"));
        Assertions.assertEquals("2.0",
                meta.getString("document.stat.averageParagraphSentenceCount"));
        Assertions.assertEquals("28.8",
                meta.getString("document.stat.averageParagraphWordCount"));
    }

    @Test
    public void testWriteRead() throws IOException {
        TextStatisticsTagger tagger = new TextStatisticsTagger();
        tagger.setFieldName("afield");
        XML.assertWriteRead(tagger, "handler");
    }
}
