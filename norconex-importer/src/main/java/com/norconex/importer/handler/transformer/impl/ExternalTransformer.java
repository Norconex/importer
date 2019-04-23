/* Copyright 2017-2018 Norconex Inc.
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
package com.norconex.importer.handler.transformer.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.text.RegexKeyValueExtractor;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ExternalHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;
import com.norconex.importer.parser.impl.ExternalParser;

/**
 * <p>
 * Transforms a document using an external application to do so.
 * </p>
 * <p>
 * This class relies on {@link ExternalHandler} for most of the work.
 * Refer to {@link ExternalHandler} for full documentation.
 * </p>
 * <p>
 * To parse/extract raw text from files, it is recommended to use
 * {@link ExternalParser} instead.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.ExternalTransformer"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
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
 *      &lt;tempDir&gt;
 *          (Optional directory where to store temporary files used
 *           for transformation.)
 *      &lt;/tempDir&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following example invokes an external application that accepts two
 * files as arguments: the first one being the file to transform, the second
 * one being holding the transformation result. It also extract a document
 * number from STDOUT, found as "DocNo:1234" and storing it as "docnumber".
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.transformer.impl.ExternalTransformer"&gt;
 *      &lt;command&gt;/path/transform/app ${INPUT} ${OUTPUT}&lt;/command&gt;
 *      &lt;metadata&gt;
 *          &lt;match field="docnumber" valueGroup="1"&gt;DocNo:(\d+)&lt;/match&gt;
 *      &lt;/metadata&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @see ExternalHandler
 * @since 2.7.0
 */
public class ExternalTransformer extends AbstractDocumentTransformer {

    private final ExternalHandler h = new ExternalHandler();

    /**
     * Gets the command to execute.
     * @return the command
     */
    public String getCommand() {
        return h.getCommand();
    }
    /**
     * Sets the command to execute. Make sure to escape spaces in
     * executable path and its arguments as well as other special command
     * line characters.
     * @param command the command
     */
    public void setCommand(String command) {
        h.setCommand(command);
    }

    /**
     * Gets metadata extraction patterns. See class documentation.
     * @return map of patterns and field names
     */
    public List<RegexKeyValueExtractor> getMetadataExtractionPatterns() {
        return h.getMetadataExtractionPatterns();
    }
    /**
     * Adds a metadata extraction pattern that will extract the whole text
     * matched into the given field.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     */
    public void addMetadataExtractionPattern(String field, String pattern) {
        h.addMetadataExtractionPattern(field, pattern);
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
        h.addMetadataExtractionPattern(field, pattern, valueGroup);
    }
    /**
     * Adds a metadata extraction pattern that will extract matching field
     * names/values.
     * @param patterns extraction pattern
     */
    public void addMetadataExtractionPatterns(RegexKeyValueExtractor... patterns) {
        h.addMetadataExtractionPatterns(patterns);
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned
     * patterns.
     * @param patterns extraction pattern
     */
    public void setMetadataExtractionPatterns(RegexKeyValueExtractor... patterns) {
        h.setMetadataExtractionPatterns(patterns);
    }

    /**
     * Gets environment variables.
     * @return environment variables or <code>null</code> if using the current
     *         process environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return h.getEnvironmentVariables();
    }
    /**
     * Sets the environment variables. Clearing any prevously assigned
     * environment variables. Set <code>null</code> to use
     * the current process environment variables (default).
     * @param environmentVariables environment variables
     */
    public void setEnvironmentVariables(
            Map<String, String> environmentVariables) {
        h.setEnvironmentVariables(environmentVariables);
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
        h.addEnvironmentVariables(environmentVariables);
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
        h.addEnvironmentVariable(name, value);
    }

    /**
     * Gets the format of the metadata input file sent to the external
     * application. One of "json" (default), "xml", or "properties" is expected.
     * Only applicable when the <code>${INPUT}</code> token
     * is part of the command.
     * @return metadata input format
     */
    public String getMetadataInputFormat() {
        return h.getMetadataInputFormat();
    }
    /**
     * Sets the format of the metadata input file sent to the external
     * application. One of "json" (default), "xml", or "properties" is expected.
     * Only applicable when the <code>${INPUT}</code> token
     * is part of the command.
     * @param metadataInputFormat format of the metadata input file
     */
    public void setMetadataInputFormat(String metadataInputFormat) {
        h.setMetadataInputFormat(metadataInputFormat);
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
        return h.getMetadataOutputFormat();
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
        h.setMetadataOutputFormat(metadataOutputFormat);
    }

    /**
     * Gets directory where to store temporary files used for transformation.
     * @return temporary directory
     */
    public Path getTempDir() {
        return h.getTempDir();
    }
    /**
     * Sets directory where to store temporary files used for transformation.
     * @param tempDir temporary directory
     */
    public void setTempDir(Path tempDir) {
        h.setTempDir(tempDir);
    }

    @Override
    protected void transformApplicableDocument(String reference,
            InputStream input, OutputStream output, ImporterMetadata metadata,
            boolean parsed) throws ImporterHandlerException {
        h.handleDocument(reference, input, output, metadata);
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        h.loadHandlerFromXML(xml);
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        h.saveHandlerToXML(xml);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}