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
package com.norconex.importer.handler.transformer.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
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
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedOutputStream;
import com.norconex.commons.lang.io.InputStreamLineListener;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.ImporterRuntimeException;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.TextPatternTagger;
import com.norconex.importer.handler.transformer.AbstractDocumentTransformer;
import com.norconex.importer.util.regex.RegexFieldExtractor;
import com.norconex.importer.util.regex.RegexUtil;

/**
 * <p>
 * Transforms a document content using an external application to do so.
 * When constructing the command to launch the external application, these 
 * placeholders will be replaced if provided (case-sensitive): 
 * </p>
 * <table summary="Placeholder tokens">
 *   <tr>
 *     <td><code>${INPUT}</code></td>
 *     <td>File to transform.</td>
 *   </tr>
 *   <tr>
 *     <td><code>${OUTPUT}</code></td>
 *     <td>Resulting file from the transformation.</td>
 *   </tr>
 * </table>
 * <p>
 * Both are optional and if not provided, the file input or output will be 
 * STDIN or STDOUT respectively.
 * </p>
 * <p>
 * Execution environment variables can be set to replace environment variables
 * defined for the current process.
 * </p>
 * <p>
 * It is also possible to specify metadata extraction patterns that will be
 * applied on each line returned from STDOUT and STDERR.  With each pattern,
 * there could be a matadata field name supplied. If the pattern does not 
 * contain any match group, the entire matched expression will be used as the 
 * metadata field value.  
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
 * <p>
 * <b>Since 2.8.0</b>, it is also possible to set regular expressions 
 * case-sensitivity for each patterns. 
 * </p>
 * <p>
 * To extract metadata from
 * a generated file instead of STDOUT, use an additional handler such as 
 * {@link TextPatternTagger} to do so.   
 * </p>
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
 *      &lt;command&gt;c:\Apps\myapp.exe ${INPUT} ${OUTPUT}&lt;/command&gt;
 *      
 *      &lt;metadata&gt;
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
 * @since 2.7.0
 */
public class ExternalTransformer extends AbstractDocumentTransformer
        implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(ExternalTransformer.class);    

    public static final String TOKEN_INPUT = "${INPUT}";
    public static final String TOKEN_OUTPUT = "${OUTPUT}";

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
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
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
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
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
     * @deprecated Since 2.8.0, use {@link #addMetadataExtractionPatterns(RegexFieldExtractor...)}
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
    
    @Override
    protected void transformApplicableDocument(String reference,
            final InputStream input, final OutputStream output, 
            final ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {
        validate();
        String cmd = command;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Command before token replacement: " + cmd);
        }
        
        final File inputFile;
        if (cmd.contains(TOKEN_INPUT)) {
            inputFile = newInputFile(input);
            cmd = StringUtils.replace(
                    cmd, TOKEN_INPUT, inputFile.getAbsolutePath());
        } else {
            inputFile = null;
        }
        
        final File outputFile;
        if (cmd.contains(TOKEN_OUTPUT)) {
            outputFile = newOutputFile(output);
            cmd = StringUtils.replace(
                    cmd, TOKEN_OUTPUT, outputFile.getAbsolutePath());
        } else {
            outputFile = null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Command after token replacement:  " + cmd);
        }
        
        SystemCommand systemCommand = new SystemCommand(cmd);
        systemCommand.setEnvironmentVariables(environmentVariables);
        systemCommand.addOutputListener(new InputStreamLineListener() {
            @Override
            protected void lineStreamed(String type, String line) {
                if (outputFile == null) {
                    writeLine(line, output);
                }
                extractMetaFromLine(line, metadata);
            }
        });
        systemCommand.addErrorListener(new InputStreamLineListener() {
            @Override
            protected void lineStreamed(String type, String line) {
                extractMetaFromLine(line, metadata);
            }
        });
        
        try {
            int exitValue;
            if (inputFile != null) {
                exitValue = systemCommand.execute();
            } else {
                exitValue = systemCommand.execute(input);
            }
            if (exitValue != 0) {
                LOG.error("Bad command exit value: " + exitValue);
            }
        } catch (SystemCommandException e) {
            throw new ImporterHandlerException(
                    "External transformer failed. Command: " + command, e);
        } finally {
            deleteFile(inputFile);
        }
        if (outputFile != null) {
            try {
                FileUtils.copyFile(outputFile, output);
                output.flush();
            } catch (IOException e) {
                throw new ImporterHandlerException(
                        "Could not read command output file. Command: "
                                + command, e);
            } finally {
                deleteFile(outputFile);
            }
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

    private synchronized void extractMetaFromLine(
            String line, ImporterMetadata metadata) {
        RegexUtil.extractFields(metadata, line, 
                patterns.toArray(RegexFieldExtractor.EMPTY_ARRAY));
    }
    
    private File newInputFile(InputStream is) throws ImporterHandlerException {
        File tempDir;
        if (is instanceof CachedInputStream) {
            tempDir = ((CachedInputStream) is).getCacheDirectory();
        } else {
            tempDir = FileUtils.getTempDirectory();
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File file = null;
        try {
            file = File.createTempFile("input", ".tmp", tempDir);
            FileUtils.copyInputStreamToFile(is, file);
            return file;
        } catch (IOException e) {
            deleteFile(file);
            throw new ImporterHandlerException(
                    "Could not create temporary input file.", e);
        }
    }
    private File newOutputFile(OutputStream os) throws ImporterHandlerException {
        File tempDir;
        if (os instanceof CachedOutputStream) {
            tempDir = ((CachedOutputStream) os).getCacheDirectory();
        } else {
            tempDir = FileUtils.getTempDirectory();
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        try {
            return File.createTempFile("output", ".tmp", tempDir);
        } catch (IOException e) {
            throw new ImporterHandlerException(
                    "Could not create temporary output file.", e);
        }
    }

    private void validate() throws ImporterHandlerException {
        if (StringUtils.isBlank(command)) {
            throw new ImporterHandlerException("External command missing.");
        }
    }
    
    private void deleteFile(File file) {
        if (file != null && !file.delete()) {
            LOG.warn("Could not delete temporary file: "
                    + file.getAbsolutePath());
        }
    }
    
    @Override
    protected void loadHandlerFromXML(XMLConfiguration xml) throws IOException {
        setCommand(xml.getString("command", getCommand()));
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
                   .setValueGroup(node.getInt("[@valueGroup]", -1)));
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
        if (!getMetadataExtractionPatterns().isEmpty()) {
            writer.writeStartElement("metadata");
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
                .append(patterns, castOther.patterns)
                .isEquals()
                && EqualsUtil.equalsMap(getEnvironmentVariables(), 
                        castOther.getEnvironmentVariables());

    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(getCommand())
                .append(getMetadataExtractionPatterns())
                .append(getEnvironmentVariables())
                .toHashCode();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("command", getCommand())
                .append("metadataExtractionPatterns", 
                        getMetadataExtractionPatterns())
                .append("environmentVariables", getEnvironmentVariables())
                .toString();
    }
}
