/* Copyright 2010-2018 Norconex Inc.
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

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class StripAfterTransformerTest {

    @Test
    public void testTransformTextDocument()
            throws IOException, ImporterHandlerException {
        StripAfterTransformer t = new StripAfterTransformer();
        t.setStripAfterRegex("<p>");
        t.setCaseSensitive(false);
        t.setInclusive(true);
        File htmlFile = TestUtil.getAliceHtmlFile();
        InputStream is = new BufferedInputStream(new FileInputStream(htmlFile));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();

        metadata.set(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.transformDocument(
                htmlFile.getAbsolutePath(),
                is, os, metadata, false);
//        System.out.println(os.toString());

        Assert.assertEquals(
                "Length of doc content after transformation is incorrect.",
                552, os.toString().length());

        is.close();
        os.close();
    }


    @Test
    public void testWriteRead() throws IOException {
        StripAfterTransformer t = new StripAfterTransformer();
        t.setInclusive(true);
        t.setStripAfterRegex("<p>");
        XML.assertWriteRead(t, "handler");
    }
}
