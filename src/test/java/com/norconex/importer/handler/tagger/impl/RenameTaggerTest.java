/* Copyright 2019-2020 Norconex Inc.
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

import static com.norconex.commons.lang.map.PropertySetter.APPEND;
import static com.norconex.commons.lang.map.PropertySetter.REPLACE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.parser.ParseState;

public class RenameTaggerTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(RenameTaggerTest.class);

    @Test
        public void testWriteRead() {
        RenameTagger tagger = new RenameTagger();
        tagger.addRename(TextMatcher.basic("from1"), "to1", REPLACE);
        tagger.addRename(TextMatcher.basic("from2"), "to2", APPEND);
        LOG.debug("Writing/Reading this: " + tagger);
        XML.assertWriteRead(tagger, "handler");
    }

    @Test
    public void testRename() throws ImporterHandlerException {
        Properties meta = new Properties();
        meta.add("regularFrom1", "value1");
        meta.add("regexFrom2", "value2");
        meta.add("regexFrom3", "value3");
        meta.add("nochange4", "value4");


        RenameTagger tagger = new RenameTagger();
        tagger.addRename(
                TextMatcher.basic("regularFrom1"), "regularTo1", APPEND);
        tagger.addRename(
                TextMatcher.regex(".*(From)(\\d+).*"), "$1Regex$2", APPEND);

        TestUtil.tag(tagger, "n/a", meta, ParseState.POST);

        Assertions.assertEquals(4, meta.size());
        Assertions.assertEquals("value1", meta.getString("regularTo1"));
        Assertions.assertEquals("value2", meta.getString("FromRegex2"));
        Assertions.assertEquals("value3", meta.getString("FromRegex3"));
        Assertions.assertEquals("value4", meta.getString("nochange4"));
    }
}
