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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;

public class RegexContentFilterTest {

    @Test
    public void testAcceptDocument() 
            throws IOException, ImporterHandlerException {

        RegexContentFilter filter = new RegexContentFilter();
        filter.setRegex(".*string.*");
        filter.setOnMatch(OnMatch.EXCLUDE);

        Assert.assertFalse("test1 not filtered properly.", 
                filter.acceptDocument("n/a", 
                        IOUtils.toInputStream("a string to match"),
                        null, false));

        Assert.assertTrue("test2 not filtered properly.", 
                filter.acceptDocument("n/a", 
                        IOUtils.toInputStream("another one not to match"),
                        null, false));
    }    
    
    @Test
    public void testWriteRead() throws IOException {
        RegexContentFilter filter = new RegexContentFilter();
        filter.addRestriction("author", "Pascal.*", false);
        filter.setRegex("blah");
        filter.setOnMatch(OnMatch.INCLUDE);
        System.out.println("Writing/Reading this: " + filter);
        ConfigurationUtil.assertWriteRead(filter);
    }
}
