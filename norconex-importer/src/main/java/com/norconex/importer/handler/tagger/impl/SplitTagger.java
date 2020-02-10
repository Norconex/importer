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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractCharStreamTagger;

/**
 * <p>Splits an existing metadata value into multiple values based on a given
 * value separator (the separator gets discarded).  The "toField" argument
 * is optional (the same field will be used to store the splits if no
 * "toField" is specified"). Duplicates are removed.</p>
 * <p>Can be used both as a pre-parse (metadata or text content) or
 * post-parse handler.</p>
 * <p>
 * If no "fieldMatcher" expression is specified, the document content will be
 * used.  If the "fieldMatcher" matches more than one field, they will all
 * be split and stored in the same multi-value metadata field.
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.SplitTagger"
 *     {@nx.include com.norconex.importer.handler.tagger.AbstractCharStreamTagger#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 *   <!-- multiple split tags allowed -->
 *   <split
 *       toField="targetFieldName"
 *       {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *     <fieldMatcher {@nx.include com.norconex.commons.lang.text.TextMatcher#matchAttributes}>
 *       (one or more matching fields to split)
 *     </fieldMatcher>
 *     <separator regex="[false|true]">(separator value)</separator>
 *   </split>
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="com.norconex.importer.handler.tagger.impl.SplitTagger">
 *   <fieldMatcher>myField</fieldMatcher>
 *   <split>
 *     <separator regex="true">\s*,\s*</separator>
 *   </split>
 * </handler>
 * }
 * <p>
 * The above example splits a single value field holding a comma-separated
 * list into multiple values.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 1.3.0
 */
@SuppressWarnings("javadoc")
public class SplitTagger extends AbstractCharStreamTagger {

    private final List<SplitDetails> splits = new ArrayList<>();

    @Override
    protected void tagTextDocument(String reference, Reader input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (SplitDetails split : splits) {

            if (split.fieldMatcher.getPattern() == null) {
                splitContent(split, input, metadata);
            } else {
                splitMetadata(split, metadata);
            }
        }
    }

    private void splitContent(
            SplitDetails split, Reader input, ImporterMetadata metadata) {

        String delim = split.getSeparator();
        if (!split.isSeparatorRegex()) {
            delim = Pattern.quote(delim);
        }
        List<String> targetValues = new ArrayList<>();
        @SuppressWarnings("resource") // input stream controlled by caller.
        Scanner scanner = new Scanner(input).useDelimiter(delim);
        while (scanner.hasNext()) {
            targetValues.add(scanner.next());
        }
        PropertySetter.orDefault(split.getOnSet()).apply(
                metadata, split.getToField(), targetValues);
    }
    private void splitMetadata(SplitDetails split, ImporterMetadata metadata) {

        List<String> allTargetValues = new ArrayList<>();
        for (Entry<String, List<String>> en :
                metadata.matchKeys(split.fieldMatcher).entrySet()) {
            String fromField = en.getKey();
            List<String> sourceValues = en.getValue();
            List<String> targetValues = new ArrayList<>();
            for (String sourceValue : sourceValues) {
                if (split.isSeparatorRegex()) {
                    targetValues.addAll(regexSplit(
                            sourceValue, split.getSeparator()));
                } else {
                    targetValues.addAll(regularSplit(
                            sourceValue, split.getSeparator()));
                }
            }

            // toField is blank, we overwrite the source and do not
            // carry values further.
            if (StringUtils.isBlank(split.getToField())) {
                // overwrite source field
                PropertySetter.REPLACE.apply(
                        metadata, fromField, targetValues);
            } else {
                allTargetValues.addAll(targetValues);
            }
        }
        if (StringUtils.isNotBlank(split.getToField())) {
            // set on target field
            PropertySetter.orDefault(split.getOnSet()).apply(
                    metadata, split.getToField(), allTargetValues);
        }
    }

