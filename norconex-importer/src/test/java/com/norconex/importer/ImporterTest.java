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
package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;
import com.norconex.importer.handler.transformer.IDocumentTransformer;
import com.norconex.importer.response.ImporterResponse;

public class ImporterTest {

    private Importer importer;
    
    @Before
    public void setUp() throws Exception {
        ImporterConfig config = new ImporterConfig();
        config.setPostParseHandlers(new IDocumentTransformer[] {
                new IDocumentTransformer() {
            @Override
            public void transformDocument(String reference, InputStream input,
                    OutputStream output, ImporterMetadata metadata, 
                            boolean parsed) throws ImporterHandlerException {
                try {
                    // Clean up what we know is extra noise for a given format
                    Pattern pattern = 
                            Pattern.compile("[^a-zA-Z ]", Pattern.MULTILINE);
                    String txt = IOUtils.toString(input);
                    txt = pattern.matcher(txt).replaceAll("");
                    txt = txt.replaceAll("DowntheRabbitHole", "");
                    txt = StringUtils.replace(txt, " ", "");
                    txt = StringUtils.replace(txt, "httppdfreebooksorg", "");
                    IOUtils.write(txt, output);
                } catch (IOException e) {
                    throw new ImporterHandlerException(e);
                }
            }
        }});
        importer = new Importer(config);
    }

    @After
    public void tearDown() throws Exception {
        importer = null;
    }
    
    @Test
    public void testImportDocument() throws IOException, ImporterException {
        
        // MS Doc
        File docxOutput = File.createTempFile("ImporterTest-doc-", ".txt");
        Properties metaDocx = new Properties();
        writeToFile(importer.importDocument(
                TestUtil.getAliceDocxFile(), metaDocx).getDocument(), 
                        docxOutput);

        // PDF
        File pdfOutput = File.createTempFile("ImporterTest-pdf-", ".txt");
        Properties metaPdf = new Properties();
        writeToFile(importer.importDocument(
                TestUtil.getAlicePdfFile(), metaPdf).getDocument(), pdfOutput);

        // ZIP/RTF
        File rtfOutput = File.createTempFile("ImporterTest-zip-rtf-", ".txt");
        Properties metaRtf = new Properties();
        writeToFile(importer.importDocument(
                TestUtil.getAliceZipFile(), metaRtf).getDocument(), rtfOutput);
        
        Assert.assertTrue("Converted file size is too small to be valid.",
                pdfOutput.length() > 10);

        double doc = docxOutput.length();
        double pdf = pdfOutput.length();
        double rtf = rtfOutput.length();
        if (Math.abs(pdf - doc) / 1024.0 > 0.03 
                || Math.abs(pdf - rtf) / 1024.0 > 0.03) {
            Assert.fail("Content extracted from examples documents are too "
                    + "different from each other. They were not deleted to "
                    + "help you troubleshoot under: " 
                    + FileUtils.getTempDirectoryPath() + "ImporterTest-*");
        } else {
            FileUtils.deleteQuietly(docxOutput);
            FileUtils.deleteQuietly(pdfOutput);
            FileUtils.deleteQuietly(rtfOutput);
        }
    }

    @Test
    public void testImportRejected() throws IOException, ImporterException {
        ImporterConfig config = new ImporterConfig();
        config.setPostParseHandlers(new RegexMetadataFilter(
                "Content-Type", "application/pdf", OnMatch.EXCLUDE));
        Importer importer = new Importer(config);
        ImporterResponse result = importer.importDocument(
                TestUtil.getAlicePdfFile(), ContentType.PDF, null, 
                        new ImporterMetadata(), "n/a");
        System.out.println("Reject desc: "
                        + result.getImporterStatus().getDescription());
        Assert.assertTrue(result.getImporterStatus().isRejected() 
                && result.getImporterStatus().getDescription().contains(
                        "RegexMetadataFilter"));
    }
    
    private void writeToFile(ImporterDocument doc, File file)
            throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(doc.getContent(), out);
        out.close();
    }
    
}
