/* Copyright 2014 Norconex Inc.
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
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;

public class GenericDocumentParserFactoryTest {

    @Test
    public void testIgnoringContentTypes() 
            throws IOException, ImporterException {
        
        GenericDocumentParserFactory factory = 
                new GenericDocumentParserFactory();
        factory.setIgnoredContentTypesRegex("application/pdf");
        ImporterMetadata metadata = new ImporterMetadata();

        ImporterConfig config = new ImporterConfig();
        config.setParserFactory(factory);
        Importer importer = new Importer(config);
        ImporterDocument doc = importer.importDocument(
                TestUtil.getAlicePdfFile(), ContentType.PDF, null, 
                        metadata, "n/a").getDocument();
        
        try (InputStream is = doc.getContent()) {
            String output = IOUtils.toString(is).substring(0, 100);
            output = StringUtils.remove(output, '\n');
            Assert.assertTrue("Non-parsed output expected to be binary.",
                    !StringUtils.isAsciiPrintable(output));
        }
    }
}
