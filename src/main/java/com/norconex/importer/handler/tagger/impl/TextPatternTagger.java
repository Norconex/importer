/* Copyright 2015-2020 Norconex Inc.
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
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.RegexFieldValueExtractor;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractStringTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Extracts and add all text values matching the regular expression provided
 * in to a field provided explicitly, or also matching a regular
 * expression.  The target field is considered a multi-value field.
 * </p>
 *
 * <p>
 * It is possible to extract both the field names
 * and their values with regular expression.  This is done by using
 * match groups in your regular expressions (parenthesis).  For each pattern
 * you define, you can specify which match group hold the field name and
 * which one holds the value.
 * Specifying a field match group is optional if a <code>field</code>
 * is provided.  If no match groups are specified, a <code>field</code>
 * is expected.
 * </p>
 *
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 *
 * <p>
 * This class can be used as a pre-parsing handler on text documents only
 * or a post-parsing handler.
 * </p>
 *
 * {@nx.xml.usage
 *  <handler class="com.norconex.importer.handler.tagger.impl.TextPatternTagger"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" >
 *
 *      <restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)">
 *          (regular expression of value to match)
 *      </restrictTo>
 *      <!-- multiple "restrictTo" tags allowed (only one needs to match) -->
 *
 *      <pattern toField="(target field name)"
 *              fieldGroup="(field name match group index)"
 *              valueGroup="(field value match group index)"
 *              ignoreCase="[false|true]"
 *              ignoreDiacritic="[false|true]"
 *              onSet="[append|prepend|replace|optional]">
 *          (regular expression)
 *      </pattern>
 *      <!-- multiple pattern tags allowed -->
 *
 *  </handler>
 * }
 *
 * <h4>Usage example:</h4>
 * <p>
 * The first pattern in the following example extracts what look like email
 * addresses in to an "email" field (simplified regex). The second pattern
 * extracts field names and values from "label" and "value" cells on
 * a given HTML table:
 * </p>
 *
 * {@nx.xml.example
 *  <handler class="TextPatternTagger">
 *      <pattern field="emails">
 *          [A-Za-z0-9+_.-]+?@[a-zA-Z0-9.-]+
 *      </pattern>
 *      <pattern fieldGroup="1" valueGroup="2"><![CDATA[
 *        <tr><td class="label">(.*?)</td><td class="value">(.*?)</td></tr>
 *      ]]></pattern>
 *  </handler>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.3.0
 * @deprecated Since 3.0.0, use {@link RegexTagger}.
 */
@Deprecated
public class TextPatternTagger
        extends AbstractStringTagger implements IXMLConfigurable {

    private final List<RegexFieldValueExtractor> patterns = new ArrayList<>();

    @Override
    protected void tagStringContent(HandlerDoc doc, StringBuilder content,
            ParseState parseState, int sectionIndex)
                    throws ImporterHandlerException {
        RegexFieldValueExtractor.extractFieldValues(
                doc.getMetadata(), content, patterns);
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
        addPattern(new RegexFieldValueExtractor(pattern).setToField(field));
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
        addPattern(new RegexFieldValueExtractor(
                pattern).setToField(field).setValueGroup(valueGroup));
    }
    /**
     * Adds one or more pattern that will extract matching field names/values.
     * @param pattern field extractor pattern
     */
    public void addPattern(RegexFieldValueExtractor... pattern) {
        if (ArrayUtils.isNotEmpty(pattern)) {
            patterns.addAll(Arrays.asList(pattern));
        }
    }
    /**
     * Sets one or more patterns that will extract matching field names/values.
     * Clears previously set pattterns.
     * @param patterns field extractor pattern
     */
    public void setPattern(RegexFieldValueExtractor... patterns) {
        CollectionUtil.setAll(this.patterns, patterns);
    }
    /**
     * Gets the patterns used to extract matching field names/values.
     * @return patterns
     */
    public List<RegexFieldValueExtractor> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        List<XML> nodes = xml.getXMLList("pattern");
        for (XML node : nodes) {
            node.checkDeprecated("@caseSensitive", "ignoreCase", true);
            RegexFieldValueExtractor ex = new RegexFieldValueExtractor(
                    node.getString(".", null));
            ex.getRegex().setIgnoreCase(node.getBoolean("@ignoreCase", false));
            ex.getRegex().setIgnoreDiacritic(
                    node.getBoolean("@ignoreDiacritic", false));
            ex.setToField(node.getString("@toField", null));
            ex.setFieldGroup(node.getInteger("@fieldGroup", -1));
            ex.setValueGroup(node.getInteger("@valueGroup", -1));
            ex.setOnSet(PropertySetter.fromXML(node, null));
            addPattern(ex);
        }
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
        for (RegexFieldValueExtractor rfe : patterns) {
            XML node = xml.addElement("pattern", rfe.getRegex().getPattern())
                    .setAttribute("toField", rfe.getToField())
                    .setAttribute("fieldGroup", rfe.getFieldGroup())
                    .setAttribute("valueGroup", rfe.getValueGroup())
                    .setAttribute("ignoreCase", rfe.getRegex().isIgnoreCase())
                    .setAttribute("ignoreDiacritic",
                            rfe.getRegex().isIgnoreDiacritic());
            PropertySetter.toXML(node, rfe.getOnSet());
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