    private List<String> regexSplit(String metaValue, String separator) {
        String[] values = metaValue.split(separator);
        List<String> cleanValues = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                cleanValues.add(value);
            }
        }
        return cleanValues;
    }
    private List<String> regularSplit(String metaValue, String separator) {
        return Arrays.asList(
                StringUtils.splitByWholeSeparator(metaValue, separator));
    }

    public List<SplitDetails> getSplitDetailsList() {
        return Collections.unmodifiableList(splits);
    }
    @Deprecated
    public List<SplitDetails> getSplits() {
        return getSplitDetailsList();
    }
    public void removeSplitDetails(String fromField) {
        List<SplitDetails> toRemove = new ArrayList<>();
        for (SplitDetails split : splits) {
            if (Objects.equals(split.getFromField(), fromField)) {
                toRemove.add(split);
            }
        }
        synchronized (splits) {
            splits.removeAll(toRemove);
        }
    }
    @Deprecated
    public void removeSplit(String fromField) {
        removeSplitDetails(fromField);
    }

    public void addSplitDetails(SplitDetails sd) {
        if (sd != null) {
            splits.add(sd);
        }
    }

    @Deprecated
    public void addSplit(
            String fromField, String separator, boolean regex) {
        splits.add(new SplitDetails(fromField, null, separator, regex));
    }
    @Deprecated
    public void addSplit(
            String fromField, String toField, String separator, boolean regex) {
        splits.add(new SplitDetails(fromField, toField, separator, regex));
    }


    public static class SplitDetails {
        private final TextMatcher fieldMatcher = new TextMatcher();
        private String toField;
        private PropertySetter onSet;
        private String separator;
        private boolean separatorRegex;

        public SplitDetails() {
            super();
        }
        /**
         * Constructor.
         * @param fromField source field
         * @param separator split separator
         * @param regex is separator a regular expression
         * @deprecated Since 3.0.0, use
         *        {@link #SplitDetails(TextMatcher, String, String)}
         */
        @Deprecated
        public SplitDetails(String fromField, String separator, boolean regex) {
            this(fromField, null, separator, regex);
        }
        /**
         * Constructor.
         * @param fromField source field
         * @param toField target field
         * @param separator split separator
         * @param regex is separator a regular expression
         * @deprecated Since 3.0.0, use {@link
         *      #SplitDetails(TextMatcher, String, String, boolean)}
         */
        @Deprecated
        public SplitDetails(String fromField, String toField,
                String separator, boolean regex) {
            this(new TextMatcher(fromField), toField, separator);
            this.separatorRegex = regex;
        }
        /**
         * Constructor.
         * @param fieldMatcher source field matcher
         * @param toField target field
         * @param separator split separator
         * @since 3.0.0
         */
        public SplitDetails(
                TextMatcher fieldMatcher, String toField, String separator) {
            this(fieldMatcher, toField, separator, false);
        }
        /**
         * Constructor.
         * @param fieldMatcher source field matcher
         * @param toField target field
         * @param separator split separator
         * @param separatorRegex whether the separator is a regular expression
         * @since 3.0.0
         */
        public SplitDetails(TextMatcher fieldMatcher, String toField,
                String separator, boolean separatorRegex) {
            super();
            this.fieldMatcher.copyFrom(fieldMatcher);
            this.toField = toField;
            this.separator = separator;
            this.separatorRegex = separatorRegex;
        }

        /**
         * Gets field matcher for fields to split.
         * @return field matcher
         * @since 3.0.0
         */
        public TextMatcher getFieldMatcher() {
            return fieldMatcher;
        }
        /**
         * Sets the field matcher for fields to split.
         * @param fieldMatcher field matcher
         * @since 3.0.0
         */
        public void setFieldMatcher(TextMatcher fieldMatcher) {
            this.fieldMatcher.copyFrom(fieldMatcher);
        }

        /**
         * Gets the from field.
         * @return from field
         * @deprecated Since 3.0.0, use {@link #getFieldMatcher()} instead
         */
        @Deprecated
        public String getFromField() {
            return fieldMatcher.getPattern();
        }
        /**
         * Sets the from field.
         * @param fromField from field.
         * @deprecated Since 3.0.0, use
         *             {@link #setFieldMatcher(TextMatcher)} instead
         */
        @Deprecated
        public void setFromField(String fromField) {
            this.fieldMatcher.setPattern(fromField);
        }
        public String getToField() {
            return toField;
        }
        public void setToField(String toField) {
            this.toField = toField;
        }
        public String getSeparator() {
            return separator;
        }
        public void setSeparator(String separator) {
            this.separator = separator;
        }
        /**
         * Gets whether regular expression is used.
         * @return <code>true</code> if regex is used
         * @deprecated Since 3.0.0, use {@link #isSeparatorRegex()} instead
         */
        @Deprecated
        public boolean isRegex() {
            return isSeparatorRegex();
        }
        /**
         * Gets whether the separator value is a regular expression.
         * @return <code>true</code> if a regular expression.
         * @since 3.0.0
         */
        public boolean isSeparatorRegex() {
            return separatorRegex;
        }
        /**
         * Sets whether regular expression is used.
         * @param regex <code>true</code> if regex is used
         * @deprecated Since 3.0.0, use
         *             {@link #setSeparatorRegex(boolean)} instead
         */
        @Deprecated
        public void setRegex(boolean regex) {
            setSeparatorRegex(regex);
        }
        /**
         * Sets whether the separator value is a regular expression.
         * @param regex <code>true</code> if a regular expression.
         * @since 3.0.0
         */
        public void setSeparatorRegex(boolean regex) {
            this.separatorRegex = regex;
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
        for (XML node : xml.getXMLList("split")) {
            node.checkDeprecated("@fromField", "fieldMatcher", true);
            node.checkDeprecated(
                    "@regex", "separator[@regex]", true);
            SplitDetails sd = new SplitDetails();
            sd.fieldMatcher.loadFromXML(node.getXML("fieldMatcher"));
            sd.setToField(node.getString("@toField", null));
            sd.setSeparator(node.getString("separator"));
            sd.setSeparatorRegex(node.getBoolean("separator/@regex", false));
            sd.setOnSet(PropertySetter.fromXML(node, null));
            addSplitDetails(sd);
        }
    }

    @Override
    protected void saveCharStreamTaggerToXML(XML xml) {
        for (SplitDetails split : splits) {
            XML sxml = xml.addElement("split")
                    .setAttribute("toField", split.getToField());
            sxml.addElement("separator", split.getSeparator())
                    .setAttribute("regex", split.isSeparatorRegex());
            PropertySetter.toXML(sxml, split.getOnSet());
            split.fieldMatcher.saveToXML(sxml.addElement("fieldMatcher"));
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
