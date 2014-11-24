/* Copyright 2014 Norconex Inc.
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
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.splitter.SplittableDocument;

/**
 * @author Pascal Essiembre
 *
 */
public class CsvSplitterTest {

    private InputStream input;
    
    @Before
    public void setup() throws IOException {
        input = CsvSplitterTest.class.getResourceAsStream(
                 CsvSplitterTest.class.getSimpleName() + ".csv");
    }

    @After
    public void tearDown() throws IOException {
        IOUtils.closeQuietly(input);
    }

    @Test
    public void testReferenceColumnByName() 
            throws IOException, ImporterHandlerException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        splitter.setReferenceColumn("clientPhone");
        List<ImporterDocument> docs = split(splitter);
        Assert.assertEquals(
                "Could not find embedded William Dalton phone reference.", 
                "654-0987", docs.get(2).getMetadata().getEmbeddedReference());
    }

    @Test
    public void testReferenceColumnByPosition() 
            throws IOException, ImporterHandlerException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(false);
        splitter.setReferenceColumn("2");
        List<ImporterDocument> docs = split(splitter);
        Assert.assertEquals("Could not find embedded William Dalton reference.", 
                "William Dalton", 
                docs.get(3).getMetadata().getEmbeddedReference());
    }
    
    
    @Test
    public void testContentColumn() 
            throws ImporterHandlerException, IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        splitter.setContentColumns("clientName", "3");

        List<ImporterDocument> docs = split(splitter);
        Assert.assertEquals("William Dalton 654-0987", 
                IOUtils.toString(docs.get(2).getContent()));
    }
    
    
    @Test
    public void testFirstRowHeader() 
            throws ImporterHandlerException, IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setUseFirstRowAsFields(true);
        List<ImporterDocument> docs = split(splitter);
        
        Assert.assertEquals(
                "Invalid number of docs returned.", 4, docs.size());

        Assert.assertEquals("Could not find William Dalton by column name.", 
                "William Dalton", 
                docs.get(2).getMetadata().getString("clientName"));
    }
    
    private List<ImporterDocument> split(CsvSplitter splitter) 
            throws IOException, ImporterHandlerException {
        ImporterMetadata metadata = new ImporterMetadata();
        SplittableDocument doc = new SplittableDocument("n/a", input, metadata);
        
        CachedStreamFactory factory = new CachedStreamFactory(
                100 * 1024,  100 * 1024);
        
        List<ImporterDocument> docs = splitter.splitApplicableDocument(
                doc, new NullOutputStream(), factory, false);
        return docs;
        
    }
    
    @Test
    public void testWriteRead() throws IOException {
        CsvSplitter splitter = new CsvSplitter();
        splitter.setEscapeCharacter('.');
        splitter.setLinesToSkip(10);
        splitter.setQuoteCharacter('!');
        splitter.addRestriction("key", "value", true);
        splitter.setSeparatorCharacter('@');
        splitter.setUseFirstRowAsFields(true);
        System.out.println("Writing/Reading this: " + splitter);
        ConfigurationUtil.assertWriteRead(splitter);
    }


}
