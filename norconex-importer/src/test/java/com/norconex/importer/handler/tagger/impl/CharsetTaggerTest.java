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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CharsetTaggerTest {

    @Test
    public void testCharsetTagger()
            throws IOException, ImporterHandlerException {

        testCharsetTagger("ISO-8859-1",   "ISO-8859-1");
        testCharsetTagger("ISO-8859-2",   "ISO-8859-1");
        testCharsetTagger("windows-1250", "ISO-8859-1");
        testCharsetTagger("UTF-8",        "ISO-8859-1");

        testCharsetTagger("ISO-8859-1",   "ISO-8859-2");
        testCharsetTagger("ISO-8859-2",   "ISO-8859-2");
        testCharsetTagger("windows-1250", "ISO-8859-2");
        testCharsetTagger("UTF-8",        "ISO-8859-2");

        testCharsetTagger("ISO-8859-1",   "windows-1250");
        testCharsetTagger("ISO-8859-2",   "windows-1250");
        testCharsetTagger("windows-1250", "windows-1250");
        testCharsetTagger("UTF-8",        "windows-1250");

        testCharsetTagger("ISO-8859-1",   "UTF-8");
        testCharsetTagger("ISO-8859-2",   "UTF-8");
        testCharsetTagger("windows-1250", "UTF-8");
        testCharsetTagger("UTF-8",        "UTF-8");

        testCharsetTagger("ISO-8859-1",   "KOI8-R");
        testCharsetTagger("ISO-8859-2",   "KOI8-R");
        testCharsetTagger("windows-1250", "KOI8-R");
        testCharsetTagger("UTF-8",        "KOI8-R");
    }


    private void testCharsetTagger(String fromCharset, String toCharset)
            throws IOException, ImporterHandlerException {

        byte[] sourceBytes = "En télécommunications".getBytes(fromCharset);
        byte[] targetBytes = "En télécommunications".getBytes(toCharset);

        CharsetTagger t = new CharsetTagger();
        //t.setSourceCharset(fromCharset);
        t.setTargetCharset(toCharset);
        t.setFieldsRegex("field1");

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString("field1", new String(sourceBytes, fromCharset));
        metadata.setString(ImporterMetadata.DOC_CONTENT_ENCODING, fromCharset);

        t.tagDocument(
                "ref-" + fromCharset + "-" + toCharset,
                new NullInputStream(0),
                metadata, false);

        String convertedValue = metadata.getString("field1");
        byte[] convertedBytes = convertedValue.getBytes(toCharset);

//        String sourceValue = new String(sourceBytes, fromCharset);
//        String targetValue = new String(targetBytes, toCharset);
//        System.out.println("=== " + fromCharset + " > " + toCharset + "===");
//        System.out.println(" original value: " + sourceValue);
//        System.out.println("   target value: " + convertedValue);
//        System.out.println("converted value: " + convertedValue);
//        System.out.println(" original bytes: " + Arrays.toString(sourceBytes));
//        System.out.println("   target bytes: " + Arrays.toString(targetBytes));
//        System.out.println("converted bytes: " + Arrays.toString(convertedBytes));

        Assert.assertArrayEquals(fromCharset + " > " + toCharset,
                targetBytes, convertedBytes);
    }

    @Test
    public void testWriteRead() throws IOException {
        CharsetTagger t = new CharsetTagger();
        t.setTargetCharset(StandardCharsets.ISO_8859_1.toString());
        t.setFieldsRegex(".*");
        XML.assertWriteRead(t, "handler");
    }
}
