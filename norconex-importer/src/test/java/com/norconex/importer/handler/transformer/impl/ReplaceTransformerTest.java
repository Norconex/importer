/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
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
