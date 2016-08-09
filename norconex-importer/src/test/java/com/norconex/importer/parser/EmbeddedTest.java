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
package com.norconex.importer.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.norconex.importer.Importer;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.ImporterException;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.response.ImporterResponse;

public class EmbeddedTest {

    private static final String ZIP = "application/zip";
    private static final String PPT = "application/vnd"
            + ".openxmlformats-officedocument.presentationml.presentation";
    private static final String XLS = "application/vnd"
            + ".openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PNG = "image/png";
    private static final String TXT = "text/plain";

    // temp folder is for embedded tests only. move embedded tests in own file?
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    @Before
    public void before() {
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.INFO);
        logger.setAdditivity(false);
        logger.addAppender(new ConsoleAppender(
                new PatternLayout("%-5p [%C{1}] %m%n"), 
                ConsoleAppender.SYSTEM_OUT));
    }

    @Test
    public void testEmbeddedDefaultMerged() 
            throws IOException, ImporterException {
        
        // Make sure merge works (extracting all embedded)
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        ImporterResponse response = importFileZipFile(f);

        // no split == no child responses
        Assert.assertEquals("Non-split parsing cannot have child responses.",
                0, response.getNestedResponses().length);
        
        // must have detected 5 content types:
        // 1 zip, which holds:
        //      1 PowerPoint file, which holds 1 excel and 1 picture (png)
        //      1 Plain text file
        String[] expectedTypes = { ZIP, PPT, XLS, PNG, TXT };
        List<String> responseTypes = getTikaContentTypes(response);
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }
        
        // make sure spreadsheet is extracted
        String content = IOUtils.toString(
                response.getDocument().getContent(), CharEncoding.UTF_8);
        Assert.assertTrue("Spreadsheet not extracted.", 
                content.contains("column 1"));
    }
    
    @Test
    public void testEmbeddedDefaultSplit() 
            throws IOException, ImporterException {
        
        // Make sure split works (extracting all embedded)
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setSplitContentTypes(".*");
        ImporterResponse zipResponse = importFileZipFile(f);

        // split == 2 child responses, one of which has two or more more
        Assert.assertEquals("Zip must have two embedded docs.",
                2, zipResponse.getNestedResponses().length);

        ImporterResponse pptResponse = findResponse(zipResponse, PPT);
        Assert.assertTrue("PowerPoint must have at least two embedded docs.",  
                pptResponse.getNestedResponses().length >= 2);
        
        // must have detected 5 content types:
        // 1 zip, which holds:
        //      1 PowerPoint file, which holds 1 excel and 1 picture (png)
        //      1 Plain text file
        String[] expectedTypes = { ZIP, PPT, XLS, PNG, TXT };
        List<String> responseTypes = getTikaContentTypes(zipResponse);
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }

        ImporterResponse xlsResponse = findResponse(pptResponse, XLS);
        String xlsContent = IOUtils.toString(
                xlsResponse.getDocument().getContent(), CharEncoding.UTF_8);
        // make sure spreadsheet is extracted
        Assert.assertTrue("Spreadsheet not extracted.", 
                xlsContent.contains("column 1"));
    }

    @Test
    public void testEmbeddedSplitZipOnly()
            throws IOException, ImporterException {
        
        // Split embedded files in zip but no deeper (PowerPoint embedded files
        // must not be split).
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setSplitContentTypes(ZIP);
        ImporterResponse zipResponse = importFileZipFile(f);

        // split == 2 child responses, with PowerPoint being merged (no child).
        Assert.assertEquals("Zip must have two embedded docs.",
                2, zipResponse.getNestedResponses().length);

        ImporterResponse pptResponse = findResponse(zipResponse, PPT);
        Assert.assertEquals("PowerPoint must not have any embedded docs.",  
                0, pptResponse.getNestedResponses().length);
        
        // must NOT have detected PowerPoint embedded (XLS and PNG)
        Assert.assertNull("Must not find Excel sheet response.", 
                findResponse(pptResponse, XLS));
        Assert.assertNull("Must not find PNG inage response.", 
                findResponse(pptResponse, PNG));

        String pptContent = IOUtils.toString(
                pptResponse.getDocument().getContent(), CharEncoding.UTF_8);
        // make sure spreadsheet is extracted as part of PowerPoint
        Assert.assertTrue("Spreadsheet not extracted.", 
                pptContent.contains("column 1"));
    }    
    
    @Test
    public void testNoExtractContainerZipMerged()
            throws IOException, ImporterException {
        
        // Extract zip content, but not its embedded files.  So should just
        // get file names as zip content.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractContainerContentTypes(
                ZIP);
        ImporterResponse zipResponse = importFileZipFile(f);

        
        // must NOT have other content types, just zip.
        List<String> responseTypes = getTikaContentTypes(zipResponse);
        Assert.assertEquals("Should only have 1 content type.", 
                1, responseTypes.size());

        // the only content type must be zip
        Assert.assertEquals("Must be zip content type.",
                ZIP, zipResponse.getDocument().getContentType().toString());
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));

        // make sure content contains file names
        Assert.assertTrue("File names must be extracted.", 
                content.contains("embedded.pptx")
                && content.contains("embedded.txt"));
    }

    @Test
    public void testNoExtractContainerZipSplit()
            throws IOException, ImporterException {
        
        // Extract zip content, but not its embedded files.  So should just
        // get file names as zip content.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractContainerContentTypes(
                ZIP);
        f.getParseHints().getEmbeddedConfig().setSplitContentTypes(".*");
        ImporterResponse zipResponse = importFileZipFile(f);
        
        // must NOT have other content types, just zip.
        Assert.assertTrue("Should not have nested responses.", 
                ArrayUtils.isEmpty(zipResponse.getNestedResponses()));

        List<String> responseTypes = getTikaContentTypes(zipResponse);
        Assert.assertEquals("Should only have 1 content type.", 
                1, responseTypes.size());
        // must NOT have detected PowerPoint or text file
        Assert.assertNull("Must not find Excel sheet response.", 
                findResponse(zipResponse, PPT));
        Assert.assertNull("Must not find Text response.", 
                findResponse(zipResponse, TXT));

        // the only content type must be zip
        Assert.assertEquals("Must be zip content type.",
                ZIP, zipResponse.getDocument().getContentType().toString());
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));

        // make sure content contains file names
        Assert.assertTrue("File names must be extracted.", 
                content.contains("embedded.pptx")
                && content.contains("embedded.txt"));
    }

    
    @Test
    public void testNoExtractContainerPowerPointMerged()
            throws IOException, ImporterException {
        
        // Extract zip content and its embedded files, except for its 
        // PowerPoint, which should not see its embedded files extracted.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractContainerContentTypes(
                PPT);
        ImporterResponse zipResponse = importFileZipFile(f);

        
        // must have only zip, ppt, and txt.
        List<String> responseTypes = getTikaContentTypes(zipResponse);
        Assert.assertEquals("Should only have 3 content typs.", 
                3, responseTypes.size());
        
        // must have detected 3 content types:
        // 1 zip, which holds:
        //      1 PowerPoint file, which holds no embedded
        //      1 Plain text file
        String[] expectedTypes = { ZIP, PPT, TXT };
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }

        // Must not contain XLS and PNG
        Assert.assertFalse("Must not contain XLS.", 
                responseTypes.contains(XLS));
        Assert.assertFalse("Must not contain PNG.", 
                responseTypes.contains(PNG));
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));

        // make sure PowerPoint was otherwise extracted
        Assert.assertTrue("PowerPoint must be extracted.", 
                content.contains("Slide 1: Embedded Test"));
    }


    @Test
    public void testNoExtractContainerPowerPointSplit()
            throws IOException, ImporterException {
        
        // Extract zip content and its embedded files, except for its 
        // PowerPoint, which should not see its embedded files extracted.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractContainerContentTypes(
                PPT);
        f.getParseHints().getEmbeddedConfig().setSplitContentTypes(".*");
        ImporterResponse zipResponse = importFileZipFile(f);

        
        // must have only zip, ppt, and txt.
        List<String> responseTypes = getTikaContentTypes(zipResponse);
        Assert.assertEquals("Should only have 3 content typs.", 
                3, responseTypes.size());
        
        // must have detected 3 content types:
        // 1 zip, which holds:
        //      1 PowerPoint file, which holds no embedded
        //      1 Plain text file
        String[] expectedTypes = { ZIP, PPT, TXT };
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }

        // Must not contain XLS and PNG
        Assert.assertFalse("Must not contain XLS.", 
                responseTypes.contains(XLS));
        Assert.assertFalse("Must not contain PNG.", 
                responseTypes.contains(PNG));

        // must have detected PowerPoint and text file
        Assert.assertNotNull("Must have PowerPoint response.", 
                findResponse(zipResponse, PPT));
        Assert.assertNotNull("Must have Text response.", 
                findResponse(zipResponse, TXT));
        
        // must NOT have detected PowerPoint embedded files
        Assert.assertNull("Must not find Excel sheet response.", 
                findResponse(zipResponse, XLS));
        Assert.assertNull("Must not find Image response.", 
                findResponse(zipResponse, PNG));
        
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));

        // make sure PowerPoint was otherwise extracted
        ImporterResponse pptResponse = findResponse(zipResponse, PPT);
        Assert.assertTrue("PowerPoint must be extracted.", 
                IOUtils.toString(pptResponse.getDocument().getContent(), 
                        CharEncoding.UTF_8).contains("Slide 1: Embedded Test"));
    }
    
    
    @Test
    public void testNoExtractEmbeddedExcelMerged()
            throws IOException, ImporterException {
        
        // Extract zip content and all its embedded except for its excel file.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractEmbeddedContentTypes(
                XLS);
        ImporterResponse zipResponse = importFileZipFile(f);

        
        // must have all content types except for Excel.
        // 1 zip, which holds:
        //      1 PowerPoint file, which should have extracted PNG only
        //      1 Plain text file
        List<String> responseTypes = getTikaContentTypes(zipResponse);

        String[] expectedTypes = { ZIP, PPT, PNG, TXT };
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }

        // Must not contain XLS and PNG
        Assert.assertFalse("Must not contain XLS.", 
                responseTypes.contains(XLS));
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));
    }

    @Test
    public void testNoExtractEmbeddedExcelSplit()
            throws IOException, ImporterException {
        
        // Extract zip content and all its embedded except for its excel file.
        
        GenericDocumentParserFactory f = new GenericDocumentParserFactory();
        f.getParseHints().getEmbeddedConfig().setNoExtractEmbeddedContentTypes(
                XLS);
        f.getParseHints().getEmbeddedConfig().setSplitContentTypes(".*");;
        ImporterResponse zipResponse = importFileZipFile(f);

        
        // must have all content types except for Excel.
        // 1 zip, which holds:
        //      1 PowerPoint file, which should have extracted PNG only
        //      1 Plain text file
        List<String> responseTypes = getTikaContentTypes(zipResponse);

        String[] expectedTypes = { ZIP, PPT, PNG, TXT };
        for (String type : expectedTypes) {
            Assert.assertTrue("Expected to find " + type, 
                    responseTypes.contains(type));
        }

        // Must not contain XLS and PNG
        Assert.assertFalse("Must not contain XLS.", 
                responseTypes.contains(XLS));
        
        // must have detected PowerPoint, text file, and PNG
        Assert.assertNotNull("Must have PowerPoint response.", 
                findResponse(zipResponse, PPT));
        Assert.assertNotNull("Must have Text response.", 
                findResponse(zipResponse, TXT));
        Assert.assertNotNull("Must have Image response.", 
                findResponse(zipResponse, PNG));
        
        // must NOT have detected PowerPoint embedded files
        Assert.assertNull("Must not find Excel sheet response.", 
                findResponse(zipResponse, XLS));
        
        
        String content = IOUtils.toString(
                zipResponse.getDocument().getContent(), CharEncoding.UTF_8);
        
        // make sure spreadsheet content is NOT extracted
        Assert.assertFalse("Spreadsheet must not be extracted.", 
                content.contains("column 1"));
    }
    
    
    
    private ImporterResponse findResponse(
            ImporterResponse response, String contentType) {
        if (response.getDocument().getContentType().toString().equals(
                contentType)) {
            return response;
        }
        ImporterResponse[] childResponses = response.getNestedResponses();
        if (childResponses == null) {
            return null;
        }
        for (ImporterResponse childResponse : childResponses) {
            ImporterResponse foundResponse = 
                    findResponse(childResponse, contentType);
            if (foundResponse != null) {
                return foundResponse;
            }
        }
        return null;
    }
    
    private List<String> getTikaContentTypes(ImporterResponse response) {
        List<String> types = new ArrayList<>();
        ImporterMetadata meta = response.getDocument().getMetadata();
        List<String> rawTypes = meta.getStrings("Content-Type");
        for (String t : rawTypes) {
            types.add(StringUtils.substringBefore(t, ";"));
        }
        ImporterResponse[] nestedResponses = response.getNestedResponses();
        for (ImporterResponse nr : nestedResponses) {
            types.addAll(getTikaContentTypes(nr));
        }
        return types;
    }
    
    private ImporterResponse importFileZipFile(GenericDocumentParserFactory f)
            throws IOException, ImporterException {
        
        ImporterMetadata metadata = new ImporterMetadata();
        ImporterConfig config = new ImporterConfig();
        config.setParserFactory(f);
        Importer importer = new Importer(config);
        ImporterResponse response = importer.importDocument(
                getZipFile(), metadata);
        return response;
    }

    private File getZipFile() throws IOException {
        InputStream is = getClass().getResourceAsStream(
                "/parser/embedded/embedded.zip");
        File file = folder.newFile("test-embedded.zip");
        FileUtils.copyInputStreamToFile(is, file);
        is.close();
        return file;
    }
}
