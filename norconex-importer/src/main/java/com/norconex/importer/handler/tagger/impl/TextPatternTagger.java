/* Copyright 2015-2018 Norconex Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.text.RegexKeyValueExtractor;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

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
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextPatternTagger"
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
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The first pattern in the following example extracts what look like email
 * addresses in to an "email" field (simplified regex). The second pattern
 * extracts field names and values from "label" and "value" cells on
 * a given HTML table:
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TextPatternTagger" &gt;
 *      &lt;pattern field="emails"&gt;
 *          [A-Za-z0-9+_.-]+?@[a-zA-Z0-9.-]+
 *      &lt;/pattern&gt;
 *      &lt;pattern fieldGroup="1" valueGroup="2"&gt;&lt;![CDATA[
 *        &lt;tr&gt;&lt;td class="label"&gt;(.*?)&lt;/td&gt;&lt;td class="value"&gt;(.*?)&lt;/td&gt;&lt;/tr&gt;
 *      ]]&gt;&lt;/pattern&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.3.0
 */
public class TextPatternTagger
        extends AbstractStringTagger implements IXMLConfigurable {

    private final List<RegexKeyValueExtractor> patterns = new ArrayList<>();

    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex) {
        RegexKeyValueExtractor.extractKeyValues(metadata, content, patterns);
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
        addPattern(new RegexKeyValueExtractor(pattern).setKey(field));
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
        addPattern(new RegexKeyValueExtractor(
                pattern).setKey(field).setValueGroup(valueGroup));
    }
    /**
     * Adds one or more pattern that will extract matching field names/values.
     * @param pattern field extractor pattern
     */
    public void addPattern(RegexKeyValueExtractor... pattern) {
        if (ArrayUtils.isNotEmpty(pattern)) {
            patterns.addAll(Arrays.asList(pattern));
        }
    }
    /**
     * Sets one or more patterns that will extract matching field names/values.
     * Clears previously set pattterns.
     * @param patterns field extractor pattern
     */
    public void setPattern(RegexKeyValueExtractor... patterns) {
        CollectionUtil.setAll(this.patterns, patterns);
    }
    /**
     * Gets the patterns used to extract matching field names/values.
     * @return patterns
     */
    public List<RegexKeyValueExtractor> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("pattern");
        for (XML node : nodes) {
            addPattern(new RegexKeyValueExtractor(node.getString(".", null))
                   .setCaseSensitive(node.getBoolean("@caseSensitive", false))
                   .setKey(node.getString("@field", null))
                   .setKeyGroup(node.getInteger("@fieldGroup", -1))
                   .setValueGroup(node.getInteger("@valueGroup", -1)));
        }
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
        for (RegexKeyValueExtractor rfe : patterns) {
            xml.addElement("pattern", rfe.getRegex())
                    .setAttribute("field", rfe.getKey())
                    .setAttribute("fieldGroup", rfe.getKeyGroup())
                    .setAttribute("valueGroup", rfe.getValueGroup())
                    .setAttribute("caseSensitive", rfe.isCaseSensitive());
        }
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
