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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class DOMContentFilterTest {

    private String html = "<html><head><title>Test page</title></head>"
            + "<body>This is sample content.<p>"
            + "<div class=\"disclaimer\">please skip me!</div></body></html>";

    private String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<food><fruit color=\"red\">an apple</fruit></food>";

    @Test
    public void testFilterHTML() 
            throws IOException, ImporterHandlerException {
        DOMContentFilter filter = new DOMContentFilter();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(
                ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        filter.setOnMatch(OnMatch.EXCLUDE);

        filter.setSelector("div.disclaimer");
        Assert.assertFalse("disclaimer should have been rejected.", 
                filter(filter, html, metadata));

        filter.setSelector("div.disclaimer");
        filter.setRegex("\\bskip me\\b");
        Assert.assertFalse("disclaimer skip me should have been rejected.", 
                filter(filter, html, metadata));

        filter.setSelector("div.disclaimer");
        filter.setRegex("\\bdo not skip me\\b");
        Assert.assertTrue(
                "disclaimer do not skip me should have been accepted.", 
                filter(filter, html, metadata));
    }    

    
    @Test
    public void testFilterXML() 
            throws IOException, ImporterHandlerException {
        DOMContentFilter filter = new DOMContentFilter();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(
                ImporterMetadata.DOC_CONTENT_TYPE, "application/xml");
        filter.setOnMatch(OnMatch.INCLUDE);

        filter.setSelector("food > fruit[color=red]");
        Assert.assertTrue("Red fruit should have been accepted.", 
                filter(filter, xml, metadata));

        filter.setSelector("food > fruit[color=green]");
        Assert.assertFalse("Green fruit should have been rejected.", 
                filter(filter, xml, metadata));

        filter.setSelector("food > fruit");
        filter.setRegex("apple");
        Assert.assertTrue("Apple should have been accepted.", 
                filter(filter, xml, metadata));

        filter.setSelector("food > fruit");
        filter.setRegex("carrot");
        Assert.assertFalse("Carrot should have been rejected.", 
                filter(filter, xml, metadata));
    }    
    
    private boolean filter(DOMContentFilter filter, 
            String content, ImporterMetadata metadata) 
                    throws ImporterHandlerException {
        return filter.acceptDocument("n/a", 
                IOUtils.toInputStream(content), metadata, false);
    }
    
    @Test
    public void testWriteRead() throws IOException {
        DOMContentFilter filter = new DOMContentFilter();
        filter.addRestriction("document.contentType", "text/html", false);
        filter.setRegex("blah");
        filter.setOnMatch(OnMatch.INCLUDE);
        filter.setSelector("selector");
        System.out.println("Writing/Reading this: " + filter);
        ConfigurationUtil.assertWriteRead(filter);
    }
}
