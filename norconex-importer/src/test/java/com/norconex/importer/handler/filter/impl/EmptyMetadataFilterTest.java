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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class EmptyMetadataFilterTest {

    @Test
    public void testAcceptDocument() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "a string to match");
        meta.addString("field2", "");

        EmptyMetadataFilter filter = new EmptyMetadataFilter();

        filter.setFields("field1");
        filter.setOnMatch(OnMatch.EXCLUDE);
        
        Assert.assertTrue("field1 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));

        filter.setFields("field2");
        Assert.assertFalse("field2 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));

        filter.setFields("field3");
        Assert.assertFalse("field3 not filtered properly.", 
                filter.acceptDocument("n/a", null, meta, false));
    }    
    
    @Test
    public void testWriteRead() throws IOException {
        EmptyMetadataFilter filter = new EmptyMetadataFilter();
        filter.addRestriction("author", "Pascal.*", false);
        filter.setFields("field1", "field2", "field3");
        filter.setOnMatch(OnMatch.INCLUDE);
        System.out.println("Writing/Reading this: " + filter);
        XMLConfigurationUtil.assertWriteRead(filter);
    }
}
