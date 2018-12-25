/* Copyright 2015-2017 Norconex Inc.
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfigLoader;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.ScriptRunner;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.importer.response.ImporterStatus.Status;

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

    // Relates to: https://github.com/Norconex/importer/issues/86
    @Test
    public void testPrePostScriptFilter() throws IOException {
        try (Reader r = ResourceLoader.getXmlReader(getClass())) {
            Importer importer =
                    new Importer(ImporterConfigLoader.loadImporterConfig(r));
            ImporterResponse resp = importer.importDocument(
                    new ByteArrayInputStream("test".getBytes()),
                    new ImporterMetadata(), "N/A");
            ImporterDocument doc = resp.getDocument();
            Assert.assertNotNull("Document must not be null", doc);
            Status status = resp.getImporterStatus().getStatus();
            Assert.assertEquals(Status.SUCCESS, status);
        }
    }

    @Test
    public void testWriteRead() throws IOException {
        ScriptFilter f = new ScriptFilter();
        f.setScript("a script");
        f.setEngineName("an engine name");
        f.setMaxReadSize(256);
        System.out.println("Writing/Reading this: " + f);
        XMLConfigurationUtil.assertWriteRead(f);
    }

}
