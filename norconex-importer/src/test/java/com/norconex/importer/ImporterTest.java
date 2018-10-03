/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import com.norconex.commons.lang.xml.XMLValidationException;
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
        config.setPostParseHandlers(Arrays.asList((IDocumentTransformer) (reference, input, output, metadata,
                parsed) -> {
            try {
               // Clean up what we know is extra noise for a given format
               Pattern pattern = Pattern.compile("[^a-zA-Z ]");
               String txt = IOUtils.toString(
                input, StandardCharsets.UTF_8);
               txt = pattern.matcher(txt).replaceAll("");
               txt = txt.replaceAll("DowntheRabbitHole", "");
               txt = StringUtils.replace(txt, " ", "");
               txt = StringUtils.replace(txt, "httppdfreebooksorg", "");
               IOUtils.write(txt, output, StandardCharsets.UTF_8);
            } catch (IOException e) {
               throw new ImporterHandlerException(e);
            }
         }));
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
        config.setPostParseHandlers(Arrays.asList(new RegexMetadataFilter(
                "Content-Type", "application/pdf", OnMatch.EXCLUDE)));
        Importer importer = new Importer(config);
        ImporterResponse result = importer.importDocument(
                TestUtil.getAlicePdfFile(), ContentType.PDF, null,
                        new ImporterMetadata(), "n/a");

//        System.out.println("Reject desc: "
//                        + result.getImporterStatus().getDescription());
        Assert.assertTrue("PDF should have been rejected with proper "
                + "status description.",
                result.getImporterStatus().isRejected()
                && result.getImporterStatus().getDescription().contains(
                        "RegexMetadataFilter"));
    }

    private void writeToFile(ImporterDocument doc, File file)
            throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(doc.getInputStream(), out);
        out.close();
    }


    @Test
    public void testValidation() throws IOException {
        InputStream is = getClass().getResourceAsStream(
                "/validation/importer-full.xml");

        try (Reader r = new InputStreamReader(is)) {
            ImporterConfigLoader.loadImporterConfig(r, false);
        } catch (XMLValidationException e) {
            Assert.fail(e.getErrors().size()
                    + "Validation warnings/errors were found.");
        }
    }
}
