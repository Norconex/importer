/* Copyright 2010-2016 Norconex Inc.
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
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer;

public class ReduceConsecutivesTransformerTest {

    private final String xml = "<transformer><reduce>\\stext</reduce>"
            + "<reduce>\\t</reduce><reduce>\\n\\r</reduce>"
            + "<reduce>\\s</reduce><reduce>.</reduce></transformer>";
    
    @Test
    public void testTransformTextDocument() 
            throws ImporterHandlerException, IOException {
        String text = "\t\tThis is the text TeXt I want to modify...\n\r\n\r"
                + "     Too much space.";
        
        ReduceConsecutivesTransformer t = new ReduceConsecutivesTransformer();

        Reader reader = new InputStreamReader(
                IOUtils.toInputStream(xml, CharEncoding.UTF_8));
        try {
            t.loadFromXML(reader);
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not reduce consecutives.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
        
        InputStream is = IOUtils.toInputStream(text, CharEncoding.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try { 
            t.transformDocument(
                    "dummyRef", is, os, new ImporterMetadata(), true);
            String response = os.toString();
            System.out.println(response);
            Assert.assertEquals(
                    "\tthis is the text i want to modify.\n\r too much space.", 
                    response.toLowerCase());
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }
    
    
    @Test
    public void testWriteRead() throws IOException {
        ReduceConsecutivesTransformer t = new ReduceConsecutivesTransformer();
        Reader reader = new InputStreamReader(
                IOUtils.toInputStream(xml, CharEncoding.UTF_8));
        t.loadFromXML(reader);
        reader.close();
        System.out.println("Writing/Reading this: " + t);
        ConfigurationUtil.assertWriteRead(t);
    }

}
