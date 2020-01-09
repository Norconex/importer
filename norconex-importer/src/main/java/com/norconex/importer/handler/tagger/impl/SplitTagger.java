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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>Splits an existing metadata value into multiple values based on a given
 * value separator.  The "toField" argument
 * is optional (the same field will be used to store the splits if no
 * "toField" is specified").</p>
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.SplitTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;

 *      &lt;split fromField="sourceFieldName"
 *             toField="targetFieldName"
 *             regex="[false|true]"
 *             onSet="[append|prepend|replace|optional]"&gt;
 *          &lt;separator&gt;(separator value)&lt;/separator&gt;
 *      &lt;/split&gt;
 *      &lt;!-- multiple split tags allowed --&gt;
 *
 *  &lt;/handler&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following example splits a single value field holding a comma-separated
 * list into multiple values.
 * </p>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.SplitTagger"&gt;
 *      &lt;split fromField="myField" regex="true"&gt;
 *          &lt;separator&gt;\s*,\s*&lt;/separator&gt;
 *      &lt;/split&gt;
 *  &lt;/handler&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class SplitTagger extends AbstractDocumentTagger {

    private final List<SplitDetails> splits = new ArrayList<>();

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (SplitDetails split : splits) {
            if (metadata.containsKey(split.getFromField())) {
                List<String> sourceValues =
                        metadata.getStrings(split.getFromField());
                List<String> targetValues =
                        SetUniqueList.setUniqueList(new ArrayList<>());
                for (String metaValue : sourceValues) {
                    if (split.isRegex()) {
                        targetValues.addAll(regexSplit(
                                metaValue, split.getSeparator()));
                    } else {
                        targetValues.addAll(regularSplit(
                                metaValue, split.getSeparator()));
                    }
                }

                if (StringUtils.isNotBlank(split.getToField())) {
                    // set on target field
                    PropertySetter.orDefault(split.getOnSet()).apply(
                            metadata, split.getToField(), targetValues);
                } else {
                    // overwrite source field
                    PropertySetter.REPLACE.apply(
                            metadata, split.fromField, targetValues);
                }
            }
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
        private String fromField;
        private String toField;
        private PropertySetter onSet;
        private String separator;
        private boolean regex;
        public SplitDetails() {
            super();
        }
        public SplitDetails(String fromField, String separator, boolean regex) {
            this(fromField, null, separator, regex);
        }
        public SplitDetails(String fromField, String toField,
                String separator, boolean regex) {
            super();
            this.fromField = fromField;
            this.toField = toField;
            this.separator = separator;
            this.regex = regex;
        }
        public String getFromField() {
            return fromField;
        }
        public void setFromField(String fromField) {
            this.fromField = fromField;
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
        public boolean isRegex() {
            return regex;
        }
        public void setRegex(boolean regex) {
            this.regex = regex;
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
    protected void loadHandlerFromXML(XML xml) {
        for (XML node : xml.getXMLList("split")) {
            SplitDetails sd = new SplitDetails();
            sd.setFromField(node.getString("@fromField"));
            sd.setToField(node.getString("@toField", null));
            sd.setSeparator(node.getString("separator"));
            sd.setRegex(node.getBoolean("@regex", false));
            sd.setOnSet(PropertySetter.fromXML(node, null));
            addSplitDetails(sd);
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (SplitDetails split : splits) {
            XML sxml = xml.addElement("split")
                    .setAttribute("fromField", split.getFromField())
                    .setAttribute("toField", split.getToField())
                    .setAttribute("regex", split.isRegex());
            sxml.addElement("separator", split.getSeparator());
            PropertySetter.toXML(sxml, split.getOnSet());
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
