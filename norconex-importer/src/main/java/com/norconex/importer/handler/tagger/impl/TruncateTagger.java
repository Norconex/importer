/* Copyright 2017-2020 Norconex Inc.
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
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.text.StringUtil;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;

/**
 * <p>
 * Truncates a <code>fromField</code> value(s) and optionally replace truncated
 * portion by a hash value to help ensure uniqueness (not 100% guaranteed to
 * be collision-free).  If the field to truncate has multiple values, all
 * values will be subject to truncation. You can store the value(s), truncated
 * or not, in another target field.
 * </p>
 * <h3>Storing values in an existing field</h3>
 * <p>
 * If a target field with the same name already exists for a document,
 * values will be added to the end of the existing value list.
 * It is possible to change this default behavior by supplying a
 * {@link PropertySetter}.
 * </p>
 * <p>
 * When storing the truncated values in a new field already having one or
 * more values, the truncated values will be <i>added</i> to the list of
 * existing values, unless "overwrite" is set to <code>true</code>.
 * </p>
 * <p>
 * The <code>maxLength</code> is guaranteed to be respected. This means any
 * appended hash code and suffix will fit within the <code>maxLength</code>.
 * </p>
 * <p>
 * Can be used both as a pre-parse or post-parse handler.
 * </p>
 *
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TruncateTagger"
 *      fromField="(fromField holding one or more values to truncate)"
 *      maxLength="(maximum length)"
 *      toField="(optional fromField where to store the truncated value)"
 *      onSet="[append|prepend|replace|optional]"
 *      appendHash="[false|true]"
 *      suffix="(value to append after truncation. Goes before hash if one.)" &gt;
 *
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              fromField="(name of header/metadata fromField name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *  &lt;/handler&gt;
 * </pre>
 *
 * <h4>Usage example:</h4>
 * <p>
 * To truncate this "myField" value...
 * </p>
 * <pre>    Please truncate me before you start thinking I am too long.</pre>
 * <p>
 * ...to become this...
 * </p>
 * <pre>    Please truncate me before you start thi!0996700004</pre>
 * ...you would set a max length of 50, with a "!" suffix and append a hash.
 * Like this:
 *
 * <pre>
 *  &lt;handler class="com.norconex.importer.handler.tagger.impl.TruncateTagger"
 *      fromField="myField"
 *      maxLength="50"
 *      appendHash="true"
 *      suffix="!" /&gt;
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.8.0
 */
public class TruncateTagger extends AbstractDocumentTagger {

    private static final Logger LOG =
            LoggerFactory.getLogger(TruncateTagger.class);

    private String fromField;
    private int maxLength;
    private String toField;
    private PropertySetter onSet;
    private boolean appendHash;
    private String suffix;

    public TruncateTagger() {
        super();
    }
    public TruncateTagger(String fromField, int maxLength) {
        super();
        this.fromField = fromField;
        this.maxLength = maxLength;
    }
    public String getToField() {
        return toField;
    }
    public void setToField(String keepToField) {
        this.toField = keepToField;
    }

    /**
     * Gets whether existing value for the same field should be overwritten.
     * @return <code>true</code> if overwriting existing value.
     * @deprecated Since 3.0.0 use {@link #getOnSet()}.
     */
    @Deprecated
    public boolean isOverwrite() {
        return PropertySetter.REPLACE == onSet;
    }
    /**
     * Sets whether existing value for the same field should be overwritten.
     * @param overwrite <code>true</code> if overwriting existing value.
     * @deprecated Since 3.0.0 use {@link #setOnSet(PropertySetter)}.
     */
    @Deprecated
    public void setOverwrite(boolean overwrite) {
        this.onSet = overwrite
                ? PropertySetter.REPLACE : PropertySetter.APPEND;
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

    public boolean isAppendHash() {
        return appendHash;
    }
    public void setAppendHash(boolean appendHash) {
        this.appendHash = appendHash;
    }
    public String getSuffix() {
        return suffix;
    }
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    public int getMaxLength() {
        return maxLength;
    }
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    public String getFromField() {
        return fromField;
    }
    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    @Override
    public void tagApplicableDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        List<String> values = metadata.getStrings(fromField);
        List<String> truncValues = new ArrayList<>(values.size());
        for (String value : values) {
            String truncValue = truncate(value);
            truncValues.add(truncValue);
            if (LOG.isDebugEnabled()
                    && !Objects.equals(truncValue, value)) {
                LOG.debug("\"{}\" value truncated to \"{}\".",
                        fromField, truncValue);
            }
        }

        if (StringUtils.isNotBlank(getToField())) {
            // set on target field
            PropertySetter.orDefault(onSet).apply(
                    metadata, getToField(), truncValues);
        } else {
            // overwrite source field
            PropertySetter.REPLACE.apply(metadata, getFromField(), truncValues);
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (isAppendHash()) {
            return StringUtil.truncateWithHash(
                    value, getMaxLength(), getSuffix());
        }
        if (StringUtils.isNotEmpty(getSuffix())) {
            return StringUtils.abbreviate(value, getSuffix(), getMaxLength());
        }
        return StringUtils.truncate(value, getMaxLength());
    }

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("@overwrite", "onSet", true);
        fromField = xml.getString("@fromField", fromField);
        appendHash = xml.getBoolean("@appendHash", appendHash);
        suffix = xml.getString("@suffix", suffix);
        toField = xml.getString("@toField", toField);
        setOnSet(PropertySetter.fromXML(xml, onSet));
        maxLength = xml.getInteger("@maxLength", maxLength);
    }
    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("fromField", fromField);
        xml.setAttribute("appendHash", appendHash);
        xml.setAttribute("suffix", suffix);
        xml.setAttribute("toField", toField);
        PropertySetter.toXML(xml, onSet);
        xml.setAttribute("maxLength", maxLength);
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
