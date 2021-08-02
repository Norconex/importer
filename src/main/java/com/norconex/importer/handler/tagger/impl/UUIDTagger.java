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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.importer.handler.HandlerDoc;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.parser.ParseState;

/**
 * <p>Generates a random Universally unique identifier (UUID) and stores it
 * in the specified <code>field</code>.
 * If no <code>field</code> is provided, the UUID will be added to
 * <code>document.uuid</code>.
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
 * <p>Can be used both as a pre-parse or post-parse handler.</p>
 *
 * {@nx.xml.usage
 * <handler class="com.norconex.importer.handler.tagger.impl.UUIDTagger"
 *     toField="(target field)"
 *     {@nx.include com.norconex.commons.lang.map.PropertySetter#attributes}>
 *
 *   {@nx.include com.norconex.importer.handler.AbstractImporterHandler#restrictTo}
 *
 * </handler>
 * }
 *
 * {@nx.xml.example
 * <handler class="UUIDTagger" field="uuid" onSet="replace" />
 * }
 * <p>
 * The above generates a UUID and stores it in a "uuid" field, overwriting
 * any existing values under that field.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.7.0
 */
@SuppressWarnings("javadoc")
public class UUIDTagger extends AbstractDocumentTagger {

    public static final String DEFAULT_FIELD = "document.uuid";

    private String toField = DEFAULT_FIELD;
    private PropertySetter onSet;

    /**
     * Constructor.
     */
    public UUIDTagger() {
        super();
    }

    @Override
    public void tagApplicableDocument(
            HandlerDoc doc, InputStream document, ParseState parseState)
                    throws ImporterHandlerException {

        String uuid = UUID.randomUUID().toString();
        String finalField = toField;
        if (StringUtils.isBlank(finalField)) {
            finalField = DEFAULT_FIELD;
        }
        PropertySetter.orAppend(onSet).apply(
                doc.getMetadata(), finalField, uuid);
    }

    /**
     * Gets the target field.
     * @return target field
     * @since 3.0.0
     */
    public String getToField() {
        return toField;
    }
    /**
     * Sets the target field.
     * @param toField target field
     * @since 3.0.0
     */
    public void setToField(String toField) {
        this.toField = toField;
    }

    /**
     * Gets the target field.
     * @return target field
     * @deprecated Since 3.0.0, use {@link #getToField()}
     */
    @Deprecated
    public String getField() {
        return getToField();
    }
    /**
     * Sets the target field.
     * @param toField target field
     * @deprecated Since 3.0.0, use {@link #setToField(String)}
     */
    @Deprecated
    public void setField(String toField) {
        setToField(toField);
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

    @Override
    protected void loadHandlerFromXML(XML xml) {
        xml.checkDeprecated("@overwrite", "onSet", true);
        xml.checkDeprecated("@field", "toField", true);
        toField = xml.getString("@toField", toField);
        setOnSet(PropertySetter.fromXML(xml, onSet));
    }

    @Override
    protected void saveHandlerToXML(XML xml) {
        xml.setAttribute("toField", toField);
        PropertySetter.toXML(xml, onSet);
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
