/* Copyright 2016 Norconex Inc.
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
package com.norconex.importer.parser;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.response.ImporterResponse;

public class ImageParserTest extends AbstractParserTest {

    @Test
    public void testBMP() throws Exception {
        testParsing("image/x-ms-bmp", "bmp");
    }
    @Test
    public void testGIF() throws Exception {
        testParsing("image/gif", "gif");
    }
    @Test
    public void testJPG() throws Exception {
        testParsing("image/jpeg", "jpg");
    }
    @Test
    public void testJPG_XMP() throws Exception {
        // JPEG with XMP metadata.  Can be dealt with, with a tool such
        // as http://www.exiv2.org
        // Currently parsed by Tika using Jempbox
        ImporterResponse[] responses = 
                testParsing("/parser/image/importer-xmp.jpg",
                        "image/jpeg", ".*", "jpg", "Image");
        ImporterMetadata meta = responses[0].getDocument().getMetadata();
        Assert.assertEquals("Could not find XMP metadata dc:subject with "
                + "expected value \"XML Parsing\".",
                "XMP Parsing", meta.getString("dc:subject"));
    }
    @Test
    public void testPNG() throws Exception {
        testParsing("image/png", "png");
    }
    @Test
    public void testPSD() throws Exception {
        testParsing("/parser/image/importer.psd",
                "image/vnd.adobe.photoshop", ".*", "psd", "Image");        
    }
    @Test
    public void testTIF() throws Exception {
        testParsing("/parser/image/importer.tif",
                "image/tiff", ".*", "tiff", "Image");        
    }

    @Test
    public void testJBIG2() throws Exception {
        ImporterResponse[] responses = testParsing("/parser/image/importer.jb2",
                "image/x-jbig2", ".*", "jb2", "Image");        
        ImporterMetadata meta = responses[0].getDocument().getMetadata();
        
        Assert.assertEquals("Image 'width' not extracted or invalid",
                "125", meta.getString("width"));
        Assert.assertEquals("Image 'height' not extracted or invalid",
                "16", meta.getString("height"));
    }

    private void testParsing(String contentType, String extension)
            throws Exception {
        testParsing("/parser/image/importer." + extension,
                contentType, ".*", extension, "Image");
    }
}
