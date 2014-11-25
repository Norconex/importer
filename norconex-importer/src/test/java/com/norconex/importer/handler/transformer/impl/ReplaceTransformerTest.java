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
package com.norconex.importer.handler.transformer.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.impl.ReplaceTransformer;

public class ReplaceTransformerTest {

    private final String xml = "<transformer>"
            + "<replace><fromValue>CAKES</fromValue>"
            + "<toValue>FRUITS</toValue></replace>"
            + "<replace><fromValue>candies</fromValue>"
            + "<toValue>vegetables</toValue></replace>"
            + "</transformer>";
    
    @Test
    public void testTransformTextDocument() 
            throws IOException, ImporterHandlerException {
        String text = "I like to eat cakes and candies.";
        
        ReplaceTransformer t = new ReplaceTransformer();

        Reader reader = new InputStreamReader(IOUtils.toInputStream(xml));
        t.loadFromXML(reader);
        reader.close();
        
        InputStream is = IOUtils.toInputStream(text);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        t.transformDocument("dummyRef", is, os, new ImporterMetadata(), true);
        
        String response = os.toString();
        System.out.println(response);
        Assert.assertEquals(
                "i like to eat fruits and vegetables.", 
                response.toLowerCase());

        is.close();
        os.close();
    }
    
    
    @Test
    public void testWriteRead() throws IOException {
        ReplaceTransformer t = new ReplaceTransformer();
        Reader reader = new InputStreamReader(IOUtils.toInputStream(xml));
        t.loadFromXML(reader);
        reader.close();
        System.out.println("Writing/Reading this: " + t);
        ConfigurationUtil.assertWriteRead(t);
    }

}
