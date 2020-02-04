/* Copyright 2016-2020 Norconex Inc.
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
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.io.TextReader;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.text.TextMatcher.Method;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;

/**
 * <p>
 * Counts the number of matches of a given string (or string pattern) and
 * store the resulting value in a field in the specified "toField".
 * </p>
 * <p>
 * If no "fieldMatcher" expression is specified, the document content will be
 * used.  If the "fieldMatcher" matches more than one field, the sum of all
 * matches will be stored as a single value. More often than not,
 * you probably want to set your "countMatcher" to "partial".
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * the count value will be added to the end of the existing value list.
 * It is possible to change this default behavior
 * with {@link #setOnSet(PropertySetter)}.
 * </p>
 *
 * <p>Can be used as a pre-parse tagger on text document only when matching
 * strings on document content, or both as a pre-parse or post-parse handler
 * when the "fieldMatcher" is used.</p>
 *
 * {@nx.xml.usage
 *  <handler class="com.norconex.importer.handler.tagger.impl.CountMatchesTagger"
 *      toField="(target field)"
 *      maxReadSize="(max characters to read at once)"
 *      {@nx.include com.norconex.importer.handler.tagger.AbstractCharStreamTagger#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (optional expression for fields used to count matches)
 *   </fieldMatcher>
 *
 *   <countMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *     (expression used to count matches)
 *   </countMatcher>
 *
 *  </handler>
 * }
 *
 * {@nx.xml.example
 *  <handler class="com.norconex.importer.handler.tagger.impl.CountMatchesTagger"
 *      toField="urlSegmentCount">
 *    <fieldMatcher>document.reference</fieldMatcher>
 *    <countMatcher method="regex">/[^/]+</countMatcher>
 *  </handler>
 * }
 * <p>
 * The above will count the number of segments in a URL.
 * </p>
 *
 * @author Pascal Essiembre
 * @see Pattern
 * @since 2.6.0
 */
@SuppressWarnings("javadoc")
public class CountMatchesTagger extends AbstractCharStreamTagger {

    private TextMatcher fieldMatcher = new TextMatcher();
    private TextMatcher countMatcher = new TextMatcher();
    private String toField;
    private PropertySetter onSet;
    private int maxReadSize = TextReader.DEFAULT_MAX_READ_SIZE;

    @Override
    protected void tagTextDocument(String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        // "toField" and value must be present.
        if (StringUtils.isBlank(getToField())) {
            throw new IllegalArgumentException("'toField' cannot be blank.");
        }
        if (countMatcher.getPattern() == null) {
            throw new IllegalArgumentException(
                    "'countMatcher' pattern cannot be null.");
        }

        int count = 0;
        if (fieldMatcher.getPattern() == null) {
            count = countContentMatches(input);
        } else {
            count = countFieldMatches(metadata);
        }

        PropertySetter.orDefault(onSet).apply(metadata, getToField(), count);
    }

    private int countFieldMatches(ImporterMetadata metadata) {
        int count = 0;
        for (String value : metadata.matchKeys(fieldMatcher).valueList()) {
            Matcher m = countMatcher.toMatcher(value);
            while (m.find()) {
                count++;
            }
        }
        return count;
    }
    private int countContentMatches(Reader reader)
            throws ImporterHandlerException {
        int count = 0;
        String text = null;
        try (TextReader tr = new TextReader(reader, maxReadSize)) {
            while ((text = tr.readText()) != null) {
                Matcher m = countMatcher.toMatcher(text);
                while (m.find()) {
                    count++;
                }
            }
        } catch (IOException e) {
            throw new ImporterHandlerException("Cannot tag text document.", e);
        }
        return count;
    }

    /**
     * Gets the maximum number of characters to read from content for tagging
     * at once. Default is {@link TextReader#DEFAULT_MAX_READ_SIZE}.
     * @return maximum read size
     */
    public int getMaxReadSize() {
        return maxReadSize;
    }
    /**
     * Sets the maximum number of characters to read from content for tagging
     * at once.
     * @param maxReadSize maximum read size
     */
    public void setMaxReadSize(int maxReadSize) {
        this.maxReadSize = maxReadSize;
    }

