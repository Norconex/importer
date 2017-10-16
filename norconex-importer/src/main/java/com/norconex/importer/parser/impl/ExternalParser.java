/* Copyright 2015-2017 Norconex Inc.
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
package com.norconex.importer.parser.impl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterDocument;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.impl.ExternalTransformer;
import com.norconex.importer.parser.DocumentParserException;
import com.norconex.importer.parser.GenericDocumentParserFactory;
import com.norconex.importer.parser.IDocumentParser;
import com.norconex.importer.util.regex.RegexFieldExtractor;

/**
 * <p>
 * Parses and extracts text from a file using an external application to do so.
 * </p>
 * <p>
 * This parser relies heavily on the mechanics of 
 * {@link ExternalTransformer}. Refer to that class for documentation.
 * </p>
 * <p>
 * This parser can be made configurable via XML. See
 * {@link GenericDocumentParserFactory} for general indications how 
 * to configure parsers.  
 * </p>
 * <p>
 * To use an external application to change a file content after parsing has
 * already occurred, consider using {@link ExternalTransformer} instead. 
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;parser contentType="(content type this parser is associated to)" 
 *          class="com.norconex.importer.parser.impl.ExternalParser" &gt;
 *          
 *      &lt;command&gt;
 *          c:\Apps\myapp.exe ${INPUT} ${OUTPUT} ${INPUT_META} ${OUTPUT_META} ${REFERENCE}
 *      &lt;/command&gt;
 *      
 *      &lt;metadata 
 *              inputFormat="[json|xml|properties]" 
 *              outputFormat="[json|xml|properties]"&gt;
 *          &lt;!-- pattern only used when no output format is specified --&gt;
 *          &lt;pattern field="(target field name)" 
 *                  fieldGroup="(field name match group index)"
 *                  valueGroup="(field value match group index)"
 *                  caseSensitive="[false|true]"&gt;
 *              (regular expression)
 *          &lt;/pattern&gt;
 *          &lt;!-- repeat pattern tag as needed --&gt;
 *      &lt;/metadata&gt;
 *      
 *      &lt;environment&gt;
 *          &lt;variable name="(environment variable name)"&gt;
 *              (environment variable value)
 *          &lt;/variable&gt;
 *          &lt;!-- repeat variable tag as needed --&gt;
 *      &lt;/environment&gt;
 *      
 *  &lt;/parser&gt;
 * </pre> 
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following example invokes an external application processing for 
 * simple text files that accepts two files as arguments: 
 * the first one being the file to 
 * transform, the second one being holding the transformation result. 
 * It also extract a document number from STDOUT, found as "DocNo:1234"
 * and storing it as "docnumber".
 * </p> 
 * <pre>
 *  &lt;parser contentType="text/plain" 
 *          class="com.norconex.importer.parser.impl.ExternalParser" &gt;
 *      &lt;command&gt;/path/transform/app ${INPUT} ${OUTPUT}&lt;/command&gt;
 *      &lt;metadata&gt;
 *          &lt;pattern field="docnumber" valueGroup="1"&gt;DocNo:(\d+)&lt;/match&gt;
 *      &lt;/metadata&gt;
 *  &lt;/parser&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see ExternalTransformer
 * @since 2.2.0
 */
public class ExternalParser implements IDocumentParser, IXMLConfigurable {

    public static final String TOKEN_INPUT = ExternalTransformer.TOKEN_INPUT;
    public static final String TOKEN_OUTPUT = ExternalTransformer.TOKEN_OUTPUT;
    /** @since 2.8.0 */
    public static final String TOKEN_INPUT_META = 
            ExternalTransformer.TOKEN_INPUT_META;
    /** @since 2.8.0 */
    public static final String TOKEN_OUTPUT_META = 
            ExternalTransformer.TOKEN_OUTPUT_META;
    /** @since 2.8.0 */
    public static final String TOKEN_REFERENCE = 
            ExternalTransformer.TOKEN_REFERENCE;

    /** @since 2.8.0 */
    public static final String META_FORMAT_JSON = 
            ExternalTransformer.META_FORMAT_JSON;
    /** @since 2.8.0 */
    public static final String META_FORMAT_XML = 
            ExternalTransformer.META_FORMAT_XML;
    /** @since 2.8.0 */
    public static final String META_FORMAT_PROPERTIES = 
            ExternalTransformer.META_FORMAT_PROPERTIES;
    
