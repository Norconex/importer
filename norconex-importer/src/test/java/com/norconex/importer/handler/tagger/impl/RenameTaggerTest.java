/* Copyright 2019 Norconex Inc.
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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class RenameTaggerTest {

    private static final Logger LOG =
            LogManager.getLogger(RenameTaggerTest.class);

    @Test
    public void testWriteRead() throws IOException {
        RenameTagger tagger = new RenameTagger();
        tagger.addRename("from1", "to1", true, true);
        tagger.addRename("from2", "to2", false, false);
        LOG.debug("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }

    @Test
    public void testRename() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("regularFrom1", "value1");
        meta.addString("regexFrom2", "value2");
        meta.addString("regexFrom3", "value3");
        meta.addString("nochange4", "value4");


        RenameTagger tagger = new RenameTagger();
        tagger.addRename("regularFrom1", "regularTo1", false, false);
        tagger.addRename(".*(From)(\\d+).*", "$1Regex$2", false, true);

        tagger.tagDocument("n/a", null, meta, true);

        Assert.assertEquals(4, meta.size());
        Assert.assertEquals("value1", meta.getString("regularTo1"));
        Assert.assertEquals("value2", meta.getString("FromRegex2"));
        Assert.assertEquals("value3", meta.getString("FromRegex3"));
        Assert.assertEquals("value4", meta.getString("nochange4"));
    }
}
