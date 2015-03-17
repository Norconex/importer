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
package com.norconex.importer.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.importer.Importer;
import com.norconex.importer.ImporterException;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.response.ImporterResponse;

public abstract class AbstractParserTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    protected File getFile(String resourcePath) throws IOException {
        File file = folder.newFile(
                StringUtils.substringAfterLast(resourcePath, "/"));
        FileUtils.copyInputStreamToFile(getInputStream(resourcePath), file);
        return file;
    }
    
    protected InputStream getInputStream(String resourcePath) {
        return getClass().getResourceAsStream(resourcePath);
    }

    protected void testParsing(
            String resourcePath, String contentType, 
            String contentRegex, String extension, String family) 
                    throws IOException, ImporterException {
        ImporterMetadata metadata = null;
        ImporterResponse response = null;
        ImporterDocument doc = null;
        
        // Test file
        metadata = new ImporterMetadata();
        response = new Importer().importDocument(
                getFile(resourcePath), metadata);
        doc = response.getDocument();
        assertDefaults(doc, "File", 
                resourcePath, contentType, contentRegex, extension, family);

        // Test input stream
        metadata = new ImporterMetadata();
        response = new Importer().importDocument(
                getInputStream(resourcePath), metadata, "guess");
        doc = response.getDocument();
        assertDefaults(doc, "Stream", 
                resourcePath, contentType, contentRegex, extension, family);
    }
    
    private void assertDefaults(
            ImporterDocument doc, 
            String testType, 
            String resourcePath, 
            String contentType,
            String contentRegex, 
            String extension,
            String family) throws IOException {
        Pattern p = Pattern.compile(
                contentRegex, Pattern.DOTALL | Pattern.MULTILINE);

        String content = IOUtils.toString(doc.getContent());
        Assert.assertEquals(testType + " content-type detection failed for \"" 
                + resourcePath + "\".", ContentType.valueOf(contentType), 
                doc.getContentType());

        Assert.assertTrue(testType + " content extraction failed for \"" 
                + resourcePath + "\". Content:\n" + content,
                p.matcher(content).find());

        String ext = doc.getContentType().getExtension();
        Assert.assertEquals(testType + " extension detection failed for \"" 
                + resourcePath + "\".", extension, ext);
        
        String familyEnglish = doc.getContentType()
                .getContentFamily().getDisplayName(Locale.ENGLISH);
//        System.out.println("FAMILY: " + familyEnglish);
        Assert.assertEquals(testType + " family detection failed for \"" 
                + resourcePath + "\".", family, familyEnglish);
        
    }
}