    private final ExternalTransformer t = new ExternalTransformer();
    private String contentType;
    
    /**
     * Gets the command to execute.
     * @return the command
     */
    public String getCommand() {
        return t.getCommand();
    }
    /**
     * Sets the command to execute. Make sure to escape spaces in 
     * executable path and its arguments as well as other special command
     * line characters.
     * @param command the command
     */
    public void setCommand(String command) {
        t.setCommand(command);
    }
    
    /**
     * Gets directory where to store temporary files used for transformation.
     * @return temporary directory
     * @since 2.8.0
     */
    public File getTempDir() {
        return t.getTempDir();
    }
    /**
     * Sets directory where to store temporary files used for transformation.
     * @param tempDir temporary directory
     * @since 2.8.0
     */
    public void setTempDir(File tempDir) {
        t.setTempDir(tempDir);
    }
    
    /**
     * Gets metadata extraction patterns. See class documentation.
     * @return map of patterns and field names
     */
    public List<RegexFieldExtractor> getMetadataExtractionPatterns() {
        return t.getMetadataExtractionPatterns();
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned 
     * patterns.
     * To reverse the match group order in a double match group pattern,
     * set the field name to "reverse:true".
     * @param patterns map of patterns and field names
     * @deprecated Since 2.8.0, use 
     * {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void setMetadataExtractionPatterns(Map<Pattern, String> patterns) {
        t.setMetadataExtractionPatterns(patterns);
    }
    /**
     * Adds metadata extraction patterns, keeping any patterns previously
     * assigned.
     * To reverse the match group order in a double match group pattern,
     * set the field name to "reverse:true".
     * @param patterns map of patterns and field names
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPatterns(Map<Pattern, String> patterns) {
        t.addMetadataExtractionPatterns(patterns);
    }
    /**
     * Adds a metadata extraction pattern. See class documentation.
     * @param pattern pattern with two match groups
     * @param reverse whether to reverse match groups (inverse key and value).
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPattern(Pattern pattern, boolean reverse) {
        t.addMetadataExtractionPattern(pattern, reverse);
    }
    /**
     * Adds a metadata extraction pattern. See class documentation.
     * @param pattern pattern with no or one match group
     * @param field field name where to store the matched pattern
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPattern(Pattern pattern, String field) {
        t.addMetadataExtractionPattern(pattern, field);
    }
    
    /**
     * Adds a metadata extraction pattern that will extract the whole text 
     * matched into the given field.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     * @since 2.8.0
     */
    public void addMetadataExtractionPattern(String field, String pattern) {
        t.addMetadataExtractionPattern(field, pattern);
    }
    /**
     * Adds a metadata extraction pattern, which will extract the value from 
     * the specified group index upon matching.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     * @param valueGroup which pattern group to return.
     * @since 2.8.0
     */
    public void addMetadataExtractionPattern(
            String field, String pattern, int valueGroup) {
        t.addMetadataExtractionPattern(field, pattern, valueGroup);
    }
    /**
     * Adds a metadata extraction pattern that will extract matching field
     * names/values.
     * @param patterns extraction pattern
     * @since 2.8.0
     */
    public void addMetadataExtractionPatterns(RegexFieldExtractor... patterns) {
        t.addMetadataExtractionPatterns(patterns);
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned 
     * patterns.
     * @param patterns extraction pattern
     * @since 2.8.0
     */    
    public void setMetadataExtractionPatterns(RegexFieldExtractor... patterns) {
        t.setMetadataExtractionPatterns(patterns);
    }
    
    /**
     * Gets environment variables.
     * @return environment variables or <code>null</code> if using the current
     *         process environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return t.getEnvironmentVariables();
    }
    /**
     * Sets the environment variables. Clearing any prevously assigned
     * environment variables. Set <code>null</code> to use
     * the current process environment variables (default).
     * @param environmentVariables environment variables
     */
    public void setEnvironmentVariables(
            Map<String, String> environmentVariables) {
        t.setEnvironmentVariables(environmentVariables);
    }
    /**
     * Adds the environment variables, keeping environment variables previously
     * assigned. Existing variables of the same name
     * will be overwritten. To clear all previously assigned variables and use
     * the current process environment variables, pass 
     * <code>null</code> to 
     * {@link ExternalParser#setEnvironmentVariables(Map)}.
     * @param environmentVariables environment variables
     */
    public void addEnvironmentVariables(
            Map<String, String> environmentVariables) {
        t.addEnvironmentVariables(environmentVariables);
    }
    /**
     * Adds an environment variables to the list of previously
     * assigned variables (if any). Existing variables of the same name
     * will be overwritten. Setting a variable with a 
     * <code>null</code> name has no effect while <code>null</code>
     * values are converted to empty strings.
     * @param name environment variable name
     * @param value environment variable value
     */
    public void addEnvironmentVariable(String name, String value) {
        t.addEnvironmentVariable(name, value);
    }

    /**
     * Gets the format of the metadata input file sent to the external 
     * application. One of "json" (default), "xml", or "properties" is expected. 
     * Only applicable when the <code>${INPUT}</code> token 
     * is part of the command.  
     * @return metadata input format
     * @since 2.8.0
     */
    public String getMetadataInputFormat() {
        return t.getMetadataInputFormat();
    }
    /**
     * Sets the format of the metadata input file sent to the external 
     * application. One of "json" (default), "xml", or "properties" is expected. 
     * Only applicable when the <code>${INPUT}</code> token 
     * is part of the command.  
     * @param metadataInputFormat format of the metadata input file
     * @since 2.8.0
     */
    public void setMetadataInputFormat(String metadataInputFormat) {
        t.setMetadataInputFormat(metadataInputFormat);
    }
    /**
     * Gets the format of the metadata output file from the external 
     * application. By default no format is set, and metadata extraction
     * patterns are used to extract metadata information.
     * One of "json", "xml", or "properties" is expected. 
     * Only applicable when the <code>${OUTPUT}</code> token 
     * is part of the command.  
     * @return metadata output format
     * @since 2.8.0
     */
    public String getMetadataOutputFormat() {
        return t.getMetadataOutputFormat();
    }
    /**
     * Sets the format of the metadata output file from the external 
     * application. One of "json" (default), "xml", or "properties" is expected.
     * Set to <code>null</code> for relying metadata extraction
     * patterns instead.
     * Only applicable when the <code>${OUTPUT}</code> token 
     * is part of the command.  
     * @param metadataOutputFormat format of the metadata output file
     * @since 2.8.0
     */
    public void setMetadataOutputFormat(String metadataOutputFormat) {
        t.setMetadataOutputFormat(metadataOutputFormat);
    }

    @Override
    public List<ImporterDocument> parseDocument(ImporterDocument doc,
            Writer output) throws DocumentParserException {
        try {
            t.transformDocument(doc.getReference(), doc.getContent(), 
                    new WriterOutputStream(output, StandardCharsets.UTF_8), 
                    doc.getMetadata(), false);
        } catch (ImporterHandlerException e) {
            throw new DocumentParserException("Could not parse document: "
                    + doc.getReference(), e);
        }
        return null;
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        String xml = IOUtils.toString(in);
        xml = xml.replaceAll("<(/{0,1})parser", "<$1transformer");
        contentType = xml.replaceFirst(
                ".*?contentType\\s*=\\s*\"(.*?)\".*", "$1");
        xml = xml.replaceFirst("(.*?)contentType\\s*=\\s*\".*?\"(.*)", "$1$2");
        StringReader r = new StringReader(xml);
        t.loadFromXML(r);
    }
    
    @Override
    public void saveToXML(Writer out) throws IOException {
        StringWriter w = new StringWriter();
        t.saveToXML(w);
        String xml = w.toString();
        String ctAttrib = "";
        if (!xml.contains("contentType") && contentType != null) {
            ctAttrib = "contentType=\"" + contentType + "\" ";
        }
        xml = xml.replaceFirst("<transformer class=\".*?\"", 
                "<parser " + ctAttrib
                + "class=\"" + getClass().getName() + "\"");
        xml = xml.replace("</transformer>", "</parser>");
        
        out.write(xml);
        out.flush();
        out.close();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ExternalParser)) {
            return false;
        }
        ExternalParser castOther = (ExternalParser) other;
        return t.equals(castOther.t);
    }
    @Override
    public int hashCode() {
        return t.hashCode();
    }
    @Override
    public String toString() {
        String toString = t.toString();
        toString = toString.replaceFirst(
            "ExternalTransformer\\[xmltag=transformer,restrictions=\\[.*?\\],",
            ExternalParser.class.getSimpleName() + "[");
        return toString;
    }
}
