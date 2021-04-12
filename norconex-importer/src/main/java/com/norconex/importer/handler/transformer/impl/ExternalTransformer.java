/* Copyright 2017-2021 Norconex Inc.
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.exec.SystemCommand;
import com.norconex.commons.lang.exec.SystemCommandException;
import com.norconex.commons.lang.io.ICachedStream;
import com.norconex.commons.lang.io.InputStreamLineListener;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.ImporterRuntimeException;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;
import com.norconex.importer.parser.impl.ExternalParser;
import com.norconex.importer.util.regex.RegexFieldExtractor;
import com.norconex.importer.util.regex.RegexUtil;

/**
 * <p>
 * Transforms a document content using an external application to do so.
 * </p>
 * <p>
 * <b>Since 2.8.0</b>, it is now possible to also pass the document metadata
 * and reference to the external application and get new metadata back.
 * 2.8.0 also makes metadata fields regular expression matching more flexible.
 * </p>
 * <p>
 * <b>Since 2.8.0</b>, it is also possible to set regular expressions
 * case-sensitivity for each patterns.
 * </p>
 * <p>
 * <b>Since 2.8.0</b>, match group indexes can be specified
 * to extract field names and values using the same regular
 * expression.  This is done by using
 * match groups in your regular expressions (parenthesis).  For each pattern
 * you define, you can specify which match group hold the field name and
 * which one holds the value.
 * Specifying a field match group is optional if a <code>field</code>
 * is provided.  If no match groups are specified, a <code>field</code>
 * is expected.
 * </p>
 *
 * <h3>Command-line arguments:</h3>
 * <p>
 * When constructing the command to launch the external application, this
 * transformer will look for specific tokens to be replaced by file paths
 * arguments (in addition to other arguments you may have).
 * The paths are created by this transformer. They are case-sensitive and
 * the file they represent are temporary (will be deleted after
 * the transformation). It is possible to omit one or more tokens to use
 * standard streams instead where applicable.  These tokens are:
 * </p>
 * <dl>
 *
 *   <dt><code>${INPUT}</code></dt>
 *   <dd>Path to document to transform. When omitted, the document content
 *       is sent to the external application using the standard input
 *       stream (STDIN).</dd>
 *
 *   <dt><code>${INPUT_META}</code></dt>
 *   <dd>Path to file containing metadata information available
 *       so far for the document to transform. By default in
 *       JSON format. When omitted, no metadata will be made available
 *       to the external application.</dd>
 *
 *   <dt><code>${OUTPUT}</code></dt>
 *   <dd>Path to document resulting from the transformation.
 *       When omitted, the transformed content will be read from the external
 *       application standard output (STDOUT).</dd>
 *
 *   <dt><code>${OUTPUT_META}</code></dt>
 *   <dd>Path to file containing new metadata for the document.
 *       By default, the expected format is JSON.
 *       When omitted, any metadata extraction patterns defined will be
 *       applied against both the external program standard output (STDOUT)
 *       and standard error (STDERR). If no patterns are defined, it is
 *       assumed no new metadata resulted from the transformation.</dd>
 *
 *   <dt><code>${REFERENCE}</code></dt>
 *   <dd>Unique reference to the document being transformed
 *       (URL, original file system location, etc.). When omitted,
 *       the document reference will not be made available
 *       to the external application.</dd>
 *
 * </dl>
 *
 * <h3>Metadata file format:</h3>
 *
 * <p>
 * If <code>${INPUT_META}</code> is part of the command, metadata can be
 * provided to the external application in JSON (default) or XML format or
 * Properties.  Those
 * formats can also be used if <code>${OUTPUT_META}</code> is part of the
 * command. The formats are:
 * </p>
 *
 * <h4>JSON</h4>
 * <pre>
 * {
 *   "field1" : [ "value1a", "value1b", "value1c" ],
 *   "field2" : [ "value2" ],
 *   "field3" : [ "value3a", "value3b" ]
 * }
 * </pre>
 *
 * <h4>XML</h4>
 * <p>Java Properties XML file format, with the exception that
 * metadata with multiple values are supported, and will have their values
 * saved on different lines (repeating the key).
 * Example:
 * </p>
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 * &lt;properties&gt;
 *   &lt;comment&gt;My Comment&lt;/comment&gt;
 *   &lt;entry key="field1"&gt;value1a&lt;/entry&gt;
 *   &lt;entry key="field1"&gt;value1b&lt;/entry&gt;
 *   &lt;entry key="field1"&gt;value1c&lt;/entry&gt;
 *   &lt;entry key="field2"&gt;value2&lt;/entry&gt;
 *   &lt;entry key="field3"&gt;value3a&lt;/entry&gt;
 *   &lt;entry key="field3"&gt;value3b&lt;/entry&gt;
 * &lt;/properties&gt;
 * </pre>
 *
 * <h4>Properties</h4>
 * <p>Java Properties standard file format, with the exception that
 * metadata with multiple values are supported, and will have their values
 * saved on different lines (repeating the key). Refer to Java
 * {@link Properties#load(java.io.Reader)} for syntax information.
 * Example:
 * </p>
 * <pre>
 *   # My Comment
 *   field1 = value1a
 *   field1 = value1b
 *   field1 = value1c
 *   field2 = value2
 *   field3 = value3a
 *   field3 = value3b
 * </pre>
 *
 * <h3>Metadata extraction patterns:</h3>
 * <p>
 * It is possible to specify metadata extraction patterns that will be
 * applied either on the returned metadata file or from the standard output and
 * error streams.  If <code>${OUTPUT_META}</code> is found in the command,
 * the output format will be
 * used to parse the outgoing metadata file. Leave the format to
 * <code>null</code> to rely on extraction patterns for parsing the output file.
 * </p>
 * <p>
 * When <code>${OUTPUT_META}</code> is omitted, extraction patterns will be
 * applied to
 * the external application standard output and standard error streams. If
 * there are no <code>${OUTPUT_META}</code> and no metadata extraction patterns
 * are defined, it is assumed the external application did not produce any new
 * metadata.
 * </p>
 * <p>
 * When using metadata extraction patterns with standard streams, each pattern
 * is applied on each line returned from STDOUT and STDERR.  With each pattern,
 * there could be a matadata field name supplied. If the pattern does not
 * contain any match group, the entire matched expression will be used as the
 * metadata field value.
 * </p>
 * <p>
 * Field names and values can be obtained by using the same regular
 * expression.  This is done by using
 * match groups in your regular expressions (parenthesis).  For each pattern
 * you define, you can specify which match group hold the field name and
 * which one holds the value.
 * Specifying a field match group is optional if a <code>field</code>
 * is provided.  If no match groups are specified, a <code>field</code>
 * is expected.
 * </p>
 *
 * <h3>Environment variables:</h3>
 *
 * <p>
 * Execution environment variables can be set to replace environment variables
 * defined for the current process.
 * </p>
 *
 * <p>
 * To extract raw text from files, it is recommended to use an
 * {@link com.norconex.importer.parser.impl.ExternalParser} instead.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ExternalTransformer"&gt;
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
 *  &lt;/transformer&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following example invokes an external application that accepts two
 * files as arguments: the first one being the file to transform, the second
 * one being holding the transformation result. It also extract a document
 * number from STDOUT, found as "DocNo:1234" and storing it as "docnumber".
 * </p>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ExternalTransformer"&gt;
 *      &lt;command&gt;/path/transform/app ${INPUT} ${OUTPUT}&lt;/command&gt;
 *      &lt;metadata&gt;
 *          &lt;match field="docnumber" valueGroup="1"&gt;DocNo:(\d+)&lt;/match&gt;
 *      &lt;/metadata&gt;
 *  &lt;/transformer&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @see ExternalParser
 * @since 2.7.0
 */
