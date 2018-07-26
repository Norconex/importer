/* Copyright 2010-2018 Norconex Inc.
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.SplitTagger"&gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;

 *      &lt;split fromField="sourceFieldName" toField="targetFieldName"
 *               regex="[false|true]"&gt;
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

    private final List<Split> splits = new ArrayList<>();

    @Override
    public void tagApplicableDocument(
            String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        for (Split split : splits) {
            if (metadata.containsKey(split.getFromField())) {
                String[] metaValues = metadata.getStrings(split.getFromField())
                        .toArray(ArrayUtils.EMPTY_STRING_ARRAY);
                List<String> sameFieldValues =
                        SetUniqueList.setUniqueList(new ArrayList<String>());
                for (int i = 0; i < metaValues.length; i++) {
                    String metaValue = metaValues[i];
                    String[] splitValues = null;
                    if (split.isRegex()) {
                        splitValues = regexSplit(
                                metaValue, split.getSeparator());
                    } else {
                        splitValues = regularSplit(
                                metaValue, split.getSeparator());
                    }
                    if (ArrayUtils.isNotEmpty(splitValues)) {
                        if (StringUtils.isNotBlank(split.getToField())) {
                            metadata.addString(split.getToField(), splitValues);
                        } else {
                            sameFieldValues.addAll(Arrays.asList(splitValues));
                        }
                    }
                }
                if (StringUtils.isBlank(split.getToField())) {
                    metadata.setString(split.getFromField(),
                            sameFieldValues.toArray(
                                    ArrayUtils.EMPTY_STRING_ARRAY));
                }
            }
        }
    }


    private String[] regexSplit(String metaValue, String separator) {
        String[] values = metaValue.split(separator);
        List<String> cleanValues = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                cleanValues.add(value);
            }
        }
        return cleanValues.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }
    private String[] regularSplit(String metaValue, String separator) {
        return StringUtils.splitByWholeSeparator(metaValue, separator);
    }

    public List<Split> getSplits() {
        return Collections.unmodifiableList(splits);
    }

    public void removeSplit(String fromField) {
        List<Split> toRemove = new ArrayList<>();
        for (Split split : splits) {
            if (Objects.equals(split.getFromField(), fromField)) {
                toRemove.add(split);
            }
        }
        synchronized (splits) {
            splits.removeAll(toRemove);
        }
    }

    public void addSplit(
            String fromField, String separator, boolean regex) {
        splits.add(new Split(fromField, null, separator, regex));
    }
    public void addSplit(
            String fromField, String toField, String separator, boolean regex) {
        splits.add(new Split(fromField, toField, separator, regex));
    }


    public static class Split {
        private final String fromField;
        private final String toField;
        private final String separator;
        private final boolean regex;
        public Split(String fromField, String toField,
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
        public String getToField() {
            return toField;
        }
        public String getSeparator() {
            return separator;
        }
        public boolean isRegex() {
            return regex;
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
            addSplit(
                    node.getString("@fromField"),
                    node.getString("@toField", null),
                    node.getString("separator"),
                    node.getBoolean("@regex", false));
        }
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        for (Split split : splits) {
            XML sxml = xml.addElement("split")
                    .setAttribute("fromField", split.getFromField())
                    .setAttribute("toField", split.getToField())
                    .setAttribute("regex", split.isRegex());
            sxml.addElement("separator", split.getSeparator());
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
