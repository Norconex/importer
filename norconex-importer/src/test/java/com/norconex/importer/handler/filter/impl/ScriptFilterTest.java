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
package com.norconex.importer.handler.filter.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;

public class ScriptFilterTest {

    @Test
    public void testLua() throws IOException, ImporterHandlerException {
        testScriptFilter(ScriptRunner.LUA_ENGINE,
                "local test = metadata:getString('fruit') == 'apple' "
              + " and content:find('Alice') ~= nil; "
              + "return test;"
        );
    }

    @Test
    public void testJavaScript() throws IOException, ImporterHandlerException {
        testScriptFilter(ScriptRunner.JAVASCRIPT_ENGINE,
                "var test = metadata.getString('fruit') == 'apple'"
              + "  && content.indexOf('Alice') > -1;"
              + "/*return*/ test;"
        );
    }

    private void testScriptFilter(String engineName, String script)
            throws IOException, ImporterHandlerException {

        ScriptFilter f = new ScriptFilter();
        f.setEngineName(engineName);
        f.setScript(script);

        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString("fruit", "apple");
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");

        Assert.assertTrue("Filter returned false.",  f.acceptDocument(
                htmlFile.getAbsolutePath(), is, metadata, false));

        is.close();
    }

    @Test
    public void testWriteRead() throws IOException {
        ScriptFilter f = new ScriptFilter();
        f.setScript("a script");
        f.setEngineName("an engine name");
        f.setMaxReadSize(256);
        XML.assertWriteRead(f, "handler");
    }
}
