/* Copyright 2010-2020 Norconex Inc.
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
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractStringTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Extracts and add values found between a matching start and
 * end strings to a document metadata field.
 * The matching string end-points are defined in pairs and multiple ones
 * can be specified at once. The field specified for a pair of end-points
 * is considered a multi-value field.</p>
 * <p>
 * If "fieldMatcher" is specified, it will use content from matching fields and
 * storing all text extracted into the target field, multi-value.
 * Else, the document content is used.
 * </p>
 *
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 * <p>
 * This class can be used as a pre-parsing handler on text documents only
 * or a post-parsing handler.
 * </p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.TextBetweenTagger"
 *     {@nx.include com.norconex.importer.handler.tagger.AbstractStringTagger#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple textBetween tags allowed -->
 *   <textBetween
 *       toField="(target field name)"
 *       inclusive="[false|true]"
 *       {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *     <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (optional expression matching fields to perform extraction on)
 *     </fieldMatcher>
 *     <startMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (expression matching "left" delimiter)
 *     </startMatcher>
 *     <endMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (expression matching "right" delimiter)
 *     </endMatcher>
 *   </textBetween>
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.TextBetweenTagger">
 *   <textBetween toField="content">
 *     <startMatcher>OPEN</startMatcher>
 *     <endMatcher>CLOSE</endMatcher>
 *   </textBetween>
 * </handler>
 * }
 * <p>
 * The above example extract the content between "OPEN" and
 * "CLOSE" strings, excluding these strings, and store it in a "content"
 * field.
 * </p>
 * @author Pascal Essiembre
 */
