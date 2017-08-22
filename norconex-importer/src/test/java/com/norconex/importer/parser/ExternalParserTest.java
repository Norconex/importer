/* Copyright 2017 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.commons.lang.exec.ExternalApp;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.commons.lang.unit.DataUnit;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.parser.impl.ExternalParser;
import com.norconex.importer.util.regex.RegexFieldExtractor;

public class ExternalParserTest {
    
    public static final String INPUT = "1 2 3\n4 5 6\n7 8 9";
    public static final String EXPECTED_OUTPUT = "3 2 1\n6 5 4\n9 8 7";
    
    @Test
    public void testWriteRead() throws IOException {
        ExternalParser p = new ExternalParser();
        p.setCommand("my command");
        
        p.setMetadataExtractionPatterns(
            new RegexFieldExtractor("asdf.*", "blah"),
            new RegexFieldExtractor("qwer.*", "halb")
        );
        
        Map<String, String> envs = new HashMap<>();
        envs.put("env1", "value1");
        envs.put("env2", "value2");
        p.setEnvironmentVariables(envs);
        
        System.out.println("Writing/Reading this: " + p);
        XMLConfigurationUtil.assertWriteRead(p);
    }
    
    @Test
    public void testInFileOutFile() 
            throws IOException, DocumentParserException {
        testWithExternalApp(ExternalApp.newCommandLine(
                ExternalApp.TYPE_INFILE_OUTFILE) + " ${INPUT} ${OUTPUT}");
    }
    @Test
    public void testInFileStdout() 
            throws IOException, DocumentParserException {
        testWithExternalApp(ExternalApp.newCommandLine(
                ExternalApp.TYPE_INFILE_STDOUT) + " ${INPUT}");
    }
    @Test
    public void testStdinOutFile() 
            throws IOException, DocumentParserException {
        testWithExternalApp(ExternalApp.newCommandLine(
                ExternalApp.TYPE_STDIN_OUTFILE) + " ${OUTPUT}");
    }
    @Test
    public void testStdinStdout() 
            throws IOException, DocumentParserException {
        testWithExternalApp(
                ExternalApp.newCommandLine(ExternalApp.TYPE_STDIN_STDOUT));
    }
    private void testWithExternalApp(String command) 
            throws IOException, DocumentParserException {
        InputStream input = inputAsStream();
        StringWriter output = new StringWriter();
        ImporterDocument doc = new ImporterDocument("reference", 
                new CachedStreamFactory(
                        (int) DataUnit.KB.toBytes(10), 
                        (int) DataUnit.KB.toBytes(5)).newInputStream(input));
        ImporterMetadata metadata = doc.getMetadata();
        
        
        ExternalParser p = new ExternalParser();
        p.setCommand(command);
        addPatternsAndEnvs(p);
        p.parseDocument(doc, output); 
        
        String content = output.toString();
        // remove any stdout content that could be mixed with output to 
        // properly validate
        content = content.replace("field1:StdoutBefore", "");
        content = content.replace("<field2>StdoutAfter</field2>", "");
        content = content.trim();
        
        Assert.assertEquals(EXPECTED_OUTPUT, content);
        assertMetadata(metadata);
    }
    
    private void assertMetadata(ImporterMetadata meta) {
        Assert.assertEquals("StdoutBefore", meta.getString("field1"));
        Assert.assertEquals("StdoutAfter", meta.getString("field2"));
        Assert.assertEquals("field3 StdErrBefore", meta.getString("field3"));
        Assert.assertEquals("StdErrAfter", meta.getString("field4"));
    }
    
    private void addPatternsAndEnvs(ExternalParser p) {
        Map<String, String> envs = new HashMap<>();
        envs.put(ExternalApp.ENV_STDOUT_BEFORE, "field1:StdoutBefore");
        envs.put(ExternalApp.ENV_STDOUT_AFTER, "<field2>StdoutAfter</field2>");
        envs.put(ExternalApp.ENV_STDERR_BEFORE, "field3 StdErrBefore");
        envs.put(ExternalApp.ENV_STDERR_AFTER, "StdErrAfter:field4");
        p.setEnvironmentVariables(envs);

        p.setMetadataExtractionPatterns(
            new RegexFieldExtractor("^(f.*):(.*)", 1, 2),
            new RegexFieldExtractor("^<field2>(.*)</field2>", "field2", 1),
            new RegexFieldExtractor("^f.*StdErr.*", "field3", 1),
            new RegexFieldExtractor("^(S.*?):(.*)", 2, 1)
        );        
    }
    
    private InputStream inputAsStream() throws IOException {
        return new ByteArrayInputStream(INPUT.getBytes());
    }
}
