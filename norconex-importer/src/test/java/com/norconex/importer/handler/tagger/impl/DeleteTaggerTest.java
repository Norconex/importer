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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.input.NullInputStream;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class DeleteTaggerTest {

    static {
        // Root
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        logger.setAdditivity(false);
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
//        // Crawler
//        logger = Logger.getLogger(HttpCrawler.class);
//        logger.setLevel(Level.DEBUG);
        
//        // Apache
//        logger = Logger.getLogger("org.apache");
//        logger.setLevel(Level.WARN);
        
    }
    
    @Test
    public void testWriteRead() throws IOException {
        DeleteTagger tagger = new DeleteTagger();
        tagger.addField("potato");
        tagger.addField("potato");
        tagger.addField("carrot");
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }
    
    @Test
    public void testDeleteField() throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("field1", "delete me");
        meta.addString("field1", "delete me too");
        meta.setString("field2", "delete also");
        meta.setString("field3", "keep this one");
        meta.setString("field4", "one last to delete");

        DeleteTagger tagger = new DeleteTagger();
        tagger.addField("field1");
        tagger.addField("field2");
        tagger.addField("field4");

        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        Assert.assertEquals("Invalid field count", 1, meta.size());
        Assert.assertEquals("Value wrongfully deleted or modified", 
                "keep this one", meta.getString("field3"));
    }
    
    @Test
    public void testDeleteViaXMLConfig() 
            throws IOException, ImporterHandlerException {
        ImporterMetadata meta = new ImporterMetadata();
        meta.addString("content-type", "blah");
        meta.addString("x-access-level", "blah");
        meta.addString("X-CONTENT-TYPE-OPTIONS", "blah");
        meta.addString("X-FRAME-OPTIONS", "blah");
        meta.addString("X-PARSED-BY", "blah");
        meta.addString("date", "blah");
        meta.addString("X-RATE-LIMIT-LIMIT", "blah");
        meta.addString("source", "blah");
        
        DeleteTagger tagger = new DeleteTagger();
        
        Reader r = new StringReader(
                "<tagger fields=\"X-ACCESS-LEVEL,X-content-type-options,"
              + "X-FRAME-OPTIONS,X-PARSED-BY,X-RATE-LIMIT-LIMIT\" />");
        tagger.loadFromXML(r);
        
        tagger.tagDocument("blah", new NullInputStream(0), meta, false);
        
        Assert.assertEquals("Invalid field count", 3, meta.size());
    }
    



}
