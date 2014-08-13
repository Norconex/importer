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
package com.norconex.importer.parser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterException;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterDocument;

public class DefaultDocumentParserFactoryTest {

    @Test
    public void testIgnoringContentTypes() 
            throws IOException, ImporterException {
        
        DefaultDocumentParserFactory factory = 
                new DefaultDocumentParserFactory();
        factory.setIgnoredContentTypesRegex("application/pdf");
        ImporterMetadata metadata = new ImporterMetadata();

        ImporterConfig config = new ImporterConfig();
        config.setParserFactory(factory);
        Importer importer = new Importer(config);
        ImporterDocument doc = importer.importDocument(
                TestUtil.getAlicePdfFile(), ContentType.PDF, 
                        metadata, "n/a").getDocument();
        
        try (InputStream is = doc.getContent().getInputStream()) {
            String output = IOUtils.toString(is).substring(0, 100);
            output = StringUtils.remove(output, '\n');
            Assert.assertTrue("Non-parsed output expected to be binary.",
                    !StringUtils.isAsciiPrintable(output));
        }
    }
}
