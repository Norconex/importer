/* Copyright 2010-2014 Norconex Inc.
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
import com.norconex.importer.handler.transformer.impl.ReduceConsecutivesTransformer;

public class ReduceConsecutivesTransformerTest {

    private final String xml = "<transformer><reduce>\\stext</reduce>"
            + "<reduce>\\t</reduce><reduce>\\n\\r</reduce>"
            + "<reduce>\\s</reduce><reduce>.</reduce></transformer>";
    
    @Test
    public void testTransformTextDocument() throws ImporterHandlerException {
        String text = "\t\tThis is the text TeXt I want to modify...\n\r\n\r"
                + "     Too much space.";
        
        ReduceConsecutivesTransformer t = new ReduceConsecutivesTransformer();

        Reader reader = new InputStreamReader(IOUtils.toInputStream(xml));
        try {
            t.loadFromXML(reader);
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not reduce consecutives.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
        
        InputStream is = IOUtils.toInputStream(text);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try { 
            t.transformDocument("dummyRef", is, os, new ImporterMetadata(), true);
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
        Reader reader = new InputStreamReader(IOUtils.toInputStream(xml));
        t.loadFromXML(reader);
        reader.close();
        System.out.println("Writing/Reading this: " + t);
        ConfigurationUtil.assertWriteRead(t);
    }

}
