/* Copyright 2014-2016 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.ImporterHandlerException;

public class LanguageTaggerTest {

//    @Before
//    public void before() {
//        Logger logger = Logger.getRootLogger();
//        logger.setLevel(Level.INFO);
//        logger.setAdditivity(false);
//        logger.addAppender(new ConsoleAppender(
//                new PatternLayout("%-5p [%C{1}] %m%n"), 
//                ConsoleAppender.SYSTEM_OUT));
//    }
    
    private static Map<String, String> sampleTexts;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        sampleTexts = new HashMap<>();
        sampleTexts.put("en", "just a bit of text");
        sampleTexts.put("fr", "juste un peu de texte");
        sampleTexts.put("it", "solo un po 'di testo");
        sampleTexts.put("es", "sólo un poco de texto");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        sampleTexts.clear();
        sampleTexts = null;
    }

    @Test
    public void testNonMatchingDocLanguage() throws ImporterHandlerException {
        CachedStreamFactory factory = 
                new CachedStreamFactory(10 * 1024, 10 * 1024);
        LanguageTagger tagger = new LanguageTagger();
        tagger.setLanguages("fr", "it");
        ImporterDocument doc = new ImporterDocument(
                "n/a", factory.newInputStream(sampleTexts.get("en")));
        tagger.tagDocument(doc.getReference(), 
                doc.getContent(), doc.getMetadata(), true);
        Assert.assertNotEquals("en", doc.getMetadata().getLanguage());
    }
    
    @Test
    public void testDefaultLanguageDetection() throws ImporterHandlerException {
        CachedStreamFactory factory = 
                new CachedStreamFactory(10 * 1024, 10 * 1024);
        LanguageTagger tagger = new LanguageTagger();
        tagger.setLanguages("en", "fr", "it", "es");
        
        for (String lang : sampleTexts.keySet()) {
            ImporterDocument doc = new ImporterDocument(
                    "n/a", factory.newInputStream(sampleTexts.get(lang)));
            tagger.tagDocument(doc.getReference(), 
                    doc.getContent(), doc.getMetadata(), true);
            Assert.assertEquals(lang, doc.getMetadata().getLanguage());
        }
    }
    
    @Test
    public void testWriteRead() throws IOException {
        LanguageTagger tagger = new LanguageTagger();
        tagger.setKeepProbabilities(true);
        tagger.setFallbackLanguage("fr");

        ConfigurationUtil.assertWriteRead(tagger);
        
        tagger.setLanguages("it", "br", "en");
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
