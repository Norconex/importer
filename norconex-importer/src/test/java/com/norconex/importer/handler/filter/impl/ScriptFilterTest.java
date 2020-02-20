/* Copyright 2015-2019 Norconex Inc.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.Doc;
import com.norconex.importer.doc.DocMetadata;
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

        Properties metadata = new Properties();
        metadata.set("fruit", "apple");
        metadata.set(DocMetadata.CONTENT_TYPE, "text/html");

        Assertions.assertTrue(f.acceptDocument(
                htmlFile.getAbsolutePath(), is, metadata, false),
                "Filter returned false.");

        is.close();
    }

    // Relates to: https://github.com/Norconex/importer/issues/86
    @Test
    public void testPrePostScriptFilter() throws IOException {
        try (Reader r = ResourceLoader.getXmlReader(getClass())) {
            ImporterConfig cfg = new ImporterConfig();
            cfg.loadFromXML(new XML(r));
            Importer importer = new Importer(cfg);
            ImporterResponse resp = importer.importDocument(
                    new ByteArrayInputStream("test".getBytes()),
                    new Properties(), "N/A");
            Doc doc = resp.getDocument();
            Assertions.assertNotNull(doc, "Document must not be null");
            Status status = resp.getImporterStatus().getStatus();
            Assertions.assertEquals(Status.SUCCESS, status);
        }
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