@SuppressWarnings("javadoc")
public class TextBetweenTagger
        extends AbstractStringTagger implements IXMLConfigurable {

    private final List<TextBetweenDetails> betweens = new ArrayList<>();

    @Override
    protected void tagStringContent(HandlerDoc doc, StringBuilder content,
            ParseState parseState, int sectionIndex)
                    throws ImporterHandlerException {
        for (TextBetweenDetails between : betweens) {
            if (between.fieldMatcher.getPattern() == null) {
                betweenContent(between, content, doc.getMetadata());
            } else {
                betweenMetadata(between, doc.getMetadata());
            }
        }
    }

    private void betweenContent(TextBetweenDetails between,
            StringBuilder content, Properties metadata) {
        PropertySetter.orAppend(between.onSet).apply(metadata,
                between.toField, betweenText(between, content.toString()));
    }
    private void betweenMetadata(
            TextBetweenDetails between, Properties metadata) {
        List<String> allTargetValues = new ArrayList<>();
        for (Entry<String, List<String>> en :
                metadata.matchKeys(between.fieldMatcher).entrySet()) {
            String fromField = en.getKey();
            List<String> sourceValues = en.getValue();
            List<String> targetValues = new ArrayList<>();
            for (String sourceValue : sourceValues) {
                targetValues.addAll(betweenText(between, sourceValue));
            }

            // if toField is blank, we overwrite the source and do not
            // carry values further.
            if (StringUtils.isBlank(between.getToField())) {
                // overwrite source field
                PropertySetter.REPLACE.apply(
                        metadata, fromField, targetValues);
            } else {
                allTargetValues.addAll(targetValues);
            }
        }
        if (StringUtils.isNotBlank(between.getToField())) {
            // set on target field
            PropertySetter.orAppend(between.onSet).apply(
                    metadata, between.toField, allTargetValues);
        }
    }
    private List<String> betweenText(
            TextBetweenDetails between, String text) {
        List<Pair<Integer, Integer>> matches = new ArrayList<>();
        Matcher leftMatch = between.startMatcher.toRegexMatcher(text);
        while (leftMatch.find()) {
            Matcher rightMatch = between.endMatcher.toRegexMatcher(text);
            if (rightMatch.find(leftMatch.end())) {
                if (between.inclusive) {
                    matches.add(new ImmutablePair<>(
                            leftMatch.start(), rightMatch.end()));
                } else {
                    matches.add(new ImmutablePair<>(
                            leftMatch.end(), rightMatch.start()));
                }
            } else {
                break;
            }
        }
        List<String> values = new ArrayList<>();
        for (int i = matches.size() -1; i >= 0; i--) {
            Pair<Integer, Integer> matchPair = matches.get(i);
            String value = text.substring(
                    matchPair.getLeft(), matchPair.getRight());
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Gets whether start and end text pairs should be kept or
     * not.
     * @return always <code>false</code>
     * @deprecated Since 3.0.0, use {@link TextBetweenDetails#isInclusive()}
     */
    @Deprecated
    public boolean isInclusive() {
        return false;
    }
    /**
     * Sets whether start and end text pairs should be kept or
     * not. <b>Calling this method has no effect.</b>
     * @param inclusive <code>true</code> to keep matching start and end text
     * @deprecated Since 3.0.0, use {@link TextBetweenDetails#setInclusive(boolean)}
     */
    @Deprecated
    public void setInclusive(boolean inclusive) {
        //NOOP
    }
    /**
     * Gets whether to ignore case when matching start and end text.
     * @return always <code>false</code>
     * @deprecated Since 3.0.0, use {@link TextBetweenDetails#isCaseSensitive()}
     */
    @Deprecated
    public boolean isCaseSensitive() {
        return false;
    }
    /**
     * Sets whether to ignore case when matching start and end text.
     * <b>Calling this method has no effect.</b>
     * @param caseSensitive <code>true</code> to consider character case
     * @deprecated Since 3.0.0,
     *             use {@link TextBetweenDetails#setCaseSensitive(boolean)}
     */
    @Deprecated
    public void setCaseSensitive(boolean caseSensitive) {
        //NOOP
    }
    /**
     * Adds a new pair of end points to match.
     * @param toField target metadata field name where to store the extracted
     *             values
     * @param fromText the left string to match
     * @param toText the right string to match
     * @deprecated Since 3.0.0, use
     *              {@link #addTextBetweenDetails(TextBetweenDetails)}
     */
    @Deprecated
    public void addTextEndpoints(
            String toField, String fromText, String toText) {
        if (StringUtils.isBlank(toField)
                || StringUtils.isBlank(fromText)
                || StringUtils.isBlank(toText)) {
            return;
        }
        betweens.add(new TextBetweenDetails(toField, fromText, toText));
    }
    /**
     * Adds text between instructions.
     * @param details "text between" details
     */
    public void addTextBetweenDetails(TextBetweenDetails details) {
        betweens.add(details);
    }
    /**
     * Gets text between instructions.
     * @return "text between" details
     * @since 3.0.0
     */
    public List<TextBetweenDetails> getTextBetweenDetailsList() {
        return new ArrayList<>(betweens);
    }

    @Override
    protected void loadStringTaggerFromXML(XML xml) {
        xml.checkDeprecated("@caseSensitive",
                "startMatcher@ignoreCase and endMatcher@ignoreCase", true);
        xml.checkDeprecated("@name", "toField", true);

        List<XML> nodes = xml.getXMLList("textBetween");
        for (XML node : nodes) {
            TextBetweenDetails tbd = new TextBetweenDetails();
            tbd.setToField(node.getString("@toField", null));
            tbd.setInclusive(node.getBoolean("@inclusive", false));
            tbd.setOnSet(PropertySetter.fromXML(node, null));
            tbd.fieldMatcher.loadFromXML(node.getXML("fieldMatcher"));
            tbd.startMatcher.loadFromXML(node.getXML("startMatcher"));
            tbd.endMatcher.loadFromXML(node.getXML("endMatcher"));
            addTextBetweenDetails(tbd);
        }
    }

    @Override
    protected void saveStringTaggerToXML(XML xml) {
        for (TextBetweenDetails between : betweens) {
            XML bxml = xml.addElement("textBetween")
                    .setAttribute("toField", between.toField)
                    .setAttribute("inclusive", between.inclusive);
            PropertySetter.toXML(bxml, between.getOnSet());
            between.fieldMatcher.saveToXML(bxml.addElement("fieldMatcher"));
            between.startMatcher.saveToXML(bxml.addElement("startMatcher"));
            between.endMatcher.saveToXML(bxml.addElement("endMatcher"));
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

    public static class TextBetweenDetails {
        private final TextMatcher fieldMatcher = new TextMatcher();
        private final TextMatcher startMatcher = new TextMatcher();
        private final TextMatcher endMatcher = new TextMatcher();
        private String toField;
        private boolean inclusive;
        private PropertySetter onSet;
        /**
         * Constructor.
         * @param toField target field
         * @param start start matcher
         * @param end end matcher
         * @deprecated Since 3.0.0.
         */
        @Deprecated
        public TextBetweenDetails(String toField, String start, String end) {
            super();
            this.toField = toField;
            this.startMatcher.setPattern(start);
            this.endMatcher.setPattern(end);
        }

        /**
         * Constructor.
         * @since 3.0.0
         */
        public TextBetweenDetails() {
            super();
        }
        /**
         * Constructor.
         * @param toField target field
         * @param fieldMatcher optional source fields
         * @param startMatcher start matcher
         * @param endMatcher end matcher
         * @since 3.0.0
         */
        public TextBetweenDetails(
                String toField, TextMatcher fieldMatcher,
                TextMatcher startMatcher, TextMatcher endMatcher) {
            super();
            this.toField = toField;
            this.fieldMatcher.copyFrom(fieldMatcher);
            this.startMatcher.copyFrom(startMatcher);
            this.endMatcher.copyFrom(endMatcher);
        }

        /**
         * Gets field matcher for fields on which to extract values.
         * @return field matcher
         * @since 3.0.0
         */
        public TextMatcher getFieldMatcher() {
            return fieldMatcher;
        }
        /**
         * Sets field matcher for fields on which to extract values.
         * @param fieldMatcher field matcher
         * @since 3.0.0
         */
        public void setFieldMatcher(TextMatcher fieldMatcher) {
            this.fieldMatcher.copyFrom(fieldMatcher);
        }
        /**
         * Gets the start delimiter matcher for text to extract.
         * @return start delimiter matcher
         * @since 3.0.0
         */
        public TextMatcher getStartMatcher() {
            return startMatcher;
        }
        /**
         * Sets the start delimiter matcher for text to extract.
         * @param startMatcher start delimiter matcher
         * @since 3.0.0
         */
        public void setStartMatcher(TextMatcher startMatcher) {
            this.startMatcher.copyFrom(startMatcher);
        }
        /**
         * Gets the end delimiter matcher for text to extract.
         * @return end delimiter matcher
         * @since 3.0.0
         */
        public TextMatcher getEndMatcher() {
            return endMatcher;
        }
        /**
         * Sets the end delimiter matcher for text to extract.
         * @param endMatcher end delimiter matcher
         * @since 3.0.0
         */
        public void setEndMatcher(TextMatcher endMatcher) {
            this.endMatcher.copyFrom(endMatcher);
        }
        /**
         * Sets the target field for extracted text.
         * @param toField target field
         * @since 3.0.0
         */
        public void setToField(String toField) {
            this.toField = toField;
        }

        public boolean isInclusive() {
            return inclusive;
        }
        public void setInclusive(boolean inclusive) {
            this.inclusive = inclusive;
        }

        /**
         * Gets whether matching is case sensitive.
         * @return <code>true</code> if case sensitive
         * @deprecated Since 3.0.0, use {@link #getStartMatcher()}
         *     and {@link #getEndMatcher()}
         */
        @Deprecated
        public boolean isCaseSensitive() {
            return !startMatcher.isIgnoreCase();
        }
        /**
         * Sets whether matching is case sensitive.
         * @param caseSensitive <code>true</code> if case sensitive
         * @deprecated Since 3.0.0, use {@link #setStartMatcher(TextMatcher)}
         *     and {@link #setEndMatcher(TextMatcher)}
         */
        @Deprecated
        public void setCaseSensitive(boolean caseSensitive) {
            startMatcher.setIgnoreCase(!caseSensitive);
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
         * Gets target field.
         * @return target field.
         * @deprecated Since 3.0.0, use {@link #getToField()} instead
         */
        @Deprecated
        public String getName() {
            return getToField();
        }

        public String getToField() {
            return toField;
        }

        /**
         * Gets start expression.
         * @return start expression
         * @deprecated Since 3.0.0, use {@link #getStartMatcher()}.
         */
        @Deprecated
        public String getStart() {
            return startMatcher.getPattern();
        }
        /**
         * Gets end expression.
         * @return end expression
         * @deprecated Since 3.0.0, use {@link #getEndMatcher()}.
         */
        @Deprecated
        public String getEnd() {
            return endMatcher.getPattern();
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
}
