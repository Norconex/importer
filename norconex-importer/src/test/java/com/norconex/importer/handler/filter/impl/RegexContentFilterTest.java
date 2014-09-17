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
