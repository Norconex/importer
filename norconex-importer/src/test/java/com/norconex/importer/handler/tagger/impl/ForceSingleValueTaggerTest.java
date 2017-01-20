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

import java.io.IOException;

import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.handler.tagger.impl.ForceSingleValueTagger;

public class ForceSingleValueTaggerTest {

    @Test
    public void testWriteRead() throws IOException {
        ForceSingleValueTagger tagger = new ForceSingleValueTagger();
        tagger.addSingleValueField("field1", "keepFirst");
        tagger.addSingleValueField("field2", "keepFirst");
        tagger.addSingleValueField("field3", "keepFirst");
        System.out.println("Writing/Reading this: " + tagger);
        XMLConfigurationUtil.assertWriteRead(tagger);
    }

}
