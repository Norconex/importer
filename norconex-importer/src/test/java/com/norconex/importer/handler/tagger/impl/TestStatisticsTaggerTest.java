/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.ImporterMetadata;
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
        InputStream is = IOUtils.toInputStream(txt);

        ImporterMetadata meta = new ImporterMetadata();
        meta.setContentType("text/html");
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
        tagger.setContentTypeRegex("fakeRegex");
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
