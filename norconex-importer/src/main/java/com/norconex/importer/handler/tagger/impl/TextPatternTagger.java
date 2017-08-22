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
package com.norconex.importer.handler.tagger.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.tagger.AbstractStringTagger;
import com.norconex.importer.util.regex.RegexFieldExtractor;
import com.norconex.importer.util.regex.RegexUtil;

/**
 * <p>Extracts and add all text values matching the regular expression provided
 * in to a field provided explicitely, or also matching a regular
 * expression.  The target field is considered a multi-value field.
 * </p>
 * 
 * <p>
 * <b>Since 2.8.0</b>, it is now possible to extract both the field names
 * and their values with regular expression.  This is done by using
 * match groups in your regular expressions (parenthesis).  For each pattern
 * you define, you can specify which match group hold the field name and 
 * which one holds the value.  
 * Specifying a field match group is optional if a <code>field</code> 
 * is provided.  If no match groups are specified, a <code>field</code>
 * is expected.
 * </p>
 * 
 * <p>
 * <b>Since 2.8.0</b>, case-sensitivity for 
 * regular expressions is now set on each patterns. 
 * </p>
 * <p>
 * This class can be used as a pre-parsing handler on text documents only
 * or a post-parsing handler.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TextPatternTagger"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *      &lt;pattern field="(target field name)" 
 *              fieldGroup="(field name match group index)"
 *              valueGroup="(field value match group index)"
 *              caseSensitive="[false|true]"&gt;
 *          (regular expression)
 *      &lt;/pattern&gt;
 *      &lt;!-- multiple pattern tags allowed --&gt;
 * 
 *  &lt;/tagger&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The first pattern in the following example extracts what look like email 
 * addresses in to an "email" field (simplified regex). The second pattern
 * extracts field names and values from "label" and "value" cells on
 * a given HTML table:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TextPatternTagger" &gt;
 *      &lt;pattern field="emails"&gt;
 *          [A-Za-z0-9+_.-]+?@[a-zA-Z0-9.-]+
 *      &lt;/pattern&gt;
 *      &lt;pattern fieldGroup="1" valueGroup="2"&gt;&lt;![CDATA[
 *        &lt;tr&gt;&lt;td class="label"&gt;(.*?)&lt;/td&gt;&lt;td class="value"&gt;(.*?)&lt;/td&gt;&lt;/tr&gt;
 *      ]]&gt;&lt;/pattern&gt;
 *  &lt;/tagger&gt;
 * </pre>
 *  
 * @author Pascal Essiembre
 * @since 2.3.0
 */
public class TextPatternTagger 
        extends AbstractStringTagger implements IXMLConfigurable {

    private static final Logger LOG = 
            LogManager.getLogger(RegexFieldExtractor.class);
    private List<RegexFieldExtractor> patterns = new ArrayList<>();

    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex) {
        RegexUtil.extractFields(metadata, content, 
                patterns.toArray(RegexFieldExtractor.EMPTY_ARRAY));
    }

    /**
     * Gets whether to ignore case when matching text.
     * @return <code>true</code> if case sensitive.
     * @deprecated Always false. Case sensitivity is now set from each pattern
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return false;
    }
    /**
     * Sets whether to ignore case when matching text.
     * @param caseSensitive <code>true</code> to consider character case
     * @deprecated Always false. Case sensitivity is now set on each pattern
     */
    @Deprecated
    public void setCaseSensitive(boolean caseSensitive) {
        LOG.warn("setCaseSensitive is deprecated. Set it on patterns instead");
    }
    /**
     * Adds a pattern that will extract the whole text matched into 
     * given field.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     */
    public void addPattern(String field, String pattern) {
        if (StringUtils.isAnyBlank(pattern, field)) {
            return;
        }
        addPattern(new RegexFieldExtractor(pattern).setField(field));
    }
    /**
     * Adds a new pattern, which will extract the value from the specified 
     * group index upon matching.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     * @param valueGroup which pattern group to return.
     */
    public void addPattern(String field, String pattern, int valueGroup) {
        if (StringUtils.isAnyBlank(pattern, field)) {
            return;
        }
        addPattern(new RegexFieldExtractor(
                pattern).setField(field).setValueGroup(valueGroup));
    }
    /**
     * Adds one or more pattern that will extract matching field names/values.
     * @param pattern field extractor pattern
     */
    public void addPattern(RegexFieldExtractor... pattern) {
        if (ArrayUtils.isNotEmpty(pattern)) {
            patterns.addAll(Arrays.asList(pattern));
        }
    }
    /**
     * Sets one or more patterns that will extract matching field names/values.
     * Clears previously set pattterns.
     * @param pattern field extractor pattern
     */
    public void setPattern(RegexFieldExtractor... pattern) {
        patterns.clear();
        if (ArrayUtils.isNotEmpty(pattern)) {
            patterns.addAll(Arrays.asList(pattern));
        }
    }
    /**
     * Gets the patterns used to extract matching field names/values.
     * @return patterns
     */
    public List<RegexFieldExtractor> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }
    
    @Override
    protected void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        List<HierarchicalConfiguration> nodes = xml.configurationsAt("pattern");
        for (HierarchicalConfiguration node : nodes) {
            int valueGroup = node.getInt("[@group]", -1);
            if (valueGroup != -1) {
                LOG.warn("\"group\" attribute is deprecated in favor of "
                        + "\"valueGroup\". Update your XML configuration.");
            }
            valueGroup = node.getInt("[@valueGroup]", valueGroup);
            addPattern(new RegexFieldExtractor(node.getString("", null))
                   .setCaseSensitive(node.getBoolean("[@caseSensitive]", false))
                   .setField(node.getString("[@field]", null))
                   .setFieldGroup(node.getInt("[@fieldGroup]", -1))
                   .setValueGroup(valueGroup));
        }
    }

    @Override
    protected void saveStringTaggerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
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
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TextPatternTagger)) {
            return false;
        }
        TextPatternTagger castOther = (TextPatternTagger) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(patterns, castOther.patterns)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(patterns)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("patterns", patterns)
                .toString();
    }
}