    /**
     * Gets the field matcher.
     * @return field matcher
     * @since 3.0.0
     */
    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    /**
     * Sets the field matcher.
     * @param fieldMatcher field matcher
     * @since 3.0.0
     */
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher = fieldMatcher;
    }

    /**
     * Gets the count matcher.
     * @return count matcher
     * @since 3.0.0
     */
    public TextMatcher getCountMatcher() {
        return countMatcher;
    }
    /**
     * Sets the count matcher.
     * @param countMatcher count matcher
     * @since 3.0.0
     */
    public void setCountMatcher(TextMatcher countMatcher) {
        this.countMatcher = countMatcher;
    }

    /**
     * Sets the target field.
     * @return target field
     * @since 3.0.0
     */
    public String getToField() {
        return toField;
    }
    /**
     * Gets the target field.
     * @param toField target field
     * @since 3.0.0
     */
    public void setToField(String toField) {
        this.toField = toField;
    }

    /**
     * Gets the property setter to use when a value is set.
     * @return property setter
     * @since 3.0.0
     */
    public PropertySetter getOnSet() {
        return onSet;
    }
    /**
     * Sets the property setter to use when a value is set.
     * @param onSet property setter
     * @since 3.0.0
     */
    public void setOnSet(PropertySetter onSet) {
        this.onSet = onSet;
    }

    /**
     * Gets matches details.
     * @return matches details
     * @deprecated Since 3.0.0, use {@link #getToField()},
     *             {@link #getFieldMatcher()}, and {@link #getCountMatcher()}.
     */
    @Deprecated
    public List<MatchDetails> getMatchesDetails() {
        MatchDetails md = new MatchDetails(
                fieldMatcher.getPattern(),
                getToField(),
                getCountMatcher().getPattern());
        md.setCaseSensitive(!countMatcher.isIgnoreCase());
        md.setRegex(Method.REGEX == countMatcher.getMethod());
        return Collections.unmodifiableList(Arrays.asList(md));
    }
    /**
     * Removes match details.
     * @param matchDetails match details
     * @deprecated Since 3.0.0, this method does nothing.
     */
    @Deprecated
    public void removeMatchDetails(MatchDetails matchDetails) {
        //NOOP
    }
    /**
     * Adds a match details.
     * @param matchDetails the match details
     * @deprecated Since 3.0.0, use {@link #setToField(String)},
     *             {@link #setFieldMatcher(TextMatcher)},
     *             and {@link #setCountMatcher(TextMatcher)}.
     */
    @Deprecated
    public void addMatchDetails(MatchDetails matchDetails) {
        setToField(matchDetails.getToField());
        countMatcher.setMethod(
                matchDetails.isRegex() ? Method.REGEX : Method.BASIC);
        countMatcher.setIgnoreCase(!matchDetails.isCaseSensitive());
        fieldMatcher.setPattern(matchDetails.getFromField());
    }

    @Deprecated
    public static class MatchDetails {
        private String fromField;
        private String toField;
        private String value;
        private boolean regex;
        private boolean caseSensitive;
        public MatchDetails() {
            super();
        }
        public MatchDetails(
                String fromField, String toField, String value) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.value = value;
        }
        public String getFromField() {
            return fromField;
        }
        public String getValue() {
            return value;
        }
        public String getToField() {
            return toField;
        }
        public boolean isRegex() {
            return regex;
        }
        /**
         * Whether the matching should be case sensitive or not.
         * @return <code>true</code> if case sensitive
         */
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        /**
         * Sets the field with the value we want to perform matches on.
         * @param fromField field with the value to perform matches on
         */
        public void setFromField(String fromField) {
            this.fromField = fromField;
        }
        /**
         * Sets the text or regular expression to match
         * @param value the substring to match or regular expression
         */
        public void setValue(String value) {
            this.value = value;
        }
        /**
         * Sets the field to store the match count.
         * @param toField field to store the match count
         */
        public void setToField(String toField) {
            this.toField = toField;
        }
        /**
         * Sets whether the <code>value</code> to match is a regular expression.
         * @param regex <code>true</code> if <code>value</code> is a
         *              regular expression
         */
        public void setRegex(boolean regex) {
            this.regex = regex;
        }
        /**
         * Sets whether to do a case sensitive match or not.
         * Matches are not case sensitive by default.
         * @param caseSensitive <code>true</code> if doing a case sensitive
         *                      match
         */
        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
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



    @Override
    protected void loadCharStreamTaggerFromXML(XML xml) {
        xml.checkDeprecated(
                "countMatches", "fieldMatcher and countMatcher", true);
        setOnSet(PropertySetter.fromXML(xml, onSet));
        setToField(xml.getString("@toField", toField));
        setMaxReadSize(xml.getInteger("@maxReadSize", maxReadSize));
        fieldMatcher.loadFromXML(xml.getXML("fieldMatcher"));
        countMatcher.loadFromXML(xml.getXML("countMatcher"));
    }
    @Override
    protected void saveCharStreamTaggerToXML(XML xml) {
        PropertySetter.toXML(xml, getOnSet());
        xml.setAttribute("toField", toField);
        xml.setAttribute("maxReadSize", maxReadSize);
        fieldMatcher.saveToXML(xml.addElement("fieldMatcher"));
        countMatcher.saveToXML(xml.addElement("countMatcher"));
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
