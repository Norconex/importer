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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterException;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.response.ImporterResponse;
import com.norconex.importer.response.ImporterStatus.Status;

/**
 * Part of the tests includes testing all use cases explained
 * in the {@link AbstractDocumentFilter} class.
 * @author Pascal Essiembre
 */
public class RegexContentFilterTest {

    @Test
    public void testMatchesExclude() 
            throws IOException, ImporterHandlerException {
        RegexContentFilter filter = new RegexContentFilter();
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);
        Assert.assertFalse("Should have been rejected.", 
                filter.acceptDocument("n/a", IOUtils.toInputStream(
                        "a string that matches", CharEncoding.UTF_8),
                        null, false));
    }    
    @Test
    public void testMatchesInclude() 
            throws IOException, ImporterHandlerException {
        RegexContentFilter filter = new RegexContentFilter();
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.INCLUDE);
        Assert.assertTrue("Should have been accepted.", 
                filter.acceptDocument("n/a", IOUtils.toInputStream(
                        "a string that matches", CharEncoding.UTF_8),
                        null, false));
    }    
    @Test
    public void testNoMatchesExclude() 
            throws IOException, ImporterHandlerException {
        RegexContentFilter filter = new RegexContentFilter();
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);
        Assert.assertTrue("Should have been accepted.", 
                filter.acceptDocument("n/a", IOUtils.toInputStream(
                        "a text that does not match", CharEncoding.UTF_8),
                        null, false));
    }    
    @Test
    public void testNoMatchesUniqueInclude() 
            throws IOException, ImporterHandlerException {
        
        RegexContentFilter filter = new RegexContentFilter();
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.INCLUDE);
        Assert.assertFalse("Should have been rejected.", 
                filter.acceptDocument("n/a", IOUtils.toInputStream(
                        "a text that does not match", CharEncoding.UTF_8),
                        null, false));
    }    

    @Test
    public void testMatchesOneOfManyIncludes() 
            throws IOException, ImporterException {
        RegexContentFilter filter1 = new RegexContentFilter();
        filter1.setRegex(".*string.*");
        filter1.setOnMatch(OnMatch.INCLUDE);

        RegexContentFilter filter2 = new RegexContentFilter();
        filter2.setRegex(".*asdf.*");
        filter2.setOnMatch(OnMatch.INCLUDE);
        
        RegexContentFilter filter3 = new RegexContentFilter();
        filter3.setRegex(".*qwer.*");
        filter3.setOnMatch(OnMatch.INCLUDE);

        ImporterConfig config = new ImporterConfig();
        config.setPreParseHandlers(filter1, filter2, filter3);
        
        ImporterResponse response = new Importer(config).importDocument(
                new ReaderInputStream(
                        new StringReader("a string that matches"), 
                        CharEncoding.UTF_8),
                new ImporterMetadata(), "N/A");
        Assert.assertEquals("Status should have been SUCCESS",
                Status.SUCCESS, response.getImporterStatus().getStatus());
    }    
    
    @Test
    public void testNoMatchesOfManyIncludes() 
            throws IOException, ImporterException {
        RegexContentFilter filter1 = new RegexContentFilter();
        filter1.setRegex(".*zxcv.*");
        filter1.setOnMatch(OnMatch.INCLUDE);

        RegexContentFilter filter2 = new RegexContentFilter();
        filter2.setRegex(".*asdf.*");
        filter2.setOnMatch(OnMatch.INCLUDE);
        
        RegexContentFilter filter3 = new RegexContentFilter();
        filter3.setRegex(".*qwer.*");
        filter3.setOnMatch(OnMatch.INCLUDE);

        ImporterConfig config = new ImporterConfig();
        config.setPostParseHandlers(filter1, filter2, filter3);
        
        ImporterResponse response = new Importer(config).importDocument(
                new ReaderInputStream(
                        new StringReader("no matches"), CharEncoding.UTF_8),
                new ImporterMetadata(), "N/A");
        Assert.assertEquals("Status should have been REJECTED",
                Status.REJECTED, response.getImporterStatus().getStatus());
    }
    
    @Test
    public void testWriteRead() throws IOException {
        RegexContentFilter filter = new RegexContentFilter();
        filter.addRestriction("author", "Pascal.*", false);
        filter.setRegex("blah");
        filter.setMaxReadSize(256);
        filter.setOnMatch(OnMatch.INCLUDE);
        System.out.println("Writing/Reading this: " + filter);
        ConfigurationUtil.assertWriteRead(filter);
    }
}
