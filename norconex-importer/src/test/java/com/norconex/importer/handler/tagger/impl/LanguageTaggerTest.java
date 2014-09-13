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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.norconex.commons.lang.Content;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.ImporterHandlerException;

public class LanguageTaggerTest {

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
        LanguageTagger tagger = new LanguageTagger();
        tagger.setLanguages("fr", "it");
        ImporterDocument doc = new ImporterDocument(
                "n/a", new Content(sampleTexts.get("en")));
        tagger.tagDocument(doc.getReference(), 
                doc.getContent().getInputStream(), doc.getMetadata(), true);
        Assert.assertNotEquals("en", doc.getMetadata().getLanguage());
    }
    
    @Test
    public void testDefaultLanguageDetection() throws ImporterHandlerException {
        LanguageTagger tagger = new LanguageTagger();
        
        for (String lang : sampleTexts.keySet()) {
            ImporterDocument doc = new ImporterDocument(
                    "n/a", new Content(sampleTexts.get(lang)));
            tagger.tagDocument(doc.getReference(), 
                    doc.getContent().getInputStream(), doc.getMetadata(), true);
            Assert.assertEquals(lang, doc.getMetadata().getLanguage());
        }
    }
    
    @Test
    public void testWriteRead() throws IOException {
        LanguageTagger tagger = new LanguageTagger();
        tagger.setKeepProbabilities(true);
        tagger.setShortText(true);
        tagger.setFallbackLanguage("fr");

        ConfigurationUtil.assertWriteRead(tagger);
        
        tagger.setLanguages("it", "br", "en");
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
