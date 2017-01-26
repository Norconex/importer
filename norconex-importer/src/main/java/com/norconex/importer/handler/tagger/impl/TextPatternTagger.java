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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.tagger.AbstractStringTagger;

/**
 * <p>Extracts and add all text values matching the regular expression provided
 * in to a specified target field ("field").  The target field 
 * is considered a multi-value field.
 * </p>
 * <p>
 * An optional match group index can provided if you want to extract
 * only a portion of a regular expression match.
 * </p>
 * <p>
 * This class can be used as a pre-parsing handler on text documents only
 * or a post-parsing handler.
 * </p>
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TextPatternTagger"
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *      
 *      &lt;pattern field="targetFieldName" group="(optional match group index)"&gt;
 *          (regular expression)
 *      &lt;/pattern&gt;
 *      &lt;!-- multiple pattern tags allowed --&gt;
 * 
 *  &lt;/tagger&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following extracts what look like email addresses (simplified regex):
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.handler.tagger.impl.TextPatternTagger" &gt;
 *      &lt;pattern field="emails"&gt;
 *          [A-Za-z0-9+_.-]+?@[a-zA-Z0-9.-]+
 *      &lt;/pattern&gt;
 *  &lt;/tagger&gt;
 * </pre>
 *  
 * @author Pascal Essiembre
 * @since 2.3.0
 */
public class TextPatternTagger 
        extends AbstractStringTagger implements IXMLConfigurable {

    private Set<TextPattern> patterns = new HashSet<TextPattern>();

    private boolean caseSensitive;

    @Override
    protected void tagStringContent(String reference, StringBuilder content,
            ImporterMetadata metadata, boolean parsed, int sectionIndex) {
        int flags = Pattern.DOTALL;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        for (TextPattern tp : patterns) {
            Pattern p = Pattern.compile(tp.pattern, flags);
            Matcher match = p.matcher(content);
            while (match.find()) {
                String text = match.group(tp.group);
                metadata.addString(tp.field, text);
            }
        }
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching start and end text.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    /**
     * Adds a pattern that will extract the whole text matched.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     */
    public void addPattern(String field, String pattern) {
        addPattern(field, pattern, 0);
    }
    /**
     * Adds a new pattern, which will extract the value from the specified 
     * group index upon matching.
     * @param field target field to store the matching pattern.
     * @param pattern the pattern
     * @param group which pattern group to return.
     */
    public void addPattern(String field, String pattern, int group) {
        if (StringUtils.isAnyBlank(pattern, field)) {
            return;
        }
        this.patterns.add(new TextPattern(field, pattern, group));
    }
    
    @Override
    protected void loadStringTaggerFromXML(XMLConfiguration xml)
            throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("pattern");
        for (HierarchicalConfiguration node : nodes) {
            addPattern(
                    node.getString("[@field]", null),
                    node.getString("", null),
                    node.getInt("[@group]", 0));
        }
    }

    @Override
    protected void saveStringTaggerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
         writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        for (TextPattern tp : patterns) {
            writer.writeStartElement("pattern");
            writer.writeAttribute("field", tp.field);
            writer.writeAttributeInteger("group", tp.group);
            writer.writeCharacters(tp.pattern);
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
                .append(caseSensitive, castOther.caseSensitive)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(patterns)
                .append(caseSensitive)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("patterns", patterns)
                .append("caseSensitive", caseSensitive)
                .toString();
    }
    
    private static class TextPattern {
        private final String field;
        private final String pattern ;
        private final int group;
        public TextPattern(String field, String pattern, int group) {
            super();
            this.field = field;
            this.pattern = pattern;
            this.group = group;
        }
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof TextPattern)) {
                return false;
            }
            TextPattern castOther = (TextPattern) other;
            return new EqualsBuilder()
                    .append(field, castOther.field)
                    .append(pattern, castOther.pattern)
                    .append(group, castOther.group)
                    .isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(field)
                    .append(pattern)
                    .append(group)
                    .toHashCode();
        }
        private transient String toString;
        @Override
        public String toString() {
            if (toString == null) {
                toString = new ToStringBuilder(this)
                        .append("field", field)
                        .append("pattern", pattern)
                        .append("group", group)
                        .toString();
            }
            return toString;
        }
    }
}
