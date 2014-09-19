/* Copyright 2014 Norconex Inc.
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
