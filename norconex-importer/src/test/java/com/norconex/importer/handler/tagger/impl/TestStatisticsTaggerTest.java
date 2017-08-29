/* Copyright 2010-2017 Norconex Inc.
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
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
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
        meta.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument("n/a", is, meta, false);

        is.close();

        Assert.assertEquals(616, meta.getInt("document.stat.characterCount"));
        Assert.assertEquals(115, meta.getInt("document.stat.wordCount"));
        Assert.assertEquals(8, meta.getInt("document.stat.sentenceCount"));
        Assert.assertEquals(4, meta.getInt("document.stat.paragraphCount"));
        Assert.assertEquals("4.2", 
                meta.getString("document.stat.averageWordCharacterCount"));
        Assert.assertEquals("77.0", 
                meta.getString("document.stat.averageSentenceCharacterCount"));
        Assert.assertEquals("14.4", 
                meta.getString("document.stat.averageSentenceWordCount"));
        Assert.assertEquals("154.0", 
                meta.getString("document.stat.averageParagraphCharacterCount"));
        Assert.assertEquals("2.0", 
                meta.getString("document.stat.averageParagraphSentenceCount"));
        Assert.assertEquals("28.8", 
                meta.getString("document.stat.averageParagraphWordCount"));
    }
    
    @Test
    public void testWriteRead() throws IOException {
        TextStatisticsTagger tagger = new TextStatisticsTagger();
        tagger.setFieldName("afield");
        System.out.println("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }

}