public class ExternalTransformer extends AbstractDocumentTransformer
        implements IXMLConfigurable {

    private static final Logger LOG =
            LogManager.getLogger(ExternalTransformer.class);

    public static final String TOKEN_INPUT = "${INPUT}";
    public static final String TOKEN_OUTPUT = "${OUTPUT}";
    /** @since 2.8.0 */
    public static final String TOKEN_INPUT_META = "${INPUT_META}";
    /** @since 2.8.0 */
    public static final String TOKEN_OUTPUT_META = "${OUTPUT_META}";
    /** @since 2.8.0 */
    public static final String TOKEN_REFERENCE = "${REFERENCE}";

    /** @since 2.8.0 */
    public static final String META_FORMAT_JSON = "json";
    /** @since 2.8.0 */
    public static final String META_FORMAT_XML = "xml";
    /** @since 2.8.0 */
    public static final String META_FORMAT_PROPERTIES = "properties";

    /**
     * @deprecated Since 2.8.0, specify field name and value match groups
     * instead.
     */
    @Deprecated
    public static final String REVERSE_FLAG = "reverse:true";

    private String command;
    private List<RegexFieldExtractor> patterns = new ArrayList<>();

    // Null means inherit from those of java process
    private Map<String, String> environmentVariables = null;

    private String metadataInputFormat = META_FORMAT_JSON;
    private String metadataOutputFormat = META_FORMAT_JSON;
    private File tempDir;

    /**
     * Gets the command to execute.
     * @return the command
     */
    public String getCommand() {
        return command;
    }
    /**
     * Sets the command to execute. Make sure to escape spaces in
     * executable path and its arguments as well as other special command
     * line characters.
     * @param command the command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Gets directory where to store temporary files used for transformation.
     * @return temporary directory
     * @since 2.8.0
     */
    public File getTempDir() {
        return tempDir;
    }
    /**
     * Sets directory where to store temporary files used for transformation.
     * @param tempDir temporary directory
     * @since 2.8.0
     */
    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * Gets metadata extraction patterns. See class documentation.
     * @return map of patterns and field names
     */
    public List<RegexFieldExtractor> getMetadataExtractionPatterns() {
        return Collections.unmodifiableList(patterns);
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned
     * patterns.
     * To reverse the match group order in a double match group pattern,
     * set the field name to "reverse:true".
     * @param metaPatterns map of patterns and field names
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void setMetadataExtractionPatterns(
            Map<Pattern, String> metaPatterns) {
        this.patterns.clear();
        addMetadataExtractionPatterns(metaPatterns);
    }
    /**
     * Adds metadata extraction patterns, keeping any patterns previously
     * assigned.
     * To reverse the match group order in a double match group pattern,
     * set the field name to "reverse:true".
     * @param metaPatterns map of patterns and field names
     * @deprecated Since 2.8.0, use
     * {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPatterns(
            Map<Pattern, String> metaPatterns) {
        for (Entry<Pattern, String> p : metaPatterns.entrySet()) {
            if ("reverse:true".equals(p.getValue())) {
                addMetadataExtractionPattern(p.getKey(), true);
            } else if ("reverse:false".equals(p.getValue())) {
                addMetadataExtractionPattern(p.getKey(), false);
            } else {
                addMetadataExtractionPattern(p.getKey(), p.getValue());
            }
        }
    }
    /**
     * Adds a metadata extraction pattern. See class documentation.
     * @param pattern pattern with two match groups
     * @param reverse whether to reverse match groups (inverse key and value).
     * @deprecated Since 2.8.0, use
     * {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPattern(Pattern pattern, boolean reverse) {
        RegexFieldExtractor r = new RegexFieldExtractor(pattern.pattern());
        if (reverse) {
            r.setFieldGroup(2);
            r.setFieldGroup(1);
        } else {
            r.setFieldGroup(1);
            r.setFieldGroup(2);
        }
        this.patterns.add(r);
    }
    /**
     * Adds a metadata extraction pattern. See class documentation.
     * @param pattern pattern with no or one match group
     * @param field field name where to store the matched pattern
     * @deprecated Since 2.8.0, use
     * {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
     */
    @Deprecated
    public void addMetadataExtractionPattern(Pattern pattern, String field) {
        RegexFieldExtractor r = new RegexFieldExtractor(pattern.pattern());
        r.setField(field);
        this.patterns.add(r);
    }

    /**
     * Adds a metadata extraction pattern that will extract the whole text
     * matched into the given field.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     * @since 2.8.0
     */
    public void addMetadataExtractionPattern(String field, String pattern) {
        if (StringUtils.isAnyBlank(pattern, field)) {
            return;
        }
        addMetadataExtractionPatterns(
                new RegexFieldExtractor(pattern).setField(field));
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
        if (StringUtils.isAnyBlank(pattern, field)) {
            return;
        }
        addMetadataExtractionPatterns(new RegexFieldExtractor(
                pattern).setField(field).setValueGroup(valueGroup));
    }
    /**
     * Adds a metadata extraction pattern that will extract matching field
     * names/values.
     * @param patterns extraction pattern
     * @since 2.8.0
     */
    public void addMetadataExtractionPatterns(RegexFieldExtractor... patterns) {
        if (ArrayUtils.isNotEmpty(patterns)) {
            this.patterns.addAll(Arrays.asList(patterns));
        }
    }
    /**
     * Sets metadata extraction patterns. Clears any previously assigned
     * patterns.
     * @param patterns extraction pattern
     * @since 2.8.0
     */
    public void setMetadataExtractionPatterns(RegexFieldExtractor... patterns) {
        this.patterns.clear();
        addMetadataExtractionPatterns(patterns);
    }

    /**
     * Gets environment variables.
     * @return environment variables or <code>null</code> if using the current
     *         process environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
    /**
     * Sets the environment variables. Clearing any prevously assigned
     * environment variables. Set <code>null</code> to use
     * the current process environment variables (default).
     * @param environmentVariables environment variables
     */
    public void setEnvironmentVariables(
            Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
    /**
     * Adds the environment variables, keeping environment variables previously
     * assigned. Existing variables of the same name
     * will be overwritten. To clear all previously assigned variables and use
     * the current process environment variables, pass
     * <code>null</code> to
     * {@link ExternalTransformer#setEnvironmentVariables(Map)}.
     * @param environmentVariables environment variables
     */
    public void addEnvironmentVariables(
            Map<String, String> environmentVariables) {
        if (this.environmentVariables != null) {
            this.environmentVariables.putAll(environmentVariables);
        } else {
            this.environmentVariables = environmentVariables;
        }
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
        if (this.environmentVariables == null) {
            this.environmentVariables = new HashMap<>();
        }
        environmentVariables.put(name, value);
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
        return metadataInputFormat;
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
        this.metadataInputFormat = metadataInputFormat;
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
        return metadataOutputFormat;
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
        this.metadataOutputFormat = metadataOutputFormat;
    }

    @Override
    protected void transformApplicableDocument(String reference,
            final InputStream input, final OutputStream output,
            final ImporterMetadata metadata, boolean parsed)
                    throws ImporterHandlerException {
        validate();
        String cmd = command;
        final Files files = new Files();

        //--- Resolve command tokens ---
        if (LOG.isDebugEnabled()) {
            LOG.debug("Command before token replacement: " + cmd);
        }
        FileReader outputMetaReader = null;
        try {
            cmd = resolveInputToken(cmd, files, input);
            cmd = resolveInputMetaToken(cmd, files, input, metadata);
            cmd = resolveOutputToken(cmd, files, output);
            cmd = resolveOutputMetaToken(cmd, files, output);
            cmd = resolveReferenceToken(cmd, reference);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Command after token replacement:  " + cmd);
            }

            //--- Execute Command ---
            CommandResponse response = executeCommand(
                    cmd, files, input, output);
            LOG.debug("Exit value: " + response.exitValue);

            metadata.putAll(response.stdoutMeta);
            metadata.putAll(response.stderrMeta);

            try {
                if (files.hasOutputFile() && output != null) {
                    FileUtils.copyFile(files.outputFile, output);
                    output.flush();
                }
                if (files.hasOutputMetaFile()) {
                    outputMetaReader = new FileReader(files.outputMetaFile);
                    String format = getMetadataOutputFormat();
                    ImporterMetadata fileMeta = new ImporterMetadata();
                    if (META_FORMAT_PROPERTIES.equalsIgnoreCase(format)) {
                        fileMeta.load(outputMetaReader);
                    } else if (META_FORMAT_XML.equals(format)) {
                        fileMeta.loadFromXML(outputMetaReader);
                    } else if (META_FORMAT_JSON.equals(format)) {
                        fileMeta.loadFromJSON(outputMetaReader);
                    } else {
                        extractMetaFromFile(outputMetaReader, fileMeta);
                    }
                    // File metadata overwrites other metadata of the same
                    // name previously set:
                    metadata.keySet().removeAll(fileMeta.keySet());
                    metadata.putAll(fileMeta);
                }
            } catch (IOException e) {
                throw new ImporterHandlerException(
                        "Could not read command output file. Command: "
                                + command, e);
            }
        } finally {
            IOUtils.closeQuietly(outputMetaReader);
            files.deleteAll();
        }
    }

    private CommandResponse executeCommand(
            final String cmd,
            final Files files,
            final InputStream input,
            final OutputStream output) throws ImporterHandlerException {

        final CommandResponse response = new CommandResponse();

        SystemCommand systemCommand = new SystemCommand(cmd);
        systemCommand.setEnvironmentVariables(environmentVariables);
        systemCommand.addOutputListener(new InputStreamLineListener() {
            @Override
            protected void lineStreamed(String type, String line) {
                if (!files.hasOutputFile() && output != null) {
                    writeLine(line, output);
                }
                if (!files.hasOutputMetaFile()) {
                    extractMetaFromLine(line, response.stdoutMeta);
                }
            }
        });
        systemCommand.addErrorListener(new InputStreamLineListener() {
            @Override
            protected void lineStreamed(String type, String line) {
                if (!files.hasOutputMetaFile()) {
                    extractMetaFromLine(line, response.stderrMeta);
                }
            }
        });

        try {
            int exitValue;
            if (files.hasInputFile() || input == null) {
                exitValue = systemCommand.execute();
            } else {
                exitValue = systemCommand.execute(input);
            }
            if (exitValue != 0) {
                LOG.error("Bad command exit value: " + exitValue);
            }
            response.exitValue = exitValue;
            return response;
        } catch (SystemCommandException e) {
            throw new ImporterHandlerException(
                    "External transformer failed. Command: " + command, e);
        }
    }

    private void writeLine(String line, OutputStream output) {
        try {
            output.write(line.getBytes());
            output.write('\n');
            output.flush();
        } catch (IOException e) {
            throw new ImporterRuntimeException(
                    "Could not write to output", e);
        }
    }

    private synchronized void extractMetaFromFile(
            Reader reader, ImporterMetadata metadata) {
        Iterator<String> it = IOUtils.lineIterator(reader);
        while (it.hasNext()) {
            extractMetaFromLine(it.next(), metadata);
        }
    }

    private synchronized void extractMetaFromLine(
            String line, ImporterMetadata metadata) {
        RegexUtil.extractFields(metadata, line,
                patterns.toArray(RegexFieldExtractor.EMPTY_ARRAY));
    }

    private File createTempFile(
            Object stream, String name, String suffix)
                    throws ImporterHandlerException {
        File tempDirectory;
        if (tempDir != null) {
            tempDirectory = tempDir;
        } else if (stream != null && stream instanceof ICachedStream) {
            tempDirectory = ((ICachedStream) stream).getCacheDirectory();
        } else {
            tempDirectory = FileUtils.getTempDirectory();
        }
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }
        File file = null;
        try {
            file = File.createTempFile(name, suffix, tempDirectory);
            return file;
        } catch (IOException e) {
            Files.delete(file);
            throw new ImporterHandlerException(
                    "Could not create temporary input file.", e);
        }
    }

    private String resolveInputToken(String cmd, Files files, InputStream is)
            throws ImporterHandlerException {
        if (!cmd.contains(TOKEN_INPUT) || is == null) {
            return cmd;
        }
        String newCmd = cmd;
        files.inputFile = createTempFile(is, "input", ".tmp");
        newCmd = StringUtils.replace(
                newCmd, TOKEN_INPUT, files.inputFile.getAbsolutePath());
        try {
            FileUtils.copyInputStreamToFile(is, files.inputFile);
            return newCmd;
        } catch (IOException e) {
            Files.delete(files.inputFile);
            throw new ImporterHandlerException(
                    "Could not create temporary input file.", e);
        }
    }
    private String resolveInputMetaToken(
            String cmd, Files files, InputStream is, ImporterMetadata meta)
                    throws ImporterHandlerException {
        if (!cmd.contains(TOKEN_INPUT_META)) {
            return cmd;
        }
        String newCmd = cmd;
        files.inputMetaFile = createTempFile(
                is, "input-meta", "." + StringUtils.defaultIfBlank(
                        getMetadataInputFormat(), META_FORMAT_JSON));
        newCmd = StringUtils.replace(newCmd,
                TOKEN_INPUT_META, files.inputMetaFile.getAbsolutePath());
        try (FileWriter fw = new FileWriter(files.inputMetaFile)) {
            String format = getMetadataInputFormat();
            if (META_FORMAT_PROPERTIES.equalsIgnoreCase(format)) {
                meta.store(fw);
            } else if (META_FORMAT_XML.equals(format)) {
                meta.storeToXML(fw);
            } else {
                meta.storeToJSON(fw);
            }
            fw.flush();
            return newCmd;
        } catch (IOException e) {
            Files.delete(files.inputMetaFile);
            throw new ImporterHandlerException(
                    "Could not create temporary input metadata file.", e);
        }
    }

    private String resolveOutputToken(String cmd, Files files, OutputStream os)
            throws ImporterHandlerException {
        if (!cmd.contains(TOKEN_OUTPUT) || os == null) {
            return cmd;
        }
        String newCmd = cmd;
        files.outputFile = createTempFile(os, "output", ".tmp");
        newCmd = StringUtils.replace(
                newCmd, TOKEN_OUTPUT, files.outputFile.getAbsolutePath());
        return newCmd;
    }

    private String resolveOutputMetaToken(
            String cmd, Files files, OutputStream os)
                    throws ImporterHandlerException {
        if (!cmd.contains(TOKEN_OUTPUT_META)) {
            return cmd;
        }
        String newCmd = cmd;
        files.outputMetaFile = createTempFile(
                os, "output-meta", "." + StringUtils.defaultIfBlank(
                        getMetadataOutputFormat(), ".tmp"));
        newCmd = StringUtils.replace(newCmd,
                TOKEN_OUTPUT_META, files.outputMetaFile.getAbsolutePath());
        return newCmd;
    }

    private String resolveReferenceToken(String cmd, String reference) {
        if (!cmd.contains(TOKEN_REFERENCE)) {
            return cmd;
        }
        return StringUtils.replace(cmd, TOKEN_REFERENCE, reference);
    }

    private void validate() throws ImporterHandlerException {
        if (StringUtils.isBlank(command)) {
            throw new ImporterHandlerException("External command missing.");
        }
    }

    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setCommand(xml.getString("command", getCommand()));
        String dir = xml.getString("tempDir", null);
        if (StringUtils.isNotBlank(dir)) {
            setTempDir(new File(dir));
        }
        List<HierarchicalConfiguration> xmlMatches =
                xml.configurationsAt("metadata.match");
        if (!xmlMatches.isEmpty()) {
            LOG.warn("\"match\" is deprecated in favor of \"pattern\". "
                    + "Please update your XML configuration");
            Map<Pattern, String> xmlPatterns = new HashMap<>();
            for (HierarchicalConfiguration node : xmlMatches) {
                String field = node.getString("[@field]", null);
                // empty instead of blank in case spaces can be field name
                if (StringUtils.isEmpty(field)) {
                    if (node.getBoolean("[@reverseGroups]", false)) {
                        field = "reverse:true";
                    } else {
                        field = "ExternalTransformer-unnamed-field";
                    }
                }
                xmlPatterns.put(Pattern.compile(node.getString("")), field);
            }
            setMetadataExtractionPatterns(xmlPatterns);
        }

        setMetadataInputFormat(xml.getString(
                "metadata[@inputFormat]", getMetadataInputFormat()));
        setMetadataOutputFormat(xml.getString(
                "metadata[@outputFormat]", getMetadataOutputFormat()));

        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("metadata.pattern");
        for (HierarchicalConfiguration node : nodes) {
            int valueGroup = node.getInt("[@group]", -1);
            valueGroup = node.getInt("[@valueGroup]", valueGroup);
            addMetadataExtractionPatterns(
                new RegexFieldExtractor(node.getString("", null))
                   .setCaseSensitive(node.getBoolean("[@caseSensitive]", false))
                   .setField(node.getString("[@field]", null))
                   .setFieldGroup(node.getInt("[@fieldGroup]", -1))
                   .setValueGroup(valueGroup));
        }

        List<HierarchicalConfiguration> xmlEnvs =
                xml.configurationsAt("environment.variable");
        if (!xmlEnvs.isEmpty()) {
            Map<String, String> vars = new HashMap<>();
            for (HierarchicalConfiguration node : xmlEnvs) {
                vars.put(node.getString("[@name]"), node.getString(""));
            }
            setEnvironmentVariables(vars);
        }
    }
    @Override
    protected void saveHandlerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeElementString("command", getCommand());
        writer.writeElementString(
                "tempDir", Objects.toString(getTempDir(), null));
        if (!getMetadataExtractionPatterns().isEmpty()) {
            writer.writeStartElement("metadata");
            writer.writeAttributeString(
                    "inputFormat", getMetadataInputFormat());
            writer.writeAttributeString(
                    "outputFormat", getMetadataOutputFormat());
            for (RegexFieldExtractor rfe : patterns) {
                writer.writeStartElement("pattern");
                writer.writeAttributeString("field", rfe.getField());
                writer.writeAttributeInteger("fieldGroup", rfe.getFieldGroup());
                writer.writeAttributeInteger("valueGroup", rfe.getValueGroup());
                writer.writeAttributeBoolean(
                        "caseSensitive", rfe.isCaseSensitive());
                writer.writeCharacters(rfe.getRegex());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        if (getEnvironmentVariables() != null) {
            writer.writeStartElement("environment");
            for (Entry<String, String> entry
                    : getEnvironmentVariables().entrySet()) {
                writer.writeStartElement("variable");
                writer.writeAttribute("name", entry.getKey());
                writer.writeCharacters(entry.getValue());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ExternalTransformer)) {
            return false;
        }
        ExternalTransformer castOther = (ExternalTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(getCommand(), castOther.getCommand())
                .append(getTempDir(), castOther.getTempDir())
                .append(patterns, castOther.patterns)
                .append(metadataInputFormat, castOther.metadataInputFormat)
                .append(metadataOutputFormat, castOther.metadataOutputFormat)
                .isEquals()
                && EqualsUtil.equalsMap(getEnvironmentVariables(),
                        castOther.getEnvironmentVariables());

    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(getCommand())
                .append(getTempDir())
                .append(getMetadataExtractionPatterns())
                .append(getEnvironmentVariables())
                .append(getMetadataInputFormat())
                .append(getMetadataOutputFormat())
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("command", getCommand())
                .append("tempDir", getTempDir())
                .append("metadataExtractionPatterns",
                        getMetadataExtractionPatterns())
                .append("environmentVariables", getEnvironmentVariables())
                .append("metadataInputFormat", getMetadataInputFormat())
                .append("metadataOutputFormat", getMetadataOutputFormat())
                .toString();
    }

    static class CommandResponse {
        // Make those separate to prevent possible concurrency exceptions
        private final ImporterMetadata stdoutMeta = new ImporterMetadata();
        private final ImporterMetadata stderrMeta = new ImporterMetadata();
        private int exitValue;
    }

    static class Files {
        File inputFile;
        File inputMetaFile;
        File outputFile;
        File outputMetaFile;
        boolean hasInputFile() {
            return inputFile != null;
        }
        boolean hasInputMetaFile() {
            return inputMetaFile != null;
        }
        boolean hasOutputFile() {
            return outputFile != null;
        }
        boolean hasOutputMetaFile() {
            return outputMetaFile != null;
        }
        void deleteAll() {
            delete(inputFile);
            delete(inputMetaFile);
            delete(outputFile);
            delete(outputMetaFile);
        }
        static void delete(File file) {
            if (file != null && !file.delete()) {
                LOG.warn("Could not delete temporary file: "
                        + file.getAbsolutePath());
            }
        }
    }
}
