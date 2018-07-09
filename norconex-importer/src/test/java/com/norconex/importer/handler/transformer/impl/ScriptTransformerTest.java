/* Copyright 2015-2018 Norconex Inc.
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
package com.norconex.importer.handler.transformer.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;

public class ScriptTransformerTest {

    @Test
    public void testLua() throws IOException, ImporterHandlerException {
        testScriptTagger("lua",
                "metadata:addString('test', {'success'});"
              + "local text = content:gsub('Alice', 'Roger');"
              + "return text;"
        );
    }

    @Test
    public void testJavaScript()
            throws IOException, ImporterHandlerException {
        testScriptTagger(ScriptRunner.DEFAULT_SCRIPT_ENGINE,
                "metadata.addString('test', 'success');"
              + "text = content.replace(/Alice/g, 'Roger');"
              + "/*return*/ text;"
        );
    }

    private void testScriptTagger(String engineName, String script)
            throws IOException, ImporterHandlerException {
        ScriptTransformer t = new ScriptTransformer();
        t.setEngineName(engineName);
        t.setScript(script);

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.transformDocument(
                htmlFile.getAbsolutePath(), is, out, metadata, false);

        is.close();

        String successField = metadata.getString("test");

        Assert.assertEquals("success", successField);
        String content = new String(out.toString());

        Assert.assertEquals(0, StringUtils.countMatches(content, "Alice"));
        Assert.assertEquals(34, StringUtils.countMatches(content, "Roger"));
    }

    @Test
    public void testWriteRead() throws IOException {
        ScriptTransformer t = new ScriptTransformer();
        t.setScript("a script");
        t.setEngineName("an engine name");
        t.setMaxReadSize(256);
        XML.assertWriteRead(t, "handler");
    }
}
