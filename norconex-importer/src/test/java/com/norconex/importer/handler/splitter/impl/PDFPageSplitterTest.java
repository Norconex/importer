/* Copyright 2018 Norconex Inc.
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
package com.norconex.importer.handler.splitter.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;

/**
 * @author Pascal Essiembre
 *
 */
public class PDFPageSplitterTest {

    private InputStream input;
    
    @Before
    public void setup() throws IOException {
        input = CsvSplitterTest.class.getResourceAsStream(
                 CsvSplitterTest.class.getSimpleName() + ".pdf");
    }
    @After
    public void tearDown() throws IOException {
        IOUtils.closeQuietly(input);
    }    
    
    @Test
    public void testSplit() throws IOException {
        
    }
    
    
    @Test
    public void testWriteRead() throws IOException {
        PDFPageSplitter splitter = new PDFPageSplitter();
        splitter.setReferencePagePrefix("#page");
        System.out.println("Writing/Reading this: " + splitter);
        XMLConfigurationUtil.assertWriteRead(splitter);
    }

//    private List<ImporterDocument> split(CsvSplitter splitter) 
//            throws IOException, ImporterHandlerException {
//        ImporterMetadata metadata = new ImporterMetadata();
//        SplittableDocument doc = new SplittableDocument("n/a", input, metadata);
//        
//        CachedStreamFactory factory = new CachedStreamFactory(
//                100 * 1024,  100 * 1024);
//        
//        List<ImporterDocument> docs = splitter.splitApplicableDocument(
//                doc, new NullOutputStream(), factory, false);
//        return docs;
//        
//    }

}
