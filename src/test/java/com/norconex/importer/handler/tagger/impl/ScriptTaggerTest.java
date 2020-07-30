/* Copyright 2015-2020 Norconex Inc.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.DocMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.parser.ParseState;

public class ScriptTaggerTest {

    @Test
    public void testLua() throws ImporterHandlerException, IOException {
        testScriptTagger("lua",
                "metadata:add('test', {'success'});"
              + "local story = content:gsub('Alice', 'Roger');"
              + "metadata:add('story', {story});"
        );
    }

    @Test
    public void testJavaScript() throws ImporterHandlerException, IOException {
        testScriptTagger(ScriptRunner.DEFAULT_SCRIPT_ENGINE,
                "metadata.add('test', 'success');"
              + "var story = content.replace(/Alice/g, 'Roger');"
              + "metadata.add('story', story);"
        );
    }

    private void testScriptTagger(String engineName, String script)
            throws ImporterHandlerException, IOException {
        ScriptTagger t = new ScriptTagger();
        t.setEngineName(engineName);
        t.setScript(script);

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        Properties metadata = new Properties();
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");

        t.tagDocument(TestUtil.toHandlerDoc(
                htmlFile.getAbsolutePath(), is, metadata), is, ParseState.PRE);

        is.close();

        String successField = metadata.getString("test");
        String storyField = metadata.getString("story");

        Assertions.assertEquals("success", successField);
        Assertions.assertEquals(0, StringUtils.countMatches(storyField, "Alice"));
        Assertions.assertEquals(34, StringUtils.countMatches(storyField, "Roger"));
    }

    @Test
        public void testWriteRead() {
        ScriptTagger tagger = new ScriptTagger();
        tagger.setScript("a script");
        tagger.setEngineName("an engine name");
        tagger.setMaxReadSize(256);
        XML.assertWriteRead(tagger, "handler");
    }
}
