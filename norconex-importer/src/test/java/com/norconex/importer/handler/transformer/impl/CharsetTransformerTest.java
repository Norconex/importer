/* Copyright 2015 Norconex Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class CharsetTransformerTest {

    @Test
    public void testCharsetTransformer() 
            throws IOException, ImporterHandlerException {
        
        testCharsetTransformer("ISO-8859-1",   "ISO-8859-1");
        testCharsetTransformer("ISO-8859-2",   "ISO-8859-1");
        testCharsetTransformer("windows-1250", "ISO-8859-1");
        testCharsetTransformer("UTF-8",        "ISO-8859-1");
    
        testCharsetTransformer("ISO-8859-1",   "ISO-8859-2");
        testCharsetTransformer("ISO-8859-2",   "ISO-8859-2");
        testCharsetTransformer("windows-1250", "ISO-8859-2");
        testCharsetTransformer("UTF-8",        "ISO-8859-2");
    
        testCharsetTransformer("ISO-8859-1",   "windows-1250");
        testCharsetTransformer("ISO-8859-2",   "windows-1250");
        testCharsetTransformer("windows-1250", "windows-1250");
        testCharsetTransformer("UTF-8",        "windows-1250");

        testCharsetTransformer("ISO-8859-1",   "UTF-8");
        testCharsetTransformer("ISO-8859-2",   "UTF-8");
        testCharsetTransformer("windows-1250", "UTF-8");
        testCharsetTransformer("UTF-8",        "UTF-8");

        testCharsetTransformer("ISO-8859-1",   "KOI8-R");
        testCharsetTransformer("ISO-8859-2",   "KOI8-R");
        testCharsetTransformer("windows-1250", "KOI8-R");
        testCharsetTransformer("UTF-8",        "KOI8-R");
    }
    
    private void testCharsetTransformer(String fromCharset, String toCharset) 
            throws IOException, ImporterHandlerException {

        byte[] startWith = "En télécommunications".getBytes(toCharset);
        
        CharsetTransformer t = new CharsetTransformer();
        t.setTargetCharset(toCharset);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();
        InputStream is = getFileStream("/charset/" + fromCharset + ".txt");
        
        t.transformDocument(fromCharset + ".txt", is, os, metadata, false);

        byte[] output = os.toByteArray();
        
        is.close();
        os.close();

        byte[] targetStartWith = Arrays.copyOf(output, startWith.length);

//        System.out.println("=== " + fromCharset + " > " + toCharset + "===");
//        System.out.println(Arrays.toString(startWith));
//        System.out.println(Arrays.toString(targetStartWith));

        Assert.assertArrayEquals(fromCharset + " > " + toCharset, 
                startWith, targetStartWith);
    }

    private InputStream getFileStream(String resourcePath) {
        return getClass().getResourceAsStream(resourcePath);
    }

    
    @Test
    public void testWriteRead() throws IOException {
        CharsetTransformer t = new CharsetTransformer();
        t.setTargetCharset(CharEncoding.ISO_8859_1);
        System.out.println("Writing/Reading this: " + t);
        ConfigurationUtil.assertWriteRead(t);
    }

}
