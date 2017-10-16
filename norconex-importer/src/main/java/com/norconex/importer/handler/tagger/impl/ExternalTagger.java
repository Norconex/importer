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
package com.norconex.importer.handler.tagger.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.IDocumentTagger;
import com.norconex.importer.handler.transformer.impl.ExternalTransformer;
import com.norconex.importer.util.regex.RegexFieldExtractor;

/**
 * <p>
 * Extracts metadata from a document using an external application to do so.
 * </p>
 * <p>
 * This tagger relies heavily on the mechanics of 
 * {@link ExternalTransformer}, with a few differences:
 * </p>
 * <ul>
 *   <li>
 *     There is no <code>${OUTPUT}</code> token (since taggers do not 
 *     modify cnotent).
 *   </li>
 *   <li>
 *     You can chose not to send any input at all to save some processing
 *     with {@link #setInputDisabled(boolean)}.
 *   </li>
 * </ul>
 * <p>
 * Refer to {@link ExternalTransformer} class for documentation.
 * </p>
 * <p>
 * To use an external application to change a file content consider using 
 * {@link ExternalTransformer} instead. 
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TaggerTransformer"&gt;
 *  
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *      &lt;command inputDisabled="[false|true]"&gt;
 *          c:\Apps\myapp.exe ${INPUT} ${INPUT_META} ${OUTPUT_META} ${REFERENCE}
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
 *      &lt;tempDir&gt;
 *          (Optional directory where to store temporary files used
 *           for transformation.)
 *      &lt;/tempDir&gt;
 *
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following example invokes an external application that accepts
 * a document to transform and outputs a file containing the new metadata
 * information.
 * </p> 
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TaggerTransformer" &gt;
 *      &lt;command&gt;/path/tag/app ${INPUT} ${OUTPUT_META}&lt;/command&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * @author Pascal Essiembre
 * @see ExternalTransformer
 * @since 2.8.0
 */
public class ExternalTagger implements IDocumentTagger, IXMLConfigurable {


    public static final String TOKEN_INPUT = ExternalTransformer.TOKEN_INPUT;
    public static final String TOKEN_INPUT_META = 
            ExternalTransformer.TOKEN_INPUT_META;
    public static final String TOKEN_OUTPUT_META = 
            ExternalTransformer.TOKEN_OUTPUT_META;
    public static final String TOKEN_REFERENCE = 
            ExternalTransformer.TOKEN_REFERENCE;
    public static final String META_FORMAT_JSON = 
            ExternalTransformer.META_FORMAT_JSON;
    public static final String META_FORMAT_XML = 
            ExternalTransformer.META_FORMAT_XML;
    public static final String META_FORMAT_PROPERTIES = 
            ExternalTransformer.META_FORMAT_PROPERTIES;

    private final ExternalTransformer t = new ExternalTransformer();
    private boolean inputDisabled;
    
    /**
     * Gets whether to send the document content or not, regardless
     * whether ${INPUT} token is part of the command or not.
     * @return <code>true</code> to prevent sending the input content
     */
    public boolean isInputDisabled() {
        return inputDisabled;
    }
    /**
     * Sets whether to send the document content or not, regardless
     * whether ${INPUT} token is part of the command or not.
     * @param inputDisabled <code>true</code> to prevent sending the 
     *        input content
     */
    public void setInputDisabled(boolean inputDisabled) {
        this.inputDisabled = inputDisabled;
    }
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
     * Gets metadata extraction patterns. See class documentation.
     * @return map of patterns and field names
     */
    public List<RegexFieldExtractor> getMetadataExtractionPatterns() {
        return t.getMetadataExtractionPatterns();
    }
    /**
     * Adds a metadata extraction pattern that will extract the whole text 
     * matched into the given field.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
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
     */
    public void addMetadataExtractionPattern(
            String field, String pattern, int valueGroup) {
        t.addMetadataExtractionPattern(field, pattern, valueGroup);
    }
    /**
     * Adds a metadata extraction pattern that will extract matching field
     * names/values.
     * @param patterns extraction pattern
     */
    public void addMetadataExtractionPatterns(RegexFieldExtractor... patterns) {
        t.addMetadataExtractionPatterns(patterns);
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned 
     * patterns.
     * @param patterns extraction pattern
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
     * {@link #setEnvironmentVariables(Map)}.
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
     */
    public void setMetadataOutputFormat(String metadataOutputFormat) {
        t.setMetadataOutputFormat(metadataOutputFormat);
    }

    /**
     * Gets directory where to store temporary files used for transformation.
     * @return temporary directory
     */
    public File getTempDir() {
        return t.getTempDir();
    }
    /**
     * Sets directory where to store temporary files used for transformation.
     * @param tempDir temporary directory
     */
    public void setTempDir(File tempDir) {
        t.setTempDir(tempDir);
    }
    
    @Override
    public void tagDocument(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {
        InputStream is = input;
        if (isInputDisabled()) {
            is = null;
        }
        t.transformDocument(reference, is, null, metadata, parsed);
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        String xml = IOUtils.toString(in);
        xml = xml.replaceAll("<(/{0,1})tagger", "<$1transformer");
        this.inputDisabled = Boolean.parseBoolean(xml.replaceFirst(
                "<\\s*command\\b.*?\\s+inputDisabled\\s*=\\s*\"(.*?)\"", "$1")); 
        xml = xml.replaceFirst(
                "(<\\s*command\\b.*?\\s+)inputDisabled\\s*=\\s*\".*?\"", "$1");
        StringReader r = new StringReader(xml);
        t.loadFromXML(r);
    }
    
    @Override
    public void saveToXML(Writer out) throws IOException {
        StringWriter w = new StringWriter();
        t.saveToXML(w);
        String xml = w.toString();
        String attrib = "";
        if (!xml.contains("inputDisabled")) {
            attrib = "inputDisabled=\"" + inputDisabled + "\" ";
        }
        xml = xml.replaceFirst("<transformer class=\".*?\"", 
                "<tagger " + attrib
                + "class=\"" + getClass().getName() + "\"");
        xml = xml.replace("</transformer>", "</tagger>");
        
        out.write(xml);
        out.flush();
        out.close();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ExternalTagger)) {
            return false;
        }
        ExternalTagger castOther = (ExternalTagger) other;
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
            "ExternalTagger\\[xmltag=tagger,restrictions=\\[.*?\\],",
            ExternalTagger.class.getSimpleName() + "[");
        return toString;
    }
}